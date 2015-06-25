/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.configuration;


import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.ConfigurationManager.SelfTuningInteger;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;

public class ConfigurationTests extends AbstractTestTools
{
	@Before
	public void beforeTest()
	{
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::receiveEvent);
	}
/*
	@Test
	public void testConfig()
	{
		logTestName();

		// Manually insert a change and validate the log shows the change.
		// There are three types of changes: Added, Removed and Modified
		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this);
		BasicTools.wait(1000);
		PublisherManager.deregister(ConfigurationManager.PUBLISHER_NAME, this);
	}
*/
		@Test
	public void testSettings() throws Exception
	{
		logTestName();

		assertSame(123, ConfigurationManager.getInstance().getInt("test.int.123", null));

		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.true1", null));
		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.true2", null));
		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.true3", null));

		assertFalse(ConfigurationManager.getInstance().getBoolean("test.boolean.false1", null));
		assertFalse(ConfigurationManager.getInstance().getBoolean("test.boolean.false2", null));
		assertFalse(ConfigurationManager.getInstance().getBoolean("test.boolean.false3", null));

		Throwable throwable		= null;
		try
		{
			ConfigurationManager.getInstance().getInt("test.int.exception", null);
		}
		catch (Throwable t)
		{
			throwable = t;
		}
		assertNotNull(throwable);
	}

	public void receiveEvent(String name, long timeMS, Object event)
	{
		getLogger().info(BasicEvent.EVENT_TEST, "Received a change notification from ConfigurationManager.");
	}

	@Test
	public void testInheritance()
	{
		logTestName();

		// Inheritance is through the "package name", keeping the setting name.
		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.true1", null));
		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.Foo.true1", null));
		assertTrue(ConfigurationManager.getInstance().getBoolean("test.boolean.Foo.Bar.FB.true1", null));
		assertNull(ConfigurationManager.getInstance().getSetting("test.boolean.true1.Boo", null));
	}

	@Test
	public void testMemoryProvider()
	{
		String							testName	= logTestName();

		ConfigurationManager			manager		= ConfigurationManager.getInstance();
		MemoryConfigurationProvider		provider	= new MemoryConfigurationProvider(testName);
		Map<String, String>				map1		= new HashMap<>();
		Map<String, String>				map2		= new HashMap<>();
//		Map<String, String>				map3		= new HashMap<>();

		String							name1		= "Name1";
		String							name2		= "Name2";
		String							name3		= "Name3";
		String							name4		= "Name4";
		String							name5		= "Name5";

		map1.put(name1, name1);
		map1.put(name2, name2);
		map1.put(name3, name3);
		map2.put(name4, name4);
		map2.put(name5, name5);

		provider.setSettingMap(map1);
		assertNotNull(manager.getSetting(name1, null));
		assertNotNull(manager.getSetting(name2, null));
		assertNotNull(manager.getSetting(name3, null));
		assertNull(manager.getSetting(name4, null));
		assertNull(manager.getSetting(name5, null));

		provider.addSettingMap(map2);
		assertNotNull(manager.getSetting(name1, null));
		assertNotNull(manager.getSetting(name2, null));
		assertNotNull(manager.getSetting(name3, null));
		assertNotNull(manager.getSetting(name4, null));
		assertNotNull(manager.getSetting(name5, null));

		provider.setSettingMap(map2);
		assertNull(manager.getSetting(name1, null));
		assertNull(manager.getSetting(name2, null));
		assertNull(manager.getSetting(name3, null));
		assertNotNull(manager.getSetting(name4, null));
		assertNotNull(manager.getSetting(name5, null));
	}

	@Test
	public void testSelfTuning()
	{
		String							testName		= logTestName();
		MemoryConfigurationProvider		provider		= new MemoryConfigurationProvider(testName);
		ConfigurationManager.getInstance().addProvider(provider);

		String							prefix		= testName;
		SelfTuningInteger				self		= new ConfigurationManager.SelfTuningInteger(prefix);
		String		starting	= String.format("%s%s", prefix, ConfigurationManager.SETTING_NAME_STARTING_POINT);
		String		cores		= String.format("%s%s", prefix, ConfigurationManager.SETTING_NAME_CORE_PER_INCREMENT);

		validateSelfTuning(self, provider, starting, cores, 3, 0);
		validateSelfTuning(self, provider, starting, cores, 3, 1);
		validateSelfTuning(self, provider, starting, cores, 3, 4);
		validateSelfTuning(self, provider, starting, cores, 3, 8);
		validateSelfTuning(self, provider, starting, cores, 3, 9);

		ConfigurationManager.getInstance().removeProvider(provider);
	}
	private void validateSelfTuning(SelfTuningInteger self, MemoryConfigurationProvider provider,
			String startingName, String coresName, int startingValue, int coresValue)
	{
		provider.addSetting(startingName, Integer.toString(startingValue));
		provider.addSetting(coresName, Integer.toString(coresValue));

		// No need to wait, as updateConfiguration() is called directly
		self.updateConfiguration();

		int				expectedValue		=
				startingValue + (0 == coresValue ? 0 : BasicTools.getNumberOfProcessors() / coresValue);
		assertEquals(
				String.format("(%,d, %,d) to Self = %s", startingValue, coresValue, self.toString()),
				expectedValue, self.getCurrentValue());
	}

}
