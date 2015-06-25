/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.test;

public class TestWriteRequest extends TestReadRequest
{
	public static final long				serialVersionUID	= 20140101085959L;

	// == 5,000 ms == 5 seconds after last write
	private static final long				AGE_MAX_MS			= 5 * 1000;

	private long		m_ageMaxMS		= AGE_MAX_MS;
	private String		m_value;

	public TestWriteRequest(int burnUS, int sleepMS, boolean doThrow, String value)
	{
		this(burnUS, sleepMS, doThrow, value, AGE_MAX_MS);
	}
	public TestWriteRequest(int burnUS, int sleepMS, boolean doThrow, String value, long ageMaxMS)
	{
		super(burnUS, sleepMS, doThrow);
		m_value = value;
		m_ageMaxMS = ageMaxMS;
	}

	public void setValue(String value)
	{
		m_value = value;
	}

	public String getValue()
	{
		return m_value;
	}

	public long getAgeMaxMS()
	{
		return m_ageMaxMS;
	}
}
