/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;


import com.expedia.echox3.basics.collection.simple.ObjectPool;

public class OperationContext extends ObjectPool.AbstractPooledObject implements IOperationContext
{
	private long				m_timeBeginNS;
	private OperationCounter	m_operation;

	public OperationContext()
	{
	}

	public void setOperation(OperationCounter operation)
	{
		m_operation = operation;
		m_timeBeginNS = System.nanoTime();
	}

	public double end(boolean isSuccess)
	{
		long		timeEndNS		= System.nanoTime();
		double		durationMS		= (timeEndNS - m_timeBeginNS) / (1000.0 * 1000);
		m_operation.end(isSuccess, durationMS);
		this.release();

		return durationMS;
	}
}
