/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.simple.IAccumulator;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager.IEventListener;
import com.expedia.echox3.basics.tools.time.TimeUnits;

public abstract class BaseCounter implements IBaseCounter
{
	protected enum CounterVisibility
	{
		Invisible,
		Live,		// Running counter updated on every call from the counter call.
		JMX,		// Latched every few seconds and exposed via JMX
		Log			// Latched at longer intervals and used for logging
	}

	public static final String							COUNTER_DOMAIN				= BasicTools.MBEAN_DOMAIN;
	public static final String							SETTING_SUFFIX_IS_ENABLED	= "IsEnabled";
	private static final Map<String, IBaseCounter>		COUNTER_MAP					= new HashMap<>();

	private final CounterVisibility		m_visibility;
	private final StringGroup			m_name;
	private final String				m_isEnabledSettingName;
	private boolean						m_isEnabled					= true;
	private long						m_beanDurationMS			= 0;

	static
	{
		new ConfigurationListener();

		// A counter MAY get initialized before the configuration system is ready.
		// If so, postpone completing the startup sequence.
		// The ConfigurationListener will call completeStartup() when it gets notified
		// that the ConfigurationManager is ready.
		if (null != ConfigurationManager.getInstance().getPublisher())
		{
			completeStartup();
		}
	}

	protected BaseCounter(StringGroup name, CounterVisibility visibility)
	{
		m_name = name.getCopy();
		m_visibility = visibility;

		m_isEnabledSettingName = name.getString() + SETTING_SUFFIX_IS_ENABLED;
		if (CounterVisibility.Live.equals(visibility))
		{
			MBeanUpdateThread.getLogger().debug(BasicEvent.EVENT_COUNTER_CREATE,
					"Create counter %s, enabled via setting %s", m_name, m_isEnabledSettingName);
		}

		if (CounterVisibility.JMX.equals(m_visibility))
		{
			registerJmx();
		}

		// Sets m_isEnabled
		updateConfigurationInternal();

	}

	private static void completeStartup()
	{
		new MBeanUpdateThread();
		new LogUpdateThread();
	}

	@Override
	public void updateConfiguration()
	{
		updateConfigurationInternal();
	}
	private void updateConfigurationInternal()
	{
		boolean		isEnabled = ConfigurationManager.getInstance().getBoolean(
														m_isEnabledSettingName, Boolean.toString(m_isEnabled));
		setEnabled(isEnabled);
	}

	@Override
	public StringGroup getName()
	{
		return m_name;
	}

	@Override
	public boolean isEnabled()
	{
		return m_isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled)
	{
		m_isEnabled = isEnabled;
	}

	public CounterVisibility getVisibility()
	{
		return m_visibility;
	}

	public long getBeanDurationMS()
	{
		return m_beanDurationMS;
	}

	public void setBeanDurationMS(long beanDurationMS)
	{
		m_beanDurationMS = beanDurationMS;
	}

	// Important note: Typically, a counter will register for JMX its clone, which does not have a name :).
	private void registerJmx()
	{
		BasicTools.registerMBean(this, COUNTER_DOMAIN, m_name);
	}

	@Override
	public void close()
	{
		if (CounterVisibility.JMX.equals(m_visibility))
		{
			BasicTools.unregisterMBean(COUNTER_DOMAIN, m_name);
		}
	}

	public void addCounter(String name, IBaseCounter counter)
	{
		synchronized (COUNTER_MAP)
		{
			COUNTER_MAP.put(name, counter);
		}
	}

	private static class MBeanUpdateThread extends AbstractScheduledThread
	{
		private long		m_lastWakeup		= 0;

		protected MBeanUpdateThread()
		{
			super(true);

			setName("CounterMBeanUpdateThread");
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			if (0 == m_lastWakeup)
			{
				m_lastWakeup = timeMS;
				return;
			}

			long			durationMS		= timeMS - m_lastWakeup;
			synchronized (COUNTER_MAP)
			{
				for (IBaseCounter baseCounter : COUNTER_MAP.values())
				{
					if (baseCounter.isEnabled())
					{
						baseCounter.doBeanUpdate(durationMS);
					}
				}
			}
			m_lastWakeup = timeMS;
		}
	}


	private static class LogUpdateThread extends AbstractScheduledThread
	{
		private final BasicLogger		m_logger			= new BasicLogger(LogUpdateThread.class);
		private long					m_lastWakeup		= System.currentTimeMillis();

		protected LogUpdateThread()
		{
			super(true);

			setName("CounterLogUpdateThread");
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			if (0 == m_lastWakeup)
			{
				m_lastWakeup = timeMS;
				return;
			}

			long			durationMS		= timeMS - m_lastWakeup;
			synchronized (COUNTER_MAP)
			{
				for (IBaseCounter baseCounter : COUNTER_MAP.values())
				{
					if (baseCounter.isEnabled())
					{
						baseCounter.doLogUpdate(m_logger, durationMS);
					}
				}
			}
			m_lastWakeup = timeMS;
		}

	}
	protected void logHistogram(BasicLogger logger, long durationMS, IHistogram histogram)
	{
		StringBuilder sb				= new StringBuilder(1000);

		addHeaderToLogEntry(sb, durationMS);

		List<IHistogram.BinData> binDataList		= histogram.getBinData();
		for (IHistogram.BinData binData : binDataList)
		{
			sb.append(String.format("[%.2e, %.2e[=%d;", binData.getMin(), binData.getMax(), binData.getCount()));
		}
		logger.info(BasicEvent.EVENT_COUNTER_HISTOGRAM, sb.toString());
	}
	protected void logAccumulator(BasicLogger logger, long durationMS, IAccumulator accumulator)
	{
		StringBuilder sb				= new StringBuilder(1000);

		addHeaderToLogEntry(sb, durationMS);

		addItemToLogEntry(sb, "Count", accumulator.getCount());
		addItemToLogEntry(sb, "Min", accumulator.getMin());
		addItemToLogEntry(sb, "Avg", accumulator.getAvg());
		addItemToLogEntry(sb, "Max", accumulator.getMax());
		addItemToLogEntry(sb, "Std", accumulator.getStd());

		logger.info(BasicEvent.EVENT_COUNTER_ACCUMULATOR, sb.toString());
	}
	protected void addHeaderToLogEntry(StringBuilder sb, long durationMS)
	{
		sb.append("Domain=");
		sb.append(COUNTER_DOMAIN);
		sb.append(';');

		int					iName			= 0;
		for (String name : getName().getStringArray())
		{
			sb.append("Name-");
			sb.append(iName++);
			sb.append('=');
			sb.append(name);
			sb.append(';');
		}

		addItemToLogEntry(sb, "Duration", TimeUnits.formatMS(durationMS));

	}
	protected void addItemToLogEntry(StringBuilder sb, String name, Object value)
	{
		sb.append(String.format("%s=%s;", name, value.toString()));
	}


	private static class ConfigurationListener
	{
		public ConfigurationListener()
		{
			PublisherManager.register(ConfigurationManager.getPublisherName(), this::updateConfiguration);
		}

		public void updateConfiguration(String publisherName, long timeMS, Object event)
		{
			if (ConfigurationManager.getPublisherName().equals(publisherName))
			{
				if (null != event && ConfigurationManager.REASON_STARTUP_COMPLETE.equals(event))
				{
					completeStartup();
				}

				synchronized (COUNTER_MAP)
				{
					for (IBaseCounter counter : COUNTER_MAP.values())
					{
						counter.updateConfiguration();
					}
				}
			}
		}
	}
}
