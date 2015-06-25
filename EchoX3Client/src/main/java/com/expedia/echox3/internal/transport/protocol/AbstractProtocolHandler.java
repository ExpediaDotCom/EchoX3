/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.map.ArrayMap;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.EchoThreadPoolHot;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.socket.SelectorThread;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;

public abstract class AbstractProtocolHandler
{
	//CHECKSTYLE:OFF
	private static final BasicLogger			LOGGER								= new BasicLogger(AbstractProtocolHandler.class);
	protected static final ConfigurationManager	CONFIGURATION_MANAGER				= ConfigurationManager.getInstance();
	//CHECKSTYLE:ON
	private static final String					SETTING_NAME_SELECTOR				= ".selector";
	private static final String					SETTING_NAME_OUTGOING_QUEUE_SIZE	= ".outgoingQueueSize";

	private final String						m_protocolName;
	// m_address ==
	// if server, then Listen address
	// if client, then not used
	private ComputerAddress							m_address;
	private String									m_name;
	private String									m_description;
	private String									m_settingPrefix;

	private final AtomicInteger						m_nextSelector			= new AtomicInteger(0);
	private volatile SelectorThread[]				m_selectorThreadList	= null;

	private volatile IEchoThreadPool m_workerThreadPool		= null;
	private volatile int							m_outgoingQueueSize;

	// Messages are sent along the highway, on any available lane.
	private final Map<Integer, TransportHighway>	m_highwayMap			= new ArrayMap<>();

	private final ObjectPool<ByteArrayWrapper>		m_byteArrayPool;
	private final ObjectPool<ReceiveMessage>		m_receiveMessagePool;

	private final List<AbstractMessageHandler>		m_messageHandlerList	= new ArrayList<>();

	private final SocketCounterFamily				m_socketCounterFamily;

	public AbstractProtocolHandler(String protocolName, String description)
	{
		m_protocolName			= protocolName;
		m_description			= description;
		m_name					= String.format("%s(%s)", getClass().getSimpleName(), protocolName);
		m_settingPrefix			= getClass().getName() + "." +  protocolName;

		m_byteArrayPool			= new ObjectPool<>(
				new StringGroup(ByteArrayWrapper.class.getSimpleName() + "." + m_protocolName), ByteArrayWrapper::new);
		m_receiveMessagePool	= new ObjectPool<>(
				new StringGroup(m_protocolName), () -> new ReceiveMessage(m_byteArrayPool));

		m_socketCounterFamily	= new SocketCounterFamily(protocolName);
		IOperationContext	context	= m_socketCounterFamily.getReadMessageCounter().begin();
		BasicTools.sleepMS(5);
		context.end(true);

		String			poolName		= String.format("Worker.%s", m_protocolName);
		m_workerThreadPool = new EchoThreadPoolHot(poolName);

		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public boolean setAddress(ComputerAddress address)
	{
		boolean		isChanged		= !address.equals(m_address);
		if (isChanged)
		{
			m_address = address;
			m_name = String.format("%s on %s", m_protocolName, m_address.getInetSocketAddress().toString());
		}
		return isChanged;
	}

	public ComputerAddress getAddress()
	{
		return m_address;
	}

	public String getProtocolName()
	{
		return m_protocolName;
	}

	public String getDescription()
	{
		return m_description;
	}

	public String getName()
	{
		return m_name;
	}

	public void setSettingPrefix(String settingPrefix)
	{
		m_settingPrefix = settingPrefix;
	}

	public String getSettingPrefix()
	{
		return m_settingPrefix;
	}

	public String getSettingName(String shortName)
	{
		return m_settingPrefix + shortName;
	}

	public IEchoThreadPool getWorkerThreadPool()
	{
		return m_workerThreadPool;
	}

	public int getOutgoingQueueSize()
	{
		return m_outgoingQueueSize;
	}

	public SocketCounterFamily getSocketCounterFamily()
	{
		return m_socketCounterFamily;
	}

	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		updateConfiguration();
	}

