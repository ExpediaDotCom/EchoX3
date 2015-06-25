/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class UnknownDestRequest extends AbstractDestRequest
{
	public UnknownDestRequest()
	{
		super(MessageType.UnknownMessageType);
	}

	@Override
	public void release()
	{
		super.release();
	}

	@Override
	public boolean composeTransmitMessage()
	{
		// There is always an exception to return.
		return super.composeTransmitMessage();
	}

	@Override
	public boolean setReceiveMessage(ReceiveMessage receiveMessage)
	{
		return super.setReceiveMessage(receiveMessage);
	}

	@Override
	public void runInternal()
	{
		setException(new BasicException(BasicEvent.EVENT_MESSAGE_TYPE_UNKNOWN,
				"Unknown message type %s with content length %,d bytes.",
				getReceiveMessage().getMessageType().toString(),
				getReceiveMessage().getContentByteBuffer().remaining()));
	}
}
