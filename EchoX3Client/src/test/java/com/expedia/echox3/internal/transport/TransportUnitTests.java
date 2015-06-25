/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.histogram.IHistogram.BinData;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBufferManager;
import com.expedia.echox3.internal.transport.message.AbstractSourceMessageHandler;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.request.SourceMessageHandlerBase;
import com.expedia.echox3.internal.transport.request.DestMessageHandlerBase;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.DestProtocolHandler;
import com.expedia.echox3.internal.transport.message.TransmitMessage;
import com.expedia.echox3.internal.transport.socket.ComputerAddressGroup;
import com.expedia.echox3.internal.transport.request.MessageType;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;
import com.expedia.echox3.internal.transport.request.source.EchoSourceRequest;

public class TransportUnitTests extends AbstractTestTools
{
	@Test
	public void testLoopback() throws BasicException
	{
		String							testName				= logTestName();
		testName = testName.replace('.', '_');

		String							serverProtocolName		= testName + "Server";
		DestProtocolHandler serverProtocolHandler	= new DestProtocolHandler(
				serverProtocolName, "Null server used for loopback testing");
		assertNotNull(serverProtocolHandler.toString());
		serverProtocolHandler.updateConfiguration();
		assertNotNull(serverProtocolHandler.toString());

		AbstractMessageHandler			serverMessageHandler	= new DestMessageHandlerBase("ServerMessageHandler");
		assertNotNull(serverMessageHandler.toString());
		serverProtocolHandler.registerMessageHandler(serverMessageHandler);

		String							clientProtocolName		= testName + "Client";
		SourceProtocolHandler			clientProtocolHandler	= new SourceProtocolHandler(
				clientProtocolName, "Null client used for loopback testing");
		assertNotNull(clientProtocolHandler.toString());
		clientProtocolHandler.updateConfiguration();
		assertNotNull(clientProtocolHandler.toString());
		AbstractSourceMessageHandler	clientMessageHandler	= new SourceMessageHandlerBase(
																		"ClientMessageHandler", clientProtocolHandler);
		assertNotNull(clientMessageHandler.toString());
		ComputerAddressGroup			addressGroup	= new ComputerAddressGroup("Test");
		ComputerAddress					address			= new ComputerAddress(clientProtocolHandler.getSettingPrefix());
		address.resolve();
		addressGroup.addAddress(address);
		clientProtocolHandler.addAddressGroup(addressGroup);
		int			sleepTime	= 0;
		int			sleepMax	= 2 * 1000;
		TransportHighway				transportHighway		= null;
		while (sleepTime < sleepMax && (null == transportHighway || !transportHighway.isActive() ))
		{
			transportHighway = address.getSourceHighway();
//			transportHighway = clientProtocolHandler.getHighwayMap().values().iterator().next();
			BasicTools.sleepMS(100);
			sleepTime += 100;
		}
		getLogger().info(BasicEvent.EVENT_DEBUG, "Done waiting for TransportHighway after %,d ms", sleepTime);
		assertNotNull(transportHighway);

		Queue<TransmitMessage>			transmitQueue		= transportHighway.getTransmitQueue();
		IEchoThreadPool receiveThreadPool	= serverProtocolHandler.getWorkerThreadPool();

		getLogger().info(BasicEvent.EVENT_TEST, "Begin sending test messages");
		long							t1					= System.nanoTime();
		int								iMax				= 100 * 1000;
		int								iInform				= iMax / 20;
		int								iSleepMS			= 100;
		int								i;
		byte[]							bytes				= "The quick brown fox jumps...".getBytes();
		RequestCompleteListener			listener			= new RequestCompleteListener();
		for (i = 0; i < iMax; i++)
		{
			EchoSourceRequest clientRequest	= (EchoSourceRequest) clientMessageHandler.getRequest(MessageType.Echo);
			clientRequest.setObject(bytes);
			clientRequest.composeTransmitMessage();
			assertNotNull(clientRequest.getTransmitMessage());
			assertNotNull(clientRequest.getTransmitMessage().getContentByteBuffer());
			((SourceTransportHighway) transportHighway).addPendingRequest(clientRequest);
			clientRequest.setObjectToNotify(listener);
			TransmitMessage			transmitMessage		= clientRequest.stealTransmitMessage();
			assertNotNull(transmitMessage);
			assertNotNull(transmitMessage.getContentByteBuffer());
			transportHighway.transmit(transmitMessage);
			if (0 == (i % iInform))
			{
				showQueueSize("Sending", i, transmitQueue, receiveThreadPool.getQueueSize(), listener);
			}
		}
		getLogger().info(BasicEvent.EVENT_TEST, "Done sending test messages, begin waiting");
		long					t2					= System.nanoTime();
		while (listener.getCountTotal() < iMax)
		{
			showQueueSize("Waiting", i, transmitQueue, receiveThreadPool.getQueueSize(), listener);
			BasicTools.sleepMS(iSleepMS);
		}
		showQueueSize("Done waiting", i, transmitQueue, receiveThreadPool.getQueueSize(), listener);
		long					t3					= System.nanoTime();
		assertEquals(iMax, listener.getCountTotal());
		reportPerformance("submit only        ", t2 - t1, iMax, true);
		reportPerformance("send/receive only  ", t3 - t2, iMax, true);
		reportPerformance("submit/send/receive", t3 - t1, iMax, true);

		serverMessageHandler.close();
		dumpByteBufferHistogram();
	}
	private void showQueueSize(String message, int iSent,
			Collection transmitQueue, int queueSize, RequestCompleteListener listener)
	{
		getLogger().info(BasicEvent.EVENT_TEST,
				"%-15s: Sent = %,7d; TransmitQ = %,7d; ReceiveQ = %,7d; Success = %,7d; Failure = %,7d; Total = %,7d",
				message, iSent, transmitQueue.size(), queueSize,
				listener.getCountSuccess(), listener.getCountFailure(), listener.getCountTotal());
	}
	private void dumpByteBufferHistogram()
	{
		dumpByteBufferHistogram("Exact", ManagedByteBufferManager.getInstance().getHistogramExact());
		dumpByteBufferHistogram("Ladder", ManagedByteBufferManager.getInstance().getHistogramLadder());
		dumpByteBufferHistogram("Custom", ManagedByteBufferManager.getInstance().getHistogramCustom());
	}
	private void dumpByteBufferHistogram(String name, IHistogram histogram)
	{
		getLogger().info(BasicEvent.EVENT_DEBUG, "Histogram of %s objects", name);

		List<BinData>		listCustom		= histogram.getBinData();
		for (BinData binData : listCustom)
		{
			getLogger().info(BasicEvent.EVENT_DEBUG, "%,10.0f -> %,10.0f = %,d",
					binData.getMin(), binData.getMax(), binData.getCount());
		}
	}


/*
	private class TestServerMessageHandler extends ServerMessageHandlerBase
	{
		private String		m_protocolHandlerName;

		public TestServerMessageHandler(String protocolHandlerName)
		{
			super(TestServerMessageHandler.class.getSimpleName());

			m_protocolHandlerName = protocolHandlerName;
		}

		@Override
		public String getProtocolHandlerName()
		{
			return m_protocolHandlerName;
		}
	}
*/
	private class RequestCompleteListener implements AbstractSourceRequest.IRequestCompleteListener
	{
		private final AtomicInteger			m_countSuccess		= new AtomicInteger(0);
		private final AtomicInteger			m_countFailure		= new AtomicInteger(0);

		@Override
		public void processCompletedRequest(AbstractSourceRequest clientRequest)
		{
			if (null == clientRequest.getException())
			{
				m_countSuccess.incrementAndGet();
			}
			else
			{
				m_countFailure.incrementAndGet();
			}
			clientRequest.release();
		}

		public int getCountSuccess()
		{
			return m_countSuccess.get();
		}

		public int getCountFailure()
		{
			return m_countFailure.get();
		}

		public int getCountTotal()
		{
			return getCountSuccess() + getCountFailure();
		}
	}
}
