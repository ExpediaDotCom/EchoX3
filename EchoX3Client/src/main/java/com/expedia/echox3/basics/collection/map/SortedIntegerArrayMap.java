/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.map;

import java.io.Serializable;
import java.util.Arrays;

public class SortedIntegerArrayMap extends SortedArray implements Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	public static final int		NOT_FOUND						= 0;

	private int[]		m_objectList;

	public SortedIntegerArrayMap()
	{
		this(DEFAULT_SIZE_INCREMENT);
	}

	public SortedIntegerArrayMap(int sizeIncrement)
	{
		super(sizeIncrement);

		m_objectList = new int[sizeIncrement];
	}

/*	Not needed
	@Override
	public void clear()
	{
		super.clear();
	}
*/

	@Override
	public int put(int key)
	{
		throw new IllegalArgumentException("Use the put(int, Object) entry point to keep the arrays in sync");
	}
	@Override
	public void putAll(Iterable<Integer> array)
	{
		throw new IllegalArgumentException("Use the put(int, Object) entry point to keep the arrays in sync");
	}

	public int put(int id, int value)
	{
		int			index		= super.put(id);
		int			valueSav	= m_objectList[index];
		m_objectList[index] = value;

		return valueSav;
	}

	public int getValueByIndex(int index)
	{
		return m_objectList[index];
	}
	public int getValueById(int id)
	{
		int		index		= find(id);
		if (getKey(index) == id)
		{
			return getValueByIndex(index);
		}
		else
		{
			return NOT_FOUND;
		}
	}

	@Override
	public boolean contains(int id)
	{
		int			index		= find(id);
		int			key			= getKey(index);
		if (id == key)
		{
			int		value		= getValueByIndex(index);
			return NOT_FOUND != value;
		}
		else
		{
			return false;
		}
	}

	@Override
	protected void increaseSize(int newSize)
	{
		super.increaseSize(newSize);
		m_objectList = Arrays.copyOf(m_objectList, newSize);
	}

	@SuppressWarnings("PMD.AvoidArrayLoops")		// Must copy from tail towards beginning.
	protected void moveItemsUp(int i1, int i2)
	{
		super.moveItemsUp(i1, i2);

		System.arraycopy(m_objectList, i1, m_objectList, i1 + 1, i2 - i1);
	}

	@Override
	protected String getPointAsText(int i)
	{
		return String.format("(%,d; 0x%2x)", getKey(i), getValueByIndex(i));
	}
}
