/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

public interface ObjectPoolMBean
{
	int			getOutstandingCount();
	int			getBlankCount();
	int			getAllocatedCount();
	int			getWaitingCount();
	int			getMaxCount();
	long		getNotifyCount();

	void		reset();
}
