/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools.misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;

public class LoadBalancer<T extends LoadBalancer.ILoadBalanced>
{
	public static final long					serialVersionUID	= 20150601085959L;

	private final String					m_name;
	private final AbstractReadWriteLock		m_lock		= AbstractReadWriteLock.createReadWriteLock();
	private final List<T>					m_list		= new ArrayList<>();
	private final AtomicInteger				m_next		= new AtomicInteger();

	public LoadBalancer(String name)
	{
		m_name = name;
	}

	public void add(T t)
	{
		IOperationContext		context		= m_lock.lockWrite();
		m_list.add(t);
		m_lock.unlockWrite(context, true);
	}
	public void remove(T t)
	{
		IOperationContext		context		= m_lock.lockWrite();
		m_list.remove(t);
		m_lock.unlockWrite(context, true);
	}
	public void clear()
	{
		IOperationContext		context		= m_lock.lockWrite();
		m_list.clear();
		m_lock.unlockWrite(context, true);
	}

	public int size()
	{
		return m_list.size();
	}

	public T getNext(T prefer)
	{
		IOperationContext		context		= m_lock.lockRead();
		T						element		= null;
		int						load		= Integer.MAX_VALUE;

		int		iMax		= m_list.size();
		int		next		= m_next.getAndIncrement() % iMax;
		for(int i = 0; i < iMax; i++)
		{
			if (next >= iMax)
			{
				next = 0;
			}

			T		t		= m_list.get(next++);
			if (!t.isActive())
			{
				continue;
			}
			if (t == prefer)		// NOPMD
			{
				element = t;
				break;
			}

			int		l		= t.getLoad();
			if (l < load)
			{
				element = t;
				load = l;
			}
		}

		 m_lock.unlockRead(context, null != element);

		return element;
	}

	public List<T> getList()
	{
		return m_list;
	}

	@Override
	public String toString()
	{
		return String.format("%s.%s(%,d items)", LoadBalancer.class.getSimpleName(), m_name, m_list.size());
	}

	public interface ILoadBalanced extends Serializable
	{
		boolean		isActive();
		int			getLoad();
	}
}
