/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.jvm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.time.WallClock;

public class BasicMBeanProxy
{
	private static final String		MESSAGE_FORMAT_INCOMPATIBLE		= "Incompatible types: Expected %s, but got %s.";
	private static final String		MESSAGE_FORMAT_SERVER_OBJECT	= "%s/%s";

	private String						m_serverName;
	private String						m_port;
	private ConnectionInformation		m_connectionInformation;
	private ObjectName					m_objectName;
	private AttributeList				m_attributeValueList;

	public BasicMBeanProxy(String serverName, String port)
	{
		m_serverName = serverName;
		m_port = port;
	}

	public String getServerName()
	{
		return m_serverName;
	}

	public String getPort()
	{
		return m_port;
	}

	public boolean isConnected()
	{
		return null != getConnectionInformation();
	}

	public MBeanServerConnection getConnection()
	{
		return getConnectionInformation().getServerConnection();
	}

	private ConnectionInformation getConnectionInformation()
	{
		return m_connectionInformation;
	}

	public void selectObject(String mbeanName) throws BasicException
	{
		try
		{
			m_objectName = new ObjectName(mbeanName);
		}
		catch (MalformedObjectNameException e)
		{
			String message = String.format("Failed to select object: %s", mbeanName);
			throw new BasicException(BasicEvent.EVENT_MBEAN_SELECT_ERROR, e, message);
		}
	}

