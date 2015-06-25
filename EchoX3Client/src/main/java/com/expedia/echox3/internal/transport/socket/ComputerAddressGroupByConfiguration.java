/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.util.HashSet;
import java.util.Set;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.pubsub.Publisher;

public class ComputerAddressGroupByConfiguration extends ComputerAddressGroup
{
 	private final String		m_settingPrefix;

	public ComputerAddressGroupByConfiguration(String settingPrefix, String groupName)
	{
		super(groupName);

		m_settingPrefix = settingPrefix;

		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		Set<String>				nameSet		= ConfigurationManager.getInstance().getSettingNameSet(m_settingPrefix);
		Set<ComputerAddress>	addressSet	= new HashSet<>();
		for (String name : nameSet)
		{
			// A name will be a full setting name. The list will include .address, .port and .laneCount.
			if (!name.endsWith(ComputerAddress.SETTING_NAME_ADDRESS))
			{
				continue;
			}

			String				prefix			= name.replace(ComputerAddress.SETTING_NAME_ADDRESS, "");
			ComputerAddress		address;
			try
			{
				address = new ComputerAddress(prefix, null);
				address.resolve();		// TODO Allow unknown server?
			}
			catch (BasicException exception)
			{
				// Make log entry
				getLogger().error(BasicEvent.EVENT_TODO, exception,
						"Failed to resolve server address from configuration using prefix %s", prefix);
				continue;
			}
			addressSet.add(address);
		}
		setAddressList(addressSet);
	}

	@Override
	public void close()
	{
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).deregister(this::updateConfiguration);

		super.close();
	}
}
