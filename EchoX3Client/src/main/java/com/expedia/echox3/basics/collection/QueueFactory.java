/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.expedia.echox3.basics.collection.ring.RingBufferQueue;

public class QueueFactory<T>
{
	public BlockingQueue<T> getBlockingQueue(int size)
	{
		if (0 == size)
		{
			return getUnboundQueue();
		}
		else
		{
			return getArrayBlockingQueue(size);
		}
	}

	public Queue<T> getQueue(int size)
	{
		if (0 == size)
		{
			return getUnboundQueue();
		}
		else
		{
			return getArrayBlockingQueue(size);
		}
	}

	public BlockingQueue<T> getUnboundQueue()
	{
		return new LinkedBlockingQueue<>();
	}

	public BlockingQueue<T> getArrayBlockingQueue(int size)
	{
		return new ArrayBlockingQueue<>(size, true);
	}

	public Queue<T> getRingBufferQueue(int size)
	{
		return new RingBufferQueue<>(size);
	}
}
