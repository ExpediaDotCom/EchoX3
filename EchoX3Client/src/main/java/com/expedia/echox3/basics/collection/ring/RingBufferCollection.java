/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.ring;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Thread safe standard ring buffer with overflow protection (throws).
 *
 * Typical usage will see one (or more) thread put items in the Ring buffer,
 * while a thread pool will remove items from the thread pool.
 *
 * @param <T>	Type of object to place in the collection
 */
public class RingBufferCollection<T> implements Collection<T>
{
	private int					m_version		= 0;		// NOPMD
	private int					m_tailIndex		= 0;		// Index of the oldest element
	private int					m_nextIndex		= 0;		// Index of the next element to put
	private int					m_valueCount	= 0;		// Count of elements currently in the buffer
	private T[]					m_valueList;

	@SuppressWarnings("unchecked")
	public RingBufferCollection(int count)
	{
		m_valueList = (T[]) new Object[count];
	}

	/**
	 * Returns the number of elements in this collection.  If this collection
	 * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this collection
	 */
	@Override
	public int size()
	{
		return m_valueCount;
	}

	/**
	 * Returns <tt>true</tt> if this collection contains no elements.
	 *
	 * @return <tt>true</tt> if this collection contains no elements
	 */
	@Override
	public boolean isEmpty()
	{
		return 0 == m_valueCount;
	}

