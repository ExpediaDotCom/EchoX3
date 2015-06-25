/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.hash.IHashProvider;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheObjectFactory;

/**
 * Configuration of a named cache.
 *
 * While there is a lot in this class, users of EchoX3 should only concern themselves
 * with the family of public methods getSettingAs...().
 * These methods are available on the ObjectCacheConfiguration object
 * passed in the updateConfiguration() method in ICacheObject and IObjectFactory.
 * Note that the methods are optimized to avoid multiple parsing calls
 * if the same number setting is used for each object.
 */
public class ObjectCacheConfiguration
{
	private static final String		COMMA						= ",";
	private static final String		SETTING_NAME_MAINTENANCE	= "MaintenancePeriod";
	//CHECKSTYLE:OFF
	private static final String		SETTING_NAME_PERIOD_NUMBER	= SETTING_NAME_MAINTENANCE + ConfigurationManager.SETTING_NAME_NUMBER;
	private static final String		SETTING_NAME_PERIOD_UNITS	= SETTING_NAME_MAINTENANCE + ConfigurationManager.SETTING_NAME_UNITS;
	private static final String		SETTING_NAME_CLASS_NAME		= "FactoryClassName";
	//CHECKSTYLE:ON

	private static final String		SETTING_NAME_SIZE_MAX		= "SizeMax";
	private static final String		SETTING_NAME_SIZE_UNITS		= "SizeUnits";

	private static final String		SETTING_NAME_BIN_ITEM_MAX	= "BinItemMax";
	private static final String		SETTING_NAME_BIN_COUNT_MIN	= "BinCountMin";
	private static final String		SETTING_NAME_BIN_COUNT_MAX	= "BinCountMax";

	// The very basics of the cache definition
	private String					m_cacheName;
	private String					m_factoryClassName			= null;
	private long					m_maintenancePeriodMS		= 5 * 60 * 1000;		// 5 minutes

	// Values expected to change only very rarely; default are in the JAR; setting not exposed
	private int						m_binItemMax				= 700;
	private int						m_binCountMin				= 11;
	private int						m_binCountMax				= Integer.MAX_VALUE;

	private long					m_sizeMax					= 0;
	private String					m_sizeUnits					= "Units";

	// Cached items for the cached objects
	private volatile IConfigurationProvider		m_configurationProvider;
	private final Map<String, Integer>			m_intMap				= new HashMap<>();
	private final Map<String, Long>				m_longMap				= new HashMap<>();
	private final Map<String, Double>			m_doubleMap				= new HashMap<>();

	public ObjectCacheConfiguration(String cacheName, IConfigurationProvider provider)
	{
		m_cacheName = cacheName;
		m_configurationProvider = provider;
	}

	public String getCacheName()
	{
		return m_cacheName;
	}

	public IConfigurationProvider getConfigurationProvider()
	{
		return m_configurationProvider;
	}

	public void updateConfiguration(ObjectCacheConfiguration cacheConfiguration)
	{
		updateConfiguration(cacheConfiguration.m_configurationProvider);
	}
	public void updateConfiguration(IConfigurationProvider provider)
	{
		synchronized (this)
		{
			m_configurationProvider = provider;

			m_intMap.clear();
			m_longMap.clear();
			m_doubleMap.clear();
		}
	}

	public Map<String, String> getSettingMap()
	{
		return m_configurationProvider.getSettingMap();
	}

