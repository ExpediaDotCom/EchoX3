/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class WorkDestRequest extends AbstractDestRequest
{
	private int			m_burnUS;
	private int			m_sleepMS;

	public WorkDestRequest()
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
		m_burnUS  = receiveMessage.getContentByteBuffer().getInt();
		m_sleepMS = receiveMessage.getContentByteBuffer().getInt();
		return true;
	}

	@Override
	public void runInternal()
	{
		BasicTools.burnCpuUS(m_burnUS);
		BasicTools.sleepMS(m_sleepMS);
		markResponseReady();
	}
}
