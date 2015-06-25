/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.ring;

import java.util.NoSuchElementException;
import java.util.Queue;

public class RingBufferQueue<T> extends RingBufferCollection<T> implements Queue<T>
{
	@SuppressWarnings("unchecked")
	public RingBufferQueue(int count)
	{
		super(count);
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do
	 * so immediately without violating capacity restrictions.
	 * When using a capacity-restricted queue, this method is generally
	 * preferable to {@link #add}, which can fail to insert an element only
	 * by throwing an exception.
	 *
	 * @param value the element to add
	 * @return <tt>true</tt> if the element was added to this queue, else
	 * <tt>false</tt>
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this queue
	 * @throws NullPointerException     if the specified element is null and
	 *                                  this queue does not permit null elements
	 * @throws IllegalArgumentException if some property of this element
	 *                                  prevents it from being added to this queue
	 */
	@Override
	public boolean offer(T value)
	{
		return addInternal(value);
	}

	/**
	 * Retrieves and removes the head of this queue.  This method differs
	 * from {@link #poll poll} only in that it throws an exception if this
	 * queue is empty.
	 *
	 * @return the head of this queue
	 * @throws java.util.NoSuchElementException if this queue is empty
	 */
	@Override
	public T remove()
	{
		T		value		= poll();
		if (null == value)
		{
			throw new NoSuchElementException("The queue is empty");
		}
		return value;
	}

	/**
	 * Retrieves and removes the head of this queue,
	 * or returns <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty
	 */
	@Override
	public T poll()
	{
		return removeInternal();
	}

	/**
	 * Retrieves, but does not remove, the head of this queue.  This method
	 * differs from {@link #peek peek} only in that it throws an exception
	 * if this queue is empty.
	 *
	 * @return the head of this queue
	 * @throws java.util.NoSuchElementException if this queue is empty
	 */
	@Override
	public T element()
	{
		T		value		= peek();
		if (null == value)
		{
			throw new NoSuchElementException("The queue is empty");
		}
		return value;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue,
	 * or returns <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty
	 */
	@Override
	public T peek()
	{
		return peekInternal();
	}
}
