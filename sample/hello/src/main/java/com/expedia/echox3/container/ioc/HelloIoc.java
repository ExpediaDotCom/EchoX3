/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.container.ioc;

import java.io.Serializable;
import java.net.URL;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.ioc.IocSimpleCacheClient;
import com.expedia.echox3.visible.trellis.IClientFactory.ClientType;

@SuppressWarnings("PMD.SystemPrintln")
public class HelloIoc
{
	private static final String			CACHE_NAME_1		= "HelloIocTrellis1";
	private static final String			CACHE_NAME_2		= "HelloIocTrellis2";
	private static final String			FILE_NAME			= "data/HelloTrellis.SimpleCache.properties";

	private static final String			KEY					= "TheFox";
	private static final String			QUICK_FOX			= "The QUICK brown fox jumps over the lazy dog.";
	private static final String			BROWN_FOX			= "The quick BROWN fox jumps over the lazy dog.";

	@SuppressWarnings("PMD.usePrintln")
	public static void main(String[] args) throws BasicException
	{
		URL						url			= ClassLoader.getSystemResource(FILE_NAME);
		IocSimpleCacheClient	client1		= new IocSimpleCacheClient(ClientType.Local, CACHE_NAME_1, url);
		IocSimpleCacheClient	client2		= new IocSimpleCacheClient();

		client2.setClientType(ClientType.Local);
		client2.setCacheName(CACHE_NAME_2);
		client2.setConfigurationUrl(url.toString());

		client1.put(KEY, QUICK_FOX);
		validateClientContent(client1, KEY, QUICK_FOX);

		client2.setCacheName(CACHE_NAME_2);
		client2.put(KEY, BROWN_FOX);
		validateClientContent(client2, KEY, BROWN_FOX);

		validateClientContent(client1, KEY, QUICK_FOX);
	}

	private static void validateClientContent(IocSimpleCacheClient client, String key, String expectedValue)
			throws BasicException
	{
		Serializable				back			= client.get(key);

		if ((null == expectedValue && null == back)
			|| (null != expectedValue && expectedValue.equals(back)))
		{
			System.out.println("Success -> Retrieved " + (null == back ? "null" : back.toString()));
		}
		else
		{
			System.out.println("Failure -> Retrieved " + back.toString());
		}
	}
}
