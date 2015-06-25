/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;


import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import static com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram.Precision;
import com.expedia.echox3.basics.collection.simple.IAccumulator;
import com.expedia.echox3.basics.collection.simple.SimpleAccumulator;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;

import com.sun.management.GarbageCollectionNotificationInfo;

/**
 * This class is responsible for measuring and exposing in JMX the GC data
 */
public class GarbageInfo implements GarbageInfoMBean
{
	public static final String BEAN_PREFIX							= "Memory";
	public static final String BEAN_TYPE_COLLECTOR					= "Collector";
	public static final String BEAN_TYPE_POOL						= "PoolInfo";

	public static final String PRIMARY_POOL_CONTAINS_YOUNG			= "Eden";
	public static final String PRIMARY_POOL_CONTAINS_OLD			= "Old";

	private static final GarbageInfo				INSTANCE_YOUNG;
	private static final GarbageInfo				INSTANCE_OLD;
	private static final GarbageLatchThread			LATCH_THREAD;

	private static final RuntimeMXBean RUNTIME_MX_BEAN					= ManagementFactory.getRuntimeMXBean();

	private static final Map<String, MemoryUsageWrapper>	MEMORY_USAGE_MAP	= new HashMap<>();

	private final GarbageCollectorMXBean	m_garbageBean;
	// Accumulators accumulate/latch the current value
	private final IAccumulator				m_accumulatorPauseCurrent	= new SimpleAccumulator();
	private IAccumulator					m_accumulatorPauseLatched	= m_accumulatorPauseCurrent.getBlankClone();
	private final IAccumulator				m_accumulatorClockCurrent	= new SimpleAccumulator();
	private IAccumulator					m_accumulatorClockLatched	= m_accumulatorClockCurrent.getBlankClone();
	private long							m_countTotal				= 0;
	private long							m_durationTotalMS			= 0;
	private long							m_latchTimeMS				= WallClock.getCurrentTimeMS();
	// Histogram accumulate value since beginning of time.
	private final IHistogram m_histogram	= new LogarithmicHistogram(1, 10 * 1000, Precision.Normal);

	private String							m_primaryPoolName;

	static
	{
		// TODO Refactor into methods to pick-up INSTANCE_*
		List<GarbageCollectorMXBean>	beanList			= ManagementFactory.getGarbageCollectorMXBeans();
		GarbageInfo						garbageYoung		= null;
		GarbageInfo						garbageOld			= null;
		for (GarbageCollectorMXBean bean : beanList)
		{
			String[]		poolNames		= bean.getMemoryPoolNames();
			if (null == garbageYoung)
			{
				garbageYoung	= getGarbageInfo(bean, poolNames, PRIMARY_POOL_CONTAINS_YOUNG);
			}

			if (null == garbageOld)
			{
				garbageOld	= getGarbageInfo(bean, poolNames, PRIMARY_POOL_CONTAINS_OLD);
			}
		}

		INSTANCE_YOUNG		= garbageYoung;
		INSTANCE_OLD		= garbageOld;
		LATCH_THREAD = new GarbageLatchThread();
	}

	public GarbageInfo()
	{
		m_garbageBean	= null;
		// for the static initializer in AbstractContainer
	}

	public GarbageInfo(GarbageCollectorMXBean garbageBean, String generation)
	{
		m_garbageBean = garbageBean;
		StringGroup mbeanNameList		= generateMbeanNameList(BEAN_TYPE_COLLECTOR, generation);

		BasicTools.registerMBean(this, null, mbeanNameList);
	}

	private static GarbageInfo getGarbageInfo(
			GarbageCollectorMXBean bean, String[] poolNames, String poolContainText)
	{
		String					primaryPoolName			= null;
		for (String poolName : poolNames)
		{
			if (poolName.contains(poolContainText))
			{
				primaryPoolName = poolName;
			}
		}
		if (null == primaryPoolName)
		{
			return null;
		}

		GarbageInfo				garbageInfo				= new GarbageInfo(bean, poolContainText);
		garbageInfo.m_primaryPoolName	= primaryPoolName;

		if (garbageInfo.getGarbageBean() instanceof NotificationEmitter)
		{
			NotificationEmitter				notificationEmitter	= (NotificationEmitter) garbageInfo.getGarbageBean();
			GarbageNotificationListener		listener			= new GarbageNotificationListener();
			notificationEmitter.addNotificationListener(listener, null, garbageInfo);
		}

		return garbageInfo;
	}

	public static StringGroup generateMbeanNameList(String beanType, String generation)
	{
		StringGroup mbeanNameList		= new StringGroup();
		mbeanNameList.append(BEAN_PREFIX);
		mbeanNameList.append(beanType);
		mbeanNameList.append(generation);
		return mbeanNameList;
	}

