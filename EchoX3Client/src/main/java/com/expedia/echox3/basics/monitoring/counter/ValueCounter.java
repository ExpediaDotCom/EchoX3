/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.simple.IAccumulator;
import com.expedia.echox3.basics.collection.simple.SimpleAccumulator;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class ValueCounter extends BaseCounter implements IValueCounter, ValueCounterMBean
{
	private IAccumulator		m_accumulatorValue		= new SimpleAccumulator();
	private IHistogram			m_histogramValue		= null;

	private ValueCounter		m_beanClone				= null;
	private ValueCounter		m_logClone				= null;

	public ValueCounter(StringGroup name, IHistogram histogram)
	{
		super(name, CounterVisibility.Live);

		m_histogramValue = histogram;

		m_beanClone = new ValueCounter(name, CounterVisibility.JMX);
		m_beanClone.setEnabled(isEnabled());
		m_beanClone.m_histogramValue = m_histogramValue.getBlankClone();

		m_logClone = new ValueCounter(name, CounterVisibility.Log);
		m_logClone.m_histogramValue = m_histogramValue.getBlankClone();		// Have something to start with.

		addCounter(getName().toString(), this);
	}
	private ValueCounter(StringGroup nameList, CounterVisibility visibility)
	{
		super(nameList, visibility);
	}

	@Override
	public void close()
	{
		super.close();

		if (null != m_beanClone)
		{
			m_beanClone.close();
		}
	}

	@Override
	public void setEnabled(boolean isEnabled)
	{
		super.setEnabled(isEnabled);
		if (null != m_beanClone)
		{
			m_beanClone.setEnabled(isEnabled);
		}
		if (null != m_logClone)
		{
			m_logClone.setEnabled(isEnabled);
		}

		if (!isEnabled)
		{
			if (null != m_accumulatorValue)
			{
				// Means during constructor, accumulators and histograms will already be in reset state.
				m_accumulatorValue.reset();
				m_histogramValue.reset();
			}
		}
	}

	public void record(double value)
	{
		if (!isEnabled())
		{
			return;
		}

		m_accumulatorValue.record(value);
		if (null != m_histogramValue)
		{
			m_histogramValue.record(value);
		}
	}

	public void doBeanUpdate(long durationMS)
	{
		setBeanDurationMS(durationMS);
		m_beanClone.setBeanDurationMS(durationMS);

		m_accumulatorValue.cloneAndReset(m_beanClone.m_accumulatorValue);
		m_logClone.m_accumulatorValue.record(m_beanClone.m_accumulatorValue);

		m_histogramValue.cloneAndReset(m_beanClone.m_histogramValue);
		m_logClone.m_histogramValue.record(m_beanClone.m_histogramValue);
	}

	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		IHistogram		histogram		= m_logClone.m_histogramValue.cloneAndReset();
		IAccumulator	accumulator		= m_logClone.m_accumulatorValue.cloneAndReset();

		logHistogram(logger, durationMS, histogram);
		logAccumulator(logger, durationMS, accumulator);
	}

	public double getCount()
	{
		return m_accumulatorValue.getCount();
	}
	public double getMin()
	{
		return m_accumulatorValue.getMin();
	}
	public double getAvg()
	{
		return m_accumulatorValue.getAvg();
	}
	public double getMax()
	{
		return m_accumulatorValue.getMax();
	}
	public double getStd()
	{
		return m_accumulatorValue.getStd();
	}
	public double getIncomingRate()
	{
		return 0 == getBeanDurationMS() ? 0.0 : (getCount() * 1000.0 / getBeanDurationMS());
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getName());
	}
}
