/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.request.MessageType;

public class SetHighwayDestRequest extends AbstractDestRequest
{
	private int		m_highwayNumber;

	public SetHighwayDestRequest()
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

		initTransmitMessage(getReceiveMessage().getClientContext(), 0);

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
		m_highwayNumber = receiveMessage.getContentByteBuffer().getInt();
		return true;
	}

	@Override
	public void runInternal()
	{
		AbstractTransportLane			transportLane		= getReceiveMessage().getTransportLane();
		TransportHighway				oldHighway			= transportLane.getTransportHighway();
		AbstractProtocolHandler			protocolHandler		= transportLane.getProtocolHandler();
		Map<Integer, TransportHighway>	highwayMap			= protocolHandler.getHighwayMap();
		TransportHighway				newHighway			= highwayMap.get(m_highwayNumber);

		getLogger().info(BasicEvent.EVENT_TRANSPORT_LANE_JOIN_HIGHWAY,
				"Client transport lane %s leaving highway %,d, joining %,d",
				transportLane.toString(), oldHighway.getLocalNumber(), m_highwayNumber);

		transportLane.setTransportHighway(newHighway);

		markResponseReady();
	}
}
