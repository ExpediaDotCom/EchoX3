/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request.source;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.internal.transport.request.AbstractRequest;
import com.expedia.echox3.internal.transport.request.MessageType;

public abstract class AbstractSourceRequest extends AbstractRequest
{
	private static final TimeoutManager			TIMEOUT_MANAGER			= new TimeoutManager();

	private long								m_clientContext;					// Because the TransmitMessage is stolen before the response arrives
	private long								m_timeoutMS;						// timeout
	private long								m_timeSubmittedMS;					// WallClock time when the request is submitted.
	private long								m_timeResponseMS;					// WallClock time when the response is ready
	private long								m_timeTimeoutMS;					// WallClock time when the request will timeout.
	private IRequestCompleteListener			m_objectToNotify		= null;

	protected AbstractSourceRequest(MessageType messageType)
	{
		super(messageType);
	}

	@Override
	public void release()
	{
		m_objectToNotify = null;
		super.release();
	}

	public IRequestCompleteListener getObjectToNotify()
	{
		return m_objectToNotify;
	}

	public void setObjectToNotify(IRequestCompleteListener objectToNotify)
	{
		m_objectToNotify		= objectToNotify;
		m_timeoutMS				= calculateTimeout();
		m_timeSubmittedMS		= System.currentTimeMillis();
		m_timeResponseMS		= 0;
		m_timeTimeoutMS			= m_timeSubmittedMS + m_timeoutMS;
	}
	protected long calculateTimeout()
	{
		return TIMEOUT_MANAGER.getTimeout(this, getMessageType(), 1);
	}

	@Override
	public void initTransmitMessage(int size)
	{
		super.initTransmitMessage(size);
		setClientContext(getTransmitMessage().getClientContext());
	}

	public long getClientContext()
	{
		return m_clientContext;
	}

	public void setClientContext(long clientContext)
	{
		m_clientContext = clientContext;
	}

	public long getTimeoutMS()
	{
		return m_timeoutMS;
	}

	public long getTimeSubmittedMS()
	{
		return m_timeSubmittedMS;
	}

	public long getTimeResponseMS()
	{
		return m_timeResponseMS;
	}

	public long getTimeTimeoutMS()
	{
		return m_timeTimeoutMS;
	}

	public long getDurationMS()
	{
		return getTimeResponseMS() - getTimeSubmittedMS();
	}

	@Override
	protected void markResponseReady()
	{
		m_timeResponseMS = System.currentTimeMillis();
		super.markResponseReady();

		if (null != getObjectToNotify())
		{
			getObjectToNotify().processCompletedRequest(this);
		}
	}

	@Override
	public void runInternal()
	{
		markResponseReady();
	}

	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString()
	{
		return String.format("%s: Context %,d", getClass().getSimpleName(), getClientContext());
	}

	public interface IRequestCompleteListener
	{
		void processCompletedRequest(AbstractSourceRequest clientRequest);
	}

	/**
	 * Manages the raw data from which the timeout of individual requests are calculated.
	 * The data comes from configuration.
	 * The setting names are <RequestClassName>.[Timeout|Increment][Number|Units], with full inheritance.
	 *
	 * A timeout is :	CalculatedTimeout = timeout + (count * increment)
	 *
	 * The data is stored in arrays indexed by the message number for fast retrieval.
	 * The data is kept by the singleton TimeoutManager to keep only one copy, read from configuration once, ...
	 *
	 */
	private static class TimeoutManager
	{
		private static final ConfigurationManager	CONFIGURATION_MANAGER		= ConfigurationManager.getInstance();

		private static final String			FORMAT_BASE				= "%s.Timeout";
		private static final String			FORMAT_INCREMENT		= "%s.Increment";
		private static final String			DEFAULT_BASE			= "2000";
		private static final String			DEFAULT_INCREMENT		= "20";

		private String[]		m_classNameList			= new String[MessageType.MESSAGE_COUNT_MAX];
		private long[]			m_timeoutList			= new long[MessageType.MESSAGE_COUNT_MAX];
		private long[]			m_incrementList			= new long[MessageType.MESSAGE_COUNT_MAX];

		public TimeoutManager()
		{
			Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
		}

		public void updateConfiguration(String publisherName, long timeMS, Object event)
		{
			// Update the timeout list...

			for (int i = 0; i < MessageType.MESSAGE_COUNT_MAX; i++)
			{
				if (null != m_classNameList[i])
				{
					updateConfiguration(i);
				}
			}
		}
		private void updateConfiguration(int messageNumber)
		{
			String		baseName			= String.format(FORMAT_BASE, m_classNameList[messageNumber]);
			String		incrementName		= String.format(FORMAT_INCREMENT, m_classNameList[messageNumber]);

			//CHECKSTYLE:OFF
			m_timeoutList[messageNumber]		= CONFIGURATION_MANAGER.getTimeInterval(baseName, DEFAULT_BASE, "ms");
			m_incrementList[messageNumber]	= CONFIGURATION_MANAGER.getTimeInterval(incrementName, DEFAULT_INCREMENT, "ms");
			//CHECKSTYLE:ON
		}

		public long getTimeout(AbstractSourceRequest request, MessageType messageType, int count)
		{
			int			messageNumber	= messageType.getNumber();

			if (null == m_classNameList[messageNumber])
			{
				// As of yet unknown message, register it...
				m_classNameList[messageNumber]		= request.getClass().getName();
				updateConfiguration(messageNumber);
			}

			return m_timeoutList[messageNumber] + (count * m_incrementList[messageNumber]);
		}
	}
}
