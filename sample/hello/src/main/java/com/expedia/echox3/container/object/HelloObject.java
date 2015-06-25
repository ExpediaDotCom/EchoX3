/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.container.object;

import java.io.Serializable;
import java.net.URL;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.application.test.TestReadRequest;
import com.expedia.echox3.visible.application.test.TestReadResponse;
import com.expedia.echox3.visible.application.test.TestWriteRequest;
import com.expedia.echox3.visible.trellis.ClientFactory;
import com.expedia.echox3.visible.trellis.IAdminClient;
import com.expedia.echox3.visible.trellis.IClientFactory;
import com.expedia.echox3.visible.trellis.IObjectCacheClient;

public class HelloObject
{
	private static final String			CACHE_NAME			= "HelloObject";
	private static final String			FILE_NAME			= "data/Hello.ObjectCache.properties";

	private static final String			KEY					= "TheFox";
	private static final String			VALUE				= "The quick brown fox jumps over the lazy dog.";

	@SuppressWarnings("PMD.SystemPrintln")
	public static void main(String[] args) throws BasicException
	{
		//CHECKSTYLE:OFF
		System.out.println(CACHE_NAME);

		IClientFactory factory			= ClientFactory.getInstance();

		// Set the configuration file for this cache, required only in local.
		URL							url				= ClassLoader.getSystemResource(FILE_NAME);
		System.out.println("Found the cache configuration file at " + url.toString());
		factory.putLocalConfiguration(CACHE_NAME, url);

		// Create the local cache; or connect to the remote cache; always required
		IAdminClient admin			= factory.getAdminClient(IClientFactory.ClientType.Local);
		admin.connectToCache(CACHE_NAME);

		IObjectCacheClient client			= factory.getObjectClient(IClientFactory.ClientType.Local);
		TestWriteRequest			writeRequest	= new TestWriteRequest(0, 0, false, VALUE);
		client.writeOnly(CACHE_NAME, KEY, writeRequest);

		TestReadRequest				readRequest		= new TestReadRequest(0, 0, false);
		Serializable				response		= client.readOnly(CACHE_NAME, KEY, readRequest);
		TestReadResponse			readResponse	= (TestReadResponse) response;
		String						back			= readResponse.getValue();

		if (VALUE.equals(back))
		{
			System.out.println("Success -> Retrieved " + back);
		}
		else
		{
			System.out.println("Failure -> Retrieved " + back);
		}
		//CHECKSTYLE:ON
	}
}
