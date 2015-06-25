/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;


import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class NullOperationCounter extends BaseCounter implements IOperationCounter
{
	public NullOperationCounter(StringGroup nameList)
	{
		super(nameList, CounterVisibility.Invisible);
	}

	@Override
	public IOperationContext begin()
	{
		return IOperationContext.NULL_CONTEXT;
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		// Nothing to do
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		// Nothing to do
	}
}
