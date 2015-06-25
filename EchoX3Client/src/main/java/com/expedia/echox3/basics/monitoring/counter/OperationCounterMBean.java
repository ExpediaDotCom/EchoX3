/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

@SuppressWarnings("PMD.UnusedModifier")
public interface OperationCounterMBean
{
	public boolean		isEnabled();
	public long			getCountBegin();
	public long			getCountSuccess();
	public long			getCountFailure();
	public long			getCountOutstanding();

	public double		getRateBeginQPS();
	public double		getRateSuccessQPS();
	public double		getRateFailureQPS();

	public double		getDurationCount();
	public double		getDurationMinMS();
	public double		getDurationAvgMS();
	public double		getDurationMaxMS();
	public double		getDurationStdMS();
}
