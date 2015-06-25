/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.util.Map;

@SuppressWarnings("PMD")
public interface BasicClassLoaderMBean
{
	public Map<String, Integer>		getClassSizeMap();
	public boolean					isClassLoaded(String className);
}
