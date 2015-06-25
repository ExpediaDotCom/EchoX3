/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetHighwaySourceRequest extends AbstractSourceRequest
{
	private int		m_highwayNumber;

	public GetHighwaySourceRequest()
	{
		super(MessageType.GetHighwayNumber);
	}

	public int getHighwayNumber()
	{
		return m_highwayNumber;
	}

	public boolean composeTransmitMessage()
	{
		initTransmitMessage(0);

		return true;
	}

	@Override
	public boolean setReceiveMessage(ReceiveMessage receiveMessage)
	{
		if (!super.setReceiveMessage(receiveMessage))
		{
			return false;
		}

		// Parse the response
		m_highwayNumber = receiveMessage.getContentByteBuffer().getInt();
		return true;
	}
}
