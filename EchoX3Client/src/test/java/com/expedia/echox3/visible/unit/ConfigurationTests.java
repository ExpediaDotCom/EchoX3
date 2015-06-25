/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.unit;

import java.net.URL;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.FileConfigurationProvider;
import com.expedia.echox3.basics.configuration.IConfigurationProvider;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class ConfigurationTests extends AbstractTestTools
{
	@Test
	public void testSimpleConfiguration() throws Exception
	{
		String							filename			= "data/TestSimple.ObjectCache.properties";
		URL								url					= FileFinder.findUrlOnClasspath(filename);
		assertNotNull(url);
		IConfigurationProvider			provider			= new FileConfigurationProvider(url);

		ObjectCacheConfiguration configuration		= new ObjectCacheConfiguration("Test1", provider);

		String							settingName			= "MaintenancePeriodNumber";
		int								asInt				= configuration.getSettingAsInteger(settingName, -1);
		long							asLong				= configuration.getSettingAsLong(settingName, -1);
		double							asDouble			= configuration.getSettingAsDouble(settingName, -1);

		assertTrue(asInt == asLong);
		assertTrue(Math.abs(asInt - asDouble) < 0.1);
	}
}
