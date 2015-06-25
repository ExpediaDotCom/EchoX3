/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import java.io.Serializable;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetVersionSourceRequest extends AbstractSourceRequest
{
	private Map<String, ManifestWrapper>		m_manifestMap;

	public GetVersionSourceRequest()
	{
		super(MessageType.GetVersion);
	}

	public boolean composeTransmitMessage()
	{
		initTransmitMessage(0);

		return true;
	}

	@Override
	@SuppressWarnings("unckecked")
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
			Serializable	response	= BasicSerial.toObject(getReceiveMessage().getProtocolName(), wrapper);
			if (response instanceof Map)
			{
				m_manifestMap = (Map<String, ManifestWrapper>) response;
			}
/*
			else
			{
				// TODO Throw an exception
			}
*/
		}
		catch (BasicException exception)
		{
			getLogger().error(BasicEvent.EVENT_PROTOCOL_PARSE_ERROR, exception, "Failed to parse version response");
		}
		finally
		{
			wrapper.release();
		}
		return true;
	}

	public Map<String, ManifestWrapper> getManifestMap()
	{
		return m_manifestMap;
	}
}
