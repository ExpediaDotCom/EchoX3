/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.wrapper;

import com.expedia.echox3.internal.provider.IObjectCacheProvider;

public class TrellisBaseClient
{
	private IObjectCacheProvider m_provider;

	protected TrellisBaseClient(IObjectCacheProvider provider)
	{
		m_provider = provider;
	}

	public IObjectCacheProvider getProvider()
	{
		return m_provider;
	}
}
