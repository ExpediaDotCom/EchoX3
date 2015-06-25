/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.jvm.WallClockMeasureProxy;

public class WallClockMeasureProxyTests extends AbstractTestTools
{
	private WallClockMeasureProxy m_wallClockMeasureProxy;

	@Before
	public void setUp()
	{
		m_wallClockMeasureProxy = new WallClockMeasureProxy();
	}

	@Test
	public void testToString()
	{
		final String expected ="MasterName=null; CorrectedTime=0; LocalTime=0; MeasuredTime=0; OffsetMS=0";
		assertEquals(expected, m_wallClockMeasureProxy.toString());
	}
}
