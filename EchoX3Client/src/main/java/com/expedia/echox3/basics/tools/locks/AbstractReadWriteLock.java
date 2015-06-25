/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;


import java.io.Closeable;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;

/**
 * Base class/interface definition for the ReadWrite lock used by EchoX3.
 * Timing tests shows major performance improvement over Java's ReentrantReadWriteLock.
 * Note that EchoX3's locks are NOT reentrant.
 *
 * Performance numbers... all numbers are for a READ Lock/Unlock cycle.
 * Numbers are on a MacBook pro with 4 physical cores running with HyperThreading enabled
 * for a total of 8 available cores.
 * 		Single threaded
 * 			ReentrantReadWriteLock		   28 ns
 * 			AtomicReadWriteLock			   35 ns
 *
 * 		Multi-threaded (2 threads + 1 slow write thread)
 * 			ReentrantReadWriteLock		  525 ns
 * 			AtomicReadWriteLock			  525 ns
 *
 * 		Multi-threaded (4 threads + 1 slow write thread)
 * 			ReentrantReadWriteLock		2,600 ns
 * 			AtomicReadWriteLock			  400 ns
 *
 * 		Multi-threaded (8 threads + 1 slow write thread)
 * 			ReentrantReadWriteLock		3,500 ns
 * 			AtomicReadWriteLock			  700 ns
 */
public abstract class AbstractReadWriteLock implements Closeable
{
	private final LockCounterFamily			m_counterFamily;

	protected AbstractReadWriteLock(LockCounterFamily counterFamily)
	{
		m_counterFamily = counterFamily;
	}

	public static AbstractReadWriteLock createReadWriteLock(LockCounterFamily counterFamily)
	{
		if (null == counterFamily)
		{
			return new FastReadWriteLock();
		}
		else
		{
			return new AtomicReadWriteLock(counterFamily);
		}
	}
	public static AbstractReadWriteLock createReadWriteLock()
	{
		return createReadWriteLock(null);
	}

	public LockCounterFamily getCounterFamily()
	{
		return m_counterFamily;
	}

	public abstract IOperationContext	lockRead();
	public abstract void				unlockRead(IOperationContext context, boolean isSuccess);
	public abstract IOperationContext	lockWrite();
	public abstract void				unlockWrite(IOperationContext context, boolean isSuccess);

	@Override
	public void close()
	{
		if (null != m_counterFamily)
		{
			m_counterFamily.close();
		}
	}
}
