/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.ObjectPool.AbstractPooledObject;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.configuration.MemoryConfigurationProvider;
import com.expedia.echox3.basics.thread.IEchoThreadPool;
import com.expedia.echox3.basics.thread.EchoThreadPool;
import com.expedia.echox3.basics.tools.misc.BasicTools;


public class ObjectPoolTests extends AbstractTestTools
{
	private MemoryConfigurationProvider		m_testConfigurationProvider;

	@Before
	public void setUp()
	{
		m_testConfigurationProvider		= new MemoryConfigurationProvider("TestProvider");
		ConfigurationManager.getInstance().addProvider(m_testConfigurationProvider);
	}
	@Test
	public void testHelloObjectPool()
	{
		String							testName	= logTestName();
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);

		TestPooledObject	object		= pool.get();
		assertTrue(1 == pool.getOutstandingCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
		object.release();
		assertTrue(0 == pool.getOutstandingCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
	}

	@Test
	public void testHelloObjectPoolWithArg()
	{
		String									testName	= logTestName();
		ObjectPool<TestPooledObjectWithArg>		pool		= new ObjectPool<>(
				new StringGroup(testName), () -> new TestPooledObjectWithArg("FirstName", "SecondName"));

		TestPooledObjectWithArg					object		= pool.get();
		assertTrue(1 == pool.getOutstandingCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
		object.release();
		assertTrue(0 == pool.getOutstandingCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
	}

	@Test
	public void testGrow()
	{
		String							testName	= logTestName();
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);

		TestPooledObject[]					objectList		= new TestPooledObject[12];
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i] = pool.get();
		}
		assertTrue(objectList.length == pool.getOutstandingCount());
		assertTrue(objectList.length <= pool.getAllocatedCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());

		int			count		= objectList.length;
		Random random		= new Random();
		while (0 != count)
		{
			int			i		= random.nextInt(objectList.length);
			if (null != objectList[i])
			{
				objectList[i].release();
				objectList[i] = null;
				count--;
			}
		}
		assertTrue(0 == pool.getOutstandingCount());
		assertTrue(objectList.length <= pool.getBlankCount());
		assertTrue(objectList.length <= pool.getAllocatedCount());
		assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
	}

