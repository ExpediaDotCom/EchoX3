/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class ItemSizeCounter extends BaseCounter implements IItemSizeCounter, ItemSizeCounterMBean
{
	private long				m_sum			= 0;
	private long				m_count			= 0;

	// Object for JMX
	private ItemSizeCounter		m_beanClone;

	// Accumulators for logging
	private double				m_logSum			= 0.0;
	private int					m_logItemCount		= 0;
	private int					m_logPointCount		= 0;

	public ItemSizeCounter(StringGroup name)
	{
		super(name, BaseCounter.CounterVisibility.Live);

		m_beanClone = new ItemSizeCounter(name, BaseCounter.CounterVisibility.JMX);
		m_beanClone.setEnabled(isEnabled());

		addCounter(getName().toString(), this);
	}
	private ItemSizeCounter(StringGroup nameList, BaseCounter.CounterVisibility visibility)
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

		if (!isEnabled)
		{
			m_sum		= 0;
			m_count		= 0;

			m_logSum		= 0.0;
			m_logItemCount	= 0;
			m_logPointCount	= 0;
		}
	}

	@Override
	public void add(long size)
	{
		synchronized (this)
		{
			m_sum += size;
			m_count++;
		}
	}
	@Override
	public void remove(long size)
	{
		synchronized (this)
		{
			m_sum -= size;
			m_count--;
		}
	}

	@Override
	public long getItemCount()
	{
		return m_count;
	}

	@Override
	public long getItemSizeAvg()
	{
		return m_sum / m_count;
	}

	@Override
	public long getItemSizeSum()
	{
		return m_sum;
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		setBeanDurationMS(durationMS);

		synchronized (this)
		{
			m_beanClone.m_sum		= m_sum;
			m_beanClone.m_count		= m_count;

			m_logSum				+= m_sum;
			m_logItemCount			+= m_count;
			m_logPointCount++;
		}
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		int				pointCount			= 0 == m_logPointCount ? 1 : m_logPointCount;
		double			averageTotal		= m_logSum / pointCount;
		long			averageItemCount	= m_logItemCount / pointCount;
		double			averageItem			= averageTotal / (0.0 == averageItemCount ? 1.0 : averageItemCount);

		StringBuilder	sb					= new StringBuilder(1000);
		addItemToLogEntry(sb, "ItemCount", averageItemCount);
		addItemToLogEntry(sb, "SizeAvg", averageItem);
		addItemToLogEntry(sb, "SizeTotal", averageTotal);
		logger.info(BasicEvent.EVENT_COUNTER_RESOURCE, sb.toString());

		m_logSum		= 0.0;
		m_logItemCount	= 0;
		m_logPointCount	= 0;
	}
}
