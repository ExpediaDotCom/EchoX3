/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.QueueFactory;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane.LaneStatus;

public class TransportHighway
{
	private static final AtomicInteger					NUMBER_DISPENSER		= new AtomicInteger(0);
	private static final QueueFactory<TransmitMessage>	QUEUE_FACTORY			= new QueueFactory<>();

	private final AbstractProtocolHandler		m_protocolHandler;
	private final int							m_localNumber			= NUMBER_DISPENSER.incrementAndGet();
	private int									m_remoteNumber			= 0;
	private final List<AbstractTransportLane>	m_transportLaneList		= new ArrayList<>();
	private final AtomicInteger					m_nextLane				= new AtomicInteger(0);

	private int									m_queueSizeMax			= -1;
	// TODO Revisit the collection type: Queue or RingBuffer? Which is faster?
	private volatile Queue<TransmitMessage>		m_transmitQueue			= null;

	public TransportHighway(AbstractProtocolHandler protocolHandler)
	{
		m_protocolHandler = protocolHandler;

		setQueueSize(m_protocolHandler.getOutgoingQueueSize());
	}

	public boolean setQueueSize(int size)
	{
		boolean		isChanged		= false;
		synchronized (this)
		{
			if (null == m_transmitQueue)
			{
				isChanged = true;
				// TODO Revisit which queue type is best
				m_transmitQueue = QUEUE_FACTORY.getQueue(size);
			}
			else if (size != m_queueSizeMax)
			{
				isChanged = true;
				Queue<TransmitMessage>		transmitQueue	= QUEUE_FACTORY.getQueue(size);
				transmitQueue.addAll(m_transmitQueue);
				m_transmitQueue = transmitQueue;
			}
			m_queueSizeMax = size;
		}
		return isChanged;
	}

	public int getLocalNumber()
	{
		return m_localNumber;
	}

	public void setRemoteNumber(int remoteNumber)
	{
		m_remoteNumber = remoteNumber;
	}

	public int getRemoteNumber()
	{
		return m_remoteNumber;
	}

	public AbstractProtocolHandler getProtocolHandler()
	{
		return m_protocolHandler;
	}

	public List<AbstractTransportLane> getTransportLaneList()
	{
		return m_transportLaneList;
	}

	public long getConnectTimeMS()
	{
		if (!isActive())
		{
			return 0;
		}

		long		connectTimeMS		= Long.MAX_VALUE;
		for (AbstractTransportLane transportLane : getTransportLaneList())
		{
			connectTimeMS = Math.min(connectTimeMS, transportLane.getConnectTimeMS());
		}
		return connectTimeMS;
	}
	public long getCloseTimeMS()
	{
		if (isActive())
		{
			return Long.MAX_VALUE;
		}

		long		closeTimeMS		= 0;
		for (AbstractTransportLane transportLane : getTransportLaneList())
		{
			closeTimeMS = Math.max(closeTimeMS, transportLane.getConnectTimeMS());
		}
		return closeTimeMS;
	}

	public boolean isConnected()
	{
		for (int i = 0; i < m_transportLaneList.size(); i++)
		{
			AbstractTransportLane		transportLane		= m_transportLaneList.get(i);
			if (transportLane.isConnected())
			{
				return true;
			}
		}
		return false;
	}
	public boolean isActive()
	{
		// m_transportLaneList is an ArrayList, avoid creating an Iterator
		for (int i = 0; i < m_transportLaneList.size(); i++)
		{
			AbstractTransportLane		transportLane		= m_transportLaneList.get(i);
			if (transportLane.isActive())
			{
				return true;
			}
		}
		return false;
	}

	public void close(String reason)
	{
		for (AbstractTransportLane transportLane : m_transportLaneList)
		{
			if (transportLane.isConnected())
			{
				transportLane.close(null, reason);
			}
		}
		getProtocolHandler().removeHighway(this);
	}

