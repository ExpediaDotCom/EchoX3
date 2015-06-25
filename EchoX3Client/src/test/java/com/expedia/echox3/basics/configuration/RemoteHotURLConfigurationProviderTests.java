/**
 * Copyright 2011 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.configuration;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

public class RemoteHotURLConfigurationProviderTests extends AbstractTestTools
{

	private final static String SETTING_URL								=
			RemoteHotUrlConfigurationProvider.SETTING_PREFIX_URL;
	private final static String SETTING_URL_SUFFIX						= "test";
	private final static String SETTING_URL_SUFFIX1						= "test1";

	//file names are sorted in the properties file
	private final static String SETTING_URL_FILENAME1					= "config/test.manualurl.properties";
	private final static String SETTING_URL_FILENAME2					= "config/test.manualurl1.properties";
	private final static String SETTING_URL_NONEXISTENT_FILENAME		= "config/nonExistentFile.properties";

	private final static String SETTING_URL_OVERRIDE					= "test.string.urlOverride.test";
	private final static String SETTING_BASIC							= "test.int.123";
	private final static String SETTING_BASIC_OLD_VALUE					= "123";
	private final static String SETTING_BASIC_NEW_VALUE					= "ABC";
	private final static String SETTING_MULTIPLE_URL_TEST				= "test.string.multipleurl.test";
	private final static String SETTING_OLD_VALUE						= "oldvalue";
	private final static String SETTING_NEW_VALUE						= "newvalue";
	private final static String SETTING_VALUE_FOUND						= "found";

	// TC means Test configuration
	private	final	static	short		TCP_ADDED								= 1;
	private	final	static	short		TCP_REMOVED								= 2;
	private	final	static	int			WAIT_ITERATIONS							= 10000;
	private	final	static	int			SLEEP_TIME								= 1;

	// rhucn means Remote Hot Url Configuration Notification
	private	volatile		int			m_rhucnCounter							= 0;
	private volatile Boolean m_rhucnReceived							= false;
	// tcn means test configuration notification
	private	volatile		short						m_tcnEvent				= 0;
	private volatile		MemoryConfigurationProvider	m_provider				= null;

	@BeforeClass
	public static void beforeClass()
	{
		ConfigurationManager.getInstance();
	}

	private void initializeTest() throws Exception
	{
		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::receiveEvent);
		m_provider = new MemoryConfigurationProvider("Test");

		// This is where the URL cache files will go; files in that directory will not be cleaned up by the tests
		String			folderName		= System.getProperty("java.io.tmpdir");
		m_provider.addSetting(BaseFileHandler.SETTING_NAME_WORKING_FOLDER, folderName);

		waitForTestConfigurationNotification(TCP_ADDED);
		m_rhucnCounter = 0;
	}

	@After
	public void closeProvider() throws Exception
	{
		m_provider.close();
		assertTrue("Test configuration provider not removed", waitForTestConfigurationNotification(TCP_REMOVED));
	}

	private boolean waitForTestConfigurationNotification(short state) throws Exception
	{
		for (int i = 0; i < WAIT_ITERATIONS && m_tcnEvent != state; i++)
		{
			Thread.sleep(SLEEP_TIME);
		}
		return (m_tcnEvent == state);
	}

	@Test
	public void testSettings() throws Exception
	{
		logTestName();

		initializeTest();

		/* Give a url and check if its config settings are loaded. */
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX, SETTING_URL_FILENAME1, SETTING_BASIC,
				SETTING_BASIC_OLD_VALUE, SETTING_BASIC_NEW_VALUE, "load url config test failed");

		/* test the functionality to replace url. */
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX, SETTING_URL_FILENAME2, SETTING_URL_OVERRIDE,
				SETTING_OLD_VALUE, SETTING_NEW_VALUE, "override url test failed");

		assertEquals(2, m_rhucnCounter);
	}

	@Test
	public void testLoadFromCacheFile() throws Exception
	{
		logTestName();

		initializeTest();

		URL sourceUrl		= FileFinder.findUrlOnClasspath(SETTING_URL_FILENAME1);
		String sourceUrlString	= sourceUrl.toString();
		URI sourceUri		= new URI(sourceUrlString);
		Path sourcePath		= Paths.get(sourceUri);

		String destUrlString	= sourceUrlString.replaceAll(SETTING_URL_FILENAME1, SETTING_URL_NONEXISTENT_FILENAME);
		URI destUri			= new URI(destUrlString);
		Path destPath		= Paths.get(destUri);

		Files.deleteIfExists(destPath);
		Files.copy(sourcePath, destPath);

		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX, SETTING_URL_NONEXISTENT_FILENAME, SETTING_BASIC,
				SETTING_BASIC_OLD_VALUE, SETTING_BASIC_NEW_VALUE, "load url from cache file test failed (initial)");
		assertTrue("URL file should exist", Files.deleteIfExists(destPath));

		RemoteHotUrlConfigurationProvider 		instance	= RemoteHotUrlConfigurationProvider.getInstance();
		Map<? super Comparable<?>, FileConfigurationProvider>	providerMap	= instance.getProviderMap();
		providerMap.remove(SETTING_URL + SETTING_URL_SUFFIX); // mimic "new load"; will read from cache (destPath gone)
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX, destUrlString, SETTING_BASIC,
				SETTING_BASIC_OLD_VALUE, SETTING_BASIC_NEW_VALUE, "load url from cache file test failed (cache)");
		assertTrue(providerMap.containsKey(SETTING_URL + SETTING_URL_SUFFIX));

		String cacheFile	= instance.getCompleteCacheFileName(destUrlString);
		Path cachePath	= Paths.get(cacheFile);
		assertTrue("Cache file should exist", Files.deleteIfExists(cachePath));
	}

	@Test
	public void testMultipleUrl() throws Exception
	{
		logTestName();

		initializeTest();

		/* support for testing addition of more than one url*/
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX, SETTING_URL_FILENAME1, SETTING_BASIC,
				SETTING_BASIC_OLD_VALUE, SETTING_BASIC_NEW_VALUE, "Multiple url test failed");
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX1, SETTING_URL_FILENAME2, SETTING_MULTIPLE_URL_TEST,
				null, SETTING_VALUE_FOUND, "multiple url test failed");

		/* removing one of the config url added above */
		modifyUrlAndVerifySetting(SETTING_URL_SUFFIX1, null, SETTING_MULTIPLE_URL_TEST,
				SETTING_VALUE_FOUND, null, "remove url test failed");
		assertEquals(3, m_rhucnCounter);
	}

	private void modifyUrlAndVerifySetting(String urlSettingSuffix,
										   String url,
										   String settingName,
										   String beforeValue,
										   String afterValue,
										   String errorMessage)
			throws Exception
	{
		validateSetting("Before test " + errorMessage, beforeValue, settingName);

		addURLToProvider(SETTING_URL + urlSettingSuffix, url);

		validateSetting("After test " + errorMessage, afterValue, settingName);
	}

	private void validateSetting(String errorMessage, String expected, String settingName) throws Exception
	{
		String actual = ConfigurationManager.getInstance().getSetting(settingName, null);
		assertEquals(errorMessage, expected, actual);
	}

	private String addURLToProvider(String settingName, String fileName)
			throws Exception
	{
		String urlString	= null;

		if(null != fileName)
		{
			if(fileName.startsWith("file:/"))
			{
				urlString				= fileName;
			}
			else
			{
				URL url			= FileFinder.findUrlOnClasspath(fileName);
				urlString				= url.toString();
			}
		}
		m_provider.addSetting(settingName, urlString);

		wasNotificationReceived();

		return urlString;
	}

	public void receiveEvent(String name, long timeMS, Object event)
	{
		String		eventText		= event.toString();

		if (eventText.contains(ConfigurationManager.REASON_PROVIDER_CHANGE)
				&& eventText.contains(RemoteHotUrlConfigurationProvider.class.getSimpleName()))
		{
			synchronized (this)
			{
				m_rhucnCounter++;
				m_rhucnReceived = true;
			}
		}
		else if (eventText.contains(ConfigurationManager.REASON_ADD_PROVIDER)
			&& eventText.contains(MemoryConfigurationProvider.class.getSimpleName()))
		{
			synchronized (this)
			{
				m_tcnEvent = TCP_ADDED;
			}
		}
		else if (eventText.contains(ConfigurationManager.REASON_REM_PROVIDER)
				&& eventText.contains(MemoryConfigurationProvider.class.getSimpleName()))
		{
			synchronized (this)
			{
				m_tcnEvent = TCP_REMOVED;
			}
		}
	}

	private boolean wasNotificationReceived() throws Exception
	{
		for (int i = 0; i < WAIT_ITERATIONS && !m_rhucnReceived; i++)
		{
			Thread.sleep(SLEEP_TIME);
		}

		synchronized (this)
		{
			if (m_rhucnReceived)
			{
				m_rhucnReceived = false;
				return true;
			}
		}
		return m_rhucnReceived;
	}
}
