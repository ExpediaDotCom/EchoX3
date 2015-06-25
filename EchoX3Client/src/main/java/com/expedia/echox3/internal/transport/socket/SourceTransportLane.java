/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;

public class SourceTransportLane extends AbstractTransportLane
{
	private ComputerAddress		m_remoteServerAddress;
	private SocketAddress		m_localSocketAddress;

	public SourceTransportLane(AbstractProtocolHandler protocolHandler, ComputerAddress remoteServerAddress)
	{
		super(protocolHandler);

		// Selector is given for the life of the SourceTransportLane
		setSelectorThread(protocolHandler.getNextSelector());

		// TODO Support configurable local address
		InetAddress			inetAddress		= null;
		try
		{
			inetAddress = InetAddress.getByName(BasicTools.getComputerName());
			m_localSocketAddress = new InetSocketAddress(inetAddress, 0);
		}
		catch (UnknownHostException e)
		{
			m_localSocketAddress = null;
		}

		// Always do this to ensure the Highway picks-up the ComputerAddress.
		m_remoteServerAddress = remoteServerAddress;
		setTransportHighway(remoteServerAddress.getSourceHighway());
	}

	public void connect()
	{
		getSelectorThread().addTask(new DelayedConnect(this));
	}
	private void connectInternal()
	{
		synchronized (this)
		{
			if (null != getSocketChannel())
			{
				// connect() is called only out of band, either when a new server connection is created
				// or in the reconnect thread.
				return;
			}

			try
			{
				configureSocket(SocketChannel.open());
				setLaneStatus(LaneStatus.ConnectPending);
				if (null != m_localSocketAddress)
				{
					getSocketChannel().bind(m_localSocketAddress);
				}
				addWaitFor(SelectionKey.OP_CONNECT);
				m_remoteServerAddress.resolve();		// In case the generic address points somewhere else.
				getSocketChannel().connect(m_remoteServerAddress.getInetSocketAddress());

				getLogger().debug(BasicEvent.EVENT_SOCKET_CLIENT_OPEN_PENDING, "%s: Connection pending to %s using %s",
						getName(), toString(), m_localSocketAddress.toString());
			}
			catch (Exception exception)
			{
				getLogger().info(BasicEvent.EVENT_SOCKET_CLIENT_OPEN_FAILURE, exception,
						"Failed connection: " + getName());
				close(new BasicException(BasicEvent.EVENT_SOCKET_CLIENT_OPEN_FAILURE, "Failed to post open."), null);
			}
		}
	}

	public ComputerAddress getRemoteServerAddress()
	{
		return m_remoteServerAddress;
	}






	private static class DelayedConnect implements Runnable
	{
		private final SourceTransportLane m_transportLane;

		private DelayedConnect(SourceTransportLane transportLane)
		{
			m_transportLane = transportLane;
		}

		@Override
		public void run()
		{
			m_transportLane.connectInternal();
		}
	}
}
