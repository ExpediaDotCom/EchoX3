/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.event;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class BasicException extends Exception
{
	private static final long serialVersionUID		= 20130331235959L;

	private BasicEvent m_eventIndex;

	public BasicException(BasicEvent eventIndex, String format, Object...args)
	{
		super(eventIndex.toString() + ": " + String.format(format, args));

		m_eventIndex = eventIndex;
	}

	public BasicException(BasicEvent eventIndex, Throwable throwable, String format, Object...args)
	{
		super(eventIndex.toString() + ": " + String.format(format, args), throwable);

		m_eventIndex = eventIndex;
	}

	public BasicEvent getBasicEvent()
	{
		return m_eventIndex;
	}

	public String getMessageChain()
	{
		StringBuilder sb 			= new StringBuilder(1000);

		Throwable throwable	= this;
		while (null != throwable)
		{
			sb.append(throwable.getClass().getSimpleName());
			sb.append('=');
			sb.append(throwable.getMessage());
			sb.append('\n');
			throwable = throwable.getCause();
		}

		return sb.toString();
	}

	public String getCallStackChain()
	{
		ByteArrayOutputStream outputStream	= new ByteArrayOutputStream();
		PrintStream printStream		= new PrintStream(outputStream);

		printStackTrace(printStream);
		printStream.flush();
		byte[]						bytes			= outputStream.toByteArray();
		String stackTrace		= new String(bytes);
		return stackTrace;
	}
}
