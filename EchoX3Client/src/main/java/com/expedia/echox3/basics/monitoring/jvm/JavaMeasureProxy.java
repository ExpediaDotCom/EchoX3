/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;

@SuppressWarnings("PMD.TooManyFields")
public class JavaMeasureProxy extends AbstractMeasureProxy
{
	// Constants used in the toString() method
	private static final String S_S								= "%s=%s ";
	private static final String S_D								= "%s=%,d ";
	private static final String S_F								= "%s=%,.1f ";

	private static final String MBEAN_NAME_JAVA_RUNTIME			= "java.lang:type=Runtime";
	private static final String MBEAN_NAME_OPERATING_SYSTEM		= "java.lang:type=OperatingSystem";
	private static final String MBEAN_NAME_MEMORY				= "java.lang:type=Memory";

	// WARNING: DO not change the order, others depend on it.
	private static final String[]		RUNTIME_ATTRIBUTE_NAME_LIST		=
			{
					"VmName",
					"VmVendor",
					"VmVersion",
					"SpecVersion",
					"SystemProperties",
					"StartTime",
					"Uptime"
			};
	// WARNING: DO not change the order, others depend on it.
	private static final String[]		OS_ATTRIBUTE_NAME_LIST		=
			{
					"Name",
					"TotalPhysicalMemorySize",
					"Version",
					"AvailableProcessors",
					"ProcessCpuTime"
			};
	// WARNING: DO not change the order, others depend on it.
	private static final String[]		MEMORY_ATTRIBUTE_NAME_LIST		=
			{
					"HeapMemoryUsage",
					"NonHeapMemoryUsage"
			};

	private String m_javaName;
	private String m_javaVendor;
	private String m_javaVersion;
	private String m_specVersion;
	private String m_runtimeVersion;
	private long		m_startTimeMS;
	private long		m_upTimeMS				= 1;		// Ensure it is never 0, se can divide by it

	private String m_osName;
	private String m_osVersion;
	private long		m_memoryBytes;
	private int			m_processorCount;

	// Since start of JVM
	private long		m_cpuTotalNS;
	private double		m_cpuTotalPercent;

	//  Over the last measurement period

	private long		m_lastCpuMeasuredMS		= 0;
	private long		m_durationMS;
	private long		m_cpuCurrentNS			= 0;
	private double		m_cpuCurrentPercent		= 0;


	private long		m_heapMemoryUsed;
	private long		m_heapMemoryMax;
	private long		m_nonMemoryUsed;
	private long		m_nonMemoryMax;


	public JavaMeasureProxy()
	{
		super(null, null);
	}

	public boolean measure(String serverName)
	{
		boolean		isSuccess;
		isSuccess		 = measureRuntime(serverName);
		isSuccess		&= measureOperatingSystem(serverName);
		isSuccess		&= measureMemory(serverName);
		return isSuccess;
	}

