/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.cache;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.IObjectFactory;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class LocalObjectFactoryWrapper
{
	private static final BasicLogger LOGGER				= new BasicLogger(LocalObjectFactoryWrapper.class);

	private final ObjectCacheConfiguration m_cacheConfiguration;
	private String								m_className			= null;
	private IObjectFactory m_factory			= null;

	public LocalObjectFactoryWrapper(ObjectCacheConfiguration cacheConfiguration)
	{
		m_cacheConfiguration = cacheConfiguration;
		m_className = m_cacheConfiguration.getFactoryClassName();
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void setClassName(String className)
	{
		if (!className.equals(m_className))
		{
			m_className			= className;
			m_factory = null;
		}
	}

	public String getCacheName()
	{
		return m_cacheConfiguration.getCacheName();
	}

	public String getClassName()
	{
		return m_className;
	}

	private void createFactory()
	{
		try
		{
			ClassLoader				classLoader		= BasicSerial.getClassLoader(getCacheName());
			Class					clazz			= Class.forName(m_className, true, classLoader);
			@SuppressWarnings("unchecked")        // For the "typed" constructor
			Constructor				constructor		= clazz.getConstructor();
			IObjectFactory objectFactory	= (IObjectFactory) constructor.newInstance();
			objectFactory.updateConfiguration(m_cacheConfiguration);
			if (null != m_factory)
			{
				m_factory.close();
			}
			m_factory = objectFactory;
		}
		catch (Exception e)
		{
			getLogger().error(BasicEvent.EVENT_NO_FACTORY_CONSTRUCTOR,
					"Cache %s: Unable to load the new configuration: "
							+ "The supplied IObjectFactory class %s does not have a parameter-less constructor.",
					getCacheName(), getClassName());
		}
	}

	public void updateConfiguration()
	{
		setClassName(m_cacheConfiguration.getFactoryClassName());
		if (null != m_factory)
		{
			m_factory.updateConfiguration(m_cacheConfiguration);
		}
	}

	public ICacheObject createObject(ObjectKey key) throws BasicException
	{
		synchronized (this)
		{
			if (null == m_factory)
			{
				createFactory();
			}
		}
		if (null == m_factory)
		{
			throw new BasicException(BasicEvent.EVENT_NO_FACTORY_CONSTRUCTOR,
					"Cache %s: No factory available.", getCacheName());
		}

		ICacheObject cacheObject;
		try
		{
			cacheObject = m_factory.createObject();
			if (null == cacheObject)
			{
				Serializable keyObject = key.getKey(getCacheName());
				cacheObject = m_factory.createObject(keyObject);
			}
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_CACHE_OBJECT_EXCEPTION_CREATE, exception,
					"Unexpected exception creating a new object with factory %s", m_factory.getClass().getName());
		}

		return cacheObject;
	}

	public void close()
	{
		if (null != m_factory)
		{
			m_factory.close();
		}
	}
}
