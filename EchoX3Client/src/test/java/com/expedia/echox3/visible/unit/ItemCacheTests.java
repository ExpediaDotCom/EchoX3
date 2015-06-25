/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.unit;

import java.net.URL;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.FileConfigurationProvider;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.internal.store.cache.LocalObjectCache;
import com.expedia.echox3.visible.application.test.TestReadRequest;
import com.expedia.echox3.visible.application.test.TestReadResponse;
import com.expedia.echox3.visible.application.test.TestWriteRequest;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class ItemCacheTests extends AbstractTestTools
{
	private static final String			SHORT_FOX_TEXT		= "The quick brown fox ... ";
	private static final byte[]			KEY_1;
	private static final byte[]			KEY_2;
	private static final byte[]			KEY_3;

	private static final String			QUICK_FOX_TEXT		= "The quick brown fox jumps over the lazy dog.";
	private static final String			VALUE_1				= QUICK_FOX_TEXT + "1";
	private static final String			VALUE_2				= QUICK_FOX_TEXT + "2";
	private static final String			VALUE_3				= QUICK_FOX_TEXT + "3";

	static
	{
		byte[]		bytes1		= null;
		byte[]		bytes2		= null;
		byte[]		bytes3		= null;
		try
		{
			bytes1 = BasicSerial.toBytes(ItemCacheTests.class.getSimpleName(), SHORT_FOX_TEXT + "1");
			bytes2 = BasicSerial.toBytes(ItemCacheTests.class.getSimpleName(), SHORT_FOX_TEXT + "2");
			bytes3 = BasicSerial.toBytes(ItemCacheTests.class.getSimpleName(), SHORT_FOX_TEXT + "3");
		}
		catch (BasicException e)
		{
			// Will not fail!
			getLogger().error(BasicEvent.EVENT_DEBUG, "Failed to serialize QUICK_FOX", e);
		}
		KEY_1	= bytes1;
		KEY_2	= bytes2;
		KEY_3	= bytes3;
	}

	@Test
	public void testSimple() throws BasicException
	{
		String						testName			= logTestName();
		String						filename			= "data/TestObject.ObjectCache.properties";
		URL							url					= FileFinder.findUrlOnClasspath(filename);
		assertNotNull(url);
		IConfigurationProvider		provider			= new FileConfigurationProvider(url);
		ObjectCacheConfiguration configuration		= new ObjectCacheConfiguration("TestObjectCache", provider);

		LocalObjectCache cache					= new LocalObjectCache(configuration);
		assertEquals(0, cache.getItemCount());

		TestWriteRequest	writeRequest1		= new TestWriteRequest(100, 10, false, VALUE_1);
		TestWriteRequest	writeRequest2		= new TestWriteRequest(100, 10, false, VALUE_2);
		TestWriteRequest	writeRequest3		= new TestWriteRequest(100, 10, false, VALUE_3);
		cache.writeOnly(KEY_1, BasicSerial.toBytes(testName, writeRequest1));
		cache.writeOnly(KEY_2, BasicSerial.toBytes(testName, writeRequest2));
		cache.writeOnly(KEY_3, BasicSerial.toBytes(testName, writeRequest3));
		assertEquals(3, cache.getItemCount());

		TestReadRequest		readRequest		= new TestReadRequest(100, 10, false);
		byte[]				readBytes		= BasicSerial.toBytes(testName, readRequest);
		byte[]				bytes1			= cache.readOnly(KEY_1, readBytes);
		byte[]				bytes2			= cache.readOnly(KEY_2, readBytes);
		byte[]				bytes3			= cache.readOnly(KEY_3, readBytes);
		TestReadResponse	response1		= (TestReadResponse) BasicSerial.toObject(testName, bytes1);
		TestReadResponse	response2		= (TestReadResponse) BasicSerial.toObject(testName, bytes2);
		TestReadResponse	response3		= (TestReadResponse) BasicSerial.toObject(testName, bytes3);

		assertNotNull(response1);
		assertNotNull(response2);
		assertNotNull(response3);
		assertEquals(VALUE_1, response1.getValue());
		assertEquals(VALUE_2, response2.getValue());
		assertEquals(VALUE_3, response3.getValue());
	}
}
