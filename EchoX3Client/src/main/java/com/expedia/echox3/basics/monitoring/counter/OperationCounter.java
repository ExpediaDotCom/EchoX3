/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import java.util.function.Supplier;

import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.simple.IAccumulator;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.SimpleAccumulator;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class OperationCounter extends BaseCounter implements IOperationCounter, OperationCounterMBean
{
	private final ObjectPool<OperationContext> m_contextPool;

	private long				m_countBegin			= 0;
	private long				m_countSuccess			= 0;
	private long				m_countFailure			= 0;

	// Apply only to the m_beanClone instance...
	private double				m_rateBegin				= 0;
	private double				m_rateSuccess			= 0;
	private double				m_rateFailure			= 0;

	private IAccumulator m_accumulatorTimeMS		= new SimpleAccumulator();
	private IHistogram m_histogramTimeMS		= null;

	private OperationCounter	m_beanClone;
	private OperationCounter	m_logClone;

	public OperationCounter(StringGroup nameList, IHistogram histogramTimeMS)
	{
		super(nameList, CounterVisibility.Live);

		m_histogramTimeMS = histogramTimeMS;

		m_beanClone = new OperationCounter(nameList, CounterVisibility.JMX);
		m_beanClone.setEnabled(isEnabled());
		m_beanClone.m_histogramTimeMS = m_histogramTimeMS.getBlankClone();
		m_beanClone.m_accumulatorTimeMS = m_accumulatorTimeMS.getBlankClone();

		m_logClone = new OperationCounter(nameList, CounterVisibility.Log);
		m_logClone.m_histogramTimeMS = m_histogramTimeMS.getBlankClone();		// Have something to start with.

		StringGroup						contextName		= new StringGroup(nameList.getStringArray());
		contextName.prepend(OperationCounter.class.getSimpleName());
		m_contextPool = new ObjectPool<>(contextName, OperationContext::new);

		addCounter(getName().toString(), this);
	}
	private OperationCounter(StringGroup name, CounterVisibility visibility)
	{
		super(name, visibility);

		m_contextPool = null;
	}

	public ObjectPool<OperationContext> getContextPool()
	{
		return m_contextPool;
	}

	@Override
	public void close()
	{
		super.close();

		if (null != m_beanClone)
		{
			m_beanClone.close();
		}
		if (null != m_contextPool)
		{
			m_contextPool.release();
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
			if (null != m_accumulatorTimeMS)
			{
				// Means during constructor, accumulators and histograms will already be in reset state.
				m_accumulatorTimeMS.reset();
			}
			if (null != m_histogramTimeMS)
			{
				m_histogramTimeMS.reset();
			}

			// Really applicable only to m_beanClone, but does not hurt.
			m_rateBegin		= 0.0;
			m_rateSuccess	= 0.0;
			m_rateFailure	= 0.0;

			m_countBegin	= 0;
			m_countSuccess	= 0;
			m_countFailure	= 0;
		}
	}

	public IOperationContext begin()
	{
		if (!isEnabled())
		{
			return IOperationContext.NULL_CONTEXT;
		}

		synchronized (this)
		{
			m_countBegin++;
		}

		OperationContext		context		= getContextPool().get();
		context.setOperation(this);
		return context;
	}

	void end(boolean isSuccess, double durationMS)
	{
		synchronized (this)
		{
			if (isSuccess)
			{
				m_countSuccess++;
			}
			else
			{
				m_countFailure++;
			}

			m_accumulatorTimeMS.record(durationMS);
			if (null != m_histogramTimeMS)
			{
				m_histogramTimeMS.record(durationMS);
			}
		}
	}

	public void doBeanUpdate(long durationMS)
	{
		setBeanDurationMS(durationMS);

		synchronized (this)
		{
			m_beanClone.m_rateBegin = (m_countBegin - m_beanClone.m_countBegin) * 1000.0 / durationMS;
			m_beanClone.m_rateSuccess = (m_countSuccess - m_beanClone.m_countSuccess) * 1000.0 / durationMS;
			m_beanClone.m_rateFailure = (m_countFailure - m_beanClone.m_countFailure) * 1000.0 / durationMS;

			m_beanClone.m_countBegin = m_countBegin;
			m_beanClone.m_countSuccess = m_countSuccess;
			m_beanClone.m_countFailure = m_countFailure;
			m_accumulatorTimeMS.cloneAndReset(m_beanClone.m_accumulatorTimeMS);
			m_logClone.m_accumulatorTimeMS.record(m_beanClone.m_accumulatorTimeMS);

			m_histogramTimeMS.cloneAndReset(m_beanClone.m_histogramTimeMS);
			m_logClone.m_histogramTimeMS.record(m_beanClone.m_histogramTimeMS);
		}
	}

	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		// Synchronization done inside IHistogram.cloneAndReset!
		m_accumulatorTimeMS.cloneAndReset(m_beanClone.m_accumulatorTimeMS);
		m_logClone.m_accumulatorTimeMS.record(m_beanClone.m_accumulatorTimeMS);

		IHistogram		histogram		= m_logClone.m_histogramTimeMS.cloneAndReset();
		logHistogram(logger, durationMS, histogram);
	}


	//	**************************************************
	//	MBean methods
	//	**************************************************
	public long getCountBegin()
	{
		return m_countBegin;
	}

	public long getCountSuccess()
	{
		return m_countSuccess;
	}

	public long getCountFailure()
	{
		return m_countFailure;
	}

	public long getCountOutstanding()
	{
		return m_countBegin - (m_countSuccess + m_countFailure);
	}

	public double getRateBeginQPS()
	{
		return m_rateBegin;
	}

	public double getRateSuccessQPS()
	{
		return m_rateSuccess;
	}

	public double getRateFailureQPS()
	{
		return m_rateFailure;
	}

	public double getDurationCount()
	{
		return m_accumulatorTimeMS.getCount();
	}
	public double getDurationMinMS()
	{
		return m_accumulatorTimeMS.getMin();
	}
	public double getDurationAvgMS()
	{
		return m_accumulatorTimeMS.getAvg();
	}
	public double getDurationMaxMS()
	{
		return m_accumulatorTimeMS.getMax();
	}
	public double getDurationStdMS()
	{
		return m_accumulatorTimeMS.getStd();
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getName());
	}
}
