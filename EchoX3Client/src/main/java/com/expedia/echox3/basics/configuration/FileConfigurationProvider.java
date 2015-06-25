/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import com.expedia.echox3.basics.file.BasicFileReader;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;

public class FileConfigurationProvider extends AbstractConfigurationProvider
{
	private boolean			m_isFileSystem;
	private boolean			m_isFailedLoad		= false;
	private long			m_lastModified		= 0;

	public FileConfigurationProvider(URL url)
	{
		super(url);

		m_isFileSystem = url.toString().startsWith("file:");
		reload();
	}

	public FileConfigurationProvider(File file)
	{
		super(file);

		m_isFileSystem = true;
		reload();
	}

	public FileConfigurationProvider(String filename)
	{
		super(filename);

		m_isFileSystem = true;
		reload();
	}

	public boolean isFailedLoad()
	{
		return m_isFailedLoad;
	}

	public final boolean reload()
	{
		long			lastModified;
		Properties properties;
		File file;
		if (getSource() instanceof URL)
		{
			try
			{
				properties = BasicFileReader.readPropertiesFile((URL) getSource());
				m_isFailedLoad = false;
			}
			catch (Throwable throwable)
			{
				getLogger().error(BasicEvent.EVENT_CONFIGURATION_URL_LOAD_ERROR, throwable,
						String.format("Failed to load configuration URL %s", getSource().toString()));
				m_isFailedLoad = true;
				return false;
			}
			if (m_isFileSystem)
			{
				// URL is a file, maintain m_lastModified
				String filename		= ((URL) getSource()).getFile();
				file			= new File(filename);
				lastModified = file.lastModified();
			}
			else
			{
				lastModified = System.currentTimeMillis();
			}
		}
		else
		{
			if (getSource() instanceof File)
			{
				file = (File) getSource();
			}
			else // if (getSource() instanceof String)
			{
				file = new File(getSource().toString());
			}
			lastModified = file.lastModified();
			try
			{
				properties = BasicFileReader.readPropertiesFile(file);
				m_isFailedLoad = false;
			}
			catch (Throwable throwable)
			{
				getLogger().error(BasicEvent.EVENT_CONFIGURATION_FILE_LOAD_ERROR,
						String.format("Failed to load configuration file %s", getSource().toString()), throwable);
				m_isFailedLoad = true;
				return false;
			}
		}

		boolean		isModified		= false;
		if (lastModified > m_lastModified)
		{
			isModified = setSettingMap(properties);
			m_lastModified = lastModified;
		}

		return isModified;
	}
}
