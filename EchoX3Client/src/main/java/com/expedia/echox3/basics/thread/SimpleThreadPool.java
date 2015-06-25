/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;

public class SimpleThreadPool extends ThreadPoolExecutor implements IEchoThreadPool
{
	private final String						m_poolName;
	private final BlockingQueue<Runnable>		m_workQueue;
	private int									m_queueSizeMax;
	private int									m_poolSizeMax;
	private IOperationCounter					m_queueOperationCounter;
	private IOperationCounter					m_executionOperationCounter;
	private ObjectPool<RunnableWrapper>			m_wrapperObjectPool;

	/**
	 * Creates a new {@code ThreadPoolExecutor} with the given initial
	 * parameters and default rejected execution handler.
	 *
	 * @param corePoolSize    the number of threads to keep in the pool, even
	 *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 *                        pool
	 * @param keepAliveTime   when the number of threads is greater than
	 *                        the core, this is the maximum time that excess idle threads
	 *                        will wait for new tasks before terminating.
	 * @param unit            the time unit for the {@code keepAliveTime} argument
	 * @param workQueue       the queue to use for holding tasks before they are
	 *                        executed.  This queue will hold only the {@code Runnable}
	 *                        tasks submitted by the {@code execute} method.
	 * @param threadFactory   the factory to use when the executor
	 *                        creates a new thread
	 * @throws IllegalArgumentException if one of the following holds:<br>
	 *                                            {@code corePoolSize < 0}<br>
	 *                                            {@code keepAliveTime < 0}<br>
	 *                                            {@code maximumPoolSize <= 0}<br>
	 *                                            {@code maximumPoolSize < corePoolSize}
	 * @throws NullPointerException     if {@code workQueue}
	 *                                            or {@code threadFactory} is null
	 */
	private SimpleThreadPool(String poolName, int corePoolSize, int maximumPoolSize,
							long keepAliveTime, TimeUnit unit,
							BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);

		m_poolName = poolName;
		m_workQueue = workQueue;

		m_queueOperationCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(
				new StringGroup(String.format("%s.%s.%s",
						SimpleThreadPool.class.getSimpleName(),
						m_poolName, "Queue")),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time spent in the queue, before execution. Failure = rejected execution."
		);
		m_executionOperationCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(
				new StringGroup(String.format("%s.%s.%s",
						SimpleThreadPool.class.getSimpleName(),
						m_poolName, "Execution")),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time spent in the executing. Failure = exception by the Runnable.");

		String						runnablePoolName	= RunnableWrapper.class.getSimpleName() + "." + poolName;
		m_wrapperObjectPool = new ObjectPool<>(new StringGroup(runnablePoolName), RunnableWrapper::new);

		prestartAllCoreThreads();
	}

	/**
	 * A standard, easy to create, ThreadPoolExecutor ...
	 * ... with added counter for the queue and the executing threads.
	 *
	 * Timing information, on a typical 2.x GHz/core system (similar on a desktop, laptop or production server)
	 * Single thread pool:
	 * 		about  50K QPS
	 * 		about 250K QPS on the 4 cores (+ Hyper-threading system)
	 * 			TBD if this scales with number of cores.
	 *
	 * @param poolName		Prefix to the name of the threads
	 * @param poolSize		Number of thread per pool
	 * @param queueSize		Size of the queue for each pool.
	 * @return				The newly created SimpleThreadPool (implements Executor)
	 */
	public static SimpleThreadPool createThreadPool(String poolName, int poolSize, int queueSize)
	{
		BlockingQueue<Runnable> queue;
		if (0 == queueSize)
		{
			queue = new LinkedBlockingQueue<>();
		}
		else
		{
			queue = new ArrayBlockingQueue<>(queueSize, true);
		}

		ThreadFactory factory = new SimpleThreadFactory(poolName, poolSize);
		SimpleThreadPool	pool =
				new SimpleThreadPool(poolName, poolSize, poolSize, 0, TimeUnit.SECONDS, queue, factory);
		pool.m_queueSizeMax	= queueSize;
		pool.m_poolSizeMax	= poolSize;

		return pool;
	}

	/**
	 * A ThreadPoolGroup is an array of SimpleThreadPool.
	 * execute() calls are round robin distributed amongst the pools to avoid the performance limitation
	 * of the single thread pool.
	 *
	 * Timing information, on a typical 2.x GHz/core system (similar on a desktop, laptop or production server)
	 * Single thread pool:
	 * 		about  50K QPS
	 * 		about 250K QPS on the 4 cores (+ Hyper-threading system)
	 * 			TBD if this scales with number of cores.
	 * 			(Possibly limited by the speed of the thread submitting the Runnable for the test)
	 *
	 * @param poolName		Prefix to the name of the threads
	 * @param poolCount		Number of thread pools in the group
	 * @param poolSize		Number of thread per pool
	 * @param queueSize		Size of the queue for each pool.
	 * @return				The newly created ThreadPoolGroup (implements Executor)
	 */
	public static ThreadPoolGroup createThreadPoolGroup(String poolName, int poolCount, int poolSize, int queueSize)
	{
		return new ThreadPoolGroup(poolName, poolCount, poolSize, queueSize);
	}

	@Override
	public String getName()
	{
		return m_poolName;
	}

	@Override
	public int getPoolSizeMax()
	{
		return m_poolSizeMax;
	}
	@Override
	public int getQueueSizeMax()
	{
		return m_queueSizeMax;
	}
	@Override
	public int getQueueSize()
	{
		return m_workQueue.size();
	}

