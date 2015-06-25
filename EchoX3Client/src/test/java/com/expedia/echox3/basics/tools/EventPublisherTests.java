/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager.IEventListener;


public class EventPublisherTests extends AbstractTestTools
{
	private static final String PUBLISHER_NAME_11		= "Pub1Pool1";

	private static final String PUBLISHER_NAME_21		= "Pub2Pool1";
	private static final String PUBLISHER_NAME_32		= "Pub3Pool2";

	@Test
	public void testHello() throws UnknownHostException, InterruptedException
	{
		logTestName();

		CountDownLatch		someLatch		= new CountDownLatch(1);
		CountDownLatch		rightLatch		= new CountDownLatch(1);
		TestEventListener	listener		= new TestEventListener(someLatch, rightLatch, PUBLISHER_NAME_11, "Pool1");
		// Give the system time to settle
		BasicTools.sleepMS(100);

		PublisherManager.post(PUBLISHER_NAME_21, null);
		someLatch.await(15, TimeUnit.SECONDS);
		assertEquals(0, someLatch.getCount());
		assertEquals(1, rightLatch.getCount());		// Not triggered yet
		assertFalse(listener.gotRight());

		// Should not be receive by our listener
		PublisherManager.post(PUBLISHER_NAME_32, null);
		BasicTools.sleepMS(1000);
		assertEquals(0, someLatch.getCount());
		assertEquals(1, rightLatch.getCount());		// Not triggered yet

		PublisherManager.post(PUBLISHER_NAME_11, null);
		rightLatch.await(15, TimeUnit.SECONDS);
		assertEquals(0, rightLatch.getCount());		// Not triggered yet
		assertTrue(listener.gotRight());

	}





	private static class TestEventListener
	{
		private CountDownLatch		m_someLatch;		// Got some notification
		private CountDownLatch		m_rightLatch;		// Got the right notification
		private String				m_publisherName;
		private String				m_threadContains;
		private volatile boolean	m_gotRight			= false;

		private TestEventListener(
				CountDownLatch someLatch, CountDownLatch rightLatch, String publisherName, String threadContains)
		{
			m_someLatch = someLatch;
			m_rightLatch = rightLatch;
			m_publisherName = publisherName;
			m_threadContains = threadContains;

			// Listen to everybody, gotRight only when the correct publisher.
			PublisherManager.register(PUBLISHER_NAME_11, this::receiveEvent);
			PublisherManager.register(PUBLISHER_NAME_21, this::receiveEvent);
			PublisherManager.register(PUBLISHER_NAME_32, this::receiveEvent);
		}

		public void receiveEvent(String publisherName, long timeMS, Object event)
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Received event from %s", publisherName);
			m_someLatch.countDown();

			String		threadName		= Thread.currentThread().getName();
			m_gotRight = publisherName.equals(m_publisherName) && threadName.contains(m_threadContains);
			if (m_gotRight)
			{
				m_rightLatch.countDown();
			}
		}

		public boolean gotRight()
		{
			return m_gotRight;
		}
	}

}
