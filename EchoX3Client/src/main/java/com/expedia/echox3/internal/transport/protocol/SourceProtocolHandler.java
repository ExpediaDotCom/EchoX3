/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.SourceTransportLane;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.socket.ComputerAddressGroup;

// Individual protocol handlers are responsible for managing m_addressGroupList
public class SourceProtocolHandler extends AbstractProtocolHandler
{
	private KeepAliveThread						m_keepAliveThread 				= null;
	private TimeoutThread						m_timeoutThread 				= null;

	// Map of ComputerAddressGroup -> Version of the group,
	// To know when the group needs to be updated.
	private final Map<ComputerAddressGroup, Integer>
												m_addressGroupVersionMap		= new HashMap<>();


	private final Map<ComputerAddress, TransportHighway>
												m_transportHighwayMap			= new HashMap<>();

	// This map contains the FIRST ComputerAddress of a given name added to the set.
	// If a different group requires the same ComputerAddress, it will use the original one.
	// If then the original group goes away, the original ComputerAddress stays (referred by "equals")
	// until it is no longer needed by anyone.
	// Note that a ComputerAddress includes name, port AND # of lanes.
	private final Map<ComputerAddress, ComputerAddress>		m_addressMap		= new HashMap<>();

	public SourceProtocolHandler(String protocolName, String description)
	{
		super(protocolName, description);

		m_keepAliveThread	= new KeepAliveThread(this);
		m_timeoutThread		= new TimeoutThread(this);

		updateConfiguration();		// NOPMD
	}

	protected TransportHighway createTransportHighway()
	{
		return new SourceTransportHighway(this);
	}

	public void runKeepAlive()
	{
		m_keepAliveThread.requestImmediateRun();
	}

