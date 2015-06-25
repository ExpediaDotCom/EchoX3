/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

@SuppressWarnings("PMD.UnusedModifier")
public interface IncrementCounterMBean
{
	public boolean		isEnabled();
	public long			getValue();
	public double		getIncomingRate();
}
