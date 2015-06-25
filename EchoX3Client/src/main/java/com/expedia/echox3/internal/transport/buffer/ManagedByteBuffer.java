/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.buffer;

import java.nio.ByteBuffer;

import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;

public class ManagedByteBuffer extends AbstractPooledObject
{
	private boolean							m_isCustom			= true;
	private ByteBuffer						m_byteBuffer		= null;
	private int								m_offset			= 0;

	public ManagedByteBuffer()
	{

	}

	/* package */ void setCustom(boolean isCustom)
	{
		m_isCustom = isCustom;
	}

	public void setByteBuffer(ByteBuffer byteBuffer)
	{
		m_byteBuffer = byteBuffer;
	}

	public ByteBuffer getByteBuffer()
	{
		return m_byteBuffer;
	}

	@Override
	public void release()
	{
		m_byteBuffer.clear();

		// If the buffer comes from CUSTOM, clear it
		if (m_isCustom)
		{
			m_byteBuffer = null;
		}

		super.release();
	}

	public int getOffset()
	{
		return m_offset;
	}

	public void setOffset(int offset)
	{
		m_offset = offset;
	}

	public void rewind()
	{
		m_byteBuffer.rewind();
		m_byteBuffer.position(m_offset);
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
		return String.format("%s(%,d; isCustom=%s)", ManagedByteBuffer.class.getSimpleName(),
				null == getByteBuffer() ? -1 : getByteBuffer().capacity(), m_isCustom);
	}
}
