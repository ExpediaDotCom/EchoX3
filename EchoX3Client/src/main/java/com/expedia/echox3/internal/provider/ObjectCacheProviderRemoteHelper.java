/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler.ZoneDispatcherList;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;
import com.expedia.echox3.internal.transport.socket.ComputerAddressGroup;
import com.expedia.echox3.internal.transport.socket.ComputerAddressLoadBalancer;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.visible.trellis.ClientFactory;

/**
 * Contains method that do not belong in ObjectCacheProviderRemote,
 * but are useful for its customers.
 * Some may require some intimate internal knowledge
 * while others just fit better closer to ObjectCacheProviderRemote.
 */
public final class ObjectCacheProviderRemoteHelper
{
	public static final int			CONNECT_DELAY_MS		= 1000;

	private ObjectCacheProviderRemoteHelper()
	{
		// Private constructor that is NEVER called: An instance of this class cannot be created.
		// Every method of this class is static.
	}

	public static void connectToAddressList(String cacheName, Collection<ComputerAddress> addressList)
	{
		ObjectCacheProviderRemote client			= ClientFactory.getInstance().getProviderRemote();
		ComputerAddressGroup			addressGroup	= new ComputerAddressGroup(cacheName);
		for (ComputerAddress address : addressList)
		{
			addressGroup.addAddress(address);
		}

		SourceProtocolHandler			protocolHandler	= client.getProtocolHandler();
		protocolHandler.addAddressGroup(addressGroup);

		BasicTools.sleepMS(CONNECT_DELAY_MS);
		client.addAddressGroupLoadBalancer(cacheName, addressGroup);
	}

	public static ComputerAddressLoadBalancer getLoadBalancer(String cacheName)
	{
		ObjectCacheProviderRemote client			= ClientFactory.getInstance().getProviderRemote();
		ComputerAddressLoadBalancer		loadBalancer	= client.getAddressGroupLoadBalancer(cacheName);

		return loadBalancer;
	}

	public static void removeAddressList(String cacheName)
	{
		ObjectCacheProviderRemote client			= ClientFactory.getInstance().getProviderRemote();
		ComputerAddressLoadBalancer		loadBalancer	= client.removeAddressGroupLoadBalancer(cacheName);

		if (null != loadBalancer)
		{
			SourceProtocolHandler protocolHandler = client.getProtocolHandler();
			protocolHandler.removeAddressGroup(loadBalancer.getAddressGroup());
		}
	}
/*
	interface IDispatcherListBuilder
	{
		ComputerAddress[] buildDispatcherList (
				DispatcherUserSourceMessageHandler messageHandler,
				SourceTransportHighway transportHighway, String warehouseName)
				throws BasicException;
	}
*/
	public static Map<String, List<ComputerAddress>> buildDispatcherUserAddressSet()
	{
		String							cacheName		= ObjectCacheProviderRemote.getPermanentAddressGroupName();
		ObjectCacheProviderRemote client			= ClientFactory.getInstance().getProviderRemote();
		DispatcherUserSourceMessageHandler
										messageHandler	= client.getClientMessageHandler();

		Map<String, List<ComputerAddress>>	map			= new HashMap<>();
		Set<TransportHighway>			transportSet	= new TreeSet<>();
		ComputerAddressLoadBalancer		loadBalancer	= client.getAddressGroupLoadBalancer(cacheName);
		final int						count			= loadBalancer.getHighwayCount();
		for (int i = 0; i < count; i++)
		{
			try
			{
				TransportHighway		transportHighway = loadBalancer.getNextTransportHighway();
				if (!transportSet.add(transportHighway))
				{
					// If not modified, already done this transportHighway
					// The same may come back multiple times if some are down.
					continue;
				}

				SourceTransportHighway			sourceHighway	= (SourceTransportHighway) transportHighway;
				Map<String, List<ComputerAddress>>	tempMap	= messageHandler.getZoneUserDispatcherList(sourceHighway);
				for (Map.Entry<String, List<ComputerAddress>> entry : tempMap.entrySet())
				{
					String					zoneName		= entry.getKey();
					List<ComputerAddress>	masterSet		= map.get(zoneName);
					if (null == masterSet)
					{
						masterSet = new ArrayList<>();
						map.put(zoneName, masterSet);
					}
					List<ComputerAddress>		tempList		= entry.getValue();
					masterSet.addAll(tempList);
				}
			}
			catch (BasicException exception)
			{
				// Nobody connected, return an empty set
				break;
			}
		}

		return map;
	}

	public static Map<String, ZoneDispatcherList> buildDispatcherAddressSet(String cacheName)
	{
		ObjectCacheProviderRemote client			= ClientFactory.getInstance().getProviderRemote();
		DispatcherUserSourceMessageHandler	messageHandler	= client.getClientMessageHandler();

		Map<String, ZoneDispatcherList>		zoneMap			= new HashMap<>();
		Set<TransportHighway>				transportSet	= new TreeSet<>();
		ComputerAddressLoadBalancer			loadBalancer	= client.getAddressGroupLoadBalancer(cacheName);
		final int							count			= loadBalancer.getHighwayCount();
		for (int i = 0; i < count; i++)
		{
			try
			{
				TransportHighway		transportHighway = loadBalancer.getNextTransportHighway();
				if (!transportSet.add(transportHighway))
				{
					// If not modified, already done this transportHighway
					// The same may come back multiple times if some are down.
					continue;
				}

				SourceTransportHighway	sourceHighway	= (SourceTransportHighway) transportHighway;
				if (ObjectCacheProviderRemote.getLogger().isDebugEnabled())
				{
					ObjectCacheProviderRemote.getLogger().debug(BasicEvent.EVENT_TEST,
							"Sending getZoneDispatcherList to %s", sourceHighway.getRemoteAddress().toString());
				}

				ZoneDispatcherList		tempAddressList			= messageHandler.getZoneDispatcherList(sourceHighway);
				ZoneDispatcherList		zoneDispatcherList		= zoneMap.get(tempAddressList.getZoneName());
				if (null == zoneDispatcherList)
				{
					zoneDispatcherList = new ZoneDispatcherList(tempAddressList.getZoneName());
					zoneMap.put(tempAddressList.getZoneName(), zoneDispatcherList);
				}
				zoneDispatcherList.accumulate(tempAddressList);
			}
			catch (BasicException exception)
			{
				// Nobody connected, return an empty set
				break;
			}
		}

		return zoneMap;
	}
}