	public void addTransportLane(AbstractTransportLane lane)
	{
		synchronized (m_transportLaneList)
		{
			m_transportLaneList.add(lane);
		}
	}

	public void removeTransportLane(AbstractTransportLane lane)
	{
		synchronized (m_transportLaneList)
		{
			m_transportLaneList.remove(lane);

			if (m_transportLaneList.isEmpty())
			{
				// Highway no longer required!
				getProtocolHandler().removeHighway(this);
			}
		}
	}

	public void notifyStatusChange(AbstractTransportLane transportLane, LaneStatus statusPrev, LaneStatus statusNew)
	{
		// Nothing to do in the basic transport highway
	}

	public Queue<TransmitMessage> getTransmitQueue()
	{
		return m_transmitQueue;
	}
	public int getTransmitQueueSize()
	{
		return m_transmitQueue.size();
	}

	public void transmit(TransmitMessage message) throws BasicException
	{
		// synchronized is about 10% faster than MagicReadWriteLock.
		// In raw Loopback tests, synchronized gets 110-115K QPS vs 100-105K QPS for MagicReadWriteLock.
		// The limiting factor is at the socket lever, not this lock.
		AbstractProtocolHandler		protocolHandler		= m_transportLaneList.get(0).getProtocolHandler();
		message.setCounterFamily(protocolHandler.getSocketCounterFamily());
		message.markInQueue();
		synchronized (this)
		{
			if (!isActive())
			{
				message.release();		// Won't get transmitted!
				throw new BasicException(BasicEvent.EVENT_TRANSPORT_LANE_NOT_AVAILABLE,
						"No TransportHighway has no available lane to transmit the message.");
			}

			if (!getTransmitQueue().offer(message))
			{
				// TODO Replace this println with a counter
				// TODO Add increment counter for transmit message dropped
				System.out.println("Failed to queue message " + message);		// NOPMD
			}

			// Kick one transport lane, round robin to distribute the load
			// e.g. there could be a long message in the driver queue of one of the sockets.
			// TODO Protect m_transportLaneList
			// TODO LoadBalancer amongst the transportLanes
			int			startLane		= m_nextLane.getAndIncrement() % m_transportLaneList.size();
			for (int i = 0; i < m_transportLaneList.size(); i++)
			{
				AbstractTransportLane	transportLane		= m_transportLaneList.get(startLane++);
				if (startLane == m_transportLaneList.size())
				{
					startLane = 0;
				}
				if (!transportLane.isActive())
				{
					continue;
				}

				boolean			isChanged	=
											  transportLane.addWaitFor(SelectionKey.OP_WRITE);
//* Wake up everybody?
				if (isChanged)
				{
					break;		// Kick only one.
				}
//*/
			}
		}

		// Note:	if all lanes are already in write mode, nothing happens ... the next available lane will pick-it up
		//			if no lane is active, nothing happens, the message is dropped ... too bad.
	}

	public TransmitMessage assignTransmitMessage(AbstractTransportLane transportLane)
	{
		TransmitMessage		transmitMessage;
		synchronized (this)
		{
			transmitMessage = getTransmitQueue().poll();
			if (null == transmitMessage)
			{
				transportLane.removeWaitFor(SelectionKey.OP_WRITE);
			}
			transportLane.setTransmitMessage(transmitMessage);
		}
		return transmitMessage;
	}

	@Override
	public String toString()
	{
		AbstractProtocolHandler			protocolHandler		= getProtocolHandler();
		AbstractTransportLane			transportLane		= getTransportLaneList().isEmpty()
																? null : getTransportLaneList().get(0);

		StringBuilder					sb					= new StringBuilder(200);
		if (null == transportLane)
		{
			sb.append("No lanes");
		}
		else
		{
			sb.append(transportLane.toString());
			sb.append(" on ");
		}
		sb.append(protocolHandler.toString());

		return sb.toString();
	}
}
