/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.unit;

import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.FileConfigurationProvider;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.file.UrlFinder;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheObject;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheObjectFactory;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class BaseItemTests extends AbstractTestTools
{
	@Test
	public void testHiperItemKey()
	{
		logTestName();

		String longText = "The quick brown fox jumps over the lazy dog. ";
		longText = longText + longText + longText + longText;
		longText = longText + longText + longText + longText;
		validateKey(new ObjectKey("Normal key".getBytes()));
		validateKey(new ObjectKey("X".getBytes()));
		validateKey(new ObjectKey("Key".getBytes()));
		validateKey(new ObjectKey(longText.getBytes()));
	}

	private void validateKey(ObjectKey key)
	{
		assertNotNull(key);
		assertTrue(0 < key.getKeyForBucket());
		assertTrue(0 < key.getKeyForBin());
		assertTrue(0 < key.getKeyForMap());

		assertFalse(key.getKeyForBucket() == key.getKeyForBin());
		assertFalse(key.getKeyForBucket() == key.getKeyForMap());
		assertFalse(key.getKeyForBin() == key.getKeyForMap());
	}


		@Test
		public void testSimpleEntryFactory()
		{
			String						testName		= logTestName();
			String						urlName			= "config/TestObject.config.properties";
			URL							url				= FileFinder.findUrlOnClasspath(urlName);
			IConfigurationProvider		provider		= new FileConfigurationProvider(url);
			SimpleCacheObjectFactory	factory			= new SimpleCacheObjectFactory();
			ObjectCacheConfiguration	configuration	= new ObjectCacheConfiguration(testName, provider);
			factory.updateConfiguration(configuration);

			long						time		= System.currentTimeMillis();
			BasicTools.sleepMS(10);
			ICacheObject object		= factory.createObject();
			assertTrue(object instanceof SimpleCacheObject);
			SimpleCacheObject entry		= (SimpleCacheObject) object;
			long						tWrite1		= entry.getWriteTimeMS();
			long						tRead1		= entry.getReadTimeMS();
			BasicTools.sleepMS(10);

			String						text		= "The quick brown fox ...";
			object.writeOnly(text.getBytes(Charset.defaultCharset()));
			long						tWrite2		= entry.getWriteTimeMS();
			long						tRead2		= entry.getReadTimeMS();
			BasicTools.sleepMS(10);

			Serializable				bytes		= object.readOnly(null);
			long						tWrite3		= entry.getWriteTimeMS();
			long						tRead3		= entry.getReadTimeMS();

			// Validate content of the object
			assertTrue(bytes instanceof byte[]);
			String back	= new String((byte[]) bytes, Charset.defaultCharset());
			assertEquals(text, back);

			// Validate the maintenance of the times...
			assertTrue(tWrite1 >= time);
			assertTrue(tWrite1 == tRead1);
			assertTrue(tWrite1 <  tWrite2);
			assertTrue(tWrite2 == tRead2);
			assertTrue(tWrite2 == tWrite3);
			assertTrue(tWrite2 <  tRead3);
	}

}
