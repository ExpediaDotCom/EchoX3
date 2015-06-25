/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.simple.IAccumulator;
import com.expedia.echox3.basics.collection.simple.RingBufferAccumulator;
import com.expedia.echox3.basics.collection.simple.SimpleAccumulator;

public class AccumulatorTests extends AbstractTestTools
{
	private static final double			EQUAL_DELTA		= 1.0e-4;

	@Test
	public void testAccumulator()
	{
		logTestName();

		int[]			valueList		= { 2, 4, 4, 4, 5, 5, 7, 9 };
		IAccumulator	accumulator1	= new SimpleAccumulator();

		for (int value : valueList)
		{
			accumulator1.record(value);
		}
		validateAccumulator(accumulator1, 2, 5, 9, 2);

		IAccumulator	accumulator2	= new SimpleAccumulator();
		accumulator2.record(accumulator1);
		validateAccumulator(accumulator2, 2, 5, 9, 2);

		IAccumulator	accumulator3	= accumulator1.cloneAndReset();
		validateAccumulator(accumulator3, 2, 5, 9, 2);
	}
	private void validateAccumulator(IAccumulator accumulator,
			double min, double avg, double max, double std)
	{
		assertEquals("Min", min, accumulator.getMin(), EQUAL_DELTA);
		assertEquals("Avg", avg, accumulator.getAvg(), EQUAL_DELTA);
		assertEquals("Max", max, accumulator.getMax(), EQUAL_DELTA);
		if (!(accumulator instanceof RingBufferAccumulator))
		{
			assertEquals("Std", std, accumulator.getStd(), EQUAL_DELTA);
		}
		else
		{
			Exception		exception		= null;
			try
			{
				accumulator.getStd();
			}
			catch (Exception e)
			{
				exception = e;
			}
			assertNotNull(exception);
		}
	}

	@Test
	public void testRingBuffer()
	{
		IAccumulator		ring1			= new RingBufferAccumulator(4);
		ring1.record(1);
		validateAccumulator(ring1, 1, 1, 1, 0);
		ring1.record(2);
		ring1.record(3);
		validateAccumulator(ring1, 1, 2, 3, 0);
		ring1.record(4);
		ring1.record(5);
		validateAccumulator(ring1, 2, 3.5, 5, 0);

		IAccumulator		ring2			= ring1.cloneAndReset();
		ring1.record(6);
		ring1.record(7);
		ring1.record(8);
		validateAccumulator(ring1, 6, 7.0, 8, 0);
		validateAccumulator(ring2, 2, 3.5, 5, 0);

		ring1.cloneAndReset(ring2);
		validateAccumulator(ring2, 6, 7.0, 8, 0);
		validateAccumulator(ring1, Double.POSITIVE_INFINITY, 0.0, Double.NEGATIVE_INFINITY, 0);
	}

}