	public boolean updateConfiguration()
	{
		boolean		isChanged;

		isChanged  = adjustSelectorCount();

		m_outgoingQueueSize = CONFIGURATION_MANAGER.getInt(getSettingName(SETTING_NAME_OUTGOING_QUEUE_SIZE), "100");
		for (TransportHighway transportHighway : m_highwayMap.values())
		{
			isChanged |= transportHighway.setQueueSize(m_outgoingQueueSize);
		}
		return isChanged;
	}
	private boolean adjustSelectorCount()
	{
		boolean		isChanged		= false;

		int			selectorCount		=
							CONFIGURATION_MANAGER.getSelfTuningInteger(getSettingName(SETTING_NAME_SELECTOR), "1", "4");
		selectorCount = Math.max(1, selectorCount);		// at least 1!

		SelectorThread[]		selectorThreadList;
		synchronized (m_nextSelector)
		{
			if (null == m_selectorThreadList)
			{
				isChanged = true;
				selectorThreadList = new SelectorThread[selectorCount];
			}
			else if (m_selectorThreadList.length < selectorCount)
			{
				isChanged = true;
				// Reduce number of selector threads...
				// The existing selector must continue to live until the sockets associated with them terminate.
				// The SelectorThread must be marked for termination and then exit when no more clients.
				for (int i = selectorCount; i < m_selectorThreadList.length; i++)
				{
					// for all the extra threads...
					m_selectorThreadList[i].terminate();		// Mark them as ExitRequested.
				}
				selectorThreadList = Arrays.copyOf(m_selectorThreadList, selectorCount);
			}
			else if (m_selectorThreadList.length > selectorCount)
			{
				isChanged = true;
				// Increase the number of selector threads (easier)
				selectorThreadList = Arrays.copyOf(m_selectorThreadList, selectorCount);
			}
			else // if (m_selectorThreadList.length == selectorCount)
			{
				selectorThreadList = m_selectorThreadList;
			}

			// ... and fill in any blank in the array with new thread ...
			for (int i = 0; i < selectorThreadList.length; i++)
			{
				if (null == selectorThreadList[i])
				{
					try
					{
						selectorThreadList[i] = new SelectorThread(this, i);
					}
					catch (BasicException exception)
					{
						getLogger().error(BasicEvent.EVENT_SELECTOR_EVENT_EXCEPTION, exception,
								"The TransportHandler may not operate properly.");
						selectorThreadList[i] = null;
					}
				}
			}

			m_selectorThreadList = selectorThreadList;
		}

		return isChanged;
	}

	public final SelectorThread getNextSelector()
	{
		SelectorThread		thread		= null;

		waitForStartup();

		synchronized (m_nextSelector)
		{
			for (int i = 0; i < m_selectorThreadList.length; i++)
			{
				int					selectorIndex		= m_nextSelector.incrementAndGet();
				int					threadIndex			= selectorIndex % m_selectorThreadList.length;
				thread = m_selectorThreadList[threadIndex];
				if (null != thread)
				{
					break;
				}
			}
		}
		return null == thread ? null : thread;
	}
	public int getSelectorCount()
	{
		return null == m_selectorThreadList ? 0 : m_selectorThreadList.length;
	}
	private void waitForStartup()
	{
		while (0 == getSelectorCount())
		{
			BasicTools.sleepMS(10);
		}
	}

	public ReceiveMessage getNewReceiveMessage()
	{
		ReceiveMessage		message		= m_receiveMessagePool.get();
		message.setCounterFamily(getSocketCounterFamily());
		return message;
	}

	public Map<Integer, TransportHighway> getHighwayMap()
	{
		return m_highwayMap;
	}

	public TransportHighway getNewHighway()
	{
		TransportHighway		highway		= createTransportHighway();
		synchronized (m_highwayMap)
		{
			m_highwayMap.put(highway.getLocalNumber(), highway);
		}
		return highway;
	}
	protected abstract TransportHighway createTransportHighway();
	public void removeHighway(TransportHighway highway)
	{
		synchronized (m_highwayMap)
		{
			m_highwayMap.remove(highway.getLocalNumber());
		}
	}

	public List<TransportHighway> getTransportHighwayList()
	{
		List<TransportHighway>			list			= new LinkedList<>();
		synchronized (m_highwayMap)
		{
			Collection<TransportHighway>	highwayList		= m_highwayMap.values();
			list.addAll(highwayList);
		}
		return list;
	}

	// For the extending class to override as needed (i.e. only for the servers)
	// Servers receive this call on a new connection
	public void processIsAcceptable()
	{
//		System.out.println("isConnectable!");
	}

