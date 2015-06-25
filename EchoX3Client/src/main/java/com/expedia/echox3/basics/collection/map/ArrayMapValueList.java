/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.map;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class ArrayMapValueList<T> implements Collection<T>, Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private ArrayMapEntrySet<T>		m_entrySet;

	public ArrayMapValueList(ArrayMapEntrySet<T> entrySet)
	{
		m_entrySet = entrySet;
	}

	/**
	 * Returns the number of elements in this set (its cardinality).  If this
	 * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	@Override
	public int size()
	{
		return m_entrySet.size();
	}

	/**
	 * Returns <tt>true</tt> if this set contains no elements.
	 *
	 * @return <tt>true</tt> if this set contains no elements
	 */
	@Override
	public boolean isEmpty()
	{
		return m_entrySet.isEmpty();
	}

	/**
	 * Returns <tt>true</tt> if this set contains the specified element.
	 * More formally, returns <tt>true</tt> if and only if this set
	 * contains an element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o element whose presence in this set is to be tested
	 * @return <tt>true</tt> if this set contains the specified element
	 * @throws ClassCastException   if the type of the specified element
	 *                              is incompatible with this set
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              set does not permit null elements
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object o)
	{
		for (Map.Entry<Integer, T> entry : m_entrySet)
		{
			if (entry.getValue().equals(o))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an iterator over the elements in this set.  The elements are
	 * returned in no particular order (unless this set is an instance of some
	 * class that provides a guarantee).
	 *
	 * @return an iterator over the elements in this set
	 */
	@Override
	public Iterator<T> iterator()
	{
		return new ValueListIterator();
	}

	/**
	 * Returns an array containing all of the elements in this set.
	 * If this set makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the
	 * elements in the same order.
	 * <p/>
	 * <p>The returned array will be "safe" in that no references to it
	 * are maintained by this set.  (In other words, this method must
	 * allocate a new array even if this set is backed by an array).
	 * The caller is thus free to modify the returned array.
	 * <p/>
	 * <p>This method acts as bridge between array-based and collection-based
	 * APIs.
	 *
	 * @return an array containing all the elements in this set
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object[] toArray()
	{
		return toArray((T[]) new Object[m_entrySet.size()]);
	}

	/**
	 * Returns an array containing all of the elements in this set; the
	 * runtime type of the returned array is that of the specified array.
	 * If the set fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this set.
	 * <p/>
	 * <p>If this set fits in the specified array with room to spare
	 * (i.e., the array has more elements than this set), the element in
	 * the array immediately following the end of the set is set to
	 * <tt>null</tt>.  (This is useful in determining the length of this
	 * set <i>only</i> if the caller knows that this set does not contain
	 * any null elements.)
	 * <p/>
	 * <p>If this set makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements
	 * in the same order.
	 * <p/>
	 * <p>Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and collection-based APIs.  Further, this method allows
	 * precise control over the runtime type of the output array, and may,
	 * under certain circumstances, be used to save allocation costs.
	 * <p/>
	 * <p>Suppose <tt>x</tt> is a set known to contain only strings.
	 * The following code can be used to dump the set into a newly allocated
	 * array of <tt>String</tt>:
	 * <p/>
	 * <pre>
	 *     String[] y = x.toArray(new String[0]);</pre>
	 *
	 * Note that <tt>toArray(new Object[0])</tt> is identical in function to
	 * <tt>toArray()</tt>.
	 *
	 * @param a the array into which the elements of this set are to be
	 *          stored, if it is big enough; otherwise, a new array of the same
	 *          runtime type is allocated for this purpose.
	 * @return an array containing all the elements in this set
	 * @throws ArrayStoreException  if the runtime type of the specified array
	 *                              is not a supertype of the runtime type of every element in this
	 *                              set
	 * @throws NullPointerException if the specified array is null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <U> U[] toArray(U[] a)
	{
		// Get iterator BEFORE size, so iterator detects change in size.
		Iterator<Map.Entry<Integer, T>>		iterator		= m_entrySet.iterator();
		Object[]							valueArray;
		int									size			= m_entrySet.size();
		if (a.length < size)
		{
			valueArray = (U[]) Array.newInstance(a.getClass().getComponentType(), size);
		}
		else
		{
			valueArray = a;
			if (a.length > size)
			{
				valueArray[size] = null;
			}
		}

		int		i		= 0;
		while (iterator.hasNext())
		{
			Map.Entry<Integer, T>		entry		= iterator.next();
			valueArray[i++] = entry.getValue();
		}

		return (U[]) valueArray;
	}

	/**
	 * Adds the specified element to this set if it is not already present
	 * (optional operation).  More formally, adds the specified element
	 * <tt>e</tt> to this set if the set contains no element <tt>e2</tt>
	 * such that
	 * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
	 * If this set already contains the element, the call leaves the set
	 * unchanged and returns <tt>false</tt>.  In combination with the
	 * restriction on constructors, this ensures that sets never contain
	 * duplicate elements.
	 * <p/>
	 * <p>The stipulation above does not imply that sets must accept all
	 * elements; sets may refuse to add any particular element, including
	 * <tt>null</tt>, and throw an exception, as described in the
	 * specification for {@link Collection#add Collection.add}.
	 * Individual set implementations should clearly document any
	 * restrictions on the elements that they may contain.
	 *
	 * @param e element to be added to this set
	 * @return <tt>true</tt> if this set did not already contain the specified
	 *         element
	 * @throws UnsupportedOperationException if the <tt>add</tt> operation
	 *                                       is not supported by this set
	 * @throws ClassCastException            if the class of the specified element
	 *                                       prevents it from being added to this set
	 * @throws NullPointerException          if the specified element is null and this
	 *                                       set does not permit null elements
	 * @throws IllegalArgumentException      if some property of the specified element
	 *                                       prevents it from being added to this set
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean add(T e)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Removes the specified element from this set if it is present
	 * (optional operation).  More formally, removes an element <tt>e</tt>
	 * such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
	 * this set contains such an element.  Returns <tt>true</tt> if this set
	 * contained the element (or equivalently, if this set changed as a
	 * result of the call).  (This set will not contain the element once the
	 * call returns.)
	 *
	 * @param o object to be removed from this set, if present
	 * @return <tt>true</tt> if this set contained the specified element
	 * @throws ClassCastException            if the type of the specified element
	 *                                       is incompatible with this set
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if the specified element is null and this
	 *                                       set does not permit null elements
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws UnsupportedOperationException if the <tt>remove</tt> operation
	 *                                       is not supported by this set
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Returns <tt>true</tt> if this set contains all of the elements of the
	 * specified collection.  If the specified collection is also a set, this
	 * method returns <tt>true</tt> if it is a <i>subset</i> of this set.
	 *
	 * @param c collection to be checked for containment in this set
	 * @return <tt>true</tt> if this set contains all of the elements of the
	 *         specified collection
	 * @throws ClassCastException   if the types of one or more elements
	 *                              in the specified collection are incompatible with this
	 *                              set
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified collection contains one
	 *                              or more null elements and this set does not permit null
	 *                              elements
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>),
	 *                              or if the specified collection is null
	 * @see #contains(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean containsAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Adds all of the elements in the specified collection to this set if
	 * they're not already present (optional operation).  If the specified
	 * collection is also a set, the <tt>addAll</tt> operation effectively
	 * modifies this set so that its value is the <i>union</i> of the two
	 * sets.  The behavior of this operation is undefined if the specified
	 * collection is modified while the operation is in progress.
	 *
	 * @param c collection containing elements to be added to this set
	 * @return <tt>true</tt> if this set changed as a result of the call
	 * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
	 *                                       is not supported by this set
	 * @throws ClassCastException            if the class of an element of the
	 *                                       specified collection prevents it from being added to this set
	 * @throws NullPointerException          if the specified collection contains one
	 *                                       or more null elements and this set does not permit null
	 *                                       elements, or if the specified collection is null
	 * @throws IllegalArgumentException      if some property of an element of the
	 *                                       specified collection prevents it from being added to this set
	 * @see #add(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends T> c)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Retains only the elements in this set that are contained in the
	 * specified collection (optional operation).  In other words, removes
	 * from this set all of its elements that are not contained in the
	 * specified collection.  If the specified collection is also a set, this
	 * operation effectively modifies this set so that its value is the
	 * <i>intersection</i> of the two sets.
	 *
	 * @param c collection containing elements to be retained in this set
	 * @return <tt>true</tt> if this set changed as a result of the call
	 * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
	 *                                       is not supported by this set
	 * @throws ClassCastException            if the class of an element of this set
	 *                                       is incompatible with the specified collection
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this set contains a null element and the
	 *                                       specified collection does not permit null elements
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Removes from this set all of its elements that are contained in the
	 * specified collection (optional operation).  If the specified
	 * collection is also a set, this operation effectively modifies this
	 * set so that its value is the <i>asymmetric set difference</i> of
	 * the two sets.
	 *
	 * @param c collection containing elements to be removed from this set
	 * @return <tt>true</tt> if this set changed as a result of the call
	 * @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
	 *                                       is not supported by this set
	 * @throws ClassCastException            if the class of an element of this set
	 *                                       is incompatible with the specified collection
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this set contains a null element and the
	 *                                       specified collection does not permit null elements
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException(ArrayMapEntrySet.MODIFICATION_NOT_ALLOWED);
	}

	/**
	 * Removes all of the elements from this set (optional operation).
	 * The set will be empty after this call returns.
	 *
	 * @throws UnsupportedOperationException if the <tt>clear</tt> method
	 *                                       is not supported by this set
	 */
	@Override
	public void clear()
	{
		m_entrySet.clear();
	}




	public class ValueListIterator implements Iterator<T>, Serializable
	{
		public static final long					serialVersionUID	= 20150601085959L;

		private Iterator<Map.Entry<Integer, T>> m_iterator;

		@SuppressWarnings("unchecked")
		public ValueListIterator()
		{
			m_iterator = m_entrySet.iterator();
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext()
		{
			return m_iterator.hasNext();
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@SuppressWarnings("unchecked")
		public T next()
		{
			Map.Entry<Integer, T>		entry		= m_iterator.next();
			return entry.getValue();
		}

		/**
		 * Removes from the underlying collection the last element returned
		 * by this iterator (optional operation).  This method can be called
		 * only once per call to {@link #next}.  The behavior of an iterator
		 * is unspecified if the underlying collection is modified while the
		 * iteration is in progress in any way other than by calling this
		 * method.
		 *
		 * @throws UnsupportedOperationException if the {@code remove}
		 *                                       operation is not supported by this iterator
		 * @throws IllegalStateException         if the {@code next} method has not
		 *                                       yet been called, or the {@code remove} method has already
		 *                                       been called after the last call to the {@code next}
		 *                                       method
		 */
		@Override
		public void remove()
		{
			m_iterator.remove();
		}
	}
}
