/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.file.SimpleFilenameFilter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.thread.AbstractBaseThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;

public class FolderConfigurationProvider extends ProviderListConfigurationProvider
{
	private static final String WEB_INF_CLASSES		=
															"WEB-INF" + BaseFileHandler.FOLDER_SEPARATOR + "classes";
	private static final WatchService				WATCH_SERVICE;
	private static final WatcherThread				WATCH_THREAD;

	private WatchKey m_key		= null;

	static
	{
		FileSystem fileSystem		= FileSystems.getDefault();
		WatchService watchService	= null;
		WatcherThread	watcherThread	= null;
		try
		{
			watchService = fileSystem.newWatchService();
			watcherThread = new WatcherThread(watchService);
		}
		catch (IOException exception)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_FOLDER_WATCHER_INIT_ERROR,
					"Failed to initialize WatcherThread. Configuration files will not be hot.", exception);
		}

		WATCH_SERVICE = watchService;
		WATCH_THREAD = watcherThread;
	}

	public FolderConfigurationProvider(String folderName)
	{
		super(folderName);

		File file		= new File(folderName);
		getLogger().info(BasicEvent.EVENT_CONFIGURATION_LOAD_FOLDER,
				"Creating a provider for folder '%s'. The content of this folder is HOT.", file.getAbsolutePath());

		reloadFiles();
		FileSystem fileSystem		= FileSystems.getDefault();
		Path path			= fileSystem.getPath(folderName);
		try
		{
			m_key = path.register(WATCH_SERVICE,
					StandardWatchEventKinds.OVERFLOW,
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
			WATCH_THREAD.registerKey(m_key, this);
		}
		catch (IOException e)
		{
			getLogger().warn(BasicEvent.EVENT_FOLDER_REGISTER_FAILED, e,
					"Failed to register the folder %s for notification, its configuration content will not be hot.",
					path.toAbsolutePath());
		}
		ConfigurationManager.getInstance().addProvider(this);
	}

	private void reloadFiles()
	{
		Set<File> includeSet;
		Set<File> excludeSet;
		try
		{
			includeSet = FileFinder.getFilenameSet((String) getSource(), ConfigurationManager.FILENAME_FILTER);

			// Exclude the duplicate files in WEB-INF/classes, as they are on the classpath.
			// These files are loaded only when running in the debugger.
			excludeSet = FileFinder.getFilenameSet((String) getSource(),
					new SimpleFilenameFilter(null, WEB_INF_CLASSES, ConfigurationManager.FILENAME_SUFFIX));
		}
		catch (Exception e)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_FOLDER_LIST_ERROR, e,
					"Failed to obtain the list of configuration files");
			return;
		}
		includeSet.removeAll(excludeSet);

		boolean			isModified		= false;
		synchronized (getProviderMap())
		{
			// See which file provider needs to be added...
			for (File file : includeSet)
			{
				FileConfigurationProvider		provider		= getProviderMap().get(file);
				if (null == provider)
				{
					provider = new FileConfigurationProvider(file);
					getProviderMap().put(file, provider);
					isModified = true;
				}
				else
				{
					isModified |= provider.reload();
				}
			}

			//  See which file provider has been removed...
			Iterator<? super Comparable<?>> iterator		= getProviderMap().keySet().iterator();
			while (iterator.hasNext())
			{
				// Strictly speaking, the object SHOULD always be a File, as it is only put a few lines above.
				// However, this is a ProviderListConfigurationProvider, where the key to the getProviderMap()
				// can be anything and is only defined as an Object.
				// The paranoid thing to do is to validate.
				Object		object		= iterator.next();
				if (!(object instanceof File))
				{
					iterator.remove();
					isModified = true;
					continue;
				}

				File		file		= (File) object;
				if (!includeSet.contains(file))
				{
					iterator.remove();
					isModified = true;
				}
			}
		}

		if (isModified)
		{
			ConfigurationManager.getInstance().postChangeEvent(ConfigurationManager.REASON_PROVIDER_CHANGE, this);
		}
	}

	@Override
	public void close()
	{
		m_key.cancel();

		super.close();
	}





	private static class WatcherThread extends AbstractBaseThread
	{
		private WatchService m_watcher;
		private Map<WatchKey, FolderConfigurationProvider> m_keyMap		= new HashMap<>();

		public WatcherThread(WatchService watcher)
		{
			m_watcher = watcher;

			setName("FolderConfigurationUpdate");
			setDaemon(true);
			start();
		}

		public void registerKey(WatchKey key, FolderConfigurationProvider provider)
		{
			m_keyMap.put(key, provider);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void run()
		{
			while (true)
			{
				WatchKey key			= null;
				try
				{
					key = m_watcher.take();
					boolean 		isValid		= key.isValid();
					if (!isValid)
					{
						getLogger().debug(BasicEvent.EVENT_DEBUG, "Key is not valid!");
						continue;
					}
					// Walk the list of events to clear them
					List<WatchEvent<?>> list		= key.pollEvents();
//					boolean					isCreate	= false;
					for (WatchEvent<?> event : list)		// NOPMD, event is unused.
					{
						getLogger().debug(BasicEvent.EVENT_DEBUG, "Receive Watcher event %s", event.kind().name());

//						if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()))
//						{
//							isCreate = true;
//						}
					}
					// If isCreate, there is a slight delay before the file is available for read ...
					// Actually, be paranoid and always a bit to be safe.
//					if (isCreate)
					{
						BasicTools.sleepMS(100);
					}

					// Send the notification to the folder...
					FolderConfigurationProvider		provider	= m_keyMap.get(key);
					if (null != provider)
					{
						provider.reloadFiles();
					}
				}
				catch (Throwable throwable)
				{
					getLogger().error(BasicEvent.EVENT_CONFIGURATION_FOLDER_WATCHER_RUN_ERROR, throwable,
							"Unexpected exception issue in configuration update thread.");
				}
				finally
				{
					if (null != key)
					{
						key.reset();
					}
				}
			}
		}
	}
}
