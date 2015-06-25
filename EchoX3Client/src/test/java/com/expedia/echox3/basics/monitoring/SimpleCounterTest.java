/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.basics.tools.locks.LockCounterFamily;
import com.expedia.echox3.basics.tools.locks.PlainReadWriteLock;

public class SimpleCounterTest extends AbstractTestTools
{
//	private static final CounterFactory		COUNTER_FACTORY		= CounterFactory.getInstance();

	@Test
	public void testPerformance()
	{
		String		testName	= logTestName();

		int		cPass			= 5;
		int		iMax			= 1000 * 1000;
//		int[]	cThreadList		= { 1, 2, 4, 6, 8 };
		int[]	cThreadList		= { 1, 2, 4 };

		StringGroup				nameList			= new StringGroup(testName);
		LockCounterFamily		counterFamily		= new LockCounterFamily(nameList);
		MagicNamedCounter		namedCounter		= new MagicNamedCounter(counterFamily);
		for (int cThread : cThreadList)
		{
			measurePerformance(new SynchronizedCounter(),	cPass, iMax, cThread);
			measurePerformance(new AtomicCounter(),			cPass, iMax, cThread);
			measurePerformance(new MagicReadCounter(),		cPass, iMax, cThread);
			measurePerformance(new PlainWriteCounter(),		cPass, iMax / 20, cThread);
			measurePerformance(new MagicWriteCounter(),	cPass, iMax, cThread);

			measurePerformance(namedCounter,			cPass, iMax, cThread);
		}
	}

	private void measurePerformance(ISimpleCounter counter, int cPass, int iMax, int cThread)	// NOPMD
	{
		for (int iPass = 0; iPass < cPass; iPass++)
		{
			measurePerformance(counter, iMax, cThread);
		}
	}
	private void measurePerformance(ISimpleCounter counter, int iMax, int cThread)
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
		reportPerformance(
				String.format("%s %,d threads", counter.getClass().getSimpleName(), cThread),
				durationNS, iMax * cThread, true);
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

	private interface ISimpleCounter
	{
		void set(long value);
	}
	private static class SynchronizedCounter implements ISimpleCounter
	{
		private long	m_value;		// NOPMD

		@Override
		public void set(long value)
		{
			synchronized (this)
			{
				m_value = value;
			}
		}
	}
	private static class AtomicCounter implements ISimpleCounter
	{
		private AtomicLong		m_value		= new AtomicLong(0);		// NOPMD

		@Override
		public void set(long value)
		{
			m_value.set(value);
		}
	}
	private static class MagicReadCounter implements ISimpleCounter
	{
		private long						m_value;		// NOPMD
		private AbstractReadWriteLock		m_lock		= AbstractReadWriteLock.createReadWriteLock();

		@Override
		public void set(long value)
		{
			IOperationContext	context		= m_lock.lockRead();
			m_value = value;
			m_lock.unlockRead(context, true);
		}
	}
	private static class MagicWriteCounter implements ISimpleCounter
	{
		private long						m_value;		// NOPMD
		private AbstractReadWriteLock		m_lock		= AbstractReadWriteLock.createReadWriteLock();

		@Override
		public void set(long value)
		{
			IOperationContext	context		= m_lock.lockWrite();
			m_value = value;
			m_lock.unlockWrite(context, true);
		}
	}
	private static class MagicNamedCounter implements ISimpleCounter
	{
		private long						m_value;		// NOPMD
		private AbstractReadWriteLock		m_lock;

		public MagicNamedCounter(LockCounterFamily counterFamily)
		{
			m_lock = AbstractReadWriteLock.createReadWriteLock(counterFamily);
		}

		@Override
		public void set(long value)
		{
			IOperationContext	context		= m_lock.lockRead();
			m_value = value;
			m_lock.unlockRead(context, true);
		}
	}
	private static class PlainWriteCounter implements ISimpleCounter
	{
		private long						m_value;		// NOPMD
		private AbstractReadWriteLock		m_lock		= new PlainReadWriteLock();

		@Override
		public void set(long value)
		{
			IOperationContext	context		= m_lock.lockWrite();
			m_value = value;
			m_lock.unlockWrite(context, true);
		}
	}

	private static class PerformanceMeasure extends AbstractPerformanceMeasure
	{
		private ISimpleCounter		m_counter;

		public PerformanceMeasure(CyclicBarrier barrier, int iMax, ISimpleCounter counter)
		{
			super(barrier, iMax);
			m_counter = counter;
		}

		@Override
		void runInternal(int iMaxPerPass)
		{
			for (int i = 0; i < iMaxPerPass; i++)
			{
				m_counter.set(System.nanoTime());
			}
		}
	}
}
