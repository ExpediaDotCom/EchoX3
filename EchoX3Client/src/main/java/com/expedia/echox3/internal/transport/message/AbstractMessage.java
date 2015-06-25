/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.internal.transport.buffer.ManagedByteBuffer;
import com.expedia.echox3.internal.transport.protocol.SocketCounterFamily;
import com.expedia.echox3.internal.transport.socket.AbstractTransportLane;
import com.expedia.echox3.internal.transport.request.MessageType;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

public abstract class AbstractMessage extends ObjectPool.AbstractPooledObject
{
	public static final String				CLIENT_PREFIX			= "EX3";
	public static final String				CLIENT_JAVA				= "J";
	public static final String				CLIENT_CPP				= "C";
	public static final String				CLIENT_C_SHARP			= "#";
	public static final String				CLIENT_C_LINUX			= "L";
	public static final String				CLIENT_VERSION_0100		= "0100";  // MajorMajorMinorMaintenance
	public static final String				CLIENT_CURRENT_TEXT		= CLIENT_PREFIX + CLIENT_JAVA + CLIENT_VERSION_0100;
	public static final byte[]				CLIENT_CURRENT_BYTES	= CLIENT_CURRENT_TEXT.getBytes(BasicSerial.CHARSET);

	public static final int					HEADER_SIZE				= 32;
	private static final AtomicInteger		INSTANCE_COUNTER		= new AtomicInteger(0);

	protected byte[]							m_protocolName			= CLIENT_CURRENT_BYTES;
	protected long								m_clientContext;
	protected MessageType						m_messageType;
	protected int								m_contentSize;
	protected int								m_future				= INSTANCE_COUNTER.incrementAndGet();
	protected int								m_checksum;				// hashCode formula, protocolName to future

	private SocketCounterFamily					m_counterFamily;
	private final ObjectPool<ByteArrayWrapper>	m_byteArrayObjectPool;
	private AbstractTransportLane				m_transportLane			= null;

	private byte[]							m_scratchPad			= new byte[128];

	protected AbstractMessage(ObjectPool<ByteArrayWrapper> byteArrayWrapperObjectPool)
	{
		m_byteArrayObjectPool = byteArrayWrapperObjectPool;
/*
		m_byteArrayObjectPool = new ObjectPool<>(
				new StringGroup(getClass().getSimpleName() + "." + ByteArrayWrapper.class.getSimpleName()),
				ByteArrayWrapper.class);
*/
	}

	public SocketCounterFamily getCounterFamily()
	{
		return m_counterFamily;
	}

	public void setCounterFamily(SocketCounterFamily counterFamily)
	{
		m_counterFamily = counterFamily;
	}

	public AbstractTransportLane getTransportLane()
	{
		return m_transportLane;
	}

	public void setTransportLane(AbstractTransportLane transportLane)
	{
		m_transportLane = transportLane;
	}

	public void setProtocolName(AbstractMessage message)
	{
		System.arraycopy(message.m_protocolName, 0, m_protocolName, 0, m_protocolName.length);
	}

	public String getProtocolName()
	{
		return new String(m_protocolName, BasicSerial.CHARSET);
	}

	public long getClientContext()
	{
		return m_clientContext;
	}

	public void setMessageType(MessageType messageType)
	{
		m_messageType = messageType;
	}

	public MessageType getMessageType()
	{
		return m_messageType;
	}

	public int getContentSize()
	{
		return m_contentSize;
	}

	public int getFuture()
	{
		return m_future;
	}

	public int getChecksum()
	{
		return m_checksum;
	}

	abstract public ManagedByteBuffer getContentManagedByteBuffer();
	abstract public ByteBuffer getContentByteBuffer();

