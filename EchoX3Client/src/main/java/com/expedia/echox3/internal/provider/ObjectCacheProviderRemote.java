/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.provider;


import java.util.HashMap;
import java.util.Map;

import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.internal.transport.dispatch.user.DispatcherUserSourceMessageHandler;
import com.expedia.echox3.internal.transport.protocol.SourceProtocolHandler;
import com.expedia.echox3.internal.transport.socket.ComputerAddressGroup;
import com.expedia.echox3.internal.transport.socket.ComputerAddressGroupByConfiguration;
import com.expedia.echox3.internal.transport.socket.ComputerAddressLoadBalancer;
import com.expedia.echox3.internal.transport.socket.SourceTransportHighway;
import com.expedia.echox3.visible.trellis.ClientFactory;

public class ObjectCacheProviderRemote implements IObjectCacheProvider
{
	private static final BasicLogger	LOGGER				= new BasicLogger(ObjectCacheProviderRemote.class);

	//CHECKSTYLE:OFF
	private static final String			SETTING_PREFIX							= ObjectCacheProviderRemote.class.getName();
	private static final String			SETTING_BOOTSTRAP_DISPATCHER_PREFIX		= SETTING_PREFIX + ".bootstrap.dispatcher";
	//CHECKSTYLE:ON

	private static final String			PERMANENT_ADDRESS_GROUP_NAME			= "PermanentAddressGroupName";

	private ComputerAddressGroup						m_bootstrapDispatcherGroup		= null;

	private final DispatcherUserSourceMessageHandler	m_clientMessageHandler;

	// m_lock protects m_loadBalancerMap.
	private final AbstractReadWriteLock					m_loadBalancerLock;
	private Map<String, ComputerAddressLoadBalancer>	m_loadBalancerMap				= new HashMap<>();

	public ObjectCacheProviderRemote()
	{
		// NOTE: Need to use MagicReadWriteLock Without counters (as opposed to with disabled counters)
		// See output of profiler for justification: Limiting factor is the counter in this lock.
		m_loadBalancerLock = AbstractReadWriteLock.createReadWriteLock();
		m_clientMessageHandler = ClientFactory.getUserMessageHandler();
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	/* package */ SourceProtocolHandler getProtocolHandler()
	{
		return m_clientMessageHandler.getProtocolHandler();
	}

	public static String getPermanentAddressGroupName()
	{
		return PERMANENT_ADDRESS_GROUP_NAME;
	}

	public void connectToPermanent()
	{
		if (null != m_bootstrapDispatcherGroup)
		{
			// already connected
			return;
		}

		m_bootstrapDispatcherGroup = new ComputerAddressGroupByConfiguration(
				SETTING_BOOTSTRAP_DISPATCHER_PREFIX, "BootstrapDispatcher");
		getProtocolHandler().addAddressGroup(m_bootstrapDispatcherGroup);
		addAddressGroupLoadBalancer(PERMANENT_ADDRESS_GROUP_NAME, m_bootstrapDispatcherGroup);
	}
	public void closePermanent()
	{
		if (null != m_bootstrapDispatcherGroup)
		{
			getProtocolHandler().removeAddressGroup(m_bootstrapDispatcherGroup);
			m_bootstrapDispatcherGroup = null;
			removeAddressGroupLoadBalancer(PERMANENT_ADDRESS_GROUP_NAME);
		}
	}

	protected AbstractReadWriteLock getLoadBalancerLock()
	{
		return m_loadBalancerLock;
	}
	public void addAddressGroupLoadBalancer(String cacheName, ComputerAddressGroup addressGroup)
	{
		// Not optimum from lock point of view, but optimum from code simplicity...
		// this is not called very often (e.g. only at startup, not for each request).
		IOperationContext					context				= getLoadBalancerLock().lockWrite();
		ComputerAddressLoadBalancer			loadBalancer		= m_loadBalancerMap.get(cacheName);
		if (null == loadBalancer)
		{
			loadBalancer = new ComputerAddressLoadBalancer(addressGroup);
			m_loadBalancerMap.put(cacheName, loadBalancer);
		}

		getLoadBalancerLock().unlockWrite(context, true);
	}
	public ComputerAddressLoadBalancer removeAddressGroupLoadBalancer(String cacheName)
	{
		IOperationContext					context				= getLoadBalancerLock().lockWrite();
		ComputerAddressLoadBalancer			loadBalancer		= m_loadBalancerMap.remove(cacheName);
		getLoadBalancerLock().unlockWrite(context, true);

		return loadBalancer;
	}
	/* package */ ComputerAddressLoadBalancer getAddressGroupLoadBalancer(String cacheName)
	{
		IOperationContext				context			= getLoadBalancerLock().lockRead();
		ComputerAddressLoadBalancer		loadBalancer	= m_loadBalancerMap.get(cacheName);
		getLoadBalancerLock().unlockRead(context, true);
		return loadBalancer;
	}

	public SourceTransportHighway getNextTransportHighway(String cacheName) throws BasicException
	{
		ComputerAddressLoadBalancer		loadBalancer		= getAddressGroupLoadBalancer(cacheName);
		return (SourceTransportHighway) loadBalancer.getNextTransportHighway();
	}

	public DispatcherUserSourceMessageHandler getClientMessageHandler()
	{
		return m_clientMessageHandler;
	}

	// Methods from IHiperAdminClient
	@Override
	public void connectToCache(String cacheName) throws BasicException
	{
		// NYI
	}

	@Override
	public void close(String cacheName)
	{
		// NYI
	}

	@Override
	public void pushClassDefinition(String cacheName, Class clazz)
	{
		// Nothing to do in local mode.
	}

	@Override
	public void upgradeClass(String cacheName, String classFrom, String factoryClassName) throws BasicException
	{
		// NYI
	}

	@Override
	public Map<String, ManifestWrapper> getVersion() throws BasicException
	{
		return ManifestWrapper.getManifestMap();
	}


	// Methods common to both IHiper*Client
	@Override
	public void flush(String cacheName, int durationMS) throws BasicException
	{
		// NYI
	}

	@Override
	public void writeOnly(String cacheName, byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		return;
	}

	@Override
	public void writeOnly(String cacheName, byte[][] keyBytesList, byte[] requestBytes) throws BasicException
	{
		return;
	}

	@Override
	public void writeOnly(String cacheName, byte[][] keyBytesList, byte[][] requestBytesList) throws BasicException
	{
		return;
	}

	@Override
	public byte[] readOnly(String cacheName, byte[] keyBytes, byte[] requestBytes) throws BasicException
	{
		return null;
	}

	@Override
	public byte[][] readOnly(String cacheName, byte[][] keyBytesList, byte[] requestBytes) throws BasicException
	{
		return null;
	}

	@Override
	public byte[][] readOnly(String cacheName, byte[][] keyBytesList, byte[][] requestBytesList) throws BasicException
	{
		return null;
	}

	@Override
	public byte[] reduce(String cacheName, byte[][] keyList, byte[] request)
	{
		return new byte[0];
	}
}
