/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.file;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;

public class BasicFileReader extends BaseFileHandler implements Closeable
{
	private static final String FAILED_TO_OPEN_FORMAT	= "Failed to open file '%s'";

	private volatile InputStream m_inputStream			= null;
	private volatile GZIPInputStream m_gzipInputStream		= null;
	private volatile InputStreamReader m_inputReader			= null;
	private volatile BufferedReader m_bufferedReader		= null;

	public void openFile(URL url) throws BasicException
	{
		openStream(url);

		m_inputReader = new InputStreamReader(m_inputStream);
		m_bufferedReader = new BufferedReader(m_inputReader);
	}
	public void openStream(URL url) throws BasicException
	{
		// Make sure there is not a leaked opened file
		close();

		try
		{
			setFilename(url.getPath());
			m_inputStream = url.openStream();
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FAILED, exception,
					FAILED_TO_OPEN_FORMAT, url.toString());
		}
	}

	public void openGzipStream(URL url) throws BasicException
	{
		openStream(url);

		try
		{
			m_gzipInputStream = new GZIPInputStream(m_inputStream);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FAILED, exception,
					FAILED_TO_OPEN_FORMAT, url.toString());
		}
	}

	public void openFile(String filename) throws BasicException
	{
		openStream(filename);
		m_inputReader = new InputStreamReader(m_inputStream);
		m_bufferedReader = new BufferedReader(m_inputReader);
	}

	public void openStream(String filename) throws BasicException
	{
		// Make sure there is not a leaked opened file
		close();

		try
		{
			setFilename(filename);
			m_inputStream = new FileInputStream(filename);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FAILED, exception,
					FAILED_TO_OPEN_FORMAT, filename);
		}
	}

	public void openGzipStream(String filename) throws BasicException
	{
		openStream(filename);
		try
		{
			m_gzipInputStream = new GZIPInputStream(m_inputStream);
		}
		catch (IOException exception)
		{
			throw new BasicException(BasicEvent.EVENT_FILE_OPEN_FAILED, exception,
					FAILED_TO_OPEN_FORMAT, filename);
		}
	}

	public void openFile(File file) throws BasicException
	{
		openFile(file.getAbsolutePath());
	}
	public void openStream(File file) throws BasicException
	{
		openStream(file.getAbsolutePath());
	}

	public void attachStreamAsReader(InputStream inputStream)
	{
		// Note that the caller retains ownership (and responsibility to close) of the inputStream)
		m_inputReader = new InputStreamReader(inputStream);
		m_bufferedReader = new BufferedReader(m_inputReader);
	}

	public String readLine() throws IOException
	{
		String line;

		while (null != (line = m_bufferedReader.readLine()))
		{
			incrementLineCount();
			if (line.startsWith("#") || line.isEmpty())
			{
				continue;
			}

			break;
		}

		return line;
	}

	public InputStream getInputStream()
	{
		return m_inputStream;
	}

	public InputStream getGzipInputStream()
	{
		return m_gzipInputStream;
	}

	@SuppressWarnings("UnusedDeclaration")
	public InputStreamReader getInputReader()
	{
		return m_inputReader;
	}

	@SuppressWarnings("UnusedDeclaration")
	public BufferedReader getBufferedReader()
	{
		return m_bufferedReader;
	}

	@SuppressWarnings("NonAtomicOperationOnVolatileField")
	public void close()
	{
		m_bufferedReader	= closeSafe(m_bufferedReader);
		m_inputReader		= closeSafe(m_inputReader);
		m_gzipInputStream	= closeSafe(m_gzipInputStream);
		m_inputStream		= closeSafe(m_inputStream);
	}

	@SuppressWarnings("UnusedDeclaration")
	public static String readTextFile(String filename) throws IOException
	{
		InputStream inputStream		= null;
		try
		{
			inputStream = new FileInputStream(filename);
			return readTextFile(inputStream);
		}
		finally
		{
			closeSafe(inputStream);
		}
	}
	public static String readTextFile(InputStream inputStream) throws IOException
	{
		StringBuilder sb			= new StringBuilder();

		byte[]					bytes		= new byte[4096];
		int 					cb;
		while (-1 != (cb = inputStream.read(bytes)))
		{
			sb.append(new String(bytes, 0, cb));
		}
		return sb.toString();
	}
	public static byte[] readInputStream(InputStream inputStream) throws IOException
	{
		ByteArrayOutputStream outputStream		= new ByteArrayOutputStream();
		byte[]					byteArray			= new byte[10 * 1024];
//		int						cbGuess				= inputStream.available();
		int						cb;
		while (-1 != (cb = inputStream.read(byteArray)))
		{
			outputStream.write(byteArray, 0, cb);
		}
		outputStream.flush();
		byte[]		outputBytes		= outputStream.toByteArray();
		outputStream.close();

		return outputBytes;
	}

	public static Properties readPropertiesFile(URL url) throws IOException
	{
		InputStream inputStream		= null;

		try
		{
			inputStream		= url.openStream();
			return readPropertiesFile(inputStream);
		}
		finally
		{
			closeSafe(inputStream);
		}
	}

	public static Properties readPropertiesFile(File file) throws IOException
	{
		InputStream inputStream		= null;
		try
		{
			inputStream = new FileInputStream(file);
			return readPropertiesFile(inputStream);
		}
		finally
		{
			closeSafe(inputStream);
		}
	}

	public static Properties readPropertiesFile(String filename) throws IOException
	{
		InputStream inputStream		= null;
		try
		{
			inputStream = new FileInputStream(filename);
			return readPropertiesFile(inputStream);
		}
		finally
		{
			closeSafe(inputStream);
		}
	}
	public static Properties readPropertiesFile(InputStream inputStream) throws IOException
	{
		Properties properties				= new Properties();

		properties.load(inputStream);

		return properties;
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getFilename());
	}
}
