/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.simple.ArraySet;

public class ArraySetTests extends AbstractTestTools
{
	private ArraySet<Integer> m_integerArraySet;

	@Before
	public void setUp()
	{
		m_integerArraySet		= new ArraySet<>();
	}

	private void add(Integer... integerArray)
	{
		for(int i = 0 ; i < integerArray.length ; ++i)
		{
			m_integerArraySet.add(integerArray[i]);
//			m_integerArraySet.add(integerArray[i]); // this second add should be a NOOP
		}
	}

	@Test
	public void testContains()
	{
		logTestName();

		add(3, 7, 9);
		add(23, 27, 1);
		assertFalse(m_integerArraySet.contains( 2));
		assertTrue (m_integerArraySet.contains( 3));
		assertFalse(m_integerArraySet.contains( 5));
		assertTrue (m_integerArraySet.contains( 7));
		assertTrue (m_integerArraySet.contains( 9));
		assertFalse(m_integerArraySet.contains(10));
	}

	@Test
	public void testContainsAll()
	{
		logTestName();

		add(1, 2, 3);
		final Integer[]			array				= m_integerArraySet.toArray(new Integer[3]);
		final List<Integer> list				= new LinkedList<>(Arrays.asList(array));
		assertTrue (m_integerArraySet.containsAll(list));
		assertTrue (m_integerArraySet.remove(1));
		assertFalse(m_integerArraySet.containsAll(list));
	}

	@Test
	public void testToArray()
	{
		logTestName();

		final List<Integer> integerList 		= populate();
		Collections.sort(integerList);
		final Iterator<Integer> arraySetIterator	= m_integerArraySet.iterator();
		final Object[]			objectArray			= m_integerArraySet.toArray();
		final int				size				= integerList.size();
		final Integer[]			integerArray		= integerList.toArray(new Integer[size]);
		final Integer[]			smallIntegerArray	= m_integerArraySet.toArray(new Integer[size - 1]);
		final Integer[]			exactIntegerArray	= m_integerArraySet.toArray(new Integer[size]);
		final Integer[]			largeIntegerArray	= m_integerArraySet.toArray(new Integer[size + 10]);
		assertArrayEquals(integerArray, objectArray);
		assertArrayEquals(integerArray, integerArray);
		assertArrayEquals(integerArray, smallIntegerArray);
		assertArrayEquals(integerArray, exactIntegerArray);
		for(int i = 0 ; i < size; ++i)
		{
			final Integer arraySetValue		= arraySetIterator.next();
			assertEquals(integerArray[i], arraySetValue);
			assertEquals(integerArray[i], largeIntegerArray[i]);
		}
		for (int i = size; i < largeIntegerArray.length; i++)
		{
			assertNull(largeIntegerArray[i]);
		}

		assertFalse(arraySetIterator.hasNext());
		assertNull(largeIntegerArray[size]);
	}

	@Test
	public void testRemove()
	{
		logTestName();

		final Integer value1				= RANDOM.nextInt();
		final Integer value2				= RANDOM.nextInt();
		add(value1, value2);
		assertEquals(2, m_integerArraySet.size());
		assertTrue(m_integerArraySet.remove(value1));
		assertEquals(1, m_integerArraySet.size());
		final Integer[]			array				= m_integerArraySet.toArray(new Integer[1]);
		assertEquals(value2, array[0]);
		assertTrue(m_integerArraySet.remove(value2));
		assertTrue(m_integerArraySet.isEmpty());
		assertFalse(m_integerArraySet.remove(value2));
	}

	@Test
	public void testAddAll()
	{
		logTestName();

		final List<Integer> expected	 		= populate();
		m_integerArraySet.clear();
		assertEquals(0, m_integerArraySet.size());
		m_integerArraySet.addAll(expected);
		final Integer[]			integerArray		= expected.toArray(new Integer[m_integerArraySet.size()]);
		final List<Integer> actual				= Arrays.asList(integerArray);
		assertEquals(expected, actual);
	}

