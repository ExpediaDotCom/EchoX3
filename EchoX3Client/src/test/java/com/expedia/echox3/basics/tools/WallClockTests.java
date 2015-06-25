/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.time.BasicMasterTime;
import com.expedia.echox3.basics.tools.time.WallClock;

public class WallClockTests extends AbstractTestTools
{
	@Test
	public void testHello() throws BasicException
	{
		logTestName();

		long		offsetMS		= BasicMasterTime.measureOffsetMS(BasicMasterTime.MasterType.Internal);
		long		javaTimeMS		= System.currentTimeMillis();
		long		basicTimeMS		= WallClock.getCurrentTimeMS();
		long		deltaMS			= javaTimeMS - basicTimeMS;
		long		diff			= deltaMS - offsetMS;
		assertTrue(Math.abs(diff) < 1000);
	}

	@Test
	public void testFormatter()
	{
		logTestName();

		WallClock.FormatType[] formatTypeList = WallClock.FormatType.values();
		WallClock.FormatSize[] formatSizeList = WallClock.FormatSize.values();

		long						timeMS			= WallClock.getCurrentTimeMS();

		for (WallClock.FormatType formatType : formatTypeList)
		{
			for (WallClock.FormatSize formatSize : formatSizeList)
			{
				getLogger().info(BasicEvent.EVENT_TEST,
						"Time(%-8s, %-6s) = %-40s %-40s", formatType, formatSize,
								WallClock.formatTime(formatType, formatSize, timeMS),
								WallClock.formatTime(formatType, formatSize, timeMS + (12 * 60 * 60 * 1000))
				);
			}
		}
	}
}
