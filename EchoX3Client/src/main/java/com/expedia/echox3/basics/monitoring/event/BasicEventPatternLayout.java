/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

@SuppressWarnings("rawtype")
public class BasicEventPatternLayout extends PatternLayout
{
	// ~X1! means X-1; this "conversion string" identifies text replacements that will be done by this class, as opposed
	// to conversion characters from https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html.
	// Using a conversion string that beings with % causes super.format() to strip out characters we don't want touched,
	// and/or spurious log entries that dirty the logs.
	private final static String KEYWORD_BEGIN_DELIMITER		= "~X1!{";
	private final static String KEYWORD_END_DELIMITER		= "}";

	private 	 				String[]						m_keywordArray				= null;

	private final Map<Class<?>, Map<String, Method>>		m_classMethodMap			= new HashMap<>();

	// Default constructor is used when log4j parses log4j.properties
	public BasicEventPatternLayout()
	{
		super();
		m_keywordArray = new String[0];
	}

	public BasicEventPatternLayout(String pattern)
	{
		super(pattern);
		populateKeywordList();
	}

	@Override
	public String format(LoggingEvent loggingEvent)
	{
		final StringBuilder			format			= new StringBuilder(super.format(loggingEvent));
		final Object				message			= loggingEvent.getMessage();
		final Map<String, Method>	methodMap		= getMethodMap(message);
		for (int i = 0; i < m_keywordArray.length; i++)
		{
			final String keyword			= m_keywordArray[i];
			final Method method			= methodMap.get(keyword);
			Object replacement;
			try
			{
				if (null == method)
				{
					replacement		= "";
				}
				else
				{
					replacement		= method.invoke(message);
				}
			}
			catch (Exception e)
			{
				// Can't do the requested substitution; this should only happen if method.invoke() throws an
				// Exception, which should NEVER happen...it's just a getter. But I'm a LITTLE paranoid!
				replacement			= "*** EXCEPTION ***";
			}

			int		indexOfKeyword;
			while (-1 != (indexOfKeyword = format.indexOf(keyword)))
			{
				format.replace(indexOfKeyword, indexOfKeyword + keyword.length(), replacement.toString());
			}
		}
		return format.toString();
	}

	private Map<String, Method> getMethodMap(final Object message)
	{
		synchronized(m_classMethodMap)
		{
			Map<String, Method> methodMap		= m_classMethodMap.get(message.getClass());
			if (null == methodMap)
			{
				methodMap = new HashMap<>();
				for (int i = 0; i < m_keywordArray.length; i++)
				{
					final String methodName		= "get" + m_keywordArray[i].substring(
							KEYWORD_BEGIN_DELIMITER.length(),
							m_keywordArray[i].length() - KEYWORD_END_DELIMITER.length());
					try
					{
						final Method method			= message.getClass().getMethod(methodName);
						methodMap.put(m_keywordArray[i], method);
					}
					catch (NoSuchMethodException e)
					{
						// just continue; this object doesn't have the getter
					}
				}
				m_classMethodMap.put(message.getClass(), methodMap);
			}
			return methodMap;
		}
	}

	@Override
	public void setConversionPattern(String pattern)
	{
		super.setConversionPattern(pattern);
		populateKeywordList();
	}

	private void populateKeywordList()
	{
		final String conversionPattern	= getConversionPattern();
		final Set<String> keywordSet			= new HashSet<>();
		int						beginIndex			= 0;
		while(-1 != (beginIndex = conversionPattern.indexOf(KEYWORD_BEGIN_DELIMITER, beginIndex)))
		{
			final int			endIndex			= conversionPattern.indexOf(KEYWORD_END_DELIMITER, beginIndex);
			if(-1 != endIndex)
			{
				final int		endIndexWDelimiter	= endIndex + KEYWORD_END_DELIMITER.length();
				final String keyword				= conversionPattern.substring(beginIndex, endIndexWDelimiter);
				keywordSet.add(keyword);
				beginIndex							= endIndexWDelimiter;
			}
		}
		m_keywordArray								= keywordSet.toArray(new String[keywordSet.size()]);
	}
}
