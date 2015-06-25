/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.BasicFileReader;
import com.expedia.echox3.basics.file.BasicFileWriter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;
import com.expedia.echox3.basics.tools.time.WallClock;

public class RemoteHotUrlConfigurationProvider extends ProviderListConfigurationProvider
{
	public static final String SETTING_PREFIX			= RemoteHotUrlConfigurationProvider.class.getName();
	public static final String SETTING_PREFIX_URL		= SETTING_PREFIX + ".url.";

	private static final Set<Character>		ILLEGAL_FILENAME_CHARS		=
			new HashSet<>(Arrays.asList(new Character[]{':', '/', '?', '*', '~'}));
	private static final RemoteHotUrlConfigurationProvider INSTANCE	= new RemoteHotUrlConfigurationProvider();

	private RemoteHotUrlConfigurationProvider()
	{
		super(SETTING_PREFIX_URL);
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
		ConfigurationManager.getInstance().addProvider(this);

		PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);

		new UpdateThread(this);
	}

	public static RemoteHotUrlConfigurationProvider getInstance()
	{
		return INSTANCE;
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		ConfigurationManager		manager			= ConfigurationManager.getInstance();
		Set<String>					includeSet		= manager.getSettingNameSet(SETTING_PREFIX_URL);
		boolean						isChanged		= false;
		URL							url;

		synchronized (getProviderMap())
		{
			for (String settingName : includeSet)
			{
				String settingValue = manager.getSetting(settingName, null);

				try
				{
					url = new URL(settingValue);
				}
				catch (Exception exception)
				{
					getLogger().error(BasicEvent.EVENT_INVALID_URL, exception,
							"Unable to convert setting %s = %s into a url",
							settingName,
							settingValue);
					continue;
				}

				boolean isProviderChanged;
				boolean isNew = false;
				FileConfigurationProvider provider = getProviderMap().get(settingName);

				// Add the ones that are not there, or update the ones that are changed.
				if (null == provider || !provider.getSource().equals(url))
				{
					isNew = true;
					provider = new FileConfigurationProvider(url);
					synchronized (getProviderMap())
					{
						getProviderMap().put(settingName, provider);
					}
					isProviderChanged = !provider.isFailedLoad();
				}
				else // This setting did not change. But reload as the file contents might have changed.
				{
					isProviderChanged = provider.reload();
				}

				if (isProviderChanged)
				{
					isChanged = true;
					writeCacheFile(provider);
				}
				else if (isNew)
				{
					readCacheFile(provider);
				}
			}

			isChanged |= getProviderMap().keySet().retainAll(includeSet);
		}

		if (isChanged)
		{
			ConfigurationManager.getInstance().postChangeEvent(ConfigurationManager.REASON_PROVIDER_CHANGE, this);
		}
	}

	public void writeCacheFile(FileConfigurationProvider provider)
	{

		String workingFilename = getCompleteCacheFileName(provider.getSource());
		try
		{
			BasicFileWriter.writePropertiesFile(workingFilename, provider.getSettingMap());
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_URL_CONFIGURATION_CACHE_WRITE_FAILED, exception,
					"Unable to write configuration from url %s to file %s",
					provider.getSource().toString(), workingFilename);
		}
	}

	private void readCacheFile(FileConfigurationProvider provider)
	{
		String workingFilename = getCompleteCacheFileName(provider.getSource());
		try
		{
			Properties properties			= BasicFileReader.readPropertiesFile(workingFilename);
			provider.setSettingMap(properties);
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_URL_CONFIGURATION_CACHE_READ_FAILED, exception,
					"Unable to read configuration for url %s from file %s",
					provider.getSource().toString(), workingFilename);
		}
	}

	public String getCompleteCacheFileName(final Object url)
	{
		String cacheFileName		= transformToFilename(url.toString());
		return BaseFileHandler.getWorkingFilename("cache", cacheFileName);
	}

	public static String transformToFilename(String url)
	{
		StringBuilder stringBuilder		= new StringBuilder(url.length());
		for(int i = 0 ; i < url.length() ; i++)
		{
			char			c					= url.charAt(i);
			stringBuilder.append(ILLEGAL_FILENAME_CHARS.contains(c) ? '_' : c);
		}
		return stringBuilder.toString();
	}

	@Override
	public void close()
	{
		PublisherManager.deregister(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);

		super.close();
	}

	private static class UpdateThread extends AbstractScheduledThread
	{
		RemoteHotUrlConfigurationProvider m_provider;

		public UpdateThread(RemoteHotUrlConfigurationProvider provider)
		{
			super(false);

			m_provider = provider;

			//setSettingPrefix(SETTING_PREFIX);
			setName(RemoteHotUrlConfigurationProvider.class.getSimpleName());
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			m_provider.updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
		}
	}
}
