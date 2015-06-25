/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.transport.dispatch.user;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.internal.transport.dispatch.user.source.GetZoneDispatcherListSourceRequest;
import com.expedia.echox3.internal.transport.dispatch.user.source.GetDispatcherListForCacheSourceRequest;
import com.expedia.echox3.internal.transport.dispatch.user.source.GetZoneUserDispatcherListSourceRequest;
import com.expedia.echox3.internal.transport.dispatch.user.source.UserReadOnlyBulkSourceRequest;
import com.expedia.echox3.internal.transport.dispatch.user.source.UserReadOnlyMultipleSourceRequest;
import com.expedia.echox3.internal.transport.dispatch.user.source.UserReadOnlySingleSourceRequest;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.request.MessageType;
import com.expedia.echox3.internal.transport.request.SourceMessageHandlerBase;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

public class DispatcherUserSourceMessageHandler extends SourceMessageHandlerBase
{
	// 100 - 149	Dispatcher Client (1st hop)
	//CHECKSTYLE:OFF
	// 10x			For setup style message
	public static final MessageType		GetZoneUserDispatcherList			= new MessageType(100, "GetZoneUserDispatcherList");
	public static final MessageType		GetZoneDispatcherList				= new MessageType(101, "GetZoneDispatcherList");
	public static final MessageType		GetDispatcherListForCache			= new MessageType(102, "GetDispatcherListForCache");

	// 11x			For ReadOnly messages
	public static final MessageType		UserReadOnlySingle					= new MessageType(110, "UserReadOnlySingle");
	public static final MessageType		UserReadOnlyMultiple				= new MessageType(111, "UserReadOnlyMultiple");
	public static final MessageType		UserReadOnlyBulk					= new MessageType(112, "UserReadOnlyBulk");
	// 12x			For WriteOnly messages
	// 13x			Reserved for ReadWrite messages
	// 14x			Not used, reserved for future use.
	//CHECKSTYLE:ON

	public DispatcherUserSourceMessageHandler(SourceProtocolHandler protocolHandler)
	{
		super(BasicTools.getComputerName(), protocolHandler);

		setObjectPool(GetZoneUserDispatcherList,	GetZoneUserDispatcherListSourceRequest::new);
		setObjectPool(GetZoneDispatcherList,		GetZoneDispatcherListSourceRequest::new);
		setObjectPool(GetDispatcherListForCache,	GetDispatcherListForCacheSourceRequest::new);

		setObjectPool(UserReadOnlySingle,			UserReadOnlySingleSourceRequest::new);
		setObjectPool(UserReadOnlyMultiple,			UserReadOnlyMultipleSourceRequest::new);
		setObjectPool(UserReadOnlyBulk,				UserReadOnlyBulkSourceRequest::new);

		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);		// NOPMD
	}


	public Map<String, List<ComputerAddress>> getZoneUserDispatcherList(SourceTransportHighway transportHighway)
			throws BasicException
	{
		GetZoneUserDispatcherListSourceRequest		request		= (GetZoneUserDispatcherListSourceRequest)
				getRequest(GetZoneUserDispatcherList);

		try
		{
			processRequestSynchronously(transportHighway, request);
			Map<String, List<ComputerAddress>>		map		= request.getZoneClientDispatcherMap();
			return map;
		}
		finally
		{
			request.release();
		}
	}

	public ZoneDispatcherList getZoneDispatcherList(SourceTransportHighway transportHighway)
			throws BasicException
	{
		GetZoneDispatcherListSourceRequest request		= (GetZoneDispatcherListSourceRequest)
				getRequest(GetZoneDispatcherList);

		try
		{
			processRequestSynchronously(transportHighway, request);
			String					zone			= request.getZone();
			List<ComputerAddress>	adminList		= request.getAdminList();
			List<ComputerAddress>	userList		= request.getUserList();

			return new ZoneDispatcherList(zone, adminList, userList);
		}
		finally
		{
			request.release();
		}
	}
	public static class ZoneDispatcherList
	{
		private String					m_zoneName;
		private List<ComputerAddress>	m_adminList		= new LinkedList<>();
		private List<ComputerAddress>	m_userList		= new LinkedList<>();

		public ZoneDispatcherList(String zoneName)
		{
			m_zoneName		= zoneName;
		}
		public ZoneDispatcherList(String zoneName, List<ComputerAddress> adminList, List<ComputerAddress> userList)
		{
			m_zoneName = zoneName;
			m_adminList.addAll(adminList);
			m_userList.addAll(userList);
		}

		public String getZoneName()
		{
			return m_zoneName;
		}

		public List<ComputerAddress> getAdminList()
		{
			return m_adminList;
		}

		public void setAdminList(List<ComputerAddress> adminList)
		{
			m_adminList = adminList;
		}

		public List<ComputerAddress> getUserList()
		{
			return m_userList;
		}

		public void setUserList(List<ComputerAddress> userList)
		{
			m_userList = userList;
		}

		public void accumulate(ZoneDispatcherList list)
		{
			m_adminList.addAll(list.m_adminList);
			m_userList.addAll(list.m_userList);
		}
	}

	public List<ComputerAddress> getDispatcherListForCache(SourceTransportHighway transportHighway, String cacheName)
			throws BasicException
	{
		GetDispatcherListForCacheSourceRequest		request		= (GetDispatcherListForCacheSourceRequest)
				getRequest(GetDispatcherListForCache);
		request.setCacheName(cacheName);

		try
		{
			processRequestSynchronously(transportHighway, request);
			List<ComputerAddress>		list		= request.getAddressList();
			return list;
		}
		finally
		{
			request.release();
		}
	}

}
