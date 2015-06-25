/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.nio.ByteBuffer;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBuffer;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBufferManager;
import com.expedia.echox3.internal.transport.request.MessageType;

// Used for incoming messages which must be read in two parts (header, then variable portion)
public class ReceiveMessage extends AbstractMessage implements Runnable
{
	// ReceiveMessages are pooled, so these ByteBuffers are allocated only once at startup (or when the pool grows).
	private volatile ByteBuffer			m_headerByteBuffer		= ByteBuffer.allocate(HEADER_SIZE);
	private byte[]						m_checksumBytes			= new byte[HEADER_SIZE - (Integer.SIZE / 8)];
	private volatile ManagedByteBuffer	m_managedByteBuffer		= null;
	private volatile ByteBuffer			m_contentByteBuffer		= null;

	private IOperationContext			m_readContext			= null;

	public ReceiveMessage(ObjectPool<ByteArrayWrapper> byteArrayWrapperObjectPool)
	{
		super(byteArrayWrapperObjectPool);
	}

	@Override
	public void release()
	{
		m_headerByteBuffer.clear();
		if (null != m_managedByteBuffer)
		{
			m_managedByteBuffer.release();
			m_managedByteBuffer = null;
			m_contentByteBuffer = null;
		}

		if (null != m_readContext)
		{
			m_readContext.end(false);
			m_readContext = null;
		}

		super.release();
	}

	public void setReadContext(IOperationContext readContext)
	{
		m_readContext = readContext;
	}
	public void endReadContext(boolean isSuccess)
	{
		m_readContext.end(isSuccess);
		m_readContext = null;
	}

	public ByteBuffer getHeaderByteBuffer()
	{
		return m_headerByteBuffer;
	}

	public void setContentByteBuffer(int cb)
	{
		allocateByteBuffer(cb);
		m_contentSize = cb;
	}

	/**
	 * Used for testing.
	 * When setting directly the contentByteBuffer, the m_managedByteBuffer is not set
	 * and nothing gets released.
	 *
	 * @param managedByteBuffer
	 */
	public void setContent(ManagedByteBuffer managedByteBuffer)
	{
		m_managedByteBuffer = managedByteBuffer;
		m_contentByteBuffer = m_managedByteBuffer.getByteBuffer();
		m_managedByteBuffer.rewind();
	}
	private void allocateByteBuffer(int cb)
	{
		m_managedByteBuffer = ManagedByteBufferManager.get(cb);
		m_contentByteBuffer = m_managedByteBuffer.getByteBuffer();
	}

	@Override
	public ManagedByteBuffer getContentManagedByteBuffer()
	{
		return m_managedByteBuffer;
	}

	@Override
	public ByteBuffer getContentByteBuffer()
	{
		return m_contentByteBuffer;
	}

	// After parse, the Content byte buffer is ready to read data into it.
	// True = success, False = parsing error => communication is out of sync, close the socket.
	public boolean parseHeader()
	{
		m_headerByteBuffer.rewind();
		m_headerByteBuffer.get(m_protocolName);
		m_clientContext		= m_headerByteBuffer.getLong();
		int		number		= m_headerByteBuffer.getInt();
		m_messageType		= MessageType.getMessageType(number);
		m_contentSize		= m_headerByteBuffer.getInt();
		m_future			= m_headerByteBuffer.getInt();
		m_checksum			= m_headerByteBuffer.getInt();

		// Validate the checksum...
		m_headerByteBuffer.rewind();
		m_headerByteBuffer.get(m_checksumBytes);
		int		hashCode	= HashUtil.hashJava32(m_checksumBytes);

		return hashCode == m_checksum;
	}


	public boolean isComplete()
	{
		return null != getContentByteBuffer() && !getContentByteBuffer().hasRemaining();
	}

	@Override
	public void run()
	{
		// try even if processMessage does not throw, because paranoid is good.
		try
		{
			getTransportLane().getProtocolHandler().processMessage(this);
		}
		finally
		{
			// Give back to the ObjectPool when done.
			this.release();
		}
	}
}
