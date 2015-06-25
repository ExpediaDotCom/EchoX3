/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.file;

import java.io.File;
import java.io.FilenameFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;


public class FileFinder
{
	private static ClassLoader s_classLoader		= FileFinder.class.getClassLoader();
//	private static final Logger LOGGER			= Logger.getLogger(FileFinder.class.getName());

	public static ClassLoader getClassLoader()
	{
		return s_classLoader;
	}

	public static URL findUrlOnClasspath(String shortName)
	{
		URL url				= getClassLoader().getResource(shortName);

		return url;
	}

	public static File getUniqueFilename(String rootFolderName, FilenameFilter filter)
	{
		Set<File> set		= getFilenameSet(rootFolderName, filter);
		int				size	= set.size();
		if (1 == size)
		{
			return set.iterator().next();
		}
		else if (0 == size)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_FILE_FINDER_NOT_FOUND,
					String.format("File not found: %s/%s", rootFolderName, filter));
		}
		else // if (1 < size)
		{
			StringBuilder sb			= new StringBuilder(1000);
			sb.append(String.format("Multiple files found for: %s/%s", rootFolderName, filter));
			for (File file : set)
			{
				sb.append(BaseFileHandler.LINE_SEPARATOR);
				sb.append(file.getAbsolutePath());
			}

			throw new BasicRuntimeException(BasicEvent.EVENT_FILE_FINDER_NOT_FOUND, sb.toString());
		}
	}

	public static Set<File> getFilenameSet(String rootFolderName, FilenameFilter filter)
	{
		Set<File> set		= new TreeSet<>();

		Path dir = FileSystems.getDefault().getPath(rootFolderName);
		getFilenameSet(dir.toFile(), filter, set);

		return set;
	}

	private static void getFilenameSet(File folder, FilenameFilter filter, Set<File> set)
	{
		if (!folder.isDirectory())
		{
			return;
		}
		File[]		childList		= folder.listFiles();
		if (null != childList)
		{
			for (File child : childList)
			{
				if (child.isFile())
				{
					if (null == filter || filter.accept(folder, child.getAbsolutePath()))
					{
						set.add(child);
					}
				}
				else	// if (child.isDirectory())
				{
					getFilenameSet(child, filter, set);
				}
			}
		}
	}

	/**
	 * This assumes all classes are packaged in JARs, which is true for X-1 projects.
	 *
	 * @param packageName	an optional package to filter the list of classes; null for all classes
	 * @return				List of all classes, possibly filtered
	 * @throws BasicException	Something went wrong
	 */
	@SuppressWarnings({"PMD", "rawtypes"})
	public static List<Class> getClassList(String packageName) throws BasicException
	{
		List<String>		classNameList		= getClassNameList(packageName);
		List<Class>			classList			= new LinkedList<>();
		for (String className : classNameList)
		{
			try
			{
				Class clazz		= Class.forName(className, true, getClassLoader());
				classList.add(clazz);
			}
			catch (ClassNotFoundException e)
			{
				// Class.forName() fails on some inner classes, ignore.
			}
		}
		return classList;
	}

	public static List<String> getClassNameList(String packageName) throws BasicException
	{
		Map<String, ManifestWrapper>	manifestWrapperMap		= ManifestWrapper.getManifestMap();
		SimpleFilenameFilter			filter					= new SimpleFilenameFilter(
									null, packageName, ".class");

		List<String> classList				= new LinkedList<>();
		try
		{
			for (ManifestWrapper wrapper : manifestWrapperMap.values())
			{
				String		uriText		= wrapper.getUrl().toString();
				int			index		= uriText.indexOf('!');
				if (-1 != index)
				{
					// Found a jar! use it
					String jarName				= uriText.substring(0, index) + "!/";
					URLConnection urlConnection		= new URL(jarName).openConnection();
					if (urlConnection instanceof JarURLConnection)		// Always true :)
					{
						JarURLConnection		jarConnection		= (JarURLConnection) urlConnection;
						try (JarFile jar = jarConnection.getJarFile())
						{
							Enumeration<JarEntry>		entries		= jar.entries();
							while (entries.hasMoreElements())
							{
								JarEntry	entry		= entries.nextElement();
								String		entryName	= entry.getName().replace("/", ".");
								if (filter.accept(null, entryName))
								{
									String		className	= entryName.substring(0,
																		entryName.length() - ".class".length());
										classList.add(className);
								}
							}
						}
						catch (Exception exception)
						{
							throw new BasicException(BasicEvent.EVENT_TODO, exception, "Something went wrong");
						}
					}
				}
/*
				else
				{
					// Ignore files not packaged in a JAR
				}
*/
			}
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_TODO, exception, "Something went wrong");
		}
		return classList;
	}
}
