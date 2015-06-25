/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Level;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.SocketCounterFamily;

public abstract class AbstractTransportLane
{
	public enum LaneStatus
	{						//	Client		Server
		NotConnected,		//	0			0
		AcceptPending,		//				1
		ConfiguringSocket,	//	1			2
		ConnectPending,		//	2
		Connected,			//	3
		Conditioning,		//	4
		Validating,			//	5					// NYI
		Active				//	6			3
	}

	private static final BasicLogger LOGGER								= new BasicLogger(AbstractTransportLane.class);

	private static final String			SETTING_NAME_RECEIVE_BUFFER_KB	= ".ReceiveBufferKB";
	private static final String			SETTING_NAME_SEND_BUFFER_KB		= ".SendBufferKB";

	private AbstractProtocolHandler		m_protocolHandler;
	private TransportHighway			m_transportHighway;
	private String						m_name					= null;
	private volatile LaneStatus			m_laneStatus			= LaneStatus.NotConnected;
	private SocketChannel				m_socketChannel			= null;
	private SelectorThread				m_selectorThread		= null;
	private SelectionKey				m_key					= null;
	private ReceiveMessage				m_receiveMessage;
	private TransmitMessage				m_transmitMessage		= null;

	private long							m_connectTimeMS			= 0;
	private long							m_lastMessageTimeMS		= 0;
	private long							m_closeTimeMS			= 0;

