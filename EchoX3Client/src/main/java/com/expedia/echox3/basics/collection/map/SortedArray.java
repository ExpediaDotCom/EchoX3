/**
 * Copyright 2011-2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection.map;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SortedArray implements Iterable<Integer>, Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	public static final int		DEFAULT_SIZE_INCREMENT		= 10;

	private String		m_name;
	private final int	m_sizeIncrement;
	private int[]		m_keyList;
	private int 		m_idCount;

	public SortedArray()
	{
		this(DEFAULT_SIZE_INCREMENT);
	}
	public SortedArray(int sizeIncrement)
	{
		m_sizeIncrement = sizeIncrement;

		m_keyList = new int[m_sizeIncrement];
		m_idCount = 0;
	}

	public String getName()
	{
		return m_name;
	}

	/**
	 * Give a meaningful name to your SortedArray, so it can be better understood in the ContentViewer.
	 *
	 * @param name	Name that will appear in the toString() method
	 */
	public void setName(String name)
	{
		m_name = name;
	}

	public void putAll(Iterable<Integer> list)
	{
		for (int i : list)
		{
			put(i);
		}
	}

	/**
	 * Extending classes should override increase and insertItem to keep their arrays in sync.
	 *
	 * @param key		The id of the new entry
	 * @return 			The position of the new element, so an extending class can insert in a matching array.
	 */
	public int put(int key)
	{
		int		index		= find(key);

		if (index == m_idCount)
		{
			// insert at the last position
			ensureSize(++m_idCount);
		}
		else if (m_keyList[index] != key)
		{
			int			oldSize		= m_idCount;
			ensureSize(++m_idCount);
			moveItemsUp(index, oldSize);
		}
/*
		else	// Replace at existing position
		{

		}
*/
		m_keyList[index] = key;

//		validateOrder();

		return index;
	}

	private void ensureSize(int newSize)
	{
		if (newSize > m_keyList.length)
		{
			// Need to resize
			newSize = m_keyList.length + m_sizeIncrement;
			increaseSize(newSize);
		}
	}

	/**
	 * extending classes must override this method, and call the base method.
	 *
	 * @param newSize	new size of the matching array
	 */
	protected void increaseSize(int newSize)
	{
		m_keyList = Arrays.copyOf(m_keyList, newSize);
	}

	@SuppressWarnings("PMD.AvoidArrayLoops")		// Must copy from tail towards beginning.
	protected void moveItemsUp(int i1, int i2)
	{
//		for (int i = i2; i > i1; i--)
//		{
//			m_keyList[i] = m_keyList[i - 1];
//		}

		System.arraycopy(m_keyList, i1, m_keyList, i1 + 1, i2 - i1);
	}

	public void clear()
	{
		m_idCount = 0;
	}
	public boolean isEmpty()
	{
		return 0 == m_idCount;
	}
	public int size()
	{
		return m_idCount;
	}

	public int getKey(int index)
	{
		if (index == m_idCount)
		{
			// Past the end of the list, return an invalid id, so it matches nothing
			// This would return 0 if there are unused entries at the end of the array,
			// but would throw if all values in the array are used.
			return -1;
		}
		else
		{
			return m_keyList[index];
		}
	}

	/**
	 * BEWARE: Not all elements are necessarily used.
	 *
	 * @return	The internal array, use cautiously
	 */
/* Are these in use?
	@SuppressWarnings("PMD.MethodReturnsInternalArray")
	public int[] getKeyListRaw()
	{
		return m_keyList;
	}
	public int[] getKeyListCopy()
	{
		return Arrays.copyOf(m_keyList, size());
	}
*/

	public boolean contains(int key)
	{
		int			index		= find(key);

		if (m_keyList.length == index)
		{
			return false;
		}
		else
		{
			return m_keyList[find(key)] == key;
		}
	}

	// Returns the position where the elements belong if it is not found.
	// [ 3, 7, 8 ]
	//	7 return 1
	//	6 return 1
	public int find(int id)
	{
		int		indexMin	= 0;
		int		indexMax	= m_idCount;
		int		indexOut;

		while (true)
		{
			int		indexMiddle		= (indexMin + indexMax) / 2;
			if (indexMiddle == m_idCount)
			{
				indexOut = m_idCount;
				break;
			}

			int		idMiddle		= m_keyList[indexMiddle];
			if (idMiddle == id)
			{
				indexOut = indexMiddle;
				break;
			}
			else if (idMiddle > id)
			{
				if (indexMin == indexMax)
				{
					indexOut = indexMin;
					break;
				}
				indexMax = indexMiddle;		// No -1, to return the location where a not found belongs.
			}
			else
			{
				if (indexMin == indexMax)
				{
					indexOut = indexMin + 1;
					break;
				}
				indexMin = indexMiddle + 1;
			}
		}

		return indexOut;
	}

	public String validate()
	{
		String result;

		result = validateOrder();
		if (null == result)
		{
			result = validateFind();
		}

		return result;
	}
	public String validateOrder()
	{
		for (int i = 0; i < m_idCount -1; i++)
		{
			if (m_keyList[i] >= m_keyList[i + 1])
			{
				return String.format("*** ERROR *** Item %,d is out of order", i);
			}
		}
		return null;
	}

	public String validateFind()
	{
		for (int i = 0; i < size(); i++)
		{
			int		idOriginal	= m_keyList[i];
			int		index		= find(idOriginal);
			int		idFound		= m_keyList[index];
			if (idFound != idOriginal)
			{
				idFound = find(idOriginal);
				return String.format("*** ERROR *** %,d != %,d @ index %,d", idOriginal, index, idFound);
			}
		}
		return null;
	}

	@Override
	public Iterator<Integer> iterator()
	{
		return new IteratorByIndex(this);
	}

	/**
	 * Extending classes Override this method to format point "i" appropriately for the toString() method.
	 *
	 * @param i		The index of the point to format
	 * @return		An appropriately format point.
	 */
	protected String getPointAsText(int i)
	{
		return String.format("(%,d)", getKey(i));
	}

	@Override
	public String toString()
	{
		String text		= String.format("%s (%s, %,d values): ",
				getClass().getSimpleName(), m_name, m_idCount);

		String prefix		= "";
		int					iMax		= Math.min(100, size());
		StringBuilder sb 			= new StringBuilder(iMax * 20);
		sb.append(text);
		for (int i = 0; i < iMax; i++)
		{
			sb.append(prefix);
			sb.append(getPointAsText(i));
			prefix = ", ";
		}
		if (size() > iMax)
		{
			sb.append(prefix);
			sb.append("...");
		}

		return sb.toString();
	}


	public static class IteratorByIndex implements Iterator<Integer>, Serializable
	{
		public static final long					serialVersionUID	= 20150601085959L;

		private SortedArray		m_array;
		private Integer			m_nextIndex;		// Index to return on the next call to next()

		public IteratorByIndex(SortedArray array)
		{
			m_array = array;
			if (0 < m_array.size())
			{
				m_nextIndex = 0;
			}
			else
			{
				m_nextIndex = null;
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
			return null != m_nextIndex;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public Integer next()
		{
			if (null == m_nextIndex)
			{
				throw new NoSuchElementException("Iteration exhausted.");
			}

			int			index		= m_nextIndex;

			m_nextIndex = index + 1;
			if (m_array.size() == m_nextIndex)
			{
				m_nextIndex = null;
			}

			return index;
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
	}
}
