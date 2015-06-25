/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.test;

import java.io.Serializable;

import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class TestObject implements ICacheObject
{
	public static final long					serialVersionUID	= 20150601085959L;

	private int				m_version			= 0;
	private long			m_ageMaxMS;
	private long			m_lastWriteTimeMS	= 0;
//	private long			m_lastReadTimeMS	= 0;
	private String			m_value;

	@Override
	public void updateConfiguration(ObjectCacheConfiguration configuration)
	{
		// Nothing to do, this object does not use configuration
	}

	@Override
	public void flush(long timeNowMS, long timeFlushMS)
	{
		// Not supported, flush immediately.
		m_value = null;
	}

	@Override
	public void doMaintenance(long timeNowMS, int memoryLevelPercent)
	{
		// Clear any obsolete data.
		// NOTE: Adjust the ageMaxMS for the memoryLevelPercent
		long		ageMaxMS		= m_ageMaxMS * memoryLevelPercent / 100;
		if (timeNowMS < (m_lastWriteTimeMS + ageMaxMS))
		{
			m_value = null;
		}
	}

	@Override
	public boolean canDelete()
	{
		return null == m_value;
	}

	@Override
	public long getSize()
	{
		return m_value.length();
	}

	@Override
	public void writeOnly(Serializable request)
	{
		TestWriteRequest		writeRequest		= (TestWriteRequest) request;
		writeRequest.work();
		m_version++;
		m_value				= writeRequest.getValue();
		m_ageMaxMS			= writeRequest.getAgeMaxMS();
		m_lastWriteTimeMS	= System.currentTimeMillis();
//		m_lastReadTimeMS	= System.currentTimeMillis();
	}

	@Override
	public Serializable readOnly(Serializable request)
	{
		if (!(request instanceof TestReadRequest))
		{
			return null;
		}
		if (null == m_value)
		{
			return null;
		}

		TestReadRequest		readRequest		= (TestReadRequest) request;
		readRequest.work();

//		m_lastReadTimeMS = System.currentTimeMillis();

		return new TestReadResponse(m_version, System.currentTimeMillis(), m_value);
	}

	@Override
	public void upgradeFrom(ICacheObject cacheObject)
	{
		// NYI
	}

	@Override
	public String toString()
	{
		return String.format("%s(V-%,d - %s) = %s",
				TestObject.class.getSimpleName(), m_version,
				TimeUnits.formatMS(m_lastWriteTimeMS),
				(null == m_value ? "Not set" : m_value));
	}
}
