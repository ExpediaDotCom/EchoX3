/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.time;


import java.util.Set;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.time.WallClock;

/**
 * Worker class that knows how to talk the NTP protocol.
 * Typically, one would call measureOffsetMS or measureTime.
 * The first gives a plain answer while the second also tells where the answer came from.
 *
 * For all X-1 needs, only Expedia's Infoblox should be used as the Master; do not use NIST.
 *
 * 		offset = LocalTime - MasterTime
 */
public abstract class BasicMasterTime
{
	private static final BasicLogger			LOGGER		= new BasicLogger(BasicMasterTime.class);

	public enum MasterType
	{
		Internal,
		InternetNist
	}

	private static final String SYNC_OBJECT		= "Synchronization object";

	private MasterType				m_masterType;
	private String					m_sourceName			= null;
	private BasicException			m_exception				= null;
	private long					m_offsetMS				= 0;

	private volatile String[]		m_hostList;

	protected BasicMasterTime(MasterType masterType)
	{
		m_masterType = masterType;

		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public MasterType getMasterType()
	{
		return m_masterType;
	}

	public String getSourceName()
	{
		return m_sourceName;
	}

	public boolean isSuccess()
	{
		return null == m_exception;
	}
	public BasicException getException()
	{
		return m_exception;
	}

	public long getOffsetMS()
	{
		return m_offsetMS;
	}

	public static BasicMasterTime measureTime()
	{
		BasicMasterTime			masterTime		= InternalMasterTime.getInstance();
		if (0 == masterTime.getHostList().length)
		{
			masterTime = InternetMasterTime.getInstance();
		}

		masterTime.measureInternal();

		return masterTime;
	}
	public static BasicMasterTime measureTime(MasterType masterType)
	{
		BasicMasterTime masterTime;
		switch (masterType)
		{
		default:
		case Internal:
			masterTime = InternalMasterTime.measure();
			break;

		case InternetNist:
			masterTime = InternetMasterTime.measure();
			break;
		}
		return masterTime;
	}

	public static long measureOffsetMS(MasterType masterType) throws BasicException
	{
		BasicMasterTime		masterTime		= measureTime(masterType);

		if (masterTime.isSuccess())
		{
			return masterTime.getOffsetMS();
		}
		else
		{
			throw masterTime.getException();
		}
	}


	protected void measureInternal()
	{
		// Ensure only one measurement at a time...
		// Sufficient and simplifies the socket code.
		// NOTE: InternetMasterTime depends on getHostList() being called inside a lock.

		synchronized (SYNC_OBJECT)
		{
			for (int i = 0; i < m_hostList.length; i++)		// Only try so many times.
			{
				m_sourceName = getHostName(i);

				try
				{
					SntpClient		client		= new SntpClient(m_sourceName);
					client.measure();

					Throwable		throwable		= client.getThrowable();
					if (null != throwable)
					{
						// set on failure, but keep trying...
						m_exception = new BasicException(BasicEvent.EVENT_MASTER_CLOCK_EXCEPTION,
								"Throwable while measuring time", throwable);
					}
					else
					{
						// Exit on first success
						SntpMessage		message		= client.getResponse();
						m_exception = null;
						m_offsetMS = message.getOffset();
						break;
					}
				}
				catch (Throwable throwable)
				{
					m_exception = new BasicException(BasicEvent.EVENT_MASTER_CLOCK_EXCEPTION,
							"Unexpected throwable while measuring time", throwable);
				}
			}
		}
		getLogger().debug(BasicEvent.EVENT_TODO, "Mesured %s", toString());
	}

	@SuppressWarnings("PMD.MethodReturnsInternalArray")
	abstract public String getHostName(int attemptNumber);

	public String[] getHostList()
	{
		return m_hostList;
	}

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		ConfigurationManager	configurationManager	= ConfigurationManager.getInstance();
		Set<String>				settingNameList			= configurationManager.getSettingNameSet(getClass().getName());
		String[]				hostList				= new String[settingNameList.size()];
		int						i						= 0;
		for (String settingName : settingNameList)
		{
			String			hostName		= configurationManager.getSetting(settingName, null);
			hostList[i++] = hostName;
		}
		m_hostList = hostList;
	}


	@Override
	public String toString()
	{
		if (isSuccess())
		{
			return String.format("%s(%s): %,d ms", getMasterType(), getSourceName(), getOffsetMS());
		}
		else
		{
			return String.format("%s(%s): Failure = %s/%s",
					getMasterType(), getSourceName(),
					m_exception.getClass().getSimpleName(), m_exception.getMessageChain());
		}
	}
}
