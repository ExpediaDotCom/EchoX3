/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.jvm.BasicMBeanManager;
import com.expedia.echox3.basics.monitoring.jvm.BasicMBeanProxy;
import com.expedia.echox3.basics.monitoring.jvm.GarbageInfo;
import com.expedia.echox3.basics.monitoring.jvm.GarbageInfoMeasureProxy;
import com.expedia.echox3.basics.monitoring.jvm.JavaMeasureProxy;
import com.expedia.echox3.basics.monitoring.jvm.MonitoringCollator;
import com.expedia.echox3.basics.monitoring.jvm.WallClockMeasureProxy;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;

public class MBeanIntgExtTests extends AbstractTestTools
{
	private static final String SERVER_NAME_001		= "localhost";

	@BeforeClass
	public static void beforeTest()
	{
		WallClock.getCurrentTimeMS();		// Force class to load
		GarbageInfo.getInstanceYoung();		// Force class to load
	}

	@Test
	public void helloJmx() throws Exception
	{
		logTestName();

		BasicMBeanProxy proxy1	= ensureConnection(SERVER_NAME_001);
		assertNotNull(proxy1);
	}

	@Test
	public void testGetGarbage() throws TimeoutException
	{
		ensureConnection(SERVER_NAME_001);

		GarbageInfoMeasureProxy		proxyYoung	= new GarbageInfoMeasureProxy(GarbageInfo.PRIMARY_POOL_CONTAINS_YOUNG);
		validateGarbageInfo(proxyYoung);

		GarbageInfoMeasureProxy		proxyOld	= new GarbageInfoMeasureProxy(GarbageInfo.PRIMARY_POOL_CONTAINS_OLD);
		validateGarbageInfo(proxyOld);
	}
	private void validateGarbageInfo(GarbageInfoMeasureProxy proxy)
	{
		boolean						isSuccess	= proxy.measure(SERVER_NAME_001);
		assertTrue(isSuccess);

		assertNotNull(proxy);
		assertTrue(proxy.isConnected());
		assertNotNull(proxy.getCollectorName());
//		assertTrue(0 < proxy.getTotalDurationMS());		// Not specified in G1 collector
//		assertTrue(0 != proxy.getPrimaryPoolMax());		// Not specified in G1 collector
	}

	@Test
	public void testJavaMonitor() throws TimeoutException
	{
		ensureConnection(SERVER_NAME_001);

		JavaMeasureProxy proxy	= new JavaMeasureProxy();
		boolean					isSuccess	= proxy.measure(SERVER_NAME_001);
		assertTrue(isSuccess);
		validateJava(proxy);
	}
	private void validateJava(JavaMeasureProxy proxy)
	{
		assertTrue(proxy.isConnected());
		assertNotNull(proxy.getJavaVersion());
	}

	@Test
	public void testWallClockMeasure() throws TimeoutException
	{
		ensureConnection(SERVER_NAME_001);

		WallClockMeasureProxy	proxy		= new WallClockMeasureProxy();
		boolean					isSuccess	= proxy.measure(SERVER_NAME_001);
		assertTrue(isSuccess);
		validateWallClock(proxy);
	}
	private void validateWallClock(WallClockMeasureProxy proxy)
	{
		assertTrue(proxy.isConnected());
		assertNotNull(proxy.getMasterName());
	}

	@Test
	public void testMonitoringCollator() throws TimeoutException
	{
		ensureConnection(SERVER_NAME_001);

		MonitoringCollator collator1		= new MonitoringCollator(SERVER_NAME_001);
		collator1.measure();
		validateCollator(collator1);

		Set<MonitoringCollator> set				= new TreeSet<>(MonitoringCollator.getComparatorByName());
		set.add(collator1);
		Iterator<MonitoringCollator> iterator	= set.iterator();
		assertTrue(collator1 == iterator.next());		// NOPMD Exactly the same object
		assertFalse(iterator.hasNext());
	}
	private void validateCollator(MonitoringCollator collator)
	{
		validateGarbageInfo(collator.getGarbageInfoYoung());
		validateGarbageInfo(collator.getGarbageInfoOld());
		validateJava(collator.getJavaMeasureProxy());
		validateWallClock(collator.getWallClockMeasure());
	}





	protected BasicMBeanProxy ensureConnection(String serverName) throws TimeoutException
	{
		String				port		= BasicMBeanManager.DEFAULT_JMX_PORT;
		BasicMBeanProxy		proxy		= BasicMBeanManager.getMbeanProxy(serverName, port);
		long				waitTime	= 5000;

		// Wait for it to be connected
		long				timeMS		= WallClock.getCurrentTimeMS();
		while (!proxy.isConnected())
		{
			BasicTools.sleepMS(10);

			if ((WallClock.getCurrentTimeMS() - timeMS) >  waitTime)
			{
				throw new TimeoutException("Unable to connect to the remote server");
			}
		}
		getLogger().info(BasicEvent.EVENT_TEST, "Connected to %s after %s",
				serverName, TimeUnits.formatMS(WallClock.getCurrentTimeMS() - timeMS));

		return proxy;
	}

}
