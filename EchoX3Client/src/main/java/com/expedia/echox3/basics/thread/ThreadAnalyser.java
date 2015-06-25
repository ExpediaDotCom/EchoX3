/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;

@SuppressWarnings("PMD")
public class ThreadAnalyser extends AbstractScheduledThread
{
	private static final ThreadAnalyser INSTANCE;

	private static final String FORMAT_REPORT	= "Thread(%-40s): %s";
	private static final double			PERCENT_MIN	= 0.01;

	private Map<String, ThreadStatistics> m_statisticsMap		= null;
	private Map<Long, ThreadStatistics> m_mapPrevious		= new HashMap<Long, ThreadStatistics>();

	static
	{
		ThreadMXBean bean	= ManagementFactory.getThreadMXBean();

		if (bean.isThreadCpuTimeSupported())
		{
			if (!bean.isThreadCpuTimeEnabled())
			{
				bean.setThreadCpuTimeEnabled(true);
			}

			getLogger().debug(BasicEvent.EVENT_DEBUG, "CPU time is supported, launching analyser thread.");
			INSTANCE = new ThreadAnalyser();
		}
		else
		{
			getLogger().debug(BasicEvent.EVENT_DEBUG, "CPU time is NOT supported, NOT launching analyser thread.");
			INSTANCE = null;
		}
	}

	private ThreadAnalyser()
	{
		super(true);

		setName("ThreadAnalyser");
		setDaemon(true);
		start();
	}

	public static ThreadAnalyser getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void updateConfiguration()
	{
		super.updateConfiguration();
		ThreadStatistics.setPeriodMS(getThreadSchedule().getPeriodMS());
	}

	public Map<String, ThreadStatistics> getStatisticsMap()
	{
		return m_statisticsMap;
	}


	// Remove trailing digits (and '/'), to merge TreadPool-1/2 with ThreadPool-2/2, etc.
	private static String cleanThreadName(String threadName)
	{
		int 		len;
		for (len = threadName.length() - 1; len > 0; len--)
		{
			char		ch		= threadName.charAt(len);
			if ('/' != ch && !Character.isDigit(ch) && ('.' != ch))
			{
				break;
			}
		}
		threadName = threadName.substring(0, len + 1);

		return threadName;
	}

	@Override
	public void runOnce(long timeMS)
	{
		Map<String, ThreadStatistics> mapDelta		= new TreeMap<String, ThreadStatistics>();

		ThreadMXBean	bean			= ManagementFactory.getThreadMXBean();
		long []			threadList		= bean.getAllThreadIds();

		for (long id : threadList)
		{
			ThreadInfo threadInfo			= bean.getThreadInfo(id);
			if (null == threadInfo)
			{
				// The thread has gone away
				continue;
			}
			String threadName			= threadInfo.getThreadName();
			ThreadStatistics	statisticsCurrent	= new ThreadStatistics(bean, id, threadInfo);
			ThreadStatistics	statisticsPrevious	= m_mapPrevious.get(id);
			if (null != statisticsPrevious)
			{
				String cleanName				= cleanThreadName(threadName);
				ThreadStatistics	statisticsDelta			= mapDelta.get(cleanName);
				if (null == statisticsDelta)
				{
					statisticsDelta = new ThreadStatistics();
					mapDelta.put(cleanName, statisticsDelta);
				}
				statisticsDelta.accumulate(statisticsPrevious, statisticsCurrent);
			}

			m_mapPrevious.put(id, statisticsCurrent);
		}

		m_statisticsMap = mapDelta;
		if (getLogger().isDebugEnabled())
		{
			report(mapDelta);
		}

	}

	private void report(Map<String, ThreadStatistics> mapDelta)
	{
		StringBuilder sb		= new StringBuilder("Thread activity report for ");
		sb.append(getThreadSchedule().toString());

		for (Map.Entry<String, ThreadStatistics> entry : mapDelta.entrySet())
		{
			String threadName		= entry.getKey();
			ThreadStatistics	statistics		= entry.getValue();

			double		totalPercent	= statistics.getTotalPercent();

			if (PERCENT_MIN <= totalPercent)
			{
				sb.append(BaseFileHandler.LINE_SEPARATOR);
				String message		= String.format(FORMAT_REPORT, threadName, statistics.toString());
				sb.append(message);
			}
		}

		getLogger().debug(BasicEvent.EVENT_DEBUG, sb.toString());
	}


