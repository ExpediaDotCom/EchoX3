/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import com.expedia.echox3.basics.tools.pubsub.Publisher;

public class DestProtocolHandlerManager extends AbstractProtocolHandlerManager
{
	//CHECKSTYLE:OFF
	public static final String							PUBLISHER_NAME		= DestProtocolHandlerManager.class.getSimpleName();
	public static final Publisher						PUBLISHER			= Publisher.getPublisher(PUBLISHER_NAME);
	private static final DestProtocolHandlerManager INSTANCE			= new DestProtocolHandlerManager();
	//CHECKSTYLE:ON

	protected AbstractProtocolHandler createProtocolHandler(String protocolName, String description)
	{
		return new DestProtocolHandler(protocolName, description);
	}

	public static DestProtocolHandlerManager getInstance()
	{
		return INSTANCE;
	}

	public Publisher getPublisher()
	{
		return PUBLISHER;
	}
}
