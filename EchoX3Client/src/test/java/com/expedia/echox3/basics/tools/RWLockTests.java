/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.thread.AbstractBaseThread;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.basics.tools.locks.AtomicReadWriteLock;
import com.expedia.echox3.basics.tools.locks.BasicReadWriteLock;
import com.expedia.echox3.basics.tools.locks.FastReadWriteLock;
import com.expedia.echox3.basics.tools.locks.LockCounterFamily;
import com.expedia.echox3.basics.tools.locks.PlainReadWriteLock;
import com.expedia.echox3.basics.tools.misc.BasicTools;

//import static org.junit.Assert.assertEquals;

public class RWLockTests extends AbstractTestTools
{
	private static final String[]					GLOBAL_LOCK_NAME	=
			{ RWLockTests.class.getSimpleName(), "Global" };
	private static final AbstractReadWriteLock GLOBAL_LOCK = AbstractReadWriteLock.createReadWriteLock(
					new LockCounterFamily(new StringGroup(GLOBAL_LOCK_NAME)));

	public static AbstractReadWriteLock getLock()
	{
		return GLOBAL_LOCK;
	}

	@BeforeClass
	public static void beforeClass()
	{
		// Load configuration...
		ConfigurationManager.getInstance();
	}

	@Test
	public void testHello()
	{
		logTestName();

		validateSingleLock(true , 10, 10);
		validateSingleLock(true , 10, 20);
		validateSingleLock(true , 20, 10);
		validateSingleLock(false, 10, 10);
		validateSingleLock(false, 10, 20);
		validateSingleLock(false, 20, 10);
	}
	private void validateSingleLock(boolean isWrite, long timeAsk, long timeRelease)
	{
		long			timeStart	= getStartTime();
		RWLockThread	thread		= new RWLockThread("Single", isWrite, timeStart, timeAsk, timeAsk, timeRelease);
		thread.waitUntilDone();
		thread.validate(5);
	}

	@Test
	public void testMultipleRead()
	{
		logTestName();

		long			timeStart	= getStartTime();
		RWLockThread[]	threadList	= new RWLockThread[3];
		threadList[0] = new RWLockThread("R0", false, timeStart, 20, 20, 30);
		threadList[1] = new RWLockThread("R1", false, timeStart, 20, 20, 30);
		threadList[2] = new RWLockThread("R1", false, timeStart, 20, 20, 30);

		validateThreadList(threadList, 5);
	}

	@Test
	public void testMultipleWrite()
	{
		logTestName();

		long			timeStart	= getStartTime();
		RWLockThread[]	threadList	= new RWLockThread[3];
		threadList[0] = new RWLockThread("W0", true, timeStart, 20, 20, 50);
		threadList[1] = new RWLockThread("W1", true, timeStart, 25, 50, 60);
		threadList[2] = new RWLockThread("W1", true, timeStart, 30, 60, 70);

		validateThreadList(threadList, 5);
	}

	@Test
	public void testRWR()
	{
		logTestName();

		long			timeStart	= getStartTime();
		RWLockThread[]	threadList	= new RWLockThread[5];
		threadList[0] = new RWLockThread("R0", false,	timeStart, 20, 20, 50);
		threadList[1] = new RWLockThread("R1", false,	timeStart, 25, 25, 60);
		threadList[2] = new RWLockThread("W2", true,	timeStart, 30, 60, 70);
		threadList[3] = new RWLockThread("R3", false,	timeStart, 35, 70, 80);
		threadList[4] = new RWLockThread("R4", false,	timeStart, 40, 70, 90);

		validateThreadList(threadList, 5);
	}

	@Test
	public void testWRW()
	{
		logTestName();

		long			timeStart	= getStartTime();
		RWLockThread[]	threadList	= new RWLockThread[5];
		threadList[0] = new RWLockThread("W0", true,	timeStart, 20, 20, 50);
		threadList[1] = new RWLockThread("W1", true,	timeStart, 25, 50, 60);
		threadList[2] = new RWLockThread("R2", false,	timeStart, 30, 60, 70);
		threadList[3] = new RWLockThread("W3", true,	timeStart, 35, 70, 80);
		threadList[4] = new RWLockThread("W4", true,	timeStart, 40, 80, 90);

		validateThreadList(threadList, 5);
	}

