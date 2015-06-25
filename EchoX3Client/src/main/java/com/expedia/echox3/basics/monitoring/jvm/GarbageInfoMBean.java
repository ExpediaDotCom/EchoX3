/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.jvm;

/**
 * This interface defines the values exposed in JMX by the class GarbageInfo
 */
public interface GarbageInfoMBean
{
	String		getCollectorName();
	String		getPrimaryPoolName();

	long		getTotalCount();
	long		getTotalDurationMS();
	long		getTotalElapsedMS();
	String		getTotalElapsed();
	double		getTotalAverageMS();
	double		getTotalPeriodSec();
	double		getTotalDutyCyclePercent();

	long		getLatchTimeMS();
	String		getLatchDate();

	long		getCurrentCount();
	double		getCurrentDurationMS();
	long		getCurrentElapsedMS();
	String		getCurrentElapsed();

	double		getPauseCurrentMinimumMS();
	double		getPauseCurrentAverageMS();
	double		getPauseCurrentMaximumMS();

	double		getClockCurrentMinimumMS();
	double		getClockCurrentAverageMS();
	double		getClockCurrentMaximumMS();

	double		getCurrentPeriodSec();
	double		getCurrentDutyCyclePercent();

	String[]	getHistogramData();
}
