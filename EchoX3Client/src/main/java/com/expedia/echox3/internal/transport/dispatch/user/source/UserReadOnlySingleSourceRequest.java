/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.dispatch.user.source;

import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;

/**
 * Although on the Client API, this entry point returns the list of ComputerAddress
 * to access the Admin side of the Dispatchers.
 * This is because, at startup, a new dispatcher only has access to the bootstrap information
 * which only contains the client ComputerAddress of other dispatchers.
 * This entry point allows the dispatcher to access the list of dispatchers in its zone.
 */
public class UserReadOnlySingleSourceRequest extends UserSourceRequest
{
	private byte[]				m_key;
	private byte[]				m_request;
	private ByteArrayWrapper	m_response;

	public UserReadOnlySingleSourceRequest()
	{
		super(DispatcherUserSourceMessageHandler.UserReadOnlySingle);
	}

	@Override
	public void release()
	{
		m_key		= null;
		m_request	= null;
		if (null != m_response)
		{
			m_response.release();
			m_response = null;
		}

		super.release();
	}


	public boolean composeTransmitMessage()
	{
		int			size		= 0;
		size += TransmitMessage.getStringSize(getCacheName());
		size += TransmitMessage.getByteArraySize(m_key);
		size += TransmitMessage.getByteArraySize(m_request);

		initTransmitMessage(size);

		getTransmitMessage().putString(getCacheName());
		getTransmitMessage().putByteArray(m_key);
		getTransmitMessage().putByteArray(m_request);

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
		m_response = receiveMessage.getByteArray();

		return true;
	}
}
