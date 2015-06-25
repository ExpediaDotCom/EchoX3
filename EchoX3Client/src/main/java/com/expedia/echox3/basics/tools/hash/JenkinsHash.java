/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.hash;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;

/**
 * Algorithm reference: http://en.wikipedia.org/wiki/Jenkins_hash_function and references cited on that page.
 * Source copied by pcote from http://256.com/sources/jenkins%5Fhash%5Fjava/JenkinsHash.java
 *
 * Hash algorithm by Bob Jenkins, 1996.
 *
 * You may use this code any way you wish, private, educational, or commercial.  It's free.
 * See: http://burtleburtle.net/bob/hash/doobs.html
 *
 * Use for hash table lookup, or anything where one collision in 2^^32
 * is acceptable.  Do NOT use for cryptographic purposes.
 *
 * Java port by Gray Watson http://256.com/gray/
 */
// CHECKSTYLE:OFF		As this is copied code.
@SuppressWarnings("PMD")
public class JenkinsHash extends ObjectPool.AbstractPooledObject implements IHashProvider
{

	// max value to limit it to 4 bytes
	private static final long	MAX_VALUE			= 0xFFFFFFFFL;

	// From FnvHash, as good as anything else
	private static final long	INITIAL_VALUE		= 0x7FFFFFFFFFFFFFFFL + 5472609002491880230L;

	private static final ObjectPool<JenkinsHash>	OBJECT_POOL		= new ObjectPool<>(
									new StringGroup("ObjectPool.Hash.JenkinsHash"), JenkinsHash::new);
	// internal variables used in the various calculations
	long a;
	long b;
	long c;

	byte[]		m_scratchBytes		= new byte[8];

	public JenkinsHash()
	{
		// set up the internal state
		// the golden ratio; an arbitrary value
		a = 0x09e3779b9L;
		// the golden ratio; an arbitrary value
		b = 0x09e3779b9L;
		// the previous hash value
		c = INITIAL_VALUE;
	}

	public static JenkinsHash get()
	{
		return OBJECT_POOL.get();
	}
	public void release()
	{
		// set up the internal state
		// the golden ratio; an arbitrary value
		a = 0x09e3779b9L;
		// the golden ratio; an arbitrary value
		b = 0x09e3779b9L;
		// the previous hash value
		c = INITIAL_VALUE;

		super.release();
	}

	/**
	 * Convert a byte into a long value without making it negative.
	 */
	private long byteToLong(byte b)
	{
		long val = b & 0x7F;
		if ((b & 0x80) != 0)
		{
			val += 128;
		}
		return val;
	}

	/**
	 * Do addition and turn into 4 bytes.
	 */
	private long add(long val, long add)
	{
		return (val + add) & MAX_VALUE;
	}

	/**
	 * Do subtraction and turn into 4 bytes.
	 */
	private long subtract(long val, long subtract)
	{
		return (val - subtract) & MAX_VALUE;
	}

	/**
	 * Left shift val by shift bits and turn in 4 bytes.
	 */
	private long xor(long val, long xor)
	{
		return (val ^ xor) & MAX_VALUE;
	}

	/**
	 * Left shift val by shift bits.  Cut down to 4 bytes.
	 */
	private long leftShift(long val, int shift)
	{
		return (val << shift) & MAX_VALUE;
	}

	/**
	 * Convert 4 bytes from the buffer at offset into a long value.
	 */
	private long fourByteToLong(byte[] bytes, int offset)
	{
		return (byteToLong(bytes[offset + 0])
				+ (byteToLong(bytes[offset + 1]) << 8)
				+ (byteToLong(bytes[offset + 2]) << 16)
				+ (byteToLong(bytes[offset + 3]) << 24));
	}

	/**
	 * Mix up the values in the hash function.
	 */
	private void hashMix()
	{
		a = subtract(a, b); a = subtract(a, c); a = xor(a, c >> 13);
		b = subtract(b, c); b = subtract(b, a); b = xor(b, leftShift(a, 8));
		c = subtract(c, a); c = subtract(c, b); c = xor(c, (b >> 13));
		a = subtract(a, b); a = subtract(a, c); a = xor(a, (c >> 12));
		b = subtract(b, c); b = subtract(b, a); b = xor(b, leftShift(a, 16));
		c = subtract(c, a); c = subtract(c, b); c = xor(c, (b >> 5));
		a = subtract(a, b); a = subtract(a, c); a = xor(a, (c >> 3));
		b = subtract(b, c); b = subtract(b, a); b = xor(b, leftShift(a, 10));
		c = subtract(c, a); c = subtract(c, b); c = xor(c, (b >> 15));
	}

