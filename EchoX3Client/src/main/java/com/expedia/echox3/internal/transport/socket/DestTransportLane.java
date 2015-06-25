/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.transport.protocol.DestProtocolHandler;

public class DestTransportLane extends AbstractTransportLane
{
	public DestTransportLane(DestProtocolHandler transportHandler)
	{
		super(transportHandler);

		setLaneStatus(LaneStatus.AcceptPending);
	}

	@Override
	public void close(BasicException exception, String reason)
	{
		super.close(exception, reason);

		// Remove from the highway ONLY on the server side.
		// On the client side, the lane continues to exist and will get reconnected ...
		// ... and will be reopened when it reconnects ... on a new ServerTransportLane object.
		setTransportHighway(null);
	}
}
