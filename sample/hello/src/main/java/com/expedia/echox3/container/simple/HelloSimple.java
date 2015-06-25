/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.container.simple;

import java.io.Serializable;
import java.net.URL;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.trellis.ClientFactory;
import com.expedia.echox3.visible.trellis.IAdminClient;
import com.expedia.echox3.visible.trellis.IClientFactory;
import com.expedia.echox3.visible.trellis.ISimpleCacheClient;

@SuppressWarnings("PMD.SystemPrintln")
public class HelloSimple
{
	private static final String			CACHE_NAME			= "HelloSimple";
	private static final String			FILE_NAME			= "data/Hello.SimpleCache.properties";

	private static final String			KEY					= "TheFox";
	private static final String			VALUE				= "The quick brown fox jumps over the lazy dog.";

	@SuppressWarnings("PMD.usePrintln")
	public static void main(String[] args) throws BasicException
	{
		System.out.println(CACHE_NAME);

		IClientFactory factory			= ClientFactory.getInstance();

		// Set the configuration file for this cache, required only in local.
		URL							url				= ClassLoader.getSystemResource(FILE_NAME);
		System.out.println("Found the cache configuration file at " + url.toString());
		factory.putLocalConfiguration(CACHE_NAME, url);

		// Create the local cache; or connect to the remote cache; always required
		IAdminClient admin			= factory.getAdminClient(IClientFactory.ClientType.Local);
		admin.connectToCache(CACHE_NAME);

		ISimpleCacheClient client			= factory.getSimpleClient(IClientFactory.ClientType.Local);
		client.put(CACHE_NAME, KEY, VALUE);
		Serializable				back			= client.get(CACHE_NAME, KEY);

		if (VALUE.equals(back))
		{
			System.out.println("Success -> Retrieved " + back.toString());
		}
		else
		{
			System.out.println("Failure -> Retrieved " + back.toString());
		}
	}
}
