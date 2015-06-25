/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.misc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.time.WallClock;

public class BasicTools
{
	public static final BasicLogger		LOGGER					= new BasicLogger(BasicTools.class);
	public static final String			MBEAN_DOMAIN			= "Trellis";
	public static final int				BASE_INDEX				= 47000;		// For JMX and EventIndex and ...

	private static final String[] 		READABLE_SIZE_PREFIXES	= new String[] {"", "K", "M", "G", "T", "P", "E"};

	private static final MBeanServer	MBEAN_SERVER			= ManagementFactory.getPlatformMBeanServer();

	private static final long			TIME_BIG_BANG;

	private static final Random			RANDOM					= new Random();
	private static final char[]			RANDOM_CHAR_LIST		=
								"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

	private static final MemoryMXBean	MEMORY_BEAN				= ManagementFactory.getMemoryMXBean();
	private static final long			HEAP_LOOK_AGE_MS_MAX	= 1000;
	private static long					s_heapLookTimeMS		= 0;
	private static long					s_heapLastPercent		= 0;

	// NOT final because of setComputerNameForTestingOnly()
	private static String		s_hostName;
	private static InetAddress	s_hostAddress;

	static
	{
		Calendar calendar		= Calendar.getInstance();
		calendar.setTimeInMillis(0);
		calendar.set(2014, Calendar.JANUARY, 1, 8, 59, 59);

		TIME_BIG_BANG = calendar.getTimeInMillis();

		String hostName;
		try
		{
			InetAddress		hostAddress		= java.net.InetAddress.getLocalHost();
			s_hostAddress	= hostAddress;
			hostName		= hostAddress.getHostName();
		}
		catch (UnknownHostException e)
		{
			hostName = System.getenv("COMPUTERNAME");
			if (null == hostName)
			{
				hostName = "Unknown_host";
			}
		}
		String[]		hostNameParts		= hostName.split("\\.");
		if (hostName.endsWith(".local"))
		{
			// This is a Mac style name (e.g. MachineName.local), keep intact
			s_hostName = hostName;
		}
		else
		{
			s_hostName = hostNameParts[0].toUpperCase(Locale.US);
		}
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static long getTimeBigBang()
	{
		return TIME_BIG_BANG;
	}

	public static String setComputerNameForTestingOnly(String computerName)
	{
		String nameSav		= s_hostName;
		s_hostName = computerName.toUpperCase(Locale.US);
		return nameSav;
	}
	public static String getComputerName()
	{
		return s_hostName;
	}

	public static InetAddress getHostAddress()
	{
		return s_hostAddress;
	}

	public static void registerMBean(Object object, String domain, StringGroup nameList)
	{
		String objectName		= createMBeanName(domain, nameList);
		try
		{
			ObjectName name		= new ObjectName(objectName);
			MBEAN_SERVER.registerMBean(object, name);
		}
		catch (Exception exception)
		{
			getLogger().info(BasicEvent.EVENT_TOOLS_MBEAN_NAME_ERROR, exception,
					"Failed to register the MBean ", nameList);
		}
	}
	public static void unregisterMBean(String domain, StringGroup nameList)
	{
		String objectName		= createMBeanName(domain, nameList);
		try
		{
			ObjectName name		= new ObjectName(objectName);
			MBEAN_SERVER.unregisterMBean(name);
		}
		catch (Exception exception)
		{
			getLogger().info(BasicEvent.EVENT_TOOLS_MBEAN_NAME_ERROR, exception,
					"Failed to unregister the MBean ", nameList);
		}
	}
	public static String createMBeanName(String domain, StringGroup nameList)
	{
		StringBuilder sb			= new StringBuilder(256);
		if (null == domain)
		{
			sb.append(MBEAN_DOMAIN);
		}
		else
		{
			sb.append(domain);
		}
		sb.append(':');

		int				index		= 0;
		for (String namePart : nameList.getStringArray())
		{
			if (0 != index)
			{
				sb.append(",");
			}
			sb.append(String.format("Name-%,d=%s", index++, namePart));
		}
		return sb.toString();
	}

	public static String generateRandomString(int length)
	{
		StringBuilder sb			= new StringBuilder();

		for (int i = 0; i < length; i++)
		{
			int		r		= RANDOM.nextInt(RANDOM_CHAR_LIST.length);
			sb.append(RANDOM_CHAR_LIST[r]);
		}

		return sb.toString();
	}

	public static String generateReproducibleRandomString(int length)
	{
		Random random		= new Random(TIME_BIG_BANG + length);
		StringBuilder sb			= new StringBuilder(Math.max(0, length));

		for (int i = 0; i < length; i++)
		{
			int		r		= random.nextInt(RANDOM_CHAR_LIST.length);
			sb.append(RANDOM_CHAR_LIST[r]);
		}

		return sb.toString();
	}

	/**
	 * Sleep as a wait within the thread, to be interruptable with a notify on the thread object.
	 *
	 * @param ms		Number of ms to wait; 0 = until notified.
	 */
	public static void wait(int ms)
	{
		Thread thread		= Thread.currentThread();

		// Proceed this wait to be easily interruptible.
		synchronized (thread)
		{
			try
			{
				thread.wait(ms);
			}
			catch (InterruptedException e)
			{
				// Ignore and exit.
			}
		}
	}

	/**
	 * Real Thread.sleep(), just without the Interrupted Exception.
	 *
	 * @param ms		Number of ms to wait; 0 = return immediately (possibly give up time slice).
	 */
	public static void sleepMS(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			// NOPMD Ignore and exit.
		}
	}

	public static int burnCpuMS(int ms)
	{
		return new Long(burnCpuUS(ms * 1000) / 1000).intValue();
	}
	public static long burnCpuUS(long us)
	{
		return burnCpuNS(us * 1000) / 1000;
	}
	public static long burnCpuNS(long requestedNS)
	{
		long	startNS			= System.nanoTime();
		long	endNS			= startNS + requestedNS;

		while (System.nanoTime() < endNS)
		{	// NOPMD
			// Keep burning CPU
		}

		return System.nanoTime() - startNS;
	}


	public static int getNumberOfProcessors()
	{
		return Runtime.getRuntime().availableProcessors();
	}
	public static long getHeapSize()
	{
		MemoryUsage u = MEMORY_BEAN.getHeapMemoryUsage();

		return u.getUsed();
	}

	public static String formatByteCount(long value)
	{
		for (int i = 6; i > 0; i--)
		{
			double step = Math.pow(1024, i);
			if (value > step)
			{
				return String.format("%.1f %sB", value / step, READABLE_SIZE_PREFIXES[i]);
			}
		}
		return Long.toString(value);
	}

	public static long getHeapSizeMax()
	{
		MemoryUsage u = MEMORY_BEAN.getHeapMemoryUsage();

		return u.getMax();
	}

	public static int getHeapPercent(boolean isLazy)
	{
		long		now		= WallClock.getCurrentTimeMS();
		long		age		= now - s_heapLookTimeMS;

		if (age > HEAP_LOOK_AGE_MS_MAX || !isLazy)
		{
			MemoryUsage		u	= MEMORY_BEAN.getHeapMemoryUsage();

			s_heapLookTimeMS	= now;
			s_heapLastPercent	= (u.getUsed() * 100) / u.getMax();
		}

		return (int) s_heapLastPercent;
	}
}
