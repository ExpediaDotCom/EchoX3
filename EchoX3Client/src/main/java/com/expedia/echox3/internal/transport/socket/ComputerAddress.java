/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.misc.LoadBalancer.ILoadBalanced;

public class ComputerAddress implements Serializable, Comparable<ComputerAddress>, ILoadBalanced
{
	public static final long					serialVersionUID	= 20150601085959L;

	public static final String		SETTING_NAME_ADDRESS					= ".address";
	public static final String		SETTING_NAME_PORT						= ".port";
	public static final String		SETTING_NAME_LANE_COUNT					= ".laneCount";
	protected static final ConfigurationManager CONFIGURATION_MANAGER		= ConfigurationManager.getInstance();

	private final String				m_hostName;
	private InetAddress					m_inetAddress;
	private int							m_port					= BasicTools.BASE_INDEX;
	private InetSocketAddress			m_inetSocketAddress		= null;
	private int							m_laneCount				= 1;
	private transient TransportHighway	m_sourceHighway			= null;
	private transient long				m_serverHighway			= 0;

	public ComputerAddress(String settingPrefix)
	{
		this(settingPrefix, BasicTools.getComputerName());
	}
	public ComputerAddress(String settingPrefix, String defaultHost)
	{
		//CHECKSTYLE:OFF
		String		addressText		= CONFIGURATION_MANAGER.getSetting(settingPrefix + SETTING_NAME_ADDRESS, defaultHost);
		int			port			= CONFIGURATION_MANAGER.getInt(settingPrefix + SETTING_NAME_PORT, Integer.toString(m_port));
		int			laneCount		= CONFIGURATION_MANAGER.getInt(settingPrefix + SETTING_NAME_LANE_COUNT, Integer.toString(m_laneCount));
		//CHECKSTYLE:ON

		m_hostName = addressText;
		m_port = port;
		m_laneCount = laneCount;
	}

	public ComputerAddress(String addressText, int port, int laneCount)
	{
		m_hostName = addressText;
		m_port = port;
		m_laneCount = laneCount;

		try
		{
			resolve();
		}
		catch (BasicException e)
		{
			// May be will resolve later
		}
	}
	public final void resolve() throws BasicException
	{
		synchronized (m_hostName)
		{
			if (null == m_inetAddress)
			{
				try
				{
					m_inetAddress = InetAddress.getByName(m_hostName);
					m_inetSocketAddress = new InetSocketAddress(m_inetAddress, m_port);
				}
				catch (UnknownHostException e)
				{
					throw new BasicException(BasicEvent.EVENT_SOCKET_EXCEPTION_RESOLVE,
							"Failed to resolve address " + m_hostName, e);
				}
			}
		}
	}
	public boolean isLocal() throws BasicException
	{
		if (null == m_inetAddress)
		{
			resolve();
		}
		String		localName		= BasicTools.getComputerName().toUpperCase(Locale.US);
		String		inetName		= m_inetAddress.getHostName().toUpperCase(Locale.US);
		return localName.equals(inetName);
	}

	public String getHostName()
	{
		return m_inetAddress.getHostName();
	}

	public String getAddress()
	{
		return m_inetAddress.getHostAddress();
	}

	public int getPort()
	{
		return m_port;
	}

	public String getDisplayName()
	{
		return String.format("%S[%s]:%d", m_hostName, getAddress(), getPort());
	}

	public String getUniqueName()
	{
		return String.format("%s:%d", getAddress(), getPort());
	}

	public InetSocketAddress getInetSocketAddress()
	{
		return m_inetSocketAddress;
	}

	public int getLaneCount()
	{
		return m_laneCount;
	}

	public TransportHighway getSourceHighway()
	{
		return m_sourceHighway;
	}

	public void setSourceHighway(TransportHighway sourceHighway)
	{
		m_sourceHighway = sourceHighway;
	}

	public void setServerHighway(long serverHighway)
	{
		m_serverHighway = serverHighway;
	}

	public long getServerHighway()
	{
		return m_serverHighway;
	}

	public void close(String reason)
	{
		getSourceHighway().close(reason);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ComputerAddress))
		{
			return false;
		}
		ComputerAddress		computerAddress		= (ComputerAddress) obj;
		return getUniqueName().equals(computerAddress.getUniqueName());
	}

	@Override
	public int hashCode()
	{
		return getUniqueName().hashCode();
	}

	@Override
	public int compareTo(ComputerAddress address)
	{
		return getUniqueName().compareTo(address.getUniqueName());
	}

	@Override
	public boolean isActive()
	{
		TransportHighway		transportHighway	= getSourceHighway();

		return null != transportHighway && transportHighway.isActive();
	}

	@Override
	public int getLoad()
	{
		SourceTransportHighway		transportHighway	= (SourceTransportHighway) getSourceHighway();
		return null == transportHighway ? 999 : transportHighway.getLoad();
	}

	public static ComputerAddress fromString(String text)
	{
		String[]		parts		= text.split(":");
		String			name		= parts[0];
		int				port		= Integer.parseInt(parts[1]);
		int				laneCount	= Integer.parseInt(parts[2]);

		return new ComputerAddress(name, port, laneCount);
	}

	@Override
	public String toString()
	{
		// IMPORTANT: Keep synchronized with fromString.
		// IMPORTANT: The 3 parts used in fromString MUST be the 3 original parts past in the constructor.

		return String.format("%1$s:%2$d:%3$d", m_hostName, m_port, m_laneCount);
	}
}
