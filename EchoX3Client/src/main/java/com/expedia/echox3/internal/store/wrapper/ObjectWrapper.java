/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.wrapper;

import java.util.Arrays;

import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.basics.tools.locks.LockCounterFamily;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.visible.trellis.ICacheObject;

/**
 * Responsible for Hash -> Direction at all routing levels (always use Math.abs(int))
 * 		Director			Select the bucket		= High 32 bits of m_hash64
 * 		LocalObjectBucket		Select the bin			= (High + Low) of m_hash64
 * 		LocalObjectBin		simple Java hashCode	= Low 32 bits of m_hash64
 *
 * 	NOTE: This object is NOT pooled.
 */
public class ObjectWrapper extends ObjectKey
{
	private AbstractReadWriteLock		m_lock;
	private long						m_lastModifiedMS;
	private ICacheObject				m_trellisObject;

	public ObjectWrapper(ObjectKey key, ICacheObject object)
	{
		ByteArrayWrapper	wrapper			= key.getWrappedKey();
		byte[]				keyBytes		= Arrays.copyOfRange(
				wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
		setKeyBytes(keyBytes, key.getHash64());

		m_lastModifiedMS = WallClock.getCurrentTimeMS();
		m_trellisObject = object;
	}

	public void setLastModifiedMS(long lastModifiedMS)
	{
		m_lastModifiedMS = lastModifiedMS;
	}

	public long getLastModifiedMS()
	{
		return m_lastModifiedMS;
	}

	public void createLock(LockCounterFamily counterFamily)
	{
		m_lock = AbstractReadWriteLock.createReadWriteLock(counterFamily);
	}

	public AbstractReadWriteLock getLock()
	{
		return m_lock;
	}

	public ICacheObject getTrellisObject()
	{
		return m_trellisObject;
	}

	public void setTrellisObject(ICacheObject trellisObject)
	{
		m_trellisObject = trellisObject;
	}
}