	@Test
	public void testSinglePerformance()
	{
		String						testName	= logTestName();

		StringGroup					name		= new StringGroup(testName);
		LockCounterFamily			counter		= new LockCounterFamily(name);
		AbstractReadWriteLock[]		lockList	= new AbstractReadWriteLock[4];
		lockList[0] = new BasicReadWriteLock(counter);
		lockList[1] = new AtomicReadWriteLock(counter);
		counter.setEnabled(false);
		lockList[2] = new AtomicReadWriteLock(counter);
		lockList[3] = new FastReadWriteLock();

		int		iMax		= 10;
		int		cont		= 10 * 1000;
		for (int i = 0; i < iMax; i++)
		{
			for (int iLock = 0; iLock < lockList.length; iLock++)
			{
				measureSinglePerformance(lockList[iLock], cont);
			}
		}
	}
	private void measureSinglePerformance(AbstractReadWriteLock lock, int count)
	{
		long		t1			= System.nanoTime();
		for (int i = 0; i < count; i++)
		{
			IOperationContext		context		= lock.lockRead();
			lock.unlockRead(context, true);
		}
		long		t2			= System.nanoTime();
		long		durationNS	= t2 - t1;
		reportPerformance(lock.getClass().getSimpleName() + " Lock/Unlock cycle", durationNS, count, true);
	}
	@Test
	public void testMultiPerformance()
	{
		String						testName	= logTestName();
		StringGroup					name0		= new StringGroup(testName + ".0");
		StringGroup					name1		= new StringGroup(testName + ".1");
		StringGroup					name2		= new StringGroup(testName + ".2");
//		StringGroup					name3		= new StringGroup(testName + ".3");
		LockCounterFamily			counter0	= new LockCounterFamily(name0);
		LockCounterFamily			counter1	= new LockCounterFamily(name1);
		LockCounterFamily			counter2	= new LockCounterFamily(name2);
//		LockCounterFamily			counter3	= new LockCounterFamily(name3);
		counter0.setEnabled(false);

		AbstractReadWriteLock[]		lockList	= new AbstractReadWriteLock[5];
		lockList[0] = new FastReadWriteLock();
		lockList[1] = new AtomicReadWriteLock(counter0);
		lockList[2] = new AtomicReadWriteLock(counter1);
		lockList[3] = new PlainReadWriteLock();
		lockList[4] = new BasicReadWriteLock(counter2);

		StringBuilder		sbHeader		= new StringBuilder(132);
		sbHeader.append("Thread cnt   ");
		for (int i = 0; i < lockList.length; i++)
		{
			if (null != lockList[i])
			{
				sbHeader.append(String.format("%7s ",
						lockList[i].getClass().getSimpleName().replace("ReadWriteLock", "")));
			}
		}
		getLogger().info(BasicEvent.EVENT_TEST, sbHeader.toString());

		int[]						threadCountList		= { 1, 2, 3, 4, 6, 8 };
		int							durationMS			= 1000;

		// Warm-up pass
		for (int i = 0; i < lockList.length; i++)
		{
			if (null != lockList[i])
			{
				measureMultiPerformance(lockList[i], 2, durationMS);
			}
		}

		for (int threadCount : threadCountList)
		{
			StringBuilder		sb		= new StringBuilder(132);
			sb.append(String.format("%,2d threads = ", threadCount));
			for (int i = 0; i < lockList.length; i++)
			{
				if (null != lockList[i])
				{
					long nsPer = measureMultiPerformance(lockList[i], threadCount, durationMS);
					sb.append(String.format("%,7d ", nsPer));
				}
			}
			sb.append("ns / Lock/Unlock cycle");
			getLogger().info(BasicEvent.EVENT_TEST, sb.toString());
		}
	}
	public long measureMultiPerformance(AbstractReadWriteLock lock, int threadCount, long durationMS)
	{
		PerformanceRWLockThread[]		threadList		= new PerformanceRWLockThread[threadCount + 1];
		// Thread 0 is the write thread
		threadList[0] = new PerformanceRWLockThread(lock, durationMS, false, 100);
		for (int i = 0; i < threadCount; i++)
		{
			threadList[i + 1] = new PerformanceRWLockThread(lock, durationMS, true, 0);
		}
		// Wait for ALL the thread to be done
		for (int i = 0; i < threadCount; i++)
		{
			threadList[i].waitUntilDone();
		}

		// Count only the READ threads
		long		count		= 0;
		for (int i = 0; i < threadCount; i++)
		{
			count += threadList[i + 1].getLoopCount();
		}
		String		message		= String.format("%s(%,d)", lock.getClass().getSimpleName(), threadCount);
		long		nsPer		= reportPerformance(message, durationMS * 1000 * 1000, count, false);
		return nsPer;
	}




	private long getStartTime()
	{
		long			now			= System.currentTimeMillis();
		long			timeStart	= now - (now % 100) + 200;
		return timeStart;
	}
	private void validateThreadList(RWLockThread[] threadList, int tolerance)
	{
		for (RWLockThread thread : threadList)
		{
			thread.waitUntilDone();
			if (!thread.m_isDone)
			{
				thread.validate(tolerance);
			}
		}
	}

