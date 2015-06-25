/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;

import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

/**
 * AtomicReadWriteLock is a non-reentrant ReadWriteLock that is 10x more efficient
 * than Java's ReentrantReadWriteLock when used appropriately.
 * If you have continuous contention by more than a few threads on the same object,
 * ... revisit your design.
 * If you hold a lock for more than 1 milli-second ... revisit your design.
 *
 * Within these guidelines, AtomicReadWriteLock is fast, well instrumented
 * and does zero allocation during a lock/unlock cycle.
 */
public class AtomicReadWriteLock extends AbstractReadWriteLock
{
	// This boolean is used during local testing by the development team.
	private static final boolean	IS_WAIT_ENABLED				= true;

	private static final String		SETTING_PREFIX				= AtomicReadWriteLock.class.getName();
	private static final String		SETTING_SPIN_DURATION_MAX	= SETTING_PREFIX + ".SpinDurationMaxUS";
	private static long				s_spinDurationMaxUS			= 25;
	private static long				s_spinDurationMaxNS			= s_spinDurationMaxUS * 1000;

	// Note that Integer rollover is irrelevant since waitForNumber tests for == and not < or >.
	private final AtomicInteger		m_numberDispenser		= new AtomicInteger(1);
	private final AtomicInteger		m_numberReadyToServe	= new AtomicInteger(1);
	private final AtomicInteger		m_readerCount			= new AtomicInteger(0);
	private final AtomicInteger		m_writerCount			= new AtomicInteger(0);

	private final AtomicInteger		m_waiterCount			= new AtomicInteger(0);

	static
	{
		updateConfigurationStatic(null, 0, null);
		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, AtomicReadWriteLock::updateConfigurationStatic);
	}

	public AtomicReadWriteLock(LockCounterFamily counterFamily)
	{
		super(counterFamily);
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private static void updateConfigurationStatic(String publisherName, long timeMS, Object event)
	{
		s_spinDurationMaxUS =
				ConfigurationManager.getInstance().getLong(SETTING_SPIN_DURATION_MAX, s_spinDurationMaxUS);
		s_spinDurationMaxNS = s_spinDurationMaxUS * 1000;
	}

	@Override
	public IOperationContext lockRead()
	{
		IOperationContext		context		= getCounterFamily().beginReadWait();
//		long					loopCount	=
											  acquireReadLock();
		getCounterFamily().end(context, true);
		wakeupAsNeeded();
//		getCounterFamily().recordRead(loopCount);

		return getCounterFamily().beginReadHold();
	}
	@Override
	public void unlockRead(IOperationContext context, boolean isSuccess)
	{
		releaseReadLock();
		wakeupAsNeeded();

		getCounterFamily().end(context, isSuccess);
	}

	@Override
	public IOperationContext lockWrite()
	{
		IOperationContext		context		= getCounterFamily().beginWriteWait();
//		long					loopCount	=
											  acquireWriteLock();
		getCounterFamily().end(context, true);
//		wakeupAsNeeded();		// Nobody to wakeup when acquiring the write lock
//		getCounterFamily().recordWrite(loopCount);

		return getCounterFamily().beginWriteHold();
	}
	@Override
	public void unlockWrite(IOperationContext context, boolean isSuccess)
	{
		releaseWriteLock();
		wakeupAsNeeded();

		getCounterFamily().end(context, isSuccess);
	}
/*
	private int getQueueLength()
	{
		return m_numberDispenser.get() - m_numberReadyToServe.get();
	}
*/
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
		long		waitStartNS		= System.nanoTime();
		while (number != atomic.get())
		{
			loopCount++;

			if (IS_WAIT_ENABLED)
			{
				long		nowNS		= System.nanoTime();
				long		durationNS	= nowNS - waitStartNS;
				if (s_spinDurationMaxNS < durationNS)
				{
					// Spin long enough, enter wait mode...
					synchronized (this)
					{
						if (number == atomic.get())
						{
							break;
						}

						m_waiterCount.incrementAndGet();
						try
						{
								wait();
						}
						catch (InterruptedException e)
						{
							// Do nothing, continue through the loop
						}
						m_waiterCount.decrementAndGet();
					}
				}
			}
		}
		return loopCount;
	}
	private void wakeupAsNeeded()
	{
		if (IS_WAIT_ENABLED)
		{
			synchronized (this)
			{
				if (0 != m_waiterCount.get())
				{
					// Notify all, to ensure the next number is notified. Notify does not guarantee the order.
					this.notifyAll();
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getCounterFamily().getName().toString());
	}
}
