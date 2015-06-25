/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.visible.application.simplecache;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.internal.provider.IObjectCacheProvider;
import com.expedia.echox3.internal.wrapper.TrellisBaseClient;
import com.expedia.echox3.visible.trellis.ISimpleCacheClient;

/**
 * Implementation of the EchoX3 interface ISimpleCacheClient, see ISimpleCacheClient for method details.
 *
 * Note that keys are never compressed.
 * This is to avoid the possibility of the same long key being serialized with different compression size threshold,
 * resulting in it being sometimes compressed and sometimes not compressed.
 */
public class SimpleCacheClient extends TrellisBaseClient implements ISimpleCacheClient
{
	public SimpleCacheClient(IObjectCacheProvider provider)
	{
		super(provider);
	}

	@Override
	public void put(String cacheName, Serializable key, Serializable value) throws BasicException
	{
		byte[]		keyBytes		= BasicSerial.toBytes(cacheName, key, BasicSerial.CompressType.none);
		byte[]		valueBytes		= BasicSerial.toBytes(cacheName, value);
		getProvider().writeOnly(cacheName, keyBytes, valueBytes);
	}

	@Override
	public void put(String cacheName, Serializable[] keyList, Serializable[] valueList) throws BasicException
	{
		byte[][]	keyBytesList		= BasicSerial.toBytesArray(cacheName, keyList, BasicSerial.CompressType.none);
		byte[][]	valueBytesList		= BasicSerial.toBytesArray(cacheName, valueList);
		getProvider().writeOnly(cacheName, keyBytesList, valueBytesList);
	}

	@Override
	public void put(String cacheName, Map<Serializable, Serializable> keyValueMap) throws BasicException
	{
		Serializable[]		keyList		= new Serializable[keyValueMap.size()];
		Serializable[]		valueList	= new Serializable[keyValueMap.size()];

		int					i			= 0;
		for (Map.Entry<Serializable, Serializable> entry : keyValueMap.entrySet())
		{
			keyList[i]		= entry.getKey();
			valueList[i]	= entry.getValue();
			i++;
		}
		put(cacheName, keyList, valueList);
	}

	@Override
	public Serializable get(String cacheName, Serializable key) throws BasicException
	{
		byte[]			keyBytes		= BasicSerial.toBytes(cacheName, key, BasicSerial.CompressType.none);
		byte[]			valueBytes		= getProvider().readOnly(cacheName, keyBytes, null);
		Serializable	value			= BasicSerial.toObject(cacheName, valueBytes);
		return value;
	}

	@Override
	public Serializable[] get(String cacheName, Serializable[] keyList) throws BasicException
	{
		byte[][]		keyBytesList	= BasicSerial.toBytesArray(cacheName, keyList, BasicSerial.CompressType.none);
		byte[][]		valueBytes		= getProvider().readOnly(cacheName, keyBytesList, "".getBytes());
		Serializable[]	valueList		= BasicSerial.toObjectArray(cacheName, valueBytes);
		return valueList;
	}

	@Override
	public Map<Serializable, Serializable> get(String cacheName, Collection<Serializable> keySet) throws BasicException
	{
		Serializable[]		keyList			= new Serializable[keySet.size()];
		int					i;

		i = 0;
		for (Serializable key : keySet)
		{
			keyList[i++] = key;
		}
		Serializable[]		valueList		= get(cacheName, keyList);

		Map<Serializable, Serializable>		map		= new HashMap<>();
		for (i = 0; i < keyList.length; i++)
		{
			map.put(keyList[i], valueList[i]);
		}
		return map;
	}

	@Override
	public void flush(String cacheName, int durationMS) throws BasicException
	{
		getProvider().flush(cacheName, durationMS);
	}
}
