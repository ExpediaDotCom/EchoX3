/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.provider;


import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;

public interface IObjectCacheProvider
{
	// Methods from the admin client
	void					connectToCache			(String cacheName) throws BasicException;
	void					close					(String cacheName);

	void					pushClassDefinition		(String cacheName, Class clazz);
	void					upgradeClass			(String cacheName, String classNameFrom, String factoryClassName)
																throws BasicException;
	Map<String, ManifestWrapper>	getVersion		()			throws BasicException;

	// Methods common to the cache client
	void					flush		(String cacheName, int durationMS) throws BasicException;
	void					writeOnly	(String cacheName, byte[] key, byte[] request) throws BasicException;
	void					writeOnly	(String cacheName, byte[][] keyList, byte[] request) throws BasicException;
	void					writeOnly	(String cacheName, byte[][] keyList, byte[][] requestList)
																throws BasicException;

	byte[]					readOnly	(String cacheName, byte[] key, byte[] request) throws BasicException;
	byte[][]				readOnly	(String cacheName, byte[][] keyList, byte[] request) throws BasicException;
	byte[][]				readOnly	(String cacheName, byte[][] keyList, byte[][] requestList)
																throws BasicException;

	byte[]					reduce		(String cacheName, byte[][] keyList, byte[] request);
}
