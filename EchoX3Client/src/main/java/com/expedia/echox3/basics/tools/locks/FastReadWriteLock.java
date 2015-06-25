/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;

import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IValueCounter;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

/**
 * This is the same as AtomicReadWriteLock, without the counters.
 * FastReadWriteLock should only be used when there is a specific need to NOT have counters...
 * ... and it is known that the lock will never be owned for more than a few micro-seconds ...
 * ... and it is known that there will be minimal contention.
 * For example, ConfigurationManager needs a lock, but counters need ConfigurationManager.
 * Note that AtomicReadWriteLock is incredibly fast with counters disabled.
 * Note also that FastReadWriteLock ALWAYS spins and never gives up its time slice.
 * Use with caution.
 */
public class FastReadWriteLock extends AbstractReadWriteLock
{
//	private static final String		SETTING_PREFIX				= FastReadWriteLock.class.getName();
//	private static final String		SETTING_LOOP_COUNT_ALERT	= SETTING_PREFIX + ".loopCountAlert";
/*
	It is too dangerous to have counters on this lock, even with delayed init.
	Do not enable the counter, use this counter VERY CAREFULLY at appropriate places.

	// Note that ONLY spin counts ABOVE s_loopCountAlert are recorded, by design,
	// to avoid enabling the counters too early (and to avoid undue logging of normal operation)
	private static IValueCounter	s_readSpinCounter		= null;
	private static IValueCounter	s_writeSpinCounter		= null;
	private static volatile long	s_loopCountAlert		= 1000;
//	private static volatile long	s_loopCountMax			= 0;
*/
	private final AtomicInteger		m_numberDispenser		= new AtomicInteger(1);
	private final AtomicInteger		m_numberReadyToServe	= new AtomicInteger(1);
	private final AtomicInteger		m_readerCount			= new AtomicInteger(0);
	private final AtomicInteger		m_writerCount			= new AtomicInteger(0);

	public FastReadWriteLock()
	{
		super(null);
	}
/*
	@SuppressWarnings("PMD")
	synchronized
	private static void initCounter()
	{
		if (null != s_readSpinCounter)
		{
			return;
		}

		String[]		nameList		= new String[3];
		nameList[0]		= LockCounterFamily.COUNTER_NAME_PREFIX;
		nameList[1]		= FastReadWriteLock.class.getSimpleName();

		StringGroup counterName		= new StringGroup(nameList);

		nameList[2]		= "ReadSpinCount";
		s_readSpinCounter = CounterFactory.getInstance().getValueCounter(counterName,
				LogarithmicHistogram.Precision.Coarse, CounterFactory.CounterRange.us,
				"Read spin aggregated for all FastReadWriteLocks");

		nameList[2]		= "WriteSpinCount";
		s_writeSpinCounter = CounterFactory.getInstance().getValueCounter(counterName,
				LogarithmicHistogram.Precision.Coarse, CounterFactory.CounterRange.us,
				"Write spin count aggregated for all FastReadWriteLocks");

		updateConfigurationStatic(null, 0, null);
		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, FastReadWriteLock::updateConfigurationStatic);
	}
	@SuppressWarnings("PMD.UnusedFormalParameter")
	private static void updateConfigurationStatic(String publisherName, long timeMS, Object event)
	{
		s_loopCountAlert = ConfigurationManager.getInstance().getLong(SETTING_LOOP_COUNT_ALERT, s_loopCountAlert);
	}

	public static IValueCounter getReadSpinCounter()
	{
		if (null == s_readSpinCounter)
		{
			initCounter();
		}
		return s_readSpinCounter;
	}

	public static IValueCounter getWriteSpinCounter()
	{
		if (null == s_writeSpinCounter)
		{
			initCounter();
		}
		return s_writeSpinCounter;
	}
*/
	@Override
	public IOperationContext lockRead()
	{
//		long		loopsWaited		=
				acquireReadLock();
//		s_loopCountMax = Math.max(s_loopCountMax, loopsWaited);
//		if (loopsWaited > s_loopCountAlert)
//		{
//			getReadSpinCounter().record(loopsWaited);
//		}

		return null;
	}

	@Override
	public void unlockRead(IOperationContext context, boolean isSuccess)
	{
		releaseReadLock();
	}
	@Override
	public IOperationContext lockWrite()
	{
//		long		loopsWaited		=
				acquireWriteLock();
//		s_loopCountMax = Math.max(s_loopCountMax, loopsWaited);
//		if (loopsWaited > s_loopCountAlert)
//		{
//			getWriteSpinCounter().record(loopsWaited);
//		}

		return null;
	}
	@Override
	public void unlockWrite(IOperationContext context, boolean isSuccess)
	{
		releaseWriteLock();
	}

	private long acquireReadLock()
	{
		long		loopsWaited		= 0;
		int			myNumber		= m_numberDispenser.getAndIncrement();

		loopsWaited += waitForMyNumber(myNumber);

		// If it is my turn, then everyone who has to start is already started:
		// 		No-one can start until m_numberReadyToServe is increased.
		loopsWaited += waitForNoWriter();

		// Done, have the lock.
		m_readerCount.incrementAndGet();
		m_numberReadyToServe.incrementAndGet();		// The next writer can run when this writer releases.

		return loopsWaited;
	}
	private void releaseReadLock()
	{
		m_readerCount.decrementAndGet();
	}
	private long acquireWriteLock()
	{
		long		loopsWaited		= 0;
		int			myNumber		= m_numberDispenser.getAndIncrement();

		loopsWaited += waitForMyNumber(myNumber);
		// If it is my turn, then everyone who has to start is already started:
		// 		No-one can start until m_numberReadyToServe is increased.
		//		Therefore, the order of wait is irrelevant.
		loopsWaited += waitForNoReader();
		loopsWaited += waitForNoWriter();

		// Done, have the lock.
		m_writerCount.incrementAndGet();
		m_numberReadyToServe.incrementAndGet();		// The next writer can run when this writer releases.

		return loopsWaited;
	}
	private void releaseWriteLock()
	{
		m_writerCount.decrementAndGet();
	}
	private long waitForMyNumber(int myNumber)
	{
		return waitForNumber(myNumber, m_numberReadyToServe);
	}
	private long waitForNoWriter()
	{
		return waitForNumber(0, m_writerCount);
	}
	private long waitForNoReader()
	{
		return waitForNumber(0, m_readerCount);
	}
	private long waitForNumber(int number, AtomicInteger atomic)
	{
		long		loopCount		= 0;
		while (number != atomic.get())
		{
			loopCount++;
		}
		return loopCount;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