	private boolean measureRuntime(String serverName)
	{
		BasicMBeanProxy proxy = BasicMBeanManager.getMbeanProxy(serverName);

		// Don't let someone else change the content of the list of attributes
		// It's OK to synchronize on proxy because it's the same value for all callers, as BasicMBeanManager
		// keeps a Map
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (proxy)
		{
			setObjectName(MBEAN_NAME_JAVA_RUNTIME);
			if (!load(proxy, RUNTIME_ATTRIBUTE_NAME_LIST))
			{
				return false;
			}

			try
			{
				Object vmName		= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[0]);
				Object vmVendor	= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[1]);
				Object vmVersion	= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[2]);
				Object specVersion	= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[3]);
				Object properties	= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[4]);
				Object startTime	= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[5]);
				Object upTime		= proxy.getAttributeFromList(RUNTIME_ATTRIBUTE_NAME_LIST[6]);

				m_javaName			= getSafeString(vmName);
				m_javaVendor		= getSafeString(vmVendor);
				m_javaVersion		= getSafeString(vmVersion);
				m_specVersion		= getSafeString(specVersion);
				m_runtimeVersion	= getCompositeProperty(properties, "java.runtime.version");
				m_startTimeMS		= getSafeLong(startTime);
				m_upTimeMS			= Math.max(1, getSafeLong(upTime));		// != 0, for division
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_JMX_JAVA_PARSING_EXCEPTION, exception,
						"Failed to parse data for %s from %s:%s",
						getClass().getSimpleName(), proxy.getServerName(), proxy.getPort());
				return false;
			}
		}
		return true;
	}
	@SuppressWarnings("rawtypes")
	private String getCompositeProperty(Object properties, String propertyName)
	{
		String value			= null;
		if (properties instanceof CompositeData)
		{
			CompositeData compositeData		= (CompositeData) properties;
			value = compositeData.get(propertyName).toString();
		}
		else if (properties instanceof TabularData)
		{
//			TabularData tabularData = (TabularData) properties;
//			TabularType type = tabularData.getTabularType();
			for (Object object : ((Map) properties).entrySet())
			{
				Map.Entry			entry		= (Map.Entry) object;
				CompositeData		data		= (CompositeData) entry.getValue();
				String				key			= data.get("key").toString();
				if (propertyName.equals(key))
				{
					value = data.get("value").toString();
					break;
				}
			}
		}

		return value;
	}
	private boolean measureOperatingSystem(String serverName)
	{
		BasicMBeanProxy proxy = BasicMBeanManager.getMbeanProxy(serverName);

		// Don't let someone else change the content of the list of attributes
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (proxy)
		{
			setObjectName(MBEAN_NAME_OPERATING_SYSTEM);
			if (!load(proxy, OS_ATTRIBUTE_NAME_LIST))
			{
				return false;
			}
			if (0 != m_lastCpuMeasuredMS)
			{
				m_durationMS	= (getLastMeasuredMS() - m_lastCpuMeasuredMS);
			}
			m_lastCpuMeasuredMS = getLastMeasuredMS();

			try
			{
				Object osName		= proxy.getAttributeFromList(OS_ATTRIBUTE_NAME_LIST[0]);
				Object memory		= proxy.getAttributeFromList(OS_ATTRIBUTE_NAME_LIST[1]);
				Object osVersion	= proxy.getAttributeFromList(OS_ATTRIBUTE_NAME_LIST[2]);
				Object procCount	= proxy.getAttributeFromList(OS_ATTRIBUTE_NAME_LIST[3]);
				Object cpuTime	= proxy.getAttributeFromList(OS_ATTRIBUTE_NAME_LIST[4]);

				m_osName			= getSafeString(osName);
				m_memoryBytes		= getSafeLong(memory);
				m_osVersion			= getSafeString(osVersion);
				m_processorCount	= Math.max(1, getSafeInt(procCount));

				long		cpuTimeNS		= getSafeLong(cpuTime);
				if (0 != (m_durationMS * m_processorCount))
				{
					m_cpuCurrentNS = cpuTimeNS - m_cpuTotalNS;
					m_cpuCurrentPercent = m_cpuCurrentNS * 100.0 / (m_durationMS * 1000L * 1000 * m_processorCount);
				}
				if (0 != m_upTimeMS)
				{
					m_cpuTotalNS = cpuTimeNS;
					m_cpuTotalPercent = m_cpuTotalNS * 100.0 / (m_upTimeMS * 1000L * 1000 * m_processorCount);
				}
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_JMX_JAVA_PARSING_EXCEPTION, exception,
						"Failed to parse data for %s from %s:%s",
						getClass().getSimpleName(), proxy.getServerName(), proxy.getPort());
				return false;
			}
		}
		return true;
	}
	private boolean measureMemory(String serverName)
	{
		BasicMBeanProxy proxy = BasicMBeanManager.getMbeanProxy(serverName);

		// Don't let someone else change the content of the list of attributes
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (proxy)
		{
			setObjectName(MBEAN_NAME_MEMORY);
			if (!load(proxy, MEMORY_ATTRIBUTE_NAME_LIST))
			{
				return false;
			}

			try
			{
				Object heapMemory		= proxy.getAttributeFromList(MEMORY_ATTRIBUTE_NAME_LIST[0]);
				Object nonMemory		= proxy.getAttributeFromList(MEMORY_ATTRIBUTE_NAME_LIST[1]);

				m_heapMemoryUsed	= Long.parseLong(getCompositeProperty(heapMemory, "used"));
				m_heapMemoryMax		= Long.parseLong(getCompositeProperty(heapMemory, "max"));
				m_nonMemoryUsed		= Long.parseLong(getCompositeProperty(nonMemory, "used"));
				m_nonMemoryMax		= Long.parseLong(getCompositeProperty(nonMemory, "max"));
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_JMX_JAVA_PARSING_EXCEPTION, exception,
						"Failed to parse data for %s from %s:%s",
						getClass().getSimpleName(), proxy.getServerName(), proxy.getPort());
				return false;
			}
		}
		return true;
	}

	public String getJavaName()
	{
		return m_javaName;
	}

	public String getJavaVendor()
	{
		return m_javaVendor;
	}

	public String getJavaVersion()
	{
		return m_javaVersion;
	}

	public String getSpecVersion()
	{
		return m_specVersion;
	}

	public String getRuntimeVersion()
	{
		return m_runtimeVersion;
	}

	public long getStartTimeMS()
	{
		return m_startTimeMS;
	}

	public long getUpTimeMS()
	{
		return m_upTimeMS;
	}

	public long getHeapMemoryUsed()
	{
		return m_heapMemoryUsed;
	}

	public long getHeapMemoryMax()
	{
		return m_heapMemoryMax;
	}

	public long getNonMemoryUsed()
	{
		return m_nonMemoryUsed;
	}

	public long getNonMemoryMax()
	{
		return m_nonMemoryMax;
	}

	public String getOsName()
	{
		return m_osName;
	}

	public String getOsVersion()
	{
		return m_osVersion;
	}

	public long getMemoryBytes()
	{
		return m_memoryBytes;
	}

	public int getProcessorCount()
	{
		return m_processorCount;
	}

	public long getCpuTotalNS()
	{
		return m_cpuTotalNS;
	}

	public double getCpuTotalPercent()
	{
		return m_cpuTotalPercent;
	}
	public double getTotalBusyProcessorCount()
	{
		return m_cpuTotalPercent * m_processorCount / 100;
	}

	public long getDurationMS()
	{
		return m_durationMS;
	}

	public long getCpuCurrentNS()
	{
		return m_cpuCurrentNS;
	}

	public double getCpuCurrentPercent()
	{
		return m_cpuCurrentPercent;
	}
	public double getCurrentBusyProcessorCount()
	{
		return m_cpuCurrentPercent * m_processorCount / 100;
	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder		= new StringBuilder(24 * 25); // 24 fields, including class name
		stringBuilder.append(getClass().getSimpleName()).append(": ");
		stringBuilder.append(String.format(S_S, "javaName", m_javaName));
		stringBuilder.append(String.format(S_S, "javaVendor", m_javaVendor));
		stringBuilder.append(String.format(S_S, "javaVersion", m_javaVersion));
		stringBuilder.append(String.format(S_S, "specVersion", m_specVersion));
		stringBuilder.append(String.format(S_S, "runtimeVersion", m_runtimeVersion));
		stringBuilder.append(String.format(S_D, "startTimeMS", m_startTimeMS));
		stringBuilder.append(String.format(S_D, "upTimeMS", m_upTimeMS));
		stringBuilder.append(String.format(S_S, "osName", m_osName));
		stringBuilder.append(String.format(S_S, "osVersion", m_osVersion));
		stringBuilder.append(String.format(S_D, "memoryBytes", m_memoryBytes));
		stringBuilder.append(String.format(S_D, "processorCount", m_processorCount));
		stringBuilder.append(String.format(S_D, "cpuTotalNS", m_cpuTotalNS));
		stringBuilder.append(String.format(S_F, "cpuTotalPercent", m_cpuTotalPercent));
		stringBuilder.append(String.format(S_D, "lastCpuMeasuredMS", m_lastCpuMeasuredMS));
		stringBuilder.append(String.format(S_D, "durationMS", m_durationMS));
		stringBuilder.append(String.format(S_D, "cpuCurrentNS", m_cpuCurrentNS));
		stringBuilder.append(String.format(S_F, "cpuCurrentPercent", m_cpuCurrentPercent));
		stringBuilder.append(String.format(S_D, "heapMemoryUsed", m_heapMemoryUsed));
		stringBuilder.append(String.format(S_D, "heapMemoryMax", m_heapMemoryMax));
		stringBuilder.append(String.format(S_D, "nonMemoryUsed", m_nonMemoryUsed));
		stringBuilder.append(String.format(S_D, "nonMemoryMax", m_nonMemoryMax));
		return stringBuilder.toString();
	}

}
