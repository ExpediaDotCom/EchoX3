/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;

/**
 * This is a thin wrapper implementing the Java ReentrantReadWriteLock as a AbstractReadWriteLock,
 * for compatibility with the EchoX3 code and comparative timing tests.
 * This lock has NO counters.
 */
public class PlainReadWriteLock extends AbstractReadWriteLock
{
	private final ReentrantReadWriteLock		m_lock				= new ReentrantReadWriteLock(true);

	public PlainReadWriteLock()
	{
		super(null);
	}

	@Override
	public IOperationContext lockRead()
	{
		m_lock.readLock().lock();

		return null;
	}
	@Override
	public void unlockRead(IOperationContext context, boolean isSuccess)
	{
		m_lock.readLock().unlock();
	}
	@Override
	public IOperationContext lockWrite()
	{
		m_lock.writeLock().lock();
		return null;
	}
	@Override
	public void unlockWrite(IOperationContext context, boolean isSuccess)
	{
		m_lock.writeLock().unlock();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
