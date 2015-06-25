/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.collection.simple.ManagedSet;
import com.expedia.echox3.basics.AbstractTestTools;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ManagedSetTests extends AbstractTestTools
{
	@Test
	public void testBasic()
	{
		logTestName();

		validateManagedSet(new ManagedSet<>());
		validateManagedSet(new ManagedSet<>(new StringComparator()));

	}

	private void validateManagedSet(ManagedSet<String> managedSet)
	{
		List<String> list0		= new LinkedList<>();
		List<String> list1		= new LinkedList<>();
		List<String> list2		= new LinkedList<>();
		List<String> list3		= new LinkedList<>();
		List<String> listX		= new LinkedList<>();

		list1.add("One");

		list2.add("One");
		list2.add("Two");

		list3.add("One");
		list3.add("Two");
		list3.add("Three");

		listX.add("Two");
		listX.add("Three");

		validateManagedSet(managedSet, list0, 0, 0);
		validateManagedSet(managedSet, list1, 1, 0);
		validateManagedSet(managedSet, list2, 1, 0);
		validateManagedSet(managedSet, list2, 0, 0);
		validateManagedSet(managedSet, list3, 1, 0);
		validateManagedSet(managedSet, list1, 0, 2);
		validateManagedSet(managedSet, list3, 2, 0);
		validateManagedSet(managedSet, listX, 0, 1);
		validateManagedSet(managedSet, list2, 1, 1);
		validateManagedSet(managedSet, listX, 1, 1);
		validateManagedSet(managedSet, list0, 0, 2);
	}

	private void validateManagedSet(
			ManagedSet<String> managedSet, Collection<String> set, int expectedAdded, int expectedRemoved)
	{
		managedSet.setTo(set);
		Set<String>		setAdded		= managedSet.getSetAdded();
		validateSet(setAdded, expectedAdded);

		Set<String> setRemoved		= managedSet.getSetRemoved();
		validateSet(setRemoved, expectedRemoved);
		String			changeText		= managedSet.getChangeText();
		if (0 != (expectedAdded + expectedRemoved))
		{
			assertTrue(!changeText.isEmpty());
		}
		else
		{
			assertTrue(changeText.isEmpty());
		}


		managedSet.clearHistory();
		validateSet(setAdded, 0);
		validateSet(setRemoved, 0);

		String			prev		= null;
		for (String current : managedSet)
		{
			if (null != prev)
			{
				assertTrue(0 > prev.compareTo(current));
			}
			prev = current;
		}
	}
	private void validateSet(Set<String> set, int expectedSize)
	{
		assertNotNull(set);
		assertEquals(expectedSize, set.size());
	}

	private static final class StringComparator implements Comparator<String>
	{
		/**
		 * Compares its two arguments for order.  Returns a negative integer,
		 * zero, or a positive integer as the first argument is less than, equal
		 * to, or greater than the second.<p>
		 * <p>
		 * In the foregoing description, the notation
		 * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
		 * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
		 * <tt>0</tt>, or <tt>1</tt> according to whether the value of
		 * <i>expression</i> is negative, zero or positive.<p>
		 * <p>
		 * The implementor must ensure that <tt>sgn(compare(x, y)) ==
		 * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
		 * implies that <tt>compare(x, y)</tt> must throw an exception if and only
		 * if <tt>compare(y, x)</tt> throws an exception.)<p>
		 * <p>
		 * The implementor must also ensure that the relation is transitive:
		 * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
		 * <tt>compare(x, z)&gt;0</tt>.<p>
		 * <p>
		 * Finally, the implementor must ensure that <tt>compare(x, y)==0</tt>
		 * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
		 * <tt>z</tt>.<p>
		 * <p>
		 * It is generally the case, but <i>not</i> strictly required that
		 * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
		 * any comparator that violates this condition should clearly indicate
		 * this fact.  The recommended language is "Note: this comparator
		 * imposes orderings that are inconsistent with equals."
		 *
		 * @param o1 the first object to be compared.
		 * @param o2 the second object to be compared.
		 * @return a negative integer, zero, or a positive integer as the
		 * first argument is less than, equal to, or greater than the
		 * second.
		 * @throws NullPointerException if an argument is null and this
		 *                              comparator does not permit null arguments
		 * @throws ClassCastException   if the arguments' types prevent them from
		 *                              being compared by this comparator.
		 */
		@Override
		public int compare(String o1, String o2)
		{
			return o1.compareTo(o2);
		}
	}


}
