/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.wrapper;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.internal.provider.ObjectCacheProviderLocal;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheClient;
import com.expedia.echox3.visible.trellis.ClientFactory;
import com.expedia.echox3.visible.trellis.IAdminClient;
import com.expedia.echox3.visible.trellis.IClientFactory;
import com.expedia.echox3.visible.trellis.IMonitorClient;
import com.expedia.echox3.visible.trellis.ISimpleCacheClient;
import com.expedia.echox3.visible.trellis.IObjectCacheClient;

public class WiringTests extends AbstractTestTools
{
	@Test
	public void testAdmin()
	{
		logTestName();

		IClientFactory factory			= ClientFactory.getInstance();

		IAdminClient client			= factory.getAdminClient(IClientFactory.ClientType.Local);
		assertTrue(client instanceof AdminClient);

		AdminClient adminClient		= (AdminClient) client;
		assertNotNull(adminClient.getProvider());
		assertTrue(adminClient.getProvider() instanceof ObjectCacheProviderLocal);
	}

	@Test
	public void testSimpleCache()
	{
		logTestName();

		IClientFactory factory			= ClientFactory.getInstance();

		ISimpleCacheClient client			= factory.getSimpleClient(IClientFactory.ClientType.Local);
		assertTrue(client instanceof SimpleCacheClient);

		SimpleCacheClient blobClient		= (SimpleCacheClient) client;
		assertNotNull(blobClient.getProvider());
		assertTrue(blobClient.getProvider() instanceof ObjectCacheProviderLocal);
	}

	@Test
	public void testObject()
	{
		logTestName();

		IClientFactory factory			= ClientFactory.getInstance();

		IObjectCacheClient client			= factory.getObjectClient(IClientFactory.ClientType.Local);
		assertTrue(client instanceof ObjectCacheClient);

		ObjectCacheClient objectClient	= (ObjectCacheClient) client;
		assertNotNull(objectClient.getProvider());
		assertTrue(objectClient.getProvider() instanceof ObjectCacheProviderLocal);
	}

	@Test
	public void testMonitor()
	{
		logTestName();

		IClientFactory factory			= ClientFactory.getInstance();

		IMonitorClient client			= factory.getMonitorClient(IClientFactory.ClientType.Local);
		assertTrue(client instanceof MonitorClient);

		MonitorClient monitorClient	= (MonitorClient) client;
		assertNotNull(monitorClient.getProvider());
		assertTrue(monitorClient.getProvider() instanceof ObjectCacheProviderLocal);
	}
}
