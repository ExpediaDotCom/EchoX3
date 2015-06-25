/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.cache;

import java.util.Iterator;
import java.util.Map;

import com.expedia.echox3.basics.collection.simple.CopyOnSizeArray;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.internal.store.wrapper.ObjectWrapper;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.internal.store.wrapper.TrackingObjects;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceRequest;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceResponse;

public class LocalObjectBucket        // Bin type
{
	private static final BasicLogger		LOGGER						= new BasicLogger(LocalObjectCache.class);
	private static final int				BIN_COUNT_DEFAULT			= 11;

	// The lock protects the m_binList, needs writeLock only when modifying the list.
	private final ObjectCacheConfiguration m_configuration;
	private final TrackingObjects				m_trackingObjects;
	private final int							m_bucketNumber;
	private final String						m_bucketName;

	private CopyOnSizeArray<LocalObjectBin>		m_binList;
	private volatile int						m_binCountCurrent;
	private volatile int						m_binCountPrevious			= 0;

	public LocalObjectBucket(ObjectCacheConfiguration configuration, TrackingObjects trackingObjects, int bucketNumber)
	{
		m_configuration = configuration;
		m_trackingObjects = trackingObjects;
		m_bucketNumber = bucketNumber;
		m_bucketName = String.format("%s.Bucket-%4d", configuration.getCacheName(), bucketNumber);

		int		binCount		= m_configuration.getBinCountMin();
		m_binList = createBinList(binCount);
		m_binCountCurrent = m_binList.length();
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public final ObjectCacheConfiguration getConfiguration()
	{
		return m_configuration;
	}

	public final TrackingObjects getTrackingObjects()
	{
		return m_trackingObjects;
	}

	public String getBucketName()
	{
		return m_bucketName;
	}

	private CopyOnSizeArray<LocalObjectBin> createBinList(int count)
	{
		if (0 == count)
		{
			count = BIN_COUNT_DEFAULT;
		}
		CopyOnSizeArray<LocalObjectBin>		binList		= new CopyOnSizeArray<>(count);
		fillBinList(binList, 0, count);
		return binList;
	}

	private void fillBinList(CopyOnSizeArray<LocalObjectBin> binList, int binMin, int binMax)
	{
		for (int i = binMin; i < binMax; i++)
		{
			binList.set(i, createBin(i));
		}
	}
	private LocalObjectBin createBin(int binNumber)
	{
		return new LocalObjectBin(getConfiguration(), getTrackingObjects(), getBucketName(), binNumber);
	}

	public void resize(int binCount)
	{
		if (m_binList.length() == binCount)
		{
			// No change!
			return;
		}

		int		binCountPrevious	= m_binCountCurrent;
		int		binCountCurrent		= binCount;
		if (binCount > m_binList.length())
		{
			// Adjust the m_binList at the beginning if going bigger...
			m_binList.resize(binCount);
			fillBinList(m_binList, binCountPrevious, binCountCurrent);
		}

		m_binCountPrevious	= binCountPrevious;
		m_binCountCurrent	= binCountCurrent;

		long					t1				= System.nanoTime();
		long					itemCount		= 0;
		for (int i = 0; i < m_binCountPrevious; i++)
		{
			LocalObjectBin binFrom			= m_binList.get(i);
			// Lock the map of the bin for write, as items are moved out of this map
			IOperationContext				contextMap		= binFrom.getMapLock().lockWrite();
			Map<ObjectKey, ObjectWrapper>	itemMap			= binFrom.getEntryMap();
			{
				Iterator<Map.Entry<ObjectKey, ObjectWrapper>> iterator	= itemMap.entrySet().iterator();
				while (iterator.hasNext())
				{
					itemCount++;
					Map.Entry<ObjectKey, ObjectWrapper>		entry		= iterator.next();
					ObjectKey								itemKey		= entry.getKey();
					int										hash		= itemKey.getKeyForBin();
					int										binIndex	= hash % m_binCountCurrent;
					if (binIndex != i)
					{
						// Need to lock the bin where the write is taking place
						ObjectWrapper		objectWrapper	= entry.getValue();
						LocalObjectBin bin				= m_binList.get(binIndex);
						bin.moveEntryInto(objectWrapper);		// into the appropriate bin of this bucket
						iterator.remove();		// Remove only after put, so other thread can always find the item.
					}
//						else
//						{
//							continue;	// Already in the correct bin
//						}
				}
			}
			binFrom.getMapLock().unlockWrite(contextMap, true);

			// TODO  Magic number (75): Set to Eviction level
			// Apply pacing to avoid triggering an old gen GC.
			// i.e. Do not free objects (e.g. from old HashMap) faster than GC can keep-up with.
			while (75 < BasicTools.getHeapPercent(false))
			{
				BasicTools.sleepMS(1000);
			}
		}

		long				t2				= System.nanoTime();
		long				durationNS		= t2 - t1;
		getLogger().info(BasicEvent.EVENT_BIN_COUNT_CHANGE,
				"Bucket %s (%,d items) resized from %,d bins to %,d bins in %s",
				getBucketName(), itemCount, m_binCountPrevious, m_binCountCurrent,
				TimeUnits.formatNS(durationNS));

		// Adjust the m_binList at the end if going smaller...
		if (binCount < m_binList.length())
		{
			m_binList.resize(binCount);
		}
	}

	/**
	 * Request from the cache to re-distribute the items in this bucket amongst
	 * presumably newly created buckets in the (presumably the owner of this bucket)
	 * cache specified as the parameter.
	 * Typically called when changing the number of buckets.
	 *
	 * NOTE: No counter operation as the item stays in the same cache.
	 *
	 * @param cacheTo    cache where the items are going.
	 */
	public long moveItems(LocalObjectCache cacheTo)
	{
		// Move the items one bin at a time...
		// The bin list must be locked during the move.
		// ok to be in the middle of a bin resize, but the bin resize is interrupted
		long		itemCount		= 0;
		for (int iBin = 0; iBin < m_binCountCurrent; iBin++)
		{
			LocalObjectBin bin				= m_binList.get(iBin);
			IOperationContext		binContext		= bin.getMapLock().lockWrite();

			Iterator<Map.Entry<ObjectKey, ObjectWrapper>>
									iterator		= bin.getEntryMap().entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<ObjectKey, ObjectWrapper>
									entry				= iterator.next();
				ObjectKey			key					= entry.getKey();
				int					bucketNumber		= key.getKeyForMap() % cacheTo.getBucketCountCurrent();
				if (bucketNumber != m_bucketNumber)
				{
					itemCount++;
					ObjectWrapper		objectWrapper	= entry.getValue();
					LocalObjectBucket	bucket			= cacheTo.getBucketList().get(bucketNumber);
					bucket.moveEntryIntoBin(objectWrapper);		// into the appropriate bin of the other bucket
					iterator.remove();
				}
			}
			bin.getMapLock().unlockWrite(binContext, true);
		}
		return itemCount;
	}
	// NOTE: No counter operation as the item stays in the same cache.
	private void moveEntryIntoBin(ObjectWrapper objectWrapper)
	{
		int					binIndex		= objectWrapper.getKeyForBin() % m_binCountCurrent;
		LocalObjectBin bin				= m_binList.get(binIndex);
		bin.moveEntryInto(objectWrapper);
	}

