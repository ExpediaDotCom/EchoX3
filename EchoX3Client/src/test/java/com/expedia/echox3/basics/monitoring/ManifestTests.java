/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.time.WallClock;


public class ManifestTests extends AbstractTestTools
{
	@Test
	public void testList() throws UnknownHostException, BasicException
	{
		logTestName();

		Map<String, ManifestWrapper>		map		= ManifestWrapper.getManifestMap();

		assertNotNull(map);
		assertTrue(!map.isEmpty());

		for (Map.Entry<String, ManifestWrapper> mapEntry : map.entrySet())
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Manifest for " + mapEntry.getKey());

			ManifestWrapper		wrapper		= mapEntry.getValue();
			Manifest			manifest	= wrapper.getManifest();
			Attributes			attributes	= manifest.getMainAttributes();

			for (Map.Entry<Object, Object> attributeEntry : attributes.entrySet())
			{
				getLogger().info(BasicEvent.EVENT_TEST, "    %-25s = %s",
						attributeEntry.getKey().toString(), attributeEntry.getValue().toString());
			}
			getLogger().info(BasicEvent.EVENT_TEST,
					"    %-25s = %s", "BuildDate",
					WallClock.formatTime(WallClock.FormatType.DateTime, WallClock.FormatSize.Large,
							wrapper.getBuildTime()));
		}
	}
}
