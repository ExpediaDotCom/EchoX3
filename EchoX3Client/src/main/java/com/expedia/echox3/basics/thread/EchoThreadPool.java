/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.ring.RingBufferQueue;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractBaseThread.ThreadStatus;
import com.expedia.echox3.basics.tools.misc.BasicTools;

public class EchoThreadPool implements IEchoThreadPool//, PublisherManager.IEventListener
{
	private static final BasicLogger			LOGGER					= new BasicLogger(EchoThreadPool.class);

	private final String					m_name;

	private IOperationCounter				m_queueOperationCounter;
	private IOperationCounter				m_executionOperationCounter;
	private ObjectPool<RunnableWrapper>		m_wrapperObjectPool;

	private volatile int					m_threadCount			= 0;
	private AtomicInteger					m_activeCount			= new AtomicInteger(0);
	private volatile EchoPooledThread[]	m_activeThreadList		= new EchoPooledThread[0];
	private AtomicInteger					m_sleepingCount			= new AtomicInteger(0);
	private volatile EchoPooledThread[]	m_sleepingThreadList	= new EchoPooledThread[0];

	// Initialize to some ARBITRARY small size to facilitate startup and avoid null check everywhere,
	// especially in the getRunnable() method.
	private int								m_queueSizeMax			= 10;
	private RingBufferQueue<Runnable>		m_runnableList			= new RingBufferQueue<>(10);

