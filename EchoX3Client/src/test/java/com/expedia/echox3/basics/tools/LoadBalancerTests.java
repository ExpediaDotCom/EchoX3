/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import java.net.UnknownHostException;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.LoadBalancer;

public class LoadBalancerTests extends AbstractTestTools
{
	private static final String			NAME_1		= "1";
	private static final String			NAME_2		= "2";
	private static final String			NAME_3		= "3";
	private static final String			NAME_4		= "4";
	private static final String			NAME_5		= "5";

	private static final TestLoadBalancedObject		OBJECT_1		= new TestLoadBalancedObject(NAME_1, true, 2);
	private static final TestLoadBalancedObject		OBJECT_2		= new TestLoadBalancedObject(NAME_2, true, 2);
	private static final TestLoadBalancedObject		OBJECT_3		= new TestLoadBalancedObject(NAME_3, true, 2);
	private static final TestLoadBalancedObject		OBJECT_4		= new TestLoadBalancedObject(NAME_4, true, 2);
	private static final TestLoadBalancedObject		OBJECT_5		= new TestLoadBalancedObject(NAME_5, true, 2);

	@Test
	public void testHello() throws UnknownHostException, BasicException
	{
		String		testName	= logTestName();

		LoadBalancer<TestLoadBalancedObject>	loadBalancer		= createLoadBalancer(testName);

		assertEquals(OBJECT_1, loadBalancer.getNext(null));
		assertEquals(OBJECT_2, loadBalancer.getNext(null));
		assertEquals(OBJECT_3, loadBalancer.getNext(null));
		assertEquals(OBJECT_4, loadBalancer.getNext(null));
		assertEquals(OBJECT_5, loadBalancer.getNext(null));
		assertEquals(OBJECT_1, loadBalancer.getNext(null));

		OBJECT_3.setLoad(0);
		assertEquals(OBJECT_3, loadBalancer.getNext(null));
		OBJECT_3.setActive(false);
		OBJECT_4.setLoad(1);
		assertEquals(OBJECT_4, loadBalancer.getNext(null));
		assertEquals(OBJECT_5, loadBalancer.getNext(OBJECT_5));
	}

	@Test
	public void testPerformance()
	{
		String		testName	= logTestName();

		LoadBalancer<TestLoadBalancedObject>	loadBalancer		= createLoadBalancer(testName);

		for (int i = 0; i < 10; i++)
		{
			measurePerformance(loadBalancer, 100 * 1000, null);
		}
	}
	private void measurePerformance(
			LoadBalancer<TestLoadBalancedObject> loadBalancer, int count, TestLoadBalancedObject prefer)
	{
		long			t1			= System.nanoTime();
		for (int i = 0; i < count; i++)
		{
			TestLoadBalancedObject		back		= loadBalancer.getNext(prefer);
			assertNotNull(back);
			if (null != prefer)
			{
				assertEquals(back, prefer);
			}
		}
		long			t2			= System.nanoTime();
		long			durationNS	= t2 - t1;
		reportPerformance("LoadBalancer", durationNS, count, true);
	}







	private LoadBalancer<TestLoadBalancedObject> createLoadBalancer(String name)
	{
		LoadBalancer<TestLoadBalancedObject>	loadBalancer		= new LoadBalancer<>(name);
		loadBalancer.add(OBJECT_1);
		loadBalancer.add(OBJECT_2);
		loadBalancer.add(OBJECT_3);
		loadBalancer.add(OBJECT_4);
		loadBalancer.add(OBJECT_5);
		assertEquals(5, loadBalancer.size());

		return loadBalancer;
	}
}
