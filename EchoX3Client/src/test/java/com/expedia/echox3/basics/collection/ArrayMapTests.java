/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.collection.map.ArrayMap;
import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.collection.map.ArrayMapEntrySet;
import com.expedia.echox3.basics.collection.map.ArrayMapKeySet;
import com.expedia.echox3.basics.collection.map.ArrayMapValueList;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ArrayMapTests extends AbstractTestTools
{
	private static final int		MEG			= (1024 * 1024);

	@Test
	public void testBasic()
	{
		logTestName();

		int							count			= 2000;
		Map<Integer, String>		arrayMap		= new ArrayMap<>(count);
		Map<Integer, String>		treeMap			= new TreeMap<>();
		validateMapSimple(arrayMap, count);
		validateMapSimple(treeMap, count);

		populateSimple(treeMap, count);
		arrayMap.putAll(treeMap);
		String			name		= arrayMap.toString();
		assertTrue(name.contains(ArrayMap.class.getSimpleName()));
		assertTrue(name.contains(String.format("%,d", count)));
	}
	private void validateMapSimple(Map<Integer, String> map, int count)
	{
		assertNotNull(map);

		populateSimple(map, count);
		assertFalse(map.isEmpty());

		for (int i = 0; i < count; i++)
		{
			assertTrue(map.containsKey(i));
			String value		= map.get(i);
			assertNotNull(value);
			assertEquals(Integer.toString(i), value);
		}

		for (int i = 0; i < count; i++)
		{
			map.remove(i);
			assertFalse(map.containsKey(i));
			String value		= map.get(i);
			assertNull(value);
			assertEquals(count - 1, map.size());

			map.put(i, Integer.toString(1000 + i));
			assertTrue(map.containsKey(i));
			String value2		= map.get(i);
			assertNotNull(value2);
			assertEquals(Integer.toString(1000 + i), value2);
			assertEquals(count, map.size());
		}

		map.clear();
		assertEquals(0, map.size());
		assertTrue(map.isEmpty());
	}

	@Test
	public void testEntrySetSimple()
	{
		logTestName();

		int							count			= 10;
		Map<Integer, String>		arrayMap		= new ArrayMap<>(count);
		Map<Integer, String>		treeMap			= new TreeMap<>();
		validateEntrySetSimple(arrayMap, count);
		validateEntrySetSimple(treeMap, count);
	}
	@SuppressWarnings("unchecked")
	private void validateEntrySetSimple(Map<Integer, String> map, int count)
	{
		populateSimple(map, count);

		boolean[]							isSeenList		= new boolean[count];
		Set<Map.Entry<Integer, String>>		entrySet		= map.entrySet();
		for (Map.Entry<Integer, String>		entry : entrySet)
		{
			int			key			= entry.getKey();
			assertFalse(String.format("isSeen(%,d)", key), isSeenList[key]);
			isSeenList[key] = true;
			assertEquals(Integer.toString(key), entry.getValue());

			assertTrue(entrySet.contains(entry));

			ArrayMap.ArrayMapEntry<String>		localEntry		= new ArrayMap.ArrayMapEntry<>(entry.getKey(), "Foo");
			assertEquals(0, localEntry.compareTo(entry));
		}

		for (int i = 0; i < isSeenList.length; i++)
		{
			assertTrue(String.format("isSeen(%,d)", i), isSeenList[i]);
		}

		if (map instanceof ArrayMap)
		{
			Iterator<Map.Entry<Integer, String>> iterator = entrySet.iterator();
			while (iterator.hasNext())
			{
				iterator.next();
				try
				{
					iterator.remove();
					assertTrue("An exception is supposed to have occurred", false);
				}
				catch (UnsupportedOperationException exception)
				{
					// Do nothing, exception is supposed to happen on this iterator
				}
			}
		}
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testEntrySetOther()
	{
		logTestName();

		int									count			= 10;
		Map<Integer, String>				arrayMap		= new ArrayMap<>(count);
		populateSimple(arrayMap, count);
		Set<Map.Entry<Integer, String>>		set				= arrayMap.entrySet();
		Map.Entry<Integer, String>[]		entryArray1		= (Map.Entry[]) set.toArray();		// NOPMD ???

		Map.Entry<Integer, String>[]		entryArray2		= new ArrayMap.ArrayMapEntry[count + 1];
		entryArray2[count] = new ArrayMap.ArrayMapEntry<>(9999, "To be removed");
		entryArray2 = set.toArray(entryArray2);
		assertNull(entryArray2[count]);

		Map.Entry<Integer, String>[]		entryArray3		= new ArrayMap.ArrayMapEntry[count - 1];
		entryArray3 = set.toArray(entryArray3);
		assertNotNull(entryArray3);
		assertEquals(entryArray1.length, entryArray3.length);

		assertTrue(set.containsAll(set));
		assertTrue(set.containsAll(Arrays.asList(entryArray1)));

		int			i			= 0;
		for (Map.Entry<Integer, String> entry : entryArray1)
		{
			int			key				= entry.getKey();
			String		valueSav		= entry.getValue();

			assertTrue(entry.getKey().hashCode() == entry.hashCode());
			assertNotNull(entry.toString());

			assertTrue(set.contains(entry));
			set.remove(key);
			assertFalse(set.contains(entry));
			assertEquals(count - 1, set.size());

			set.add(entry);
			assertTrue(set.contains(entry));
			set.remove(entry);
			assertFalse(set.contains(entry));
			assertEquals(count - 1, set.size());

			set.add(new ArrayMap.ArrayMapEntry<>(key, valueSav));
			assertTrue(set.contains(entry));

			assertEquals(entry, entryArray1[i]);
			assertEquals(entry, entryArray2[i]);

			i++;
		}
		assertNull(entryArray2[i]);


	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testEntrySetAll()
	{
		logTestName();

		int									count		= 100;
		Map<Integer, String>				arrayMap1	= new ArrayMap<>(count);
		populateSimple(arrayMap1, count);
		Map<Integer, String>				arrayMap2	= new ArrayMap<>(count);
		populateSimple(arrayMap2, count);

		Set<Map.Entry<Integer, String>>		set1	= arrayMap1.entrySet();
		Set<Map.Entry<Integer, String>>		set2	= arrayMap2.entrySet();

		set1.removeAll(set2);
		assertTrue(set1.isEmpty());

		set1.addAll(set2);
		assertEquals(set2.size(), set1.size());

		set1.clear();
		assertTrue(set1.isEmpty());

		int									countB		= count / 3;
		Map<Integer, String>				arrayMapB	= new ArrayMap<>(count);
		Set<Map.Entry<Integer, String>>		setB		= arrayMapB.entrySet();
		populateSimple(arrayMapB, countB);
		set1.addAll(set2);
		set1.removeAll(setB);
		assertEquals(count - countB, set1.size());

		set1.addAll(set2);
		set1.retainAll(setB);
		assertEquals(countB, set1.size());
	}

		@Test
	public void testKeySet()
	{
		logTestName();

		int						count		= 100;
		Map<Integer, String>	map			= new ArrayMap<>(count);
		populateSimple(map, count);

		Set<Integer>			set			= map.keySet();
		assertNotNull(set);
		assertFalse(set.isEmpty());
		assertEquals(count, set.size());
		for (Integer key : set)
		{
			String		value		= map.get(key);
			assertNotNull(value);
			assertEquals(Integer.toString(key), value);

			assertTrue(set.contains(key));
		}

		Object[]				array		= set.toArray();
		assertNotNull(array);
		assertEquals(set.size(), array.length);

		set.clear();
		assertTrue(set.isEmpty());
		assertTrue(map.isEmpty());

		populateSimple(map, 5);
		assertFalse(set.isEmpty());
		assertFalse(map.isEmpty());
		List<Integer>		list		= new LinkedList<>();
		list.addAll(set);
		assertTrue(set.containsAll(list));
		assertTrue(set.removeAll(list));
		assertTrue(set.isEmpty());
		assertTrue(map.isEmpty());

		populateSimple(map, 5);
		set.remove(2);
		set.remove(4);
		assertEquals(3, map.size());
	}

	@Test
	public void testValues()
	{
		logTestName();

		int							count			= 10;
		boolean[]					isSeenList		= new boolean[count];
		Map<Integer, String>		map				= new ArrayMap<>(count);
		populateSimple(map, count);
		Collection<String>			list			= map.values();
		assertFalse(list.isEmpty());
		assertEquals(count, list.size());

		assertFalse(list.contains("NotInTheList"));

		for (String value : list)
		{
			assertNotNull(value);
			int		key		= Integer.parseInt(value.trim());
			assertFalse(isSeenList[key]);
			isSeenList[key] = true;
			assertEquals(value, map.get(key));

			assertTrue(list.contains(value));
			assertTrue(map.containsValue(value));
		}

		for (int i = 0; i < isSeenList.length; i++)
		{
			assertTrue(String.format("isSeen(%,d)", i), isSeenList[i]);
		}

		Object[]					array			= list.toArray();
		assertNotNull(array);
		assertEquals(count, array.length);
		int			i		= 0;
		for (String valueFromList : list)
		{
			assertEquals(valueFromList, array[i++]);
		}

		list.clear();
		Collection<String>			listEmpty		= map.values();
		assertNotNull(listEmpty);
		assertTrue(listEmpty.isEmpty());
		Object[]					arrayEmpty		= list.toArray();
		assertNotNull(arrayEmpty);
		assertEquals(0, arrayEmpty.length);
	}

	@Test
	public void testExceptions() throws NoSuchMethodException
	{
		logTestName();

		int						count			= 5;
		Map<Integer, String>	arrayMap		= new ArrayMap<>(count);
		populateSimple(arrayMap, count);

		Iterator<Map.Entry<Integer, String>>	entryIterator		= arrayMap.entrySet().iterator();
		arrayMap.remove(count / 2);
		ensureException(entryIterator,
				ArrayMapEntrySet.EntrySetIterator.class.getMethod("next"),
				ConcurrentModificationException.class);


		Set<Integer>			keySet			= arrayMap.keySet();
		ensureException(keySet,
				ArrayMapKeySet.class.getMethod("add", Integer.class),
				UnsupportedOperationException.class, 5);
		ensureException(keySet,
				ArrayMapKeySet.class.getMethod("addAll", Collection.class),
				UnsupportedOperationException.class, keySet);
		ensureException(keySet,
				ArrayMapKeySet.class.getMethod("retainAll", Collection.class),
				UnsupportedOperationException.class, keySet);
		ensureException(keySet,
				ArrayMapKeySet.class.getMethod("remove", Object.class),
				UnsupportedOperationException.class, 5);


		Iterator<Integer>	keyIterator		= keySet.iterator();
		keyIterator.next();
		ensureException(keyIterator,
				ArrayMapKeySet.KeySetIterator.class.getMethod("remove"),
				UnsupportedOperationException.class);


		Iterator<String>	valueIterator		= arrayMap.values().iterator();
		valueIterator.next();
		ensureException(valueIterator,
				ArrayMapValueList.ValueListIterator.class.getMethod("remove"),
				UnsupportedOperationException.class);


		Collection<String>		values		= arrayMap.values();
		ensureException(values,
				ArrayMapValueList.class.getMethod("add", Object.class),
				UnsupportedOperationException.class, "Foo");
		ensureException(values,
				ArrayMapValueList.class.getMethod("remove", Object.class),
				UnsupportedOperationException.class, "Foo");
		ensureException(values,
				ArrayMapValueList.class.getMethod("addAll", Collection.class),
				UnsupportedOperationException.class, values);
		ensureException(values,
				ArrayMapValueList.class.getMethod("containsAll", Collection.class),
				UnsupportedOperationException.class, values);
		ensureException(values,
				ArrayMapValueList.class.getMethod("retainAll", Collection.class),
				UnsupportedOperationException.class, values);
		ensureException(values,
				ArrayMapValueList.class.getMethod("removeAll", Collection.class),
				UnsupportedOperationException.class, values);
	}

	@Test
	public void testPerformanceFast()
	{
		logTestName();

		int							count			= 100;
		Map<Integer, String>		arrayMap		= new ArrayMap<>(count);
		Map<Integer, String>		hashMap			= new HashMap<>();
		Map<Integer, String>		treeMap			= new TreeMap<>();

		printPutGetHeader(arrayMap, arrayMap, hashMap, treeMap, count);
		for (int i = 0; i < 10; i++)
		{
			measurePutGetPerformance(arrayMap, arrayMap, hashMap, treeMap,	count);
		}
	}
	@Test
	public void testPerformanceSlow()
	{
		logTestName();

		int							count			= 1 * 1000;
		Map<Integer, String>		arrayMap1		= new ArrayMap<>();
		Map<Integer, String>		arrayMap2		= new ArrayMap<>(count);
		Map<Integer, String>		hashMap			= new HashMap<>();
		Map<Integer, String>		treeMap			= new TreeMap<>();

		printPutGetHeader(arrayMap1, arrayMap2, hashMap, treeMap, count);
		for (int i = 0; i < 20; i++)
		{
			measurePutGetPerformance(arrayMap1, arrayMap2, hashMap, treeMap, count);
		}
	}
	private void printPutGetHeader(
			Map<Integer, String> map1,
			Map<Integer, String> map2,
			Map<Integer, String> map3,
			Map<Integer, String> map4, int count
		)
	{
		getLogger().info(BasicEvent.EVENT_TEST,
				"%12s %12s %12s %12s -> Performance over get/remove/put of %,d random keys",
				map1.getClass().getSimpleName().replace("Map", ""),
				map2.getClass().getSimpleName().replace("Map", ""),
				map3.getClass().getSimpleName().replace("Map", ""),
				map4.getClass().getSimpleName().replace("Map", ""),
				count
		);
	}
	private void measurePutGetPerformance(
			Map<Integer, String> map1,
			Map<Integer, String> map2,
			Map<Integer, String> map3,
			Map<Integer, String> map4,
			int count)
	{
		Integer[]		keyList		= new Integer[count];
		for (int i = 0; i < keyList.length; i++)
		{
			keyList[i] = i;
		}
		Collections.shuffle(Arrays.asList(keyList));

		populateSimple(map1, count);
		populateSimple(map2, count);
		populateSimple(map3, count);
		populateSimple(map4, count);

		long		ns1		= measurePutGetPerformance(map1, keyList);
		long		ns2		= measurePutGetPerformance(map2, keyList);
		long		ns3		= measurePutGetPerformance(map3, keyList);
		long		ns4		= measurePutGetPerformance(map4, keyList);

		long		timeNS1	= reportPerformance(map1.getClass().getSimpleName(), ns1, count, false);
		long		timeNS2	= reportPerformance(map2.getClass().getSimpleName(), ns2, count, false);
		long		timeNS3	= reportPerformance(map3.getClass().getSimpleName(), ns3, count, false);
		long		timeNS4	= reportPerformance(map4.getClass().getSimpleName(), ns4, count, false);

		getLogger().info(BasicEvent.EVENT_TEST,
				String.format("%12s %12s %12s %12s",
						TimeUnits.formatNS(timeNS1),
						TimeUnits.formatNS(timeNS2),
						TimeUnits.formatNS(timeNS3),
						TimeUnits.formatNS(timeNS4)
				));
	}
	private long measurePutGetPerformance(Map<Integer, String> map, Integer[] keyList)
	{
		long		t1		= System.nanoTime();
		for (int key : keyList)
		{
			String value		= map.get(key);
			map.remove(key);
			map.put(key, value);
		}
		long		t2		= System.nanoTime();

		return (t2 - t1);
	}

	@Test
	public void testMemory()
	{
		logTestName();

/*
		long		heapMaxMB		= BasicTools.getHeapSizeMax() / MEG;
		long		heapRequiredMB	= 200;		// Not needed on small count!
		assertTrue(String.format("This test requires a minimum of %,d MB of heap.", heapRequiredMB),
				heapMaxMB >= heapRequiredMB);
*/

		int			count			= 100 * 1000;
		int			strlen			= 25;
		String[]	values			= new String[count];
		getLogger().info(BasicEvent.EVENT_TEST,
				"Generating %,d strings (%,d chars) for the memory test.", count, strlen);
		for (int i = 0; i < values.length; i++)
		{
			values[i] = generateRandomString(strlen);
		}

		getLogger().info(BasicEvent.EVENT_TEST, "Evaluating the maps with %,d elements ...", count);
		long		heapBefore		= getMemoryUsed();

		measureMemory(heapBefore, new ArrayMap<>(count, 1), values);
		measureMemory(heapBefore, new ArrayMap<>(), values);
//		heapBefore = getMemoryUsed();
		measureMemory(heapBefore, new ArrayMap<>(count), values);
//		heapBefore = getMemoryUsed();
		measureMemory(heapBefore, new ArrayMap<>(count /  10), values);

//		heapBefore = getMemoryUsed();
		measureMemory(heapBefore, new HashMap<>(), values);
//		heapBefore = getMemoryUsed();
		measureMemory(heapBefore, new TreeMap<>(), values);
		getLogger().info(BasicEvent.EVENT_TEST, "Done with the memory test");
		BasicTools.wait(25);
	}
	@SuppressWarnings("PMD.UnusedPrivateMethod")
	private void measureMemory(long heapBefore, Map<Integer, String> map, String[] values)
	{
		String mapName;
		if (map instanceof ArrayMap)
		{
			mapName = String.format("%s(%,6d buckets)",
					map.getClass().getSimpleName(), ((ArrayMap) map).getBucketCount());
		}
		else
		{
			mapName = map.getClass().getSimpleName();
		}

		long		t1				= System.nanoTime();
		for (int i = 0; i < values.length; i++)
		{
			map.put(i, values[i]);
		}
		long		t2				= System.nanoTime();
		for (int i = 0; i < values.length; i++)
		{
			map.put(i, values[i]);
		}
		long		t3				= System.nanoTime();
		String value			= null;
		for (int i = 0; i < values.length; i++)
		{
			value = map.get(i);
		}
		long		t4				= System.nanoTime();
		assertNotNull(value);
		long		nsPerPut1		= reportPerformance(mapName, (t2 - t1), values.length, false);
		long		nsPerPut2		= reportPerformance(mapName, (t3 - t2), values.length, false);
		long		nsPerGet		= reportPerformance(mapName, (t4 - t3), values.length, false);

		long		heapAfter		= getMemoryUsed();
		map.clear();
		getMemoryUsed();		// Perform a GC after emptying the elements.

		long		heapUsed		= heapAfter - heapBefore;
		double		bytePerItem		= (1.0 * heapUsed) / values.length;

		getLogger().info(BasicEvent.EVENT_TEST,
				"%-25s: %,5d - %,5d = %,5d MB / %,10d items == %,5.1f bytes/item"
						+ "@ (1st=%s, 2nd=%s, get=%s)",
				mapName, heapAfter / MEG, heapBefore / MEG, heapUsed / MEG, values.length, bytePerItem,
				TimeUnits.formatNS(nsPerPut1), TimeUnits.formatNS(nsPerPut2), TimeUnits.formatNS(nsPerGet)
		);
	}
	private long getMemoryUsed()
	{
		long		sizeSav		= BasicTools.getHeapSize();
		long		size;
		long		drop;
		long		dropMax;
		do
		{
			System.gc();
			size = BasicTools.getHeapSize();
			drop = (sizeSav - size);

			dropMax = size / 5;
			sizeSav = size;
		} while (dropMax < drop);

		return size;
	}





	private void populateSimple(Map<Integer, String> map, int count)
	{
		map.clear();
		assertTrue(map.isEmpty());
		assertEquals(0, map.size());

		for (int i = 0; i < count; i++)
		{
			map.put(i, String.format("%d", i));
		}

		assertFalse(map.isEmpty());
		assertEquals(count, map.size());
	}
}