	@Test
	public void testRetainAll()
	{
		logTestName();

		final List<Integer> initial	 			= populate();
		for(final Integer integer : initial)
		{
			m_integerArraySet.clear();
			assertTrue(m_integerArraySet.addAll(initial));
			final List<Integer> expected			= Collections.singletonList(integer);
			assertTrue(m_integerArraySet.retainAll(expected));
			final Integer[]			integerArray		= expected.toArray(new Integer[m_integerArraySet.size()]);
			final List<Integer> actual				= Arrays.asList(integerArray);
			assertEquals(expected, actual);
			assertFalse(m_integerArraySet.retainAll(expected));
		}
	}

	@Test
	public void testRemoveAllOneAtATime()
	{
		logTestName();

		final List<Integer> initial	 			= populate();
		for(final Integer integer : initial)
		{
			m_integerArraySet.clear();
			m_integerArraySet.addAll(initial);
			final int				sizeBefore			= m_integerArraySet.size();
			final List<Integer> expected			= Collections.singletonList(integer);
			assertTrue(m_integerArraySet.removeAll(expected));
			assertFalse(m_integerArraySet.contains(integer));
			assertEquals(sizeBefore - 1, m_integerArraySet.size());
			assertFalse(m_integerArraySet.removeAll(expected));
		}
	}

	@Test
	public void testRemoveAllEverythingAtOnce()
	{
		logTestName();

		final List<Integer> initial	 			= populate();
		assertTrue(m_integerArraySet.removeAll(initial));
		assertEquals(0, m_integerArraySet.size());
		assertFalse(m_integerArraySet.removeAll(initial));
	}

	@Test(expected = NoSuchElementException.class)
	public void testNextNoSuchElementException()
	{
		logTestName();

		final Iterator<Integer> iterator			= m_integerArraySet.iterator();
		assertFalse(iterator.hasNext());
		iterator.next();
	}

	@Test(expected = ConcurrentModificationException.class)
	public void testNextConcurrentModificationException()
	{
		logTestName();

		final List<Integer> integerList			= populate();
		final Iterator<Integer> iterator			= m_integerArraySet.iterator();
		m_integerArraySet.remove(integerList.get(integerList.size() - 1));
		iterator.next();
	}

	@Test
	public void testRemoveHappyCase()
	{
		logTestName();

		populate();
		final int					initialSize			= m_integerArraySet.size();
		final Iterator<Integer> iterator			= m_integerArraySet.iterator();
		for(int i = 0 ; i < initialSize ; i++)
		{
			iterator.next();
			iterator.remove();
			assertEquals(initialSize - i - 1, m_integerArraySet.size());
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRemoveWithoutNext()
	{
		logTestName();

		populate();
		m_integerArraySet.iterator().remove();
	}

	@Test(expected = IllegalStateException.class)
	public void testRemoveCalledTwice()
	{
		logTestName();

		populate();
		final Iterator<Integer> iterator			= m_integerArraySet.iterator();
		iterator.next();
		iterator.remove();
		iterator.remove();
	}

	@Test(expected = ConcurrentModificationException.class)
	public void testRemoveThrowsConcurrentModificationException()
	{
		logTestName();

		final List<Integer> integerList			= populate();
		final Iterator<Integer> iterator			= m_integerArraySet.iterator();
		iterator.next();
		m_integerArraySet.remove(integerList.get(integerList.size() - 1));
		iterator.remove();
	}

	@Test
	public void testFindHappyCase()
	{
		logTestName();

		populate();
		m_integerArraySet.remove(Integer.MIN_VALUE); // in case populate() chose it
		final Integer original			= Integer.MIN_VALUE;
		assertNull(m_integerArraySet.find(original));
		m_integerArraySet.add(original);
		final Integer copy				= Integer.MIN_VALUE;
		assertNotSame(original, copy);	// Verify that Java gave us two different objects; for some Integers it will not
		final Integer found				= m_integerArraySet.find(copy);
		assertSame(original, found);
	}

	private List<Integer> populate()
	{
		// There are some tests that require at least two elements
		final int					size				= 2 + Math.abs(RANDOM.nextInt() % 100);

		final Set<Integer> integerSet			= new HashSet<>();
		for(int i = 0 ; i < size ; ++i)
		{
			final int				random				= RANDOM.nextInt();
			add(random);
			integerSet.add(random);
		}
		final List<Integer> integerList			= new ArrayList<>(integerSet.size());
		integerList.addAll(integerSet);
		return integerList;
	}
}
