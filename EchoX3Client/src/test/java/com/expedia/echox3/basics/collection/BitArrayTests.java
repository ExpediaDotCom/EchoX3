/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.map.SortedArray;
import com.expedia.echox3.basics.collection.simple.BooleanBitArray;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class BitArrayTests extends AbstractTestTools
{
	@Test
	public void testSize()
	{
		assertEquals(1, BooleanBitArray.calculateLongCount(  1));
		assertEquals(1, BooleanBitArray.calculateLongCount(  5));
		assertEquals(1, BooleanBitArray.calculateLongCount( 32));
		assertEquals(1, BooleanBitArray.calculateLongCount( 64));
		assertEquals(2, BooleanBitArray.calculateLongCount( 65));
		assertEquals(2, BooleanBitArray.calculateLongCount(128));
		assertEquals(3, BooleanBitArray.calculateLongCount(129));
	}

	@Test
	public void testBasic()
	{
		logTestName();

		int					count		= 250;
		BooleanBitArray		bitArray		= new BooleanBitArray(count);

		bitArray.setAll();
		assertEquals(count, bitArray.getSetCount());
		validateBit(bitArray);

		bitArray.clear();
		assertEquals(0, bitArray.getSetCount());
		validateBit(bitArray);
	}

	@Test
	public void testAllSize()
	{
		logTestName();

		int		iMax		= Long.SIZE * 3;
		int		iValidate	= 10;

		for (int i = 0; i < iMax; i++)
		{
			BooleanBitArray		bitArray		= new BooleanBitArray(i);
			validateBit(bitArray);

			if (bitArray.getBitCount() > iValidate)
			{
				bitArray.set(iValidate);
				bitArray.resize(iMax);
				assertTrue(bitArray.get(iValidate));
				assertEquals(1, bitArray.getSetCount());
			}
			else
			{
				bitArray.resize(iMax);
			}

			validateBit(bitArray);
		}
	}

	@Test
	public void testPerformance()
	{
		logTestName();

		for (int i = 0; i < 10; i++)
		{
			measurePerformance(1000, 1000);
		}
	}
	private void measurePerformance(long bitCount, int loops)
	{
		BooleanBitArray		bitArray		= new BooleanBitArray(bitCount);

		long		t1			= System.nanoTime();
		for (int i = 0; i < loops; i++)
		{
			validateBit(bitArray);
		}
		long		t2			= System.nanoTime();
		long		durationNS	= t2 - t1;
		reportPerformance(bitArray.toString(), durationNS, loops * bitCount * 7, true);
	}


	private void validateBit(BooleanBitArray bitArray)
	{
		long		count		= bitArray.getBitCount();

		for (int i = 0; i < count; i++)
		{
			boolean		statusSav		= bitArray.get(i);

			assertEquals(statusSav, bitArray.set(i));
			assertTrue(bitArray.get(i));
			assertTrue(bitArray.clear(i));
			assertFalse(bitArray.get(i));

			assertFalse(bitArray.set(i, statusSav));
			assertEquals(statusSav, bitArray.get(i));
		}
	}

	@Test
	public void testException() throws NoSuchMethodException
	{
		SortedArray			array		= new SortedArray();
		Iterator<Integer>	iterator	= array.iterator();

		ensureException(iterator,
				SortedArray.IteratorByIndex.class.getMethod("remove"),
				UnsupportedOperationException.class);
	}
}
