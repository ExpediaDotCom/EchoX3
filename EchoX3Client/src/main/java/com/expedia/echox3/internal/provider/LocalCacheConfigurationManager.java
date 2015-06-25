/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.provider;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.configuration.FileConfigurationProvider;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class LocalCacheConfigurationManager extends AbstractScheduledThread
{
	private final ObjectCacheProviderLocal m_cacheProviderLocal;
	private final Map<String, FileConfigurationProvider>	m_providerMap			= new HashMap<>();

	public LocalCacheConfigurationManager(ObjectCacheProviderLocal cacheProviderLocal)
	{
		super(false);

		m_cacheProviderLocal = cacheProviderLocal;

		setName(LocalCacheConfigurationManager.class.getSimpleName());
		setDaemon(true);
		start();
	}

	@Override
	public void updateConfiguration()
	{
		super.updateConfiguration();
	}

	public boolean put(String cacheName, URL configurationFileAddress)
	{
		synchronized (m_providerMap)
		{
			FileConfigurationProvider		provider		= m_providerMap.get(cacheName);
			if (null == provider || !provider.getSource().equals(configurationFileAddress))
			{
				provider = new FileConfigurationProvider(configurationFileAddress);
				m_providerMap.put(cacheName, provider);
				updateCacheConfiguration(cacheName, provider);
				return true;
			}
			return false;
		}
	}
	public boolean remove(String cacheName)
	{
		synchronized (m_providerMap)
		{
			return null != m_providerMap.remove(cacheName);
		}
	}

	public IConfigurationProvider getConfigurationProvider(String cacheName) throws BasicException
	{
		IConfigurationProvider		provider		= m_providerMap.get(cacheName);
		if (null == provider)
		{
			throw new BasicException(BasicEvent.EVENT_CONFIGURATION_FILE_LOAD_ERROR,
					"The configuration file for local ObjectCache %s has not been set.", cacheName);
		}
		return provider;
	}

	@Override
	protected void runOnce(long timeMS)
	{
		for (Map.Entry<String, FileConfigurationProvider> entry : m_providerMap.entrySet())
		{
			String						cacheName		= entry.getKey();
			FileConfigurationProvider	provider		= entry.getValue();
			if (provider.reload())		// true == hasChanged
			{
				// Something has changed, update the matching cache...
				updateCacheConfiguration(cacheName, provider);
			}
		}
	}

	private void updateCacheConfiguration(String cacheName, IConfigurationProvider provider)		// NOPMD
	{
		// Something has changed, update the matching cache...
		try
		{
			// In local mode, the IConfigurationProvider and ObjectCacheConfiguration objects
			// are unlikely to change for the lifetime of the cache, but they are updated.
			// The ObjectCacheConfiguration needs to be told when its IConfigurationProvider changes.
			// The LocalObjectCache needs to be told when its ObjectCacheConfiguration changes.

			LocalObjectCache cache			= m_cacheProviderLocal.getCache(cacheName);
			// Get the configuration object...
			ObjectCacheConfiguration configuration	= cache.getConfiguration();
			// "Tell" the configuration that its provider has been updated.
			configuration.updateConfiguration(provider);
			// "Kick" the cache, so it knows the configuration has changed.
			cache.updateConfiguration(configuration);
		}
		catch (BasicException e)
		{
			// Exception indicates the cache does not exist (e.g. has not been created).
			// Ignore exception, the cache will be created later.
		}
	}
}
