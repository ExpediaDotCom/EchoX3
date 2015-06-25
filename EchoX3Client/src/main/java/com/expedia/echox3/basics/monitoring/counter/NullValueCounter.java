/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class NullValueCounter extends BaseCounter implements IValueCounter
{
	public NullValueCounter(StringGroup nameList)
	{
		super(nameList, CounterVisibility.Invisible);
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		// Null counter
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		// Null counter
	}

	@Override
	public void record(double value)
	{
		// Null counter
	}
}
