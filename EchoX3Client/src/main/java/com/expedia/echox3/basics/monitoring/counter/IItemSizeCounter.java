/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.counter;

public interface IItemSizeCounter extends IBaseCounter
{
	void		add(long size);
	void		remove(long size);
}