	/**
	 * Hash a variable-length key into a 32-bit value.  Every bit of the
	 * key affects every bit of the return value.  Every 1-bit and 2-bit
	 * delta achieves avalanche.  The best hash table sizes are powers of 2.
	 *
	 * @param buffer Byte array that we are hashing on.
	 */
	private void hash(byte[] buffer, int indexMin, int length)
	{
		int len, pos;

		// handle most of the key
		pos = 0;
		for (len = length; len >=12; len -= 12)
		{
			a = add(a, fourByteToLong(buffer, pos));
			b = add(b, fourByteToLong(buffer, pos + 4));
			c = add(c, fourByteToLong(buffer, pos + 8));
			hashMix();
			pos += 12;
		}

		c += buffer.length;

		// all the case statements fall through to the next on purpose
		switch(len)
		{
			case 11:
				c = add(c, leftShift(byteToLong(buffer[pos + 10]), 24));
			case 10:
				c = add(c, leftShift(byteToLong(buffer[pos + 9]), 16));
			case 9:
				c = add(c, leftShift(byteToLong(buffer[pos + 8]), 8));
				// the first byte of c is reserved for the length
			case 8:
				b = add(b, leftShift(byteToLong(buffer[pos + 7]), 24));
			case 7:
				b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16));
			case 6:
				b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8));
			case 5:
				b = add(b, byteToLong(buffer[pos + 4]));
			case 4:
				a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24));
			case 3:
				a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16));
			case 2:
				a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8));
			case 1:
				a = add(a, byteToLong(buffer[pos + 0]));
				// case 0: nothing left to add
			default:
				// Nothing to do, to make PMD happy.
		}
		hashMix();
	}

	/**
	 * Implements the One At A Time hash algorithm, as per reference
	 *
	 * @param byteList		List of bytes to hash
	 * @return				The resultant 32 bits hash
	 */
	public static int hashOne32(byte[] byteList)
	{
		int		hash		= 0;

		for(byte b : byteList)
		{
			hash += b;
			hash += (hash << 10);
			hash ^= (hash >> 6);
		}
		hash += (hash << 3);
		hash ^= (hash >> 11);
		hash += (hash << 15);

		return hash;
	}

	public void setInitialValue(long initialValue)
	{
		c = initialValue;
	}

	public static int hash32(byte[] buffer)
	{
		return hash32(buffer, 0, buffer.length);
	}
	public static int hash32(ByteArrayWrapper wrapper)
	{
		return hash32(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static int hash32(byte[] buffer, int indexMin, int length)
	{
		JenkinsHash			jenkinsHash			= get();
		jenkinsHash.add32(buffer, indexMin, length);
		int					hashCode			= jenkinsHash.getHashCode32();
		jenkinsHash.release();

		return hashCode;
	}
	public int hash32_2()
	{
		return (int) b;
	}
	public int hash32_3()
	{
		return (int) a;
	}

	public static long hash64(byte[] buffer)
	{
		return hash64(buffer, 0, buffer.length);
	}
	public static long hash64(ByteArrayWrapper wrapper)
	{
		return hash64(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static long hash64(byte[] buffer, int indexMin, int length)
	{
		JenkinsHash			jenkinsHash			= get();
		jenkinsHash.add64(buffer, indexMin, length);
		long					hashCode			= jenkinsHash.getHashCode64();
		jenkinsHash.release();

		return hashCode;
	}



	@Override
	public void add32(byte[] byteList, int indexMin, int length)
	{
		add64(byteList, indexMin, length);
	}


	@Override
	public void add32(int n)
	{
		add64(n);
	}

	@Override
	public void add32(long n)
	{
		add64(n);
	}

	@Override
	public int getHashCode32()
	{
		return (int) c;
	}

	@Override
	public void add64(byte[] byteList, int indexMin, int length)
	{
		hash(byteList, indexMin, length);
	}

	@Override
	public void add64(int n)
	{
		hash(toScratchBytes(n, 4), 0, 4);
	}

	@Override
	public void add64(long n)
	{
		hash(toScratchBytes(n, 8), 0, 8);
	}
	private byte[] toScratchBytes(long n, int cBytes)
	{
		for (int i = 0; i < cBytes; i++)
		{
			m_scratchBytes[i] = (byte) (n & 0xff);
			n = n >> 8;
		}

		return m_scratchBytes;
	}

	@Override
	public long getHashCode64()
	{
		return (b << 32) + c;
	}
}
