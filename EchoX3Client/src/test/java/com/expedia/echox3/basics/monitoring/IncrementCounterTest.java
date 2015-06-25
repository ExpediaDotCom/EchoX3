/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring;

import java.util.concurrent.CyclicBarrier;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IIncrementCounter;
import com.expedia.echox3.basics.monitoring.counter.IncrementCounterMBean;

public class IncrementCounterTest extends AbstractTestTools
{
	private static final CounterFactory		COUNTER_FACTORY		= CounterFactory.getInstance();

	@Test
	public void testOperation()
	{
		String					testName		= logTestName();

		IIncrementCounter		counter			= COUNTER_FACTORY.getIncrementCounter(
				new StringGroup(testName), "");

		counter.increment();
		counter.increment(5);
		counter.doBeanUpdate(5000);
		IncrementCounterMBean		bean	= counter.getMBean();
		assertNotNull(bean);
		assertEquals(6, bean.getValue());

		counter.decrement();
		counter.decrement(2);
		counter.doBeanUpdate(5000);
		assertEquals(3, bean.getValue());
	}

	@Test
	public void testPerformance()
	{
		String					testName		= logTestName();

		IIncrementCounter		counter			= COUNTER_FACTORY.getIncrementCounter(
				new StringGroup(testName), "");

		int		cPass			= 5;
		int		iMax			= 100 * 1000;
		int[]	cThreadList		= { 1, 2, 4 };

		for (int cThread : cThreadList)
		{
			measurePerformance(counter, cPass, iMax, cThread);
			measurePerformance(counter, cPass, iMax, cThread);
		}
	}

	private void measurePerformance(IIncrementCounter counter, int cPass, int iMax, int cThread)
	{
		for (int i = 0; i < cPass; i++)
		{
			measurePerformance(counter, iMax, cThread);
		}
	}
	private void measurePerformance(IIncrementCounter counter, int iMax, int cThread)
	{
		CyclicBarrier		barrier		= new CyclicBarrier(cThread + 1);

		AbstractPerformanceMeasure[]	measureList		= new AbstractPerformanceMeasure[cThread];
		for (int i = 0; i < cThread; i++)
		{
			measureList[i] = new PerformanceMeasure(barrier, iMax, counter);
			measureList[i].start();
		}

		// Trigger the start of all the threads
		waitForBarrier(barrier);

		// Wait for all the threads to complete
		waitForBarrier(barrier);

		long		durationNS		= 0;
		for (int i = 0; i < cThread; i++)
		{
			durationNS += measureList[i].m_durationNS;
		}
		reportPerformance(String.format("increment() w. %,d threads", cThread), durationNS, iMax * cThread, true);
	}


	private static void waitForBarrier(CyclicBarrier barrier)
	{
		try
		{
			barrier.await();
		}
		catch (Exception exception)
		{
			// Whatever, this should never happen!
			assertNull(exception);
		}
	}

	private abstract static class AbstractPerformanceMeasure extends Thread
	{
		private CyclicBarrier	m_barrier;
		private int				m_iMax;
		private long			m_durationNS;

		public AbstractPerformanceMeasure(CyclicBarrier barrier, int iMax)
		{
			m_barrier = barrier;
			m_iMax = iMax;
		}

		@Override
		public void run()
		{
			waitForBarrier(m_barrier);

				long			t1		= System.nanoTime();
				runInternal(m_iMax);
				long			t2		= System.nanoTime();
				m_durationNS			= t2 - t1;

			waitForBarrier(m_barrier);
		}
		abstract void runInternal(int iMaxPerPass);

		public long getDurationNS()
		{
			return m_durationNS;
		}

	}
	private static class PerformanceMeasure extends AbstractPerformanceMeasure
	{
		private IIncrementCounter		m_counter;

		public PerformanceMeasure(CyclicBarrier barrier, int iMax, IIncrementCounter counter)
		{
			super(barrier, iMax);
			m_counter = counter;
		}

		@Override
		void runInternal(int iMaxPerPass)
		{
			for (int i = 0; i < iMaxPerPass; i++)
			{
				m_counter.increment();
			}
		}
	}
}
