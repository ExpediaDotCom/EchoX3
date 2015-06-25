/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.event;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.expedia.echox3.basics.monitoring.event.BasicEvent.LogMessage;

@SuppressWarnings({"PMD", "rawtypes"})
public class BasicLogger
{
	private final LogMessage	m_message		= new LogMessage();
	private final Logger		m_logger;

	public BasicLogger(Class clazz)
	{
		m_logger = LogManager.getLogger(clazz.getName());
	}

	public Logger getLogger()
	{
		return m_logger;
	}

	public boolean isDebugEnabled()
	{
		return m_logger.isDebugEnabled();
	}
	public boolean isLevelEnabled(Level level)
	{
		return m_logger.isEnabledFor(level);
	}

	public void log(Level level, BasicEvent event, String format, Object...args)
	{
		log(level, event, null, format, args);
	}

	public void log(Level level, BasicEvent event, Throwable throwable, String format, Object...args)
	{
		if (getLogger().isEnabledFor(level))
		{
			String					messageText		= getMessage(event, format, args);
			// One message per logger
			synchronized (m_message)
			{
				m_message.set(event, messageText);
				getLogger().log(level, m_message, throwable);
			}
		}
	}

	public void trace(BasicEvent event, String format, Object...args)
	{
		trace(event, null, format, args);
	}
	public void trace(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.TRACE, event, throwable, format, args);
	}

	public void debug(BasicEvent event, String format, Object...args)
	{
		debug(event, null, format, args);
	}
	public void debug(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.DEBUG, event, throwable, format, args);
	}

	public void info(BasicEvent event, String format, Object...args)
	{
		info(event, null, format, args);
	}
	public void info(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.INFO, event, throwable, format, args);
	}

	public void warn(BasicEvent event, String format, Object...args)
	{
		warn(event, null, format, args);
	}
	public void warn(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.WARN, event, throwable, format, args);
	}

	public void error(BasicEvent event, String format, Object...args)
	{
		error(event, null, format, args);
	}
	public void error(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.ERROR, event, throwable, format, args);
	}

	public void fatal(BasicEvent event, String format, Object...args)
	{
		fatal(event, null, format, args);
	}
	public void fatal(BasicEvent event, Throwable throwable, String format, Object...args)
	{
		log(Level.FATAL, event, throwable, format, args);
	}

	private String getMessage(BasicEvent event, String format, Object...args)
	{
		// eventIndex and eventText are inserted by the logger formatter
//		String			eventText	= event.toString();
		String			message		= String.format(format, args);
//		StringBuilder	sb			= new StringBuilder(eventText.length() + message.length() + 5);

//		sb.append(eventText);
//		sb.append(" ");
//		sb.append(message);

//		return sb.toString();
		return message;
	}
}
