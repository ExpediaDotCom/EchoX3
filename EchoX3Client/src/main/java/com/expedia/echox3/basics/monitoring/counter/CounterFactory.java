/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.histogram.LinearHistogram;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.StringGroup;

public class CounterFactory
{
	public enum CounterRange
	{
		// Max value is always setup to cover 4-5 decades.
		us,				// minStep = 1 us; max = 1 sec
		ms,				// minStep = 1 ms; max = 1 minute
		sec,			// minStep = 1 sec; max = 1 day
		min,			// minStep = 1 min; max = 1 week
		hour,			// minStep = 1 hour; max = 1 year
		none
	}

	private static final CounterFactory			INSTANCE		= new CounterFactory();

	private static final double			ONE_SECOND					= 1000;
	private static final double			ONE_MINUTE					=  60 * ONE_SECOND;
	private static final double			ONE_HOUR					=  60 * ONE_MINUTE;
	private static final double			ONE_DAY						=  24 * ONE_HOUR;
	private static final double			ONE_WEEK					=   7 * ONE_DAY;
	private static final double			ONE_YEAR					= 365 * ONE_DAY;
	private static final double[]		HISTOGRAM_MIN_STEP_LIST		=
			{ 0.001, 1, ONE_SECOND, ONE_MINUTE, ONE_HOUR };
	private static final double[]		HISTOGRAM_MAX_VALUE_LIST		=
			{ ONE_SECOND, ONE_MINUTE, ONE_DAY, ONE_WEEK, ONE_YEAR };

	public static CounterFactory getInstance()
	{
		return INSTANCE;
	}

	public IOperationCounter getLogarithmicOperationCounter(StringGroup nameList,
							LogarithmicHistogram.Precision precision, CounterRange range, String description)
	{
		double		minStep		= HISTOGRAM_MIN_STEP_LIST[range.ordinal()];
		double		maxValue	= HISTOGRAM_MAX_VALUE_LIST[range.ordinal()];
		IHistogram histogram	=
				CounterRange.none == range ? null : new LogarithmicHistogram(minStep, maxValue, precision);

		return new OperationCounter(nameList, histogram);
	}
	public IOperationCounter getLinearOperationCounter(StringGroup nameList, double min, double max, double step)
	{
		IHistogram	histogram	= new LinearHistogram(min, max, step);

		return new OperationCounter(nameList, histogram);
	}

	public IIncrementCounter getIncrementCounter(StringGroup nameList, String description)
	{
		return new IncrementCounter(nameList);
	}

	public IResourceCounter getResourceCounter(StringGroup nameList, String description)
	{
		return new ResourceCounter(nameList);
	}

	public IItemSizeCounter getItemSizeCounter(StringGroup nameList, String description)
	{
		return new ItemSizeCounter(nameList);
	}

	public IValueCounter getValueCounter(StringGroup nameList,
			LogarithmicHistogram.Precision precision, CounterRange range, String description)
	{
		double		minStep		= HISTOGRAM_MIN_STEP_LIST[range.ordinal()];
		double		maxValue	= HISTOGRAM_MAX_VALUE_LIST[range.ordinal()];
		IHistogram	histogram	=
				CounterRange.none == range ? null : new LogarithmicHistogram(minStep, maxValue, precision);

		return new ValueCounter(nameList, histogram);
	}
}
