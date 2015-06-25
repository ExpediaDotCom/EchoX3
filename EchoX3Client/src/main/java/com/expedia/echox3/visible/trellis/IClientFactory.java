/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.net.URL;

/**
 * This is the first set of entry points used by clients.
 * These entry points are used to access the Trellis client objects.
 *
 * Note that, apart from the ClientType parameter in the entry points in the factory class,
 * Local and Remote mode have identical interfaces and behave identically for the user.
 *
 */
public interface IClientFactory
{
	/**
	 * Determines the mode of operation of the client requested.
	 */
	enum ClientType
	{
		NotSet,
		Local,
		Remote
	}

	/**
	 * Required only in Local mode, but it is recommended that this be used always.
	 * It has no effect in Remote mode.
	 * This maps the a named cache to its (hot) configuration file.
	 * As a reminder, the configuration file has the following settings:
	 *
	 * 		# Mandatory settings
	 *		FactoryClassName=[Fully qualified class name of the class used to create empty cache objects]
	 *							i.e. The objects implementing the interface ICacheObject
	 *		MaintenancePeriodNumber=15
	 *		MaintenancePeriodUnits=Sec
	 *		# Time units can be (case insensitive)
	 *		# ms, millisecond, milli, sec, second, min, minute, hour, hr, day, week, year
	 *
	 *		# Optional settings
	 *		# Size is specified in any units practical for the user's application.
	 *		# Size must be in the same units as ICacheObject.getSize().
	 *		# SizeUnits is used for display (JMX, logging).
	 *		SizeMax=6,000,000
	 *		SizeUnits=bytes
	 *
	 * @param cacheName						The name of the cache to configure
	 * @param configurationFileAddress		A valid URL
	 *                                         e.g.		http://MachineName/path/FileName.properties
	 *                                         Mac		file:/folder/Filename.properties
	 *                                         Win		file:/TBD
	 * @return								True if the map was modified by this call
	 */
	boolean							putLocalConfiguration(String cacheName, URL configurationFileAddress);

	/**
	 * Removes a mapping written by putLocalConfiguration()
	 * Seldom used, and no-op if no mapping exists.
	 * Note that this will NOT delete the cache.
	 * An existing cache using a removed configuration stop updating its configuration.
	 *
	 * @param cacheName						The name of the cache to clear
	 * @return								True if the map was modified by this call
	 */
	boolean							removeLocalConfiguration(String cacheName);

	/**
	 * Called to obtain a SimpleCache client (put/get mode)
	 *
	 * @param clientType		Local or Remote mode
	 * @return					The requested ISimpleCacheClient client
	 */
	ISimpleCacheClient getSimpleClient(ClientType clientType);

	/**
	 * Called to obtain a ObjectCache client (read/write mode)
	 *
	 * @param clientType		Local or Remote mode
	 * @return					The requested IObjectCacheClient client
	 */
	IObjectCacheClient getObjectClient(ClientType clientType);

	/**
	 * Called to obtain a Admin client (maintenance API)
	 *
	 * @param clientType		Local or Remote mode
	 * @return					The requested IAdminClient client
	 */
	IAdminClient getAdminClient(ClientType clientType);

	/**
	 * Called to obtain a Monitor client (Read-only monitoring entry points)
	 *
	 * @param clientType		Local or Remote mode
	 * @return					The requested IMonitorClient client
	 */
	IMonitorClient getMonitorClient(ClientType clientType);
}
