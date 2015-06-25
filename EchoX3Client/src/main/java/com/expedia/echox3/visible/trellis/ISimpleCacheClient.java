/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;

/**
 * This is the interface for the SimpleCache application running on top of Object mode.
 * The SimpleCache application is effectively a cache, with put/get methods and standard expiration time.
 * The  cache is configured via the ObjectCacheConfiguration, via the properties...
 *		# Time to live (TTL)
 *		# Time units can be (case insensitive)
 *		# ms, millisecond, milli, sec, second, min, minute, hour, hr, day, week, year
 *		TTLNumber=2
 *		TTLUnits=Minute
 *		# TTLType can be
 *		#		TimeWrite	Expiration clock starts on write (reset on update)
 *		#		TimeRead	Expiration clock is reset on each read
 *		TTLType=TimeWrite
 */
public interface ISimpleCacheClient
{
	/**
	 * Writes a value to the named cache.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param key					Object key, will be serialized, but never compressed
	 * @param value					Value is serialized and (if not already a byte[]) compressed if < threshold (~2KB)
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void				put(String cacheName, Serializable   key,		Serializable   value) throws BasicException;

	/**
	 * Similar to put, but with multiple key/value pairs for efficient write.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Array of keys
	 * @param valueList				Array of values matching 1:1 the keyList
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void				put(String cacheName, Serializable[] keyList,	Serializable[] valueList) throws BasicException;

	/**
	 * Similar to put, taking a Map<> object. The call is mapped to put(cacheName, keyList, valueList)
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyValueMap			Map of <Key, Value> pairs.
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void				put(String cacheName, Map<Serializable, Serializable> keyValueMap) throws BasicException;

	/**
	 * Retrieves a value from the named cache
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param key					Key of the object to recover
	 * @return						The de-serialized object, in exactly the form in which it was writer.
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Serializable		get(String cacheName, Serializable             key) throws BasicException;

	/**
	 * Retrieves the values for the keys in the list.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Array of keys for which the value is requested. Duplicates are allowed.
	 * @return						The de-serialized object, in exactly the form in which it was writer.
	 *								The index of the elements in the array will match exactly the index of the keys.
	 *								Not found items will contain null.
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Serializable[]		get(String cacheName, Serializable             keyList[]) throws BasicException;

	/**
	 * Retrieves the values corresponding to the keys in the keySet.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keySet				Set of keys, the matching values will be retrieved from the cache.
	 * @return						A Map of <Key, Value> pairs for the requested set of keys.
	 * 								Keys for which no values were found will return null = map.get("NotFound");
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Map<Serializable, Serializable>
						get(String cacheName, Collection<Serializable> keySet) throws BasicException;

	/**
	 * Request to perform a soft flush for the named cache.
	 * The flush of the items will be spread uniformly over the duration, on a per server basis.
	 * If the number of items is very small compared to the duration, the flush may appear jerky.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param durationMS			Duration over which to spread the flush.
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void				flush(String cacheName, int durationMS) throws BasicException;
}
