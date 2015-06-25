/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;

public class BasicClassLoader extends ClassLoader implements BasicClassLoaderMBean
{
	private final String					m_name;
	private final StringGroup m_beanNameList;
	private final Map<String, byte[]>		m_classMap			= new HashMap<>();

	public BasicClassLoader(String name)
	{
		super(BasicClassLoader.class.getClassLoader());

		m_name = name;
		m_beanNameList = new StringGroup(new String[] {"ClassLoader", m_name});
		BasicTools.registerMBean(this, null, m_beanNameList);
	}

	public String getName()
	{
		return m_name;
	}

	public void unregisterMBean()
	{
		BasicTools.unregisterMBean(null, m_beanNameList);
	}

	public void putClassMap(Map<String, byte[]> map) throws BasicException
	{
		synchronized (m_classMap)
		{
			Collection<String>		classNameList		= map.keySet();
			List<String>			missedList			= new LinkedList<>();

			while (true)
			{
				for (String className : classNameList)
				{
					try
					{
						byte[]		bytes	= map.get(className);
						putClass(className, bytes);
					}
					catch (Throwable throwable)
					{
						missedList.add(className);
					}
				}

				if (missedList.isEmpty())
				{
					break;
				}
				if (missedList.size() == classNameList.size())
				{
					throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_DEPENDENCY_RESOLUTION,
							"Failed to resolve the class dependencies.");
				}

				classNameList = missedList;
				missedList = new LinkedList<>();
			}
		}
	}

	public void putClass(String className, byte[] bytes) throws BasicException
	{
		synchronized (m_classMap)
		{
			byte[]		bytesPrev		= m_classMap.get(className);
			if (null == bytesPrev)
			{
				try
				{
					defineClass(className, bytes, 0, bytes.length);
				}
				catch (Throwable throwable)
				{
					throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_DEFINE_CLASS_FAILED,
							String.format("Class definition for class %s failed.", className), throwable);
				}
			}
			else
			{
				// Class already defined, validate definitions are the same ...
				if (!Arrays.equals(bytes, bytesPrev))
				{
					throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_INCOMPATIBLE_DEFINITION,
							String.format(
									"New class definition for class %s conflicts with existing definition.",
									className));
				}
			}
		}
	}

	@Override
	public Map<String, Integer> getClassSizeMap()
	{
		Map<String, Integer>		map		= new TreeMap<String, Integer>();
		synchronized (m_classMap)
		{
			for (Map.Entry<String, byte[]> entry : m_classMap.entrySet())
			{
				map.put(entry.getKey(), entry.getValue().length);
			}
		}
		return map;
	}

	@Override
	public boolean isClassLoaded(String className)
	{
		boolean		isLoaded;

		synchronized(m_classMap)
		{
			// Look here first because it is faster
			isLoaded = null != m_classMap.get(className);

			// Also check with the class loader, in case it is on the classpath.
			if (!isLoaded)
			{
				try
				{
					Class.forName(className, false, this);
					isLoaded = true;
				}
				catch (Throwable throwable)
				{
					// Do nothing, isLoaded is already false.
				}
			}
		}

		return isLoaded;
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s, %d classes)",
				getClass().getSimpleName(), m_name, m_classMap.size());
	}
}
