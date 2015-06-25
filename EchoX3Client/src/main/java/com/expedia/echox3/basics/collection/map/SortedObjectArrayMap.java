/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection.map;

import java.io.Serializable;
import java.util.Arrays;

public class SortedObjectArrayMap extends SortedArray implements Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private Object[]	m_objectList;
	private int			m_countNull		= 0;


	public SortedObjectArrayMap()
	{
		this(DEFAULT_SIZE_INCREMENT);
	}

	public SortedObjectArrayMap(int sizeIncrement)
	{
		super(sizeIncrement);

		m_objectList = new Object[sizeIncrement];
	}

	@Override
	public void clear()
	{
		super.clear();
		Arrays.fill(m_objectList, null);
		m_countNull = 0;
	}

	@Override
	public boolean isEmpty()
	{
		return 0 == size();
	}

	@Override
	public int size()
	{
		return super.size() - m_countNull;
	}

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

	public Object put(int id, Object value)
	{
		int			index1		= super.find(id);
		boolean		wasNull		= false;
		Object valueSav	= null;
		if (getKey(index1) == id)
		{
			valueSav = m_objectList[index1];
			if (null == valueSav)
			{
				wasNull = true;
			}
		}

		int			index2		= super.put(id);
		m_objectList[index2] = value;

		if (null == value)
		{
			m_countNull += (wasNull ? 0 : 1);
		}
		else // null != value
		{
			m_countNull -= (wasNull ? 1 : 0);
		}

		return valueSav;
	}

	public Object getValueByIndex(int index)
	{
		return m_objectList[index];
	}
	public Object getValueById(int id)
	{
		int		index		= find(id);
		if (getKey(index) == id)
		{
			return getValueByIndex(index);
		}
		else
		{
			return null;
		}
	}

	@Override
	public boolean contains(int id)
	{
		int			index		= find(id);
		int			key			= getKey(index);
		if (id == key)
		{
			Object value		= getValueByIndex(index);
			return null != value;
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
		return String.format("(%,d; %s)", getKey(i), getValueByIndex(i).toString());
	}
}
