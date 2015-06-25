/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.io.Serializable;

/**
 * Interface to be implemented by the User object stored in the object cache (on the server).
 * These objects are created by the User's IObjectFactory.
 * The IObjectFactory is responsible to give the object the configuration at construction time.
 * After construction, the object's updateConfiguration() will be called if the configuration has changed.
 */
public interface ICacheObject extends Serializable
{
	/**
	 * Called when the configuration has changed (aka hot configuration).
	 *
	 * @param configuration		The newly (probably) changed configuration object
	 */
	void			updateConfiguration(ObjectCacheConfiguration configuration);

	/**
	 * Client request to modify the object.
	 * This call is executed on all copies of the object.
	 *
	 * @param request	As per User contract, this is the request from the client's call
	 */
	void			writeOnly(Serializable request);

	/**
	 * Client request to extract data from the object; this call should NOT modify the object.
	 * This call is executed only on one copy object.
	 *
	 * @param request	As per User contract, this is the request from the client's call
	 */
	Serializable	readOnly(Serializable request);

	/**
	 * Request the size of the object, in the same units as SizeUnits and SizeMax in the cache configuration file.
	 *
	 * @return		The size, in the appropriate units (e.g. bytes, entries).
	 */
	long			getSize();

	/**
	 * Called periodically at interval MaintenancePeriod (see configuration).
	 * This call is where the object does its housekeeping.
	 * For example, an object could free memory pointing to items no longer required (expired).
	 * Upon completion, canDelete() will be called to see if the object is still required.
	 *
	 * When the system is under stress, the parameter memoryLevelPercent will be below 100.
	 * This is an attempt by the system to protect itself gracefully by using eviction.
	 * Eviction is the act of removing items from the "cache" earlier than it would normally.
	 *
	 * Under these circumstances, the object should attempt to reduce its memory usage proportionally.
	 * For example, if the data in an object is distributed fairly evenly over time such as prices by date
	 * for 1 day from now, 2 days from now ... up to 100 days from now, with the closer times being more important...
	 * The application could choose to keep only the memoryLevelPercent days.
	 * At memoryLevelPercent = 85, it would keep 85.
	 *
	 * In another example, if the application was a simple cache, with a TTL (time to live) of 90 minutes
	 * and memoryLevelPercent was at 85, the application could artificially modify the TTL to 90 * 85% = 76.5 minutes.
	 *
	 * Of course, an application can choose to ignore the parameter memoryLevelPercent. However, bear in mind
	 * that EchoX3 will protect itself and, when memory utilization on the storage servers reaches a threshold
	 * (approximately 87% heap utilization), all write requests will result in an OOM response.
	 *
	 * @param timeNowMS				The current time, to avoid each object having to check for the time.
	 *                              Note that EchoX3 manages the time synchronized to an external master source,
	 *                              maintaining an offset to the local clock.
	 *                              This helps ensure that all servers in a farm have their clock in sync.
	 * @param memoryLevelPercent	100 for normal or lower to indicate the system is under memory pressure
	 */
	void			doMaintenance(long timeNowMS, int memoryLevelPercent);

	/**
	 * Called frequently (e.g. after doMaintenance, writeOnly) to ask the object if it can be deleted from the cache.
	 * As this call is frequent, it should be very fast.
	 *
	 * @return		True indicates the object can be deleted; false indicates the object is still live.
	 */
	boolean			canDelete();

	/**
	 * On the surface, flush is simple, delete everything. However, EchoX3 is more powerful...
	 * EchoX3 supports a soft flush. This is a flush that is spread over some period of time...
	 * to avoid a burst of load on the backend.
	 * The client issuing the flush specifies a duration for the flush (e.g. 5 minutes) and
	 * EchoX3 will uniformly distribute the flush of each item over the duration.
	 * For example, if the flush duration is 5 minutes (300,000 ms) and there are 3,000,000 items,
	 * the flush will be distributed as approximately 10 items per millisecond.
	 *
	 * Within a EchoX3 server, the flush executes immediately
	 * and each object is told the time at which it should flush.
	 * It is up to the implementation to determine the best approach to achieve the goal,
	 * depending on the particulars of the object.
	 *
	 * @param timeNowMS			The time where the flush begins (i.e. now)
	 * @param timeFlushMS		The time at which the object should flush itself
	 */
	void			flush(long timeNowMS, long timeFlushMS);

	/**
	 * Called in response to a call to IAdminClient.upgradeClass().
	 * The newly created object (this object) is asked to populate itself from the data
	 * in the object it is replacing. The object it is replacing is the parameter.
	 * This is happening under lock.
	 * Upon completion, this object will replace the object passed as a parameter
	 * which itself will be deleted.
	 *
	 * @param cacheObject	The previous (older) cache object presumably containing the data to be stolen.
	 */
	void			upgradeFrom(ICacheObject cacheObject);
}
