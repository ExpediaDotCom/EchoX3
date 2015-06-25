/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.message.AbstractMessage;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class EchoDestRequest extends AbstractDestRequest
{
	private ByteArrayWrapper		m_byteArrayWrapper		= null;

	public EchoDestRequest()
	{
		super(MessageType.Success);
	}

	@Override
	public void release()
	{
		if (null != m_byteArrayWrapper)
		{
			m_byteArrayWrapper.release();
			m_byteArrayWrapper = null;
		}
		super.release();
	}

	@Override
	public boolean composeTransmitMessage()
	{
		if (super.composeTransmitMessage())
		{
			return true;
		}

		int		variableSize		= 0;
		variableSize += AbstractMessage.getByteArraySize(m_byteArrayWrapper);
		initTransmitMessage(getReceiveMessage().getClientContext(), variableSize);

		getTransmitMessage().putByteArray(m_byteArrayWrapper);

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
		m_byteArrayWrapper = receiveMessage.getByteArray();
		return true;
	}

	@Override
	public void runInternal()
	{
		markResponseReady();
	}
}
