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
public class UserReadOnlyMultipleSourceRequest extends UserSourceRequest
{
	private byte[][]	m_keyList;
	private byte[]		m_request;

	private ByteArrayWrapper[]		m_responseList;

	public UserReadOnlyMultipleSourceRequest()
	{
		super(DispatcherUserSourceMessageHandler.UserReadOnlySingle);
	}

	@Override
	public void release()
	{
		m_keyList	= null;
		m_request	= null;

		if (null != m_responseList)
		{
			for (int i = 0; i < m_responseList.length; i++)
			{
				m_responseList[i].release();
			}
			m_responseList = null;
		}

		super.release();
	}

	public void setKeyList(byte[][] keyList)
	{
		m_keyList = keyList;
	}

	public void setRequest(byte[] request)
	{
		m_request = request;
	}

	public ByteArrayWrapper[] getResponseList()
	{
		return m_responseList;
	}

	public boolean composeTransmitMessage()
	{
		int			size		= 0;
		size += TransmitMessage.getStringSize(getCacheName());
		size += TransmitMessage.getByteArrayListSize(m_keyList);
		size += TransmitMessage.getByteArraySize(m_request);

		initTransmitMessage(size);

		getTransmitMessage().putString(getCacheName());
		getTransmitMessage().putByteArrayList(m_keyList);
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
		m_responseList	= receiveMessage.getByteArrayList();

		return true;
	}
}
