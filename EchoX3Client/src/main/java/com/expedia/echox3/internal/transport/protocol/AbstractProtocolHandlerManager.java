/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;

public abstract class AbstractProtocolHandlerManager
{
	//CHECKSTYLE:OFF
	public static final String							NOTIFICATION_PROTOCOL_CHANGE	= "ProtocolChange";
	private static final String							SETTING_HANDLER_NAME_MIDDLE		= ".handler.";

	private static final BasicLogger					LOGGER							= new BasicLogger(AbstractProtocolHandlerManager.class);
	private static final ConfigurationManager			CONFIGURATION_MANAGER			= ConfigurationManager.getInstance();
	//CHECKSTYLE:ON

	private final String								m_settingPrefix;
	private final Map<String, AbstractProtocolHandler>	m_handlerMap					= new TreeMap<>();

	protected AbstractProtocolHandlerManager()
	{
		m_settingPrefix = getClass().getName();
		updateConfiguration();
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		updateConfiguration();
	}

	private void updateConfiguration()
	{
		boolean		isChanged		= false;

		synchronized (m_handlerMap)
		{
			String			settingNamePrefix	= m_settingPrefix + SETTING_HANDLER_NAME_MIDDLE;
			Set<String>		settingNameSet		= CONFIGURATION_MANAGER.getSettingNameSet(settingNamePrefix);
			Set<String>		protocolNameSet		= new TreeSet<>();
			for (String settingName : settingNameSet)
			{
				String						protocolName		= settingName.replace(settingNamePrefix, "");
				protocolNameSet.add(protocolName);
				String						settingValue		= CONFIGURATION_MANAGER.getSetting(settingName, null);
				AbstractProtocolHandler		protocolHandler		= m_handlerMap.get(protocolName);
				if (null == protocolHandler)
				{
					protocolHandler = createProtocolHandler(protocolName, settingValue);
					protocolHandler.updateConfiguration();
					m_handlerMap.put(protocolName, protocolHandler);
					isChanged = true;
				}
				// NOTE: A simple address change is handled locally by the ProtocolHandler.
			}

			// Remove any that is no longer required...
			Iterator<Map.Entry<String, AbstractProtocolHandler>>	iterator	= m_handlerMap.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<String, AbstractProtocolHandler>			entry		= iterator.next();
				String			protocolName		= entry.getKey();
				if (!protocolNameSet.contains(protocolName))
				{
					AbstractProtocolHandler		protocolHandler		= entry.getValue();
					protocolHandler.shutdown();
					iterator.remove();
					isChanged = true;
				}
			}
		}

		if (isChanged)
		{
			getPublisher().post(0, NOTIFICATION_PROTOCOL_CHANGE);
		}
	}
	protected abstract AbstractProtocolHandler createProtocolHandler(String protocolName, String description);
	public abstract Publisher getPublisher();

	// Methods for people wanting to listen to the protocol handlers (e.g. to register message handlers.
	public boolean registerMessageHandler(String protocolHandlerName, AbstractMessageHandler messageHandler)
	{
		synchronized (m_handlerMap)
		{
			AbstractProtocolHandler		protocolHandler		= m_handlerMap.get(protocolHandlerName);
			if (null != protocolHandler)
			{
				protocolHandler.registerMessageHandler(messageHandler);
				return true;
			}
			else
			{
				getLogger().info(BasicEvent.EVENT_MESSAGE_HANDLER_FAIL,
						"MessageHandler(%s) NOT listening to messages on missing protocol [%s].",
						messageHandler.getName(), protocolHandlerName);
				return false;
			}
		}
	}
	public boolean deregisterMessageHandler(String protocolHandlerName, AbstractMessageHandler messageHandler)
	{
		synchronized (m_handlerMap)
		{
			AbstractProtocolHandler		protocolHandler		= m_handlerMap.get(protocolHandlerName);
			if (null != protocolHandler)
			{
				protocolHandler.deregisterMessageHandler(messageHandler);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	public void registerProtocolHandler(AbstractProtocolHandler protocolHandler)
	{
		synchronized (m_handlerMap)
		{
			m_handlerMap.put(protocolHandler.getProtocolName(), protocolHandler);
		}
	}
	public void deregisterProtocolHandler(AbstractProtocolHandler protocolHandler)
	{
		synchronized (m_handlerMap)
		{
			m_handlerMap.remove(protocolHandler.getProtocolName());
		}
	}


	// Methods for people wanting to see what is in the map
	public Map<String, AbstractProtocolHandler> getProtocolHandlerMap()
	{
		Map<String, AbstractProtocolHandler>		map			= new TreeMap<>();
		synchronized (m_handlerMap)
		{
			map.putAll(m_handlerMap);
		}
		return map;
	}

	public AbstractProtocolHandler getProtocolHandler(String protocolHandlerName)
	{
		synchronized (m_handlerMap)
		{
			return m_handlerMap.get(protocolHandlerName);
		}
	}
}