	public static class ThreadStatistics
	{
		private static final String FORMAT_TO_STRING		= "Threads = %2d;"
				+ " Times = U(%5.2f %%) + K(%5.2f %%) = Total(%5.2f %%);"
				+ " Block = (%,9d; %,6d ms); Wait = (%,9d; %,6d ms)";

		private static long s_periodMS;

		int 		m_threadCount	= 0;

		long		m_blockCount	= 0;
		long		m_blockTimeMS	= 0;
		long		m_waitCount		= 0;
		long		m_waitTimeMS	= 0;
		long		m_userNS		= 0;
		long		m_totalNS		= 0;

		public ThreadStatistics()
		{

		}
		public ThreadStatistics(ThreadMXBean bean, long id, ThreadInfo threadInfo)
		{
			m_blockCount	= threadInfo.getBlockedCount();
			m_blockTimeMS = threadInfo.getBlockedTime();
			m_waitCount		= threadInfo.getWaitedCount();
			m_waitTimeMS = threadInfo.getWaitedTime();
			m_userNS		= bean.getThreadUserTime(id);
			m_totalNS		= bean.getThreadCpuTime(id);
		}

		public static void setPeriodMS(long periodMS)
		{
			s_periodMS = periodMS;
		}

		public static long getPeriodMS()
		{
			return s_periodMS;
		}

		public void accumulate(ThreadStatistics previous, ThreadStatistics current)
		{
			m_threadCount++;

			m_blockCount += (current.m_blockCount - previous.m_blockCount);
			m_blockTimeMS += (current.m_blockTimeMS - previous.m_blockTimeMS);
			m_waitCount += (current.m_waitCount - previous.m_waitCount);
			m_waitTimeMS += (current.m_waitTimeMS - previous.m_waitTimeMS);
			m_userNS += (current.m_userNS - previous.m_userNS);
			m_totalNS += (current.m_totalNS - previous.m_totalNS);
		}

		public void accumulate(ThreadStatistics statistics)
		{
			m_threadCount++;

			m_blockCount	+= statistics.m_blockCount;
			m_blockTimeMS	+= statistics.m_blockTimeMS;
			m_waitCount		+= statistics.m_waitCount;
			m_waitTimeMS	+= statistics.m_waitTimeMS;
			m_userNS		+= statistics.m_userNS;
			m_totalNS		+= statistics.m_totalNS;
		}

		public int getThreadCount()
		{
			return m_threadCount;
		}

		public long getBlockCount()
		{
			return m_blockCount;
		}

		public long getBlockTimeMS()
		{
			return m_blockTimeMS;
		}

		public long getWaitCount()
		{
			return m_waitCount;
		}

		public long getWaitTimeMS()
		{
			return m_waitTimeMS;
		}

		public long getPeriodNS()
		{
			return getPeriodMS() * 1000 * 1000;
		}

		public long getUserNS()
		{
			return m_userNS;
		}

		public double getUserMS()
		{
			return m_userNS / (1000.0 * 1000);
		}

		public double getUserPercent()
		{
			return getUserNS() * 100. / getPeriodNS();
		}

		public long getKernelNS()
		{
			return getTotalNS() - getUserNS();
		}

		public double getKernelMS()
		{
			return getTotalMS() - getUserMS();
		}

		public double getKernelPercent()
		{
			return getKernelNS() * 100. / getPeriodNS();
		}

		public long getTotalNS()
		{
			return m_totalNS;
		}

		public double getTotalMS()
		{
			return m_totalNS / (1000.0 * 1000);
		}

		public double getTotalPercent()
		{
			return getTotalNS() * 100. / getPeriodNS();
		}

		@Override
		public String toString()
		{
			String message		= String.format(FORMAT_TO_STRING,
					getThreadCount(),
					getUserPercent(), getKernelPercent(), getTotalPercent(),
					getBlockCount(), getBlockTimeMS(),
					getWaitCount(), getWaitTimeMS()
			);

			return message;
		}
	}
}
