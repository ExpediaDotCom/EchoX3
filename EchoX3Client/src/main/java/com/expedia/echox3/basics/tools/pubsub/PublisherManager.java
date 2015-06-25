/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.EchoThreadPoolHot;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.tools.time.WallClock;

public class PublisherManager
{
	/* package */ static final String	POOL_NAME_DEFAULT				= "DefaultThreadPool";

	private static final BasicLogger	LOGGER			= new BasicLogger(Publisher.class);

	private static final String 		PUBLISHER_COUNTER_NAME_FORMAT	= "Publisher.%s";
	private static final String			SETTING_PREFIX					= PublisherManager.class.getName();
	private static final String			SETTING_PREFIX_POOL				= SETTING_PREFIX + ".Publisher";
	private static final Map<String, IEchoThreadPool> THREAD_POOL_MAP
																		= new HashMap<>();

	private static final PublisherManager		INSTANCE				= new PublisherManager();

	static
	{
		String		poolFullName	= String.format(PUBLISHER_COUNTER_NAME_FORMAT, POOL_NAME_DEFAULT);
		THREAD_POOL_MAP.put(POOL_NAME_DEFAULT, new EchoThreadPoolHot(poolFullName));

		register(ConfigurationManager.getPublisherName(), PublisherManager::updateConfiguration);
	}

	public interface IEventListener
	{
		void receiveEvent(String publisherName, long timeMS, Object event);
	}

	/**
	 * Specific events implementing this interface will have the appropriate method(s) called.
	 */
	public interface IPublishedEvent
	{
		void publicationBegins(Object event, int listenerCount);
		void publicationEnds();
	}

	private PublisherManager()
	{
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}
	public static PublisherManager getInstance()
	{
		return INSTANCE;
	}

	public static void register(String name, IEventListener listener)
	{
		Publisher.getPublisher(name).register(listener);
	}
	public static void deregister(String name, IEventListener listener)
	{
		Publisher.getPublisher(name).deregister(listener);
	}

	public static void post(String publisherName, Object event)
	{
		Publisher.getPublisher(publisherName).post(event);
	}
	/**
	 * post places the notification in a queue that is processed by a thread pool.
	 * The processing of the notification by the subscribers is done in the thread of the thread pool.
	 * The post call returns immediately and is virtually free (execution time wise) to the publisher.
	 *
	 * @param publisherName	Obvious
	 * @param timeMS		Time of the notification
	 * @param event			Event relevant to the publisher
	 */
	public static void post(String publisherName, long timeMS, Object event)
	{
		Publisher.getPublisher(publisherName).post(timeMS, event);
	}

	/* package */ static IEchoThreadPool getThreadPool(String poolName)
	{
		IEchoThreadPool			threadPool;
		synchronized (THREAD_POOL_MAP)
		{
			threadPool = THREAD_POOL_MAP.get(poolName);
			if (null == threadPool)
			{
				threadPool = THREAD_POOL_MAP.get(POOL_NAME_DEFAULT);
			}
		}
		return threadPool;
	}

	/**
	 * Updates the configuration based on a set of entries in the properties files.
	 * Any pool MUST be pointed to by at least one publisher for it to make any sense.
	 *
	 * The format of the property file is...
	 * com.expedia.lodging.lsb.basics.tools.misc.EventPublisher.Publisher.BcpLim=RawTables
	 * com.expedia.lodging.lsb.basics.tools.misc.EventPublisher.Publisher.TableList=RawTables
	 * com.expedia.lodging.lsb.basics.tools.misc.EventPublisher.RawTables.PoolSize=2
	 * com.expedia.lodging.lsb.basics.tools.misc.EventPublisher.RawTables.QueueSize=10
	 *
	 * Where
	 * 		BcpLim and TableList are example of publisher names
	 * 		RawTables is the new ThreadPool, used by both publishers.
	 */
	@SuppressWarnings("PMD.UnusedFormalParameter")
	private static void updateConfiguration(String notifierName, long timeMS, Object event)
	{
		ConfigurationManager	configurationManager	= ConfigurationManager.getInstance();
		Set<String>				nameSet					= configurationManager.getSettingNameSet(SETTING_PREFIX_POOL);
		for (String poolNameSettingName : nameSet)
		{
			// Start with getting the publisherName -> PoolName
			//																						+ 1 for the "."
			String		publisherName	= poolNameSettingName.substring(SETTING_PREFIX_POOL.length() + 1);
			String		poolName		= configurationManager.getSetting(poolNameSettingName, null);
			Publisher	publisher		= Publisher.getPublisher(publisherName);
			if (null == poolName)
			{
				poolName = publisher.getThreadPoolName();
				getLogger().error(BasicEvent.EVENT_PUBLISHER_THREAD_POOL_MISSING,
						"Missing setting for name of ThreadPool of publisher %s (Setting name = %s). "
								+ "Continuing with previously assigned pool = %s.",
						publisherName, poolNameSettingName, poolName);
				continue;
			}

			// See if there is already a pool with that name
			synchronized (THREAD_POOL_MAP)
			{
				IEchoThreadPool		threadPool		= THREAD_POOL_MAP.get(poolName);
				if (null == threadPool)
				{
					// Create the new required pool...
					String poolFullName = String.format(PUBLISHER_COUNTER_NAME_FORMAT, poolName);
					threadPool = new EchoThreadPoolHot(poolFullName);

					// Tell everybody
					THREAD_POOL_MAP.put(poolName, threadPool);
					getLogger().info(BasicEvent.EVENT_PUBLISHER_THREAD_POOL_CREATE,
							"Created ThreadPool %s for publisher %s.",
							poolFullName, publisherName);
				}

				// Tell the publisher about the (potentially new) pool it should use.
				publisher.setThreadPool(poolName, threadPool);
			}
		}
	}
}
