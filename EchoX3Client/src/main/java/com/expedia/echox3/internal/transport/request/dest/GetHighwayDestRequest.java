/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetHighwayDestRequest extends AbstractDestRequest
{
	private int		m_highway;

	public GetHighwayDestRequest()
	{
		super(MessageType.Success);
	}

	@Override
	public boolean composeTransmitMessage()
	{
		if (super.composeTransmitMessage())
		{
			return true;
		}

		int		variableSize		= 0;
		variableSize += TransmitMessage.getLongSize();
		initTransmitMessage(getReceiveMessage().getClientContext(), variableSize);

		getTransmitMessage().getContentByteBuffer().putInt(m_highway);

		return true;
	}

	@Override
	public boolean setReceiveMessage(ReceiveMessage receiveMessage)
	{
		if (!super.setReceiveMessage(receiveMessage))
		{
			return false;
		}

		// Parse the request
		return true;
	}

	@Override
	public void runInternal()
	{
		AbstractTransportLane		transportLane		= getReceiveMessage().getTransportLane();
		TransportHighway			transportHighway;
		if (null != transportLane)
		{
			transportHighway = transportLane.getTransportHighway();
		}
		else
		{
			transportHighway = null;
		}

		if (null != transportHighway)
		{
			m_highway = transportHighway.getLocalNumber();
		}
		else
		{
			m_highway = 0;
		}

		markResponseReady();
	}
}
