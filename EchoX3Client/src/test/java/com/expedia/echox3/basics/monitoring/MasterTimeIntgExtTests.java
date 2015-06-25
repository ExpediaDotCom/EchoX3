/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring;

import java.net.UnknownHostException;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.log4j.Level;

import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.time.BasicMasterTime;
import com.expedia.echox3.basics.monitoring.time.BasicMasterTime.MasterType;

/**
 * Do not use NIST time unless absolutely necessary.
 * Infoblox does just fine and should be used for all X-1 needs.
 */
public class MasterTimeIntgExtTests extends AbstractTestTools
{
	private static final long			OFFSET_MAX			= 5000;		// in MS
	private static final boolean		DO_NIST				= true;

	@BeforeClass
	public static void beforeClass()
	{
		ConfigurationManager.getInstance();
		BasicMasterTime.getLogger().getLogger().setLevel(Level.DEBUG);
	}

	@Test
	public void testHello() throws UnknownHostException, BasicException
	{
		logTestName();

		long	offsetInfoblox		= BasicMasterTime.measureOffsetMS(MasterType.Internal);
		assertTrue(offsetInfoblox < OFFSET_MAX);

		if (DO_NIST)
		{
			long	offsetNIST		= BasicMasterTime.measureOffsetMS(MasterType.InternetNist);
			long	delta			= Math.abs(offsetInfoblox - offsetNIST);
			assertTrue(String.format("The offsetInternet is %,d", offsetNIST), offsetNIST < OFFSET_MAX);
			assertTrue(String.format("The offsetInternal is %,d", delta), delta < OFFSET_MAX);
		}
	}

	@Test
	public void testDetails()
	{
		logTestName();

		validateDetails(MasterType.Internal);
		if (DO_NIST)
		{
			validateDetails(MasterType.InternetNist);
		}
	}
	private void validateDetails(BasicMasterTime.MasterType masterType)
	{
		BasicMasterTime		masterTime		= BasicMasterTime.measureTime(masterType);
		assertNotNull(masterTime);

		long				offsetMS		= masterTime.getOffsetMS();
		assertTrue(masterTime.isSuccess());
		assertEquals(masterType, masterTime.getMasterType());
		assertNotNull(masterTime.getSourceName());
		assertNull(masterTime.getException());
		assertTrue(String.format("The offsetInternet is %,d", offsetMS), Math.abs(offsetMS) < OFFSET_MAX);
	}
/*
	@Test
	public void testRandom()
	{
		InternetMasterTime		masterTime		= InternetMasterTime.getInstance();
		for (int i = 0; i < 10; i++)
		{
			getLogger().info(BasicEvent.EVENT_TODO, "Found host %s", masterTime.getHostName(i));
		}
	}
*/
}
