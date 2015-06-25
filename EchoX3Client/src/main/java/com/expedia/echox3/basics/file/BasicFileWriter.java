/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.basics.tools.time.WallClock.FormatType;
import com.expedia.echox3.basics.tools.time.WallClock.FormatSize;

public class BasicFileWriter extends BaseFileHandler implements Closeable
{
	private static	final String FAILED_TO_WRITE_TEMP_FILE	= "Failed to create temporary file with "
			+ "PREFIX '%s' SUFFIX '%s'" ;
	private	static	final String FAILED_TO_WRITE_FILE		= "Failed to write file '%s' ";

	private OutputStream m_outputStream				= null;
	private PrintWriter m_printWriter				= null;

	public BasicFileWriter()
	{

	}
	public BasicFileWriter(String filename) throws BasicException
	{
		openFile(filename);
	}

	public void openTempFile(String prefix, String suffix) throws BasicException
	{
		try
		{
			File file = File.createTempFile(prefix, suffix);
			String filename = file.getAbsolutePath();
			openFile(filename);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_CREATE_FAILED, exception,
					FAILED_TO_WRITE_TEMP_FILE, prefix, suffix);
		}
	}

	public final void openFile(String filename) throws BasicException
	{
		try
		{
			ensureFile(filename, false);
			m_printWriter = new PrintWriter(filename);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FOR_WRITE_FAILED, exception,
					FAILED_TO_WRITE_FILE, filename);
		}
	}

	public final void setStream(OutputStream outputStream)
	{
		m_outputStream = outputStream;
	}

	public final void openStream(String filename) throws BasicException
	{
		openStream(filename, false);
	}
	public final void openStream(String filename, boolean fAppend) throws BasicException
	{
		try
		{
			ensureFile(filename, fAppend);
			m_outputStream = new FileOutputStream(filename, fAppend);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FOR_WRITE_FAILED, exception,
				FAILED_TO_WRITE_FILE, filename);
		}
	}

	public final void openStream(URL url) throws BasicException
	{
		setFilename(url.getPath());

		try
		{
			URLConnection connection	= url.openConnection();
			m_outputStream				= connection.getOutputStream();
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FOR_WRITE_FAILED, exception,
					FAILED_TO_WRITE_FILE, url.toString());
		}
	}

	/**
	* This method ensures if the file can be created.
	 * It does not actually create the file as that would be done as part of the FileOutputStream or PrintWriter class
	 * */
	private void ensureFile(String filename, boolean fAppend)
	{
		// Make sure there is not a leaked opened file
		close();
		File file		= new File(filename);
		if (file.exists() && !fAppend)
		{
			file.delete();
		}

		setFilename(filename);
		ensureFolder(filename);
	}

	public final void appendFile(String filename) throws BasicException
	{
		openStream(filename, true);
		m_printWriter = new PrintWriter(m_outputStream);
	}
	public void println(String line)
	{
		incrementLineCount();
		m_printWriter.println(line);
	}
	public void printStackTrace(Throwable throwable)
	{
		throwable.printStackTrace(m_printWriter);
	}

	public void flush()
	{
		m_printWriter.flush();
	}

	@Override
	public void close()
	{
		closeSafe(m_printWriter);
		m_printWriter = null;
		closeSafe(m_outputStream);
		m_outputStream = null;
	}

	public OutputStream getOutputStream()
	{
		return m_outputStream;
	}

	public PrintWriter getPrintWriter()
	{
		return m_printWriter;
	}

	public static void writePropertiesFile(String filename, Map<String, String> map) throws BasicException
	{
		long					now			= WallClock.getCurrentTimeMS();
		String					nowText		= WallClock.formatTime(FormatType.DateTime, FormatSize.Medium, now);
		String					comment		= String.format("%s on %s", filename, nowText);
		try(BasicFileWriter		writer		= new BasicFileWriter(filename))
		{
			Properties		properties		= new Properties();
			properties.putAll(map);
			properties.store(writer.getPrintWriter(), comment);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FOR_WRITE_FAILED, exception,
					FAILED_TO_WRITE_FILE, filename);
		}
	}
}