	public static class RWLockThread extends AbstractBaseThread
	{
		private volatile boolean	m_isWrite;
		private volatile long		m_timeStart;
		private volatile long		m_timeAsk;
		private volatile long		m_expectedGetTime;
		private volatile long		m_actualGet;
		private volatile long		m_timeRelease;
		private volatile boolean	m_isDone			= false;
		private volatile long		m_actualRelease;

		public RWLockThread(String name,
				boolean isWrite, long timeStart, long timeAsk, long expectedGetTime, long timeRelease)
		{
			m_isWrite = isWrite;
			m_timeStart = timeStart;
			m_timeAsk = timeAsk;
			m_expectedGetTime = expectedGetTime;
			m_timeRelease = timeRelease;

			setName(String.format("%s[%s]", name, toString()));
			start();
		}

		@Override
		public void run()
		{
			waitUntil(m_timeStart + m_timeAsk);

//			getLogger().info(BasicEvent.EVENT_TEST, "Asking for lock: %s", toString());
			IOperationContext	context			= m_isWrite ? getLock().lockWrite() : getLock().lockRead();

//			getLogger().info(BasicEvent.EVENT_TEST, "Got        lock: %s", toString());
			m_actualGet = System.currentTimeMillis() - m_timeStart;
			waitUntil(m_timeStart + m_timeRelease);

//			getLogger().info(BasicEvent.EVENT_TEST, "Releasing  lock: %s", toString());
			if (m_isWrite)
			{
				getLock().unlockWrite(context, true);
			}
			else
			{
				getLock().unlockRead(context, true);
			}
			m_actualRelease = System.currentTimeMillis() - m_timeStart;
			m_isDone = true;
		}
		private void waitUntil(long timeMS)
		{
			while (System.currentTimeMillis() < timeMS)
			{
				BasicTools.sleepMS(1);
			}
		}

		public long getActualGet()
		{
			return m_actualGet;
		}

		public long getTimeRelease()
		{
			return m_timeRelease;
		}

		public long getActualRelease()
		{
			return m_actualRelease;
		}

		public void waitUntilDone()
		{
			while (!m_isDone)
			{
				BasicTools.sleepMS(5);
			}
		}

		public void validate(int tolerance)
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Validating %s to within %d", toString(), tolerance);
			assertTrue(getName(), getActualGet() >= m_expectedGetTime - tolerance);
			assertTrue(getName(), getActualGet() <=  m_expectedGetTime + tolerance);
			assertTrue(getName(), getActualRelease() >= Math.max(getActualGet(), getTimeRelease()) - tolerance);
			assertTrue(getName(), getActualRelease() <= Math.max(getActualGet(), getTimeRelease()) + tolerance);
		}

		@Override
		public final String toString()
		{
			return String.format("%s(%s: %d, %d/%d, %d/%d)",
					m_isWrite ? "WriteLock" : "ReadLock ",
					m_isDone ? "Done" : "In progress",
					m_timeAsk, m_expectedGetTime, m_actualGet, m_timeRelease, m_actualRelease);
		}
	}

	public static class PerformanceRWLockThread extends AbstractBaseThread
	{
		private AbstractReadWriteLock	m_lock;
		private long					m_durationMS;
		private boolean					m_isRead;
		private int						m_sleepMS;

		private volatile long			m_loopCount		= 0;
		private volatile boolean		m_isDone		= false;

		public PerformanceRWLockThread(AbstractReadWriteLock lock, long durationMS, boolean isRead, int sleepMS)
		{
			m_lock = lock;
			m_durationMS = durationMS;
			m_isRead = isRead;
			m_sleepMS = sleepMS;

			start();
		}

		@Override
		public void run()
		{
			long		timeStartMS		= System.currentTimeMillis();
			long		timeEndMS		= timeStartMS + m_durationMS;
			while (System.currentTimeMillis() < timeEndMS)
			{
				m_loopCount++;
				if (m_isRead)
				{
					IOperationContext		context		= m_lock.lockRead();
					m_lock.unlockRead(context, true);
				}
				else
				{
					IOperationContext		context		= m_lock.lockWrite();
					m_lock.unlockWrite(context, true);
				}
				if (0 != m_sleepMS)
				{
					BasicTools.sleepMS(m_sleepMS);
				}
			}
			m_isDone = true;
		}

		public void waitUntilDone()
		{
			while (!m_isDone)
			{
				BasicTools.sleepMS(5);
			}
		}

		public long getLoopCount()
		{
			return m_loopCount;
		}
	}

}
