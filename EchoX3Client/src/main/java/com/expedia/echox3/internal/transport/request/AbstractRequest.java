/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.message.ReceiveMessage;
import com.expedia.echox3.internal.transport.message.TransmitMessage;

public abstract class AbstractRequest extends ObjectPool.AbstractPooledObject implements Runnable
{
	private static final BasicLogger 			LOGGER						= new BasicLogger(AbstractRequest.class);
	// There is one TransmitMessage object pool for each TransmitMessage type, by the SimpleClassName
	private static final Map<String, ObjectPool<TransmitMessage>>
												TRANSMIT_MESSAGE_OBJECT_POOL_MAP	= new HashMap<>();
	// The TransmitByteArrayWrapper pool is shared amongst all the TransmitMessage object pools.
	private static final ObjectPool<ByteArrayWrapper>
												TRANSMIT_BYTE_ARRAY_WRAPPER_POOL	= new ObjectPool<>(
			new StringGroup(ByteArrayWrapper.class.getSimpleName() + "." + AbstractRequest.class.getSimpleName()),
					ByteArrayWrapper::new);

	private MessageType							m_messageType;
	// The transmit requests are from a pool attached to each request type
	private final ObjectPool<TransmitMessage>	m_transmitMessageObjectPool;
	private TransmitMessage						m_transmitMessage;
	// The received messages are from a pool attached to the AbstractProtocolHandler - since the request type is not known until the message is received :)
	private ReceiveMessage						m_receiveMessage;			// Released in ReceiveMessage.run()
	private volatile boolean					m_isResponseReady			= false;
	private BasicException						m_exception					= null;

	protected AbstractRequest(MessageType messageType)
	{
		m_messageType = messageType;

		String			name		= TransmitMessage.class.getSimpleName() + "." + getClass().getSimpleName();
		synchronized (TRANSMIT_MESSAGE_OBJECT_POOL_MAP)
		{
			ObjectPool<TransmitMessage>		objectPool		= TRANSMIT_MESSAGE_OBJECT_POOL_MAP.get(name);
			if (null == objectPool)
			{
				objectPool = new ObjectPool<>(
						new StringGroup(name), () -> new TransmitMessage(TRANSMIT_BYTE_ARRAY_WRAPPER_POOL));
				TRANSMIT_MESSAGE_OBJECT_POOL_MAP.put(name, objectPool);
			}
			m_transmitMessageObjectPool = objectPool;
		}
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public MessageType getMessageType()
	{
		return m_messageType;
	}

	@Override
	public void release()
	{
		// ReceiveMessage released in ReceiveMessage.run()releases itself

		if (null != m_transmitMessage)
		{
			m_transmitMessage.release();
			m_transmitMessage = null;
		}
		m_isResponseReady = false;
		m_exception = null;
		super.release();
	}

	abstract public boolean composeTransmitMessage();
	public void initTransmitMessage(int size)
	{
		initTransmitMessage(0, size);
	}
	public void initTransmitMessage(long clientContext, int contentSize)
	{
		m_transmitMessage = m_transmitMessageObjectPool.get();
		m_transmitMessage.setMessageType(m_messageType);
		m_transmitMessage.set(clientContext, contentSize);

		// For server requests, this needs to be automatic
		if (null != m_receiveMessage)
		{
			getTransmitMessage().setProtocolName(m_receiveMessage);
		}
	}

	public TransmitMessage getTransmitMessage()
	{
		return m_transmitMessage;
	}
	public TransmitMessage stealTransmitMessage()
	{
		TransmitMessage		message		= m_transmitMessage;
		m_transmitMessage = null;
		return message;
	}

	public ReceiveMessage getReceiveMessage()
	{
		return m_receiveMessage;
	}

	public boolean setReceiveMessage(ReceiveMessage receiveMessage)
	{
		m_receiveMessage = receiveMessage;
		m_receiveMessage.getContentManagedByteBuffer().rewind();

		MessageType messageType		= getReceiveMessage().getMessageType();
		if (MessageType.Failure.equals(messageType))
		{
			ByteArrayWrapper		wrapper		= getReceiveMessage().getByteArray();
			try
			{
				Serializable	object		= BasicSerial.toObject(AbstractRequest.class.getSimpleName(), wrapper);
				m_exception = (BasicException) object;
				// There is much after which can be ignored...
				String			eventText		= getReceiveMessage().getString();
				getLogger().debug(BasicEvent.EVENT_TODO, "Receive message is an exception %s", eventText);
			}
			catch (BasicException e)
			{
				m_exception = new BasicException(BasicEvent.EVENT_PROTOCOL_PARSE_ERROR,
						"Failed to parse a failure response.", e);
				return false;
			}
			finally
			{
				wrapper.release();
			}
		}

		return true;
	}

	public void setException(BasicException exception)
	{
		m_exception = exception;
		markResponseReady();
	}

	public BasicException getException()
	{
		return m_exception;
	}

	protected void markResponseReady()
	{
		m_isResponseReady = true;
	}

	public boolean isResponseReady()
	{
		return m_isResponseReady;
	}

	abstract public void runInternal();

	@Override
	public void run()
	{
		try
		{
			runInternal();
		}
		catch (Exception exception)		// because I really am paranoid :)
		{
			setException(new BasicException(BasicEvent.EVENT_MESSAGE_PROCESSING_ERROR, exception,
					"Unexpected exception while processing request %s for message %s",
					getClass().getName(), getReceiveMessage().toString()));
		}
	}
}
