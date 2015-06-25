/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;

/**
 * Manages the connection between the list of servers and the MBean proxy
 */
public class BasicMBeanManager
{
	public static final String DEFAULT_JMX_PORT	= Integer.toString(BasicTools.BASE_INDEX + 7);

	private static final BasicLogger					LOGGER				= new BasicLogger(BasicMBeanManager.class);
	private static final Map<String, BasicMBeanProxy>	MBEAN_PROXY_MAP		= new HashMap<>();
	private static final JMXConnectThread				CONNECT_THREAD		= new JMXConnectThread();

	private BasicMBeanManager()
	{
		// private constructor to ensure Singleton
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static BasicMBeanProxy getMbeanProxy(String serverName)
	{
		return getMbeanProxy(serverName, DEFAULT_JMX_PORT);
	}
	public static BasicMBeanProxy getMbeanProxy(String serverName, String port)
	{
		synchronized (MBEAN_PROXY_MAP)
		{
			String key		= generateKey(serverName, port);
			BasicMBeanProxy		bean	= MBEAN_PROXY_MAP.get(key);
			if(null == bean)
			{
				// Get a new bean if the connection does not exist in the map.
				getLogger().info(BasicEvent.EVENT_MBEAN_CREATE,
						"Creating JMX connection (not connected yet) to %s", key);
				bean = new BasicMBeanProxy(serverName, port);
				MBEAN_PROXY_MAP.put(key, bean);
				CONNECT_THREAD.requestImmediateRun();
			}
			return bean;
		}
	}

	public static void releaseConnection(String serverName, String port)
	{
		synchronized (MBEAN_PROXY_MAP)
		{
			BasicMBeanProxy bean = MBEAN_PROXY_MAP.remove(generateKey(serverName, port));
			if (null != bean)
			{
				bean.releaseConnection();
			}
		}
	}

	private static String generateKey(String serverName, String port)
	{
		return String.format("%s:%s", serverName, port);
	}





	private static class JMXConnectThread extends AbstractScheduledThread
	{
		protected JMXConnectThread()
		{
			super(true);

			setName(getClass().getSimpleName());
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			Collection<BasicMBeanProxy>			list;
			synchronized (MBEAN_PROXY_MAP)
			{
				list = new ArrayList<>(MBEAN_PROXY_MAP.values());
			}

			for (BasicMBeanProxy proxy : list)
			{
				getLogger().debug(BasicEvent.EVENT_DEBUG, "Ensuring connection on " + proxy.toString());
				if (!proxy.isConnected())
				{
					String		key		= generateKey(proxy.getServerName(), proxy.getPort());
					try
					{
						proxy.connect();
						getLogger().info(BasicEvent.EVENT_MBEAN_CONNECT, "Connected (JMX) to %s", key);

						// "Allow" newly connected only if they are still in the map.
						// They could have been removed from the map after they were placed in the list...
						// If they are no longer in the map, they would become a connection leak.
						// This is expected to be an extremely rare condition.
						synchronized (MBEAN_PROXY_MAP)
						{
							if (!MBEAN_PROXY_MAP.containsKey(key))
							{
								proxy.releaseConnection();
							}
						}
					}
					catch (Exception exception)
					{
						getLogger().debug(BasicEvent.EVENT_MBEAN_CONNECT_FAILED, exception,
								"Connecting (JMX) to %s", key);
					}
				}
			}
		}
	}
}
