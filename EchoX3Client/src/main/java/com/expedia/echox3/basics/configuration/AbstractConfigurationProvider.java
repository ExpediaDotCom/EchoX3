/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.hash.IHashProvider;

public abstract class AbstractConfigurationProvider implements IConfigurationProvider
{
	private static final BasicLogger		LOGGER			= new BasicLogger(AbstractConfigurationProvider.class);

	private final Object					m_source;
	private final Map<String, String>		m_settingMap		= new HashMap<>();

	protected AbstractConfigurationProvider(Object source)
	{
		m_source = source;
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	@Override
	public Object getSource()
	{
		return m_source;
	}

	@Override
	public String getSetting(String name)
	{
		return m_settingMap.get(name);
	}

	@Override
	public Map<String, String> getSettingMap()
	{
		return m_settingMap;
	}

	@Override
	public Set<String> getSettingNameSet(String prefix)
	{
		Set<String> set		= new TreeSet<>();
		synchronized (m_settingMap)
		{
			for (String settingName : m_settingMap.keySet())
			{
				if (settingName.startsWith(prefix))
				{
					set.add(settingName);
				}
			}
		}
		return set;
	}

	protected boolean setSettingMap(Map<String, String> settingMap)
	{
		synchronized (m_settingMap)
		{
			Map<String, String>		mapPrev		= cloneMap();

			m_settingMap.clear();
			m_settingMap.putAll(settingMap);

			return reportChange(mapPrev, m_settingMap);
		}
	}
	protected boolean setSettingMap(Properties properties)
	{
		synchronized (m_settingMap)
		{
			Map<String, String> mapPrev		= cloneMap();
			m_settingMap.clear();
			for (Map.Entry<Object, Object> entry : properties.entrySet())
			{
				m_settingMap.put(entry.getKey().toString(), entry.getValue().toString());
			}
			return reportChange(mapPrev, m_settingMap);
		}
	}
	protected boolean addSettingMap(Map<String, String> settingMap)
	{
		synchronized (m_settingMap)
		{
			Map<String, String> mapPrev		= cloneMap();
			m_settingMap.putAll(settingMap);
			return reportChange(mapPrev, m_settingMap);
		}
	}
	protected boolean addSetting(String settingName, String settingValue)
	{
		synchronized (m_settingMap)
		{
			Map<String, String> mapPrev		= cloneMap();
			if (null == settingValue)
			{
				m_settingMap.remove(settingName);
			}
			else
			{
				m_settingMap.put(settingName, settingValue);
			}
			return reportChange(mapPrev, m_settingMap);
		}
	}
	private Map<String, String> cloneMap()
	{
		Map<String, String> mapPrev		= new HashMap<>();
		mapPrev.putAll(m_settingMap);
		return mapPrev;
	}

	private boolean reportChange(Map<String, String> mapPrev, Map<String, String> mapNew)
	{
		StringBuilder sb			= new StringBuilder(1000);
		Map<String, String> mapRemoved	= getAddedSettings(mapNew, mapPrev, false);
		Map<String, String> mapAdded	= getAddedSettings(mapPrev, mapNew, true);
		for (Map.Entry<String, String> entry : mapRemoved.entrySet())
		{
			sb.append(String.format("Removed %s = %s", entry.getKey(), entry.getValue()));
			sb.append(BaseFileHandler.LINE_SEPARATOR);
		}
		for (Map.Entry<String, String> entry : mapAdded.entrySet())
		{
			sb.append(String.format("Added %s = %s -> %s",
					entry.getKey(), mapPrev.get(entry.getKey()), entry.getValue()));
			sb.append(BaseFileHandler.LINE_SEPARATOR);
		}

		if (0 < (mapRemoved.size() + mapAdded.size()))
		{
			getLogger().info(BasicEvent.EVENT_CONFIGURATION_CHANGE,
					"Configuration provider %s modified:" + BaseFileHandler.LINE_SEPARATOR + "%s",
					getSource(), sb.toString());
		}

		return 0 < (mapRemoved.size() + mapAdded.size());
	}

	private Map<String, String> getAddedSettings(
			Map<String, String> mapPrev, Map<String, String> mapNew, boolean includeReplaced)
	{
		Map<String, String> mapAdded		= new TreeMap<>();

		for (Map.Entry<String, String> entry : mapNew.entrySet())
		{
			String settingName		= entry.getKey();
			String settingValue	= entry.getValue();
			String prevValue		= mapPrev.get(settingName);
			if (null == prevValue)
			{
				// Added if previous value was null
				mapAdded.put(settingName, entry.getValue());
			}
			else if (includeReplaced && !settingValue.equals(prevValue))
			{
				// Replaced if previous value was different
				mapAdded.put(settingName, entry.getValue());
			}
		}

		return mapAdded;
	}

	/**
	 * Returns a hash code value for the object. This method is
	 * supported for the benefit of hash tables such as those provided by
	 * {@link java.util.HashMap}.
	 * <p>
	 * The general contract of {@code hashCode} is:
	 * <ul>
	 * <li>Whenever it is invoked on the same object more than once during
	 * an execution of a Java application, the {@code hashCode} method
	 * must consistently return the same integer, provided no information
	 * used in {@code equals} comparisons on the object is modified.
	 * This integer need not remain consistent from one execution of an
	 * application to another execution of the same application.
	 * <li>If two objects are equal according to the {@code equals(Object)}
	 * method, then calling the {@code hashCode} method on each of
	 * the two objects must produce the same integer result.
	 * <li>It is <em>not</em> required that if two objects are unequal
	 * according to the {@link Object#equals(Object)}
	 * method, then calling the {@code hashCode} method on each of the
	 * two objects must produce distinct integer results.  However, the
	 * programmer should be aware that producing distinct integer results
	 * for unequal objects may improve the performance of hash tables.
	 * </ul>
	 * <p>
	 * As much as is reasonably practical, the hashCode method defined by
	 * class {@code Object} does return distinct integers for distinct
	 * objects. (This is typically implemented by converting the internal
	 * address of the object into an integer, but this implementation
	 * technique is not required by the
	 * Java&trade; programming language.)
	 *
	 * @return a hash code value for this object.
	 * @see Object#equals(Object)
	 * @see System#identityHashCode
	 */
	@Override
	public int hashCode()
	{
		IHashProvider		hashProvider		= HashUtil.getHashProvider();
		hashProvider.add32(m_source.hashCode());
		hashProvider.add32(m_settingMap.hashCode());
		int					hashCode			= hashProvider.getHashCode32();
		hashProvider.release();

		return hashCode;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see java.util.HashMap
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AbstractConfigurationProvider))
		{
			return false;
		}

		AbstractConfigurationProvider		provider	= (AbstractConfigurationProvider) obj;
		if (!m_source.equals(provider.m_source))
		{
			return false;
		}
		if (!m_settingMap.equals(provider.m_settingMap))
		{
			return false;
		}
		return true;
	}

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 * <p>
	 * <p> As noted in {@link AutoCloseable#close()}, cases where the
	 * close may fail require careful attention. It is strongly advised
	 * to relinquish the underlying resources and to internally
	 * <em>mark</em> the {@code Closeable} as closed, prior to throwing
	 * the {@code IOException}.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close()
	{
		m_settingMap.clear();
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getSource().toString());
	}
}
