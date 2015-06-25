/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.histogram.LinearHistogram;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;

public class HistogramTests extends AbstractTestTools
{
	@Test
	public void testSimpleLinearHistogram()
	{
		logTestName();

		LinearHistogram		histogram		= new LinearHistogram(0, 10, 0.1);

		assertEquals(100, histogram.getBinCount());
		ensureAllZero(histogram);
	}
	private void ensureAllZero(IHistogram histogram)
	{
		for (int i = 0; i < histogram.getBinCount(); i++)
		{
			assertEquals(0, histogram.getBinValue(i));
		}
	}

	@Test
	@SuppressWarnings("PMD.UnusedLocalVariable")
	public void testSimpleLogarithmicHistogram()
	{
		logTestName();

		//CHECKSTYLE:OFF
		LogarithmicHistogram		histogram1		= new LogarithmicHistogram(  0.01, 10000, LogarithmicHistogram.Precision.Normal);
		LogarithmicHistogram		histogram2A		= new LogarithmicHistogram(  2.0,  10000, LogarithmicHistogram.Precision.Coarse);
		LogarithmicHistogram		histogram2B		= new LogarithmicHistogram(  2.0,  10000, LogarithmicHistogram.Precision.Normal);
		LogarithmicHistogram		histogram2C		= new LogarithmicHistogram(  2.0,  10000, LogarithmicHistogram.Precision.Fine);
		LogarithmicHistogram		histogram3		= new LogarithmicHistogram(  3.0,  10000, LogarithmicHistogram.Precision.Normal);
		LogarithmicHistogram		histogram4		= new LogarithmicHistogram( 10.0,  10000, LogarithmicHistogram.Precision.Normal);
		LogarithmicHistogram		histogram5		= new LogarithmicHistogram(1000.0, 10000, LogarithmicHistogram.Precision.Normal);
		//CHECKSTYLE:ON

		// Invalid parameters...
		boolean			hasSeenException		= false;
		int				minStep					= 5;
		int				maxValue				= minStep - 1;		// To be smaller than minStep = exception
		try
		{
			new LogarithmicHistogram(minStep, maxValue, LogarithmicHistogram.Precision.Normal);
		}
		catch (IllegalArgumentException exception)
		{
			assertTrue(exception.getMessage().contains("" + minStep));			// NOPMD
			assertTrue(exception.getMessage().contains("" + maxValue));			// NOPMD
			hasSeenException = true;
		}
		assertTrue("Invalid argument should throw an exception", hasSeenException);
	}

	@Test
	public void testLinearHistogram()
	{
		logTestName();

		IHistogram		histogram		= new LinearHistogram(0, 10, 2);

		assertEquals(5, histogram.getBinCount());
		ensureAllZero(histogram);

		histogram.record(-1);
		histogram.record(0);
		histogram.record(3);
		histogram.record(6);
		histogram.record(10);
		histogram.record(10.1);

		assertEquals(2, histogram.getBinValue(0));
		assertEquals(1, histogram.getBinValue(1));
		assertEquals(0, histogram.getBinValue(2));
		assertEquals(1, histogram.getBinValue(3));
		assertEquals(2, histogram.getBinValue(4));

		List<IHistogram.BinData>		list	= histogram.getBinData();
		assertTrue(!list.isEmpty());
	}

	@Test
	@SuppressWarnings("PMD.UnusedLocalVariable")
	public void testLogarithmicHistogram()
	{
		logTestName();

		IHistogram		histogramC		= new LogarithmicHistogram(0.25, 10.0, LogarithmicHistogram.Precision.Coarse);
		IHistogram		histogramN		= new LogarithmicHistogram(0.25, 10.0, LogarithmicHistogram.Precision.Normal);
		IHistogram		histogramF		= new LogarithmicHistogram(0.25, 10.0, LogarithmicHistogram.Precision.Fine);
		IHistogram		histogram		= histogramC;

		assertEquals(21, histogram.getBinCount());
		ensureAllZero(histogram);

		histogram.record(-1);
		histogram.record(0);
		histogram.record(1.1);
		histogram.record(3);
		histogram.record(6);
		histogram.record(10);
		histogram.record(10.1);

		assertEquals(2, histogram.getBinValue( 0));		// 0
		assertEquals(0, histogram.getBinValue( 3));
		assertEquals(1, histogram.getBinValue( 5));		// 1.1
		assertEquals(0, histogram.getBinValue( 6));
		assertEquals(1, histogram.getBinValue(12));		// 3
		assertEquals(1, histogram.getBinValue(17));		// 6
		assertEquals(0, histogram.getBinValue(15));
		assertEquals(2, histogram.getBinValue(20));		// 10

		List<IHistogram.BinData>		list	= histogram.getBinData();
		assertTrue(!list.isEmpty());
	}

