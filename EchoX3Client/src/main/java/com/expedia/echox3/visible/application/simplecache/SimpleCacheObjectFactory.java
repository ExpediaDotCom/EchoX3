/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.simplecache;

import java.io.Serializable;

import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.IObjectFactory;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

/**
 * Simple factory creating the SimpleCacheObject.
 */
public class SimpleCacheObjectFactory implements IObjectFactory
{
	private final SimpleCacheStatusHolder m_cacheStatus
															= new SimpleCacheStatusHolder();

	@Override
	public void updateConfiguration(ObjectCacheConfiguration configuration)
	{
		// Read the configuration
		m_cacheStatus.readConfiguration(configuration);
	}

	@Override
	public ICacheObject createObject()
	{
		return new SimpleCacheObject(m_cacheStatus);
	}

	/**
	 *
	 * @param key
	 * @return
	 */
	@Override
	public ICacheObject createObject(Serializable key)
	{
		return new SimpleCacheObject(m_cacheStatus);
	}

	@Override
	public void close()
	{
		m_cacheStatus.close();
	}
}
