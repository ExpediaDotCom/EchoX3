/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBuffer;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBufferManager;

// Used for outgoing messages, composed as a single buffer, for a single write
public class TransmitMessage extends AbstractMessage
{
	private static final AtomicLong			CONTEXT_COUNTER			= new AtomicLong(0);

	// Single buffer for write. Header or Content depends on position.
	private ManagedByteBuffer		m_managedByteBuffer;
	private ByteBuffer				m_byteBuffer;
	private byte[]					m_checksumBytes			= new byte[HEADER_SIZE - (Integer.SIZE / 8)];

	private IOperationContext		m_queueContext			= null;
	private IOperationContext		m_transmitContext		= null;

	public TransmitMessage(ObjectPool<ByteArrayWrapper> byteArrayWrapperObjectPool)
	{
		super(byteArrayWrapperObjectPool);
	}

	@Override
	public void release()
	{
		if (null != m_queueContext)
		{
			m_queueContext.end(false);
			m_queueContext = null;
		}
		if (null != m_transmitContext)
		{
			m_transmitContext.end(true);
			m_transmitContext = null;
		}

		if (null != m_managedByteBuffer)
		{
			m_managedByteBuffer.release();
			m_managedByteBuffer = null;
			m_byteBuffer = null;
		}
		super.release();
	}

	public void markInQueue()
	{
		m_queueContext = getCounterFamily().getWriteQueueCounter().begin();
	}
	public void markTransmitBegin()
	{
		if (null != m_queueContext)
		{
			// Messages related to HighwayNumber are not queued.
			m_queueContext.end(true);
			m_queueContext = null;
		}
		m_transmitContext = getCounterFamily().getWriteTotalCounter().begin();
	}
	public void abortTransmit()
	{
		if (null != m_queueContext)
		{
			m_queueContext.end(false);
			m_queueContext = null;
		}
		if (null != m_transmitContext)
		{
			m_transmitContext.end(false);
			m_transmitContext = null;
		}
	}

	// After set, the byte buffer is ready to write content to it.
	// Use this entry point for creating a request Transmit message
	public void set(int contentSize)
	{
		set(0, contentSize);
	}
	// Use this entry point to create a response Transmit message
	public void set(long context, int contentSize)
	{
		if (0 == context)
		{
			context = CONTEXT_COUNTER.incrementAndGet();
		}

		m_clientContext = context;
		m_contentSize = contentSize;

		allocateByteBuffer(HEADER_SIZE + contentSize);
		m_byteBuffer.put(m_protocolName);
		m_byteBuffer.putLong(m_clientContext);
		m_byteBuffer.putInt(m_messageType.getNumber());
		m_byteBuffer.putInt(m_contentSize);
		m_byteBuffer.putInt(m_future);

		m_managedByteBuffer.rewind();
		m_byteBuffer.get(m_checksumBytes, 0, m_checksumBytes.length);
//int		checksum = Arrays.hashCode(m_checksumBytes);
		m_checksum = HashUtil.hashJava32(m_checksumBytes);
		m_byteBuffer.putInt(m_checksum);
	}
	private void allocateByteBuffer(int cb)
	{
		m_managedByteBuffer = ManagedByteBufferManager.get(cb);
		m_byteBuffer = m_managedByteBuffer.getByteBuffer();
	}

	@Override
	public ManagedByteBuffer getContentManagedByteBuffer()
	{
		return m_managedByteBuffer;
	}

	@Override
	public ByteBuffer getContentByteBuffer()
	{
		return m_byteBuffer;
	}

}
