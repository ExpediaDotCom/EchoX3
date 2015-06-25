/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.thread;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.AbstractConfigurationProvider;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.thread.AbstractScheduledThread.RunReport;
import com.expedia.echox3.basics.tools.misc.BasicTools;

/**
 * Do not use NIST time unless absolutely necessary.
 * Infoblox does just fine and should be used for all X-1 needs.
 */
public class ThreadTests extends AbstractTestTools
{
	@Test
	public void testHello() throws BasicException
	{
		logTestName();

		// The test case is a period of 1000 ms, offset 100 ms
		ThreadSchedule	schedule		= new ThreadSchedule(true, 1000, "ms", 100, "ms");

		// Tests based on "now", where 0 == lastCompletion
		validateWakeupTime(schedule,    0,    0,    0,  100);
		validateWakeupTime(schedule,   50,    0,    0,  100);
		validateWakeupTime(schedule,   99,    0,    0,  100);
		validateWakeupTime(schedule,  100,    0,    0, 1100);
		validateWakeupTime(schedule,  500,    0,    0, 1100);
		validateWakeupTime(schedule, 1099,    0,    0, 1100);

		// Tests based on lastCompletion
		validateWakeupTime(schedule,    0, 1100,    0, 2100);
		validateWakeupTime(schedule,    0, 1100,    1, 2100);
		validateWakeupTime(schedule,    0, 1100,  100, 2100);
		validateWakeupTime(schedule,    0, 1100,  999, 2100);
		validateWakeupTime(schedule,    0, 1100, 1000, 3100);
		validateWakeupTime(schedule,    0, 1100, 1500, 3100);
		validateWakeupTime(schedule,    0, 1100, 2150, 4100);
	}
	private void validateWakeupTime(
			ThreadSchedule schedule, long now, long lastWakeup, long lastDurationMS, long expectedWakeup)
	{
		RunReport	runReport			= new RunReport();
		runReport.set(lastWakeup, lastDurationMS * 1000 * 1000);
		long		actualWakeupMS		= AbstractScheduledThread.calculateNextWakeUp(now, runReport, schedule);
		assertEquals(
				String.format("Now = %d; Duration = %d", lastWakeup, lastDurationMS),
				expectedWakeup, actualWakeupMS);
	}


	// This test shows that a single thread pool has an upper limit of approx. 50,000 QPS
	// On my MacBook pro 2011. (4 cores + hyper-threading).
	// The results are similar (50K) with 4, 6 or 8 threads.
	// As soon as about 25K requests are passed through the pool,
	@Test
	public void testThreadPoolLimit()
	{
		String				testName			= logTestName();

		int					count				= 25 * 1000;
		measureThreadPoolLimit(testName, count, 4, count);
		measureThreadPoolLimit(testName, count, 6, count);
		measureThreadPoolLimit(testName, count, 8, count);

		measurePoolGroupLimit(testName, count, 1, 2, count);
		measurePoolGroupLimit(testName, count, 16, 2, count);
		measurePoolGroupLimit(testName, count, 12, 2, count);
		measurePoolGroupLimit(testName, count, 10, 2, count);
		measurePoolGroupLimit(testName, count, 10, 1, count);
		measurePoolGroupLimit(testName, count,  8, 3, count);
		measurePoolGroupLimit(testName, count,  6, 4, count);
		measurePoolGroupLimit(testName, count,  4, 6, count);
		measurePoolGroupLimit(testName, count,  3, 8, count);
	}
	private void measureThreadPoolLimit(String poolName, int itemCount, int poolSize, int queueSize)
	{
		IEchoThreadPool		threadPool		= SimpleThreadPool.createThreadPool(poolName, poolSize, queueSize);
		String				testName		= String.format("%s(T %,2d; Q %,d)", poolName, poolSize, queueSize);
		measureThreadPoolLimit(testName, threadPool, itemCount);
		threadPool.shutdown();
	}

	private void measurePoolGroupLimit(String name, int itemCount, int poolCount, int poolSize, int queueSize)
	{
		String				poolName	= String.format("%s(P %2d; T %2d; Q %d)", name, poolCount, poolSize, queueSize);
		IEchoThreadPool threadPool		= SimpleThreadPool.createThreadPoolGroup(
				poolName, poolCount, poolSize, queueSize);
		measureThreadPoolLimit(poolName, threadPool, itemCount);
	}

	private void measureThreadPoolLimit(String name, IEchoThreadPool threadPool, int count)
	{
		AtomicInteger		countDone		= new AtomicInteger(0);

		long				t1				= System.nanoTime();
		for (int i = 0; i < count; i++)
		{
			threadPool.execute(new MyFastRunnable(countDone));
		}

		while (!(count == countDone.get()))
		{
			BasicTools.sleepMS(10);
		}
		long				t2				= System.nanoTime();
		long				durationNS		= t2-t1;

		reportPerformance(name, durationNS, count, true);
	}
	private static final class MyFastRunnable implements Runnable
	{
		private AtomicInteger		m_countDone;

		public MyFastRunnable(AtomicInteger countDone)
		{
			m_countDone = countDone;
		}

		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		@Override
		public void run()
		{
			BasicTools.burnCpuUS(1);
			m_countDone.incrementAndGet();
		}
	}

	@Test
	public void testTrellisThreadPoolFunction()
	{
		String				name			= logTestName();

		EchoThreadPool pool			= new EchoThreadPool(name, 1, 1);
		pool.setThreadCount(3);
		assertEquals(3, pool.getActiveCount() + pool.getSleepingCount());

		pool.setThreadCount(4);
		assertEquals(4, pool.getActiveCount() + pool.getSleepingCount());

		pool.setThreadCount(2);
		assertEquals(2, pool.getActiveCount() + pool.getSleepingCount());
	}

	@Test
	public void testTrellisThreadPoolPerformance()
	{
		String				name			= logTestName();

		// Clean-up the output
		Logger.getLogger(AbstractConfigurationProvider.class.getName()).setLevel(Level.WARN);
		Logger.getLogger(EchoThreadPool.class.getName()).setLevel(Level.WARN);

		EchoThreadPool		pool		= new EchoThreadPool(name, 1, 10);
		int					count		= 200 * 1000;
		for (int i = 0; i < 3; i++)
		{
			measureTrellisPoolPerformance(pool, 1, 1000 * 1000, count);
			measureTrellisPoolPerformance(pool, 2, 1000 * 1000, count);
			measureTrellisPoolPerformance(pool, 4, 1000 * 1000, count);
			measureTrellisPoolPerformance(pool, 6, 1000 * 1000, count);
			measureTrellisPoolPerformance(pool, 8, 1000 * 1000, count);
		}
		pool.shutdown();
	}
	private void measureTrellisPoolPerformance(
			EchoThreadPool pool, int threadCount, int queueSize, int count)
	{
		pool.setThreadCount(threadCount);
		pool.setQueueSizeMax(queueSize);
		measureThreadPoolLimit(String.format("TrellisPool(T %,2d; Q %,d)", threadCount, queueSize), pool, count);
	}
}