	public void addAddressGroup(ComputerAddressGroup addressGroup)
	{
		synchronized (m_addressGroupVersionMap)
		{
			// Add with an invalid value to force a recalc of the m_addressSet
			Integer		version		= m_addressGroupVersionMap.put(addressGroup, Integer.MIN_VALUE);
 			if (null == version)	// i.e. it was not in the map
			{
				runKeepAlive();
			}
		}
		BasicTools.sleepMS(1000);
	}
	public void removeAddressGroup(ComputerAddressGroup addressGroup)
	{
		synchronized (m_addressGroupVersionMap)
		{
			Integer		version		= m_addressGroupVersionMap.remove(addressGroup);
			if (null != version)	// i.e. it was in the map
			{
				runKeepAlive();
			}
		}
	}
	private void updateAddressSet()
	{
		synchronized (m_addressGroupVersionMap)
		{
			boolean needsToRecalculate = false;
			for (Map.Entry<ComputerAddressGroup, Integer> entry : m_addressGroupVersionMap.entrySet())
			{
				ComputerAddressGroup	addressGroup	= entry.getKey();
				int						version			= entry.getValue();
				if (addressGroup.getVersion() != version)
				{
					needsToRecalculate = true;
					break;
				}
			}

			if (!needsToRecalculate)
			{
				return;
			}

			// Needs to recalculate...
			getLogger().debug(BasicEvent.EVENT_SOURCE_PROTOCOL_RECALCULATE,
					"Recalculating address lists for %s from %,d groups",
					toString(), m_addressGroupVersionMap.size());
			Set<ComputerAddress>		currentSet		= new HashSet<>();
			List<ComputerAddressGroup>	groupList		= new ArrayList<>(m_addressGroupVersionMap.size());
			groupList.addAll(m_addressGroupVersionMap.keySet());
			for (ComputerAddressGroup addressGroup : groupList)
			{
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (addressGroup)
				{
					Set<ComputerAddress>	addressSet		= addressGroup.getAddressSet();
					for (ComputerAddress address : addressSet)
					{
						TransportHighway		highway		= m_transportHighwayMap.get(address);
						if (null == highway)
						{
							highway = address.getSourceHighway();
							if (null == highway)
							{
								highway = getNewHighway();
								address.setSourceHighway(highway);
								for (int i = 0; i < address.getLaneCount(); i++)
								{
									SourceTransportLane		lane	= new SourceTransportLane(this, address);
									lane.setTransportHighway(highway);
								}
							}
							m_transportHighwayMap.put(address, highway);
						}
						else
						{
							if (address.getSourceHighway() != highway)		// SIC: Must be the same OBJECT.
							{
								if (null != address.getSourceHighway())
								{
									address.getSourceHighway().close(
											String.format("Switching address %s to existing highway %s",
													address.toString(), highway.toString()));
								}
								address.setSourceHighway(highway);
							}
						}
					}
					currentSet.addAll(addressSet);
					m_addressGroupVersionMap.put(addressGroup, addressGroup.getVersion());
				}
			}

			Set<ComputerAddress>		addedSet		= new HashSet<>();
			addedSet.addAll(currentSet);
			addedSet.removeAll(m_addressMap.keySet());

			Set<ComputerAddress>		removedSet		= new HashSet<>();
			removedSet.addAll(m_addressMap.keySet());
			removedSet.removeAll(currentSet);

			for (ComputerAddress address : addedSet)
			{
				m_addressMap.put(address, address);
			}
			for (ComputerAddress address : removedSet)
			{
				m_addressMap.remove(address);
				address.close(String.format("ComputerAddress %s no longer used.", address.toString()));
			}

		}
	}
/*
	public void showAddressMap()
	{
		if (getLogger().isDebugEnabled())
		{
			for (ComputerAddressGroup addressGroup : m_addressGroupVersionMap.keySet())
			{
				getLogger().debug(BasicEvent.EVENT_SOURCE_PROTOCOL_SHOW_LIST,
						"ComputerAddressGroup %s", addressGroup.getGroupName());
				for (ComputerAddress address : addressGroup.getAddressSet())
				{
					getLogger().debug(BasicEvent.EVENT_SOURCE_PROTOCOL_SHOW_LIST,
							"Address %s: IsConnected = %s; isActive = %s",
							address.toString(),
							address.getSourceHighway().isConnected(),
							address.getSourceHighway().isActive());
				}
			}
		}
	}
*/
	public Set<ComputerAddress> getAddressSet()
	{
		return m_addressMap.keySet();
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s, %s)",
				getClass().getSimpleName(), getProtocolName(), getDescription());
	}

	@Override
	public void processIsConnectable(SelectableChannel socketChannel, Object attachment)
	{
		SourceTransportLane transportLane		= (SourceTransportLane) attachment;
		boolean					isWellConnected;
		try
		{
			transportLane.removeWaitFor(SelectionKey.OP_CONNECT);
			isWellConnected = transportLane.getSocketChannel().finishConnect();
			transportLane.markOpen();
		}
		catch (IOException e)
		{
			// Ignore this exception: will be !isWellConnected.
			getLogger().debug(BasicEvent.EVENT_SOCKET_EXCEPTION_CONNECT, e, "isConnectable(%s) failed.", transportLane);
			transportLane.close(null, null);		// NO logging
			return;
		}
		if (isWellConnected)
		{
			getLogger().info(BasicEvent.EVENT_SOCKET_CLIENT_OPEN_SUCCESS,
					"Connection successful to %s. Submitting for conditioning", transportLane.toString());
		}
		else
		{
			transportLane.close(null, "Failed to connect properly, will try again later.");
		}
		transportLane.setReceiveMessage(getNewReceiveMessage());
		transportLane.setLaneStatus(AbstractTransportLane.LaneStatus.Connected);

		// The conditioner will transition the transportLane it from Connected to Active.
		TransportLaneConditioner.getInstance().condition(transportLane);
	}

	@Override
	public void shutdown()
	{
		m_keepAliveThread.terminate();
		m_timeoutThread.terminate();

		synchronized (m_addressGroupVersionMap)
		{
			// Temporary list to allow enumeration while modifying the m_ list...
			List<ComputerAddressGroup>		list		= new LinkedList<>();
			list.addAll(m_addressGroupVersionMap.keySet());
			for (ComputerAddressGroup addressGroup : list)
			{
				removeAddressGroup(addressGroup);
			}
		}
	}




	public static class KeepAliveThread extends AbstractScheduledThread
	{
		private final SourceProtocolHandler m_protocolHandler;

		public KeepAliveThread(SourceProtocolHandler protocolHandler)
		{
			super(true, String.format("%s.%s", KeepAliveThread.class.getName(), protocolHandler.getProtocolName()));

			m_protocolHandler = protocolHandler;

			setName(String.format("%s(%s)", getClass().getSimpleName(), m_protocolHandler.getProtocolName()));
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			synchronized (m_protocolHandler.m_addressGroupVersionMap)
			{
				m_protocolHandler.updateAddressSet();

				getLogger().debug(BasicEvent.EVENT_TRANSPORT_KEEP_ALIVE_WAKEUP,
						"%s: KeepAlive waking up to process %,d addresses",
						m_protocolHandler.getName(), m_protocolHandler.m_addressMap.size());
				for (ComputerAddress address : m_protocolHandler.getAddressSet())
				{
					TransportHighway		transportHighway		= address.getSourceHighway();
					// If there are no connection, make sure the highway does not try to use an old remote number
					if (!transportHighway.isConnected())
					{
						transportHighway.setRemoteNumber(0);
					}
					// Always call connect to ensure all lanes are connected
					connectHighway(transportHighway);
				}
			}
		}

		private void connectHighway(TransportHighway transportHighway)
		{
			for (AbstractTransportLane transportLane : transportHighway.getTransportLaneList())
			{
				SourceTransportLane clientTransportLane		= (SourceTransportLane) transportLane;
				getLogger().debug(BasicEvent.EVENT_TRANSPORT_KEEP_ALIVE_LANE_STATUS, "%s: %s",
						clientTransportLane, clientTransportLane.getLaneStatus().name());
				if (clientTransportLane.isNotConnected())
				{
					clientTransportLane.connect();
				}
			}
		}
	}


	public static class TimeoutThread extends AbstractScheduledThread
	{
		private final SourceProtocolHandler m_protocolHandler;

		public TimeoutThread(SourceProtocolHandler protocolHandler)
		{
			super(true, String.format("%s.%s", TimeoutThread.class.getName(), protocolHandler.getProtocolName()));

			m_protocolHandler = protocolHandler;

			setName(String.format("%s(%s)", getClass().getSimpleName(), m_protocolHandler.getProtocolName()));
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			getLogger().debug(BasicEvent.EVENT_TRANSPORT_KEEP_ALIVE_WAKEUP,
					"%s: KeepAlive waking up", m_protocolHandler.getName());

			synchronized (m_protocolHandler.m_addressGroupVersionMap)
			{
//				m_protocolHandler.updateAddressSet();

				Set<ComputerAddress>		addressSet		= m_protocolHandler.getAddressSet();
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				for (ComputerAddress address : addressSet)
				{
					SourceTransportHighway		transportHighway	=
							(SourceTransportHighway) address.getSourceHighway();
					// If there are no connection, make sure the highway does not try to use an old remote number
					if (transportHighway.isConnected())
					{
						transportHighway.processPendingForTimeout(timeMS);
					}
				}
			}
		}

		@Override
		public void terminate()
		{
			runOnce(Long.MAX_VALUE);
		}
	}
}
