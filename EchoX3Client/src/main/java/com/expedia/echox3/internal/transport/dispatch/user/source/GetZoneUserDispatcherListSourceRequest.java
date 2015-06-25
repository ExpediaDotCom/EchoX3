/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.dispatch.user.source;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
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
public class GetZoneUserDispatcherListSourceRequest extends AbstractSourceRequest
{
	private Map<String, List<ComputerAddress>>		m_zoneClientDispatcherMap		= null;

	public GetZoneUserDispatcherListSourceRequest()
	{
		super(DispatcherUserSourceMessageHandler.GetZoneUserDispatcherList);
	}

	@Override
	public void release()
	{
		m_zoneClientDispatcherMap = null;
		super.release();
	}

	public Map<String, List<ComputerAddress>> getZoneClientDispatcherMap()
	{
		return m_zoneClientDispatcherMap;
	}

	public boolean composeTransmitMessage()
	{
		initTransmitMessage(0);

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean setReceiveMessage(ReceiveMessage receiveMessage)
	{
		if (!super.setReceiveMessage(receiveMessage))
		{
			return false;
		}

		// Parse the response
		ByteArrayWrapper		wrapper		= receiveMessage.getByteArray();
		try
		{
			Serializable			serial		= BasicSerial.toObject("Dispatch", wrapper);
			m_zoneClientDispatcherMap = (Map) serial;
		}
		catch (BasicException e)
		{
			getLogger().error(BasicEvent.EVENT_TODO, e,
					"Failed to serialize the zoneDispatcher map. Contact dev.");
		}

		return true;
	}
}