	@Test
	public void testWaterMark()
	{
		IHistogram[]	histogramList		= new IHistogram[5];
		histogramList[0] = new LinearHistogram(0, 100, 1);
		histogramList[1] = new LinearHistogram(0, 100, 0.1);
		histogramList[2] = new LogarithmicHistogram(.1, 100, LogarithmicHistogram.Precision.Coarse);
		histogramList[3] = new LogarithmicHistogram(.1, 100, LogarithmicHistogram.Precision.Normal);
		histogramList[4] = new LogarithmicHistogram(.1, 100, LogarithmicHistogram.Precision.Fine);
		for (IHistogram histogram : histogramList)
		{
			for (int i = 1; i <= 100; i++)
			{
				histogram.record(i);
			}
			validateWaterMark(histogram);
			histogram.record(200);
			assertEquals(100.0, histogram.getWaterMarkValue(1.0).getMax(), 1e-3);
			assertEquals(100.0, histogram.getWaterMarkValue(2.0).getMax(), 1e-3);
		}
	}
	private void validateWaterMark(IHistogram histogram)
	{
		getLogger().info(BasicEvent.EVENT_TEST, "Validating histogram " + histogram.toString());
		for (int i = 1; i <= 100; i++)
		{
			IHistogram.BinData		wmData		= histogram.getWaterMarkValue(i / 100.0);
//			getLogger().info(BasicEvent.EVENT_TEST, "WM(%4.1f) = %s", i * 1.0, wmData.toString());
			if (100 == i)
			{
				assertTrue(wmData.getMin() <= i && i <= wmData.getMax());
			}
			else
			{
				assertTrue(wmData.getMin() <= i && i < wmData.getMax());
			}
		}
	}

	@Test
	public void testRecordHistogram()
	{
		IHistogram		histogram1		= new LinearHistogram(0, 10, 1);
		IHistogram		histogram2		= new LinearHistogram(0, 10, 1);

		List<IHistogram.BinData>		binDataListEmpty		= histogram1.getBinData();
		assertTrue(1 == binDataListEmpty.size());
		assertEquals(9.0, binDataListEmpty.get(0).getMin(), 0.001);

		histogram1.record(1.5);
		histogram1.record(1.5);
		histogram2.record(7.5);
		histogram2.record(7.5);
		histogram1.record(histogram2);

		assertEquals(1.0, histogram1.getWaterMarkValue(0.4).getMin(), 0.001);
		assertEquals(7.0, histogram1.getWaterMarkValue(0.7).getMin(), 0.001);

		List<IHistogram.BinData>		binDataListFull		= histogram1.getBinData();
		assertEquals(3, binDataListFull.size());
		assertEquals(1.0, binDataListFull.get(0).getMin(), 0.001);
		assertEquals(7.0, binDataListFull.get(1).getMin(), 0.001);
		assertEquals(9.0, binDataListFull.get(2).getMin(), 0.001);
	}

	@Test
	public void testResetFamily()
	{
		validateResetFamily(new LinearHistogram(0, 5, 1));
		validateResetFamily(new LogarithmicHistogram(1, 100, LogarithmicHistogram.Precision.Normal));
	}
	private void validateResetFamily(IHistogram histogram)		// NOPMD -> It is used
	{
		IHistogram		histogramClone;

		populateForReset(histogram);
		histogramClone = histogram.cloneAndReset();
		assertFalse(histogram.equals(histogramClone));

		populateForReset(histogram);
		assertTrue(histogram.equals(histogramClone));
		assertTrue(histogram.hashCode() == histogramClone.hashCode());
		histogram.cloneAndReset(histogramClone);
	}
	private void populateForReset(IHistogram histogram)
	{
		histogram.record(1.5);
		histogram.record(2.5);
		histogram.record(3.5);
		histogram.record(4.5);
	}
}
