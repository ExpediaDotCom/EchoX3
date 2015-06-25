/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

@SuppressWarnings("PMD.UnusedModifier")
public interface IOperationContext
{
	public IOperationContext	NULL_CONTEXT		= isSuccess -> 0;

	public double				end(boolean isSuccess);		// returns durationMS
}