	/**
	 * Initializes the Thread pool.
	 * Note that the pool is not operational until setThreadCount and setQueueSizeMax have been called.
	 *
	 * @param poolName		Unique name for this thread pool.
	 */
	public EchoThreadPool(String poolName)
	{
		m_name = poolName;

		m_queueOperationCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(
				new StringGroup(String.format("%s.%s.%s",
						EchoThreadPool.class.getSimpleName(),
						m_name, "Queue")),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time spent in the queue, before execution. Failure = rejected execution."
		);

		m_executionOperationCounter = CounterFactory.getInstance().getLogarithmicOperationCounter(
				new StringGroup(String.format("%s.%s.%s",
						EchoThreadPool.class.getSimpleName(),
						m_name, "Execution")),
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time spent in the executing. Failure = exception by the Runnable.");

		String						runnablePoolName	= RunnableWrapper.class.getSimpleName() + "." + poolName;
		m_wrapperObjectPool = new ObjectPool<>(new StringGroup(runnablePoolName), RunnableWrapper::new);
	}
	public EchoThreadPool(String poolName, int threadCount, int queueSizeMax)
	{
		this(poolName);

		setThreadCount(threadCount);
		setQueueSizeMax(queueSizeMax);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static EchoThreadPool createThreadPool(String poolName, int poolSize, int queueSize)
	{
		return new EchoThreadPool(poolName, poolSize, queueSize);
	}
	@Override
	public String getName()
	{
		return m_name;
	}
/*
	@Override
	public void receiveEvent(String publisherName, long timeMS, Object event)
	{
		if (ConfigurationManager.PUBLISHER_NAME.equals(publisherName))
		{
			updateConfiguration();
		}
	}
*/
	public void setThreadCount(int threadCount)
	{
		if (threadCount == m_threadCount)
		{
			return;
		}

		getLogger().info(BasicEvent.EVENT_THREAD_POOL_CONFIGURATION_CHANGE,
				"Pool %s: Thread count changed from %,d to %,d", getName(), m_threadCount, threadCount);

		List<EchoPooledThread>	threadToTerminateList		= new ArrayList<>(m_threadCount);
		synchronized (this)
		{
			if (threadCount > m_threadCount)
			{
				// Not enough threads,
				// New threads start in Running state and go to sleep if there is no work for them.
				// They are added to the array (list) of Running threads and nulls (aka slots)
				// are added to the array of Sleeping threads for them.

				// Create new running threads in the array of Running threads...
				// If there were 4 slots before and they were 2 active threads filled with 1, 2, null, null
				// ... with 2 sleeping threads (0 and 3)
				// AND there is an addition of two slots for a total of 6 threads
				// The active array will become 4, 5, 1, 2, null, null
				// In the example, the Sleeping array will become 0, 3, null, null, null, null
				// Add slots at the end of the array of sleeping threads...

				// Create the sleeping list first, as the newly created thread will quickly go to sleep
				// and need to land in this list.
				m_sleepingThreadList = Arrays.copyOf(m_sleepingThreadList, threadCount);

				EchoPooledThread[] threadList = new EchoPooledThread[threadCount];
				int newCount = threadCount - m_threadCount;
				for (int i = 0; i < newCount; i++)
				{
					threadList[i] = new EchoPooledThread(
							this, String.format("%s-%d", getName(), i + m_threadCount));
				}
				System.arraycopy(m_activeThreadList, 0, threadList, newCount, m_threadCount);
				m_activeThreadList = threadList;
				m_activeCount.addAndGet(newCount);
			}
			else if (threadCount < m_threadCount)
			{
				// Too many threads, terminate the extra ones, then shrink the arrays...
				// Need to find them :), as the total is some running and some sleeping
				// Keep the running ones first.
				// Going backwards in the example above, we want to keep end-up with
				// Active	4, 5, 1, 2, null, null
				// Sleeping	null(marked for termination), null(marked for termination), null, null, null, null
				// Then truncate both to 4 elements.
				int countKept = 0;
				for (int i = 0; i < m_threadCount; i++)
				{
					if (countKept == threadCount)
					{
						// Kept enough, terminate anything else...
						threadToTerminateList.add(m_activeThreadList[i]);
//						m_activeThreadList[i].setThreadStatus(ThreadStatus.ExitRequested);
						m_activeThreadList[i] = null;
						m_activeCount.decrementAndGet();
					}
					else if (null != m_activeThreadList[i])
					{
						// Keep more
						countKept++;
					}
				}
				for (int i = 0; i < m_threadCount; i++)
				{
					if (countKept == threadCount)
					{
						// Kept enough, terminate anything else...
						if (null != m_sleepingThreadList[i])
						{
							threadToTerminateList.add(m_sleepingThreadList[i]);
//							m_sleepingThreadList[i].setThreadStatus(ThreadStatus.ExitRequested);
							m_sleepingThreadList[i] = null;
							m_sleepingCount.decrementAndGet();
						}
					}
					else if (null != m_sleepingThreadList[i])
					{
						countKept++;
					}
				}
				m_activeThreadList = Arrays.copyOf(m_activeThreadList, threadCount);
				m_sleepingThreadList = Arrays.copyOf(m_sleepingThreadList, threadCount);
			}
			m_threadCount = threadCount;
		}

		// Outside the synchronized block to avoid deadlock with the TrellisPooledThread.run() method
		for (int i = 0; i < threadToTerminateList.size(); i++)
		{
			threadToTerminateList.get(i).setThreadStatus(ThreadStatus.ExitRequested);
		}
	}

	public void setQueueSizeMax(int size)
	{
		if (size == m_queueSizeMax)
		{
			return;
		}

		synchronized (this)
		{
			int							sizeCurrent		= getQueueSize();
			size = Math.max(size, sizeCurrent);			// Cannot shrink to lower than current size without loss

			RingBufferQueue<Runnable>	runnableList	= new RingBufferQueue<>(size);
			Runnable					runnable;
			while (null != (runnable = m_runnableList.poll()))
			{
				runnableList.offer(runnable);
			}

			m_wrapperObjectPool.setMaxSize(size);
			m_queueSizeMax = size;
			m_runnableList = runnableList;
		}

		getLogger().info(BasicEvent.EVENT_THREAD_POOL_CONFIGURATION_CHANGE,
				"Pool %s: Queue size changed from %,d to %,d", getName(), m_queueSizeMax, size);
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

		// Put the Runnable in the queue...
		RunnableWrapper			wrapper			= m_wrapperObjectPool.get();
		wrapper.setOperationCounter(m_executionOperationCounter);
		wrapper.setRunnable(runnable);
		IOperationContext		context			= m_queueOperationCounter.begin();

		try
		{
			wrapper.setQueueContext(context);
			m_runnableList.offer(wrapper);
		}
		catch (RuntimeException exception)
		{
			wrapper.release(false);
			throw exception;
		}

		// If any are sleeping, wake-up the next one
		// Operations under the lock of the Running/Sleeping lists.
		// Find the next one to wake-up...
		EchoPooledThread thread			= null;
		synchronized (this)
		{
			int sleepingCount = m_sleepingCount.get();    // Note the RO if all are awake
			if (sleepingCount > 0)        // Only if some are sleeping...
			{
				int sleepingIndex = m_sleepingCount.decrementAndGet();
				int activeIndex = m_activeCount.getAndIncrement();
				// Wake-up the thread at index sleepingIndex
				thread = m_sleepingThreadList[sleepingIndex];
				m_sleepingThreadList[sleepingIndex] = null;
				m_activeThreadList[activeIndex] = thread;
			}
		}

		// as needed, wake-up the next one.
		if (null != thread)
		{
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (thread)
			{
				// Wake-up the sleeping thread
//				System.out.println("Kicking sleeping thread");
				thread.setThreadStatus(ThreadStatus.Running);
				thread.notify();
			}
		}
	}
	// DANGER DANGER DANGER: The caller is responsible to ensure the call is done under synchronized(this).
	private void makeSleeping(EchoPooledThread thread)
	{
		// Remove it from the active thread list...
		int			actualIndex		= 0;
		boolean		isFound			= false;
		for (int i = 0; i < m_activeCount.get(); i++)
		{
			if (m_activeThreadList[i] != thread)		// NOPMD Looking for exactly this object
			{
				m_activeThreadList[actualIndex++] = m_activeThreadList[i];
			}
			else
			{
				isFound = true;
			}
		}

		// isFound needed for the possibility of a thread being removed (i.e. hot config decreasing)
		// and going to sleep at the same time
		if (isFound)
		{
			// The active list now has one less element
			m_activeThreadList[actualIndex] = null;
			m_activeCount.decrementAndGet();
		}

		// Put it in the sleeping list
		m_sleepingThreadList[m_sleepingCount.getAndIncrement()] = thread;
	}

	private Runnable getRunnable()
	{
		return m_runnableList.poll();
	}

	@Override
	public int getPoolSizeMax()
	{
		return m_threadCount;
	}

	@Override
	public int getQueueSizeMax()
	{
		return m_queueSizeMax;
	}

	@Override
	public int getQueueSize()
	{
		return m_runnableList.size();
	}

	public int getActiveCount()
	{
		return m_activeCount.get();
	}

	public int getSleepingCount()
	{
		return m_sleepingCount.get();
	}

	@Override
	public void shutdown()
	{
		while (!(m_threadCount == m_sleepingCount.get()))
		{
			BasicTools.sleepMS(10);
		}

		// Shutdown the threads that are all sleeping as all the tasks are completed.
		for (int i = 0; i < m_threadCount; i++)
		{
			EchoPooledThread		thread		= m_sleepingThreadList[i];
			if (null != thread)
			{
				thread.setThreadStatus(ThreadStatus.ExitRequested);
			}
		}

		if (null != m_wrapperObjectPool)
		{
			m_wrapperObjectPool.release();
			m_wrapperObjectPool = null;
		}
		m_queueOperationCounter.close();
		m_executionOperationCounter.close();
	}





	public static class RunnableWrapper extends AbstractPooledObject implements Runnable
	{
		private Runnable				m_wrappedRunnable;
		private IOperationContext		m_queueContext		= null;
		private IOperationCounter m_operationCounter;

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





	public class EchoPooledThread extends Thread
	{
		private final EchoThreadPool		m_threadPool;

		private ThreadStatus				m_threadStatus		= ThreadStatus.Starting;

		 private EchoPooledThread(EchoThreadPool threadPool, String name)
		{
			m_threadPool	= threadPool;

			setName(name);
			setDaemon(true);
			start();
		}

		public void setThreadStatus(ThreadStatus threadStatus)
		{
			if (!ThreadStatus.ExitRequested.equals(m_threadStatus)
					&& !ThreadStatus.Terminated.equals(m_threadStatus))
			{
				m_threadStatus = threadStatus;
			}

			if (ThreadStatus.ExitRequested.equals(threadStatus))
			{
				synchronized (this)
				{
					notify();
				}
			}
		}

		@Override
		public void run()
		{
			setThreadStatus(ThreadStatus.Running);

			while (true)
			{
				Runnable runnable;
				synchronized (this)
				{
					while (true)
					{
						synchronized (m_threadPool)
						{
							if (ThreadStatus.ExitRequested.equals(m_threadStatus))
							{
								m_threadStatus = ThreadStatus.Terminated;
								return;
							}

							// Get the Runnable inside the list lock, for proper synchronization with execute() above.
							// Otherwise, a Runnable could be inserted between the get and the makeSleeping.
							runnable = m_threadPool.getRunnable();
							if (null == runnable && !ThreadStatus.Waiting.equals(m_threadStatus))
							{
								setThreadStatus(ThreadStatus.Waiting);
								makeSleeping(this);
							}
						}

						if (null != runnable)
						{
							// This is the "normal" case where there is something in the queue
							break;
						}

						try
						{
							// This is the "normal" case where the queue is empty
							wait();
						}
						catch (InterruptedException e)
						{
							// Keep waiting
						}
					}
				}


				try
				{
					runnable.run();
				}
				catch (Exception exception)
				{
					getLogger().error(BasicEvent.EVENT_THREAD_POOL_UNEXPECTED_EXCEPTION, exception,
							"Pool %s: Unexpected exception while processing runnable.", getName());
				}
			}
		}
	}
}