	public static int getBooleanSize()
	{
		return Byte.SIZE / 8;
	}
	public static int getShortSize()
	{
		return Short.SIZE / 8;
	}
	public static int getIntSize()
	{
		return Integer.SIZE / 8;
	}
	public static int getLongSize()
	{
		return Long.SIZE / 8;
	}
	public static int getStringSize(String text)
	{
		if (null == text)
		{
			return getShortSize();
		}
		else
		{
			return text.getBytes(BasicSerial.CHARSET).length + getShortSize();
		}
	}
	public static int getStringListSize(String[] textList)
	{
		int		size		= Integer.SIZE / 8;
		for (int i = 0; i < textList.length; i++)
		{
			size += getStringSize(textList[i]);
		}
		return size;
	}
	public void putStringList(String[] textList)
	{
		getContentByteBuffer().putInt(textList.length);
		for (int i = 0; i < textList.length; i++)
		{
			putString(textList[i]);
		}
	}
	public String[] getStringList()
	{
		int			size		= getContentByteBuffer().getInt();
		String[]	textList	= new String[size];
		for (int i = 0; i < textList.length; i++)
		{
			textList[i] = getString();
		}
		return textList;
	}
	public void putBoolean(boolean is)
	{
		getContentByteBuffer().put((byte) (is ? 1 : 0));
	}
	public boolean getBoolean()
	{
		byte		b		= getContentByteBuffer().get();
		return 1 == b;
	}
	public void putString(String text)
	{
		if (null == text)
		{
			getContentByteBuffer().putShort((short) 0);		// Limits the size of a string to 64K chars.
		}
		else
		{
			byte[]		bytes		= text.getBytes(BasicSerial.CHARSET);
			getContentByteBuffer().putShort((short) bytes.length);		// Limits the size of a string to 64K chars.
			getContentByteBuffer().put(bytes);
		}
	}
	public String getString()
	{
		short		length		= getContentByteBuffer().getShort();
		String		text;

		if (0 == length)
		{
			text = null;
		}
		else
		{
			if (length <= m_scratchPad.length)
			{
				getContentByteBuffer().get(m_scratchPad, 0, length);
				text = new String(m_scratchPad, 0, length, BasicSerial.CHARSET);
			}
			else
			{
				byte[]		bytes		= new byte[length];
				getContentByteBuffer().get(bytes, 0, length);
				text = new String(bytes, 0, length, BasicSerial.CHARSET);
			}
		}

		return text;
	}

	public static int getByteArraySize(byte[] bytes)
	{
		return getByteArraySize(bytes, 0, bytes.length);
	}
	public static int getByteArraySize(ByteArrayWrapper wrapper)
	{
		return getByteArraySize(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static int getByteArraySize(byte[] bytes, int indexMin, int length)
	{
		return getIntSize() + (null == bytes ? 0 : length);
	}
	public void putByteArray(ByteArrayWrapper wrapper)
	{
		putByteArray(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public void putByteArray(byte[] bytes)
	{
		putByteArray(bytes, 0, null == bytes ? 0 : bytes.length);
	}
	public void putByteArray(byte[] bytes, int indexMin, int length)
	{
		if (null == bytes)
		{
			getContentByteBuffer().putInt(0);
		}
		else
		{
			getContentByteBuffer().putInt(length);
			getContentByteBuffer().put(bytes, indexMin, length);
		}
	}
	public ByteArrayWrapper getByteArray()
	{
		int			length			= getContentByteBuffer().getInt();
		int			indexMin		= getContentByteBuffer().position();
		getContentByteBuffer().position(indexMin + length);

		ByteArrayWrapper		wrapper		= m_byteArrayObjectPool.get();
		wrapper.set(getContentByteBuffer(), indexMin, length);

		return wrapper;
	}

	public static int getByteArrayListSize(byte[][] bytesList)
	{
		int		size		= getIntSize();
		for (int i = 0; i < bytesList.length; i++)
		{
			size += getByteArraySize(bytesList[i]);
		}
		return size;
	}
	public void putByteArrayList(byte[][] bytesList)
	{
		getContentByteBuffer().putInt(bytesList.length);
		for (int i = 0; i < bytesList.length; i++)
		{
			byte[]		bytes		= bytesList[i];
			putByteArray(bytes, 0, bytes.length);
		}
	}
	public ByteArrayWrapper[] getByteArrayList()
	{
		int					size			= getContentByteBuffer().getInt();
		ByteArrayWrapper[]	wrapperList		= new ByteArrayWrapper[size];
		for (int i = 0; i < size; i++)
		{
			wrapperList[i] = getByteArray();
		}
		return wrapperList;
	}

	public List<ComputerAddress> getAddressList()
	{
		int								count			= getContentByteBuffer().getInt();
		ArrayList<ComputerAddress>		addressList		= new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			String		text		= getString();
			addressList.add(ComputerAddress.fromString(text));
		}
		return addressList;
	}

	@Override
	public String toString()
	{
		return String.format("ProtocolVersion=%s; Context=%,d; Type=%s; Size=%,d; Future=%,d; Checksum=%,d",
				getProtocolName(), m_clientContext, m_messageType, m_contentSize, m_future, m_checksum);
	}
}
