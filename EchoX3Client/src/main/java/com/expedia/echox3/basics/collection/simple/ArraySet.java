/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection.simple;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements the Set interface using a sorted array. Null elements are not allowed. This class is not thread-safe and
 * must be synchronized externally. It does, however, attempt to detect concurrent modifications and notify the caller
 * of them by throwing ConcurrentModificationException.
 *
 * @param <E> generic: the type of object in this ArraySet
 */
public class ArraySet<E extends Comparable<E>> implements Set<E>, Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private static final int	DEFAULT_SIZE_INCREMENT		= 10;

	private final AtomicInteger		m_version;
	private final int				m_sizeIncrement;
	private Object[]				m_objectArray;

	private int						m_size;

	public ArraySet()
	{
		this(DEFAULT_SIZE_INCREMENT, DEFAULT_SIZE_INCREMENT);
	}

	public ArraySet(int initialSize, int sizeIncrement)
	{
		m_version			= new AtomicInteger(0);
		m_sizeIncrement		= sizeIncrement;
		m_objectArray		= new Object[initialSize];
		m_size				= 0;
	}


	// Public methods not in the Set<E> contract
	// -----------------------------------------
	@SuppressWarnings("unchecked")
	public E find(E element)
	{
		final int		index		= findInternal(element);
		return index < 0 ? null : (E) m_objectArray[index];
	}

	// Overriding methods from Set<E> that do not change the contents, and therefore do not do version checking
	// --------------------------------------------------------------------------------------------------------
	@Override
	public int size()
	{
		return m_size;
	}

	@Override
	public boolean isEmpty()
	{
		return 0 == m_size;
	}

	@Override
	public boolean contains(final Object o)
	{
		return findInternal(o) >= 0;
	}

	@Override
	public Iterator<E> iterator()
	{
		return new ArraySetIterator<>(this);
	}

	@Override
	public Object[] toArray()
	{
		final Object[]		array		= Arrays.copyOfRange(m_objectArray, 0, m_size);
		return array;
	}

	@Override
	@SuppressWarnings("unckecked")
	public <T> T[] toArray(T[] array)
	{
		if (array.length < m_size)
		{
			array = (T[]) Array.newInstance(array.getClass().getComponentType(), m_size);
		}
		else if(array.length > m_size)
		{
			Arrays.fill(array, m_size, array.length - 1, null);
		}
		//noinspection SuspiciousSystemArraycopy
		System.arraycopy(m_objectArray, 0, array, 0, m_size);
		return array;
	}

	@Override
	public boolean containsAll(final Collection<?> collection)
	{
		for(final Object object : collection)
		{
			if(findInternal(object) < 0)
			{
				return false;
			}
		}
		return true;
	}


	// Overriding methods from Set<E> that do change the contents, and therefore must do version checking
	// --------------------------------------------------------------------------------------------------
	@Override
	public boolean add(final E element)
	{
		final int		versionAtStart		= m_version.get();
		final int		index 				= findInternal(element);
		if(index >= 0)
		{
			throw new IllegalStateException("The item is already in the ArraySet.");
		}
		addInternal(element, index);
		final int		versionAtEnd		= m_version.getAndIncrement();
		if(versionAtStart != versionAtEnd)
		{
			throw new ConcurrentModificationException("Another thread changed the ArraySet during add()");
		}
		return true;
	}

	@Override
	public boolean remove(final Object elementToRemove)
	{
		final int		versionAtStart		= m_version.get();
		@SuppressWarnings("unchecked")
		final int		index				= findInternal(elementToRemove);
		final boolean	remove				= remove(index);
		if(remove)
		{
			final int	versionAtEnd		= m_version.getAndIncrement();
			if (versionAtStart != versionAtEnd)
			{
				throw new ConcurrentModificationException("Another thread changed the ArraySet during remove()");
			}
		}
		return remove;
	}

	@Override
	public boolean addAll(final Collection<? extends E> elementsToAdd)
	{
		final int			versionAtStart		= m_version.get();
		boolean				changed				= false;
		for(final E element : elementsToAdd)
		{
			final int		index 				= findInternal(element);
			if(index < 0)
			{
				changed							= true;
				addInternal(element, index);
			}
		}
		if(changed)
		{
			final int	versionAtEnd		= m_version.getAndIncrement();
			if (versionAtStart != versionAtEnd)
			{
				throw new ConcurrentModificationException("Another thread changed the ArraySet during addAll()");
			}
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> collection)
	{
		final int					versionAtStart		= m_version.get();
		int							newSize				= 0;
		final BooleanBitArray		bitArray64			= new BooleanBitArray(m_size);
		for(final Object object : collection)
		{
			//noinspection unchecked
			final int				index			= findInternal(object);
			if(index >= 0)
			{
				bitArray64.set(index);
				++newSize;
			}
		}
		if(newSize != m_size)
		{
			final Object[]			newData			= new Object[newSize];
			int						newIndex		= 0;
			for (int oldIndex = 0; oldIndex < m_size; oldIndex++)
			{
				if (bitArray64.get(oldIndex))
				{
					newData[newIndex++]				= m_objectArray[oldIndex];
				}
			}
			m_objectArray							= newData;
			m_size									= newSize;
			int						versionAtEnd	= m_version.getAndIncrement();
			if(versionAtStart != versionAtEnd)
			{
				throw new ConcurrentModificationException("Another thread changed the ArraySet during retainAll()");
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(final Collection<?> collection)
	{
		final int			versionAtStart		= m_version.get();
		boolean				changed				= false;
		for(final Object object : collection)
		{
			//noinspection unchecked
			final int		index				= findInternal(object);
			if(index >= 0)
			{
				changed							|= remove(index);
			}
		}
		if(changed)
		{
			final int		versionAtEnd		= m_version.getAndIncrement();
			if (versionAtStart != versionAtEnd)
			{
				throw new ConcurrentModificationException("Another thread changed the ArraySet during removeAll()");
			}
		}
		return changed;
	}

	@Override
	public void clear()
	{
		final int versionAtStart				= m_version.incrementAndGet();
		Arrays.fill(m_objectArray, null);
		m_size									= 0;
		final int			versionAtEnd		= m_version.getAndIncrement();
		if (versionAtStart != versionAtEnd)
		{
			throw new ConcurrentModificationException("Another thread changed the ArraySet during clear()");
		}
	}

	// Private methods never do version checking; that responsibility rests with the public methods above
	// --------------------------------------------------------------------------------------------------
	/**
	 * Determines if an Object is in this ArraySet.
	 *
	 * @param element the object to look for
	 * @return the index of the Object (if the object is found) or the <b>negative</b> value of the index <b>after</b>
	 * where the object would be (if the object is not found). This contract means that an empty ArraySet will always
	 * return -1, which indicates that the element should be added at index -(-1) - 1 = 0, allowing the ArraySet to
	 * hold Integer.MAX_VALUE values.
	 * <ul>
	 *     <li>Empty set returns -1 (because the Object would always be put at location 0)</li>
	 *     <li>[ 3, 7, 9 ]
	 *         <ul>
	 *             <li><= 2 returns -1</li>
	 *             <li>3 returns 0</li>
	 *             <li>4, 5, 6 return -2</li>
	 *             <li>7 returns 1</li>
	 *             <li>8 returns -3</li>
	 *             <li>9 returns 2</li>
	 *             <li>>= 10 returns -4</li>
	 *         </ul>
	 *     </li>
	 * </ul>
	 */
	private int findInternal(Object element)
	{
		return Arrays.binarySearch(m_objectArray, 0, m_size, element);
	}

	private void addInternal(E element, int insertionPoint)
	{
		++m_size;
		ensureSize();
		final int		index		= -insertionPoint - 1;
		makeHole(index);
		m_objectArray[index]		= element;
	}

	private void makeHole(int index)
	{
		final int		length		= m_size - index - 1;
		System.arraycopy(m_objectArray, index, m_objectArray, index + 1, length);
	}

	private void ensureSize()
	{
		if(m_size > m_objectArray.length)
		{
			m_objectArray = Arrays.copyOf(m_objectArray, m_objectArray.length + m_sizeIncrement);
		}
	}

	private boolean remove(int index)
	{
		if(index >= 0)
		{
			final int	length			= --m_size - index;
			System.arraycopy(m_objectArray, index + 1, m_objectArray, index, length);
			m_objectArray[m_size]		= null;
			return true;
		}
		return false;
	}

	public static class ArraySetIterator<E extends Comparable<E>> implements Iterator<E>
	{
		private final	AtomicInteger	m_version;
		private final	ArraySet<E>		m_arraySet;
		private			int				m_index;
		private			boolean			m_canCallRemove;

		private ArraySetIterator(ArraySet<E> arraySet)
		{
			m_version				= new AtomicInteger(arraySet.m_version.get());
			m_arraySet				= arraySet;
			m_index					= -1;
			m_canCallRemove			= false;
		}

		@Override
		public boolean hasNext()
		{
			return (m_index + 1) < m_arraySet.size();
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next()
		{
			if(m_version.get() != m_arraySet.m_version.get())
			{
				throw new ConcurrentModificationException("next(): Another thread changed the ArraySet");
			}
			if(!hasNext())
			{
				throw new NoSuchElementException("There are no more elements to return");
			}
			m_canCallRemove			= true;
			return (E) m_arraySet.m_objectArray[++m_index];
		}

		@Override
		public void remove()
		{
			if(!m_canCallRemove)
			{
				throw new IllegalStateException("The method remove() can only be called once after calling next()");
			}
			//noinspection SuspiciousMethodCalls
			m_arraySet.remove(m_arraySet.m_objectArray[m_index--]);
			m_canCallRemove			= false;
			if(m_arraySet.m_version.get() != m_version.incrementAndGet())
			{
				throw new ConcurrentModificationException("remove(): Another thread changed the ArraySet");
			}
		}
	}
}
