/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.wrapper;

import java.io.Serializable;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.BasicSerial.CompressType;
import com.expedia.echox3.internal.provider.IObjectCacheProvider;
import com.expedia.echox3.visible.trellis.IObjectCacheClient;

public class ObjectCacheClient extends TrellisBaseClient implements IObjectCacheClient
{
	public ObjectCacheClient(IObjectCacheProvider provider)
	{
		super(provider);
	}

	@Override
	public void writeOnly(String cacheName, Serializable key, Serializable request)
			throws BasicException
	{
		byte[]		keyBytes		= BasicSerial.toBytes(cacheName, key, CompressType.none);
		byte[]		requestBytes	= BasicSerial.toBytes(cacheName, request);
		getProvider().writeOnly(cacheName, keyBytes, requestBytes);
	}

	@Override
	public void writeOnly(String cacheName, Serializable[] keyList, Serializable request)
			throws BasicException
	{
		byte[][]	keyListBytes	= BasicSerial.toBytesArray(cacheName, keyList, CompressType.none);
		byte[]		requestBytes	= BasicSerial.toBytes(cacheName, request);
		getProvider().writeOnly(cacheName, keyListBytes, requestBytes);
	}

	@Override
	public void writeOnly(String cacheName, Serializable[] keyList, Serializable[] requestList)
			throws BasicException
	{
		byte[][]	keyListBytes		= BasicSerial.toBytesArray(cacheName, keyList, CompressType.none);
		byte[][]	requestListBytes	= BasicSerial.toBytesArray(cacheName, requestList);
		getProvider().writeOnly(cacheName, keyListBytes, requestListBytes);
	}

	@Override
	public Serializable readOnly(String cacheName, Serializable key, Serializable request)
			throws BasicException
	{
		byte[]			keyBytes		= BasicSerial.toBytes(cacheName, key, CompressType.none);
		byte[]			requestBytes	= BasicSerial.toBytes(cacheName, request);
		byte[]			responseBytes	= getProvider().readOnly(cacheName, keyBytes, requestBytes);
		Serializable	response		= BasicSerial.toObject(cacheName, responseBytes);
		return response;
	}

	@Override
	public Serializable[] readOnly(String cacheName, Serializable[] keyList, Serializable request)
			throws BasicException
	{
		byte[][]		keyListBytes		= BasicSerial.toBytesArray(cacheName, keyList, CompressType.none);
		byte[]			requestBytes		= BasicSerial.toBytes(cacheName, request);
		byte[][]		responseListBytes	= getProvider().readOnly(cacheName, keyListBytes, requestBytes);
		Serializable[]	responseList		= BasicSerial.toObjectArray(cacheName, responseListBytes);
		return responseList;
	}

	@Override
	public Serializable[] readOnly(String cacheName, Serializable[] keyList, Serializable[] requestList)
			throws BasicException
	{
		byte[][]	keyListBytes		= BasicSerial.toBytesArray(cacheName, keyList, CompressType.none);
		byte[][]	requestListBytes	= BasicSerial.toBytesArray(cacheName, requestList);
		byte[][]	responseListBytes	= getProvider().readOnly(cacheName, keyListBytes, requestListBytes);
		Serializable[]	responseList	= BasicSerial.toObjectArray(cacheName, responseListBytes);
		return responseList;
	}

	@Override
	public void flush(String cacheName, int durationMS)
			throws BasicException
	{
		getProvider().flush(cacheName, durationMS);
	}

	@Override
	public Serializable reduce(Class<ITrellisReducer> reducerClass,
			String cacheName, Serializable[] keyList, Serializable request)
			throws BasicException
	{
		throw new UnsupportedOperationException("NYI");
	}
}
