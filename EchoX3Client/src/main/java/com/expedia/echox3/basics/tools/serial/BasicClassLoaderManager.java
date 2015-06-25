/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.file.BasicFileReader;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;

public class BasicClassLoaderManager
{
	private static final Map<String, BasicClassLoader> LOADER_MAP	= new HashMap<>();

	public static BasicClassLoader getClassLoader(String name)
	{
		synchronized (LOADER_MAP)
		{
			BasicClassLoader		loader		= LOADER_MAP.get(name);
			if (null == loader)
			{
				loader = new BasicClassLoader(name);
				LOADER_MAP.put(name, loader);
			}
			return loader;
		}
	}

	public static void flushClassLoader(String name)
	{
		synchronized (LOADER_MAP)
		{
			BasicClassLoader		loader	= LOADER_MAP.remove(name);
			if (null != loader)
			{
				loader.unregisterMBean();
			}
		}
	}




	/**
	 * Gets class definition from the ".class" file as a byte array given a class object
	 *
	 * @param className				Fully qualified class name
	 * @return						The byte[] containing the class definition
	 * @throws BasicException		Something can always go wrong.
	 */
	public static byte[] getClassBytes(String className, ClassLoader classLoaderSuggested) throws BasicException
	{
		Class<?>		clazz		= findClass(className, classLoaderSuggested);
		byte[]			bytes		= getClassBytes(clazz);

		return bytes;
	}


	/**
	 * Gets class definition from the ".class" file as a byte array given a class object
	 *
	 * @param clazz					The class object
	 * @return						The byte[] containing the class definition
	 * @throws BasicException		Something can always go wrong.
	 */
	private static byte[] getClassBytes(Class<?> clazz) throws BasicException
	{
		// obtain input stream for class bytes
		String 		className 	= clazz.getName();
		String 		classAsPath = className.replace('.', '/') + ".class";

		URL url = clazz.getClassLoader().getResource(classAsPath);
		if (null == url)
		{
			throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_CLASS_NOT_FOUND,
					"Failed to locate resource for class " + className);
		}

		byte[]			classBytes;
		InputStream inputStream 		= null;
		try
		{
			inputStream = url.openStream();
			classBytes = BasicFileReader.readInputStream(inputStream);
		}
		catch (IOException e)
		{
			String message = String.format(
					"Attempting to read class byte stream for %s from %s", className, url.toString());
			throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_CLASS_NOT_FOUND, message, e);
		}
		finally
		{
			try
			{
				if (null != inputStream)
				{
					inputStream.close();
				}
			}
			catch (IOException e)
			{
				// Nothing to do
			}
		}

		return classBytes;
	}
	/**
	 * Attempts to find the class, using all possible class loaders, starting with the suggested one.
	 *
	 * @param className			Fully qualified class name
	 * @param classLoaderSuggested	Suggested class loader to use first. null if no suggestion.
	 * @return					The Class object for the class name
	 * @throws BasicException
	 */
	private static Class<?> findClass(String className, ClassLoader classLoaderSuggested) throws BasicException
	{
		Class<?>		classObject		= null;

		// Hunt for the class object...
		if (null != classLoaderSuggested)
		{
			classObject = findClassWithClassLoader(className, classLoaderSuggested);
		}

		if (null == classObject)
		{
			ClassLoader		classLoader		= Thread.currentThread().getContextClassLoader();
			classObject = findClassWithClassLoader(className, classLoader);
		}

		if (null == classObject)
		{
			ClassLoader		classLoader		= ClassLoader.getSystemClassLoader();
			classObject = findClassWithClassLoader(className, classLoader);
		}

		if (null == classObject)
		{
			String		message		= String.format("Definition of class %s not found.", className);
			throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_CLASS_NOT_FOUND, message);
		}

		return classObject;
	}

	/**
	 * Utility method to locate a class within a specific class loader. Typically used by findClass(String className).
	 *
	 * @param className		Fully qualified name of the class to find
	 *                      (e.g. com.expedia.e3.platform.multicache.common.tools.MultiClassLoaderManager)
	 * @param classLoader	Specific class loader to use
	 * @return				Either a Class object if it is found or null if not found.
	 * 						Returns null if the class object is not found.
	 */
	public static Class<?> findClassWithClassLoader(String className, ClassLoader classLoader)
	{
		Class<?>		classObject				= null;

		try
		{
			if (null != classLoader)
			{
				classObject = Class.forName(className, false, classLoader);
			}
		}
		catch (Throwable throwable)
		{
			// Do nothing
		}

		return classObject;
	}
}
