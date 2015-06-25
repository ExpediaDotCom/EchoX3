/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.socket.DestTransportLane;

public class DestProtocolHandler extends AbstractProtocolHandler
{
	private ServerSocketChannel		m_serverChannel;
	private SelectionKey			m_selectionKey;

	public DestProtocolHandler(String protocolName, String description)
	{
		super(protocolName, description);
	}

	@Override
	public boolean updateConfiguration()
	{
		boolean		isSuperChanged		= super.updateConfiguration();
		boolean		isAddressChanged	= false;
		try
		{
			ComputerAddress address	= new ComputerAddress(getSettingPrefix());
			address.resolve();
			if (setAddress(address))		// returns true to indicate change
			{
				isAddressChanged = true;
				closeChannel();
				startListening();		// ... on the new channel
			}
		}
		catch (BasicException e)
		{
			// TODO Handle invalid server address.
			// Make a log entry and ignore the invalid address.
		}

		return isSuperChanged || isAddressChanged;
	}

	private void startListening()
	{
		try
		{
			m_serverChannel = ServerSocketChannel.open();
			m_serverChannel.configureBlocking(false);
			m_serverChannel.socket().bind(getAddress().getInetSocketAddress());

			m_selectionKey = m_serverChannel.register(getNextSelector().getSelector(), SelectionKey.OP_ACCEPT, this);
			getLogger().info(BasicEvent.EVENT_SOCKET_SERVER_OPEN_SUCCESS,
					"%s(%s) starts listening on port %s",
					getClass().getSimpleName(), getName(), getAddress().toString());
		}
		catch (Exception exception)
		{
			// TODO handle startup exception (e.g. someone else already using this socket)
			getLogger().error(BasicEvent.EVENT_SOCKET_SERVER_OPEN_FAIL, exception,
					"Failed to initialize listening server for %s", toString());
		}
	}

	private ServerSocketChannel getServerChannel()
	{
		return m_serverChannel;
	}

	protected TransportHighway createTransportHighway()
	{
		return new TransportHighway(this);
	}


	// For the extending class to override as needed (i.e. only for the servers?)
	// Servers receive this call on a new connection
	@Override
	public void processIsAcceptable()
	{
		SocketChannel				socketChannel;
		try
		{
			socketChannel = getServerChannel().accept();
		}
		catch (Exception e)
		{
			getLogger().error(BasicEvent.EVENT_SOCKET_EXCEPTION_ACCEPT, e,
					"Failed to accept a connection on %s", getName());
			return;
		}

		// The constructor automatically links itself to the protocol handler and the transport highway.
		DestTransportLane transportLane		= new DestTransportLane(this);

		// ServerTransportLane go immediately active.
		transportLane.setReceiveMessage(getNewReceiveMessage());
		transportLane.setSelectorThread(getNextSelector());

		transportLane.getSelectorThread().addTask(new DelayedAccept(transportLane, socketChannel));
	}

	@Override
	public final void shutdown()
	{
		super.shutdown();

		closeChannel();
	}

	private void closeChannel()
	{
		// Shutdown the listen channel... The active connection should stay open.
		if (null != m_selectionKey)
		{
			m_selectionKey.cancel();
			m_selectionKey = null;
		}
		if (null != m_serverChannel)
		{
			BaseFileHandler.closeSafe(m_serverChannel);
			m_serverChannel = null;
		}
	}

	@Override
	public String toString()
	{
		return String.format("%s(%d lanes, %s, %s)",
				getClass().getSimpleName(),
				null == getAddress() ? 0 : getAddress().getLaneCount(),
				getName(), getDescription());
	}


	public class DelayedAccept implements Runnable
	{
		private final DestTransportLane m_transportLane;
		private final SocketChannel				m_socketChannel;

		public DelayedAccept(DestTransportLane transportLane, SocketChannel socketChannel)
		{
			m_transportLane = transportLane;
			m_socketChannel = socketChannel;
		}

		@Override
		public void run()
		{
			m_transportLane.configureSocket(m_socketChannel);
			m_transportLane.setLaneStatus(AbstractTransportLane.LaneStatus.Active);

			getLogger().info(BasicEvent.EVENT_SOCKET_SERVER_CONNECT, "%s: New Client connection to server lane %d = %s",
					getName(), m_transportLane.getTransportHighway().getLocalNumber(), m_transportLane.getName());
		}
	}
}
