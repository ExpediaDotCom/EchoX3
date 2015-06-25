/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.WallClock;

public class AbstractMeasureProxy
{
	private static final BasicLogger			LOGGER			= new BasicLogger(AbstractMeasureProxy.class);

	private boolean		m_isConnected;
	private long		m_lastMeasuredMS		= 0;
	private String		m_objectName;

	protected AbstractMeasureProxy(String domain, StringGroup nameList)
	{
		if (null != nameList)
		{
			m_objectName = BasicTools.createMBeanName(domain, nameList);
		}
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void setObjectName(String objectName)
	{
		m_objectName = objectName;
	}

	public boolean load(BasicMBeanProxy proxy, String[] attributeList)
	{
		if (!proxy.isConnected())
		{
			m_isConnected = false;
			return false;
		}

		try
		{
			proxy.selectObject(m_objectName);
			proxy.getAttributeList(attributeList);
			m_isConnected = true;
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_JMX_MEASURE_EXCEPTION, exception,
					"Failed to retrieve attribute list of object %s for %s from %s:%s",
					m_objectName,
					getClass().getSimpleName(),
					proxy.getServerName(), proxy.getPort());
			m_isConnected = false;
			return false;
		}

		m_lastMeasuredMS = WallClock.getCurrentTimeMS();
		return true;
	}

	protected String getSafeString(Object o)
	{
		if (null == o)
		{
			return null;
		}
		else
		{
			return o.toString();
		}
	}
	protected int getSafeInt(Object object)
	{
		return object instanceof Number ? ((Number) object).intValue() : 0;
	}
	protected long getSafeLong(Object object)
	{
		return object instanceof Number ? ((Number) object).longValue() : 0;
	}

	public boolean isConnected()
	{
		return m_isConnected;
	}

	public long getLastMeasuredMS()
	{
		return m_lastMeasuredMS;
	}
}
