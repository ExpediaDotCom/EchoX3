/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.cache;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;

import com.expedia.echox3.basics.collection.simple.CopyOnSizeArray;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.thread.ThreadSchedule;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.misc.PrimeNumbers;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.internal.store.counter.ItemCounterFamily;
import com.expedia.echox3.internal.store.wrapper.ObjectWrapper;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.internal.store.wrapper.TrackingObjects;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class LocalObjectCache implements LocalObjectCacheMBean, Closeable
{
	public enum CacheMode
	{
		Configuration,
		InitialLoad,
		Nominal,
		Maintenance,
		Eviction,
		Flush,
		Resize,
		Transmit,
		Receive
	}

	private static final BasicLogger LOGGER				= new BasicLogger(LocalObjectCache.class);

	private final ObjectPool<ObjectKey>				m_keyPool;

	// m_lock locks the bucket list, needs writeLock only when modifying the list.
	private volatile CacheMode						m_cacheMode			= CacheMode.Configuration;
	private final ObjectCacheConfiguration			m_configuration;
	private final TrackingObjects					m_trackingObjects;

	private CopyOnSizeArray<LocalObjectBucket>		m_bucketList;
	private volatile int							m_bucketCountCurrent;
	private volatile int							m_bucketCountPrevious			= 0;

	private final StringGroup						m_mbeanName;
	private final MaintenanceThread					m_maintenanceThread;

	private boolean									m_isSimpleCache					= false;
	private final LocalObjectFactoryWrapper m_factoryWrapper;

	public LocalObjectCache(ObjectCacheConfiguration configuration)
	{
		m_configuration = configuration;
		m_trackingObjects = new TrackingObjects(configuration.getCacheName());

		String[]		nameList		= new String[2];
		nameList[0] = ObjectKey.class.getSimpleName();
		nameList[1] = m_configuration.getCacheName();
		m_keyPool	= new ObjectPool<>(new StringGroup(nameList), ObjectKey::new);

		m_factoryWrapper = new LocalObjectFactoryWrapper(m_configuration);
		m_factoryWrapper.updateConfiguration();
		m_isSimpleCache = m_configuration.isSimpleCache();

		m_bucketList = new CopyOnSizeArray<>(1);
		m_bucketList.set(0, createBucket(0));
		m_bucketCountCurrent = 1;
		getLogger().info(BasicEvent.EVENT_CACHE_CREATE,
				"Creating cache %s.", configuration.getCacheName());

		m_maintenanceThread = new MaintenanceThread(this);

		m_mbeanName = new StringGroup(new String[]
											{ ItemCounterFamily.COUNTER_NAME_PREFIX, configuration.getCacheName() });
		BasicTools.registerMBean(this, null, m_mbeanName);
	}

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 */
	@Override
	public void close()
	{
		getLogger().info(BasicEvent.EVENT_CACHE_CLOSE,
				"Closing cache %s.", getConfiguration().getCacheName());
		flush(0);
		m_keyPool.release();
		m_maintenanceThread.terminate();
		m_factoryWrapper.close();
		getTrackingObjects().close();
		BasicSerial.flushSerial(getCacheName());
		BasicTools.unregisterMBean(null, m_mbeanName);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}
	public final ObjectCacheConfiguration getConfiguration()
	{
		return m_configuration;
	}
	@Override
	public Map<String, String> getConfigurationMap()
	{
		return getConfiguration().getSettingMap();
	}
	public final String getCacheName()
	{
		return getConfiguration().getCacheName();
	}

	public final TrackingObjects getTrackingObjects()
	{
		return m_trackingObjects;
	}

/*
	public final LockCounterFamily getBucketLockCounter()
	{
		return getTrackingObjects().getBucketListLockCounter();
	}
	public final LockCounterFamily getBinLockCounter()
	{
		return getTrackingObjects().getBinListLockCounter();
	}
	public final LockCounterFamily getItemLockCounter()
	{
		return getTrackingObjects().getItemLockCounter();
	}
*/
	public final ItemCounterFamily getCounterFamily()
	{
		return getTrackingObjects().getCounterFamily();
	}

	public int getBucketCountCurrent()
	{
		return m_bucketCountCurrent;
	}

	public LocalObjectFactoryWrapper getFactoryWrapper()
	{
		return m_factoryWrapper;
	}

	/*
		NOTE:	The bucket count can only be set as part of the INITIAL setConfiguration
				OR as a setBucketCount().
				It can NOT be changed with a setConfiguration.
	 */
	public void updateConfiguration(ObjectCacheConfiguration configuration)
	{
		// Deal with the name...
		if (!configuration.getCacheName().equals(getCacheName()))
		{
			getLogger().warn(BasicEvent.EVENT_INCOMPATIBLE_CACHE,
					"Unsupported attempt to change the name of a cache from %s to %s after its creation.",
					getCacheName(), configuration.getCacheName());
			return;
		}

		m_configuration.updateConfiguration(configuration);

		// Deal with the factory ...
		getFactoryWrapper().updateConfiguration();
		m_isSimpleCache = m_configuration.isSimpleCache();

		// Tell the maintenance thread to do the required update...
		//	- Update its schedule
		//	- Tell every object in the cache about the new configuration
		m_maintenanceThread.markConfigurationDirty();
	}

	@Override
	public String getFactoryClassName()
	{
		return getConfiguration().getFactoryClassName();
	}

	@Override
	public String getMaintenancePeriod()
	{
		return TimeUnits.formatMS(getConfiguration().getMaintenancePeriodMS());
	}

	@Override
	public long getMaintenancePeriodMS()
	{
		return getConfiguration().getMaintenancePeriodMS();
	}

	public CacheMode getCacheMode()
	{
		return m_cacheMode;
	}

	private void setCacheMode(CacheMode cacheMode)
	{
		// TODO Manage: Wait when needed
		m_cacheMode = cacheMode;
	}

	public CopyOnSizeArray<LocalObjectBucket> getBucketList()
	{
		return m_bucketList;
	}

	/**
	 * The increase in the number of buckets in the cache is always done by
	 * multiplying the number of buckets by some integer. This ensures that
	 * the items that need to move from one bucket to another (newly created) one
	 * will continue to reside on the same server.
	 *
	 * Example, there are 4 buckets (0, 1, 2, 3), and this server holds bucket 2.
	 * Multiply the number of buckets by 3. There are now 12 buckets (0, 1, 2, ..., 11)
	 * The buckets on this server (there should be 3) are now 2, 6 and 10.
	 *
	 * @param multiplicationFactor	Integer multiplier to the number of buckets
	 */
	public void multiplyBucketCount(int multiplicationFactor)
	{
		// TODO Manage cache mode...
		setCacheMode(CacheMode.Resize);

		// Create newly needed buckets
		int		bucketCountPrevious		= m_bucketList.length();
		int		bucketCountCurrent		= m_bucketCountPrevious * multiplicationFactor;
		m_bucketList.resize(bucketCountCurrent);
		m_bucketCountPrevious	= bucketCountPrevious;
		m_bucketCountCurrent	= bucketCountCurrent;

		long					t1				= System.nanoTime();
		for (int iMult = 1; iMult < multiplicationFactor; iMult++)		// Note the start at 1, not 0
		{
			for (int iBucket = 0; iBucket < m_bucketCountPrevious; iBucket++)
			{
				// This is a new bucket to create, only if there is a corresponding ACTIVE bucket
				// in the original bucket list
				if (null != m_bucketList.get(iBucket))
				{
					int		bucketIndex		= (iMult * m_bucketCountPrevious) + iBucket;
					m_bucketList.set(bucketIndex, createBucket(bucketIndex));
				}
			}
		}

		// Move the items from the previous list of buckets to the newly created buckets as needed...
		long					itemCount		= 0;
		for (int iBucket = 0; iBucket < m_bucketCountPrevious; iBucket++)
		{
			LocalObjectBucket bucket		= m_bucketList.get(iBucket);
			itemCount += bucket.moveItems(this);
		}

		long				t2				= System.nanoTime();
		long				durationNS		= t2 - t1;
		getLogger().info(BasicEvent.EVENT_BUCKET_COUNT_CHANGE,
				"Cache %s (Moved %,d items) multiplied by %,d from %,d buckets to %,d buckets in %s",
				getCacheName(), itemCount, multiplicationFactor, m_bucketCountPrevious, m_bucketCountCurrent,
				TimeUnits.formatNS(durationNS));

		setCacheMode(CacheMode.Nominal);
		m_bucketCountPrevious = 0;		// All items are where they belong
	}
	private LocalObjectBucket createBucket(int bucketNumber)
	{
		return new LocalObjectBucket(getConfiguration(), getTrackingObjects(), bucketNumber);
	}

	private ObjectWrapper getEntry(ObjectKey key, LocalObjectFactoryWrapper factory) throws BasicException
	{
		ObjectWrapper			entry		= null;

		int					hash			= key.getKeyForBucket();
		if (0 != m_bucketCountPrevious)
		{
			int					bucketIndex		= hash % m_bucketCountPrevious;
			LocalObjectBucket bucket			= m_bucketList.get(bucketIndex);
			if (null != bucket)
			{
				entry = bucket.getEntry(key, null);		// Don't create in the previous bucket
			}
		}
		if (null == entry)
		{
			int					bucketIndex		= hash % m_bucketCountCurrent;
			LocalObjectBucket bucket			= m_bucketList.get(bucketIndex);
			entry = bucket.getEntry(key, factory);
		}

		return entry;
	}
	private void deleteEntry(ObjectKey key) throws BasicException
	{
		if (0 != m_bucketCountPrevious)
		{
			int					hash			= key.getKeyForBucket();
			int					bucketIndex		= hash % m_bucketCountCurrent;
			LocalObjectBucket bucket			= m_bucketList.get(bucketIndex);
			bucket.deleteEntry(key);
		}

		int					hash			= key.getKeyForBucket();
		int					bucketIndex		= hash % m_bucketCountCurrent;
		LocalObjectBucket bucket			= m_bucketList.get(bucketIndex);
		bucket.deleteEntry(key);
	}

	@Override
	public void flush(int durationMS)
	{
		getLogger().info(BasicEvent.EVENT_FLUSH_BEGIN,
				String.format("Beginning flush(%s, %s)",
						getCacheName(), TimeUnits.formatMS(durationMS)));

		BasicSerial.flushSerial(getCacheName());

		// Count the active buckets (the one that hold data)
		int			activeBucketCount		= 0;
		for (int i = 0; i < m_bucketCountCurrent; i++)
		{
			if (null != m_bucketList.get(i))
			{
				activeBucketCount++;
			}
		}

		int			perBucketMS				= durationMS / activeBucketCount;
		long		nowMS					= WallClock.getCurrentTimeMS();
		int			bucketIndex				= 0;		// Among active buckets
		for (int i = 0; i < m_bucketList.length(); i++)
		{
			LocalObjectBucket bucket	= m_bucketList.get(i);
			if (null != bucket)
			{
				// Always flush based on time of last WRITE operation.
				// This is to avoid flushing an entry written after the flush has started.
				long		timeBeginFlush			= nowMS + (bucketIndex * perBucketMS);
				bucket.flush(nowMS, timeBeginFlush, perBucketMS);
			}
		}
	}

	@SuppressWarnings("unused")
	public void upgradeClass(String classNameFrom, LocalObjectFactoryWrapper factory) throws BasicException
	{
		// By definition, if the read lock is available, m_bucketCountCurrent allows the walk through all the buckets.
		for (int i = 0; i < m_bucketCountCurrent; i++)
		{
			m_bucketList.get(i).upgradeClass(classNameFrom, factory);
		}
	}


	public void writeOnly(byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		ObjectKey key			= m_keyPool.get();
		key.setKeyBytes(keyBytes);

		try
		{
			// Do not pass a factory to create an object if the request is empty
			LocalObjectFactoryWrapper factory		= null == requestBytes ? null : getFactoryWrapper();
			ObjectWrapper				entry		= getEntry(key, factory);
			if (null != entry)
			{
				processWriteOnly(entry, requestBytes);
				ICacheObject cacheObject		= entry.getTrellisObject();
				if (cacheObject.canDelete())
				{
					deleteEntry(key);
				}
			}
		}
		finally
		{
			key.release();
			getCounterFamily().recordWrite();		// Record even if object was not found!
		}
	}
	private void processWriteOnly(ObjectWrapper wrapper, byte[] requestBytes) throws BasicException
	{
		// **********
		// Optimization for the SimpleCache object mode!!
		// **********
		Serializable			request			= m_isSimpleCache
				? requestBytes : BasicSerial.toObject(getCacheName(), requestBytes);
		ICacheObject cacheObject		= wrapper.getTrellisObject();

		IOperationContext	context			= wrapper.getLock().lockWrite();
		boolean				isSuccess		= false;
		try
		{
			cacheObject.writeOnly(request);
			isSuccess = true;
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_CACHE_OBJECT_EXCEPTION_WRITE, exception,
					"Unexpected exception processing writeOnly() on object %s", cacheObject.toString());
		}
		finally
		{
			wrapper.getLock().unlockWrite(context, isSuccess);
		}
	}

	public byte[] readOnly(byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		ObjectKey		key			= m_keyPool.get();
		key.setKeyBytes(keyBytes);

		byte[]				response;
		try
		{
			ObjectWrapper		wrapper		= getEntry(key, null);
			boolean				isFound		= null != wrapper;
			getCounterFamily().recordHit(isFound);
			if (isFound)
			{
				response = processReadOnly(wrapper, requestBytes);
			}
			else
			{
				response = null;
			}
		}
		finally
		{
			key.release();
			getCounterFamily().recordRead();		// Record even if object was not found!
		}

		return response;
	}
	private byte[] processReadOnly(ObjectWrapper wrapper, byte[] requestBytes) throws BasicException
	{
		Serializable			request			= BasicSerial.toObject(getCacheName(), requestBytes);
		ICacheObject cacheObject		= wrapper.getTrellisObject();

		byte[]					responseBytes;
		IOperationContext		context			= wrapper.getLock().lockRead();
		boolean					isSuccess		= false;
		try
		{
			Serializable		response		= cacheObject.readOnly(request);

			// **********
			// Optimization for the SimpleCache object mode!!1
			// **********
			responseBytes = m_isSimpleCache
					? (byte[]) response : BasicSerial.toBytes(getCacheName(), response);
			isSuccess = true;
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_CACHE_OBJECT_EXCEPTION_READ, exception,
					"Unexpected exception processing readOnly() on object %s", cacheObject.toString());
		}
		finally
		{
			wrapper.getLock().unlockRead(context, isSuccess);
		}

		return responseBytes;
	}

	@Override
	public long getItemCount()
	{
		long					count		= 0;

		for (int i = 0; i < m_bucketCountCurrent; i++)
		{
			if (null != m_bucketList.get(i))
			{
				count += m_bucketList.get(i).getItemCount();
			}
		}

		return count;
	}

	private static class MaintenanceThread extends AbstractScheduledThread
	{
		private static final int				OFFSET_DELTA_MS			= 13;
		private static final AtomicInteger		SCHEDULE_OFFSET_MS		= new AtomicInteger(OFFSET_DELTA_MS);

		private final int						m_offsetMS				= SCHEDULE_OFFSET_MS.getAndAdd(OFFSET_DELTA_MS);
		private final LocalObjectCache m_cache;
		private AtomicInteger					m_memoryLevel			= new AtomicInteger(100);
		private AtomicBoolean					m_isConfigDirty			= new AtomicBoolean(false);

		private final MaintenanceRequest		m_maintenanceRequest	= new MaintenanceRequest();
		private final MaintenanceResponse		m_maintenanceResponse	= new MaintenanceResponse();

		public MaintenanceThread(LocalObjectCache cache)
		{
			super(false);

			m_cache = cache;
			setName(String.format("Maintenance(%s)", getCache().getCacheName()));
			setDaemon(true);
			start();
		}

		public LocalObjectCache getCache()
		{
			return m_cache;
		}

		// Will be called when global eviction is needed
		public void setMemoryLevel(int memoryLevel)
		{
			m_memoryLevel.set(memoryLevel);
			requestImmediateRun();
		}
		public void markConfigurationDirty()
		{
			m_isConfigDirty.set(true);
			requestImmediateRun();
		}

		@Override
		public void updateConfiguration()
		{
			//  Do NOT let the AbstractScheduledThread read the ThreadSchedule from configuration!
			long			periodMS		= getCache().getConfiguration().getMaintenancePeriodMS();
			if (getThreadSchedule().getPeriodMS() != periodMS)
			{
				ThreadSchedule		schedule		= new ThreadSchedule(true, periodMS, m_offsetMS);
				getLogger().info(BasicEvent.EVENT_CACHE_MAINTENANCE_CHANGE,
						"Cache %s: Maintenance schedule changed from %s to %s.",
						getCache().getCacheName(),
						getThreadSchedule().toString(),
						schedule.toString());
				setSchedule(schedule);
			}
		}

		@Override
		protected void runOnce(long timeMS)
		{
			int			memoryLevel		= m_memoryLevel.getAndSet(100);
			doMaintenance(memoryLevel);
			doResizeBuckets();
		}

		private void doMaintenance(int memoryLevel)
		{
			long			now							= WallClock.getCurrentTimeMS();
			long			timeBeginNS					= System.nanoTime();
			boolean			isConfigurationDirty		= m_isConfigDirty.getAndSet(false);		// NOPMD
			getCache().setCacheMode(CacheMode.Maintenance);
			m_maintenanceRequest.set(now, memoryLevel, isConfigurationDirty);
			m_maintenanceResponse.clear();
			int				bucketCount					= 0;
			try
			{
				for (int i = 0; i < m_cache.m_bucketList.length(); i++)
				{
					LocalObjectBucket bucket	= m_cache.m_bucketList.get(i);
					if (null != bucket)
					{
						bucketCount++;
						bucket.doMaintenance(m_maintenanceRequest, m_maintenanceResponse);
					}
				}
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_TODO, exception, "UnexpectedError");
			}
			finally
			{
				getCache().setCacheMode(CacheMode.Nominal);
			}
			long			timeDoneNS					= System.nanoTime();
			long			durationNS					= timeDoneNS - timeBeginNS;

			Level			level						=
					0 == m_maintenanceResponse.getRemovedCount() ? Level.DEBUG : Level.INFO;
			LocalObjectCache.getLogger().log(level, BasicEvent.EVENT_CACHE_MAINTENANCE_PASS,
					"Maintenance on %s (@ Memory = %,d %%) resulted in %,d object kept (%,d %s kept);"
							+ " %,d removed in %s from %,d buckets/%,d bins for %s/bin @ average %,d object/bin.",
					getCache().getCacheName(),
					m_maintenanceRequest.getMemoryLevelPercent(),
					m_maintenanceResponse.m_keptCount,
					m_maintenanceResponse.getSize(), getCache().getConfiguration().getSizeUnits(),
					m_maintenanceResponse.getRemovedCount(),
					TimeUnits.formatNS(durationNS),
					bucketCount, m_maintenanceResponse.getBinCount(),
					TimeUnits.formatNS(durationNS / m_maintenanceResponse.getBinCount()),
					m_maintenanceResponse.getKeptCount() / m_maintenanceResponse.getBinCount()
					);


			long		sizeMax			= m_cache.getConfiguration().getSizeMax();
			if (0 < sizeMax && m_maintenanceResponse.getSize() > sizeMax)
			{
				doMaintenance(memoryLevel - 5);
			}

		}

		private void doResizeBuckets()
		{
			getCache().setCacheMode(CacheMode.Resize);
			try
			{
				// As needed, adjust the number of bins in each bucket, as needed...
				for (int i = 0; i < m_cache.m_bucketList.length(); i++)
				{
					LocalObjectBucket bucket	= m_cache.m_bucketList.get(i);
					if (null == bucket)
					{
						// This bucket is not on this server
						continue;
					}
					long		itemCount		= bucket.getItemCount();
					int			binCount		= bucket.getBinCountCurrent();
					int			itemPerBin		= (int) (itemCount / binCount);
					if (itemPerBin > getCache().getConfiguration().getBinItemMax())
					{
						// The bucket has too many items per bin, resize it
						// Note the new size is  dependent on the CURRENT number of items in the bucket.
						// This makes the new number of buckets variable and not a constant series.
						int		itemPerBinTarget	= getCache().getConfiguration().getBinItemMax() / 7;
						int		binCountTarget		= (int) itemCount / itemPerBinTarget;
						binCount = PrimeNumbers.nextPrime(binCountTarget);
						bucket.resize(binCount);
					}
				}
			}
			finally
			{
				getCache().setCacheMode(CacheMode.Nominal);
			}
		}
	}

	public static class MaintenanceRequest
	{
		private long		m_timeNowMS;
		private int			m_memoryLevelPercent;
		private boolean		m_isDirty;

		public void set(long timeNowMS, int memoryLevelPercent, boolean isDirty)
		{
			m_timeNowMS = timeNowMS;
			m_memoryLevelPercent = memoryLevelPercent;
			m_isDirty = isDirty;
		}

		public long getTimeNowMS()
		{
			return m_timeNowMS;
		}

		public int getMemoryLevelPercent()
		{
			return m_memoryLevelPercent;
		}

		public boolean isDirty()
		{
			return m_isDirty;
		}
	}
	public static class MaintenanceResponse
	{
		private int			m_binCount;
		private long		m_keptCount;
		private long		m_removedCount;
		private long		m_size;


		public void clear()
		{
			m_binCount		= 0;
			m_keptCount		= 0;
			m_removedCount	= 0;
			m_size			= 0;
		}

		public void incrementBinCount(long delta)
		{
			m_binCount += delta;
		}
		public void incrementKeptCount(long delta)
		{
			m_keptCount += delta;
		}
		public void incrementRemovedCount(long delta)
		{
			m_removedCount += delta;
		}
		public void incrementSize(long delta)
		{
			m_size += delta;
		}

		public int getBinCount()
		{
			return m_binCount;
		}

		public long getKeptCount()
		{
			return m_keptCount;
		}

		public long getRemovedCount()
		{
			return m_removedCount;
		}

		public long getSize()
		{
			return m_size;
		}
	}
}
