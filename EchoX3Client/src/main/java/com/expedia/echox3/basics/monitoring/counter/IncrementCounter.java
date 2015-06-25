/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;


import java.util.concurrent.atomic.AtomicLong;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class IncrementCounter extends BaseCounter implements IIncrementCounter, IncrementCounterMBean
{
	private AtomicLong			m_value				= new AtomicLong(0);

	// Only used for m_beanClone
	private double				m_rate				= 0.0;

	private IncrementCounter	m_beanClone;		// The JMX one
	private double				m_logValueSav;		// Used for logging

	public IncrementCounter(StringGroup name)
	{
		super(name, CounterVisibility.Live);

		m_beanClone = new IncrementCounter(name, CounterVisibility.JMX);
		m_beanClone.setEnabled(isEnabled());

		addCounter(getName().toString(), this);

		m_logValueSav = 0;
	}
	private IncrementCounter(StringGroup name, CounterVisibility visibility)
	{
		super(name, visibility);
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

		if (!isEnabled)
		{
			m_value.set(0);
			m_rate = 0;
			m_logValueSav = 0;
		}
	}

	@Override
	public void increment()
	{
		if (!isEnabled())
		{
			return;
		}

		m_value.incrementAndGet();
	}

	@Override
	public void decrement()
	{
		if (!isEnabled())
		{
			return;
		}

			m_value.decrementAndGet();
	}

	@Override
	public void increment(long n)
	{
		if (!isEnabled())
		{
			return;
		}

		m_value.addAndGet(n);
	}

	@Override
	public void decrement(long n)
	{
		if (!isEnabled())
		{
			return;
		}

		m_value.addAndGet(-n);
	}

	@Override
	public IncrementCounterMBean getMBean()
	{
		return m_beanClone;
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		setBeanDurationMS(durationMS);

		long		valueSav		= m_value.get();
		m_beanClone.m_rate = (valueSav - m_beanClone.m_value.get()) * 1000.0 / durationMS;
		m_beanClone.m_value.set(valueSav);
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		double		rate	= (m_beanClone.m_value.get() - m_logValueSav) * 1000.0 / durationMS;
		double		value	= m_beanClone.m_value.get();

		StringBuilder sb				= new StringBuilder(1000);

		addHeaderToLogEntry(sb, durationMS);
		addItemToLogEntry(sb, "Value", value);
		addItemToLogEntry(sb, "Rate", rate);

		logger.info(BasicEvent.EVENT_COUNTER_INCREMENT, sb.toString());

		m_logValueSav = m_beanClone.m_value.get();
	}

	@Override
	public long getValue()
	{
		return m_value.get();
	}

	@Override
	public double getIncomingRate()
	{
		return m_rate;
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getName());
	}
}
