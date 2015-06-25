/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.Arrays;

public class BooleanBitArray
{
	private long[]		m_bitArray;
	private long		m_bitCount;

	public BooleanBitArray(long bitCount)
	{
		int			longCount		= calculateLongCount(bitCount);
		m_bitArray = new long[longCount];
		m_bitCount = bitCount;
	}

	public void resize(long bitCount)
	{
		int			longCount		= calculateLongCount(bitCount);

		if (m_bitArray.length != longCount)
		{
			m_bitArray = Arrays.copyOf(m_bitArray, longCount);
		}
		m_bitCount = bitCount;
	}
	public static int calculateLongCount(long bitCount)
	{
		int			longCount		= (int) ((bitCount + Long.SIZE - 1) / Long.SIZE);
//		boolean		isExact			= 0 == (bitCount % Long.SIZE);
//		if (!isExact)
//		{
//			longCount++;
//		}
		return longCount;
	}

	public long getBitCount()
	{
		return m_bitCount;
	}

	public boolean set(long index, boolean value)
	{
		if (value)
		{
			return set(index);
		}
		else
		{
			return clear(index);
		}
	}
	public boolean set(long index)
	{
		int			longIndex		= (int) (index / Long.SIZE);
		long		bitIndex		= index - (longIndex * Long.SIZE);
		long 		bitMask			= 0x1L << bitIndex;

		boolean		previous		= 0 != (m_bitArray[longIndex] & bitMask);
		m_bitArray[longIndex] |= bitMask;

		return previous;
	}
	public void setAll()
	{
		Arrays.fill(m_bitArray, ~0);
	}
	public void clear()
	{
		Arrays.fill(m_bitArray, 0);
	}
	public boolean clear(long index)
	{
		int			longIndex		= Long.valueOf(index / Long.SIZE).intValue();
		long		bitIndex		= index - (longIndex * Long.SIZE);
		long 		bitMask			= 0x1L << bitIndex;

		boolean		previous		= 0 != (m_bitArray[longIndex] & bitMask);
		m_bitArray[longIndex] &= (~bitMask);

		return previous;
	}

	public void or(BooleanBitArray bitArray)
	{
		if (m_bitCount != bitArray.m_bitCount)
		{
			throw new IllegalArgumentException(
					String.format("The size of the bit arrays (%,d and %,d) must be equal.",
							m_bitCount, bitArray.m_bitCount));
		}

		for (int i = 0; i < m_bitArray.length; i++)		// NOPMD
		{
			m_bitArray[i] |= bitArray.m_bitArray[i];
		}
	}
	public void and(BooleanBitArray bitArray)
	{
		if (m_bitCount != bitArray.m_bitCount)
		{
			throw new IllegalArgumentException(
					String.format("The size of the bit arrays (%,d and %,d) must be equal.",
							m_bitCount, bitArray.m_bitCount));
		}

		for (int i = 0; i < m_bitArray.length; i++)		// NOPMD
		{
			m_bitArray[i] &= bitArray.m_bitArray[i];
		}
	}
	public void xor(BooleanBitArray bitArray)
	{
		if (m_bitCount != bitArray.m_bitCount)
		{
			throw new IllegalArgumentException(
					String.format("The size of the bit arrays (%,d and %,d) must be equal.",
							m_bitCount, bitArray.m_bitCount));
		}

		for (int i = 0; i < m_bitArray.length; i++)		// NOPMD
		{
			m_bitArray[i] ^= bitArray.m_bitArray[i];
		}
	}

	public boolean get(long index)
	{
		int			longIndex		= Long.valueOf(index / Long.SIZE).intValue();
		long		bitIndex		= index - (longIndex * Long.SIZE);
		long 		bitMask			= 0x1L << bitIndex;

		return 0 != (m_bitArray[longIndex] & bitMask);
	}

	public long getSetCount()
	{
		long	count		= 0;
		for (long i = 0; i < m_bitCount; i++)
		{
			if (get(i))
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Returns a hash code value for the object. This method is
	 * supported for the benefit of hash tables such as those provided by
	 * {@link java.util.HashMap}.
	 * <p>
	 * The general contract of {@code hashCode} is:
	 * <ul>
	 * <li>Whenever it is invoked on the same object more than once during
	 * an execution of a Java application, the {@code hashCode} method
	 * must consistently return the same integer, provided no information
	 * used in {@code equals} comparisons on the object is modified.
	 * This integer need not remain consistent from one execution of an
	 * application to another execution of the same application.
	 * <li>If two objects are equal according to the {@code equals(Object)}
	 * method, then calling the {@code hashCode} method on each of
	 * the two objects must produce the same integer result.
	 * <li>It is <em>not</em> required that if two objects are unequal
	 * according to the {@link Object#equals(Object)}
	 * method, then calling the {@code hashCode} method on each of the
	 * two objects must produce distinct integer results.  However, the
	 * programmer should be aware that producing distinct integer results
	 * for unequal objects may improve the performance of hash tables.
	 * </ul>
	 * <p>
	 * As much as is reasonably practical, the hashCode method defined by
	 * class {@code Object} does return distinct integers for distinct
	 * objects. (This is typically implemented by converting the internal
	 * address of the object into an integer, but this implementation
	 * technique is not required by the
	 * Java&trade; programming language.)
	 *
	 * @return a hash code value for this object.
	 * @see Object#equals(Object)
	 * @see System#identityHashCode
	 */
	@Override
	public int hashCode()
	{
		return Arrays.hashCode(m_bitArray);
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see java.util.HashMap
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof BooleanBitArray))
		{
			return false;
		}

		BooleanBitArray		bitArray		= (BooleanBitArray) obj;
		return Arrays.equals(m_bitArray, bitArray.m_bitArray);
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
		return String.format("%s(Set %,d; of %,d)",
				BooleanBitArray.class.getSimpleName(), getSetCount(), getBitCount());
	}
}
