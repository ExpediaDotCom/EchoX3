/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.dispatch.user.source;

import java.util.List;

import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.transport.message.AbstractMessage;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

public class GetDispatcherListForCacheSourceRequest extends AbstractSourceRequest
{
	private String					m_cacheName			= null;
	private List<ComputerAddress>	m_addressList		= null;

	public GetDispatcherListForCacheSourceRequest()
	{
		super(DispatcherUserSourceMessageHandler.GetDispatcherListForCache);
	}

	public void setCacheName(String cacheName)
	{
		m_cacheName = cacheName;
	}

	@Override
	public void release()
	{
		if (null != m_addressList)
		{
			m_addressList = null;
		}
		super.release();
	}

	public boolean composeTransmitMessage()
	{
		int		variableSize		= 0;
		variableSize += AbstractMessage.getStringSize(m_cacheName);
		initTransmitMessage(variableSize);

		getTransmitMessage().putString(m_cacheName);

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
		m_addressList = receiveMessage.getAddressList();
		return true;
	}

	public List<ComputerAddress> getAddressList()
	{
		return m_addressList;
	}
}
