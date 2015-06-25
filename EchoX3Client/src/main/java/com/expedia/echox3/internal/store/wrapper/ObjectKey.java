/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.wrapper;

import java.io.Serializable;
import java.util.Arrays;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;


/**
 * Responsible for Hash -> Direction at all routing levels (always use Math.abs(int))
 * 		Director			Select the bucket		= High 32 bits of m_hash64
 * 		LocalObjectBucket		Select the bin			= (High + Low) of m_hash64
 * 		LocalObjectBin		simple Java hashCode	= Low 32 bits of m_hash64
 */
public class ObjectKey extends ObjectPool.AbstractPooledObject
{
	private ByteArrayWrapper	m_wrappedKey		= new ByteArrayWrapper();	// NOT pooled, owned by this object.
	private long				m_hash64;

	public ObjectKey()
	{
		m_hash64 = 0;
	}
	public ObjectKey(byte[] bytes)
	{
		setKeyBytes(bytes);
	}

	public void setWrappedKey(ByteArrayWrapper wrappedKey)
	{
		m_wrappedKey = wrappedKey;
		m_hash64 = Math.abs(HashUtil.hash64(m_wrappedKey));
	}
	public void setKeyBytes(byte[] bytes)
	{
		m_wrappedKey.set(bytes, 0, bytes.length);
		m_hash64 = Math.abs(HashUtil.hash64(m_wrappedKey));
	}
	public void setKeyBytes(byte[] bytes, long hash64)
	{
		m_wrappedKey.set(bytes, 0, bytes.length);
		m_hash64 = hash64;
	}

	public ByteArrayWrapper getWrappedKey()
	{
		return m_wrappedKey;
	}

	public int getKeySize()
	{
		return m_wrappedKey.getLength();
	}

	public long getHash64()
	{
		return m_hash64;
	}
	public int getKeyForBucket()
	{
		return Math.abs((int) m_hash64);
	}
	public int getKeyForBin()
	{
		return Math.abs(((int) m_hash64) + ((int) (m_hash64 >> 32)));
	}
	public int getKeyForMap()
	{
		return Math.abs((int) (m_hash64 >> 32));
	}

	@Override
	public int hashCode()
	{
		return getKeyForMap();
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ObjectKey))
		{
			return false;
		}

		ObjectKey key		= (ObjectKey) o;
		boolean			isEqual	=
				Arrays.equals(m_wrappedKey.getByteArray(), key.m_wrappedKey.getByteArray());

		return isEqual;
	}

	public Serializable getKey(String cacheName) throws BasicException
	{
		return BasicSerial.toObject(cacheName, m_wrappedKey);
	}

	@Override
	public String toString()
	{
		try
		{
			return String.format("Key(Bucket-%d, Bin-%d, Map-%d) = %s",
					getKeyForBucket(), getKeyForBin(), getKeyForMap(),
					BasicSerial.toObject(null, m_wrappedKey).toString());
		}
		catch (BasicException e)
		{
			return String.format("Key(Bucket-%d, Bin-%d, Map-%d) = byte[%,d]",
					getKeyForBucket(), getKeyForBin(), getKeyForMap(),
					m_wrappedKey.getLength());
		}
	}
}
