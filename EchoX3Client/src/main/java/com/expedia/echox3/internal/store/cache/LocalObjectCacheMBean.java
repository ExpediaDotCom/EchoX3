/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.store.cache;

import java.util.Map;

public interface LocalObjectCacheMBean
{
	String					getFactoryClassName();
	String					getMaintenancePeriod();
	long					getMaintenancePeriodMS();
	Map<String, String>		getConfigurationMap();

	long					getItemCount();
	void					flush(int durationMS);
}
