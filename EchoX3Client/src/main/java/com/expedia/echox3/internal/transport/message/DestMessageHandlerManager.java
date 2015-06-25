/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandlerManager;
import com.expedia.echox3.internal.transport.protocol.DestProtocolHandlerManager;

public class DestMessageHandlerManager extends AbstractMessageHandlerManager
{
	//CHECKSTYLE:OFF
	private static final DestMessageHandlerManager INSTANCE			= new DestMessageHandlerManager();
	//CHECKSTYLE:ON

	public DestMessageHandlerManager()
	{
		getProtocolHandlerManager().getPublisher().register(this::updateEachHandler);
	}

	public static DestMessageHandlerManager getInstance()
	{
		return INSTANCE;
	}

	@Override
	public final AbstractProtocolHandlerManager getProtocolHandlerManager()
	{
		return DestProtocolHandlerManager.getInstance();
	}
}
