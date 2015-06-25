/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetTimeDestRequest extends AbstractDestRequest
{
	private long		m_timeComputerMS;
	private long		m_timeCorrectedMS;

	public GetTimeDestRequest()
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
		variableSize += TransmitMessage.getLongSize();
		initTransmitMessage(getReceiveMessage().getClientContext(), variableSize);

		getTransmitMessage().getContentByteBuffer().putLong(m_timeComputerMS);
		getTransmitMessage().getContentByteBuffer().putLong(m_timeCorrectedMS);

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
		m_timeCorrectedMS	= WallClock.getCurrentTimeMS();
		m_timeComputerMS	= System.currentTimeMillis();
		markResponseReady();
	}
}
