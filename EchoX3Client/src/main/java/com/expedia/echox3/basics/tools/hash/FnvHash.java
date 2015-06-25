/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.hash;

import java.util.Arrays;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;

/**
 * FNV Hash, as per http://www.isthe.com/chongo/tech/comp/fnv/#FNV-1a
 *
 * http://wiki.answers.com/Q/What_are_the_chances_of_being_hit_by_a_meteorite
 * 		What are the chances of being hit by a meteorite?
 * 			1 in 7,000,000,000 and you would not survive it (lifetime)
 * 			In any given second (at 2,365,200,000 seconds/75 years): 16.6 x 10^19
 *
 * 		Use static method for complete objects (e.g. single byte[]).
 * 		Use pooled object and public method(s) for composed objects.
 */
public class FnvHash extends AbstractPooledObject implements IHashProvider
{
//	private static final long 		OFFSET_32X		= 2166136261L;
	private static final int 		OFFSET_32		= 0x7FFFFFFF + 18652614;
	private static final int		PRIME_32		= 16777619;

	// Requires a manual subtraction
//	 2166136261		  Target
//	-2147483647		- 0x7FFF FFFF
//	   18652614		Number to add from 0x7FFF FFFF to obtain the offset

	// Requires a manual subtraction
//	 1469598103 9346656037	Target
//	- 922337203 6854775807L	0x7FFF...
//	  547260900 2491880230	Number to add to 0xFFF... to obtain the actual 64 bits offset

	private static final long		OFFSET_64		= 0x7FFFFFFFFFFFFFFFL + 5472609002491880230L;
	private static final long 		PRIME_64		= 1099511628211L;

	private static final ObjectPool<FnvHash>	OBJECT_POOL		= new ObjectPool<>(
															new StringGroup("Hash.FnvHash"), FnvHash::new);

	private int						m_hash32		= OFFSET_32;
	private long					m_hash64		= OFFSET_64;

	public FnvHash()
	{
		m_hash32	= OFFSET_32;
		m_hash64	= OFFSET_64;
	}

	public static FnvHash get()
	{
		return OBJECT_POOL.get();
	}

	@Override
	public void release()
	{
		m_hash32	= OFFSET_32;
		m_hash64	= OFFSET_64;

		super.release();
	}

	public static int hash32(byte[] byteList)
	{
		return hash32(byteList, 0, byteList.length);
	}
	public static int hash32(ByteArrayWrapper wrapper)
	{
		return hash32(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static int hash32(byte[] byteList, int indexMin, int length)
	{
		// This function is much faster when doing the math using long (on 64 bits machines),
		// even if returning a int. The returned value is identical.
		long		hash		= OFFSET_32;

		int			indexMax	= indexMin + length;
		for (int i = indexMin; i < indexMax; i++)
		{
			byte	b	= byteList[i];
			hash = hash ^ b;
			hash = hash * PRIME_32;
		}

		return (int) hash;
	}

	public static long hash64(byte[] byteList)
	{
		return hash64(byteList, 0, byteList.length);
	}
	public static long hash64(ByteArrayWrapper wrapper)
	{
		return hash64(wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static long hash64(byte[] byteList, int indexMin, int length)
	{
		long		hash		= OFFSET_64;

		int			indexMax	= indexMin + length;
		for (int i = indexMin; i < indexMax; i++)
		{
			byte	b	= byteList[i];
			hash = hash ^ b;
			hash = hash * PRIME_64;
		}

		return hash;
	}

	public void add32(byte[] byteList, int indexMin, int length)
	{
		int			indexMax	= indexMin + length;
		for (int i = indexMin; i < indexMax; i++)
		{
			byte	b	= byteList[i];
			m_hash32 = m_hash32 ^ b;
			m_hash32 = m_hash32 * PRIME_32;
		}
	}
	public void add32(int n)
	{
		m_hash32 = m_hash32 ^ n;
		m_hash32 = m_hash32 * PRIME_32;
	}
	public void add32(long n)
	{
		int			n1		= (int) n;
		int			n2		= (int) (n >> 32);

		m_hash32 = m_hash32 ^ n1;
		m_hash32 = m_hash32 * PRIME_32;

		m_hash32 = m_hash32 ^ n2;
		m_hash32 = m_hash32 * PRIME_32;
	}
	public void add32(Object o)
	{
		m_hash32 = m_hash32 ^ o.hashCode();
		m_hash32 = m_hash32 * PRIME_32;
	}
	public int getHashCode32()
	{
		return m_hash32;
	}

	public void add64(byte[] byteList, int indexMin, int length)
	{
		int			indexMax	= indexMin + length;
		for (int i = indexMin; i < indexMax; i++)
		{
			byte	b	= byteList[i];
			m_hash64 = m_hash64 ^ b;
			m_hash64 = m_hash64 * PRIME_64;
		}
	}
	public void add64(int n)
	{
		m_hash64 = m_hash64 ^ n;
		m_hash64 = m_hash64 * PRIME_64;
	}
	public void add64(long n)
	{
		m_hash64 = m_hash64 ^ n;
		m_hash64 = m_hash64 * PRIME_64;
	}
	public void add64(Object o)
	{
		m_hash64 = m_hash64 ^ o.hashCode();
		m_hash64 = m_hash64 * PRIME_64;
	}
	public long getHashCode64()
	{
		return m_hash64;
	}

	/**
	 *
	 * @param args	Standard main arguments, ignored
	 */
	@SuppressWarnings("PMD.SystemPrintln")
	public static void main(String[] args)
	{
		String hello		= "Hello world.";
		byte[]		helloBytes	= hello.getBytes();
		int			hash		= Arrays.hashCode(helloBytes);
		int			hash32		= hash32(helloBytes);
		long		hash64		= hash64(helloBytes);

		System.out.println(String.format("The hash of '%s' is %d = 0x%x", hello, hash, hash));
		System.out.println(String.format("The hash32 of '%s' is %d = 0x%x", hello, hash32, hash32));
		System.out.println(String.format("The hash64 of '%s' is %d == 0x%x", hello, hash64, hash64));
	}
}
