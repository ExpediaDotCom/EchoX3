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
import com.expedia.echox3.internal.store.cache.LocalObjectFactoryWrapper;
import com.expedia.echox3.internal.store.cache.LocalObjectBin;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceRequest;
import com.expedia.echox3.internal.store.cache.LocalObjectCache.MaintenanceResponse;
import com.expedia.echox3.internal.store.wrapper.ObjectKey;
import com.expedia.echox3.internal.store.wrapper.ObjectWrapper;
import com.expedia.echox3.internal.store.wrapper.TrackingObjects;
import com.expedia.echox3.visible.application.test.TestObject;
import com.expedia.echox3.visible.application.test.TestObjectFactory;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class ItemBinTests extends AbstractTestTools
{
	private static final String			QUICK_FOX_TEXT		= "The quick brown fox ... ";
	private static final ObjectKey		KEY_1;
	private static final ObjectKey		KEY_2;
	private static final ObjectKey		KEY_3;

	static
	{
		byte[]		bytes1		= null;
		byte[]		bytes2		= null;
		byte[]		bytes3		= null;
		try
		{
			bytes1 = BasicSerial.toBytes(ItemBinTests.class.getSimpleName(), QUICK_FOX_TEXT + "1");
			bytes2 = BasicSerial.toBytes(ItemBinTests.class.getSimpleName(), QUICK_FOX_TEXT + "2");
			bytes3 = BasicSerial.toBytes(ItemBinTests.class.getSimpleName(), QUICK_FOX_TEXT + "3");
		}
		catch (BasicException e)
		{
			// Will not fail!
			getLogger().error(BasicEvent.EVENT_DEBUG, "Failed to serialize QUICK_FOX", e);
		}
		KEY_1	= new ObjectKey(bytes1);
		KEY_2	= new ObjectKey(bytes2);
		KEY_3	= new ObjectKey(bytes3);
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

		TrackingObjects				trackingObjects		= new TrackingObjects(testName);

		LocalObjectBin bin					= new LocalObjectBin(
																	configuration, trackingObjects, "Bucket-1", 5);
		assertEquals(0, bin.getItemCount());

//		TestObjectFactory			factory				= new TestObjectFactory();
		LocalObjectFactoryWrapper wrapper				= new LocalObjectFactoryWrapper(configuration);
		wrapper.setClassName(TestObjectFactory.class.getName());

		ObjectWrapper				wrapper1			= bin.getEntry(KEY_1, wrapper);
		ObjectWrapper				wrapper2			= bin.getEntry(KEY_2, wrapper);
		ObjectWrapper				wrapper3			= bin.getEntry(KEY_3, wrapper);
		assertEquals(3, bin.getItemCount());

		getLogger().info(BasicEvent.EVENT_DEBUG, "bin = %s", bin.toString());
		getLogger().info(BasicEvent.EVENT_DEBUG, "Wrapper1 = %s", wrapper1.toString());
		getLogger().info(BasicEvent.EVENT_DEBUG, "Wrapper2 = %s", wrapper2.toString());
		getLogger().info(BasicEvent.EVENT_DEBUG, "Wrapper3 = %s", wrapper3.toString());

		assertNotNull(wrapper1);
		assertNotNull(wrapper2);
		assertNotNull(wrapper3);
		assertTrue(wrapper1 != wrapper2);		// NOPMD
		assertTrue(wrapper1 != wrapper3);		// NOPMD
		assertTrue(wrapper2 != wrapper3);		// NOPMD

		assertTrue(wrapper1.getTrellisObject() instanceof TestObject);
		assertTrue(wrapper2.getTrellisObject() instanceof TestObject);
		assertTrue(wrapper3.getTrellisObject() instanceof TestObject);

		bin.deleteEntry(KEY_2);
		assertEquals(2, bin.getItemCount());

		ObjectWrapper				wrapper2b			= bin.getEntry(KEY_2, null);
		assertNull(wrapper2b);
		assertEquals(2, bin.getItemCount());

		MaintenanceRequest		maintenanceRequest		= new MaintenanceRequest();
		MaintenanceResponse		maintenanceResponse		= new MaintenanceResponse();
		maintenanceRequest.set(System.currentTimeMillis(), 100, true);
		maintenanceResponse.clear();
		bin.doMaintenance(maintenanceRequest, maintenanceResponse);
	}
}
