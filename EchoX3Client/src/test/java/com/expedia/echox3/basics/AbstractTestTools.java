/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */
package com.expedia.echox3.basics;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.*;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.misc.LoadBalancer;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;

/**
 * Created by IntelliJ IDEA.
 * User: pcote
 * Date: 5/29/12
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractTestTools
{
	public static final Random			RANDOM			= new Random();
	private static final BasicLogger	LOGGER			= new BasicLogger(AbstractTestTools.class);

	static
	{
		// Force load of the configuration system.
		ConfigurationManager.getInstance();
		WallClock.getCurrentTimeMS();
//		CounterFactory.initCounterSystem(15, 300);
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	protected String logTestName()
	{
		// Find the name of the caller, to specify the source of the message.
		String		testName		= getCallerName(3);

		getLogger().info(BasicEvent.EVENT_TEST, "Running %s", testName);

		return testName;
	}
	private static String getCallerName(int n)
	{
		// Find the name of the caller, to specify the source of the message.
		StackTraceElement[]		stack		= Thread.currentThread().getStackTrace();
		StackTraceElement		frame		= stack[n];
		String					className	= frame.getClassName();
		try
		{
			Class					clazz		= Class.forName(className);
			className = clazz.getSimpleName();
		}
		catch (ClassNotFoundException e)
		{
			// Continue using the long class name
		}

		return String.format("%s.%s", className, frame.getMethodName());
	}

	public static String generateRandomString(int length)
	{
		char[]			rgch		= "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
//		char[]			rgch		= "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		StringBuilder sb			= new StringBuilder(length);

		for (int i = 0; i < length; i++)
		{
			int		r		= RANDOM.nextInt(rgch.length);
			sb.append(rgch[r]);
		}

		return sb.toString();
	}

	public static long reportPerformance(String message, long ns, long count, boolean showResult)
	{
		long		nsPer	= ns / (0 == count ? 1 : count);
		double		rate	= 1000. * 1000 * 1000 / nsPer;
		String text	= String.format("%14s / %,10d items = %14s / item == %,12.1f item / sec for %s",
				TimeUnits.formatNS(ns), count,
				TimeUnits.formatNS(nsPer), rate, message);

		if (showResult)
		{
			getLogger().info(BasicEvent.EVENT_TEST, text);
			BasicTools.wait(50);
		}

		return nsPer;
	}

	protected void ensureException(Object object, Method method, Class exceptionClass, Object... parameterList)
	{
		try
		{
			method.invoke(object, parameterList);
		}
		catch (Exception exception)
		{
			assertEquals(exceptionClass, exception.getCause().getClass());
		}
	}





	public static class TestLoadBalancedObject implements LoadBalancer.ILoadBalanced
	{
		public static final long					serialVersionUID	= 20150601085959L;

		private String		m_name;
		private boolean		m_isActive;
		private int			m_load;

		public TestLoadBalancedObject(String name, boolean isActive, int load)
		{
			m_name = name;
			m_isActive = isActive;
			m_load = load;
		}

		public void setActive(boolean isActive)
		{
			m_isActive = isActive;
		}

		@Override
		public boolean isActive()
		{
			return m_isActive;
		}

		public void setLoad(int load)
		{
			m_load = load;
		}

		@Override
		public int getLoad()
		{
			return m_load;
		}


		/**
		 * Returns a string representation of the object. In general, the
		 * {@code toString} method returns a string that
		 * "textually represents" this object. The result should
		 * be a concise but informative representation that is easy for a
		 * person to read.
		 * It is recommended that all subclasses override this method.
		 * <p>
		 * The {@code toString} method for class {@code Object}
		 * returns a string consisting of the name of the class of which the
		 * object is an instance, the at-sign character `{@code @}', and
		 * the unsigned hexadecimal representation of the hash code of the
		 * object. In other words, this method returns a string equal to the
		 * value of:
		 * <blockquote>
		 * <pre>
		 * getClass().getName() + '@' + Integer.toHexString(hashCode())
		 * </pre></blockquote>
		 *
		 * @return a string representation of the object.
		 */
		@Override
		public String toString()
		{
			return m_name;
		}
	}
}
