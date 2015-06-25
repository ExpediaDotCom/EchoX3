/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.dest;

import java.io.Serializable;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.request.MessageType;

public class GetVersionDestRequest extends AbstractDestRequest
{
	private static final String				MANIFEST_FILENAME	= "META-INF/MANIFEST.MF";

	// Because Manifest is not Serializable :(
	// Note: calculate only once.
	private static byte[]		s_manifestBytes		= null;

	public GetVersionDestRequest()
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
		variableSize += TransmitMessage.getByteArraySize(s_manifestBytes);
		initTransmitMessage(getReceiveMessage().getClientContext(), variableSize);

		getTransmitMessage().putByteArray(s_manifestBytes);

		return true;
	}

	@Override
	public void runInternal()
	{
		if (null != s_manifestBytes)
		{
			// need to calculate it only once!
			return;
		}

		Map<String, ManifestWrapper>	manifestMap		= null;
		if (null == getException())
		{
			try
			{
				manifestMap = ManifestWrapper.getManifestMap();
			}
			catch (Exception e)
			{
				setException(new BasicException(BasicEvent.EVENT_PROTOCOL_GET_VERSION_ERROR,
						String.format("List of files '%s' not available.", MANIFEST_FILENAME), e));
			}
		}

		if (null == getException() && null != manifestMap)
		{
			try
			{
				//CHECKSTYLE:OFF
				s_manifestBytes = BasicSerial.toBytes(getReceiveMessage().getProtocolName(), (Serializable) manifestMap);
				//CHECKSTYLE:ON
			}
			catch (BasicException exception)
			{
				setException(exception);
			}
		}

		markResponseReady();
	}
}
