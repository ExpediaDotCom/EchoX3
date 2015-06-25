/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.internal.wrapper;

import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.internal.provider.IObjectCacheProvider;
import com.expedia.echox3.visible.trellis.IAdminClient;

public class AdminClient extends TrellisBaseClient implements IAdminClient
{
	public AdminClient(IObjectCacheProvider provider)
	{
		super(provider);
	}

	@Override
	public void connectToCache(String cacheName) throws BasicException
	{
		getProvider().connectToCache(cacheName);
	}

	@Override
	public void close(String cacheName)
	{
		getProvider().close(cacheName);
	}

	@Override
	public void pushClassDefinition(String cacheName, Class clazz) throws BasicException
	{
		getProvider().pushClassDefinition(cacheName, clazz);
	}

	@Override
	public void upgradeClass(String cacheName, String classFrom, Class objectFactoryClass) throws BasicException
	{
		getProvider().upgradeClass(cacheName, classFrom, objectFactoryClass.getName());
	}

	/**
	 * Loads the manifest file from the most important libraries (jar) used by the client.
	 * For example, the JDK jars are excluded (there is a specific method to obtain the JDK version),
	 * while log4j is included.
	 *
	 * @return A ManifestWrapper object containing, among other, a map of <JarFileName, Manifest>.
	 */
	@Override
	public Map<String, ManifestWrapper> getVersion() throws BasicException
	{
		return getProvider().getVersion();
	}
}
