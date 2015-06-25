/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager.IEventListener;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager.IPublishedEvent;

public class Publisher
{
	private static final BasicLogger				LOGGER			= new BasicLogger(Publisher.class);
	private static final Map<String, Publisher>		PUBLISHER_MAP	= new HashMap<>();

	private final String				m_name;
	private final List<PublisherManager.IEventListener>
										m_listenerList			= new LinkedList<>();

	// The default thread pool is ALWAYS present.
	private String						m_threadPoolName;
	private IEchoThreadPool				m_threadPool;

	public Publisher(String name)
	{
		this(name, PublisherManager.POOL_NAME_DEFAULT);
	}
	public Publisher(String name, String threadPoolName)
	{
		m_name			= name;
		StringGroup counterName		= new StringGroup();
		counterName.append(Publisher.class.getSimpleName());
		counterName.append(name);

		m_threadPoolName = threadPoolName;

		synchronized (PUBLISHER_MAP)
		{
			PUBLISHER_MAP.put(name, this);
		}
	}

	public static Publisher getPublisher(String name)
	{
		synchronized (PUBLISHER_MAP)
		{
			Publisher publisher = PUBLISHER_MAP.get(name);
			if (null == publisher)
			{
				publisher = new Publisher(name);
				PUBLISHER_MAP.put(name, publisher);
			}
			return publisher;
		}
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public String getName()
	{
		return m_name;
	}

	/* package */ String getThreadPoolName()
	{
		return m_threadPoolName;
	}

	/* package */ IEchoThreadPool getThreadPool()
	{
		if (null == m_threadPool)
		{
			m_threadPool = PublisherManager.getThreadPool(m_threadPoolName);
		}
		return m_threadPool;
	}
	/* package */ void setThreadPoolName(String threadPoolName)
	{
		m_threadPoolName = threadPoolName;
	}
	/* package */ void setThreadPool(String poolName, IEchoThreadPool threadPool)
	{
		m_threadPoolName	= poolName;
		m_threadPool		= threadPool;

		if (null != poolName && !poolName.equals(m_threadPoolName))
		{
			getLogger().info(BasicEvent.EVENT_PUBLISHER_THREAD_POOL_ASSIGN,
					"Publisher %s assigned to ThreadPool %s",
					m_name, m_threadPoolName);
		}
	}

	public void register(PublisherManager.IEventListener listener)
	{
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_DEBUG, "register(%s, %s)", getName(), listener.toString());
		}

		synchronized (m_listenerList)
		{
			m_listenerList.add(listener);
		}
	}

	public void deregister(PublisherManager.IEventListener listener)
	{
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_DEBUG, "deregister(%s, %s)", getName(), listener.toString());
		}

		synchronized (m_listenerList)
		{
			m_listenerList.remove(listener);
		}
	}

	public void post(Object event)
	{
		post(0, event);
	}
	/**
	 * post places the notification in a queue that is processed by a thread pool.
	 * The processing of the notification by the subscribers is done in the thread of the thread pool.
	 * The post call returns immediately and is virtually free (execution time wise) to the publisher.
	 *
	 * @param timeMS	Time of the notification
	 * @param event		Event relevant to the publisher
	 */
	public void post(long timeMS, Object event)
	{
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_DEBUG, "%s publishing %s", m_name, event);
		}

		getThreadPool().execute(new EventRunnable(this, timeMS, event));
	}

	private void publishInternal(long timeMS, Object event)
	{
		// Make a copy of the list inside the lock, then walk the copy outside the lock,
		// ... to minimize lock time AND to avoid dead-lock with unspecified threads registering themselves
		List<IEventListener>	listenerList;

		// No counters, only events, due to startup conflicts.
		synchronized (m_listenerList)
		{
			listenerList		= new ArrayList<>(m_listenerList);
		}

		boolean				isEvent				= (event instanceof IPublishedEvent);
		IPublishedEvent		publishedEvent		= isEvent ? (IPublishedEvent) event : null;
		if (isEvent)
		{
			try
			{
				publishedEvent.publicationBegins(event, listenerList.size());
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_PUBLISHER_EVENT_EXCEPTION_BEGIN, exception,
						"Event %s has thrown in publicationBegin()", event.toString());
			}
		}
		for (IEventListener listener : listenerList)
		{
			try
			{
				listener.receiveEvent(m_name, timeMS, event);
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_PUBLISHER_LISTENER_EXCEPTION, exception,
						"Listener %s has thrown in receiveEvent(%s)", listener.toString(), event.toString());
			}
		}
		if (isEvent)
		{
			try
			{
				publishedEvent.publicationEnds();
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_PUBLISHER_EVENT_EXCEPTION_END, exception,
						"Event %s has thrown in publicationEnds()", event.toString());
			}
		}
	}





	private static class EventRunnable implements Runnable
	{
		private Publisher		m_publisher;
		private long			m_timeMS;
		private Object			m_event;

		public EventRunnable(Publisher publisher, long timeMS, Object event)
		{
			m_publisher = publisher;
			m_timeMS = timeMS;
			m_event = event;
		}

		@Override
		public void run()
		{
			m_publisher.publishInternal(m_timeMS, m_event);
		}
	}
}