	@SuppressWarnings("unused")
	public static GarbageInfo getInstanceYoung()
	{
		return INSTANCE_YOUNG;
	}

	@SuppressWarnings("unused")
	public static GarbageInfo getInstanceOld()
	{
		return INSTANCE_OLD;
	}

	public GarbageCollectorMXBean getGarbageBean()
	{
		return m_garbageBean;
	}

	public IHistogram getHistogram()
	{
		return m_histogram;
	}

	private void latch(long timeMS)
	{
		m_accumulatorPauseCurrent.cloneAndReset(m_accumulatorPauseLatched);
		m_accumulatorClockCurrent.cloneAndReset(m_accumulatorClockLatched);
		m_latchTimeMS = timeMS;
	}

	@Override
	public String getCollectorName()
	{
		return m_garbageBean.getName();
	}
	@Override
	public String getPrimaryPoolName()
	{
		return m_primaryPoolName;
	}

	@Override
	public long getTotalCount()
	{
		return m_countTotal;
	}
	@Override
	public long getTotalDurationMS()
	{
		return m_durationTotalMS;
	}
	@Override
	public long getTotalElapsedMS()
	{
		return RUNTIME_MX_BEAN.getUptime();
	}
	@Override
	public String getTotalElapsed()
	{
		return TimeUnits.formatMS(getTotalElapsedMS());
	}
	@Override
	public double getTotalAverageMS()
	{
		return divideSafe(getTotalDurationMS(), getTotalCount());
	}
	@Override
	public double getTotalPeriodSec()
	{
		return divideSafe(getTotalElapsedMS() / 1000.0, getTotalCount());
	}
	@Override
	public double getTotalDutyCyclePercent()
	{
		return divideSafe(getTotalDurationMS() * 100.0, getTotalElapsedMS());
	}

	public long getLatchTimeMS()
	{
		return m_latchTimeMS;
	}
	public String getLatchDate()
	{
		return WallClock.formatTime(WallClock.FormatType.DateTime, WallClock.FormatSize.Large, m_latchTimeMS);
	}

	public long getCurrentCount()
	{
		return m_accumulatorPauseLatched.getCount();
	}
	public double getCurrentDurationMS()
	{
		return m_accumulatorPauseLatched.getSum();
	}
	public long getCurrentElapsedMS()
	{
		return LATCH_THREAD.getThreadSchedule().getPeriodMS();
	}
	public String getCurrentElapsed()
	{
		return TimeUnits.formatMS(getCurrentElapsedMS());
	}

	public double getPauseCurrentMinimumMS()
	{
		return m_accumulatorPauseLatched.getMin();
	}
	public double getPauseCurrentAverageMS()
	{
		return m_accumulatorPauseLatched.getAvg();
	}
	public double getPauseCurrentMaximumMS()
	{
		return m_accumulatorPauseLatched.getMax();
	}

	public double getClockCurrentMinimumMS()
	{
		return m_accumulatorClockLatched.getMin();
	}
	public double getClockCurrentAverageMS()
	{
		return m_accumulatorClockLatched.getAvg();
	}
	public double getClockCurrentMaximumMS()
	{
		return m_accumulatorClockLatched.getMax();
	}

	public double getCurrentPeriodSec()
	{
		return divideSafe(getCurrentElapsedMS() / 1000.0, getCurrentCount());
	}
	public double getCurrentDutyCyclePercent()
	{
		return divideSafe(getCurrentDurationMS() * 100.0, getCurrentElapsedMS());
	}

	private double divideSafe(double numerator, double denominator)
	{
		return 0 == denominator ? 0.0 : numerator / denominator;
	}

	public String[] getHistogramData()
	{
		IHistogram.BinData[]		binDataArray	= (IHistogram.BinData[]) m_histogram.getBinData().toArray();
		String[]					binDataText		= new String[binDataArray.length];
		int							index			= 0;
		for (IHistogram.BinData binData : binDataArray)
		{
			binDataText[index++] = binData.toString();
		}
		return binDataText;
	}

	@Override
	public String toString()
	{
		return String.format("%s on %s", m_garbageBean.getName(), m_primaryPoolName);
	}





	private static class GarbageNotificationListener implements NotificationListener
	{
		@Override
		public void handleNotification(Notification notification, Object handback)
		{
			if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
			{
				return;
			}
//			System.out.println("GC Notification received for " + handback);

			//get the information associated with this notification
			GarbageCollectionNotificationInfo info		=
					GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());

