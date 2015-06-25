/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

@SuppressWarnings("PMD.UnusedModifier")
public interface IResourceCounter extends IBaseCounter
{
	public void			setValue(double value);
	public void			moveValue(double delta);
}
