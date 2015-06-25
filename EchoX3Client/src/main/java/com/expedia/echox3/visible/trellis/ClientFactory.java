/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.net.URL;

import com.expedia.echox3.internal.provider.LocalCacheConfigurationManager;
import com.expedia.echox3.internal.provider.ObjectCacheProviderLocal;
import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.provider.ObjectCacheProviderRemote;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.wrapper.AdminClient;
import com.expedia.echox3.internal.wrapper.MonitorClient;
import com.expedia.echox3.internal.wrapper.ObjectCacheClient;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheClient;

public class ClientFactory implements IClientFactory
{
	private static final ClientFactory INSTANCE				= new ClientFactory();

	//CHECKSTYLE:OFF
	private static final ObjectCacheProviderLocal TRELLIS_PROVIDER_LOCAL	= new ObjectCacheProviderLocal();
	private static final IAdminClient ADMIN_CLIENT_LOCAL		= new AdminClient(TRELLIS_PROVIDER_LOCAL);
	private static final ISimpleCacheClient SIMPLE_CLIENT_LOCAL = new SimpleCacheClient(TRELLIS_PROVIDER_LOCAL);
	private static final IObjectCacheClient OBJECT_CLIENT_LOCAL		= new ObjectCacheClient(TRELLIS_PROVIDER_LOCAL);
	private static final IMonitorClient MONITOR_CLIENT_LOCAL	= new MonitorClient(TRELLIS_PROVIDER_LOCAL);

	private static final String								USER_PROTOCOL_NAME		= "DispatcherUserSourceMessageHandler";
	private static final SourceProtocolHandler				USER_PROTOCOL_HANDLER	= new SourceProtocolHandler(USER_PROTOCOL_NAME, USER_PROTOCOL_NAME);
	private static final DispatcherUserSourceMessageHandler	USER_MESSAGE_HANDLER	= new DispatcherUserSourceMessageHandler(USER_PROTOCOL_HANDLER);

	private static final ObjectCacheProviderRemote TRELLIS_PROVIDER_REMOTE	= new ObjectCacheProviderRemote();
	private static final IAdminClient ADMIN_CLIENT_REMOTE		= new AdminClient(TRELLIS_PROVIDER_REMOTE);
	private static final ISimpleCacheClient SIMPLE_CLIENT_REMOTE	= new SimpleCacheClient(TRELLIS_PROVIDER_REMOTE);
	private static final IObjectCacheClient OBJECT_CLIENT_REMOTE	= new ObjectCacheClient(TRELLIS_PROVIDER_REMOTE);
	private static final IMonitorClient MONITOR_CLIENT_REMOTE	= new MonitorClient(TRELLIS_PROVIDER_REMOTE);
	//CHECKSTYLE:ON

	private static final LocalCacheConfigurationManager
													LOCAL_CACHE_CONFIGURATION_MANAGER
					= new LocalCacheConfigurationManager(TRELLIS_PROVIDER_LOCAL);

	static
	{
		// Complete the initialization of the protocol handler.
		USER_PROTOCOL_HANDLER.updateConfiguration();
	}

	private ClientFactory()
	{

	}

	public static ClientFactory getInstance()
	{
		return INSTANCE;
	}

	public LocalCacheConfigurationManager getLocalCacheConfigurationManager()
	{
		return LOCAL_CACHE_CONFIGURATION_MANAGER;
	}

	public ObjectCacheProviderLocal getProviderLocal()
	{
		return TRELLIS_PROVIDER_LOCAL;
	}
	public ObjectCacheProviderRemote getProviderRemote()
	{
		return TRELLIS_PROVIDER_REMOTE;
	}

	@Override
	public boolean putLocalConfiguration(String cacheName, URL configurationFileAddress)
	{
		return LOCAL_CACHE_CONFIGURATION_MANAGER.put(cacheName, configurationFileAddress);
	}

	@Override
	public boolean removeLocalConfiguration(String cacheName)
	{
		return LOCAL_CACHE_CONFIGURATION_MANAGER.remove(cacheName);
	}

	@Override
	public IAdminClient getAdminClient(ClientType clientType)
	{
		IAdminClient client	= null;

		switch (clientType)
		{
		case Local:
			client = ADMIN_CLIENT_LOCAL;
			break;
		case Remote:
			client = ADMIN_CLIENT_REMOTE;
			break;
		}

		return client;
	}

	@Override
	public ISimpleCacheClient getSimpleClient(ClientType clientType)
	{
		ISimpleCacheClient client	= null;

		switch (clientType)
		{
		case Local:
			client = SIMPLE_CLIENT_LOCAL;
			break;
		case Remote:
			client = SIMPLE_CLIENT_REMOTE;
			break;
		}

		return client;
	}

	@Override
	public IObjectCacheClient getObjectClient(ClientType clientType)
	{
		IObjectCacheClient client	= null;

		switch (clientType)
		{
		case Local:
			client = OBJECT_CLIENT_LOCAL;
			break;
		case Remote:
			client = OBJECT_CLIENT_REMOTE;
			break;
		}

		return client;
	}

	@Override
	public IMonitorClient getMonitorClient(ClientType clientType)
	{
		IMonitorClient client	= null;

		switch (clientType)
		{
			case Local:
				client = MONITOR_CLIENT_LOCAL;
				break;
			case Remote:
				client = MONITOR_CLIENT_REMOTE;
				break;
		}

		return client;
	}

	public static SourceProtocolHandler getUserProtocolHandler()
	{
		return USER_PROTOCOL_HANDLER;
	}

	public static DispatcherUserSourceMessageHandler getUserMessageHandler()
	{
		return USER_MESSAGE_HANDLER;
	}
}
