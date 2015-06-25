/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.test;

import java.io.Serializable;

import com.expedia.echox3.basics.tools.time.TimeUnits;

public class TestReadResponse implements Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private int			m_version;
	private long		m_currentTimeMS;
	private String		m_value;

	public TestReadResponse(int version, long currentTimeMS, String value)
	{
		m_version = version;
		m_currentTimeMS = currentTimeMS;
		m_value = value;
	}

	public int getVersion()
	{
		return m_version;
	}

	public long getCurrentTimeMS()
	{
		return m_currentTimeMS;
	}

	public String getValue()
	{
		return m_value;
	}

	@Override
	public String toString()
	{
		return String.format("%s(V-%,d), last modified %s = %s",
				TestReadResponse.class.getSimpleName(), m_version, TimeUnits.formatMS(m_currentTimeMS),
				(null == m_value ? "Not set" : m_value));
	}
}
