/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.nio.ByteBuffer;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest.IRequestCompleteListener;

public abstract class AbstractSourceMessageHandler extends AbstractMessageHandler implements IRequestCompleteListener
{
	private SourceProtocolHandler			m_protocolHandler;

	protected AbstractSourceMessageHandler(String name, SourceProtocolHandler protocolHandler)
	{
		super(name);

		m_protocolHandler = protocolHandler;
		setProtocolHandlerName(m_protocolHandler.getProtocolName());
		m_protocolHandler.registerMessageHandler(this);
	}

	@Override
	public void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		// Specifically do NOT let the super.updateConfiguration code execute.
		// *SourceMessageHandlers handle their registration internally.
		// super.updateConfiguration();
	}

	public SourceProtocolHandler getProtocolHandler()
	{
		return m_protocolHandler;
	}

	public void processRequestSynchronously(SourceTransportHighway transportHighway,
			AbstractSourceRequest clientRequest) throws BasicException
	{
		transmitRequest(transportHighway, clientRequest, this);
		waitForResponse(clientRequest);
	}

	public void transmitRequest(SourceTransportHighway transportHighway,
			AbstractSourceRequest clientRequest, IRequestCompleteListener objectToNotify)
			throws BasicException
	{
		if (null == transportHighway)
		{
			throw new BasicException(BasicEvent.EVENT_TODO,
					"Transport highway not available for request %s", clientRequest.toString());
		}

		clientRequest.setObjectToNotify(objectToNotify);
		clientRequest.composeTransmitMessage();
		transportHighway.addPendingRequest(clientRequest);

		TransmitMessage		message		= clientRequest.stealTransmitMessage();
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_PROTOCOL_SEND_REQUEST, "Sending query message # %d = %s to %s",
					message.getClientContext(), message.m_messageType.toString(), transportHighway.toString());
		}

		transportHighway.transmit(message);
	}

	public void waitForResponse(AbstractSourceRequest clientRequest) throws BasicException
	{
		synchronized (clientRequest)
		{
			while (!clientRequest.isResponseReady())
			{
				// Wait for the object to not be waiting anymore...
				try
				{
					clientRequest.wait(clientRequest.getTimeoutMS() + 1000);
				}
				catch (InterruptedException e)
				{
					// Nothing to do here, keep trying
					clientRequest.setException(new BasicException(BasicEvent.EVENT_TODO, e, "???"));
				}
			}
		}
		BasicException		exception		= clientRequest.getException();
		if (null != exception)
		{
			throw exception;
		}
	}
	@Override
	public void processCompletedRequest(AbstractSourceRequest clientRequest)
	{
		// Must own the lock to notify
		synchronized (clientRequest)
		{
			clientRequest.notify();
		}
	}


	// Called in the worker thread pool
	@Override
	public boolean handleMessage(ReceiveMessage receiveMessage)
	{
		if (getLogger().isDebugEnabled())
		{
			getLogger().debug(BasicEvent.EVENT_PROTOCOL_RECEIVE_RESPONSE,
					"%s: Received response message %d = %s from %s",
					getName(),
					receiveMessage.getClientContext(), receiveMessage.m_messageType.toString(),
					receiveMessage.getTransportLane().getTransportHighway().toString());
		}

		AbstractTransportLane	transportLane		= receiveMessage.getTransportLane();
		TransportHighway		transportHighway	= transportLane.getTransportHighway();
		SourceTransportHighway clientHighway		= (SourceTransportHighway) transportHighway;
		long					clientContext		= receiveMessage.getClientContext();
		AbstractSourceRequest clientRequest		= clientHighway.removePendingRequest(clientContext);
		if (null == clientRequest)
		{
			// Might be some other message handler listening on the same ProtocolHandler
			return false;
		}

		// Let the clientRequest do the parsing and do its stuff.
		try
		{
			clientRequest.setReceiveMessage(receiveMessage);
			clientRequest.run();
		}
		catch (Exception exception)
		{

			ByteBuffer		byteBuffer			= receiveMessage.getContentByteBuffer();
			String			messageSection		= "Content";
			if (null == byteBuffer)
			{
				byteBuffer = receiveMessage.getHeaderByteBuffer();
				messageSection = "Header";
			}
			BasicException		basicException		= new BasicException(BasicEvent.EVENT_PROTOCOL_PARSE_ERROR,
					String.format("Failed to parse response to request %s at %s byte %,d of %,d",
						clientRequest.getClass().getSimpleName(),
						messageSection, byteBuffer.position(), byteBuffer.capacity()),
					exception);
			clientRequest.setException(basicException);
		}

		return true;
	}
}
