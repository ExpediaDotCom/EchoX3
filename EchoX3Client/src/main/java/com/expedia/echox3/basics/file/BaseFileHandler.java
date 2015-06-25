/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import com.expedia.echox3.basics.configuration.ConfigurationManager;

public class BaseFileHandler
{
	public static final String	LINE_SEPARATOR					= System.getProperty("line.separator");
	public static final String	FOLDER_SEPARATOR				= System.getProperty("file.separator");

	public static final String	SETTING_PREFIX					= BaseFileHandler.class.getName();
	public static final String	SETTING_NAME_WORKING_FOLDER		= SETTING_PREFIX + ".WorkingFolder";

	private static String s_currentFolderName				= "/Working";

	private String		m_filename				= null;
	private int			m_lineCount				= 0;

	public static String getCurrentFolderName()
	{
		return s_currentFolderName;
	}

	public static void setCurrentFolderName(String currentFolderName)
	{
		s_currentFolderName = currentFolderName;
	}

	public String getFilename()
	{
		return m_filename;
	}

	public void setFilename(String filename)
	{
		m_filename = filename;
	}

	public void incrementLineCount()
	{
		++m_lineCount;
	}

	public int getLineCount()
	{
		return m_lineCount;
	}

	public static String getWorkingFolderName(String subFolderName)
	{
		String rootWorkingFolder		=
							ConfigurationManager.getInstance().getSetting(SETTING_NAME_WORKING_FOLDER, "/Working");
		StringBuilder sb						= new StringBuilder(100);

		sb.append(rootWorkingFolder);
		sb.append(FOLDER_SEPARATOR);
		sb.append(subFolderName);

		return sb.toString();
	}

	public static String getWorkingFilename(String subFolderName, String filename)
	{
		String rootWorkingFolder		=
									ConfigurationManager.getInstance().getSetting(SETTING_NAME_WORKING_FOLDER, ".");
		StringBuilder sb						= new StringBuilder(100);

		sb.append(rootWorkingFolder);
		sb.append(FOLDER_SEPARATOR);
		sb.append(subFolderName);
		sb.append(FOLDER_SEPARATOR);
		sb.append(filename);

		return sb.toString();
	}

	public static boolean ensureFolder(String filename)
	{
		File file		= new File(filename);
		String parentName	= file.getParent();
		if (null != parentName)
		{
			File parentFile	= new File(parentName);
			return parentFile.mkdirs();
		}
		else
		{
			return true;
		}
	}

	/**
	 * Utility method to safely close any Closeable, null or not, catching and ignoring any exception.
	 *
	 * @param closeable		Whatever needs to be closed
	 * @return				null that can be assigned to the now closed closeable variable, in case you need it.
	 */
	public static <T extends Closeable> T closeSafe(T closeable)
	{
		if (null != closeable)
		{
			try
			{
				closeable.close();
			}
			catch (IOException e)
			{
				// Ignore errors on close
			}
		}

		return null;
	}
}
