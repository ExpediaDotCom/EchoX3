/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;
import com.expedia.echox3.internal.transport.request.AbstractRequest;

public abstract class AbstractDestRequest extends AbstractRequest
{
	protected AbstractDestRequest(MessageType messageType)
	{
		super(messageType);
	}

	@Override
	protected void markResponseReady()
	{
		super.markResponseReady();

		if (composeTransmitMessage())
		{
			sendResponse();
		}

		release();
	}

	protected void sendResponse()
	{
		TransmitMessage			transmitMessage		= stealTransmitMessage();
		AbstractTransportLane	transportLane		= getReceiveMessage().getTransportLane();
		TransportHighway		transportHighway	= null;
		if (null != transportLane)
		{
			transportHighway	= transportLane.getTransportHighway();
		}
		if (null == transportHighway)
		{
			// Drop the message
			transmitMessage.release();
			// TODO Counter
		}
		else
		{
			// Send this log entry to the AbstractMessageHandler's logger, so all 4 messages go to the same logger.
			if (AbstractMessageHandler.getLogger().isDebugEnabled())
			{
				AbstractMessageHandler.getLogger().debug(BasicEvent.EVENT_PROTOCOL_SEND_RESPONSE,
						"Sending response message %d = %s from %s",
						transmitMessage.getClientContext(), transmitMessage.getMessageType().toString(),
						transportHighway.toString());
			}

			try
			{
				transportHighway.transmit(transmitMessage);
			}
			catch (BasicException e)
			{
				// TODO Add counter
				getLogger().warn(BasicEvent.EVENT_COUNTER,
						"No lane to transmit message on TransportHighway %s", transportHighway.toString());
			}
		}
	}

	public boolean composeTransmitMessage()
	{
		BasicException		exception		= getException();
		if (null != exception)
		{
			byte[]			bytes;
			try
			{
				bytes = BasicSerial.toBytes(AbstractRequest.class.getSimpleName(), exception);
			}
			catch (BasicException e)
			{
				bytes = null;
			}
			String			eventText		= exception.getBasicEvent().toString();
			String			callStack		= exception.getCallStackChain();
			String			messageChain	= exception.getMessageChain();

			int				size			= 0;
			size += TransmitMessage.getByteArraySize(bytes);
			size += TransmitMessage.getStringSize(eventText);
			size += TransmitMessage.getStringSize(exception.getCallStackChain());
			size += TransmitMessage.getStringSize(exception.getMessageChain());

			initTransmitMessage(getReceiveMessage().getClientContext(), size);
			getTransmitMessage().setMessageType(MessageType.Failure);

			// Include the Java serialized version for Java callers and the cleaner version for other callers.
			getTransmitMessage().putByteArray(bytes);
			getTransmitMessage().putString(eventText);
			getTransmitMessage().putString(callStack);
			getTransmitMessage().putString(messageChain);

			return true;		// Done
		}
		else
		{
			return false;
		}
	}
}
