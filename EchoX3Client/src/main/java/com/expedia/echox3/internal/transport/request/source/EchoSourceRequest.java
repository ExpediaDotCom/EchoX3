/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class EchoSourceRequest extends AbstractSourceRequest
{
	private byte[]				m_object;
	private ByteArrayWrapper	m_back				= null;
	private long				m_timeBeginMS		= System.currentTimeMillis();
	private long				m_timeEndMS;

	public EchoSourceRequest()
	{
		super(MessageType.Echo);
	}

	public void setObject(byte[] object)
	{
		m_object = object;
	}

	@Override
	public void release()
	{
		if (null != m_back)
		{
			m_back.release();
			m_back = null;
		}
		super.release();
	}

	public boolean composeTransmitMessage()
	{
		int		variableSize		= 0;
		variableSize += TransmitMessage.getByteArraySize(m_object);
		initTransmitMessage(variableSize);

		getTransmitMessage().putByteArray(m_object);

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
		m_timeEndMS		= System.currentTimeMillis();
		m_back			= getReceiveMessage().getByteArray();
		return true;
	}

	public byte[] getObject()
	{
		return m_object;
	}

	public ByteArrayWrapper getBack()
	{
			return m_back;
	}

	public long getDurationMS()
	{
		return m_timeEndMS - m_timeBeginMS;
	}
}
