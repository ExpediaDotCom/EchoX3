/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.unit;

import java.lang.reflect.Field;
import java.net.URL;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.monitoring.counter.ItemSizeCounter;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.provider.ObjectCacheProviderLocal;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.internal.store.counter.ItemCounterFamily;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheClient;
import com.expedia.echox3.visible.trellis.ClientFactory;
import com.expedia.echox3.visible.trellis.IAdminClient;
import com.expedia.echox3.visible.trellis.IClientFactory;
import com.expedia.echox3.visible.trellis.ISimpleCacheClient;

public class LocalCacheTests extends AbstractTestTools
{
	private static final String			CONFIG_URL		= "config/SimpleCache.config.properties";
	private static final String			KEY1			= "Key1";
	private static final String			KEY2			= "Key2";
	private static final String			VALUE1			= "Value1";
	private static final String			VALUE2			= "Value2";

	private static final String			KEY_FORMAT		= "Key-%d";
	private static final String			VALUE_FORMAT	= "Value-%d";

	@Test
	public void testHello() throws Exception
	{
		String					testName		= logTestName();
		ISimpleCacheClient		cacheClient		= createLocalCache(testName);

		validateClient(testName, 0);
		cacheClient.put(testName, KEY1, VALUE1);		// Add value
		validateClient(testName, 1);

		cacheClient.put(testName, KEY2, null);			// NOOP
		validateClient(testName, 1);

		cacheClient.put(testName, KEY2, VALUE1);		// Add value
		validateClient(testName, 2);
		cacheClient.put(testName, KEY2, VALUE2);		// Replace value
		validateClient(testName, 2);
		cacheClient.put(testName, KEY2, null);			// Delete value
		validateClient(testName, 1);
		cacheClient.flush(testName, 0);					// Flush value
		validateClient(testName, 0);

	}
	private void validateClient(String cacheName, int itemCountExpected) throws Exception
	{
		// NOTE: This method looks inside the internal objects to validate the details of the object count.
		ClientFactory				factory				= ClientFactory.getInstance();
		ISimpleCacheClient			cacheClient			= factory.getSimpleClient(IClientFactory.ClientType.Local);
		SimpleCacheClient			simpleClient		= (SimpleCacheClient) cacheClient;
		ObjectCacheProviderLocal	providerLocal		= (ObjectCacheProviderLocal) simpleClient.getProvider();
		LocalObjectCache			objectCache			= providerLocal.getCache(cacheName);
		ItemCounterFamily			counterFamily		= objectCache.getCounterFamily();

		long						itemCount			= objectCache.getItemCount();
		assertEquals("ItemCount", itemCountExpected, itemCount);
		ItemSizeCounter				itemSizeCounter		= (ItemSizeCounter) getField(counterFamily, "m_keySize");
		itemSizeCounter.doBeanUpdate(1000);
		long						keyCount			= itemSizeCounter.getItemCount();
		assertEquals("KeyCount", itemCountExpected, keyCount);
	}

	@Test
	public void testUpdate() throws Exception
	{
		String testName = logTestName();
		ISimpleCacheClient cacheClient = createLocalCache(testName);

		validateClient(testName, 0);

		int			indexMax		= 100;
		int			iMax			= 1000;
		for (int i = 0; i < iMax; i++)
		{
			boolean			isPut		= RANDOM.nextBoolean();
			if (isPut)
			{
				cacheClient.put(testName, getText(KEY_FORMAT, i, indexMax), getText(VALUE_FORMAT, i, indexMax));
			}
			else
			{
				cacheClient.put(testName, getText(KEY_FORMAT, i, indexMax), null);
			}
		}

		for (int i = 0; i < iMax; i++)
		{
			cacheClient.put(testName, getText(KEY_FORMAT, i, indexMax), getText(VALUE_FORMAT, i, indexMax));
		}
		validateClient(testName, indexMax);

		for (int i = 0; i < iMax; i++)
		{
			cacheClient.put(testName, getText(KEY_FORMAT, i, indexMax), null);
		}
		validateClient(testName, 0);
	}
	private static final String getText(String format, int index, int indexMax)
	{
		return String.format(format, index % indexMax);
	}


	private ISimpleCacheClient createLocalCache(String cacheName) throws BasicException
	{
		URL							url				= FileFinder.findUrlOnClasspath(CONFIG_URL);
		ClientFactory				factory			= ClientFactory.getInstance();
		factory.putLocalConfiguration(cacheName, url);
		IAdminClient				adminClient		= factory.getAdminClient(IClientFactory.ClientType.Local);
		adminClient.connectToCache(cacheName);

		return factory.getSimpleClient(IClientFactory.ClientType.Local);
	}
	private Object getField(ItemCounterFamily counterFamily, String fieldName) throws Exception
	{
		Class				clazz		= counterFamily.getClass();
		Field				field		= clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object				fieldValue	= field.get(counterFamily);

		return fieldValue;
	}
}
