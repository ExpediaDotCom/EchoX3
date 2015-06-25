/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.message.DestMessageHandlerManager;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.DestProtocolHandlerManager;

public class TransportManagersTests extends AbstractTestTools
{
	@Test
	public void testServerProtocolHandlerManager() throws IOException, URISyntaxException
	{
		logTestName();

		// Make sure the manager is loaded
		//CHECKSTYLE:OFF
		DestProtocolHandlerManager	serverProtocolHandlerManager	= DestProtocolHandlerManager.getInstance();
		DestMessageHandlerManager	serverMessageHandlerManager		= DestMessageHandlerManager.getInstance();

		Map<String, AbstractProtocolHandler>	serverProtocolHandlerMap		= serverProtocolHandlerManager.getProtocolHandlerMap();
		Map<String, AbstractMessageHandler>		serverMessageHandlerMap			= serverMessageHandlerManager.getMessageHandlerMap();
		//CHECKSTYLE:ON


		assertEquals(2, serverProtocolHandlerMap.size());
		assertEquals(1, serverMessageHandlerMap.size());

		AbstractProtocolHandler			serverProtocolHandler		= serverProtocolHandlerMap.get("ProtocolDest1");

		List<AbstractMessageHandler>	serverMessageHandlerList	= serverProtocolHandler.getMessageHandlerList();
		assertEquals(1, serverMessageHandlerList.size());
		assertTrue(serverMessageHandlerMap.get("MessageDest1") == serverMessageHandlerList.get(0));
	}
}
