/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.visible.ioc;

import java.io.Serializable;
import java.net.URL;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.trellis.IClientFactory.ClientType;
import com.expedia.echox3.visible.trellis.IObjectCacheClient;

/**
 * Implements virtually the same methods as IObjectCacheClient,
 * but within the context of IocCacheClient.
 * See IocCacheClient for usage of initialization methods of the IOC wrapper;
 * See IObjectCacheClient for usage of the cache methods.
 */
public class IocObjectCacheClient extends IocCacheClient
{
	private IObjectCacheClient m_cacheClient;

	public IocObjectCacheClient()
	{
	}

	public IocObjectCacheClient(ClientType clientType, String cacheName, URL url) throws BasicException
	{
		super(clientType, cacheName, url);
	}

	@Override
	protected void update() throws BasicException
	{
		m_cacheClient = getFactory().getObjectClient(getClientType());
	}

	public void writeOnly(Serializable key, Serializable request) throws BasicException
	{
		m_cacheClient.writeOnly(getCacheName(), key, request);
	}

	public void writeOnly(Serializable[] keyList, Serializable request) throws BasicException
	{
		m_cacheClient.writeOnly(getCacheName(), keyList, request);
	}

	public void writeOnly(Serializable[] keyList, Serializable[] requestList) throws BasicException
	{
		m_cacheClient.writeOnly(getCacheName(), keyList, requestList);
	}

	public Serializable readOnly(Serializable key, Serializable request) throws BasicException
	{
		return m_cacheClient.readOnly(getCacheName(), key, request);
	}

	public Serializable[] readOnly(Serializable[] keyList, Serializable request) throws BasicException
	{
		return m_cacheClient.readOnly(getCacheName(), keyList, request);
	}

	public Serializable[] readOnly(Serializable[] keyList, Serializable[] requestList) throws BasicException
	{
		return m_cacheClient.readOnly(getCacheName(), keyList, requestList);
	}

	public void flush(int durationMS) throws BasicException
	{
		m_cacheClient.flush(getCacheName(), durationMS);
	}

	public Serializable reduce(Class<IObjectCacheClient.ITrellisReducer> reducerClass,
			Serializable[] keyList, Serializable request) throws BasicException
	{
		return m_cacheClient.reduce(reducerClass, getCacheName(), keyList, request);
	}
}
