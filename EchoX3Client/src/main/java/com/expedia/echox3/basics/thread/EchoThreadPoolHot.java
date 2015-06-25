/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.thread;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.configuration.ConfigurationManager.SelfTuningInteger;
import com.expedia.echox3.basics.tools.pubsub.Publisher;

public class EchoThreadPoolHot extends EchoThreadPool
{
	public static final String					SETTING_PREFIX				= EchoThreadPool.class.getName();
	public static final String					SETTING_NAME_FORMAT			= "%s.%s.%s";
	public static final String					SETTING_NAME_THREAD_COUNT	= "threadCount";
	public static final String					SETTING_NAME_QUEUE_SIZE		= "queueSize";

	private final SelfTuningInteger		m_threadCount;
	private final SelfTuningInteger		m_queueSizeMax;

	public EchoThreadPoolHot(String poolName)
	{
		super(poolName);

		String	threadCountName		= getSettingName(SETTING_NAME_THREAD_COUNT);
		m_threadCount				= new SelfTuningInteger(threadCountName);
		String	queueSizeName		= getSettingName(SETTING_NAME_QUEUE_SIZE);
		m_queueSizeMax				= new SelfTuningInteger(queueSizeName);

		setThreadCount(m_threadCount.getCurrentValue());
		setQueueSizeMax(m_queueSizeMax.getCurrentValue());

		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	public final void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		if (m_threadCount.updateConfiguration())
		{
			setThreadCount(m_threadCount.getCurrentValue());
		}
		if (m_queueSizeMax.updateConfiguration())
		{
			setQueueSizeMax(m_queueSizeMax.getCurrentValue());
		}
	}
	protected String getSettingName(String settingName)
	{
		return String.format(SETTING_NAME_FORMAT, SETTING_PREFIX, getName(), settingName);
	}
}