	@Override
	public void execute(Runnable runnable)
	{
		if (null == m_wrapperObjectPool)
		{
			// The thread pool is shutdown!
			throw new RejectedExecutionException(String.format("%s(%s) is shutdown.",
					getClass().getSimpleName(), getName()));
		}
		RunnableWrapper			wrapper			= m_wrapperObjectPool.get();
		wrapper.setOperationCounter(m_executionOperationCounter);
		wrapper.setRunnable(runnable);
		IOperationContext		context			= m_queueOperationCounter.begin();

		try
		{
			wrapper.setQueueContext(context);
			super.execute(wrapper);
		}
		catch (RuntimeException exception)
		{
			wrapper.release(false);
			throw exception;
		}
	}

	@Override
	public Future<?> submit(Runnable runnable)
	{
		throw new UnsupportedOperationException("Use execute() instead of submit.");
	}

	@Override
	public void shutdown()
	{
		super.shutdown();

		if (null != m_wrapperObjectPool)
		{
			m_wrapperObjectPool.release();
			m_wrapperObjectPool = null;
		}

		if (null != m_queueOperationCounter)
		{
			m_queueOperationCounter.close();
			m_queueOperationCounter = null;
			m_executionOperationCounter.close();
			m_executionOperationCounter = null;
		}
	}



	/**
	 * The main purpose of this class is to name the threads appropriately.
	 *
	 * @author Pierre
	 *
	 */
	private static class SimpleThreadFactory implements ThreadFactory
	{
		private final String		m_name;
		private int 				m_poolSize;
		private int					m_count = 0;

		public SimpleThreadFactory(String name, int poolSize)
		{
			m_name = name;
			m_poolSize = poolSize;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
		 */
		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(r);

			t.setName(String.format("%s-%,d/%,d", m_name, m_count++, m_poolSize));
			t.setDaemon(true);
//			t.setPriority(Thread.MAX_PRIORITY);

			return t;
		}
	}

	public static class RunnableWrapper extends AbstractPooledObject implements Runnable
	{
		private Runnable				m_wrappedRunnable;
		private IOperationContext		m_queueContext		= null;
		private IOperationCounter		m_operationCounter;

		public RunnableWrapper()
		{

		}

		@Override
		public void release()
		{
			release(false);		// Error, because bug-free code should call release(boolean)
		}
		public void release(boolean isSuccess)
		{
			if (null != m_queueContext)
			{
				m_queueContext.end(isSuccess);
				m_queueContext = null;
			}
			m_wrappedRunnable = null;
			super.release();
		}

		private void setOperationCounter(IOperationCounter operationCounter)
		{
			m_operationCounter = operationCounter;
		}

		private void setQueueContext(IOperationContext queueContext)
		{
			m_queueContext = queueContext;
		}

		private void setRunnable(Runnable wrappedRunnable)
		{
			m_wrappedRunnable = wrappedRunnable;
		}

		@Override
		public void run()
		{
			if (null != m_queueContext)
			{
				m_queueContext.end(true);
				m_queueContext = null;
			}

			IOperationContext	executeContext	= m_operationCounter.begin();
			boolean				isSuccess		= false;
			try
			{
				m_wrappedRunnable.run();
				isSuccess = true;
			}
			finally
			{
				executeContext.end(isSuccess);
				release(true);
			}
		}
	}


	public static final class ThreadPoolGroup implements IEchoThreadPool
	{
		private final String			m_poolName;
		private IEchoThreadPool[]		m_threadPoolList;
		private final int				m_poolCount;
		private AtomicInteger			m_nextPool			= new AtomicInteger(0);

		private ThreadPoolGroup(String poolName,int poolCount, int poolSize, int queueSize)
		{
			m_poolName			= poolName;
			m_poolCount			= poolCount;
			m_threadPoolList	= new IEchoThreadPool[poolCount];
			for (int i = 0; i < m_poolCount; i++)
			{
				String		fullPoolName		= String.format("%s-%d", poolName, i);
				m_threadPoolList[i] = SimpleThreadPool.createThreadPool(fullPoolName, poolSize, queueSize);
			}
		}

		@Override
		public String getName()
		{
			return m_poolName;
		}

		@Override
		public void execute(Runnable runnable)
		{
			int					nextPool		= Math.abs(m_nextPool.getAndIncrement() % m_poolCount);
			IEchoThreadPool threadPool		= m_threadPoolList[nextPool];
			int					load			= threadPool.getQueueSize();

			// In a round robin fashion (first one wins) find the Thread pool with the lowest load.
			// This is to avoid overloading a pool that is randomly more busy than the others.
			for (int i = 1; i < m_poolCount; i++)		// Start at 1 because pool nextPool has already been used
			{
				nextPool++;
				if (nextPool >= m_poolCount)
				{
					nextPool = 0;
				}
				IEchoThreadPool pool	= m_threadPoolList[nextPool];
				if (pool.getQueueSize() < load)
				{
					threadPool = pool;
					load = threadPool.getQueueSize();
				}
			}

			threadPool.execute(runnable);
		}

		@Override
		public int getPoolSizeMax()
		{
			int			size		= 0;
			for (int i = 0; i < m_poolCount; i++)
			{
				size += m_threadPoolList[i].getPoolSizeMax();
			}
			return size;
		}

		@Override
		public int getQueueSizeMax()
		{
			int			size		= 0;
			for (int i = 0; i < m_poolCount; i++)
			{
				size += m_threadPoolList[i].getQueueSizeMax();
			}
			return size;
		}

		@Override
		public int getQueueSize()
		{
			int			size		= 0;
			for (int i = 0; i < m_poolCount; i++)
			{
				size += m_threadPoolList[i].getQueueSize();
			}
			return size;
		}

		@Override
		public void shutdown()
		{
			for (int i = 0; i < m_poolCount; i++)
			{
				m_threadPoolList[i].shutdown();
			}
		}
	}
}
