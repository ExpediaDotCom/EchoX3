/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

/**
 * User is responsible for synchronization.
 */
public class SimpleAccumulator implements IAccumulator
{
	private int			m_count;
	private double		m_min;
	private double		m_max;
	private double		m_sum;
	private double		m_sum2;		// Sum of square for Standard Deviation calculation

	public SimpleAccumulator()
	{
		m_count = 0;
	}

	@Override
	public void record(double value)
	{
		if (0 == m_count)
		{
			m_min = value;
			m_max = value;
			m_sum = value;
			m_sum2 = (value * value);
		}
		else
		{
			m_min = Math.min(m_min, value);
			m_max = Math.max(m_max, value);
			m_sum += value;
			m_sum2 += (value * value);
		}
		m_count++;
	}
	@Override
	public void record(IAccumulator accumulatorIn)
	{
		if (!(accumulatorIn instanceof SimpleAccumulator))
		{
			throw new IllegalArgumentException("Argument must be a " + getClass().getSimpleName());
		}
		SimpleAccumulator		accumulator		= (SimpleAccumulator) accumulatorIn;

		if (0 == m_count)
		{
			m_min = accumulator.m_min;
			m_max = accumulator.m_max;
			m_sum = accumulator.m_sum;
			m_sum2 = accumulator.m_sum2;
		}
		else
		{
			m_min = Math.min(m_min, accumulator.m_min);
			m_max = Math.max(m_max, accumulator.m_max);
			m_sum += accumulator.m_sum;
			m_sum2 += accumulator.m_sum2;
		}
		m_count += accumulator.m_count;
	}

	@Override
	public IAccumulator	getBlankClone()
	{
		return new SimpleAccumulator();
	}
	@Override
	public void reset()
	{
		m_count = 0;
	}
	@Override
	public IAccumulator cloneAndReset()
	{
		SimpleAccumulator		accumulator		= (SimpleAccumulator) getBlankClone();
		cloneAndReset(accumulator);
		return accumulator;
	}

	@Override
	public void cloneAndReset(IAccumulator accumulator)
	{
		assert(accumulator instanceof SimpleAccumulator); // cloneAndReset() supported only for like accumulators
		SimpleAccumulator		simpleAccumulator		= (SimpleAccumulator) accumulator;

		simpleAccumulator.m_count = m_count;
		simpleAccumulator.m_min = m_min;
		simpleAccumulator.m_max = m_max;
		simpleAccumulator.m_sum = m_sum;
		simpleAccumulator.m_sum2 = m_sum2;

		m_count = 0;
	}

	@Override
	public int getCount()
	{
		return m_count;
	}

	@Override
	public double getMin()
	{
		return 0 == m_count ? 0.0 : m_min;
	}

	@Override
	public double getMax()
	{
		return 0 == m_count ? 0.0 : m_max;
	}

	@Override
	public double getAvg()
	{
		return 0 == m_count ? 0.0 : m_sum / m_count;
	}

	// Equation for STD from http://en.wikipedia.org/wiki/Standard_deviation
	@Override
	public double getStd()
	{
		if (0 == m_count)
		{
			return 0.0;
		}
		else
		{
			double		avg			= getAvg();
			double		sqrtSum		= avg * avg;
			double		sigma2		= (m_sum2 / m_count) - sqrtSum;

			return Math.sqrt(sigma2);
		}
	}

	@Override
	public double getSum()
	{
		return 0 == m_count ? 0.0 : m_sum;
	}
}
