/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;


public class BasicReadWriteLock extends AbstractReadWriteLock
{
	private final ReentrantReadWriteLock m_lock				= new ReentrantReadWriteLock(true);


	public BasicReadWriteLock(LockCounterFamily counterFamily)
	{
		super(counterFamily);
	}

	@Override
	public IOperationContext lockRead()
	{
		IOperationContext context		= getCounterFamily().beginReadWait();
		m_lock.readLock().lock();
		getCounterFamily().end(context, true);

		return getCounterFamily().beginReadHold();
	}
	@Override
	public void unlockRead(IOperationContext context, boolean isSuccess)
	{
		m_lock.readLock().unlock();
		getCounterFamily().end(context, isSuccess);
	}
	@Override
	public IOperationContext lockWrite()
	{
		IOperationContext		context		= getCounterFamily().beginWriteWait();
		m_lock.writeLock().lock();
		getCounterFamily().end(context, true);
		return getCounterFamily().beginWriteHold();
	}
	@Override
	public void unlockWrite(IOperationContext context, boolean isSuccess)
	{
		m_lock.writeLock().unlock();
		getCounterFamily().end(context, isSuccess);
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", getClass().getSimpleName(), getCounterFamily().getName().toString());
	}
}
