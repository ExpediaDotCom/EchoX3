/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.Arrays;

public class CopyOnSizeArray<T>
{
	volatile private T[]		m_array;

	@SuppressWarnings("unchecked")
	public CopyOnSizeArray(int size)
	{
		m_array = (T[]) new Object[size];
	}

	public void set(int index, T value)
	{
		m_array[index] = value;
	}

	public T get(int index)
	{
		return m_array[index];
	}

	public void resize(int size)
	{
		if (size == m_array.length)
		{
			return;
		}

		@SuppressWarnings("unchecked")
		T[]		array		= (T[]) Arrays.copyOf(m_array, size);
		m_array = array;
	}

	public int length()
	{
		return m_array.length;
	}
}