	@Test
	public void testChangeMaxSizeIncrease()
	{
		String						testName	= logTestName();
		addSettings(testName, 5, 6);
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);
		BasicTools.sleepMS(100);
		assertEquals(6, pool.getMaxCount());
		pool.setMaxSize(7);
		assertEquals(7, pool.getMaxCount());
		TestPooledObject[]			objectList	= new TestPooledObject[7];
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i] = pool.get();
		}
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i].release();
		}
		assertTrue(0 == pool.getOutstandingCount());
		assertEquals(7, pool.getMaxCount());
	}

	@Test
	public void testShrink()
	{
		String							testName	= logTestName();
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);

		pool.setMaxSize(100);
		TestPooledObject[]		objectList		= new TestPooledObject[100];
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i] = pool.get();
		}
		for (int i = 50; i < objectList.length; i++)
		{
			objectList[i].release();
		}

		assertEquals(80, shrinkPoolSize(pool, 80));
		assertEquals(60, shrinkPoolSize(pool, 60));
		assertEquals(50, shrinkPoolSize(pool, 40));

		for (int i = 0; i < 50; i++)
		{
			objectList[i].release();
		}

		assertEquals(40, shrinkPoolSize(pool, 40));
		assertEquals(20, shrinkPoolSize(pool, 20));
		assertEquals(10, shrinkPoolSize(pool, 10));
		assertEquals(10, shrinkPoolSize(pool, 5));

		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i] = pool.get();
		}
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i].release();
		}
	}
	private int shrinkPoolSize(ObjectPool<TestPooledObject> pool, int size)
	{
		pool.shrink(size);
		return pool.getAllocatedCount();
	}

		@Test
	public void testChangeMaxSizeDecrease()
	{
		String							testName	= logTestName();

		addSettings(testName, 5, 7);
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);
		assertEquals(7, pool.getMaxCount());
		TestPooledObject[]				objectList	= new TestPooledObject[7];
		for (int i = 0; i < objectList.length; i++)
		{
			objectList[i] = pool.get();
		}
		pool.setMaxSize(6);
		assertEquals(6, pool.getMaxCount());
		for (int i = 1; i < objectList.length; i++)
		{
			objectList[i].release();
		}
		assertTrue(1 == pool.getOutstandingCount());
		assertEquals(6, pool.getMaxCount());
		pool.setMaxSize(1);
		objectList[0].release();
		assertEquals(1, pool.getMaxCount());
	}

	@Test
	public void testWait() throws InterruptedException
	{
		String							testName		= logTestName();

		// The pool will grow only to countMax, but the loop will ask 2 * countMax
		// ... and so will have to wait for the object to be placed back in the pool
		int								countInitial	= 5;
		int								countMax		= 2 * countInitial;
		addSettings(testName, countInitial, countMax);
		IEchoThreadPool					threadPool		= EchoThreadPool.createThreadPool(testName, 2, 2 *  countMax);
		ObjectPool<TestPooledRunnable>	objectPool		=
												new ObjectPool<>(new StringGroup(testName), TestPooledRunnable::new);

		for (int i = 0; i < 2 * countMax; i++)
		{
			TestPooledRunnable		runnable	= objectPool.get();
			threadPool.execute(runnable);
		}

		assertEquals(countMax, objectPool.getAllocatedCount());
	}

	private void addSettings(String poolName, int countInitial, int countMax)
	{
		m_testConfigurationProvider.addSetting(
				String.format(ObjectPool.SETTING_INITIAL_SIZE, poolName),
				Integer.toString(countInitial));
		m_testConfigurationProvider.addSetting(
				String.format(ObjectPool.SETTING_MAX_SIZE, poolName),
				Integer.toString(countMax));
	}

	public static class TestPooledRunnable extends TestPooledObject implements Runnable
	{
		public TestPooledRunnable()
		{
			// Parameter-less constructor for ObjectPool
		}

		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p/>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		@Override
		public void run()
		{
			BasicTools.sleepMS(2);
			release();
		}
	}

	@Test
	public void testPerformance()
	{
		String							testName	= logTestName();
		ObjectPool<TestPooledObject>	pool	= new ObjectPool<>(new StringGroup(testName), TestPooledObject::new);

		TestPooledObject[]					objectList		= new TestPooledObject[12];

		int			iMax		= 10 * 1000;
		int[]		iList		= new int[iMax];
		Random random		= new Random();
		for (int i = 0; i < iMax; i++)
		{
			iList[i] = random.nextInt(objectList.length);
		}

		for (int i = 0; i < 20; i++)
		{
			measurePerformance("getOrRelease", pool, objectList, iList, true);
		}
	}
	private void measurePerformance(
			String name, ObjectPool<TestPooledObject> pool, TestPooledObject[] objectList, int[] iList, boolean inform)
	{
		long		t1		= System.nanoTime();
		for (int i : iList)
		{
			if (null == objectList[i])
			{
				objectList[i] = pool.get();
			}
			else
			{
				objectList[i].release();
				objectList[i] = null;
			}
			assertTrue(0 <= pool.getOutstandingCount());
			assertTrue(objectList.length >= pool.getOutstandingCount());
			assertTrue(pool.getOutstandingCount() + pool.getBlankCount() == pool.getAllocatedCount());
		}
		long		t2		= System.nanoTime();
		reportPerformance(name, (t2 - t1), iList.length, inform);
	}




	public static class TestPooledObject extends AbstractPooledObject
	{
/*
		private String m_name;

		@Override
		public boolean equals(final Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (o == null || getClass() != o.getClass())
			{
				return false;
			}

			final TestPooledObject that = (TestPooledObject) o;
			return !(m_name != null ? !m_name.equals(that.m_name) : that.m_name != null);
		}

		@Override
		public int hashCode()
		{
			return m_name != null ? m_name.hashCode() : 0;
		}

		@SuppressWarnings("UnusedDeclaration")
		public void setName(String value)
		{
			m_name = value;
		}

		@SuppressWarnings("UnusedDeclaration")
		public String getName()
		{
			return m_name;
		}
*/
	}
	public static class TestPooledObjectWithArg extends AbstractPooledObject
	{
		public String m_name1;
		public String m_name2;

		public TestPooledObjectWithArg(String name1, String name2)
		{
			m_name1 = name1;
			m_name2 = name2;
		}
/*
		public String getName1()
		{
			return m_name1;
		}

		public String getName2()
		{
			return m_name2;
		}
*/
/*
		@Override
		public boolean equals(final Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (o == null || getClass() != o.getClass())
			{
				return false;
			}

			final TestPooledObject that = (TestPooledObject) o;
			return !(m_name != null ? !m_name.equals(that.m_name) : that.m_name != null);
		}

		@Override
		public int hashCode()
		{
			return m_name != null ? m_name.hashCode() : 0;
		}

		@SuppressWarnings("UnusedDeclaration")
		public void setName(String value)
		{
			m_name = value;
		}

		@SuppressWarnings("UnusedDeclaration")
		public String getName()
		{
			return m_name;
		}
*/
	}
}
