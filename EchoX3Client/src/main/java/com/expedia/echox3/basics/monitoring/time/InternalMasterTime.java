/**
 * Copyright 2012-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.time;


/**
 * Wraps the multiple Internal addresses into a single class that finds one amongst the various available.
 *
 * This is a utility short-cut class on top of the SntpClient who does the real work.
 *
 * 		offset = LocalTime - MasterTime
 */
public class InternalMasterTime extends BasicMasterTime
{
	private static final InternalMasterTime			INSTANCE		= new InternalMasterTime();

	private InternalMasterTime()
	{
		super(MasterType.Internal);
	}

	public static InternalMasterTime getInstance()
	{
		return INSTANCE;
	}

	public static InternalMasterTime measure()
	{
		InternalMasterTime masterTime = new InternalMasterTime();
		masterTime.measureInternal();
		return masterTime;
	}

	public String getHostName(int attemptNumber)
	{
		if (getHostList().length > attemptNumber)
		{
			return getHostList()[0];
		}
		else
		{
			return null;
		}
	}
}
