/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.wrapper;

import com.expedia.echox3.basics.collection.simple.BooleanBitArray;

public class IndexList
{
	private int					m_countCurrent;
	private int					m_countPrevious		= 0;
	private BooleanBitArray		m_bitArray;

	public IndexList(int count)
	{
		m_bitArray = new BooleanBitArray(count);
		m_countCurrent = count;
	}

	public void add(int value)
	{
		m_bitArray.set(value);
	}
	public void remove(int value)
	{
		m_bitArray.clear(value);
	}
	public void set(Iterable<Integer> iterable)
	{
		m_bitArray.clear();
		for (int i : iterable)
		{
			m_bitArray.set(i);
		}
	}
	public void resize(int size)
	{
		m_bitArray.resize(size);
		m_countCurrent = size;
	}
	public void set(BooleanBitArray bitArray)
	{
		resize((int) bitArray.getBitCount());
		for (int i = 0; i < bitArray.getBitCount(); i++)
		{
			m_bitArray.set(i, bitArray.get(i));
		}
	}

	public boolean get(int value)
	{
		return m_bitArray.get(value);
	}

	public int getCountActive()
	{
		return (int) m_bitArray.getSetCount();
	}

	public int getCountCurrent()
	{
		return m_countCurrent;
	}

	public int getCountPrevious()
	{
		return m_countPrevious;
	}

	public String getActiveText()
	{
		int					count			= 0;
		int					bucketCount		= (int) m_bitArray.getSetCount();
		StringBuilder		sb				= new StringBuilder(bucketCount * 4);
		String prefix	= "";
		for (int i = 0; i < bucketCount; i++)
		{
			if (m_bitArray.get(i))
			{
				count++;
				sb.append(prefix);
				sb.append(i);
				prefix = ", ";
			}
		}
		return String.format("%,d of %,d buckets: Buckets %s", count, bucketCount, sb.toString());
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
		return String.format("%s(%,d of %,d set)",
				IndexList.class.getSimpleName(), m_bitArray.getSetCount(), m_bitArray.getBitCount());
	}
}
