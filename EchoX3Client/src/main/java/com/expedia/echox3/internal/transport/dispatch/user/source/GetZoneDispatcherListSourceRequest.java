/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.dispatch.user.source;

import java.util.List;

import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

/**
 * Although on the Client API, this entry point returns the list of ComputerAddress
 * to access the Admin side of the Dispatchers.
 * This is because, at startup, a new dispatcher only has access to the bootstrap information
 * which only contains the client ComputerAddress of other dispatchers.
 * This entry point allows the dispatcher to access the list of dispatchers in its zone.
 */
public class GetZoneDispatcherListSourceRequest extends AbstractSourceRequest
{
	private String						m_zone				= null;
	private List<ComputerAddress>		m_adminList			= null;
	private List<ComputerAddress>		m_userList			= null;

	public GetZoneDispatcherListSourceRequest()
	{
		super(DispatcherUserSourceMessageHandler.GetZoneDispatcherList);
	}

	@Override
	public void release()
	{
		m_adminList		= null;
		m_userList		= null;
		m_zone			= null;
		super.release();
	}

	public String getZone()
	{
		return m_zone;
	}

	public List<ComputerAddress> getAdminList()
	{
		return m_adminList;
	}

	public List<ComputerAddress> getUserList()
	{
		return m_userList;
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
		m_zone			= receiveMessage.getString();
		m_adminList		= receiveMessage.getAddressList();
		m_userList		= receiveMessage.getAddressList();

		return true;
	}
}
