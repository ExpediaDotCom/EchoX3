/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.map.SortedArray;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SortedArrayTests extends AbstractTestTools
{
	@Test
	public void testBasic()
	{
		String				testName		= logTestName();

		int					count			= 250;
		SortedArray			array		= new SortedArray();
		array.setName(testName);
		validateSimple(array, count);

	}
	private void validateSimple(SortedArray array, int count)
	{
		assertNotNull(array);
		assertNotNull(array.getName());
		assertNotNull(array.toString().contains(array.getName()));

		populateSimple(array, count);
		assertFalse(array.isEmpty());

		for (int i = 0; i < count; i++)
		{
			assertTrue(array.contains(i));
			int		value		= array.getKey(i);
			assertNotNull(value);
			assertEquals(i, value);
		}

		for (int i : array)
		{
			assertTrue(array.contains(i));
		}

		array.clear();
		assertEquals(0, array.size());
		assertTrue(array.isEmpty());

		populateSimple(array, 5);
		assertNotNull(array.toString());
	}

	@Test
	public void testAll()
	{
		SortedArray			array1		= new SortedArray();
		SortedArray			array2		= new SortedArray();

		populateSimple(array1, 10);
		array2.putAll(array1);
		assertEquals(array1.size(), array2.size());
	}

	@Test
	public void testException() throws NoSuchMethodException
	{
		SortedArray			array		= new SortedArray();
		Iterator<Integer>	iterator	= array.iterator();

		ensureException(iterator,
				SortedArray.IteratorByIndex.class.getMethod("remove"),
				UnsupportedOperationException.class);
	}




	private void populateSimple(SortedArray array, int count)
	{
		array.clear();
		assertTrue(array.isEmpty());
		assertEquals(0, array.size());

		while (array.size() != count)
		{
			int		i		= RANDOM.nextInt(count);
			array.put(i);
		}

		assertFalse(array.isEmpty());
		assertEquals(count, array.size());

		assertNull(array.validate());
	}
}
