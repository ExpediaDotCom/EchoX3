/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */

package com.expedia.echox3.basics.monitoring.time;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Pierre Cote
 *
 * List of UDP/TCP reserved ports  http://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers
 * NIST EventTime servers are listed on http://tf.nist.gov/tf-cgi/servers.cgi
 * The return format is as per     http://www.nist.gov/pml/div688/grp40/its.cfm
 * EventTime = 55530 10-11-30 03:18:55 00 0 0 809.8 UTC(NIST) *
 *
 * 		offset = LocalTime - MasterTime
 *
 */
public class InternetMasterTime extends BasicMasterTime
{
	// public so test cases know how long to sleep between calls.
	// production code should wait much longer between calls (e.g. hours)
	public static final int			TIME_DELTA_MIN_MS			= 60 * 1000;		// As per NIST, paranoid mode
	private static final Random		RANDOM						= new Random();

	private static final InternetMasterTime			INSTANCE		= new InternetMasterTime();

	private Map<String, Long>		m_lastUsedTimeMap			= new HashMap<>();

	private InternetMasterTime()
	{
		super(MasterType.InternetNist);
	}

	public static InternetMasterTime getInstance()
	{
		return INSTANCE;
	}

	@Override
	public String getHostName(int attemptNumber)
	{
		String[]		hostList		= getHostList();
		long			time			= System.currentTimeMillis();
		int				count			= hostList.length;
		for (int i = 0; i < count; i++)		// Only so many tries. This may be called multiple times.
		{
			int			index			= RANDOM.nextInt(count);
			String		hostName		= hostList[index];
			Long		lastUsed		= m_lastUsedTimeMap.get(hostName);
			lastUsed = null == lastUsed ? 0 : lastUsed;
			long		timeSinceMS		= time - lastUsed;
			if (TIME_DELTA_MIN_MS < timeSinceMS)
			{
				m_lastUsedTimeMap.put(hostName, time);
				return hostName;
			}
		}
		return null;
	}

	public static InternetMasterTime measure()
	{
		InternetMasterTime		masterTime		= new InternetMasterTime();
		masterTime.measureInternal();
		return masterTime;
	}
}
