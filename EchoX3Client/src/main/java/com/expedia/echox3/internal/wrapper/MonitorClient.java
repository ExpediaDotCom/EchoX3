/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.wrapper;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.internal.provider.IObjectCacheProvider;
import com.expedia.echox3.visible.trellis.IMonitorClient;

public class MonitorClient extends TrellisBaseClient implements IMonitorClient
{
	public MonitorClient(IObjectCacheProvider provider)
	{
		super(provider);
	}

	@Override
	public void close(String cacheName) throws BasicException
	{
		// NYI
	}
}
