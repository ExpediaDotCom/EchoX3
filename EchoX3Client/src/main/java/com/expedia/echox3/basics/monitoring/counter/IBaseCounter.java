/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.counter;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

@SuppressWarnings("PMD.UnusedModifier")
public interface IBaseCounter
{
	StringGroup			getName();

	void				doBeanUpdate(long durationMS);
	void				doLogUpdate(BasicLogger logger, long durationMS);

	void				updateConfiguration();
	boolean				isEnabled();
	void				setEnabled(boolean isEnabled);

	void				close();
}
