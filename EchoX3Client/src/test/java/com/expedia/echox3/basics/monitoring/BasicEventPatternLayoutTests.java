/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring;

import org.junit.Test;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import static org.apache.log4j.Level.DEBUG;
import static org.junit.Assert.assertEquals;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicEvent.LogMessage;
import com.expedia.echox3.basics.monitoring.event.BasicEventPatternLayout;


public class BasicEventPatternLayoutTests extends AbstractTestTools
{
	private static final Logger		REAL_LOGGER		= Logger.getLogger(AbstractTestTools.class);

	@Test
	public void testFormatWithDefaultConstructor()
	{
		final BasicEventPatternLayout	basicEventPatternLayout		= new BasicEventPatternLayout();
		final LogMessage				message						=
													new BasicEvent.LogMessage(BasicEvent.EVENT_TEST, "Default message");
		BasicEvent						eventTodo					= BasicEvent.EVENT_TEST;
		int								codeTodo					= eventTodo.getCode();
		assertEquals(eventTodo, BasicEvent.get(codeTodo));
		assertEquals(Integer.toString(BasicEvent.EVENT_TEST.getCode()), message.getEventCode());
		assertEquals(BasicEvent.EVENT_TEST.getName(), message.getEventName());

		final LoggingEvent				loggingEvent				= new LoggingEvent(
												message.getClass().getName(), REAL_LOGGER, DEBUG, message, null);
		final String					format						= basicEventPatternLayout.format(loggingEvent);
		assertEquals("Default message" + BaseFileHandler.LINE_SEPARATOR, format);
	}

	@Test
	public void testFormatWithPatternConstructor()
	{
		// Jam words together, use codes and names multiple times, to verify the parsing of the codes
		final BasicEventPatternLayout	basicEventPatternLayout		= new BasicEventPatternLayout(
				"~X1!{EventCode}'s ~X1!{InvalidMethod}name is~X1!{EventName}~X1!{EventName}'s code is~X1!{EventCode}");

		final BasicEvent.LogMessage		message					= new BasicEvent.LogMessage(BasicEvent.EVENT_TEST, "");

		final LoggingEvent				loggingEvent				= new LoggingEvent(
				message.getClass().getName(), REAL_LOGGER, DEBUG, message, null);
		final String					format						= basicEventPatternLayout.format(loggingEvent);
		assertEquals("20's name isTestTest's code is20", format);
	}
}
