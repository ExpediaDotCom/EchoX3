/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.visible.ioc;

import java.net.MalformedURLException;
import java.net.URL;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.visible.trellis.IAdminClient;
import com.expedia.echox3.visible.trellis.IClientFactory;
import com.expedia.echox3.visible.trellis.IClientFactory.ClientType;
import com.expedia.echox3.visible.trellis.ClientFactory;

/**
 * This simple wrapper is provider to simplify the use of EchoX3 by IOC applications.
 * It encapsulates some parameters in constructor or setter parameters,
 * simplifying the interface for IOC applications.
 *
 * The components needed to have a fully configured object are:
 * 		1.	Cache name			always
 * 		2.	Client type			always
 * 		3.	Configuration URL	Only for local mode, does not hurt in remote mode.
 *
 * 	Until a value is given to each of the required components, no connection is available and calls will fail.
 * 	Once these three components are known, the object will establish the proper connections
 * 	to be available with the appropriate configuration.
 * 	The three (3) components are hot. Changing any of them results in an immediate adjustment.
 * 	To access multiple namde cache or other configuration, create multiple objects.
 */
public abstract class IocCacheClient
{
	private static final IClientFactory TRELLIS_FACTORY		= ClientFactory.getInstance();

	private ClientType						m_clientType		= null;
	private String							m_cacheName			= null;
	private URL								m_url				= null;

	private IAdminClient m_adminClient		= null;
	private boolean							m_isDirty			= true;

	/**
	 * Parameterless constructor expected to be used with the setter methods
	 */
	public IocCacheClient()
	{
	}

	/**
	 * Parametized constructor, may be used to create an immediately configured object
	 *
	 * @param clientType		Local or Remote
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 * @param url				A valid URL, see IClientFactory
	 * @throws BasicException	Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	public IocCacheClient(ClientType clientType, String cacheName, URL url) throws BasicException
	{
		m_isDirty = true;
		updateInternal(clientType, cacheName, url);
	}

	protected static IClientFactory getFactory()
	{
		return TRELLIS_FACTORY;
	}

	/**
	 * Setter, boring, note that this is hot: the underlying client is updated immediately.
	 *
	 * @param clientType		Local or Remote
	 * @throws BasicException	Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	public void setClientType(ClientType clientType) throws BasicException
	{
		m_isDirty		|= (!clientType.equals(m_clientType));

		updateInternal(clientType, m_cacheName, m_url);
	}

	/**
	 * Setter, boring, note that this is hot: the underlying client is updated immediately.
	 *
	 * @param cacheName			Standard name of the cache (Object or Simple) of interest
	 * @throws BasicException	Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	public void setCacheName(String cacheName) throws BasicException
	{
		m_isDirty		|= (!cacheName.equals(m_cacheName));

		updateInternal(m_clientType, cacheName, m_url);
	}

	/**
	 * Setter, boring, note that this is hot: the underlying client is updated immediately.
	 *
	 * @param urlText		URL to the ObjectCacheConfiguration configuration file
	 * @throws BasicException	Something can always go wrong, look at exception.getBasicEvent() for details
	 */
	public void setConfigurationUrl(String urlText) throws BasicException
	{
		URL			url		= null;
		try
		{
			url = new URL(urlText);
		}
		catch (MalformedURLException e)
		{
			throw new BasicException(BasicEvent.EVENT_CONFIGURATION_URL_INVALID, e,
					"The url provided for the cache configuration file (%s) is not a valid url", urlText);
		}
		m_isDirty			|= (!url.equals(m_url));

		updateInternal(m_clientType, m_cacheName, url);
	}

	protected boolean isDirty()
	{
		return m_isDirty;
	}

	/**
	 * This method need to close() an existing cache BEFORE setting the new values.
	 *
	 * @param clientType			ClientType.Local or ClientType.Remote
	 * @param cacheName				Name of the object cache, Simple or Object
	 * @param url					Pointer to the configuration file for local mode
	 * @throws BasicException		Something can always go wrong
	 */
	protected void updateInternal(ClientType clientType, String cacheName, URL url) throws BasicException
	{
		if (!m_isDirty)
		{
			// Nothing has changed
			return;
		}

		synchronized (this)
		{
			// Note: Close with previous
			close();

			// Pick-up the new values...
			m_clientType = clientType;
			m_cacheName = cacheName;
			m_url = url;

			if (null == m_clientType || null == m_cacheName)
			{
				return;        // Not enough data to get started
			}
			if (null == url && ClientType.Local.equals(m_clientType))
			{
				return;        // Local mode and no URL yet, wait for it
			}

			// Have all necessary data, update the necessary objects...
			getFactory().putLocalConfiguration(m_cacheName, m_url);
			m_adminClient = getFactory().getAdminClient(m_clientType);
			m_adminClient.connectToCache(m_cacheName);

			// Let the extending class update itself...
			update();

			m_isDirty = false;
		}
	}
	protected abstract void update() throws BasicException;

	/**
	 * Typical, boring getter.
	 *
	 * @return		Local or Remote
	 */
	public ClientType getClientType()
	{
		return m_clientType;
	}

	/**
	 * Typical, boring getter.
	 *
	 * @return		The cache name, as per the last set()
	 */
	public String getCacheName()
	{
		return m_cacheName;
	}

	/**
	 * Typical, boring getter.
	 *
	 * @return		The configured URL, as per the last set()
	 */
	public URL getUrl()
	{
		return m_url;
	}

	protected void close()
	{
		if (null != m_adminClient && null != m_cacheName)
		{
			m_adminClient.close(m_cacheName);
		}
		m_cacheName = null;
	}
}
