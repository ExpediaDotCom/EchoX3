/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandlerManager;
import com.expedia.echox3.internal.transport.protocol.DestProtocolHandlerManager;
import com.expedia.echox3.internal.transport.request.dest.UnknownDestRequest;
import com.expedia.echox3.internal.transport.request.source.WorkSourceRequest;
import com.expedia.echox3.internal.transport.request.dest.AbstractDestRequest;
import com.expedia.echox3.internal.transport.request.dest.EchoDestRequest;
import com.expedia.echox3.internal.transport.request.dest.GetHighwayDestRequest;
import com.expedia.echox3.internal.transport.request.dest.GetTimeDestRequest;
import com.expedia.echox3.internal.transport.request.dest.GetVersionDestRequest;
import com.expedia.echox3.internal.transport.request.dest.SetHighwayDestRequest;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

public class DestMessageHandlerBase extends AbstractMessageHandler
{
	public static final String		GENERIC_CLASS_NAME		=
												DestMessageHandlerBase.class.getSimpleName().replace("Base", "");

	private static final String				SETTING_NAME_PROTOCOL_NAME		= ".ProtocolName";

	private AbstractProtocolHandlerManager	m_protocolHandlerManager;

	public DestMessageHandlerBase(String name)
	{
		super(name);

		setObjectPool(MessageType.UnknownMessageType, UnknownDestRequest::new);
		setObjectPool(MessageType.GetTime, GetTimeDestRequest::new);
		setObjectPool(MessageType.Echo, EchoDestRequest::new);
		setObjectPool(MessageType.Work, WorkSourceRequest::new);
		setObjectPool(MessageType.GetVersion, GetVersionDestRequest::new);
		setObjectPool(MessageType.GetHighwayNumber, GetHighwayDestRequest::new);
		setObjectPool(MessageType.SetHighwayNumber, SetHighwayDestRequest::new);

		setProtocolHandlerManager(DestProtocolHandlerManager.getInstance());
	}
	@Override
	public String getGenericClassName()
	{
		return GENERIC_CLASS_NAME;
	}

	@Override
	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		String		protocolName			= ConfigurationManager.getInstance().getSetting(
				getSettingName(SETTING_NAME_PROTOCOL_NAME), null);

		String		protocolHandlerName		= getProtocolHandlerName();
		if (null != protocolHandlerName && !protocolHandlerName.equals(protocolName))
		{
			getProtocolHandlerManager().deregisterMessageHandler(protocolHandlerName, this);
			setProtocolHandlerName(null);
		}
		if (null != protocolName && null == protocolHandlerName)
		{
			if (getProtocolHandlerManager().registerMessageHandler(protocolName, this))
			{
				setProtocolHandlerName(protocolName);
			}
		}
	}

	public AbstractProtocolHandlerManager getProtocolHandlerManager()
	{
		return m_protocolHandlerManager;
	}

	protected void setProtocolHandlerManager(AbstractProtocolHandlerManager protocolHandlerManager)
	{
		m_protocolHandlerManager = protocolHandlerManager;
	}

	@Override
	public boolean handleMessage(ReceiveMessage receiveMessage)
	{
		MessageType		messageType		= receiveMessage.getMessageType();
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_PROTOCOL_RECEIVE_REQUEST,
					"Received query message %d = %s from %s",
					receiveMessage.getClientContext(), receiveMessage.getMessageType().toString(),
					receiveMessage.getTransportLane().getTransportHighway().toString());
		}

		// serverRequest will release itself when done (i.e. after transmitResponse)
		// It may return from run() waiting for pending requests to other servers
		AbstractDestRequest		serverRequest	= (AbstractDestRequest) getRequest(messageType);
		if (null == serverRequest)
		{
			serverRequest = (AbstractDestRequest) getRequest(MessageType.UnknownMessageType);
		}
		if (null != serverRequest)
		{
			try
			{
				serverRequest.setReceiveMessage(receiveMessage);
				serverRequest.run();
			}
			catch (Exception exception)
			{
				// TODO Change log entry to counter???
				getLogger().warn(BasicEvent.EVENT_PROTOCOL_PROCESSING_EXCEPTION, exception,
						"Unexpected exception processing message %s", receiveMessage.toString());
			}
		}

		return null != serverRequest;
	}

	public ComputerAddress getListenAddress()
	{
		AbstractProtocolHandlerManager	protocolManager			= getProtocolHandlerManager();
		AbstractProtocolHandler			protocolHandler			= protocolManager.getProtocolHandler(
																						getProtocolHandlerName());
		return protocolHandler.getAddress();
	}

	@Override
	public void receiveShutdownNotification(String protocolHandlerName)
	{
		if (protocolHandlerName.equals(getProtocolHandlerName()))
		{
			getProtocolHandlerManager().deregisterMessageHandler(getProtocolHandlerName(), this);
			setProtocolHandlerName(null);
		}
	}

	@Override
	public void close()
	{
		super.close();

		if (null != getProtocolHandlerName())
		{
			getProtocolHandlerManager().deregisterMessageHandler(getProtocolHandlerName(), this);
			setProtocolHandlerName(null);
		}
	}
}
