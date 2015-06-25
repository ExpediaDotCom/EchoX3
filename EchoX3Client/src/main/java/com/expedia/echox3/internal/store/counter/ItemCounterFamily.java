/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.counter;


import java.io.Closeable;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram.Precision;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.BaseCounter;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory.CounterRange;
import com.expedia.echox3.basics.monitoring.counter.IIncrementCounter;
import com.expedia.echox3.basics.monitoring.counter.IItemSizeCounter;
import com.expedia.echox3.basics.monitoring.counter.IValueCounter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.internal.store.wrapper.ObjectWrapper;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;

public class ItemCounterFamily implements Closeable
{
	public static final String				COUNTER_NAME_PREFIX				= "CacheServer";
	protected static final CounterFactory	COUNTER_FACTORY					= CounterFactory.getInstance();

	private static final BasicLogger		LOGGER							= new BasicLogger(LocalObjectCache.class);

	// For "public" settings, use the simple class name.
	private static final String				SETTING_PREFIX					= ItemCounterFamily.class.getSimpleName();

	protected final String[]			m_nameList;

	private final IIncrementCounter		m_itemCount;
	private final IIncrementCounter		m_create;
	private final IItemSizeCounter		m_keySize;
	private final IIncrementCounter		m_write;
	private final IIncrementCounter		m_read;

	private final IIncrementCounter		m_expire;
	private final IIncrementCounter		m_evict;
	private final IIncrementCounter		m_flush;

	private final IValueCounter			m_hitRate;

	private boolean						m_isEnabled						= true;

	public ItemCounterFamily(String cacheName)
	{
		m_nameList = new String[3];

		m_nameList[0] = COUNTER_NAME_PREFIX;
		m_nameList[1] = cacheName;

		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);

		m_nameList[2] = "ItemCount";
		m_itemCount		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Number of items currently in the cache.");
		m_nameList[2] = "Create";
		m_create		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Number of items created (i.e. new) the cache.");
		m_nameList[2] = "KeySize";
		m_keySize		= COUNTER_FACTORY.getItemSizeCounter(new StringGroup(m_nameList),
												"Total memory used by the keys in the cache.");

		m_nameList[2] = "WriteCount";
		m_write		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Number of writeOnly() calls to the cache.");
		m_nameList[2] = "ReadCount";
		m_read		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Number of readOnly() calls to the cache.");

		m_nameList[2] = "Expiration";
		m_expire		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Count of items removed through expiration.");
		m_nameList[2] = "Eviction";
		m_evict		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Count of items removed through eviction (premature expiration).");
		m_nameList[2] = "Flush";
		m_flush		= COUNTER_FACTORY.getIncrementCounter(new StringGroup(m_nameList),
												"Count of items removed through a user flush request.");

		m_nameList[2] = "HitRate";
		m_hitRate		= COUNTER_FACTORY.getValueCounter(new StringGroup(m_nameList),
				Precision.Coarse, CounterRange.us,
				"Count of items removed through eviction (premature expiration).");
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	private String getCacheName()
	{
		return m_nameList[1];
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		String		nameIsEnabled		= String.format("%s.%s.%s",
				SETTING_PREFIX, getCacheName(), BaseCounter.SETTING_SUFFIX_IS_ENABLED);
		boolean		isEnabled			= ConfigurationManager.getInstance().getBoolean(nameIsEnabled, "true");

		if (isEnabled != m_isEnabled)
		{
			m_isEnabled = isEnabled;
			getLogger().info(BasicEvent.EVENT_CACHE_COUNTERS_STATUS,
					"ItemCache counters for cache %s are %s!", getCacheName(), m_isEnabled ? "enabled" : "disabled");
		}
	}

	public boolean isEnabled()
	{
		return m_isEnabled;
	}

	public void recordWrite()
	{
		if (isEnabled())
		{
			m_write.increment();
		}
	}
	public void recordRead()
	{
		if (isEnabled())
		{
			m_read.increment();
		}
	}

	public void recordCreate(ObjectWrapper key)
	{
		if (isEnabled())
		{
			m_itemCount.increment();
			m_create.increment();
			m_keySize.add(key.getKeySize());
		}
	}
	public void recordRemove(ObjectKey key)
	{
		if (isEnabled())
		{
			m_itemCount.decrement();
			m_keySize.remove(key.getKeySize());
		}
	}

	public void recordExpire()
	{
		if (isEnabled())
		{
			m_expire.increment();
		}
	}
	public void recordEvict()
	{
		if (isEnabled())
		{
			m_evict.increment();
		}
	}
	public void recordFlush()
	{
		if (isEnabled())
		{
			m_flush.increment();
		}
	}

	public void recordHit(boolean isHit)
	{
		if (isEnabled())
		{
			m_hitRate.record(isHit ? 1.0 : 0.0);
		}
	}

	@Override
	public void close()
	{
		m_itemCount.close();
		m_create.close();
		m_keySize.close();
		m_write.close();
		m_read.close();

		m_expire.close();
		m_evict.close();
		m_flush.close();

		m_hitRate.close();
	}
}