	public String getSettingAsString(String settingName, String defaultValue)
	{
		String		value		= m_configurationProvider.getSetting(settingName);
		if (null == value)
		{
			value = defaultValue;
		}
		return value;
	}
	public int getSettingAsInteger(String settingName, int defaultValue)
	{
		Integer		value		= m_intMap.get(settingName);
		if (null == value)
		{
			String		textValue		= getSettingAsString(settingName, Integer.toString(defaultValue));
			value = Integer.valueOf(textValue.replace(COMMA, "").trim());
			m_intMap.put(settingName, value);
		}
		return value;
	}
	public long getSettingAsLong(String settingName, long defaultValue)
	{
		Long		value		= m_longMap.get(settingName);
		if (null == value)
		{
			String		textValue		= getSettingAsString(settingName, Long.toString(defaultValue));
			value = Long.valueOf(textValue.replace(COMMA, "").trim());
			m_longMap.put(settingName, value);
		}
		return value;
	}
	public double getSettingAsDouble(String settingName, double defaultValue)
	{
		Double		value		= m_doubleMap.get(settingName);
		if (null == value)
		{
			String		textValue		= getSettingAsString(settingName, Double.toString(defaultValue));
			value = Double.valueOf(textValue);
			m_doubleMap.put(settingName, value);
		}
		return value;
	}

	public boolean isSimpleCache()
	{
		return SimpleCacheObjectFactory.class.getName().equals(getFactoryClassName());
	}
	public String getFactoryClassName()
	{
		m_factoryClassName = getSettingAsString(SETTING_NAME_CLASS_NAME, m_factoryClassName);
		return m_factoryClassName;
	}
/*
	protected void setFactoryClassName(String factoryClassName)
	{
		m_factoryClassName = factoryClassName;
	}
*/
	public int getBinCountMin()
	{
		m_binCountMin = getSettingAsInteger(SETTING_NAME_BIN_COUNT_MIN, m_binCountMin);
		return m_binCountMin;
	}

	public int getBinItemMax()
	{
		m_binItemMax = getSettingAsInteger(SETTING_NAME_BIN_ITEM_MAX, m_binItemMax);
		return m_binItemMax;
	}

	public int getBinCountMax()
	{
		m_binCountMax = getSettingAsInteger(SETTING_NAME_BIN_COUNT_MAX, m_binCountMax);
		return m_binCountMax;
	}

	public long getMaintenancePeriodMS()
	{
		long		periodNumber		= getSettingAsLong(SETTING_NAME_PERIOD_NUMBER, m_maintenancePeriodMS);
		String		periodUnits			= getSettingAsString(SETTING_NAME_PERIOD_UNITS, "ms");

		return TimeUnits.getTimeMS(periodNumber, periodUnits);
	}

	public long getSizeMax()
	{
		m_sizeMax = getSettingAsLong (SETTING_NAME_SIZE_MAX, m_sizeMax);
		return m_sizeMax;
	}
	public String getSizeUnits()
	{
		m_sizeUnits = getSettingAsString(SETTING_NAME_SIZE_UNITS, m_sizeUnits);
		return m_sizeUnits;
	}

	@Override
	public int hashCode()
	{
		IHashProvider hashProvider		= HashUtil.getHashProvider();
		hashProvider.add32(m_cacheName.hashCode());
		hashProvider.add32(m_factoryClassName.hashCode());
		hashProvider.add32(m_maintenancePeriodMS);
		hashProvider.add32(m_configurationProvider.hashCode());
		int					hashCode			= hashProvider.getHashCode32();
		hashProvider.release();

		return hashCode;
	}
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ObjectCacheConfiguration))
		{
			return false;
		}
		ObjectCacheConfiguration configuration		= (ObjectCacheConfiguration) o;

		if (!m_cacheName.equals(configuration.getCacheName()))
		{
			return false;
		}
		if (!m_factoryClassName.equals(configuration.getFactoryClassName()))
		{
			return false;
		}
		if (!(m_maintenancePeriodMS == configuration.getMaintenancePeriodMS()))
		{
			return false;
		}
		if (!m_configurationProvider.equals(configuration.m_configurationProvider))
		{
			return false;
		}

		return true;
	}

	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString()
	{
		String		rawClassName		= getFactoryClassName();
		String		factoryClassName	= null == rawClassName ? "null" :
				rawClassName.substring(rawClassName.lastIndexOf('.') + 1);

		return String.format("%s by %s @ %s",
				getCacheName(), factoryClassName, TimeUnits.formatMS(getMaintenancePeriodMS()));
	}
}
