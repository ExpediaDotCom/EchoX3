/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.hash;

import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;

/**
 * public face of various hash algorithm and other hash related tools.
 */
public abstract class HashUtil
{
	public enum Algorithm
	{
		Java32,
		Java64,
		Fnv32,
		Fnv64,
		JKOne32,
		Jenkins32,
		Jenkins64
	}

	public static final Algorithm[]		VALUES					= Algorithm.values();
	public static final int 			ALGORITHM_MAX			= VALUES.length;

	private static final int 			BITMASK_POSITIVE_32		= ~0x80000000;
	private static final long			BITMASK_POSITIVE_64		= ~0x8000000000000000L;

	public static IHashProvider getHashProvider()
	{
		return FnvHash.get();
	}
	public static int hash32(byte[] bytes)
	{
		return FnvHash.hash32(bytes);
	}
	public static long hash64(byte[] bytes)
	{
		return FnvHash.hash64(bytes);
	}
	public static long hash64(ByteArrayWrapper wrapper)
	{
		return FnvHash.hash64(wrapper);
	}
	public static long hash64(byte[] bytes, int offset, int length)
	{
		return FnvHash.hash64(bytes, offset, length);
	}

	/**
	 * This is a copy of the hash function in JDK 1.6 to ensure it does not change.
	 * MultiCache will lose data if the function changes.
	 *
	 * @param byteList		The bytes to hash
	 * @return				A 32 bits hash
	 */
	public static int hashJava32(byte[] byteList)
	{
		if (byteList == null)
		{
			return 0;
		}

		long		hash		= 1;
		for (int i = 0; i < byteList.length; i++)
		{
			byte element = byteList[i];
			hash	= 31 * hash + element;
		}

		return (int) hash & BITMASK_POSITIVE_32;
	}
	public static long hashJava64(byte[] byteList)
	{
		if (byteList == null)
		{
			return 0;
		}

		long		hash		= 1;
		for (byte element : byteList)
		{
			hash	= 31 * hash + element;
		}

		return hash & BITMASK_POSITIVE_64;
	}

	public static int hashFnv32(byte[] byteList)
	{
		return FnvHash.hash32(byteList) & BITMASK_POSITIVE_32;
	}
	public static long hashFnv64(byte[] byteList)
	{
		return FnvHash.hash64(byteList) & BITMASK_POSITIVE_64;
	}

	/**
	 * Splits the 64 bit hash into two 32(31) bits int
	 *
	 * @param byteList		The bytes to hash
	 * @return				The two 31 bits hash (always positive)
	 */
	public static int[] hashFnv2(byte[] byteList)
	{
		long		hash		= FnvHash.hash64(byteList);
		int[]		answer		= new int[2];

		answer[0] = ((int) hash)			& BITMASK_POSITIVE_32;
		answer[1] = ((int) (hash >> 32))	& BITMASK_POSITIVE_32;

		return answer;
	}

	public static int hashJenkinsOne32(byte[] byteList)
	{
		return JenkinsHash.hashOne32(byteList) & BITMASK_POSITIVE_32;
	}
	public static int hashJenkins32(byte[] byteList)
	{
		return JenkinsHash.hash32(byteList) & BITMASK_POSITIVE_32;
	}
	public static long hashJenkins64(byte[] byteList)
	{
		return JenkinsHash.hash64(byteList) & BITMASK_POSITIVE_64;
	}

	/**
	 * Composes the 3 Jenkins hash variables into an array of int
	 *
	 * @param byteList		The bytes to hash
	 * @return				a, b and c of the Jenkins algorithm.
	 */
	public static int[] hashJenkins3(byte[] byteList)
	{
		int[]			answer		= new int[3];
		JenkinsHash		jenkins		= JenkinsHash.get();

		answer[0] = JenkinsHash.hash32(byteList) & BITMASK_POSITIVE_32;
		answer[1] = (int) jenkins.b & BITMASK_POSITIVE_32;
		answer[2] = (int) jenkins.a & BITMASK_POSITIVE_32;

		jenkins.release();
		return answer;
	}

	public static long getHash(Algorithm algo, byte[] bytes)
	{
		switch (algo)
		{
		default:
		case Java32:
			return hashJava32(bytes);
		case Java64:
			return hashJava64(bytes);

		case Fnv32:
			return hashFnv32(bytes);
		case Fnv64:
			return hashFnv64(bytes);

		case JKOne32:
			return hashJenkinsOne32(bytes);
		case Jenkins32:
			return hashJenkins32(bytes);
		case Jenkins64:
			return hashJenkins64(bytes);
		}
	}

