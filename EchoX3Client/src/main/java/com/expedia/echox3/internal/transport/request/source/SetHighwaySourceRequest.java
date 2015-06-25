/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class SetHighwaySourceRequest extends AbstractSourceRequest
{
	private int		m_highway;

	public SetHighwaySourceRequest()
	{
		super(MessageType.SetHighwayNumber);
	}

	public void setHighway(int highway)
	{
		m_highway = highway;
	}

	public boolean composeTransmitMessage()
	{
		int		variableSize		= 0;
		variableSize += TransmitMessage.getLongSize();
		initTransmitMessage(variableSize);

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

		// Parse the response
		return true;
	}
}
