/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetTimeSourceRequest extends AbstractSourceRequest
{
	private long		m_timeComputerMS		= 0;
	private long		m_timeCorrectedMS		= 0;

	public GetTimeSourceRequest()
	{
		super(MessageType.GetTime);
	}

	@Override
	public void release()
	{
		m_timeComputerMS	= 0;
		m_timeCorrectedMS	= 0;
		super.release();
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
		m_timeComputerMS = receiveMessage.getContentByteBuffer().getLong();
		m_timeCorrectedMS = receiveMessage.getContentByteBuffer().getLong();

		return true;
	}

	public long getTimeComputerMS()
	{
		return m_timeComputerMS;
	}

	public long getTimeCorrectedMS()
	{
		return m_timeCorrectedMS;
	}
}
