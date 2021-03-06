/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

@SuppressWarnings("PMD.UnusedModifier")
public interface ResourceCounterMBean
{
	public boolean		isEnabled();
	public double		getValueFirst();
	public double		getMin();
	public double		getAvg();
	public double		getMax();
	public double		getValueLast();
	public double		getRate();
}
