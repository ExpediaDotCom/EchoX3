/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.counter;

public interface ItemSizeCounterMBean
{
	long		getItemCount();
	long		getItemSizeAvg();
	long		getItemSizeSum();
}
