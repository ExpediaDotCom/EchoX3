/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request;

import java.nio.channels.SelectionKey;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.message.AbstractSourceMessageHandler;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.SourceTransportLane;
import com.expedia.echox3.internal.transport.request.source.EchoSourceRequest;
import com.expedia.echox3.internal.transport.request.source.GetHighwaySourceRequest;
import com.expedia.echox3.internal.transport.request.source.GetTimeSourceRequest;
import com.expedia.echox3.internal.transport.request.source.GetVersionSourceRequest;
import com.expedia.echox3.internal.transport.request.source.SetHighwaySourceRequest;
import com.expedia.echox3.internal.transport.request.source.WorkSourceRequest;

public class SourceMessageHandlerBase extends AbstractSourceMessageHandler
{
	public static final String		GENERIC_CLASS_NAME		=
													SourceMessageHandlerBase.class.getSimpleName().replace("Base", "");

	public SourceMessageHandlerBase(String name, SourceProtocolHandler protocolHandler)
	{
		super(name, protocolHandler);

		setObjectPool(MessageType.GetTime, GetTimeSourceRequest::new);
		setObjectPool(MessageType.Echo, EchoSourceRequest::new);
		setObjectPool(MessageType.Work, WorkSourceRequest::new);
		setObjectPool(MessageType.GetVersion, GetVersionSourceRequest::new);
		setObjectPool(MessageType.GetHighwayNumber, GetHighwaySourceRequest::new);
		setObjectPool(MessageType.SetHighwayNumber, SetHighwaySourceRequest::new);
	}
	@Override
	public String getGenericClassName()
	{
		return GENERIC_CLASS_NAME;
	}

	// Common methods
	public byte[] echo(byte[] object) throws BasicException
	{
		EchoSourceRequest request		= (EchoSourceRequest) getRequest(MessageType.Echo);
		request.setObject(object);

		request.release();
		return new byte[0];
	}

	public void work(int burnUS, int sleepMS) throws BasicException
	{
		WorkSourceRequest request		= (WorkSourceRequest) getRequest(MessageType.Work);
		request.set(burnUS, sleepMS);

		request.release();
	}

	public long[] getTime(SourceTransportHighway transportHighway) throws BasicException
	{
		GetTimeSourceRequest request		= (GetTimeSourceRequest) getRequest(MessageType.GetTime);

		processRequestSynchronously(transportHighway, request);
		long[]						timeMS		= new long[2];
		timeMS[0]	= request.getTimeComputerMS();
		timeMS[1]	= request.getTimeCorrectedMS();

		request.release();
		return timeMS;
	}


	public Map<String, Map<String, String>> getVersion() throws BasicException
	{
		GetVersionSourceRequest request		= (GetVersionSourceRequest) getRequest(MessageType.GetVersion);

		request.release();
		return null;
	}

	public int getHighwayNumber(SourceTransportLane transportLane) throws BasicException
	{
		GetHighwaySourceRequest request				= (GetHighwaySourceRequest)
																			getRequest(MessageType.GetHighwayNumber);
		request.setObjectToNotify(this);
		request.composeTransmitMessage();
		((SourceTransportHighway) transportLane.getTransportHighway()).addPendingRequest(request);

		// Manually do the (async) transmission, to send to this specific transportLane
		TransmitMessage transmitMessage		= request.stealTransmitMessage();
		transmitMessage.setCounterFamily(transportLane.getProtocolHandler().getSocketCounterFamily());
		transportLane.setTransmitMessage(transmitMessage);
		transportLane.addWaitFor(SelectionKey.OP_WRITE);

		// Use the standard method to wait...
		waitForResponse(request);
		int							highwayNumber		= request.getHighwayNumber();

		request.release();
		return highwayNumber;
	}

	// Can only be called as part of the conditioning of the transport lane!
	public void setHighwayNumber(SourceTransportLane transportLane, int n) throws BasicException
	{
		SetHighwaySourceRequest request		= (SetHighwaySourceRequest) getRequest(MessageType.SetHighwayNumber);
		request.setObjectToNotify(this);
		request.setHighway(n);
		request.composeTransmitMessage();
		((SourceTransportHighway) transportLane.getTransportHighway()).addPendingRequest(request);

		// Manually do the (async) transmission, to send to this specific transportLane
		TransmitMessage					transmitMessage		= request.stealTransmitMessage();
		transmitMessage.setCounterFamily(transportLane.getProtocolHandler().getSocketCounterFamily());
		transportLane.setTransmitMessage(transmitMessage);
		transportLane.addWaitFor(SelectionKey.OP_WRITE);

		// Use the standard method to wait...
		waitForResponse(request);

		request.release();
	}
}
