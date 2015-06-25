/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools.misc;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PermanentMessageContainer
{
	private static final PermanentMessageContainer		INSTANCE		= new PermanentMessageContainer();

	private final Map<String, String> m_permanentMessageMap				= new TreeMap<>();

	private PermanentMessageContainer()
	{
		// Singleton
	}

	public static PermanentMessageContainer getInstance()
	{
		return INSTANCE;
	}

	public void registerMessage(boolean isRegister, String key, String message)
	{
		if (isRegister)
		{
			registerMessage(key, message);
		}
		else
		{
			deregisterMessage(key);
		}
	}

	public void registerMessage(String key, String message)
	{
		synchronized (m_permanentMessageMap)
		{
			m_permanentMessageMap.put(key, message);
		}
	}

	public void deregisterMessage(String key)
	{
		synchronized (m_permanentMessageMap)
		{
			m_permanentMessageMap.remove(key);
		}
	}

	public List<String> getMessageList()
	{
		List<String> list	= new LinkedList<>();
		synchronized (m_permanentMessageMap)
		{
			list.addAll(m_permanentMessageMap.values());
		}
		return list;
	}
}
