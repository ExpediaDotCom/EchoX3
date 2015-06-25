/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;
//import com.expedia.echox3.basics.monitoring.counter.IValueCounter;
import com.expedia.echox3.basics.monitoring.counter.NullOperationCounter;
//import com.expedia.echox3.basics.monitoring.counter.NullValueCounter;

public class LockCounterFamily
{
	public static final String		COUNTER_NAME_PREFIX		= "Locks";

	private final StringGroup			m_name;
	private final IOperationCounter		m_readWaitCounter;
	private final IOperationCounter		m_writeWaitCounter;
	private final IOperationCounter		m_readHoldCounter;
	private final IOperationCounter		m_writeHoldCounter;

	// Temporary commented out while we determine if these counters are really needed.
//	private final IValueCounter			m_readSpinCounter;
//	private final IValueCounter			m_writeSpinCounter;

	public LockCounterFamily(StringGroup nameList)
	{
		if (null == nameList)
		{
			m_name = new StringGroup("No name");
			m_readWaitCounter	= new NullOperationCounter(null);
			m_writeWaitCounter	= new NullOperationCounter(null);
			m_readHoldCounter	= new NullOperationCounter(null);
			m_writeHoldCounter	= new NullOperationCounter(null);
//			m_readSpinCounter	= new NullValueCounter(null);
//			m_writeSpinCounter	= new NullValueCounter(null);
		}
		else
		{
			m_name = nameList;

			StringGroup		counterName		= new StringGroup();
			counterName.set(getCounterName("ReadWait"));
			m_readWaitCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(counterName,
					LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
					"Lock contention: Time spent waiting for the READ lock.");

			counterName.set(getCounterName("WriteWait"));
			m_writeWaitCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(counterName,
					LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
					"Lock contention: Time spent waiting for the WRITE lock.");

			counterName.set(getCounterName("ReadHold"));
			m_readHoldCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(counterName,
					LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
					"Lock contention: Time spent holding the READ lock.");

			counterName.set(getCounterName("WriteHold"));
			m_writeHoldCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(counterName,
					LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
					"Lock contention: Time spent holding the WRITE lock.");


/*			counterName.set(getCounterName("ReadSpinCount"));
			m_readSpinCounter = CounterFactory.getInstance().getValueCounter(counterName,
					LogarithmicHistogram.Precision.Coarse, CounterFactory.CounterRange.us,
					"Read spin aggregated for all FastReadWriteLocks");

			counterName.set(getCounterName("WriteSpinCount"));
			m_writeSpinCounter = CounterFactory.getInstance().getValueCounter(counterName,
					LogarithmicHistogram.Precision.Coarse, CounterFactory.CounterRange.us,
					"Write spin count aggregated for all FastReadWriteLocks");
*/
		}
	}
	public String getCounterName(String shortName)
	{
		return String.format("%s.%s.%s", COUNTER_NAME_PREFIX, m_name.getString(), shortName);
	}

	public void setEnabled(boolean isEnabled)
	{
		m_readWaitCounter.setEnabled(isEnabled);
		m_readHoldCounter.setEnabled(isEnabled);
		m_writeWaitCounter.setEnabled(isEnabled);
		m_writeHoldCounter.setEnabled(isEnabled);

//		m_readSpinCounter.setEnabled(isEnabled);
//		m_writeSpinCounter.setEnabled(isEnabled);
	}

	public IOperationContext beginReadWait()
	{
		return m_readWaitCounter.begin();
	}

	public IOperationContext beginWriteWait()
	{
		return m_writeWaitCounter.begin();
	}

	public IOperationContext beginReadHold()
	{
		return m_readHoldCounter.begin();
	}

	public IOperationContext beginWriteHold()
	{
		return m_writeHoldCounter.begin();
	}

	public void end(IOperationContext context, boolean isSuccess)
	{
		context.end(isSuccess);
	}
/*
	public void recordRead(long count)
	{
		m_readSpinCounter.record(count);
	}
	public void recordWrite(long count)
	{
		m_writeSpinCounter.record(count);
	}
*/
	public StringGroup getName()
	{
		return m_name;
	}

	public void close()
	{
		m_readWaitCounter.close();
		m_readHoldCounter.close();
		m_writeWaitCounter.close();
		m_writeHoldCounter.close();

//		m_readSpinCounter.close();
//		m_writeSpinCounter.close();
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getName().toString());
	}
}
