/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.internal.store.counter.ItemCounterFamily;
import com.expedia.echox3.internal.store.wrapper.ObjectWrapper;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.internal.store.wrapper.TrackingObjects;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceRequest;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceResponse;

public class LocalObjectBin
{
	private static final BasicLogger LOGGER			= new BasicLogger(LocalObjectBin.class);

	private final ObjectCacheConfiguration m_configuration;
	private final TrackingObjects						m_trackingObjects;
	private final String								m_binName;
	private final AbstractReadWriteLock					m_mapLock;
	private final Map<ObjectKey, ObjectWrapper>			m_entryMap		= new HashMap<>();

	public LocalObjectBin(
			ObjectCacheConfiguration configuration, TrackingObjects trackingObjects, String bucketName, int number)
	{
		m_configuration = configuration;
		m_trackingObjects = trackingObjects;
		m_binName = String.format("%s.Bin-%4d", bucketName, number);
		m_mapLock = AbstractReadWriteLock.createReadWriteLock(getTrackingObjects().getBinMapLockCounter());
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
	public final ItemCounterFamily getCounterFamily()
	{
		return getTrackingObjects().getCounterFamily();
	}

	protected AbstractReadWriteLock getMapLock()
	{
		return m_mapLock;
	}

	protected Map<ObjectKey, ObjectWrapper> getEntryMap()
	{
		return m_entryMap;
	}
	public int getItemCount()
	{
		return getEntryMap().size();
	}

	public ObjectWrapper getEntry(ObjectKey key, LocalObjectFactoryWrapper factory) throws BasicException
	{
		ObjectWrapper		entry;

		// Must use synchronized here, because this is always a possible read/write operation.
		IOperationContext		readContext		= getMapLock().lockRead();
		entry = getEntryMap().get(key);
		getMapLock().unlockRead(readContext, true);
		if (null == entry)
 		{
			// Create the object outside any lock, then write it SAFELY inside the write lock.
			// This ensures safety and minimal lock duration at the cost of possibly creating the same object
			// multiple times in very rare cases.
			// However, the cost is small and the algorithm guarantees the object is written only once.
			ICacheObject cacheObject		= null;
			if (null != factory)	// e.g. null == factory on a read operation
			{
				cacheObject = factory.createObject(key);
			}
			if (null != cacheObject)
			{
				// The input key is a TEMPORARY one, create a permanent one for the map.
				// Package the gridObject into a HiperItemEntry
				entry = new ObjectWrapper(key, cacheObject);
				entry.createLock(getTrackingObjects().getItemLockCounter());

				IOperationContext		writeContext		= getMapLock().lockWrite();
				ObjectWrapper			entryPrev			= getEntryMap().get(entry);
				if (null == entryPrev)		// If the entry is still not present (i.e. not written by another thread)
				{
					// Was still not there, a new entry has been created
					getEntryMap().put(entry, entry);
					getCounterFamily().recordCreate(entry);
				}
				else
				{
					// is present... was created by another thread between unlockRead and lockWrite
					// Use the existing entry, throw away the superfluous entry created by this thread.
					entry = entryPrev;
				}
				getMapLock().unlockWrite(writeContext, true);
			}
		}
		return entry;
	}

	/**
	 * Used when changing the number of buckets (or bins?) to move an item into this bin.
	 *
	 * @param objectWrapper		object to move into this bin
	 */
	// NOTE: No counter operation as the item stays in the same cache.
	public void moveEntryInto(ObjectWrapper objectWrapper)
	{
		IOperationContext		writeContext		= getMapLock().lockWrite();
		getEntryMap().put(objectWrapper, objectWrapper);
		getMapLock().unlockWrite(writeContext, true);
	}

	public void deleteEntry(ObjectKey key)
	{
		IOperationContext		writeContext		= getMapLock().lockWrite();

		ObjectWrapper			wrapper				= getEntryMap().remove(key);
		if (null != wrapper)
		{
			// Only if an entry was actually removed.
			// Can be called with no entry deleted when resizing bucketList or binList.
			getCounterFamily().recordRemove(wrapper);
		}
		// Do NOT close the lock on the Wrapper, as it is associated with a global ItemCounterFamily
		// Closing the lock would close the ItemCounterFamily.
		// The lock object itself is released as garbage when the ObjectWrapper (extends ObjectKey) is released.

		getMapLock().unlockWrite(writeContext, true);
	}

	public void flush(long nowMS, long flushStartMS, long durationMS)
	{
		if (0 == durationMS)
		{
			flushNow();
		}
		else
		{
			flushSlow(nowMS, flushStartMS, durationMS);
		}
	}
	public void flushNow()
	{
		IOperationContext							context		= getMapLock().lockWrite();

		for (Entry<ObjectKey, ObjectWrapper> entry : m_entryMap.entrySet())
		{
			getCounterFamily().recordRemove(entry.getKey());
			getCounterFamily().recordFlush();
		}
		m_entryMap.clear();
		getMapLock().unlockWrite(context, true);
	}
	public void flushSlow(long nowMS, long flushStartMS, long durationMS)
	{
		IOperationContext							context		= getMapLock().lockWrite();

		int											count		= m_entryMap.size();
		Iterator<Entry<ObjectKey, ObjectWrapper>>	iterator	= m_entryMap.entrySet().iterator();
		int											iItem		= 0;
		while (iterator.hasNext())
		{
			Entry<ObjectKey, ObjectWrapper>		entry			= iterator.next();
			long								timeItemMS		= flushStartMS + ((durationMS * iItem++) / count);
			ObjectWrapper						wrapper			= entry.getValue();
			ICacheObject trellisObject	= wrapper.getTrellisObject();
			trellisObject.flush(nowMS, timeItemMS);
			if (trellisObject.canDelete())
			{
				iterator.remove();
				ObjectKey						key				= entry.getKey();
				getCounterFamily().recordRemove(key);
				getCounterFamily().recordFlush();
			}
		}

		getMapLock().unlockWrite(context, true);
	}

	public void upgradeClass(String classNameFrom, LocalObjectFactoryWrapper factory) throws BasicException
	{
		IOperationContext		context		= getMapLock().lockWrite();

		// By definition, if the read lock is available, m_bucketCountCurrent allows the walk through all the buckets.
		for (Map.Entry<ObjectKey, ObjectWrapper> entry : m_entryMap.entrySet())
		{
			ObjectWrapper			wrapper			= entry.getValue();
			ICacheObject cacheObject		= wrapper.getTrellisObject();
			// Compare class name, as the same classNameFrom "may" exist in different ClassLoader
			// and potentially show-up as different Class. It would still show-up as the same class name.
			if (classNameFrom.equals(cacheObject.getClass().getName()))
			{
				// Need to upgrade the object...
				try
				{
					ICacheObject upgradedObject		= factory.createObject(wrapper);
					upgradedObject.upgradeFrom(cacheObject);
					wrapper.setTrellisObject(upgradedObject);
				}
				catch (BasicException e)
				{
					throw new BasicException(BasicEvent.EVENT_CACHE_OBJECT_EXCEPTION_UPGRADE, e,
							"Failed to upgrade object %s", cacheObject.toString());
				}

			}
		}

		getMapLock().unlockWrite(context, true);
	}


	public void doMaintenance(MaintenanceRequest maintenanceRequest, MaintenanceResponse maintenanceResponse)
	{
		long					keptCount				= 0;
		long					removedCount			= 0;
		long					size					= 0;

		IOperationContext		mapContext				= getMapLock().lockWrite();
		boolean					isSuccessMap			= true;
		try
		{
			Iterator<Entry<ObjectKey, ObjectWrapper>> iterator		= getEntryMap().entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<ObjectKey, ObjectWrapper>		entry				= iterator.next();
				ObjectWrapper							objectWrapper		= entry.getValue();
				ICacheObject cacheObject			= objectWrapper.getTrellisObject();
				IOperationContext						objectContext		= objectWrapper.getLock().lockWrite();
				boolean									isSuccessObject		= false;
				try
				{
					if (maintenanceRequest.isDirty())
					{
						cacheObject.updateConfiguration(getConfiguration());
					}

					cacheObject.doMaintenance(
							maintenanceRequest.getTimeNowMS(), maintenanceRequest.getMemoryLevelPercent());
					if (cacheObject.canDelete())
					{
						iterator.remove();
						getCounterFamily().recordRemove(entry.getKey());
						getCounterFamily().recordExpire();
						removedCount++;
					}
					else
					{
						keptCount++;
						size += cacheObject.getSize();
					}
					isSuccessObject = true;
				}
				catch (Exception exception)
				{
					getLogger().warn(BasicEvent.EVENT_CACHE_OBJECT_EXCEPTION_MAINTENANCE, exception,
							"Unexpected exception processing doMaintenance() on object %s", cacheObject.toString());
				}
				finally
				{
					objectWrapper.getLock().unlockWrite(objectContext, isSuccessObject);
					isSuccessMap &= isSuccessObject;
				}
			}
		}
		finally
		{
			getMapLock().unlockWrite(mapContext, isSuccessMap);
		}

		maintenanceResponse.incrementKeptCount(keptCount);
		maintenanceResponse.incrementRemovedCount(removedCount);
		maintenanceResponse.incrementSize(size);
	}

	@Override
	public String toString()
	{
		return String.format("Bin %s: %,d items", m_binName, m_entryMap.size());
	}
}
