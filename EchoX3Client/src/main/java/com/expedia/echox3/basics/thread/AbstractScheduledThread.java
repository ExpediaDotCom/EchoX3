/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;

public abstract class AbstractScheduledThread extends AbstractBaseThread
{
	private boolean							m_runFirst;
	private final RunReport					m_scheduledReport	= new RunReport();
	private volatile ThreadSchedule			m_threadSchedule	= new ThreadSchedule(false, 15, "Min", 10, "Sec");
	private volatile Thread					m_sleepingThread	= null;
	private volatile long					m_nextWakeUpMS		= 0;

	protected AbstractScheduledThread(boolean runFirst)
	{
		this(runFirst, null);
	}

	protected AbstractScheduledThread(boolean runFirst, String settingPrefix)
	{
		super(settingPrefix);
		m_runFirst = runFirst;
	}

	public static List<AbstractScheduledThread> getScheduledThreadList()
	{
		List<AbstractScheduledThread>		list		= new LinkedList<>();
		for (AbstractBaseThread baseThread : getBaseThreadList())
		{
			if (baseThread instanceof AbstractScheduledThread)
			{
				list.add((AbstractScheduledThread) baseThread);
			}
		}
		return list;
	}

	@Override
	protected void updateConfiguration()
	{
		try
		{
			ThreadSchedule		schedule	=
									getConfigurationManager().getThreadSchedule(getSettingPrefix(), m_threadSchedule);
			if (!schedule.equals(m_threadSchedule))
			{
				getLogger().info(BasicEvent.EVENT_THREAD_SCHEDULE_CHANGE,
						"The schedule for thread %s has changed to %s", getName(), schedule.toString());

				setSchedule(schedule);
			}
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_THREAD_SCHEDULE_INVALID, exception,
					"Exception reading schedule using setting prefix %s. Continuing using previous schedule: %s.",
					getSettingPrefix(), m_threadSchedule.toString());
		}
	}
	protected void setSchedule(ThreadSchedule schedule)
	{
		m_threadSchedule = schedule;
		wakeupSleepingThread();
	}
	public void requestImmediateRun()
	{
		setThreadStatus(ThreadStatus.RunRequested);
		wakeupSleepingThread();
	}
	private void wakeupSleepingThread()
	{
		Thread sleepingThread		= m_sleepingThread;
		if (null != sleepingThread)
		{
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (sleepingThread)
			{
				if (null != m_sleepingThread)
				{
					sleepingThread.notify();
				}
			}
		}
	}

	@Override
	public void terminate()
	{
		super.terminate();
		wakeupSleepingThread();
	}

	public long getNextWakeUpMS()
	{
		return m_nextWakeUpMS;
	}

	public ThreadSchedule getThreadSchedule()
	{
		return m_threadSchedule;
	}

	public boolean isRunning()
	{
		return null == m_sleepingThread;
	}

	public long getLastWakeup()
	{
		return m_scheduledReport.getStartTimeMS();
	}

	public RunReport getScheduledReport()
	{
		return m_scheduledReport;
	}

	@Override
	public void run()
	{
		if (!ThreadStatus.RunRequested.equals(getThreadStatus()))
		{
			setThreadStatus(ThreadStatus.Waiting);
		}
		updateConfiguration();
		getLogger().info(BasicEvent.EVENT_THREAD_STARTING,
				"Starting thread %s with %s", getName(), getThreadSchedule().toString());

		waitForEnabled();
		if (m_runFirst)
		{
			performSingleRun(WallClock.getCurrentTimeMS());
		}

		while (true)		// Run forever!
		{
			waitForNextPass();
			// Always check for exit status after waiting before running.
			if (ThreadStatus.ExitRequested.equals(getThreadStatus()))
			{
				break;
			}

			// Use the scheduled wakeUp time, to allow synchronization across multiple systems
			performSingleRun(m_nextWakeUpMS);
		}

		removeThread();
	}
	public void performSingleRun(long timeMS)
	{
		setThreadStatus(ThreadStatus.Running);
		try
		{
			long		t1				= System.nanoTime();
			runOnce(timeMS);
			long		t2				= System.nanoTime();
			long		durationNS		= (t2 - t1);
			m_scheduledReport.set(timeMS, durationNS);
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_THREAD_EXCEPTION, exception,
					"Unexpected exception while doing work in scheduled thread.");
		}
		finally
		{
			if (ThreadStatus.Running.equals(getThreadStatus()))
			{
				// Don't change it if it is not the value set at the beginning...
				// e.g. it could be TerminationRequested
				setThreadStatus(ThreadStatus.Waiting);
			}
		}
	}

	abstract protected void runOnce(long timeMS);


	private void waitForEnabled()
	{
		while (!m_threadSchedule.isEnabled() && !ThreadStatus.ExitRequested.equals(getThreadStatus()))
		{
			m_sleepingThread = Thread.currentThread();
			BasicTools.wait(0);
			m_sleepingThread = null;
		}
	}

	private void waitForNextPass()
	{
		while (true)
		{
			waitForEnabled();
			if (ThreadStatus.ExitRequested.equals(getThreadStatus())
					|| ThreadStatus.RunRequested.equals(getThreadStatus()))
			{
				return;
			}
			if (0 == getThreadSchedule().getPeriodMS())
			{
				// Period of 0 ms == run continuously
				m_nextWakeUpMS = WallClock.getCurrentTimeMS();
				return;
			}

			long		now				= WallClock.getCurrentTimeMS();
			m_nextWakeUpMS				= calculateNextWakeUp(now, getScheduledReport(), getThreadSchedule());
			long		sleepMS			= m_nextWakeUpMS - now;
			if (sleepMS <= 0)
			{
				break;
			}
			else if (sleepMS > getThreadSchedule().getPeriodMS())
			{
//				long		sleepSavMS		= sleepMS;
				while (sleepMS > getThreadSchedule().getPeriodMS())
				{
					sleepMS -= getThreadSchedule().getPeriodMS();
				}
/* This occurs frequently on the WallClock thread which changes the clock
				BasicEvent.EVENT_THREAD_SCHEDULE.warn(getLogger(),
						"Attempting to wait too long @ %s when period is %s. Wait time reduced to %s",
						TimeUnits.formatMS(sleepSavMS),
						TimeUnits.formatMS(getThreadSchedule().getPeriodMS()),
						TimeUnits.formatMS(sleepMS));
*/
			}
			// Sleep the amount specified, even if it was modified in the else clause above.
			m_sleepingThread = Thread.currentThread();
			BasicTools.wait((int) sleepMS);
			m_sleepingThread = null;
		}

		long		periodMS		= m_threadSchedule.getPeriodMS();
		long		now				= WallClock.getCurrentTimeMS();
		long		tick			= (now / periodMS) * periodMS;
		long		wakeUpTime		= tick + m_threadSchedule.getOffsetMS();

		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_THREAD_SCHEDULE,
					"Thread waking up @ %s (%s /+ %s) = %s",
					new Date(tick).toString(),
					TimeUnits.formatMS(m_threadSchedule.getPeriodMS()),
					TimeUnits.formatMS(m_threadSchedule.getOffsetMS()),
					new Date(wakeUpTime).toString());
		}
	}

	/**
	 * Calculated the next time the thread should wake-up, based on the various parameter.
	 * Note that all settings are hot and the method may be called multiple time during the
	 * same sleep interval (e.g. if the schedule changes).
	 *
	 * The basic algorithm is that the thread wakes-up every period, based on the wall-clock,
	 * after the specified offset. An offset larger than the period is an illegal parameter and
	 * should be validated by the ThreadSchedule before a call is made to this method.
	 * For example, a period of 15 seconds with an offset of 4 seconds will result of 4 calls per minute
	 * at seconds = :04, :19, :34 and :49.
	 * The difficult part is if the system is slow and takes too long to work.
	 * For example, it period is 15 seconds, offset is 4 seconds and the work lasts 12 seconds.
	 * In this example, the work would start at :04 and end at :16 ... already in the next cycle.
	 * Worst, what if the work, on a particularly bad pass takes 26 or 32 seconds?
	 * This is resolved as follows (using the :04 pass of the example above):
	 * 		If the work completes before the next scheduled pass, including offset
	 * 			- the next pass occurs as scheduled
	 * 			- The :04 pass completes at :17, the :19 pass occurs as scheduled
	 * 		If the work completes later than the next scheduled pass, it is skipped
	 * 			- the next pass is scheduled for the next regularly scheduled interval
	 * 			- The :04 pass completes at :26, the next pass will occur at :34
	 *
	 * The algorithm described above means that the "next wakeup" is always
	 * the nearest following regularly scheduled time.
	 *
	 * Note that this method is static method so it can be extensively tested!
	 *
	 * @param now				Current time
	 * @param runReport			Description of the last run
	 * @param threadSchedule	Schedule requested of this thread
	 * @return					Recommended next wake-up time
	 */
	public static long calculateNextWakeUp(long now, RunReport runReport, ThreadSchedule threadSchedule)
	{
		// When did the last run complete?
		long		lastCompletion			= runReport.getCompletionTimeMS();
		// Start with that time
		long		baseTime;
		if (0 == lastCompletion)
		{
			baseTime = now;
			runReport.set(now, 0);		// Record the new "official" last completion, to not always restart from 0.
		}
		else
		{
			baseTime = lastCompletion;
		}
		long		estimatePeriodBlock		= (baseTime / threadSchedule.getPeriodMS()) * threadSchedule.getPeriodMS();
		long		wakeupTimeMS			= estimatePeriodBlock + threadSchedule.getOffsetMS();

		// Make sure to return a wakeupTimeMS for the next pass, not the one just completed
		// Also, don't skip a pass when called multiple times or when called 1 ms after this pass is due.
		while (wakeupTimeMS <= baseTime)
		{
			// Should run through the loop only once
			wakeupTimeMS += threadSchedule.getPeriodMS();
		}
		return wakeupTimeMS;
	}

	@Override
	public String toString()
	{
		return String.format("%s %s", getName(),
				null == getThreadSchedule() ? "Schedule not set" : getThreadSchedule().toString());
	}

	public static class RunReport
	{
		private long		m_startTimeMS	= 0;
		private long		m_durationNS	= 0;

		public void set(long time, long durationNS)
		{
			m_startTimeMS = time;
			m_durationNS = durationNS;
		}

		public long getStartTimeMS()
		{
			return m_startTimeMS;
		}

		public long getDurationNS()
		{
			return m_durationNS;
		}
		public long getDurationUS()
		{
			return m_durationNS / 1000;
		}
		public long getDurationMS()
		{
			return m_durationNS / (1000 * 1000);
		}

		public long getCompletionTimeMS()
		{
			return getStartTimeMS() + getDurationMS();
		}
	}
}
