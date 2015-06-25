/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.nio.ByteBuffer;

import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;

public class ByteArrayWrapper extends AbstractPooledObject
{
	private byte[]		m_byteArray		= null;		// Not owned by this object, owned by the caller
	private int			m_indexMin;					// Inclusive
	private int			m_length;					// Exclusive

	public ByteArrayWrapper()
	{
		// Parameter-less constructor, as required by AbstractObjectPool
	}
	public void set(byte[] byteArray, int indexMin, int length)
	{
		m_byteArray = byteArray;
		m_indexMin = indexMin;
		m_length = length;
	}
	public void set(ByteBuffer byteBuffer, int indexMin, int length)
	{
		m_byteArray = byteBuffer.array();
		m_indexMin = indexMin;
		m_length = length;
	}

	@Override
	public void release()
	{
		m_byteArray = null;
		super.release();
	}

	public byte[] getByteArray()
	{
		return m_byteArray;
	}

	public int getIndexMin()
	{
		return m_indexMin;
	}

	public int getLength()
	{
		return m_length;
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
		return String.format("From %,d; cb %,d of %,d bytes", m_indexMin, m_length,
				null == m_byteArray ? 0 : m_byteArray.length);
	}
}
