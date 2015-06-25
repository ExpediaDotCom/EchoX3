/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.visible.ioc;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.trellis.ClientFactory;
import com.expedia.echox3.visible.trellis.IClientFactory.ClientType;
import com.expedia.echox3.visible.trellis.ISimpleCacheClient;

/**
 * Implements virtually the same methods as ISimpleCacheClient,
 * but within the context of IocCacheClient.
 * See IocCacheClient for usage of initialization methods of the IOC wrapper;
 * See ISimpleCacheClient for usage of the cache methods.
 */
public class IocSimpleCacheClient extends IocCacheClient
{
	private ISimpleCacheClient m_cacheClient;

	public IocSimpleCacheClient()
	{
	}

	public IocSimpleCacheClient(ClientType clientType, String cacheName, URL url) throws BasicException
	{
		super(clientType, cacheName, url);
	}
	@Override
	protected void update() throws BasicException
	{
		m_cacheClient = ClientFactory.getInstance().getSimpleClient(getClientType());
	}

	public void put(Serializable key, Serializable value) throws BasicException
	{
		m_cacheClient.put(getCacheName(), key, value);
	}

	public void put(Serializable[] keyList, Serializable[] valueList) throws BasicException
	{
		m_cacheClient.put(getCacheName(), keyList, valueList);
	}

	public void put(Map<Serializable, Serializable> keyValueMap) throws BasicException
	{
		m_cacheClient.put(getCacheName(), keyValueMap);
	}

	public Serializable get(Serializable key) throws BasicException
	{
		return m_cacheClient.get(getCacheName(), key);
	}

	public Serializable[] get(Serializable[] keyList) throws BasicException
	{
		return m_cacheClient.get(getCacheName(), keyList);
	}

	public Map<Serializable, Serializable> get(Collection<Serializable> keySet) throws BasicException
	{
		return m_cacheClient.get(getCacheName(), keySet);
	}

	public void flush(int durationMS) throws BasicException
	{
		m_cacheClient.flush(getCacheName(), durationMS);
	}
}
