/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.io.Serializable;

import com.expedia.echox3.basics.monitoring.event.BasicException;

/**
 * The interface used by clients to communicate with the distributed objects.
 * An instance of this interface is obtained from the IClientFactory.
 * IAdminClient.connectToCache() must be called first to connect to the appropriate named cache.
 *
 * Note that the same client object is used to communicate to all named caches.
 */
public interface IObjectCacheClient
{
	//CHECKSTYLE:OFF

	/**
	 * Bread and butter call to modify the cache object.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param key					Object key, will be serialized, but never compressed
	 * @param request				As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void			writeOnly(String cacheName, Serializable   key,		Serializable   request) throws BasicException;

	/**
	 * As per basic writeOnly...
	 * The same request is sent to all objects.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Object key, will be serialized, but never compressed
	 * @param request				As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void			writeOnly(String cacheName, Serializable[] keyList,	Serializable   request) throws BasicException;

	/**
	 * As per basic writeOnly...
	 * A unique request is sent to all objects.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Object key, will be serialized, but never compressed
	 * @param requestList			As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	void			writeOnly(String cacheName, Serializable[] keyList,	Serializable[] requestList) throws BasicException;

	/**
	 * Bread and butter call to read from the cache object.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param key					Object key, will be serialized, but never compressed
	 * @param request				As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Serializable	readOnly(String cacheName, Serializable    key     ,	Serializable   request) throws BasicException;

	/**
	 * As per basic readOnly...
	 * The same request is sent to all objects.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Object key, will be serialized, but never compressed
	 * @param request				As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Serializable[]	readOnly(String cacheName, Serializable    keyList[],	Serializable   request) throws BasicException;

	/**
	 * As per basic readOnly...
	 * A unique request is sent to all objects.
	 *
	 * @param cacheName				The name of the cache of interest (aka first part of the key)
	 * @param keyList				Object key, will be serialized, but never compressed
	 * @param requestList			As per User contract, this is the request from the client's call
	 * @throws BasicException		Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	Serializable[]	readOnly(String cacheName, Serializable    keyList[],	Serializable[] requestList) throws BasicException;

	void			flush(   String cacheName, int durationMS) throws BasicException;

	Serializable	reduce(Class<ITrellisReducer> reducerClass,
						     String cacheName, Serializable    keyList[],	Serializable   request) throws BasicException;
	//CHECKSTYLE:ON


	/**
	 * This interface is implemented by the USER class used in IObjectCacheClient.reduce().
	 * A singleton is implemented on each server and one on the client.
	 * Each server singleton is called (reduceObjectList) with the list of objects on that server.
	 * From this, each server produces a single response.
	 * The client singleton receives the list of answers (in any order).
	 * From this list, the client singleton produces the final answer.
	 */
	interface ITrellisReducer
	{
		/**
		 * Runs on each server and produces a single answer, based on the request, from a list of objects.
		 *
		 * @param objectList	List of objects to be processed
		 * @param request		Request to apply to the list of object
		 * @return				Single answer from the reduce function
		 */
		Serializable	reduceObjectList(ICacheObject[] objectList, Serializable request);

		/**
		 * Runs on the client and reduces the list of answers (one from each server)
		 * into a single (fully reduced) answer for the caller.
		 *
		 * @param answerList	List of the answer from each servers (result of reduceObjectList)
		 * @param request		The original request
		 * @return				The final single answer of the reduction
		 */
		Serializable	reduceAnswerList(Serializable[] answerList,        Serializable request);
	}
}
