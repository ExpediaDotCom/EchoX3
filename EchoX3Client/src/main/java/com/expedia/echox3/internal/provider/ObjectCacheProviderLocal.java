/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.provider;

import java.util.Map;

import com.expedia.echox3.basics.collection.simple.CopyOnWriteSimpleMap;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.internal.store.cache.LocalObjectFactoryWrapper;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;
import com.expedia.echox3.visible.trellis.ClientFactory;

public class ObjectCacheProviderLocal implements IObjectCacheProvider
{
	private static final BasicLogger	LOGGER		= new BasicLogger(ObjectCacheProviderLocal.class);

	private static final String			SETTING_PREFIX				= ObjectCacheProviderLocal.class.getName();
	private static final String			SETTING_OOM_PERCENT			= SETTING_PREFIX + ".oomPercent";

	private final CopyOnWriteSimpleMap<String, LocalObjectCache> m_itemCacheMap		= new CopyOnWriteSimpleMap<>();

	private int							m_oomPercent				= 87;

	public ObjectCacheProviderLocal()
	{
		// NOTE: Need to use MagicReadWriteLock Without counters (as opposed to with disabled counters)
		// See output of profiler for justification: Limiting factor is the counter in this lock.

		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void setObjectCacheConfigurationMap(Map<String, ObjectCacheConfiguration> map)
	{
		for (Map.Entry<String, ObjectCacheConfiguration> entry : map.entrySet())
		{
			String						cacheName		= entry.getKey();
			ObjectCacheConfiguration	configuration	= entry.getValue();
			LocalObjectCache			objectCache;
			try
			{
				objectCache = getCache(cacheName);
			}
			catch (BasicException e)
			{
				objectCache = new LocalObjectCache(configuration);
				m_itemCacheMap.put(cacheName, objectCache);
			}
			objectCache.updateConfiguration(configuration);
		}
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		int			oomPercent		= ConfigurationManager.getInstance().getInt(
																SETTING_OOM_PERCENT, Integer.toString(m_oomPercent));
		if (oomPercent != m_oomPercent)
		{
			getLogger().info(BasicEvent.EVENT_PROTECTION_OOM_PERCENT_LEVEL,
					"OOM protection level changed from %,d %% to %,d %%.", m_oomPercent, oomPercent);
			m_oomPercent = oomPercent;
		}
	}

	// Methods from IAdminClient
	@Override
	public void connectToCache(String cacheName) throws BasicException
	{
		LocalObjectCache itemCache;
		ClientFactory					factory				= ClientFactory.getInstance();
		LocalCacheConfigurationManager configurationManager = factory.getLocalCacheConfigurationManager();
		IConfigurationProvider			provider			= configurationManager.getConfigurationProvider(cacheName);

		ObjectCacheConfiguration		configuration		= new ObjectCacheConfiguration(cacheName, provider);

		itemCache = m_itemCacheMap.get(configuration.getCacheName());
		if (null == itemCache)
		{
			synchronized (m_itemCacheMap)
			{
				itemCache = m_itemCacheMap.get(configuration.getCacheName());
				if (null == itemCache)
				{
					itemCache = new LocalObjectCache(configuration);
					m_itemCacheMap.put(configuration.getCacheName(), itemCache);
				}
				else
				{
					itemCache.updateConfiguration(configuration);
				}
			}
		}
		else
		{
			itemCache.updateConfiguration(configuration);
		}
	}
	@Override
	public void close(String cacheName)
	{
		LocalObjectCache cache			= m_itemCacheMap.remove(cacheName);
		if (null != cache)
		{
			cache.close();
		}
	}

	@Override
	public void pushClassDefinition(String cacheName, Class clazz)
	{
		// Nothing to do in local mode.
	}

	@Override
	public void upgradeClass(String cacheName, String classNameFrom, String factoryClassName) throws BasicException
	{
		LocalObjectCache cache			= getCache(cacheName);
		LocalObjectFactoryWrapper factoryWrapper	= new LocalObjectFactoryWrapper(cache.getConfiguration());
		factoryWrapper.setClassName(factoryClassName);

		cache.upgradeClass(classNameFrom, factoryWrapper);
	}

	@Override
	public Map<String, ManifestWrapper> getVersion() throws BasicException
	{
		return ManifestWrapper.getManifestMap();
	}


	// Methods common to both ITrellis*Client
	@Override
	public void flush(String cacheName, int durationMS) throws BasicException
	{
		LocalObjectCache cache		= getCache(cacheName);
		cache.flush(durationMS);
	}

	@Override
	public void writeOnly(String cacheName, byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		ensureMemory();

		LocalObjectCache cache		= getCache(cacheName);
		cache.writeOnly(keyBytes, requestBytes);
	}

	@Override
	public void writeOnly(String cacheName, byte[][] keyBytesList, byte[] requestBytes) throws BasicException
	{
		ensureMemory();

		LocalObjectCache cache			= getCache(cacheName);
		for (int i = 0; i < keyBytesList.length; i++)
		{
			cache.writeOnly(keyBytesList[i], requestBytes);
		}
	}

	@Override
	public void writeOnly(String cacheName, byte[][] keyBytesList, byte[][] requestBytesList) throws BasicException
	{
		ensureMemory();

		LocalObjectCache cache			= getCache(cacheName);
		for (int i = 0; i < keyBytesList.length; i++)
		{
			cache.writeOnly(keyBytesList[i], requestBytesList[i]);
		}
	}

	@Override
	public byte[] readOnly(String cacheName, byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		LocalObjectCache cache			= getCache(cacheName);
		byte[]					responseBytes	= cache.readOnly(keyBytes, requestBytes);
		return responseBytes;
	}

	@Override
	public byte[][] readOnly(String cacheName, byte[][] keyBytesList, byte[] requestBytes) throws BasicException
	{
		LocalObjectCache cache			= getCache(cacheName);

		byte[][]			responseList	= new byte[keyBytesList.length][];
		for (int i = 0; i < keyBytesList.length; i++)
		{
			responseList[i] = cache.readOnly(keyBytesList[i], requestBytes);
		}
		return responseList;
	}

	@Override
	public byte[][] readOnly(String cacheName, byte[][] keyBytesList, byte[][] requestBytesList) throws BasicException
	{
		LocalObjectCache cache			= getCache(cacheName);

		byte[][]			responseList		= new byte[keyBytesList.length][];
		for (int i = 0; i < keyBytesList.length; i++)
		{
			responseList[i] = cache.readOnly(keyBytesList[i], requestBytesList[i]);
		}
		return responseList;
	}


	@Override
	public byte[] reduce(String cacheName, byte[][] keyList, byte[] request)
	{
		return new byte[0];
	}


	public LocalObjectCache getCache(String cacheName) throws BasicException
	{
		LocalObjectCache cache;

		cache = m_itemCacheMap.get(cacheName);
		if (null == cache)
		{
			throw new BasicException(BasicEvent.EVENT_UNKNOWN_CACHE,
					String.format("Cache %s has not been created yet.", cacheName));
		}

		return cache;
	}

	private void ensureMemory() throws BasicException
	{
		long		heapPercent		= BasicTools.getHeapPercent(true);
		if (m_oomPercent < heapPercent)
		{
			throw new BasicException(BasicEvent.EVENT_PROTECTION_OOM,
					"OOM protection level of %,d %% exceeded at %,d %%", m_oomPercent, heapPercent);
		}
	}
}
