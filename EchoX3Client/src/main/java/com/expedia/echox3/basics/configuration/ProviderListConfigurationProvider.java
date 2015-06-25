/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class ProviderListConfigurationProvider extends AbstractConfigurationProvider
{
	private final Map<? super Comparable<?>, FileConfigurationProvider>		m_providerMap		= new TreeMap<>();

	protected ProviderListConfigurationProvider(Object source)
	{
		super(source);
	}

	public Map<? super Comparable<?>, FileConfigurationProvider> getProviderMap()
	{
		return m_providerMap;
	}

	@Override
	public String getSetting(String name)
	{
		synchronized (getProviderMap())
		{
			for (IConfigurationProvider provider : getProviderMap().values())
			{
				String value = provider.getSetting(name);
				if (null != value)
				{
					return value;
				}
			}
		}
		return null;
	}

	@Override
	public Set<String> getSettingNameSet(String prefix)
	{
		Set<String> nameSet		= new TreeSet<>();

		synchronized (getProviderMap())
		{
			for (IConfigurationProvider provider : getProviderMap().values())
			{
				Set<String> set = provider.getSettingNameSet(prefix);
				nameSet.addAll(set);
			}
		}

		return nameSet;
	}


	@Override
	public void close()
	{
		ConfigurationManager.getInstance().removeProvider(this);

		synchronized (getProviderMap())
		{
			for (IConfigurationProvider provider : getProviderMap().values())
			{
				try
				{
					provider.close();
				}
				catch (IOException e)
				{
					// Close is on its own if it fails :(
				}
			}
			getProviderMap().clear();
		}
	}
}
