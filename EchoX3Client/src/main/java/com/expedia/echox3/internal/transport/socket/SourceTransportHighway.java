/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.util.Arrays;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.LoadBalancer;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane.LaneStatus;

public class SourceTransportHighway extends TransportHighway
		implements LoadBalancer.ILoadBalanced, Comparable<SourceTransportHighway>
{
	public static final long					serialVersionUID	= 20150601085959L;

	private static final int			ENTRY_COUNT_START			= 100;
	private static final int			ENTRY_COUNT_MIN				= ENTRY_COUNT_START * 2;

	private ComputerAddress				m_remoteAddress				= null;
	private final Object				m_pendingListLock			= new Object();
	private AbstractSourceRequest[]		m_pendingRequestList		= new AbstractSourceRequest[ENTRY_COUNT_START];
	private int							m_pendingRequestCount		= 0;

	public SourceTransportHighway(AbstractProtocolHandler protocolHandler)
	{
		super(protocolHandler);
	}

	public ComputerAddress getRemoteAddress()
	{
		return m_remoteAddress;
	}

	@Override
	public void notifyStatusChange(AbstractTransportLane transportLane, LaneStatus statusPrev, LaneStatus statusNew)
	{
		if (LaneStatus.NotConnected.equals(statusNew) && !LaneStatus.ConnectPending.equals(statusPrev))
		{
			// A connection has been lost, kick the reconnect thread once
			SourceProtocolHandler protocolHandler		= (SourceProtocolHandler) getProtocolHandler();
			protocolHandler.runKeepAlive();
		}
	}

	@Override
	public void addTransportLane(AbstractTransportLane lane)
	{
		super.addTransportLane(lane);
		if (null == m_remoteAddress && lane instanceof SourceTransportLane)
		{
			m_remoteAddress = ((SourceTransportLane) lane).getRemoteServerAddress();
		}
	}

	public void addPendingRequest(AbstractSourceRequest clientRequest)
	{
		synchronized (m_pendingListLock)
		{
			int i = 0;
			for (; i < m_pendingRequestList.length; i++)
			{
				if (null == m_pendingRequestList[i])
				{
					m_pendingRequestList[i] = clientRequest;
					break;
				}
			}

			if (i == m_pendingRequestList.length)
			{
				// No empty slots...
				int originalLength = m_pendingRequestList.length;
				m_pendingRequestList = Arrays.copyOf(m_pendingRequestList, m_pendingRequestList.length * 2);
				m_pendingRequestList[originalLength] = clientRequest;
			}

			m_pendingRequestCount++;
		}
	}

	public AbstractSourceRequest removePendingRequest(long clientContext)
	{
		AbstractSourceRequest clientRequest	= null;

		synchronized (m_pendingListLock)
		{
			for (int i = 0; i < m_pendingRequestList.length; i++)
			{
				if (null != m_pendingRequestList[i] && m_pendingRequestList[i].getClientContext() == clientContext)
				{
					// Found it...
					clientRequest = m_pendingRequestList[i];
					m_pendingRequestList[i] = null;
					m_pendingRequestCount--;
					break;
				}
			}
		}

		return clientRequest;
	}

	public void processPendingForTimeout(long timeMS)
	{
		synchronized (m_pendingListLock)
		{
			int iNewPosition = 0;
			for (int i = 0; i < m_pendingRequestList.length; i++)
			{
				AbstractSourceRequest clientRequest = m_pendingRequestList[i];
				if (null != clientRequest)
				{
					if (processClientRequest(clientRequest, timeMS))
					{
						m_pendingRequestCount--;
						m_pendingRequestList[i] = null;
					}
					else
					{
						m_pendingRequestList[iNewPosition++] = clientRequest;
					}
				}
			}
			Arrays.fill(m_pendingRequestList, iNewPosition, m_pendingRequestList.length, null);

			if ((iNewPosition < (m_pendingRequestList.length / 2))
					&& m_pendingRequestList.length > ENTRY_COUNT_MIN)
			{
				int length = Math.max(ENTRY_COUNT_MIN, iNewPosition * 2);
				m_pendingRequestList = Arrays.copyOf(m_pendingRequestList, length);
			}
		}
	}
	// return true if clientRequest is "processed" == has expired.
	private boolean processClientRequest(AbstractSourceRequest clientRequest, long timeMS)
	{
		long			timeoutMS		= clientRequest.getTimeTimeoutMS();
		boolean			isTimeout		= timeMS > timeoutMS;
		if (isTimeout)
		{
			String			message		= String.format("Request %s has timed-out with duration = %,d ms > %,d ms",
					clientRequest.toString(),
					timeMS - clientRequest.getTimeSubmittedMS(),
					clientRequest.getTimeoutMS());
			clientRequest.setException(new BasicException(BasicEvent.EVENT_PROTOCOL_TIMEOUT, message));
		}
		return isTimeout;
	}

	@Override
	public int getLoad()
	{
		return m_pendingRequestCount;
	}

	@Override
	public String toString()
	{
		return String.format("Highway(%s)", m_remoteAddress.toString());
	}

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 * <p>
	 * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
	 * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
	 * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
	 * <tt>y.compareTo(x)</tt> throws an exception.)
	 * <p>
	 * <p>The implementor must also ensure that the relation is transitive:
	 * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
	 * <tt>x.compareTo(z)&gt;0</tt>.
	 * <p>
	 * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
	 * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
	 * all <tt>z</tt>.
	 * <p>
	 * <p>It is strongly recommended, but <i>not</i> strictly required that
	 * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
	 * class that implements the <tt>Comparable</tt> interface and violates
	 * this condition should clearly indicate this fact.  The recommended
	 * language is "Note: this class has a natural ordering that is
	 * inconsistent with equals."
	 * <p>
	 * <p>In the foregoing description, the notation
	 * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
	 * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
	 * <tt>0</tt>, or <tt>1</tt> according to whether the value of
	 * <i>expression</i> is negative, zero or positive.
	 *
	 * @param o the object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object
	 * is less than, equal to, or greater than the specified object.
	 * @throws NullPointerException if the specified object is null
	 * @throws ClassCastException   if the specified object's type prevents it
	 *                              from being compared to this object.
	 */
	@Override
	public int compareTo(SourceTransportHighway o)
	{
		return getRemoteAddress().compareTo(o.getRemoteAddress());
	}
}
