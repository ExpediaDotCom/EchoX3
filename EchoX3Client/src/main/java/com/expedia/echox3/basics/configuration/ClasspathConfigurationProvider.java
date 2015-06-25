/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.expedia.echox3.basics.file.UrlFinder;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;


public class ClasspathConfigurationProvider extends ProviderListConfigurationProvider
{
	private static final ClasspathConfigurationProvider			INSTANCE		= new ClasspathConfigurationProvider();

	private ClasspathConfigurationProvider()
	{
		super(ClasspathConfigurationProvider.class.getSimpleName());
		reloadFiles();

		getLogger().info(BasicEvent.EVENT_CONFIGURATION_LOAD_CLASSPATH,
				"Creating a provider for classpath. The content of this folder is not hot.");

		ConfigurationManager.getInstance().addProvider(this);
	}

	public static ClasspathConfigurationProvider getInstance()
	{
		return INSTANCE;
	}

	private void reloadFiles()
	{
		Set<URL> urlSet;
		try
		{
			urlSet	= UrlFinder.getFileUriListRecursive(
					ConfigurationManager.ROOT_FOLDER, ConfigurationManager.FILENAME_FILTER);
		}
		catch (Exception e)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_CP_ROOT_FOLDER_READ_ERROR,
					"Failed to obtain the list of configuration files", e);
			return;
		}

		synchronized (getProviderMap())
		{
			for (URL url : urlSet)
			{
				FileConfigurationProvider provider = getProviderMap().get(url.toString());
				if (null == provider)
				{
					try
					{
						provider = new FileConfigurationProvider(url);
						getProviderMap().put(url.toString(), provider);
					}
					catch (Exception e)
					{
						getLogger().error(BasicEvent.EVENT_CONFIGURATION_CP_FILE_RELOAD_ERROR,
								String.format("Failed to load configuration file %s", url.toString()), e);
					}
				}
				else
				{
					provider.reload();
				}
			}
		}
	}
}
