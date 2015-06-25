/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.util.function.Supplier;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.internal.transport.request.AbstractRequest;
import com.expedia.echox3.internal.transport.request.MessageType;

public abstract class AbstractMessageHandler
{
	private static final BasicLogger		LOGGER				= new BasicLogger(AbstractMessageHandler.class);

	private final String			m_name;
	private final String			m_settingPrefix;
	private String[]				m_nameList				= new String[3];
	@SuppressWarnings("unchecked")
	private final ObjectPool<AbstractRequest>[]
									m_objectPoolList		= new ObjectPool[MessageType.MESSAGE_COUNT_MAX];

	private String					m_protocolHandlerName	= null;		// Protocol to which this handler listens

	protected AbstractMessageHandler(String name)
	{
		m_name = name;
		m_settingPrefix = getClass().getName() + "." + name;

		m_nameList[0] = getClass().getSimpleName(); // getGenericClassName();
//		m_nameList[1] = getClass().getSimpleName().replace(getGenericClassName(), "");
		m_nameList[1] = m_name;

		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);
	}
	public abstract String getGenericClassName();

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public String getName()
	{
		return m_name;
	}

	public String getProtocolHandlerName()
	{
		return m_protocolHandlerName;
	}

	public void setProtocolHandlerName(String protocolHandlerName)
	{
		m_protocolHandlerName = protocolHandlerName;
	}

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		// Nothing to do here, let extending classes implement their own code.
	}

	public String getSettingPrefix()
	{
		return m_settingPrefix;
	}
	public String getSettingName(String shortName)
	{
		return m_settingPrefix + shortName;
	}

	@SuppressWarnings("unchecked")
	protected final void setObjectPool(MessageType messageNumber, Supplier<AbstractRequest> supplier)
	{
		AbstractRequest			request		= supplier.get();
		m_nameList[2] = request.getClass().getSimpleName();
		m_objectPoolList[messageNumber.getNumber()] = new ObjectPool(new StringGroup(m_nameList), supplier);
	}

	public AbstractRequest getRequest(MessageType messageType)
	{
		ObjectPool<AbstractRequest>		objectPool			= m_objectPoolList[messageType.getNumber()];
		if (null == objectPool)
		{
			return null;
		}

		AbstractRequest					clientRequest		= objectPool.get();
		return clientRequest;
	}

	abstract public boolean handleMessage(ReceiveMessage receiveMessage);

	public void receiveShutdownNotification(String protocolHandlerName)
	{
		// By default, do nothing, extending classes may choose to do something.
	}
	public void close()
	{
		for (ObjectPool<AbstractRequest> objectPool : m_objectPoolList)
		{
			if (null != objectPool)
			{
				objectPool.release();
			}
		}
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s on %s)", getClass().getSimpleName(), getName(), getProtocolHandlerName());
	}
}
