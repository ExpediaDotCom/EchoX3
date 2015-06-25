/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class WorkSourceRequest extends AbstractSourceRequest
{
	private int			m_burnUS;
	private int			m_sleepMS;

	public WorkSourceRequest()
	{
		super(MessageType.Work);
	}

	public void set(int burnUS, int sleepMS)
	{
		m_burnUS = burnUS;
		m_sleepMS = sleepMS;
	}

	public boolean composeTransmitMessage()
	{
		int		variableSize		= 0;
		variableSize += TransmitMessage.getIntSize();
		variableSize += TransmitMessage.getIntSize();
		initTransmitMessage(variableSize);

		getTransmitMessage().getContentByteBuffer().putInt(m_burnUS);
		getTransmitMessage().getContentByteBuffer().putInt(m_sleepMS);

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
