/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.configuration;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.expedia.echox3.basics.collection.simple.ManagedSet;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.time.WallClock;


public class FolderListConfigurationProvider implements Closeable
{
	// CHECKSTYLE:OFF
	public static final String 				SETTING_PREFIX			= FolderListConfigurationProvider.class.getName();
	public static final String				SETTING_PREFIX_URL		= SETTING_PREFIX + ".Folder.";
	private static final FolderListConfigurationProvider
													INSTANCE				= new FolderListConfigurationProvider();
	// CHECKSTYLE:ON

	private final ManagedSet<String>						m_settingNameSet		= new ManagedSet<>();
	private final Map<String, FolderConfigurationProvider>	m_providerMap			= new HashMap<>();

	private FolderListConfigurationProvider()
	{
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);

		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	public static FolderListConfigurationProvider getInstance()
	{
		return INSTANCE;
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		ConfigurationManager		manager				= ConfigurationManager.getInstance();
		Set<String> settingNameSet		= manager.getSettingNameSet(SETTING_PREFIX_URL);

		synchronized (m_settingNameSet)
		{
			m_settingNameSet.setTo(settingNameSet);
			Set<String> setAdded		= m_settingNameSet.getSetAdded();
			Set<String> setRemoved		= m_settingNameSet.getSetRemoved();
			Set<String> setRetained		= m_settingNameSet.getSetRetained();

			for (String settingName : setAdded)
			{
				String folderName		= manager.getSetting(settingName, null);
				if (null != folderName && !folderName.isEmpty())
				{
					FolderConfigurationProvider		provider	= new FolderConfigurationProvider(folderName);
					m_providerMap.put(settingName, provider);
				}
			}

			for (String settingName : setRemoved)
			{
				FolderConfigurationProvider			provider	= m_providerMap.remove(settingName);
				if (null != provider)
				{
					provider.close();
				}
			}

			for (String settingName : setRetained)
			{
				FolderConfigurationProvider		provider		= m_providerMap.get(settingName);
				String							folderName		= manager.getSetting(settingName, null);
				if (null != provider									// Should always be true
						&& !provider.getSource().equals(folderName))	// Change in folder name
				{
					// Tear down the previous one
					provider.close();

					// Start a new one
					if (null != folderName)
					{
						FolderConfigurationProvider		providerNew		= new FolderConfigurationProvider(folderName);
						m_providerMap.put(settingName, providerNew);
					}
				}
			}
		}

	}

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 * <p>
	 * <p> As noted in {@link AutoCloseable#close()}, cases where the
	 * close may fail require careful attention. It is strongly advised
	 * to relinquish the underlying resources and to internally
	 * <em>mark</em> the {@code Closeable} as closed, prior to throwing
	 * the {@code IOException}.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close()
	{
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).deregister(this::updateConfiguration);

		synchronized (m_settingNameSet)
		{
			for (FolderConfigurationProvider folderProvider : m_providerMap.values())
			{
				folderProvider.close();
			}
			m_providerMap.clear();
		}
	}
}
