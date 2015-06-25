/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;

public class ComputerAddressLoadBalancer
{
	private final ComputerAddressGroup		m_addressGroup;
	private final AtomicInteger				m_nextIndex					= new AtomicInteger(0);
	private ComputerAddress[]				m_addressList;
	private volatile int					m_addressGroupVersion		= Integer.MIN_VALUE;

	public ComputerAddressLoadBalancer(ComputerAddressGroup addressGroup)
	{
		m_addressGroup = addressGroup;
		updateAddressList();
	}

	public ComputerAddressGroup getAddressGroup()
	{
		return m_addressGroup;
	}

	private void updateAddressList()
	{
		int						size			= m_addressGroup.getAddressSet().size();
		ComputerAddress[]		addressList		= new ComputerAddress[size];
		int						index			= 0;
		Set<ComputerAddress>	addressSet		= m_addressGroup.getAddressSet();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (addressSet)
		{
			for (ComputerAddress address : addressSet)
			{
				addressList[index++] = address;
			}
		}
		m_addressList = addressList;
		m_addressGroupVersion = m_addressGroup.getVersion();
	}

	// Be careful to make this method thread safe WITHOUT lock
	// Worst case, the load is distributed 2 messages per highway at a time, only under extremely heavy load...
	// ... in which case it kind of does not matter as there is enough load to still distribute across all the highways.
	public TransportHighway getNextTransportHighway() throws BasicException
	{
		if (m_addressGroup.getVersion() != m_addressGroupVersion)
		{
			updateAddressList();
		}

		ComputerAddress[]		addressList		= m_addressList;
		if (0 == addressList.length)
		{
			throw new BasicException(BasicEvent.EVENT_TRANSPORT_LANE_NOT_AVAILABLE,
					String.format("No TransportHighway available from load balancer %s", toString()));
		}
		int						next				= m_nextIndex.getAndIncrement() % addressList.length;

		TransportHighway		highwayCurrent		= null;
		int						loadCurrent			= Integer.MAX_VALUE;
		for (int i = 0; i < addressList.length; i++)
		{
			ComputerAddress		address		= addressList[next++];
			if (next == addressList.length)
			{
				next = 0;
			}

			TransportHighway		transportHighway		= address.getSourceHighway();
			for (AbstractTransportLane transportLane : transportHighway.getTransportLaneList())
			{
				SourceTransportLane clientTransportLane = (SourceTransportLane) transportLane;
				AbstractProtocolHandler.getLogger().debug(BasicEvent.EVENT_TODO, "%s: %s",
						clientTransportLane, clientTransportLane.getLaneStatus().name());
			}
			if (!transportHighway.isActive())
			{
				continue;
			}

			int			highwayLoad		= transportHighway.getTransmitQueueSize();
			if (highwayLoad < loadCurrent)
			{
				highwayCurrent = transportHighway;
				loadCurrent = highwayLoad;
			}
		}

		if (null == highwayCurrent)
		{
			throw new BasicException(BasicEvent.EVENT_TRANSPORT_LANE_NOT_AVAILABLE,
					String.format("None of the %,d TransportHighway is active from load balancer %s",
							addressList.length, toString()));
		}

		return highwayCurrent;
	}
	public int getHighwayCount()
	{
		return m_addressList.length;
	}

	public TransportHighway waitForNextTransportHighway(long waitMS)
	{
		long					startMS				= System.currentTimeMillis();
		long					endMS				= startMS + waitMS;

		TransportHighway		transportHighway	= null;
		while (System.currentTimeMillis() < endMS)
		{
			try
			{
				transportHighway = getNextTransportHighway();
				break;		// No highway would throw.
			}
			catch (BasicException exception)
			{
				BasicTools.sleepMS(10);
			}
		}
		return transportHighway;
	}
}