	/**
	 * Returns <tt>true</tt> if this collection contains the specified element.
	 * More formally, returns <tt>true</tt> if and only if this collection
	 * contains at least one element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o element whose presence in this collection is to be tested
	 * @return <tt>true</tt> if this collection contains the specified
	 * element
	 * @throws ClassCastException   if the type of the specified element
	 *                              is incompatible with this collection
	 *                              (<a href="#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              collection does not permit null elements
	 *                              (<a href="#optional-restrictions">optional</a>)
	 */
	@Override
	public boolean contains(Object o)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns an iterator over the elements in this collection.  There are no
	 * guarantees concerning the order in which the elements are returned
	 * (unless this collection is an instance of some class that provides a
	 * guarantee).
	 *
	 * @return an <tt>Iterator</tt> over the elements in this collection
	 */
	@Override
	public Iterator<T> iterator()
	{
		return new RingBufferIterator<>(this);
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * <tt>true</tt> upon success and throwing an <tt>IllegalStateException</tt>
	 * if no space is currently available.
	 *
	 * @param value the element to add
	 * @return <tt>true</tt> (as specified by {@link java.util.Collection#add})
	 * @throws IllegalStateException    if the element cannot be added at this
	 *                                  time due to capacity restrictions
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this queue
	 * @throws NullPointerException     if the specified element is null and
	 *                                  this queue does not permit null elements
	 * @throws IllegalArgumentException if some property of this element
	 *                                  prevents it from being added to this queue
	 */
	@Override
	public boolean add(T value)
	{
		if (!addInternal(value))
		{
			throw new IllegalStateException("The queue is full");
		}
		return true;
	}
	protected boolean addInternal(T value)
	{
		synchronized (this)
		{
			if (m_valueList.length == m_valueCount)
			{
				return false;
			}
			m_valueList[m_nextIndex++] = value;
			m_valueCount = Math.min(m_valueList.length, m_valueCount + 1);
			if (m_valueList.length == m_nextIndex)
			{
				m_nextIndex = 0;
			}
			m_version++;
		}
		return true;
	}

	/**
	 * Removes a single instance of the specified element from this
	 * collection, if it is present (optional operation).  More formally,
	 * removes an element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
	 * this collection contains one or more such elements.  Returns
	 * <tt>true</tt> if this collection contained the specified element (or
	 * equivalently, if this collection changed as a result of the call).
	 *
	 * @param o element to be removed from this collection, if present
	 * @return <tt>true</tt> if an element was removed as a result of this call
	 * @throws ClassCastException            if the type of the specified element
	 *                                       is incompatible with this collection
	 *                                       (<a href="#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if the specified element is null and this
	 *                                       collection does not permit null elements
	 *                                       (<a href="#optional-restrictions">optional</a>)
	 * @throws UnsupportedOperationException if the <tt>remove</tt> operation
	 *                                       is not supported by this collection
	 */
	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}
	protected T removeInternal()
	{
		T		value;
		synchronized (this)
		{
			if (0 == m_valueCount)
			{
				value = null;
			}
			else
			{
				value = m_valueList[m_tailIndex];
				m_valueList[m_tailIndex] = null;
				m_valueCount--;
				m_tailIndex++;
				if (m_valueList.length == m_tailIndex)
				{
					m_tailIndex = 0;
				}
			}
		}
		return value;
	}
	protected T peekInternal()
	{
		T		value;
		synchronized (this)
		{
			if (0 == m_valueCount)
			{
				value = null;
			}
			else
			{
				value = m_valueList[m_tailIndex];
			}
		}
		return value;
	}

	/**
	 * Adds all of the elements in the specified collection to this collection
	 * (optional operation).  The behavior of this operation is undefined if
	 * the specified collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the
	 * specified collection is this collection, and this collection is
	 * nonempty.)
	 *
	 * @param c collection containing elements to be added to this collection
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
	 *                                       is not supported by this collection
	 * @throws ClassCastException            if the class of an element of the specified
	 *                                       collection prevents it from being added to this collection
	 * @throws NullPointerException          if the specified collection contains a
	 *                                       null element and this collection does not permit null elements,
	 *                                       or if the specified collection is null
	 * @throws IllegalArgumentException      if some property of an element of the
	 *                                       specified collection prevents it from being added to this
	 *                                       collection
	 * @throws IllegalStateException         if not all the elements can be added at
	 *                                       this time due to insertion restrictions
	 * @see #add(Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends T> c)
	{
		for (T value : c)
		{
			add(value);		// Will perform the required throw IllegalStateException
		}

		return true;
	}

	/**
	 * Removes all of the elements from this collection (optional operation).
	 * The collection will be empty after this method returns.
	 *
	 * @throws UnsupportedOperationException if the <tt>clear</tt> operation
	 *         is not supported by this collection
	 */
	@Override
	public void clear()
	{
		synchronized (this)
		{
			m_valueCount = 0;
			m_nextIndex = 0;
			Arrays.fill(m_valueList, null);
			m_version++;
		}
	}

	/**
	 * Retains only the elements in this collection that are contained in the
	 * specified collection (optional operation).  In other words, removes from
	 * this collection all of its elements that are not contained in the
	 * specified collection.
	 *
	 * @param c collection containing elements to be retained in this collection
	 * @return <tt>true</tt> if this collection changed as a result of the call
	 * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
	 *                                       is not supported by this collection
	 * @throws ClassCastException            if the types of one or more elements
	 *                                       in this collection are incompatible with the specified
	 *                                       collection
	 *                                       (<a href="#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this collection contains one or more
	 *                                       null elements and the specified collection does not permit null
	 *                                       elements
	 *                                       (<a href="#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Removes all of this collection's elements that are also contained in the
	 * specified collection (optional operation).  After this call returns,
	 * this collection will contain no elements in common with the specified
	 * collection.
	 *
	 * @param c collection containing elements to be removed from this collection
	 * @return <tt>true</tt> if this collection changed as a result of the
	 * call
	 * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
	 *                                       is not supported by this collection
	 * @throws ClassCastException            if the types of one or more elements
	 *                                       in this collection are incompatible with the specified
	 *                                       collection
	 *                                       (<a href="#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this collection contains one or more
	 *                                       null elements and the specified collection does not support
	 *                                       null elements
	 *                                       (<a href="#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns <tt>true</tt> if this collection contains all of the elements
	 * in the specified collection.
	 *
	 * @param c collection to be checked for containment in this collection
	 * @return <tt>true</tt> if this collection contains all of the elements
	 * in the specified collection
	 * @throws ClassCastException   if the types of one or more elements
	 *                              in the specified collection are incompatible with this
	 *                              collection
	 *                              (<a href="#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified collection contains one
	 *                              or more null elements and this collection does not permit null
	 *                              elements
	 *                              (<a href="#optional-restrictions">optional</a>),
	 *                              or if the specified collection is null.
	 * @see #contains(Object)
	 */
	@Override
	public boolean containsAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns an array containing all of the elements in this collection.
	 * If this collection makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in
	 * the same order.
	 * <p/>
	 * <p>The returned array will be "safe" in that no references to it are
	 * maintained by this collection.  (In other words, this method must
	 * allocate a new array even if this collection is backed by an array).
	 * The caller is thus free to modify the returned array.
	 * <p/>
	 * <p>This method acts as bridge between array-based and collection-based
	 * APIs.
	 *
	 * @return an array containing all of the elements in this collection
	 */
	@Override
	public Object[] toArray()
	{
		return toArray(new Object[m_valueCount]);
	}

	/**
	 * Returns an array containing all of the elements in this collection;
	 * the runtime type of the returned array is that of the specified array.
	 * If the collection fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this collection.
	 * <p/>
	 * <p>If this collection fits in the specified array with room to spare
	 * (i.e., the array has more elements than this collection), the element
	 * in the array immediately following the end of the collection is set to
	 * <tt>null</tt>.  (This is useful in determining the length of this
	 * collection <i>only</i> if the caller knows that this collection does
	 * not contain any <tt>null</tt> elements.)
	 * <p/>
	 * <p>If this collection makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in
	 * the same order.
	 * <p/>
	 * <p>Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and collection-based APIs.  Further, this method allows
	 * precise control over the runtime type of the output array, and may,
	 * under certain circumstances, be used to save allocation costs.
	 * <p/>
	 * <p>Suppose <tt>x</tt> is a collection known to contain only strings.
	 * The following code can be used to dump the collection into a newly
	 * allocated array of <tt>String</tt>:
	 * <p/>
	 * <pre>
	 *     String[] y = x.toArray(new String[0]);</pre>
	 *
	 * Note that <tt>toArray(new Object[0])</tt> is identical in function to
	 * <tt>toArray()</tt>.
	 *
	 * @param a the array into which the elements of this collection are to be
	 *          stored, if it is big enough; otherwise, a new array of the same
	 *          runtime type is allocated for this purpose.
	 * @return an array containing all of the elements in this collection
	 * @throws ArrayStoreException  if the runtime type of the specified array
	 *                              is not a supertype of the runtime type of every element in
	 *                              this collection
	 * @throws NullPointerException if the specified array is null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <U> U[] toArray(U[] a)
	{
		Object[] array;
		synchronized (this)
		{
			if (a.length > m_valueCount)
			{
				array = a;
				array[m_valueCount] = null;
			}
			else if (a.length == m_valueCount)
			{
				array = a;
			}
			else
			{
				array = (U[]) Array.newInstance(a.getClass().getComponentType(), m_valueCount);
			}
			int		index		= 0;
			for (T value : this)
			{
				array[index++] = value;
			}
		}

		return (U[]) array;
	}

	/**
	 * Returns a string representation of this collection.  The string
	 * representation consists of a list of the collection's elements in the
	 * order they are returned by its iterator, enclosed in square brackets
	 * (<tt>"[]"</tt>).  Adjacent elements are separated by the characters
	 * <tt>", "</tt> (comma and space).  Elements are converted to strings as
	 * by {@link String#valueOf(Object)}.
	 *
	 * @return a string representation of this collection
	 */
	@Override
	public String toString()
	{
		return String.format("RingBuffer(%,d/%,d, v%,d)", m_valueCount, m_valueList.length, m_version);
	}

	private static class RingBufferIterator<T> implements Iterator<T>
	{
		private final RingBufferCollection<T>	m_ringBuffer;
		private int					m_version;			// Version of the RingBuffer for which this iterator is valid.
		private int					m_currentIndex;		// Index of the element to return on a call to next
		private int					m_countRemaining;

		public RingBufferIterator(RingBufferCollection<T> ringBuffer)
		{
			m_ringBuffer = ringBuffer;
			synchronized (m_ringBuffer)
			{
				m_version = ringBuffer.m_version;
				m_currentIndex = ringBuffer.m_tailIndex;
				m_countRemaining = ringBuffer.m_valueCount;
			}
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
			validateVersion();
			return 0 != m_countRemaining;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws java.util.NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public T next()
		{
			validateVersion();
			if (0 == m_countRemaining)
			{
				throw new NoSuchElementException("There are no more elements to iterate.");
			}

			T		value		= m_ringBuffer.m_valueList[m_currentIndex++];
			if (m_ringBuffer.m_valueList.length == m_currentIndex)
			{
				m_currentIndex = 0;
			}
			m_countRemaining--;

			return value;
		}

		private void validateVersion()
		{
			if (m_ringBuffer.m_version != m_version)
			{
				throw new ConcurrentModificationException("RingBuffer modification during iteration not supported.");
			}
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
			throw new UnsupportedOperationException();
		}
	}
}
