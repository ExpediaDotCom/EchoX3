/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to require the absolute minimal synchronization (volatile)
 * in the m_map for a map that is a choke point (accessed on every single call) in ReadOnly mode,
 * while allowing for the rare (startup, then may be once per day or per week) write.
 *
 * @param <K>	Key
 * @param <V>	Value
 */
public class CopyOnWriteSimpleMap<K, V>
{
	private volatile Map<K, V>		m_map			= new HashMap<>();

	public V get(K key)
	{
		return m_map.get(key);
	}

	public V put(K key, V value)
	{
		Map<K, V>		map			= new HashMap<>(m_map);
		V				previous	= map.put(key, value);
		m_map = map;

		return previous;
	}

	public V remove(K key)
	{
		Map<K, V>		map			= new HashMap<>();
		map.putAll(m_map);
		V				previous	= map.remove(key);
		m_map = map;

		return previous;
	}
}
