/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.Arrays;

import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;

public class StringGroup
{
	public static final String		DEFAULT_SEPARATOR		= ".";

	private String			m_separator			= null;
	private String			m_singleString		= null;

	private String[]		m_stringArray		= null;

	public StringGroup()
	{

	}

	public StringGroup(String text)
	{
		if (null != text)
		{
			set(text, DEFAULT_SEPARATOR);
		}
	}
	public StringGroup(String[] array)
	{
		set(array);
	}

	public StringGroup getCopy()
	{
		getStringArray();

		return new StringGroup(Arrays.copyOf(m_stringArray, m_stringArray.length));
	}

	public void set(String[] array)
	{
		m_stringArray	= array;
		m_separator		= DEFAULT_SEPARATOR;
		m_singleString	= null;
	}

	public void set(String rawString)
	{
		set(rawString, DEFAULT_SEPARATOR);
	}
	public void set(String rawString, String separator)
	{
		m_singleString	= rawString;
		m_separator		= separator;
		m_stringArray	= null;
	}

	public void append(String text)
	{
		try
		{
			getStringArray();
			m_stringArray = Arrays.copyOf(m_stringArray, m_stringArray.length + 1);
			m_stringArray[m_stringArray.length - 1] = text;
		}
		catch (Exception e)
		{
			// No data yet ...
			m_stringArray = new String[1];
			m_stringArray[0] = text;
		}
	}
	public void prepend(String text)
	{
		try
		{
			getStringArray();
			String[] newArray = new String[m_stringArray.length + 1];
			System.arraycopy(m_stringArray, 0, newArray, 1, m_stringArray.length);
			newArray[0] = text;
			m_stringArray = newArray;
			composeText(m_separator);
		}
		catch (Exception e)
		{
			// No data yet ...
			m_stringArray = new String[1];
			m_stringArray[0] = text;
		}
	}

	public String[] getStringArray()
	{
		if (null == m_stringArray)
		{
			if (null != m_singleString)
			{
				parseRawString();
			}
			else
			{
				throw new BasicRuntimeException("No data is available.");
			}
		}

		return m_stringArray;
	}

	public String getString()
	{
		return getString(m_separator);
	}
	public String getString(String separator)
	{
		composeText(separator);

		return m_singleString;
	}



	private void parseRawString()
	{
		m_stringArray = m_singleString.replace(m_separator, "\n").split("\n");
		for (int i = 0; i < m_stringArray.length; i++)
		{
			m_stringArray[i] = m_stringArray[i].trim();
		}
	}

	private void composeText(String separator)
	{
		String[]		array		= getStringArray();
		String			actualSep	= "";
		StringBuilder	sb			= new StringBuilder(16 * array.length);		// Try to have only one alloc
		for (String str : array)
		{
			sb.append(actualSep);
			actualSep = separator;		// Just don't use it before the first element.
			sb.append(str);
		}
		m_singleString		= sb.toString();
		m_separator			= separator;
	}

	@Override
	public String toString()
	{
		return getString();
	}
}
