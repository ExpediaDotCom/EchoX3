/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;

import java.util.List;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.thread.EchoThreadPoolHot;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.internal.transport.message.AbstractMessageHandler;
import com.expedia.echox3.internal.transport.request.SourceMessageHandlerBase;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;
import com.expedia.echox3.internal.transport.socket.TransportHighway;
import com.expedia.echox3.internal.transport.socket.SourceTransportLane;
import com.expedia.echox3.internal.transport.request.source.GetHighwaySourceRequest;
import com.expedia.echox3.internal.transport.request.source.SetHighwaySourceRequest;

public class TransportLaneConditioner
{
	private static final TransportLaneConditioner		INSTANCE		= new TransportLaneConditioner();
	private final ObjectPool<TransportLaneRequest>		m_requestPool;
	private final IEchoThreadPool						m_threadPool;

	private TransportLaneConditioner()
	{
		// Make the thread name a bit smaller to fit within the logger limit of 25 characters :)
		m_threadPool = new EchoThreadPoolHot(getClass().getSimpleName().replace("TransportLane", "Lane"));

		String[]		nameList		= new String[1];
		nameList[0] = TransportLaneRequest.class.getSimpleName();
		m_requestPool = new ObjectPool<>(new StringGroup(nameList), TransportLaneRequest::new);
	}

	public static TransportLaneConditioner getInstance()
	{
		return INSTANCE;
	}

	public void condition(SourceTransportLane transportLane)
	{
		TransportLaneRequest		request		= m_requestPool.get();
		request.setTransportLane(transportLane);
		m_threadPool.execute(request);
	}



	public static class TransportLaneRequest extends ObjectPool.AbstractPooledObject implements Runnable
	{
		private SourceTransportLane m_transportLane;

		public TransportLaneRequest()
		{
		}

		public void setTransportLane(SourceTransportLane transportLane)
		{
			m_transportLane = transportLane;
		}

		public SourceTransportLane getTransportLane()
		{
			return m_transportLane;
		}

		@Override
		public void run()
		{
			try
			{
				conditionTransportLane();
			}
			catch (Exception exception)
			{
				getTransportLane().close(null,
						"Failed to condition the transport lane " + getTransportLane().toString());
			}

			release();		// Release this TransportLaneRequest
		}

		private void conditionTransportLane()
		{
			getTransportLane().setLaneStatus(AbstractTransportLane.LaneStatus.Conditioning);

			AbstractProtocolHandler			protocolHandler			= getTransportLane().getProtocolHandler();
			List<AbstractMessageHandler>	list					= protocolHandler.getMessageHandlerList();
			TransportHighway				transportHighway		= getTransportLane().getTransportHighway();
			ComputerAddress					address					= getTransportLane().getRemoteServerAddress();

			// Need a ClientMessageHandler, may need to wait at startup for a bit...
			while (list.isEmpty())
			{
				BasicTools.sleepMS(1);
			}
			AbstractMessageHandler			abstractMessageHandler	= list.get(0);
			SourceMessageHandlerBase		clientMessageHandler	= (SourceMessageHandlerBase) abstractMessageHandler;

			// If remote number not yet set on the highway, get the remote number of the current lane....
			int		laneHighwayNumber;
			try
			{
				laneHighwayNumber = clientMessageHandler.getHighwayNumber(getTransportLane());
			}
			catch (Exception e)
			{
				// Failure indicates bad connection, close it...
				BasicException	exception	= new BasicException(BasicEvent.EVENT_SOCKET_CONDITION_FAILED, e,
						"Failed to GetHighwayNumber for TransportLane %s", getTransportLane().toString());
				getTransportLane().close(exception, "Failed to " + GetHighwaySourceRequest.class.getSimpleName());
				return;
			}

			// Only one lane gets to set the remote number of the highway, whichever lane arrives here first.
			long		transportHighwayNumber;
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (transportHighway)
			{
				transportHighwayNumber = transportHighway.getRemoteNumber();
				if (0 == transportHighway.getRemoteNumber())
				{
					transportHighwayNumber = laneHighwayNumber;
					address.setServerHighway(laneHighwayNumber);
					transportHighway.setRemoteNumber(laneHighwayNumber);
				}
			}

			// Make sure every lane gets the same remote highway
			if (transportHighwayNumber != laneHighwayNumber)
			{
				try
				{
					clientMessageHandler.setHighwayNumber(getTransportLane(), transportHighway.getRemoteNumber());
				}
				catch (BasicException e)
				{
					// Failure indicates bad connection, close it...
					getTransportLane().close(e, "Failed to " + SetHighwaySourceRequest.class.getSimpleName());
					return;
				}
			}

			AbstractTransportLane.getLogger().info(BasicEvent.EVENT_TRANSPORT_LANE_CLIENT_READY,
					"TransportLane %s ready on highway %,d -> %,d",
					getTransportLane(),
					getTransportLane().getTransportHighway().getLocalNumber(),
					getTransportLane().getTransportHighway().getRemoteNumber());
			getTransportLane().setLaneStatus(AbstractTransportLane.LaneStatus.Active);
		}
	}
}
