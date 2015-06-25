/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import com.expedia.echox3.basics.monitoring.event.BasicException;

/**
 * ReadONly APIs to query the status of the system.
 */
public interface IMonitorClient
{
	/**
	 * Give a chance to the Monitor client to close any pending connections.
	 *
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 * @throws BasicException	All sorts of things can go wrong...
	 */
	void		close(String cacheName) throws BasicException;

}
