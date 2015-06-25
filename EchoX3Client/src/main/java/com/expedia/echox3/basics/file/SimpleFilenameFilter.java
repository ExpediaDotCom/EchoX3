/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.file;

import java.io.File;
import java.io.FilenameFilter;

public class SimpleFilenameFilter implements FilenameFilter
{
	private String m_prefix;
	private String m_contains;
	private String m_suffix;

	public SimpleFilenameFilter(String prefix, String contains, String suffix)
	{
		m_prefix = prefix;
		m_contains = contains;
		m_suffix = suffix;
	}

	public boolean accept(File dir, String name)
	{
		boolean		isAccept		= true;

		if (null != m_prefix)
		{
			isAccept = name.startsWith(m_prefix);
		}
		if (null != m_contains)
		{
			isAccept &= name.contains(m_contains);
		}
		if (null != m_suffix)
		{
			isAccept &= name.endsWith(m_suffix);
		}
		return isAccept;
	}

	@Override
	public String toString()
	{
		return String.format("(%s,%s,%s)", m_prefix, m_contains, m_suffix);
	}
}
