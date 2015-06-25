/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.simplecache;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IItemSizeCounter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.internal.store.counter.ItemCounterFamily;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class SimpleCacheStatusHolder
{
	public static final String				SETTING_NAME_TTL_NUMBER		= "TTLNumber";
	public static final String				SETTING_NAME_TTL_UNITS		= "TTLUnits";
	public static final String				SETTING_NAME_TTL_TYPE		= "TTLType";

//	private static final TimeType[]			TIME_TYPE_LIST				= TimeType.values();
	private static final TimeType			TIME_TYPE_DEFAULT			= TimeType.TimeWrite;

	public enum TimeType
	{
		NotSet,
		TimeWrite,
		TimeRead
	}

	private String					m_name;
	private TimeType				m_timeType			= TimeType.NotSet;
	private long					m_ageMaxMS			= 15 * 1000;
	private IItemSizeCounter		m_valueSizeCounter;

	public void readConfiguration(ObjectCacheConfiguration configuration)
	{
		if (null == m_valueSizeCounter)
		{
			m_name = configuration.getCacheName();

			String[]		nameList		= new String[3];
			nameList[0] = ItemCounterFamily.COUNTER_NAME_PREFIX;
			nameList[1] = m_name;
			nameList[2] = "ValueSize";
			m_valueSizeCounter = CounterFactory.getInstance().getItemSizeCounter(new StringGroup(nameList),
					"Total memory used by the keys in the cache.");
		}

		String		timeTypeText	= configuration.getSettingAsString(
																SETTING_NAME_TTL_TYPE, TimeType.TimeWrite.name());
		TimeType	timeType;
		try
		{
			timeType = TimeType.valueOf(timeTypeText);
		}
		catch (Exception exception)
		{
			timeType = TIME_TYPE_DEFAULT;
		}

		long		ageMaxNumber	= configuration.getSettingAsLong(SETTING_NAME_TTL_NUMBER, 5);
		String		ageMaxUnits		= configuration.getSettingAsString(SETTING_NAME_TTL_UNITS, "Min");

		long		ageMax			= TimeUnits.getTimeMS(ageMaxNumber, ageMaxUnits);

		if (ageMax != m_ageMaxMS || !timeType.equals(m_timeType))
		{
			LocalObjectCache.getLogger().info(BasicEvent.EVENT_CACHE_EXPIRATION_CHANGE,
					"Cache %s: Expiration changed from %s @ %s to %s @ %s.",
					m_name,
					m_timeType.toString(), TimeUnits.formatMS(m_ageMaxMS),
					timeType.toString(), TimeUnits.formatMS(ageMax));
			m_ageMaxMS		= ageMax;
			m_timeType		= timeType;
		}
	}

	public TimeType getTimeType()
	{
		return m_timeType;
	}

	public long getAgeMaxMS()
	{
		return m_ageMaxMS;
	}

	public IItemSizeCounter getValueSizeCounter()
	{
		return m_valueSizeCounter;
	}

	public void close()
	{
		m_valueSizeCounter.close();
	}
}
