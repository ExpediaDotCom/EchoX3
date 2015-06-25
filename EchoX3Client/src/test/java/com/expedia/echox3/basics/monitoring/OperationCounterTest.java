/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring;

import java.util.concurrent.CyclicBarrier;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;
import com.expedia.echox3.basics.tools.misc.BasicTools;

public class OperationCounterTest extends AbstractTestTools
{
	private static final CounterFactory		COUNTER_FACTORY		= CounterFactory.getInstance();

	@Test
	public void testOperation()
	{
		String					testName		= logTestName();

		int						sleepMS			= 100;
		IOperationCounter		counter			= COUNTER_FACTORY.getLogarithmicOperationCounter(
				new StringGroup(testName),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.ms,
				"");

		IOperationContext		context			= counter.begin();
		BasicTools.wait(sleepMS);
		double					durationMS		= context.end(true);
		assertEquals(sleepMS, durationMS, 25.0);
	}

	@Test
	public void testPerformance()
	{
		String					testName		= logTestName();

		IOperationCounter		counter			= COUNTER_FACTORY.getLogarithmicOperationCounter(
				new StringGroup(testName),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.ms,
				"");

		int		cPass			= 5;
		int		iMax			= 100 * 1000;
		int[]	cThreadList		= { 1, 2, 4 };

		for (int cThread : cThreadList)
		{
			measurePerformance(counter, cPass, iMax, cThread);
			measurePerformance(counter, cPass, iMax, cThread);
		}
	}

	private void measurePerformance(IOperationCounter counter, int cPass, int iMax, int cThread)
	{
		for (int i = 0; i < cPass; i++)
		{
			measurePerformance(counter, iMax, cThread);
		}
	}
	private void measurePerformance(IOperationCounter counter, int iMax, int cThread)
	{
		CyclicBarrier		barrier		= new CyclicBarrier(cThread + 1);

		AbstractPerformanceMeasure[]	measureList		= new AbstractPerformanceMeasure[cThread];
		for (int i = 0; i < cThread; i++)
		{
			measureList[i] = new OperationPerformanceMeasure(barrier, iMax, counter);
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
		reportPerformance(String.format("begin/end cycles w. %,d threads", cThread), durationNS, iMax * cThread, true);
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
	private static class OperationPerformanceMeasure extends AbstractPerformanceMeasure
	{
		private IOperationCounter		m_counter;

		public OperationPerformanceMeasure(CyclicBarrier barrier, int iMax, IOperationCounter counter)
		{
			super(barrier, iMax);
			m_counter = counter;
		}

		@Override
		void runInternal(int iMaxPerPass)
		{
			for (int i = 0; i < iMaxPerPass; i++)
			{
				IOperationContext		context		= m_counter.begin();
				context.end(true);
			}
		}
	}
}