			long		durationRealTimeMS	= info.getGcInfo().getDuration();
/*
			String gctype = info.getGcAction();
			if ("end of minor GC".equals(gctype))
			{
				gctype = "Young Gen GC";
			}
			else if ("end of major GC".equals(gctype))
			{
				gctype = "Old Gen GC";
			}
*/

			Map<String, MemoryUsage>		usageMapBefore		= info.getGcInfo().getMemoryUsageBeforeGc();
//			showUsageMap("Before", usageMapBefore);
			Map<String, MemoryUsage> usageMapAfter		= info.getGcInfo().getMemoryUsageAfterGc();
//			showUsageMap("After", usageMapAfter);

			// Record the collection details
			if (handback instanceof GarbageInfo)
			{
				GarbageInfo		garbageInfo			= (GarbageInfo) handback;
				long			durationTotalMS		= garbageInfo.getGarbageBean().getCollectionTime();
				long			durationPauseMS		= durationTotalMS - garbageInfo.m_durationTotalMS;
//				System.out.println(String.format("%,5d/%,5d/%,5d ms for %s due to %s on %s",
//						durationPauseMS, durationRealTimeMS, durationTotalMS, info.getGcAction(), info.getGcCause(), info.getGcName()));
				garbageInfo.m_accumulatorPauseCurrent.record(durationPauseMS);
				garbageInfo.m_accumulatorClockCurrent.record(durationRealTimeMS);
				garbageInfo.m_histogram.record(durationPauseMS);
				garbageInfo.m_countTotal++;
				garbageInfo.m_durationTotalMS = durationTotalMS;
//				System.out.println("Garbage collection for collector " + garbageInfo.getCollectorName());
			}

			// Record the usage before details
			for (Map.Entry<String, MemoryUsage> entry : usageMapBefore.entrySet())
			{
				String					poolName		= entry.getKey();
				MemoryUsage				memoryUsage		= entry.getValue();
				MemoryUsageWrapper		wrapper			= getMemoryUsageWrapper(poolName, memoryUsage.getMax());
				wrapper.setUsedBefore(memoryUsage.getUsed());
			}
			// Record the usage after details
			for (Map.Entry<String, MemoryUsage> entry : usageMapAfter.entrySet())
			{
				String					poolName		= entry.getKey();
				MemoryUsage				memoryUsage		= entry.getValue();
				MemoryUsageWrapper		wrapper			= getMemoryUsageWrapper(poolName, memoryUsage.getMax());
				wrapper.setUsedAfter(memoryUsage.getUsed());
			}
		}
		private MemoryUsageWrapper getMemoryUsageWrapper(String poolName, long max)
		{
			MemoryUsageWrapper		wrapper			= MEMORY_USAGE_MAP.get(poolName);
			if (null == wrapper)
			{
				wrapper = new MemoryUsageWrapper(poolName, max);
				MEMORY_USAGE_MAP.put(poolName, wrapper);
			}
			return wrapper;
		}
/*
		private void showUsageMap(String name, Map<String, MemoryUsage> map)
		{
			System.out.println(name);
			for (Map.Entry<String, MemoryUsage> entry : map.entrySet())
			{
				System.out.println("\t" + entry.getKey());
				System.out.println("\t\t" + entry.getValue().toString());
			}
		}
*/
	}

	private static class GarbageLatchThread extends AbstractScheduledThread
	{
		protected GarbageLatchThread()
		{
			super(true);
			setName(GarbageLatchThread.class.getSimpleName());
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			INSTANCE_YOUNG.latch(timeMS);
			INSTANCE_OLD.latch(timeMS);
		}
	}

	public static class MemoryUsageWrapper implements MemoryUsageWrapperMBean
	{
		private StringGroup		m_name;
		private long			m_usedBefore;
		private long			m_usedAfter;
		private long			m_max;

		public MemoryUsageWrapper(String poolName, long max)
		{
			m_name	= generateMbeanNameList(BEAN_TYPE_POOL, poolName);
			m_max	= max;

			BasicTools.registerMBean(this, null, m_name);
		}

		public void setUsedBefore(long usedBefore)
		{
			m_usedBefore = usedBefore;
		}

		public void setUsedAfter(long usedAfter)
		{
			m_usedAfter = usedAfter;
		}

		@Override
		public long getUsedBefore()
		{
			return m_usedBefore;
		}

		@Override
		public long getUsedAfter()
		{
			return m_usedAfter;
		}

		@Override
		public long getMax()
		{
			return m_max;
		}
	}
	public interface MemoryUsageWrapperMBean
	{
		long		getUsedBefore();
		long		getUsedAfter();
		long		getMax();
	}
}
