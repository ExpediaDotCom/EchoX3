/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.store.wrapper;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.tools.locks.LockCounterFamily;
import com.expedia.echox3.internal.store.counter.ItemCounterFamily;

public class TrackingObjects
{
	protected ItemCounterFamily		m_counterFamily				= null;
	private LockCounterFamily		m_bucketListLockCounter		= null;
	private LockCounterFamily		m_binListLockCounter		= null;
	private LockCounterFamily		m_binMapLockCounter			= null;
	private LockCounterFamily		m_itemLockCounter			= null;

	public TrackingObjects(String cacheName)
	{
		String[]		nameList		= new String[3];
		nameList[0] = "Cache";
		nameList[2] = cacheName;

		nameList[1] = "BucketList";
		m_bucketListLockCounter = new LockCounterFamily(new StringGroup(nameList));

		nameList[1] = "BinList";
		m_binListLockCounter = new LockCounterFamily(new StringGroup(nameList));

		nameList[1] = "BinMap";
		m_binMapLockCounter = new LockCounterFamily(new StringGroup(nameList));

		nameList[1] = "CacheItem";
		m_itemLockCounter = new LockCounterFamily(new StringGroup(nameList));

		m_counterFamily = new ItemCounterFamily(cacheName);
	}

	public LockCounterFamily getBucketListLockCounter()
	{
		return m_bucketListLockCounter;
	}

	public LockCounterFamily getBinListLockCounter()
	{
		return m_binListLockCounter;
	}

	public LockCounterFamily getBinMapLockCounter()
	{
		return m_binMapLockCounter;
	}

	public LockCounterFamily getItemLockCounter()
	{
		return m_itemLockCounter;
	}

	public ItemCounterFamily getCounterFamily()
	{
		return m_counterFamily;
	}

	public void close()
	{
		getBucketListLockCounter().close();
		getBinListLockCounter().close();
		getBinMapLockCounter().close();
		getItemLockCounter().close();

		getCounterFamily().close();
	}
}
