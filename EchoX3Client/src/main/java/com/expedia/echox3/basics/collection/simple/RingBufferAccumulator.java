/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.Arrays;

public class RingBufferAccumulator implements IAccumulator
{
	int				m_nextIndex		= 0;
	int				m_valueCount	= 0;
	double[]		m_valueList;

	private RingBufferAccumulator()
	{

	}

	public RingBufferAccumulator(int count)
	{
		m_valueList = new double[count];
	}

	@Override
	public void record(double value)
	{
		m_valueList[m_nextIndex++] = value;
		m_valueCount = Math.min(m_valueList.length, m_valueCount + 1);
		if (m_valueList.length == m_nextIndex)
		{
			m_nextIndex = 0;
		}
	}

	@Override
	public void record(IAccumulator accumulator)
	{
		if (!(accumulator instanceof RingBufferAccumulator))
		{
			throw new UnsupportedOperationException("Can only accumulate other ring buffers.");
		}

		RingBufferAccumulator buffer		= (RingBufferAccumulator) accumulator;
		int				index;
		if (buffer.m_valueList.length == buffer.m_valueCount)
		{
			index = buffer.m_nextIndex;		// Last value written == Next value to overwrite
		}
		else
		{
			index = 0;
		}

		// Record the values in the correct order
		for (int i = 0; i < buffer.m_valueCount; i++)
		{
			record(buffer.m_valueList[index++]);
			if (buffer.m_valueList.length == index)
			{
				index = 0;
			}
		}
	}

	@Override
	public IAccumulator getBlankClone()
	{
		return new RingBufferAccumulator();
	}

	@Override
	public void reset()
	{
		m_valueCount = 0;
		m_nextIndex = 0;
		Arrays.fill(m_valueList, 0.0);
	}

	@Override
	public IAccumulator cloneAndReset()
	{
		IAccumulator		ringBuffer		= getBlankClone();
		cloneAndReset(ringBuffer);
		return ringBuffer;
	}

	@Override
	public void cloneAndReset(IAccumulator accumulator)
	{
		RingBufferAccumulator ringBuffer		= (RingBufferAccumulator) accumulator;
		synchronized (this)
		{
			ringBuffer.m_valueCount = m_valueCount;
			ringBuffer.m_nextIndex = m_nextIndex;
			if (null == ringBuffer.m_valueList
				|| ringBuffer.m_valueList.length != m_valueList.length)
			{
				ringBuffer.m_valueList = new double[m_valueList.length];
			}

			// Swap the m_valueList, to give the current one to the clone accumulator
			double[]		temp		= ringBuffer.m_valueList;
			ringBuffer.m_valueList = m_valueList;
			m_valueList = temp;
			reset();
		}
	}

	@Override
	public int getCount()
	{
		return m_valueCount;
	}

	@Override
	public double getMin()
	{
		//  Take advantage of the fact that, if m_valueCount is NOT m_valueList.length, only the first few values are used
		double		min		= Double.POSITIVE_INFINITY;
		for (int i = 0; i < m_valueCount; i++)
		{
			min = Math.min(min, m_valueList[i]);
		}
		return min;
	}

	@Override
	public double getMax()
	{
		//  Take advantage of the fact that, if m_valueCount is NOT m_valueList.length, only the first few values are used
		double		max		= Double.NEGATIVE_INFINITY;
		for (int i = 0; i < m_valueCount; i++)
		{
			max = Math.max(max, m_valueList[i]);
		}
		return max;
	}

	@Override
	public double getAvg()
	{
		//  Take advantage of the fact that, if m_valueCount is NOT m_valueList.length, only the first few values are used
		double		sum		= 0.0;
		for (int i = 0; i < m_valueCount; i++)
		{
			sum += m_valueList[i];
		}
		return 0 == m_valueCount ? 0 : (sum / m_valueCount);
	}

	@Override
	public double getStd()
	{
		throw new UnsupportedOperationException("Standard deviation not supported for RingBufferAccumulator.");
	}

	@Override
	public double getSum()
	{
		//  Take advantage of the fact that, if m_valueCount is NOT m_valueList.length, only the first few values are used
		double		sum		= 0.0;
		for (int i = 0; i < m_valueCount; i++)
		{
			sum += m_valueList[i];
		}
		return sum;
	}

}
