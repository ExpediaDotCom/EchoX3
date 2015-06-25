/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandlerManager;

public abstract class AbstractMessageHandlerManager
{
	//CHECKSTYLE:OFF
	private static final String							SETTING_HANDLER_NAME_MIDDLE		= ".handler.";

	private static final BasicLogger					LOGGER							= new BasicLogger(AbstractMessageHandlerManager.class);
	private static final ConfigurationManager			CONFIGURATION_MANAGER			= ConfigurationManager.getInstance();
	//CHECKSTYLE:ON

	private final String								m_settingPrefix;
	// Map HandlerName -> MessageHandler
	private final Map<String, AbstractMessageHandler>	m_handlerMap					= new TreeMap<>();

	protected AbstractMessageHandlerManager()
	{
		m_settingPrefix = getClass().getName();
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);

		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public abstract AbstractProtocolHandlerManager getProtocolHandlerManager();

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		synchronized (m_handlerMap)
		{
			String			settingNamePrefix	= m_settingPrefix + SETTING_HANDLER_NAME_MIDDLE;
			Set<String>		settingNameSet		= CONFIGURATION_MANAGER.getSettingNameSet(settingNamePrefix);
			Set<String>		handlerNameSet		= new TreeSet<>();
			for (String settingName : settingNameSet)
			{
				String						handlerName			= settingName.replace(settingNamePrefix, "");
				String						className			= CONFIGURATION_MANAGER.getSetting(settingName, null);
				AbstractMessageHandler		messageHandler		= m_handlerMap.get(handlerName);
				if (null == messageHandler)
				{
					try
					{
						messageHandler = createMessageHandler(handlerName, className);
					}
					catch (BasicException e)
					{
						getLogger().error(e.getBasicEvent(), e, "Message handler not created");
						continue;
					}
					m_handlerMap.put(handlerName, messageHandler);
				}
				handlerNameSet.add(handlerName);
				// NOTE: A simple address change is handled locally by the MessageHandler.
			}

			// Remove any that is no longer required...
			Iterator<Map.Entry<String, AbstractMessageHandler>>	iterator	= m_handlerMap.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<String, AbstractMessageHandler>	entry					= iterator.next();
				String										handlerName		= entry.getKey();
				if (!handlerNameSet.contains(handlerName))
				{
					AbstractMessageHandler		messageHandler		= entry.getValue();
					messageHandler.close();
					iterator.remove();
				}
			}

			updateEachHandler(publisherName, timeMS, event);
		}
	}
	@SuppressWarnings("unchecked")
	private AbstractMessageHandler createMessageHandler(String name, String className) throws BasicException
	{
		try
		{
			Class						clazz			= Class.forName(className);
			Constructor					ctor			= clazz.getConstructor(String.class);
			AbstractMessageHandler		messageHandler	= (AbstractMessageHandler) ctor.newInstance(name);
			return messageHandler;
		}
		catch (Exception e)
		{
			throw new BasicException(BasicEvent.EVENT_TODO, e,
					"No appropriate constructor for message handler %s, using class %s", name, className);
		}
	}

	// Methods for people wanting to see what is in the map
	public Map<String, AbstractMessageHandler> getMessageHandlerMap()
	{
		Map<String, AbstractMessageHandler>		map			= new TreeMap<>();
		synchronized (m_handlerMap)
		{
			map.putAll(m_handlerMap);
		}
		return map;
	}

	public AbstractProtocolHandler getProtocolHandler(Class clazz)
	{
		AbstractProtocolHandlerManager	protocolManager		= getProtocolHandlerManager();
		AbstractMessageHandler			messageHandler		= getMessageHandler(clazz);
		String							protocolName		= messageHandler.getProtocolHandlerName();
		AbstractProtocolHandler			protocolHandler		= protocolManager.getProtocolHandler(protocolName);

		return protocolHandler;
	}

	// There is only one instance of each type of high level MessageHandler
	public AbstractMessageHandler getMessageHandler(Class clazz)
	{
		synchronized (m_handlerMap)
		{
			for (AbstractMessageHandler handler : m_handlerMap.values())
			{
				if (handler.getClass().equals(clazz))
				{
					return handler;
				}
			}
		}
		return null;
	}

	protected void updateEachHandler(String publisherName, long timeMS, Object event)
	{
		synchronized (m_handlerMap)
		{
			for (AbstractMessageHandler handler : m_handlerMap.values())
			{
				handler.updateConfiguration(publisherName, timeMS, event);
			}
		}
	}
}
