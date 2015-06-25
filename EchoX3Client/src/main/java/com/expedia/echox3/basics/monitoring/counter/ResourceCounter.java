/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class ResourceCounter extends BaseCounter implements IResourceCounter, ResourceCounterMBean
{
	private double				m_valueFirst	= 0;
	private double				m_valueLast		= 0;
	private double				m_valueMin;
	private double				m_valueMax;
	private long				m_timeNS		= 0;
	private double				m_sumValue		= 0.0;
	private long				m_sumNS			= 0;

	private double				m_rate			= 0.0;

	private ResourceCounter		m_beanClone;
	private ResourceCounter		m_logClone;

	public ResourceCounter(StringGroup name)
	{
		super(name, CounterVisibility.Live);

		m_beanClone = new ResourceCounter(name, CounterVisibility.JMX);
		m_beanClone.setEnabled(isEnabled());

		m_logClone = new ResourceCounter(name, CounterVisibility.Log);

		addCounter(getName().toString(), this);
	}
	private ResourceCounter(StringGroup nameList, CounterVisibility visibility)
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
			m_valueFirst = 0;
			m_valueMin = 0;
			m_valueMax = 0;
			m_valueLast = 0;
			m_sumValue = 0;
			m_timeNS = 0;
			m_sumNS = 0;
			m_rate = 0;
		}
	}

	public void setValue(double value)
	{
		if (!isEnabled())
		{
			return;
		}

		synchronized (this)
		{
			if (0 == m_timeNS)
			{
				// First set!
				m_valueFirst = value;
				m_valueLast = value;
				m_valueMin = value;
				m_valueMax = value;
				m_timeNS = System.nanoTime();
			}
			else
			{
				// Normal case
				markValue(value);
			}
		}
	}

	public void moveValue(double delta)
	{
		if (!isEnabled())
		{
			return;
		}

		if (0 == m_timeNS)
		{
			throw new IllegalStateException("Value can be moved only after it has been set at least once.");
		}

		synchronized (this)
		{
			markValue(m_valueLast + delta);
		}
	}

	private void markValue(double value)
	{
		long		timeNS		= System.nanoTime();
		long		deltaNS		= timeNS - m_timeNS;
		m_valueLast = value;
		m_valueMin = Math.min(m_valueMin, value);
		m_valueMax = Math.max(m_valueMax, value);
		m_sumValue += (m_valueLast * deltaNS);
		m_sumNS += deltaNS;
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		setBeanDurationMS(durationMS);

		synchronized (this)
		{
			markValue(m_valueLast);

			m_beanClone.m_valueFirst = m_valueFirst;
			m_beanClone.m_valueMin = m_valueMin;
			m_beanClone.m_valueMax = m_valueMax;
			m_beanClone.m_valueLast = m_valueLast;
			m_beanClone.m_sumValue = m_sumValue;
			m_beanClone.m_timeNS = m_timeNS;
			m_beanClone.m_sumNS = m_sumNS;
			m_beanClone.m_rate = (m_valueLast - m_valueFirst) * 1000.0 / durationMS;

			m_valueFirst = m_valueLast;
			m_valueMin = m_valueLast;
			m_valueMax = m_valueLast;
			m_sumValue = 0.0;
			m_sumNS = 0;

			m_logClone.m_valueMin		= Math.min(m_logClone.m_valueMin, m_beanClone.m_valueMin);
			m_logClone.m_valueMax		= Math.max(m_logClone.m_valueMax, m_beanClone.m_valueMax);
			m_logClone.m_valueLast		= m_beanClone.m_valueLast;
			m_logClone.m_sumValue		+= m_beanClone.m_sumValue;
			m_logClone.m_timeNS			= m_beanClone.m_timeNS;
			m_logClone.m_sumNS			+= m_beanClone.m_sumNS;
		}
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		double			min		= m_logClone.m_valueMin;
		double			avg		= 0 == m_logClone.m_sumNS ? 0 : (m_logClone.m_sumValue / m_logClone.m_sumNS);
		double			max		= m_logClone.m_valueMax;
		double			rate	= (m_logClone.m_valueLast - m_logClone.m_valueFirst) * 1000.0 / durationMS;

		StringBuilder	sb				= new StringBuilder(1000);

		addHeaderToLogEntry(sb, durationMS);

		addItemToLogEntry(sb, "Min", min);
		addItemToLogEntry(sb, "Avg", avg);
		addItemToLogEntry(sb, "Max", max);
		addItemToLogEntry(sb, "Rate", rate);

		logger.info(BasicEvent.EVENT_COUNTER_RESOURCE, sb.toString());

		// Reset the m_logClone for the next interval
		m_logClone.m_valueFirst		= m_beanClone.m_valueLast;
		m_logClone.m_valueMin		= m_beanClone.m_valueLast;
		m_logClone.m_valueMax		= m_beanClone.m_valueLast;
		m_logClone.m_valueLast		= m_beanClone.m_valueLast;
		m_logClone.m_sumValue		= 0;
		m_logClone.m_timeNS			= m_beanClone.m_timeNS;
		m_logClone.m_sumNS			= 0;
	}

	public double getValueFirst()
	{
		return m_valueFirst;
	}

	public double getMin()			// Valid only after cloneAndReset
	{
		return m_valueMin;
	}
	public double getAvg()			// Valid only after cloneAndReset
	{
		return 0 == m_sumNS ? 0 : m_sumValue / m_sumNS;
	}
	public double getMax()
	{
		return m_valueMax;
	}

	public double getValueLast()
	{
		return m_valueLast;
	}

	public double getRate()
	{
		return m_rate;
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getName());
	}
}
