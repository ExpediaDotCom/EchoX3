/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.collection.ring.RingBufferQueue;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RingBufferQueueTests extends AbstractTestTools
{
	private static final QueueFactory<Integer>		QUEUE_FACTORY		= new QueueFactory<>();

	@Test
	public void testInteger() throws BasicException
	{
		logTestName();

		RingBufferQueue<Integer> ringBuffer		= new RingBufferQueue<>(3);
		assertTrue(ringBuffer.offer(1));
		assertTrue(1 == ringBuffer.poll());
		assertTrue(ringBuffer.offer(2));
		assertTrue(ringBuffer.offer(3));
		assertEquals(2, ringBuffer.size());
		assertTrue(2 == ringBuffer.poll());
		assertTrue(3 == ringBuffer.poll());

		assertTrue(ringBuffer.offer(4));
		assertTrue(ringBuffer.offer(5));
		assertTrue(4 == ringBuffer.poll());
		assertTrue(5 == ringBuffer.poll());

		assertTrue(ringBuffer.offer(6));
		assertTrue(ringBuffer.offer(7));
		assertTrue(ringBuffer.offer(8));
		assertEquals(3, ringBuffer.size());
		assertTrue(6 == ringBuffer.poll());
		assertTrue(7 == ringBuffer.poll());
		assertTrue(8 == ringBuffer.poll());
	}

	@Test
	public void testString() throws BasicException
	{
		logTestName();

		RingBufferQueue<String> ringBuffer		= new RingBufferQueue<>(3);
		assertTrue(ringBuffer.offer("1"));
		validateIterator(ringBuffer, new String[] { "1" });
		assertEquals("1", ringBuffer.poll());

		assertTrue(ringBuffer.offer("2"));
		assertTrue(ringBuffer.offer("3"));
		validateIterator(ringBuffer, new String[] { "2", "3" });
		assertEquals("2", ringBuffer.poll());
		assertEquals("3", ringBuffer.poll());

		assertTrue(ringBuffer.offer("4"));
		assertTrue(ringBuffer.offer("5"));
		validateIterator(ringBuffer, new String[] { "4", "5" });
		assertEquals("4", ringBuffer.poll());
		assertEquals("5", ringBuffer.poll());

		assertTrue(ringBuffer.offer("6"));
		assertTrue(ringBuffer.offer("7"));
		assertTrue(ringBuffer.offer("8"));
		validateIterator(ringBuffer, new String[] { "6", "7", "8" });
		assertEquals("6", ringBuffer.poll());
		assertEquals("7", ringBuffer.poll());
		assertEquals("8", ringBuffer.poll());
	}
	private void validateIterator(Collection<?> actualList, Object[] expectedList)		// NOPMD: It is used
	{
		int		expectedIndex		= 0;
		for (Object actual : actualList)
		{
			Object expected		= expectedList[expectedIndex++];
			assertEquals(expected, actual);
		}
	}


	@Test
	public void testAddRingBuffer() throws BasicException
	{
		logTestName();

		RingBufferQueue<String> ringBuffer1		= new RingBufferQueue<>(5);
		assertTrue(ringBuffer1.offer("1"));
		assertTrue(ringBuffer1.offer("2"));
		RingBufferQueue<String> ringBuffer2		= new RingBufferQueue<>(5);
		assertTrue(ringBuffer2.offer("3"));
		assertTrue(ringBuffer2.offer("4"));
		assertTrue(ringBuffer1.addAll(ringBuffer2));

		assertEquals("1", ringBuffer1.poll());
		assertEquals("2", ringBuffer1.poll());
		assertEquals("3", ringBuffer1.poll());
		assertEquals("4", ringBuffer1.poll());
	}

	@Test
	public void testOverflow() throws BasicException
	{
		logTestName();

		RingBufferQueue<String> ringBuffer		= new RingBufferQueue<>(2);
		assertTrue(ringBuffer.offer("1"));
		assertTrue(ringBuffer.offer("2"));

		assertFalse(ringBuffer.offer("3"));
	}

	@Test
	public void testPerformance() throws BasicException
	{
		logTestName();

		for (int i = 0; i < 20; i++)
		{
			measureQueuePerformance(QUEUE_FACTORY.getArrayBlockingQueue(100), 31, 2 * 1000);
		}
		for (int i = 0; i < 20; i++)
		{
			measureQueuePerformance(QUEUE_FACTORY.getRingBufferQueue(100), 29, 2 * 1000);
		}
	}
	private static void measureQueuePerformance(Queue<Integer> queue, int items, int iterations) throws BasicException
	{
		queue.clear();

		int			addIndex			= 0;
		int			takeIndex			= 0;
		// Place that many items in the buffer...
		for (int i = 0; i < items; i++)
		{
			assertTrue(queue.offer(addIndex++));
		}
		assertEquals(items, queue.size());

		long		t1		= System.nanoTime();
		for (int i = 0; i < iterations; i++)
		{
			queue.offer(addIndex++);
			int		value		= queue.poll();
			assertEquals((takeIndex++), value);
		}
		long		t2		= System.nanoTime();

		assertEquals(items, queue.size());

		reportPerformance(
				String.format("Performance of %s(%,d)", queue.getClass().getSimpleName(), queue.size()),
				t2 - t1, iterations, true);
	}

	@Test
	public void testMultiThread() throws BasicException
	{
		for (int cThread : new int[] { 2, 4, 6 })
		{
			for (int i = 0; i < 2; i++)
			{
				measureMultiThreaded(QUEUE_FACTORY.getArrayBlockingQueue(1000),		cThread, 1 * 10 * 1000);
				measureMultiThreaded(QUEUE_FACTORY.getRingBufferQueue(1000),		cThread, 1 * 10 * 1000);
			}
		}
	}

	public void measureMultiThreaded(Queue<Integer> queue, int threadCount, int iterations) throws BasicException
	{
		int						itemCount		= 50;
		for (int i = 0; i < itemCount; i++)
		{
			assertTrue(queue.offer(i));
		}

		CountDownLatch latch			= new CountDownLatch(threadCount);
		long					t1				= System.nanoTime();
		for (int i = 0; i < threadCount; i++)
		{
			new QueueThread(latch, queue, iterations);
		}
		try
		{
			latch.await();
		}
		catch (InterruptedException e)
		{
			getLogger().warn(BasicEvent.EVENT_TEST, e, "CountDownLatch interrupted!");
		}
		long					t2				= System.nanoTime();
		reportPerformance(
				String.format("MultiThread(%-20s, %,d threads)", queue.getClass().getSimpleName(), threadCount),
				t2 - t1, iterations * threadCount, true);
	}

	private static class QueueThread extends Thread
	{
		private CountDownLatch m_latch;
		private Queue<Integer> m_queueBuffer;
		private int						m_iterations;

		private QueueThread(CountDownLatch latch, Queue<Integer> queueBuffer, int iterations)
		{
			m_latch = latch;
			m_queueBuffer = queueBuffer;
			m_iterations = iterations;

			setDaemon(true);
			start();
		}

		@Override
		public void run()
		{
			int		counter		= 0;
			for (int i = 0; i < m_iterations; i++)
			{
				assertTrue(m_queueBuffer.offer(counter++));
				assertNotNull(m_queueBuffer.poll());
			}
			m_latch.countDown();
		}
	}
}