	/**
	 * Generic method to read a single attribute from a remote MBean.
	 * This method does a network call.
	 * This method is also used by all the getAttributeAs<Type> methods.
	 *
	 * @param attributeName		name of the attribute within the currently selected MBean
	 * @return					The JMX matching object
	 * @throws BasicException	Something can always go wrong on a network call.
	 */
	public Object getAttribute(String attributeName) throws BasicException
	{
		try
		{
			Object attributeValue		= getConnection().getAttribute(m_objectName, attributeName);

			return attributeValue;
		}
		catch (InstanceNotFoundException e)
		{
			String message		= "Attribute not found: "
					+ String.format(MESSAGE_FORMAT_SERVER_OBJECT, m_serverName, m_objectName.toString());
			throw new BasicException(BasicEvent.EVENT_MBEAN_COUNTER_NOT_FOUND, e, message);
		}
		catch (Exception exception)
		{
			releaseConnection();
			String message		= "Failed to obtain the attribute for counter "
					+ String.format(MESSAGE_FORMAT_SERVER_OBJECT, m_serverName, m_objectName.toString());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, exception, message);
		}
	}

	/**
	 * Generic method to read multiple attributes from a remote MBean using a single network call.
	 * This method does a network call.
	 * This method is also used in combination with the getAttributeFromListAs<Type> methods
	 * when several attributes are required and a single network call is desired (better performance, less overhead).
	 *
	 * @param attributeNameList		name of the attributes within the currently selected MBean
	 * @throws BasicException		Something can always go wrong on a network call.
	 */
	public void getAttributeList(String[] attributeNameList) throws BasicException
	{
		try
		{
			m_attributeValueList	= getConnection().getAttributes(m_objectName, attributeNameList);
		}
		catch (Exception exception)
		{
			releaseConnection();
			String message		= "Failed to obtain attribute values for counter "
					+ String.format(MESSAGE_FORMAT_SERVER_OBJECT, m_serverName, m_objectName.toString());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, exception, message);
		}
	}

	public Object doOperation(String operationName, Object[] params, String[] signature) throws BasicException
	{
		try
		{
			return getConnection().invoke(m_objectName, operationName, params, signature);
		}
		catch (Exception exception)
		{
			String message		= "Exception using object: "
					+ String.format(MESSAGE_FORMAT_SERVER_OBJECT, m_serverName, m_objectName.toString());
			throw new BasicException(BasicEvent.EVENT_MBEAN_OPERATION_ERROR, exception, message);
		}
	}

	/**
	 *
	 * This call is very expensive, the value is cached and re-queried only every so many seconds.
	 * The caller is responsible for having an understanding of the maximum acceptable age.
	 * For example, the name of the garbage related counters do not change, so ageMaxMS can be high.
	 *
	 * @param ageMaxMS	Maximum age of the JMX object names that is acceptable to the caller
	 * 					e.g. GC object names do not change, so a large value is acceptable.
	 * @return			The complete set of object names.
	 * @throws BasicException        If the set cannot be obtained (e.g. the remote server is offline)
	 */
	public Set<ObjectName> getObjectNameSet(long ageMaxMS, String name) throws BasicException
	{
		ConnectionInformation	connectionInformation	= getConnectionInformation();
		try
		{
			long		ageMS		= WallClock.getCurrentTimeMS() - connectionInformation.getObjectNameSetTime();
			if (null == connectionInformation.getObjectNameSet() || ageMaxMS < ageMS)
			{
				ObjectName objectName		= new ObjectName(name);
				Set<ObjectName> objectNameSet	= getConnection().queryNames(objectName, null);
				connectionInformation.setObjectNameSet(objectNameSet);
			}
		}
		catch (Exception exception)
		{
			releaseConnection();
			String message		= "Failed to obtain a Set<ObjectName>";
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, exception, message);
		}

		return connectionInformation.getObjectNameSet();
	}

	public String getAttributeAsString(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttribute(attributeName);

		return attributeObject.toString();
	}

	public Integer getAttributeAsInteger(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttribute(attributeName);

		if (attributeObject instanceof Integer)
		{
			return (Integer) attributeObject;
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Integer.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}

	public Long getAttributeAsLong(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttribute(attributeName);

		if (attributeObject instanceof Long)
		{
			return (Long) attributeObject;
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Long.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}

	public double getAttributeAsDouble(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttribute(attributeName);

		if (attributeObject instanceof Number)
		{
			return ((Number) attributeObject).doubleValue();
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Double.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}

	public Map<String, Number> getAttributeMapOfNumber()
	{
		Map<String, Number>		map		= new HashMap<>();
		for (Object o : m_attributeValueList)
		{
			Attribute attribute	= (Attribute) o;
			if (attribute.getValue() instanceof Number)
			{
				map.put(attribute.getName(), (Number) attribute.getValue());
			}
		}
		return map;
	}

	public Object getAttributeFromList(String attributeName)
	{
		for (Object o : m_attributeValueList)
		{
			Attribute attribute	= (Attribute) o;
			if (attribute.getName().equals(attributeName))
			{
				return attribute.getValue();
			}
		}
		return null;
	}

	public String getAttributeFromListAsString(String attributeName)
	{
		Object object		= getAttributeFromList(attributeName);
		return null == object ? null : object.toString();
	}

	public long getAttributeFromListAsInteger(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttributeFromList(attributeName);

		if (attributeObject instanceof Number)
		{
			return ((Number) attributeObject).intValue();
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Integer.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}
	public long getAttributeFromListAsLong(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttributeFromList(attributeName);

		if (attributeObject instanceof Number)
		{
			return ((Number) attributeObject).longValue();
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Long.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}
	public double getAttributeFromListAsDouble(String attributeName) throws BasicException
	{
		Object attributeObject		= getAttributeFromList(attributeName);

		if (attributeObject instanceof Number)
		{
			return ((Number) attributeObject).doubleValue();
		}
		else
		{
			String message		= String.format(MESSAGE_FORMAT_INCOMPATIBLE,
					Double.class.getName(), attributeObject.getClass().getName());
			throw new BasicException(BasicEvent.EVENT_MBEAN_GET_OBJECT_ERROR, message);
		}
	}

	public void connect() throws BasicException
	{
		if (null == m_connectionInformation)
		{
			try
			{
				String connectionString	= String.format(
						"service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi",
						m_serverName, m_port);
				JMXServiceURL url					= new JMXServiceURL(connectionString);
				JMXConnector connector			= JMXConnectorFactory.connect(url);
				MBeanServerConnection serverConnection	= connector.getMBeanServerConnection();
				m_connectionInformation	= new ConnectionInformation(connector, serverConnection);
			}
			catch (Exception exception)
			{
				String message		= "Failed to obtain the MBean for " + m_serverName;
				throw new BasicException(BasicEvent.EVENT_MBEAN_CONNECT_ERROR, exception, message);
			}
		}
	}

	public void releaseConnection()
	{
		BasicMBeanManager.getLogger().info(BasicEvent.EVENT_MBEAN_DISCONNECT,
				"Disconnecting (JMX) from %s:%s", m_serverName, m_port);
		BaseFileHandler.closeSafe(m_connectionInformation.getConnector());
		m_connectionInformation = null;
	}

	@Override
	public String toString()
	{
		return String.format("%s:%s (isConnected = %s)", getServerName(), getPort(), isConnected());
	}

	private static class ConnectionInformation
	{
		private JMXConnector m_connector;
		private MBeanServerConnection m_serverConnection;
		private Set<ObjectName> m_objectNameSet			= null;
		private long						m_objectNameSetTime		= 0;

		public ConnectionInformation(JMXConnector connector, MBeanServerConnection serverConnection)
		{
			m_connector = connector;
			m_serverConnection = serverConnection;
		}

		public JMXConnector getConnector()
		{
			return m_connector;
		}

		public MBeanServerConnection getServerConnection()
		{
			return m_serverConnection;
		}

		public Set<ObjectName> getObjectNameSet()
		{
			return m_objectNameSet;
		}

		public void setObjectNameSet(Set<ObjectName> objectNameSet)
		{
			m_objectNameSet = objectNameSet;
			m_objectNameSetTime = WallClock.getCurrentTimeMS();
		}

		public long getObjectNameSetTime()
		{
			return m_objectNameSetTime;
		}
	}
}
