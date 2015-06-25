/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.io.ByteArrayInputStream;

public class ReusableByteArrayInputStream extends ByteArrayInputStream
{
	/**
	 * Creates a <code>ByteArrayInputStream</code>
	 * so that it  uses <code>buf</code> as its
	 * buffer array.
	 * The buffer array is not copied.
	 * The initial value of <code>pos</code>
	 * is <code>0</code> and the initial value
	 * of  <code>count</code> is the length of
	 * <code>buf</code>.
	 *
	 * @param buf the input buffer.
	 */
	public ReusableByteArrayInputStream(byte[] buf)
	{
		super(buf);
	}

	/**
	 * Creates <code>ByteArrayInputStream</code>
	 * that uses <code>buf</code> as its
	 * buffer array. The initial value of <code>pos</code>
	 * is <code>offset</code> and the initial value
	 * of <code>count</code> is the minimum of <code>offset+length</code>
	 * and <code>buf.length</code>.
	 * The buffer array is not copied. The buffer's mark is
	 * set to the specified offset.
	 *
	 * @param buf    the input buffer.
	 * @param offset the offset in the buffer of the first byte to read.
	 * @param length the maximum number of bytes to read from the buffer.
	 */
	public ReusableByteArrayInputStream(byte[] buf, int offset, int length)
	{
		super(buf, offset, length);
	}

	public void setBuffer(byte[] buf)
	{
		setBuffer(buf, 0, buf.length);
	}

	public void setBuffer(byte[] buf, int offset, int length)
	{
		this.buf	= buf;
		this.pos	= offset;
		this.count	= Math.min(offset + length, buf.length);
		this.mark = offset;
	}
}
