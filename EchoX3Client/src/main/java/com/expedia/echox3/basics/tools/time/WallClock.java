/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */
package com.expedia.echox3.basics.tools.time;

import java.util.Date;
import java.util.TimeZone;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.time.BasicMasterTime;
import com.expedia.echox3.basics.monitoring.time.BasicMasterTime.MasterType;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;


/**
 * This class is implemented as a singleton that synchronizes with the an external Time master
 * and provides with a truly synchronized time, regardless of the actual time on the local machine.
 *
 * The relevant data is exposed as an MBean (JMX).
 */
public class WallClock implements WallClockMBean
{
	// public so the test case can use these
	public static final String		SETTING_PREFIX						= WallClock.class.getName();
	public static final String		SETTING_NAME_OFFSET_THRESHOLD_MS	= SETTING_PREFIX + ".OffsetThresholdMS";
	public static final TimeZone	TIME_ZONE_LOCAL						= TimeZone.getDefault();

	public enum FormatType
	{
		Date,
		Time,
		DateTime
	}
	public enum FormatSize
	{
		Small,
		Medium,
		Large,
		XLarge
	}

	private static final Date			SCRATCH_DATE	= new Date();

	// CHECKSTYLE:OFF
	// [Type][Size]
	private static final String[][]		FORMATTER_LIST	=
			{
					// Date
					{
							"%1$tY-%1$tm-%1$td",
							"%1$ta %1$tY/%1$tm/%1$td",
							"%1$ta %1$tb %1$td, %1$tY",
							"%1$tA, %1$tB %1$td, %1$tY"
					},
					// Time
					{
							"%1$tH:%1$tM",
							"%1$tH:%1$tM:%1$tS",
							"%1$tH:%1$tM:%1$tS.%1$tL",
							"%1$tI:%1$tM:%1$tS.%1$tL %1$tp %1$tZ"
					},
					// DateTime
					{
							"%1$tY-%1$tm-%1$td @ %1$tH:%1$tM",
							"%1$ta %1$tY/%1$tm/%1$td @ %1$tH:%1$tM:%1$tS",
							"%1$ta %1$tb %1$td, %1$tY @ %1$tH:%1$tM:%1$tS.%1$tL",
							"%1$tA, %1$tB %1$td, %1$tY @ %1$tI:%1$tM:%1$tS.%1$tL %1$tp %1$tZ"
					}
			};
	// CHECKSTYLE:ON

	private static final BasicLogger		LOGGER				= new BasicLogger(WallClock.class);

	private static final StringGroup		MBEAN_NAME_LIST		= new StringGroup("WallClock");
	private static final WallClock			INSTANCE			= new WallClock();

	// setting a default value for first time.
	private static 			long					s_offsetThresholdMS		= 15 * 1000;
	private static final	ConfigurationManager	CONFIGURATION_MANAGER	= ConfigurationManager.getInstance();

	private BasicMasterTime		m_masterTimeLatest;			// Latest may be a failure, == current if success
	private BasicMasterTime		m_masterTimeCurrent;		// == Latest success
	private long				m_lastMeasuredTimeMS	= 0;
	private long				m_masterTimeOffsetMS	= 0;

	static
	{
		new ConfigurationListener();
	}

	// Private constructor to ensure singleton
	private WallClock()
	{
		new WallClockThread();

		BasicTools.registerMBean(this, null, getNameList());
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static StringGroup getNameList()
	{
		return MBEAN_NAME_LIST;
	}

	public static long getCurrentTimeMS()
	{
		return INSTANCE.getCurrentTimeMSInternal();
	}

	public static String formatTime(FormatType formatType, FormatSize formatSize, long timeMS)
	{
		synchronized (SCRATCH_DATE)
		{
			String format = FORMATTER_LIST[formatType.ordinal()][formatSize.ordinal()];
			SCRATCH_DATE.setTime(timeMS);
			String text = String.format(format, SCRATCH_DATE);
			return text;
		}
	}

	public static long getOffsetThresholdMS()
	{
		return s_offsetThresholdMS;
	}

	public static TimeZone getTimeZoneLocal()
	{
		return TIME_ZONE_LOCAL;
	}

	public static WallClock getInstance()
	{
		return INSTANCE;
	}
	private long getCurrentTimeMSInternal()
	{
		long		systemTime		= System.currentTimeMillis();
		long		actualTime		= systemTime - m_masterTimeOffsetMS;

		return actualTime;
	}

	public long getLocalTime()
	{
		return System.currentTimeMillis();
	}
	public String getLocalDate()
	{
		return WallClock.formatTime(FormatType.DateTime, FormatSize.Large, getLocalTime());
	}

	public long getOffsetMS()
	{
		return m_masterTimeOffsetMS;
	}
	public long getCorrectedTime()
	{
		return getCurrentTimeMSInternal();
	}
	public String getCorrectedDate()
	{
		return WallClock.formatTime(FormatType.DateTime, FormatSize.Large, getCorrectedTime());
	}

	public long getMeasuredTime()
	{
		return m_lastMeasuredTimeMS;
	}
	public Date getMeasuredDate()
	{
		return new Date(m_lastMeasuredTimeMS);
	}
	public String getMasterName()
	{
		return String.format("%s(%s)",
				m_masterTimeCurrent.getMasterType().name(),        // e.g. Infoblox
				m_masterTimeCurrent.getSourceName());			// e.g. IP address or ServerName
	}



	private class WallClockThread extends AbstractScheduledThread
	{
		protected WallClockThread()
		{
			super(false);		// Already measured in constructor, to ensure it is ready immediately.

			measure();
			setName(WallClockThread.class.getSimpleName());
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			measure();
		}

		private void measure()
		{
			m_masterTimeLatest = BasicMasterTime.measureTime(MasterType.Internal);
			if (m_masterTimeLatest.isSuccess())
			{
				m_masterTimeCurrent = m_masterTimeLatest;
				m_masterTimeOffsetMS = m_masterTimeCurrent.getOffsetMS();
				m_lastMeasuredTimeMS = getCurrentTimeMSInternal();
			}
		}
	}

	/**
	 * This method is responsible for updating the static member variables.
	 * It is called by the ConfigurationListener singleton defined at the bottom of this class
	 */
	private static void updateConfigurationStatic()
	{

		// First, make a simple set of the "current" sprocs...
		long	offsetThresholdMS	= CONFIGURATION_MANAGER.getLong(
											SETTING_NAME_OFFSET_THRESHOLD_MS,
											Long.toString(s_offsetThresholdMS)
										);

		if (offsetThresholdMS != s_offsetThresholdMS)
		{
			getLogger().info(BasicEvent.EVENT_WALL_CLOCK_CONFIGURATION_CHANGE,
					"WallClock configuration OffsetThresholdMS changed from %,d to %,d",
					s_offsetThresholdMS,
					offsetThresholdMS);
			s_offsetThresholdMS = offsetThresholdMS;
		}
	}

	private static class ConfigurationListener
	{
		public ConfigurationListener()
		{
			updateConfigurationStatic();
			PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::receiveEvent);
		}

		public void receiveEvent(String publisherName, long timeMS, Object event)
		{
			updateConfigurationStatic();
		}
	}
}
