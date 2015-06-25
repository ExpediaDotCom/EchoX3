/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.map;

import java.io.Serializable;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.tools.misc.PrimeNumbers;

public class ArrayMap<T> implements Map<Integer, T>, Iterable<Map.Entry<Integer, T>>, Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	// TARGET_ITEM_PER_BUCKET is a trade-off between overhead of buckets and cost of binary search
	public static final int		TARGET_ITEM_PER_BUCKET		= 1000;
	// DEFAULT_ITEM_COUNT is some minimal value (same as Java)
	public static final int		DEFAULT_ITEM_COUNT			= 16;
	// MIN_ITEM_PER_BUCKET to avoid buckets so small that they cause too much overhead
	public static final int		MIN_ITEM_PER_BUCKET			= 10;

	private SortedObjectArrayMap[]		m_arrayList;
	private int							m_targetItemPerBucket;

	/**
	 * m_version is used to detect ConcurrentModification.
	 * While the class ArrayMap is not threadSafe (the caller is responsible for [RW] locks),
	 * the ConcurrentModification code (and the variable m_version) must be thread safe...
	 * ... hence the use of m_version.
	 * Arguably, volatile might be sufficient; AtomicLong is guaranteed to be correct.
	 */
	private transient AtomicInteger		m_version		= new AtomicInteger(0);

	public ArrayMap()
	{
		this(DEFAULT_ITEM_COUNT, 0);
	}
	public ArrayMap(long itemCount)
	{
		this(itemCount, 0);
	}
	public ArrayMap(long itemCount, int bucketCount)
	{
		if (0 == bucketCount)
		{
			bucketCount = (int) (itemCount / TARGET_ITEM_PER_BUCKET);
			bucketCount = Math.max(10, bucketCount);				// At least this many buckets
			bucketCount = PrimeNumbers.nextPrime(bucketCount);		// To avoid patterns that prefer specific buckets
			m_targetItemPerBucket = ((int) itemCount / (bucketCount - 1));
		}
		else
		{
			m_targetItemPerBucket = (int) Math.min(itemCount / bucketCount, Integer.MAX_VALUE);
		}
		m_targetItemPerBucket = Math.max(m_targetItemPerBucket, MIN_ITEM_PER_BUCKET);

		reset(bucketCount);
	}

	public int getBucketCount()
	{
		return m_arrayList.length;
	}

	private void reset(int bucketCount)
	{
		m_arrayList = new SortedObjectArrayMap[bucketCount];
		for (int i = 0; i < m_arrayList.length; i++)
		{
			m_arrayList[i] = new SortedObjectArrayMap(m_targetItemPerBucket);
		}
		m_version.incrementAndGet();
	}

	/**
	 * Returns the number of key-value mappings in this map.  If the
	 * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size()
	{
		long		size		= 0;
		for (int i = 0; i < m_arrayList.length; i++)
		{
			SortedObjectArrayMap		objectArray		= m_arrayList[i];
			size += objectArray.size();
		}
		size = size > Integer.MAX_VALUE ? Integer.MAX_VALUE : size;

		return (int) size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	@Override
	public boolean isEmpty()
	{
		for (int i = 0; i < m_arrayList.length; i++)
		{
			SortedObjectArrayMap	objectArray		= m_arrayList[i];
			if (!objectArray.isEmpty())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the specified
	 * key.  More formally, returns <tt>true</tt> if and only if
	 * this map contains a mapping for a key <tt>k</tt> such that
	 * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
	 * at most one such mapping.)
	 *
	 * @param key key whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map contains a mapping for the specified
	 *         key
	 * @throws ClassCastException   if the key is of an inappropriate type for
	 *                              this map
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified key is null and this map
	 *                              does not permit null keys
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	public boolean containsKey(Object key)
	{
		return getBucket(key).contains((Integer) key);
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value.  More formally, returns <tt>true</tt> if and only if
	 * this map contains at least one mapping to a value <tt>v</tt> such that
	 * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
	 * will probably require time linear in the map size for most
	 * implementations of the <tt>Map</tt> interface.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *         specified value
	 * @throws ClassCastException   if the value is of an inappropriate type for
	 *                              this map
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified value is null and this
	 *                              map does not permit null values
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean containsValue(Object value)
	{
		ArrayMapValueList<T>		valueList		= new ArrayMapValueList<>((ArrayMapEntrySet<T>) entrySet());

		return valueList.contains(value);
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 * <p/>
	 * <p>More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise
	 * it returns {@code null}.  (There can be at most one such mapping.)
	 * <p/>
	 * <p>If this map permits null values, then a return value of
	 * {@code null} does not <i>necessarily</i> indicate that the map
	 * contains no mapping for the key; it's also possible that the map
	 * explicitly maps the key to {@code null}.  The {@link #containsKey
	 * containsKey} operation may be used to distinguish these two cases.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or
	 *         {@code null} if this map contains no mapping for the key
	 * @throws ClassCastException   if the key is of an inappropriate type for
	 *                              this map
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified key is null and this map
	 *                              does not permit null keys
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T get(Object key)
	{
		long	version		= getVersion();
		T		value		= (T) getBucket(key).getValueById((Integer) key);
		validateVersion(version);

		return value;
	}

	/**
	 * Associates the specified value with the specified key in this map
	 * (optional operation).  If the map previously contained a mapping for
	 * the key, the old value is replaced by the specified value.  (A map
	 * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
	 * if {@link #containsKey(Object) m.containsKey(k)} would return
	 * <tt>true</tt>.)
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with <tt>key</tt>, or
	 *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 *         (A <tt>null</tt> return can also indicate that the map
	 *         previously associated <tt>null</tt> with <tt>key</tt>,
	 *         if the implementation supports <tt>null</tt> values.)
	 * @throws UnsupportedOperationException if the <tt>put</tt> operation
	 *                                       is not supported by this map
	 * @throws ClassCastException            if the class of the specified key or value
	 *                                       prevents it from being stored in this map
	 * @throws NullPointerException          if the specified key or value is null
	 *                                       and this map does not permit null keys or values
	 * @throws IllegalArgumentException      if some property of the specified key
	 *                                       or value prevents it from being stored in this map
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T put(Integer key, T value)
	{
		SortedObjectArrayMap	bucket		= getBucket(key);
		long					version		= getVersion();
		int						keyCount	= bucket.size();			// Not size, to include the key -> null
		T						valueSav	= (T) bucket.put(key, value);

		// If no new entry is added, version does not need to change as iterators are still valid.
		if (bucket.size() != keyCount)
		{
			m_version.incrementAndGet();
			version++;
		}
		validateVersion(version);

		return valueSav;
	}

	/**
	 * Removes the mapping for a key from this map if it is present
	 * (optional operation).   More formally, if this map contains a mapping
	 * from key <tt>k</tt> to value <tt>v</tt> such that
	 * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
	 * is removed.  (The map can contain at most one such mapping.)
	 * <p/>
	 * <p>Returns the value to which this map previously associated the key,
	 * or <tt>null</tt> if the map contained no mapping for the key.
	 * <p/>
	 * <p>If this map permits null values, then a return value of
	 * <tt>null</tt> does not <i>necessarily</i> indicate that the map
	 * contained no mapping for the key; it's also possible that the map
	 * explicitly mapped the key to <tt>null</tt>.
	 * <p/>
	 * <p>The map will not contain a mapping for the specified key once the
	 * call returns.
	 *
	 * @param key key whose mapping is to be removed from the map
	 * @return the previous value associated with <tt>key</tt>, or
	 *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 * @throws UnsupportedOperationException if the <tt>remove</tt> operation
	 *                                       is not supported by this map
	 * @throws ClassCastException            if the key is of an inappropriate type for
	 *                                       this map
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if the specified key is null and this
	 *                                       map does not permit null keys
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	public T remove(Object key)
	{
		if (key instanceof Integer)
		{
			return put((Integer) key, null);
		}
		else
		{
			throw new ClassCastException("Keys must be of type Integer.class.");
		}
	}

	/**
	 * Copies all of the mappings from the specified map to this map
	 * (optional operation).  The effect of this call is equivalent to that
	 * of calling {@link #put(Object, Object) put(k, v)} on this map once
	 * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
	 * specified map.  The behavior of this operation is undefined if the
	 * specified map is modified while the operation is in progress.
	 *
	 * @param	map							 mappings to be stored in this map
	 * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
	 *                                       is not supported by this map
	 * @throws ClassCastException            if the class of a key or value in the
	 *                                       specified map prevents it from being stored in this map
	 * @throws NullPointerException          if the specified map is null, or if
	 *                                       this map does not permit null keys or values, and the
	 *                                       specified map contains null keys or values
	 * @throws IllegalArgumentException      if some property of a key or value in
	 *                                       the specified map prevents it from being stored in this map
	 */
	@Override
	public void putAll(Map<? extends Integer, ? extends T> map)
	{
		for (Entry<? extends Integer, ? extends T> entry : map.entrySet())
		{
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Removes all of the mappings from this map (optional operation).
	 * The map will be empty after this call returns.
	 *
	 * @throws UnsupportedOperationException if the <tt>clear</tt> operation
	 *                                       is not supported by this map
	 */
	@Override
	public void clear()
	{
		reset(m_arrayList.length);
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own <tt>remove</tt> operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Set<Integer> keySet()
	{
		return new ArrayMapKeySet((ArrayMapEntrySet) entrySet());
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is backed by the map, so changes to the map are
	 * reflected in the collection, and vice-versa.  If the map is
	 * modified while an iteration over the collection is in progress
	 * (except through the iterator's own <tt>remove</tt> operation),
	 * the results of the iteration are undefined.  The collection
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
	 * support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the values contained in this map
	 */
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<T> values()
	{
		return new ArrayMapValueList((ArrayMapEntrySet) entrySet());
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own <tt>remove</tt> operation, or through the
	 * <tt>setValue</tt> operation on a map entry returned by the
	 * iterator) the results of the iteration are undefined.  The set
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
	 * <tt>clear</tt> operations.  It does not support the
	 * <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a set view of the mappings contained in this map
	 */
	@Override
	public Set<Entry<Integer, T>> entrySet()
	{
		return new ArrayMapEntrySet<>(this);
	}

	/**
	 * Returns an iterator over a set of elements of type T.
	 *
	 * @return an Iterator.
	 */
	@Override
	public Iterator<Entry<Integer, T>> iterator()
	{
		return new ArrayMapIterator();
	}

	private SortedObjectArrayMap getBucket(Object keyObject)
	{
		int			key				= Math.abs((Integer) keyObject);
		int			bucketNo		= key % m_arrayList.length;
		return m_arrayList[bucketNo];
	}

	private long getVersion()
	{
		return m_version.get();
	}

	private void validateVersion(long versionPrev)
	{
		if (getVersion() != versionPrev)
		{
			throw new ConcurrentModificationException("ArrayMap was modified during operation. Use appropriate locks");
		}
	}

	@Override
	public String toString()
	{
		return String.format("%s(%,d items, v %,d)", getClass().getSimpleName(), size(), m_version.get());
	}


	public class ArrayMapIterator implements Iterator<Entry<Integer, T>>, Serializable
	{
		public static final long					serialVersionUID	= 20150601085959L;
		private static final int					INVALID_INDEX		= -1;

		private int							m_currentArray		= 0;
		private Iterator<Integer>			m_iterator;
		private long						m_version;
		private int							m_nextIndex			= 0;

		public ArrayMapIterator()
		{
			m_version = getVersion();
			m_iterator = m_arrayList[m_currentArray].iterator();
			prepareNext();
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
			return INVALID_INDEX != m_nextIndex;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Entry<Integer, T> next()
		{
			validateVersion(m_version);

			int					key			= m_arrayList[m_currentArray].getKey(m_nextIndex);
			T					value		= (T) m_arrayList[m_currentArray].getValueByIndex(m_nextIndex);
			Entry<Integer, T>	entry		= new ArrayMapEntry<>(key, value);
			prepareNext();

			return entry;
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
			throw new UnsupportedOperationException("Remove not supported on this iterator.");
		}

		private void prepareNext()
		{
			// Walk the possible values until a non-null is found...
			do
			{
				if (!m_iterator.hasNext())
				{
					if ((m_currentArray + 1) < m_arrayList.length)
					{
						m_iterator = m_arrayList[++m_currentArray].iterator();
						m_nextIndex = 0;
					}
					else
					{
						m_nextIndex = INVALID_INDEX;
						break;
					}
				}
				if (m_iterator.hasNext())
				{
					m_nextIndex = m_iterator.next();
				}
			} while (null == m_arrayList[m_currentArray].getValueByIndex(m_nextIndex));
		}
	}

	public static class ArrayMapEntry<T> implements Entry<Integer, T>, Comparable<Entry<Integer, T>>, Serializable
	{
		public static final long					serialVersionUID	= 20150601085959L;

		private int			m_key;
		private T			m_value;

		public ArrayMapEntry(int key, T value)
		{
			m_key = key;
			m_value = value;
		}

		/**
		 * Returns the key corresponding to this entry.
		 *
		 * @return the key corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		public Integer getKey()
		{
			return m_key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * <tt>remove</tt> operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		public T getValue()
		{
			return m_value;
		}

		/**
		 * Replaces the value corresponding to this entry with the specified
		 * value (optional operation).  (Writes through to the map.)  The
		 * behavior of this call is undefined if the mapping has already been
		 * removed from the map (by the iterator's <tt>remove</tt> operation).
		 *
		 * @param value new value to be stored in this entry
		 * @return old value corresponding to the entry
		 * @throws UnsupportedOperationException if the <tt>put</tt> operation
		 *                                       is not supported by the backing map
		 * @throws ClassCastException            if the class of the specified value
		 *                                       prevents it from being stored in the backing map
		 * @throws NullPointerException          if the backing map does not permit
		 *                                       null values, and the specified value is null
		 * @throws IllegalArgumentException      if some property of this value
		 *                                       prevents it from being stored in the backing map
		 * @throws IllegalStateException         implementations may, but are not
		 *                                       required to, throw this exception if the entry has been
		 *                                       removed from the backing map.
		 */
		@Override
		public T setValue(T value)
		{
			throw new UnsupportedOperationException("Not supported by this Map implementation.");
		}

		/**
		 * Compares this object with the specified object for order.  Returns a
		 * negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object.
		 * <p/>
		 * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
		 * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
		 * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
		 * <tt>y.compareTo(x)</tt> throws an exception.)
		 * <p/>
		 * <p>The implementor must also ensure that the relation is transitive:
		 * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
		 * <tt>x.compareTo(z)&gt;0</tt>.
		 * <p/>
		 * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
		 * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
		 * all <tt>z</tt>.
		 * <p/>
		 * <p>It is strongly recommended, but <i>not</i> strictly required that
		 * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
		 * class that implements the <tt>Comparable</tt> interface and violates
		 * this condition should clearly indicate this fact.  The recommended
		 * language is "Note: this class has a natural ordering that is
		 * inconsistent with equals."
		 * <p/>
		 * <p>In the foregoing description, the notation
		 * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
		 * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
		 * <tt>0</tt>, or <tt>1</tt> according to whether the value of
		 * <i>expression</i> is negative, zero or positive.
		 *
		 * @param entry the object to be compared.
		 * @return a negative integer, zero, or a positive integer as this object
		 *         is less than, equal to, or greater than the specified object.
		 * @throws NullPointerException if the specified object is null
		 * @throws ClassCastException   if the specified object's type prevents it
		 *                              from being compared to this object.
		 */
		@Override
		public int compareTo(Entry<Integer, T> entry)
		{
			return Integer.compare(m_key, entry.getKey());
		}

		@Override
		public int hashCode()
		{
			return getKey().hashCode();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean equals(Object o)
		{
			if (o instanceof Entry)
			{
				Entry<Integer, T>		entry		= (Entry<Integer, T>) o;
				return getKey().intValue() == entry.getKey() && getValue().equals(entry.getValue());
			}
			else if (o instanceof Integer)
			{
				return getKey() == (int) o;
			}
			else
			{
				return false;
			}
		}

		@Override
		public String toString()
		{
			return String.format("%,d; %s", getKey(), getValue().toString());
		}
	}
}