	public static void main(String[] args)
	{
		String hello			= "Hello world.";
		byte[]		helloBytes		= hello.getBytes();

		for (Algorithm algorithm : Algorithm.values())
		{
			long		hash		= getHash(algorithm, helloBytes);
			log(String.format("The %1$-15s of '%2$s' is %3$,25d == 0x%3$16X", algorithm.toString(), hello, hash));
		}

		int[]		hash2		= hashFnv2(helloBytes);
		log(String.format("The %1$-15s of '%2$s' is %3$,25d %4$,25d == 0x%3$16X 0x%4$16X",
				"FVN64/2", hello, hash2[0], hash2[1]));

		int[]		hash3		= hashJenkins3(helloBytes);
		log(String.format("The %1$-15s of '%2$s' is %3$,25d %4$,25d %5$,25d == 0x%3$16X 0x%4$16X 0x%5$16X",
				"J32*3", hello, hash3[0], hash3[1], hash3[2]));

//		validate();

		int[]		keySizeList		= { 100, 100, 10, 10, 1000, 1000, 10 * 1000 };
		for (int keySize : keySizeList)
		{
			measure(keySize);
		}
	}

	@SuppressWarnings("PMD.SystemPrintln")
	private static void log(String text)
	{
		System.out.println(text);
	}

/*
	private static void validate()
	{
		int		count		= 10 * 1000;
		int		size		= 25;
		for (int i = 0; i < count; i++)
		{
			String		text		= generateRandomString(size);
			byte[]		byteList	= text.getBytes();
			int			hash		= hashJava32(byteList);
			long		hash64		= hashJava64(byteList);
			int			hash32		= (int) hash64;
			hash32 = hash32 & BITMASK_POSITIVE_32;
			if (hash != hash32)
			{
				log(String.format("Not same hash for '%s': %,14d != %,14d", text, hash, hash32));
			}
		}
	}
*/
	public static void measure(int keySize)
	{
		int				keyCount	= 100 * 1000;
		byte[][]		keyList		= new byte[keyCount][];
		for (int i = 0; i < keyList.length; i++)
		{
			keyList[i] = BasicTools.generateRandomString(keySize).getBytes();
		}

		{
			StringBuilder sb			= new StringBuilder(String.format("%23s", ""));
			for (Algorithm algorithm : Algorithm.values())
			{
				sb.append(String.format("%9s ", algorithm.toString()));
			}
			log(sb.toString());
		}

		for (int iPass = 0; iPass < 5; iPass++)
		{
			long[]			durationList		= new long[ALGORITHM_MAX];
			for (Algorithm algorithm : Algorithm.values())
			{
				durationList[algorithm.ordinal()] = measureAlgorithm(algorithm, keyList);
			}

			StringBuilder sb		= new StringBuilder(1024);
			sb.append(String.format("Pass %2d (Key = %,6d) ", iPass, keySize));
			for (int algo = 0; algo < ALGORITHM_MAX; algo++)
			{
				sb.append(String.format("%,9.1f ", durationList[algo] * 1.0 / keyCount));
			}
			sb.append(" ns/key");
			log(sb.toString());
			BasicTools.sleepMS(25);
		}
	}

	private static long measureAlgorithm(Algorithm algorithm, byte[][] keyList)
	{
		switch (algorithm)
		{
		default:
		case Java32:
			return measureJava32(keyList);

		case Java64:
			return measureJava64(keyList);

		case Fnv32:
			return measureFnv32(keyList);
		case Fnv64:
			return measureFnv64(keyList);

		case JKOne32:
			return measureJenkinsOne32(keyList);
		case Jenkins32:
			return measureJenkins32(keyList);
		case Jenkins64:
			return measureJenkins64(keyList);
		}
	}

	private static long measureJava32(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashJava32(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureJava64(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashJava64(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureFnv32(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashFnv32(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureFnv64(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashFnv64(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureJenkinsOne32(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashJenkinsOne32(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureJenkins32(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashJenkins32(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
	private static long measureJenkins64(byte[][] keyList)
	{
		long		t1		= System.nanoTime();
		for (byte[] key : keyList)
		{
			hashJenkins64(key);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}
}

