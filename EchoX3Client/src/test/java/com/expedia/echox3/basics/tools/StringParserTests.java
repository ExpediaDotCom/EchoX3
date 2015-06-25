/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.tools.string.StringParser;

public class StringParserTests extends AbstractTestTools
{
	private String m_line1;
	private String m_line2;
	private String m_line;

	private 	StringParser	m_stringParser;

	@Before
	public void setUp()
	{
		m_line1				= Long.toString(RANDOM.nextLong());
		m_line2				= Long.toString(RANDOM.nextLong());
		m_line				= m_line1 + "\n" + m_line2;
		m_stringParser		= new StringParser(m_line);
	}

	@Test
	public void testSkipLine()
	{
		assertEquals(m_line1, m_stringParser.skipLine());
		assertEquals(m_line2, m_stringParser.getString());
	}

	@Test
	public void testSkipEntireLine()
	{
		assertEquals(m_line, m_stringParser.skip(m_line.length()));
		assertTrue(m_stringParser.isEmpty());
	}

	@Test
	public void testSkipFirstPart()
	{
		setUp();
		assertEquals(m_line1, m_stringParser.skip(m_line1.length()));
		assertEquals(m_line2, m_stringParser.getString());
	}

	@Test
	public void testSkipToNonExistentString()
	{
		assertEquals(m_line, m_stringParser.skipTo("A")); // m_line contains only numbers and \n's
		assertTrue(m_stringParser.isEmpty());
	}

	@Test
	public void testCleanup()
	{
		m_stringParser		= new StringParser("\r\r\r\r");
		m_stringParser.cleanup();
		assertEquals("\n", m_stringParser.getString());
	}
}
