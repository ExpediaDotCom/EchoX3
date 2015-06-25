/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.configuration;

public class MemoryConfigurationProvider extends AbstractConfigurationProvider
{
	public MemoryConfigurationProvider(String name)
	{
		super(name);

		ConfigurationManager.getInstance().addProvider(this);
	}

	@Override
	public boolean addSetting(String name, String value)
	{
		final boolean isChanged = super.addSetting(name, value);

		ConfigurationManager.getInstance().postChangeEvent(ConfigurationManager.REASON_PROVIDER_CHANGE, this);

		return isChanged;
	}

	@Override
	public void close()
	{
		ConfigurationManager.getInstance().removeProvider(this);
		super.close();
	}
}