	public ObjectWrapper getEntry(ObjectKey key, LocalObjectFactoryWrapper factory) throws BasicException
	{
		ObjectWrapper		entry		= null;
		int					hash		= key.getKeyForBin();
		if (0 != m_binCountPrevious)	// In case the # of bins is in the middle of a resize
		{
			int				binIndex	= hash % m_binCountPrevious;
			LocalObjectBin bin			= m_binList.get(binIndex);
			entry = bin.getEntry(key, null);
		}
		if (null == entry)
		{
			int				binIndex	= hash % m_binCountCurrent;
			LocalObjectBin bin			= m_binList.get(binIndex);
			entry = bin.getEntry(key, factory);
		}

		return entry;
	}
	public void deleteEntry(ObjectKey key)
	{
		// Note that although bin.deleteEntry is called twice, the entry will only exist in one of the bins.

		if (0 != m_binCountPrevious)
		{
			// if applicable, remove it from the previous bin (do this first, so it does not  get move under us
			int					hash		= key.getKeyForBin();
			int					binIndex	= hash % m_binCountPrevious;
			LocalObjectBin bin			= m_binList.get(binIndex);
			bin.deleteEntry(key);
		}

		// Remove it from the current bin
		int hash = key.getKeyForBin();
		int binIndex = hash % m_binCountCurrent;
		LocalObjectBin bin = m_binList.get(binIndex);
		bin.deleteEntry(key);
	}

	public long getItemCount()
	{
		long					count		= 0;
		for (int i = 0; i < m_binList.length(); i++)
		{
			LocalObjectBin bin		= m_binList.get(i);
			count += bin.getItemCount();
		}
		return count;
	}
	public int getBinCountCurrent()
	{
		return m_binCountCurrent;
	}


	public void flush(long nowMS, long flushStartMS, long durationMS)
	{
		long					perBinMS	= durationMS / m_binList.length();
		for (int i = 0; i < m_binList.length(); i++)
		{
			// This is necessary to avoid rounding errors if time/bin is small and fractional (e.g. 0.75 or 1.5)
			long			startTimeMS		= flushStartMS + (i * perBinMS);
			LocalObjectBin bin				= m_binList.get(i);
			bin.flush(nowMS, startTimeMS, perBinMS);
		}
	}

	public void upgradeClass(String classNameFrom, LocalObjectFactoryWrapper factory) throws BasicException
	{
		// By definition, if the read lock is available, m_bucketCountCurrent allows the walk through all the buckets.
		for (int i = 0; i < m_binCountCurrent; i++)
		{
			m_binList.get(i).upgradeClass(classNameFrom, factory);
		}
	}

	public void doMaintenance(MaintenanceRequest maintenanceRequest, MaintenanceResponse maintenanceResponse)
	{
		maintenanceResponse.incrementBinCount(m_binList.length());

		for (int i = 0; i < m_binList.length(); i++)
		{
			LocalObjectBin bin		= m_binList.get(i);
			bin.doMaintenance(maintenanceRequest, maintenanceResponse);
		}
	}

	@Override
	public String toString()
	{
		return String.format("Bucket %s: %,d bins; %,d items", m_bucketName, m_binCountCurrent, getItemCount());
	}
}
