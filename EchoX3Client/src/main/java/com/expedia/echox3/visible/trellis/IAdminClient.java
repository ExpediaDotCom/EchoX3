/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.util.Map;

import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;

public interface IAdminClient
{
	/**
	 * This is the first call, to establish the communication with the named cache.
	 * Under the covers (i.e. within EchoX3 internal code), the work done is different
	 * depending on Local or Remote mode.
	 * However, the call is the same in Local or Remote mode for the client.
	 * NOTE: In Local mode, IClientFactory.putLocalConfiguration() must be called first.
	 *
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 * @throws BasicException	All sorts of things can go wrong...
	 */
	void		connectToCache(String cacheName)	throws BasicException;

	/**
	 * Tells the system the connection to a named cache is no longer required.
	 * In Local mode, the local cache is deleted.
	 *
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 */
	void		close(String cacheName);

	/**
	 * Called to manually trigger a push of a top level class definition.
	 * In the normal case, this is done  automatically on an as-needed basis, transparently to the caller.
	 * This may be needed when using SimpleCache, to enable content viewer of the object content.
	 * You may also choose to make this call at startup to push a particularly complex class.
	 *
	 * Note that each named cache has its own class loader.
	 *
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 * @param clazz				The class object to push (the class loader is picked-up from this object)
	 * @throws BasicException	All sorts of things can go wrong...
	 */
	void		pushClassDefinition(String cacheName, Class clazz) throws BasicException;

	/**
	 * Request to upgrade all objects within cacheName of classFrom to new objects
	 * created using objectFactoryClass.
	 *
	 * Note that each named cache has its own class loader.
	 *
	 * @param cacheName				Standard name of the cache (Object or Simple) of interest
	 * @param classFrom				The object class of the objects to upgrade
	 * @param objectFactoryClass	The class factory to use to create the new objects
	 * @throws BasicException		All sorts of things can go wrong (e.g. communication, bad objectFactoryClass)
	 */
	void		upgradeClass(String cacheName, String classFrom, Class objectFactoryClass) throws BasicException;

	/**
	 * Loads the manifest file from the most important libraries (jar) used by the client.
	 * For example, the JDK jars are excluded (there is a specific method to obtain the JDK version),
	 * while log4j is included.
	 *
	 * @return		A ManifestWrapper object containing, among other, a map of <JarFileName, Manifest>.
	 */
	Map<String, ManifestWrapper>	getVersion() throws BasicException;
}
