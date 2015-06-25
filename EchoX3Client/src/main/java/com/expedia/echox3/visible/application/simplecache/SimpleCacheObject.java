/**
 * Copyright 2014-2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.simplecache;

import java.io.Serializable;

import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;
import com.expedia.echox3.visible.application.simplecache.SimpleCacheStatusHolder.TimeType;

/**
 * This object is a full-featured CacheObject that implements the simple cache mode.
 */
public class SimpleCacheObject implements ICacheObject
{
	public static final long					serialVersionUID	= 20150601085959L;

	// This is a pointer to the master object held by the factory object.
	private SimpleCacheStatusHolder m_cacheStatus;

	private byte[]		m_data;
	private long		m_writeTimeMS;
	private long		m_readTimeMS;
	private long		m_expirationTimeMS;

	public SimpleCacheObject(SimpleCacheStatusHolder cacheStatus)
	{
		m_cacheStatus = cacheStatus;
		m_writeTimeMS = WallClock.getCurrentTimeMS();
		m_readTimeMS = m_writeTimeMS;
		adjustExpirationTime();
	}

	@Override
	public void updateConfiguration(ObjectCacheConfiguration configuration)
	{
		// Note: the object holds a reference to the object owned and updated by the factory.
		adjustExpirationTime();
	}

	@Override
	public void flush(long timeNowMS, long timeFlushMS)
	{
		long		flushTime		= timeFlushMS - m_cacheStatus.getAgeMaxMS();
		m_readTimeMS = Math.min(m_readTimeMS, flushTime);
		m_writeTimeMS = Math.min(m_writeTimeMS, flushTime);
		adjustExpirationTime();
	}

	@Override
	public void doMaintenance(long timeNowMS, int memoryLevelPercent)
	{
		long		adjustedAgeMax	= (m_cacheStatus.getAgeMaxMS() * memoryLevelPercent) / 100;
		long		objectTimeMS	= getObjectTimeMS();
		long		objectAge		= timeNowMS - objectTimeMS;

		if (objectAge > adjustedAgeMax)
		{
			if (null != m_data)
			{
				m_cacheStatus.getValueSizeCounter().remove(m_data.length);
				m_data = null;
			}
		}
	}
	private long getObjectTimeMS()
	{
		return TimeType.TimeRead.equals(m_cacheStatus.getTimeType()) ? m_readTimeMS : m_writeTimeMS;
	}
	private void adjustExpirationTime()
	{
		long		objectTimeMS		= getObjectTimeMS();
		m_expirationTimeMS = objectTimeMS + m_cacheStatus.getAgeMaxMS();
	}

	@Override
	public boolean canDelete()
	{
		return null == m_data;
	}

	@Override
	public long getSize()
	{
		return m_data.length;
	}

	@Override
	public void writeOnly(Serializable request)
	{
		if (request instanceof byte[])
		{
			if (null != m_data)
			{
				// Remove previous value which will be overwritten
				m_cacheStatus.getValueSizeCounter().remove(m_data.length);
			}
			m_data = (byte[]) request;
			m_cacheStatus.getValueSizeCounter().add(m_data.length);
			m_writeTimeMS = WallClock.getCurrentTimeMS();
			m_readTimeMS = m_writeTimeMS;

			m_expirationTimeMS = m_writeTimeMS + m_cacheStatus.getAgeMaxMS();
		}
		else if (null == request)
		{
			if (null != m_data)
			{
				m_cacheStatus.getValueSizeCounter().remove(m_data.length);
				m_data = null;
			}
		}
	}

	@Override
	public Serializable readOnly(Serializable request)
	{
		m_readTimeMS = WallClock.getCurrentTimeMS();
		if (TimeType.TimeRead.equals(m_cacheStatus.getTimeType()))
		{
			m_expirationTimeMS = m_readTimeMS + m_cacheStatus.getAgeMaxMS();
		}
		if (WallClock.getCurrentTimeMS() > m_expirationTimeMS)
		{
			if (null != m_data)
			{
				m_cacheStatus.getValueSizeCounter().remove(m_data.length);
				m_data = null;
			}
		}

		return m_data;
	}

	@Override
	public void upgradeFrom(ICacheObject cacheObject)
	{
		// Does not do much, as it upgrades from itself. It can be used for testing.
		if (cacheObject instanceof SimpleCacheObject)
		{
			SimpleCacheObject blobObject		= (SimpleCacheObject) cacheObject;
			m_data				= blobObject.m_data;
			m_readTimeMS		= blobObject.m_readTimeMS;
			m_writeTimeMS		= blobObject.m_writeTimeMS;
			m_expirationTimeMS	= blobObject.m_expirationTimeMS;
		}
	}

	public byte[] getData()
	{
		return m_data;
	}

	public long getWriteTimeMS()
	{
		return m_writeTimeMS;
	}

	public long getReadTimeMS()
	{
		return m_readTimeMS;
	}

	public long getExpirationTimeMS()
	{
		return m_expirationTimeMS;
	}
}
