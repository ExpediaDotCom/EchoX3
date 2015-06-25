/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.util.LinkedList;
import java.util.List;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

public abstract class AbstractBaseThread extends Thread
{
	public enum ThreadStatus
	{
		Starting,
		Waiting,
		RunRequested,
		Running,
		ExitRequested,
		Terminated
	}

	private static final BasicLogger		LOGGER					= new BasicLogger(AbstractBaseThread.class);

	private static final List<AbstractBaseThread>	THREAD_LIST				= new LinkedList<>();
	private static final ConfigurationManager		CONFIGURATION_MANAGER	= ConfigurationManager.getInstance();

	private final String					m_settingPrefix;
	private volatile ThreadStatus			m_threadStatus = ThreadStatus.Starting;

	protected AbstractBaseThread()
	{
		this(null);
	}
	protected AbstractBaseThread(String settingPrefix)
	{
		PublisherManager.register(ConfigurationManager.getPublisherName(), this::updateConfiguration);

		m_settingPrefix = null == settingPrefix ? getClass().getName() : settingPrefix;

		addThread();
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static List<AbstractBaseThread> getBaseThreadList()
	{
		return THREAD_LIST;
	}

	private void addThread()
	{
		synchronized (getBaseThreadList())
		{
			getBaseThreadList().add(this);
		}
	}
	public void removeThread()
	{
		cleanupOnExit();
		getLogger().info(BasicEvent.EVENT_THREAD_EXITING, "Thread %s exiting.", getName());

		setThreadStatus(ThreadStatus.Terminated);
		synchronized (getBaseThreadList())
		{
			getBaseThreadList().remove(this);
		}
	}
	/**
	 * Extending threads have the option to override this method
	 * if they want to perform some exit cleanup.
	 */
	protected void cleanupOnExit()
	{
		PublisherManager.deregister(ConfigurationManager.getPublisherName(), this::updateConfiguration);
	}

	public static ConfigurationManager getConfigurationManager()
	{
		return CONFIGURATION_MANAGER;
	}

	public String getSettingPrefix()
	{
		return m_settingPrefix;
	}

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		updateConfiguration();
	}
	protected void updateConfiguration()
	{
		// no-op, so extending classes do not have to implement if they do not require it.
	}


	public void setThreadStatus(ThreadStatus status)
	{
		if (ThreadStatus.ExitRequested.ordinal() <= m_threadStatus.ordinal())
		{
			if (ThreadStatus.Terminated.equals(status))
			{
				m_threadStatus = status;
			}
/*
			else
			{
				// Ignore these requests, they sometimes happen in normal operations (e.g. when cancelling at startup)
				// throw new IllegalStateException("Attempting to move away from " + ThreadStatus.ExitRequested);
			}
*/
		}
		else
		{
			m_threadStatus = status;
		}
	}

	public ThreadStatus getThreadStatus()
	{
		return m_threadStatus;
	}

	public static void terminateAll()
	{
		synchronized (THREAD_LIST)
		{
			for (AbstractBaseThread thread : THREAD_LIST)
			{
				thread.terminate();
			}
		}
	}
	public void terminate()
	{
		setThreadStatus(ThreadStatus.ExitRequested);
	}
}