	public AbstractTransportLane(AbstractProtocolHandler protocolHandler)
	{
		m_protocolHandler = protocolHandler;
		setTransportHighway(m_protocolHandler.getNewHighway());
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public LaneStatus getLaneStatus()
	{
		return m_laneStatus;
	}

	public void markOpen()
	{
		m_connectTimeMS = WallClock.getCurrentTimeMS();
		m_closeTimeMS = 0;
	}
	public void markClose()
	{
		if (0 != m_connectTimeMS)
		{
			m_closeTimeMS = WallClock.getCurrentTimeMS();
			m_connectTimeMS = 0;
		}
	}
	public long getConnectTimeMS()
	{
		return m_connectTimeMS;
	}

	public long getLastMessageTimeMS()
	{
		return m_lastMessageTimeMS;
	}

	public void setLastMessageTimeMS()
	{
		m_lastMessageTimeMS = WallClock.getCurrentTimeMS();
	}

	public long getCloseTimeMS()
	{
		return m_closeTimeMS;
	}

	public void setLaneStatus(LaneStatus laneStatus)
	{
		LaneStatus		statusPrev;
		synchronized (this)
		{
			statusPrev = m_laneStatus;
			getLogger().debug(BasicEvent.EVENT_TRANSPORT_LANE_STATUS_CHANGE, "%s: Changing status from %s to %s",
					getName(), m_laneStatus.name(), laneStatus.name());
			m_laneStatus = laneStatus;
		}

		getTransportHighway().notifyStatusChange(this, statusPrev, laneStatus);
	}

	public void setSelectorThread(SelectorThread selectorThread)
	{
		m_selectorThread = selectorThread;
	}
	public SelectorThread getSelectorThread()
	{
		return m_selectorThread;
	}
	public Selector getSelector()
	{
		return getSelectorThread().getSelector();
	}

	// Split configuration into components done before and after the call to connect
	// Applies to client only; Server always calls both in sequence
	public boolean configureSocket(SocketChannel socketChannel)
	{
		setLaneStatus(LaneStatus.ConfiguringSocket);
		m_socketChannel = socketChannel;
		Set<SocketOption<?>> set		= getSocketChannel().supportedOptions();

		String		settingNameReceiveKB	= getProtocolHandler().getSettingName(SETTING_NAME_RECEIVE_BUFFER_KB);
		String		settingNameSendKB		= getProtocolHandler().getSettingName(SETTING_NAME_SEND_BUFFER_KB);
		int			receiveKB				= ConfigurationManager.getInstance().getInt(settingNameReceiveKB, "100");
		int			sendKB					= ConfigurationManager.getInstance().getInt(settingNameSendKB, "100");
		// Is always blocking
		try
		{
			if (set.contains(StandardSocketOptions.SO_RCVBUF))
			{
				getSocketChannel().setOption(StandardSocketOptions.SO_RCVBUF, receiveKB * 1024);
			}
			if (set.contains(StandardSocketOptions.SO_SNDBUF))
			{
				getSocketChannel().setOption(StandardSocketOptions.SO_SNDBUF, sendKB * 1024);
			}
			if (set.contains(StandardSocketOptions.TCP_NODELAY))
			{
				getSocketChannel().setOption(StandardSocketOptions.TCP_NODELAY, true);
			}

			// TypeOfService, as per Datagram.setTrafficClass...
			// IPTOS_LOWCOST (0x02)
			// IPTOS_RELIABILITY (0x04)
			// IPTOS_THROUGHPUT (0x08)
			// IPTOS_LOWDELAY (0x10)
			try
			{
				// Guessing that this will return false in the case where the exception would be thrown below.
				if (set.contains(StandardSocketOptions.IP_TOS))
				{
					getSocketChannel().setOption(StandardSocketOptions.IP_TOS, 0x14);
				}
			}
			catch (Exception exception)
			{
				// Ignore and continue configuring
				// This exception may be seen on Windows Server 2003 and JDK 1.7 (at least some builds)
			}

			socketChannel.configureBlocking(false);
			m_key = getSocketChannel().register(getSelector(), SelectionKey.OP_READ, this);
		}
		catch (IOException e)
		{
			// TODO Handle configuration exception
			// TODO Log warning.
			// Still return success.
		}

		m_connectTimeMS = WallClock.getCurrentTimeMS();
		return true;
	}

	public String getName()
	{
		String			name;
		if (null == m_name)
		{
			try
			{
				name = String.format("%s(%s, socket = from %s to %s)",
						getClass().getSimpleName(), m_protocolHandler.getProtocolName(),
						m_socketChannel.getLocalAddress().toString(),
						m_socketChannel.getRemoteAddress().toString());
				m_name = name;
			}
			catch (Exception e)
			{
				name = String.format("%s(%s)",
						getClass().getSimpleName(), m_protocolHandler.getName());
			}
		}
		else
		{
			name = m_name;
		}

		return name;
	}

	public SocketChannel getSocketChannel()
	{
		return m_socketChannel;
	}


	public AbstractProtocolHandler getProtocolHandler()
	{
		return m_protocolHandler;
	}

	public SocketCounterFamily getCounterFamily()
	{
		return m_protocolHandler.getSocketCounterFamily();
	}

	public TransportHighway getTransportHighway()
	{
		return m_transportHighway;
	}

	public void setTransportHighway(TransportHighway highway)
	{
		// No change, do nothing => Avoid remove/put back same, as if first/last lane,
		// it would cause lane count to go to 0 and cause the highway to close.
		if (highway == m_transportHighway)		// NOPMD Really compare for the same object
		{
			return;
		}

		if (null != m_transportHighway)
		{
			m_transportHighway.removeTransportLane(this);
		}
		m_transportHighway = highway;
		if (null != highway)
		{
			highway.addTransportLane(this);
		}
	}

	public ReceiveMessage getReceiveMessage()
	{
		return m_receiveMessage;
	}

	public void setReceiveMessage(ReceiveMessage receiveMessage)
	{
		m_receiveMessage = receiveMessage;
		receiveMessage.setTransportLane(this);
	}

	public TransmitMessage getTransmitMessage()
	{
		return m_transmitMessage;
	}

	public void setTransmitMessage(TransmitMessage transmitMessage)
	{
		if (null != transmitMessage)
		{
			transmitMessage.getContentManagedByteBuffer().rewind();
			transmitMessage.markTransmitBegin();
			transmitMessage.setTransportLane(this);
		}
		m_transmitMessage = transmitMessage;
	}

	public boolean addWaitFor(int ops) throws BasicException
	{
		if (null == m_key)
		{
			throw new BasicException(BasicEvent.EVENT_SOCKET_CLOSED, "The socket is already closed.");
		}
		int			opsKey		= m_key.interestOps();
		int			opsNew		= opsKey | ops;
		boolean		isChanged	= opsKey != opsNew;

		if (isChanged)
		{
			m_key.interestOps(opsNew);
			getSelector().wakeup();			// Force the selector thread to wake-up immediately, for more immediate action.
		}

		return isChanged;
	}
	public void removeWaitFor(int ops)
	{
		if (null == m_key)
		{
			return;
		}
		int				opsKey		= m_key.interestOps();
		int				opsNew		= opsKey & ~ops;
		if (opsKey != opsNew)
		{
			m_key.interestOps(opsNew);
		}
	}

	public int read() throws BasicException
	{
		int			cbRead		= 0;
		if (null == m_receiveMessage.getContentByteBuffer())
		{
			cbRead += read(m_receiveMessage.getHeaderByteBuffer());
			if (!m_receiveMessage.getHeaderByteBuffer().hasRemaining())
			{
				// Header read complete. Side-effect of parsing is to create m_contentByteBuffer.
				boolean		isValidHeader	= m_receiveMessage.parseHeader();		// Parse is about 20 us
				if (!isValidHeader)
				{
					throw new BasicException(BasicEvent.EVENT_SOCKET_PROTOCOL_CHECKSUM,
							String.format("Header checksum validation failed for header %s.", toString()));
				}
				m_receiveMessage.setContentByteBuffer(getReceiveMessage().getContentSize());

				// Header is valid, start reading the content.
				m_receiveMessage.setReadContext(getCounterFamily().getReadMessageCounter().begin());
			}
		}

		ByteBuffer		contentByteBuffer		= m_receiveMessage.getContentByteBuffer();
		if (null != contentByteBuffer)
		{
			if (m_receiveMessage.getContentByteBuffer().hasRemaining())
			{
				cbRead += read(contentByteBuffer);
			}
			if (!contentByteBuffer.hasRemaining())
			{
				m_receiveMessage.endReadContext(true);
			}
		}
		return cbRead;
	}

	public int read(ByteBuffer byteBuffer) throws BasicException
	{
		int		cb;

		synchronized(this)		// TODO Review lock strategy: Can read and write to socket at same time?
		{
			try
			{
				cb = getSocketChannel().read(byteBuffer);
			}
			catch (IOException e)
			{
				throw new BasicException(BasicEvent.EVENT_SOCKET_EXCEPTION_READ,
						String.format("Failed to read from socket %s", getName()), e);
			}
		}
//		getLogger().info(BasicEvent.EVENT_DEBUG, "Read %d bytes", cb);

		return cb;
	}

	public int write() throws BasicException
	{
		int			cb;
		synchronized (this)
		{
			IOperationContext		context				= getCounterFamily().getWriteSingleCounter().begin();
			boolean					isSuccess			= true;
			try
			{
				TransmitMessage		transmitMessage		= getTransmitMessage();
				ByteBuffer			byteBuffer			= transmitMessage.getContentByteBuffer();
				cb = getSocketChannel().write(byteBuffer);
				if (!byteBuffer.hasRemaining())
				{
					transmitMessage.release();
					setTransmitMessage(null);
				}
			}
			catch (Exception e)
			{
				isSuccess = false;
				throw new BasicException(BasicEvent.EVENT_SOCKET_EXCEPTION_WRITE,
						String.format("Failed to write to socket %s", getName()), e);
			}
			finally
			{
				context.end(isSuccess);
			}
		}
//		getLogger().info(BasicEvent.EVENT_DEBUG, "Wrote %d bytes", cb);

		return cb;
	}

	@SuppressWarnings("PMD")		// TODO Is it possible/reasonable to simplify?
	public void close(BasicException exception, String reason)
	{
		synchronized (this)
		{
			if (null != getTransmitMessage())
			{
				getTransmitMessage().release();
				m_transmitMessage = null;
			}
			if (null != getReceiveMessage())
			{
				getReceiveMessage().release();
				m_receiveMessage = null;
			}
			if (null != m_key)
			{
				m_key.cancel();
				assert !m_key.isValid();
				m_key = null;
//				m_selectorThread = null;		// Does not need to be cleared, the key is what counts
			}

			if (null != getSocketChannel())
			{
				// Adjust logging level/frequency of logging.
				// NOTE: ALL close are recorded, the minimum level is INFO
				Level level		= null == exception ? Level.INFO : Level.WARN;
				if (null != exception || null != reason)
				{
					getLogger().log(level, BasicEvent.EVENT_SOCKET_CLOSING, exception,
							"Closing socket %s: %s", getName(), reason);
				}

				try
				{
					m_socketChannel.close();
				}
				catch (IOException e1)
				{
					// Ignore close exceptions.
					getLogger().error(BasicEvent.EVENT_TODO, e1,
							"Error closing the SocketChannel, there may be a memory leak.");
				}
				finally
				{
					m_socketChannel = null;
				}

				// TODO Clear any pending write
				// TODO Clear any pending requests
				// TODO Notify TransportLaneStatusListener

				markClose();
			}
			else
			{
				getLogger().info(BasicEvent.EVENT_SOCKET_CLOSED, exception, "Socket %s already closed", toString());
			}
		}
		setLaneStatus(LaneStatus.NotConnected);
	}

	public boolean isNotConnected()
	{
		return LaneStatus.NotConnected.equals(getLaneStatus());
	}
	public boolean isConnected()
	{
		return LaneStatus.Connected.ordinal() <= getLaneStatus().ordinal();
	}
	public boolean isActive()
	{
		return LaneStatus.Active.equals(getLaneStatus());
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