	// For the extending class to override as needed (i.e. only for the clients)
	public void processIsConnectable(SelectableChannel socketChannel, Object attachment)
	{
//		System.out.println("isConnectable!");
	}

	public void processIsWritable(AbstractTransportLane transportLane)
	{
		// Do NOT loop, attempt to transmit only one message.
		// This to ensure the next message goes to the next lane.
		// The performance degradation (some is expected) falls within the noise (not measurable)
		// when running the loopback test at 110K QPS (per highway).

		TransmitMessage		transmitMessage		= transportLane.getTransmitMessage();
		if (null == transmitMessage)
		{
			transmitMessage = transportLane.getTransportHighway().assignTransmitMessage(transportLane);
		}

		if (null != transmitMessage)
		{
			try
			{
				transportLane.write();
			}
			catch (BasicException exception)
			{
				transportLane.close(exception, "Failed to write to " + transportLane.getName());
			}
		}
	}

	public void processIsReadable(AbstractTransportLane transportLane)
	{
		BasicException				closingException	= null;
		String						closingReason		= null;

		while (true)
		{
			int					cbRead;
			try
			{
				cbRead = transportLane.read();
			}
			catch (BasicException e)
			{
				closingException = e;
				closingReason = "Failed to read from " + transportLane.getName();
				break;
			}

			if (0 == cbRead)
			{
				break;
			}
			else if (-1 == cbRead)
			{
				closingReason = "The channel was closed cleanly by the remote computer (-1 == cbHeader)";
				break;
			}

			ReceiveMessage		message		= transportLane.getReceiveMessage();
			if (message.isComplete())
			{
				transportLane.setLastMessageTimeMS();
				transportLane.setReceiveMessage(getNewReceiveMessage());

				// At this point, the message belongs to the AbstractProtocolHandler!
				// TODO DEBUG log entry
				getWorkerThreadPool().execute(message);
			}
		}

		if (null != closingReason)
		{
			transportLane.close(closingException, closingReason);
		}
	}

	public List<AbstractMessageHandler> getMessageHandlerList()
	{
		return m_messageHandlerList;
	}

	public void registerMessageHandler(AbstractMessageHandler messageHandler)
	{
		getLogger().info(BasicEvent.EVENT_MESSAGE_HANDLER_BEGIN,
				"%s(%s) now listening to messages on %s(%s).",
				messageHandler.getClass().getSimpleName(), messageHandler.getName(),
				getClass().getSimpleName(), getName());

		m_messageHandlerList.add(messageHandler);
	}
	public void deregisterMessageHandler(AbstractMessageHandler messageHandler)
	{
		getLogger().info(BasicEvent.EVENT_MESSAGE_HANDLER_STOP,
				"MessageHandler(%s) stops listening to Protocol(%s).", messageHandler.getName(), getName());

		m_messageHandlerList.remove(messageHandler);
	}

	// Called in the WorkerPool, finds the appropriate message handler.
	// Common to Client and Server: The registered MessageHandlers are different.
	public void processMessage(ReceiveMessage receiveMessage)
	{
		boolean		isHandled		= false;
		// Pass to the attached MessageHandler
		for (int i = 0; i < m_messageHandlerList.size(); i++)
		{
			AbstractMessageHandler		messageHandler		= m_messageHandlerList.get(i);
			isHandled = messageHandler.handleMessage(receiveMessage);
			if (isHandled)
			{
				break;
			}
		}

		if (!isHandled)
		{
			// TODO Increment ignored/dropped message?
			getLogger().info(BasicEvent.EVENT_COUNTER, "No MessageHandler for message %s", receiveMessage.toString());
		}
	}

	public void shutdown()
	{
		// To avoid concurrent modification on m_messageHandlerList in de-registerMessageHandler.
		List<AbstractMessageHandler>		list		= new ArrayList<>(m_messageHandlerList.size());
		list.addAll(m_messageHandlerList);

		for (AbstractMessageHandler messageHandler : list)
		{
			deregisterMessageHandler(messageHandler);                            // Tell me (and make a log entry)
			messageHandler.receiveShutdownNotification(getProtocolName());        // Tell the message handler
		}

		m_byteArrayPool.release();
		m_receiveMessagePool.release();
		m_workerThreadPool.shutdown();
		m_socketCounterFamily.close();
	}
}
