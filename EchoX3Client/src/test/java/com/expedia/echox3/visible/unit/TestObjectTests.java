/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.unit;

import java.io.Serializable;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.visible.application.test.TestObjectFactory;
import com.expedia.echox3.visible.application.test.TestReadRequest;
import com.expedia.echox3.visible.application.test.TestReadResponse;
import com.expedia.echox3.visible.application.test.TestWriteRequest;
import com.expedia.echox3.visible.trellis.ICacheObject;

public class TestObjectTests extends AbstractTestTools
{
	private static final String		QUICK_FOX		= "The quick brown fox";
	@Test
	public void testRequests()
	{
		TestReadRequest		readRequest1	= new TestReadRequest(100, 10, false);
		TestReadRequest		readRequest2	= new TestReadRequest(100, 10, true);
		TestWriteRequest	writeRequest	= new TestWriteRequest(100, 10, false, QUICK_FOX);
		validateRequest(readRequest1, 8, 25, false);
		validateRequest(readRequest2, 8, 25, true);
		validateRequest(writeRequest, 8, 25, false);
	}
	private void validateRequest(Serializable object,					// NOPMD
			long durationMin, long durationMax, boolean expectThrow)
	{
		Exception		exception		= null;

		long			t1				= System.currentTimeMillis();
		try
		{
			if (object instanceof TestReadRequest)
			{
				((TestReadRequest) object).work();
			}
			if (object instanceof TestWriteRequest)
			{
				((TestWriteRequest) object).work();
			}
		}
		catch (Exception ex)
		{
			exception = ex;
		}
		long			t2				= System.currentTimeMillis();
		long			durationMS		= t2 - t1;

		assertTrue(durationMS >= durationMin);
		assertTrue(durationMS < durationMax);
		assertTrue(expectThrow == (null != exception));
	}

	@Test
	public void testObject()
	{
		TestObjectFactory		factory			= new TestObjectFactory();
		ICacheObject object			= factory.createObject(null);
		TestReadRequest			readRequest		= new TestReadRequest(100, 10, false);
		TestWriteRequest		writeRequest	= new TestWriteRequest(100, 10, false, QUICK_FOX);

		object.writeOnly(writeRequest);
		Serializable			backSerial		= object.readOnly(readRequest);
		assertTrue(backSerial instanceof TestReadResponse);
		TestReadResponse		response		= (TestReadResponse) backSerial;
		String					backText		= response.getValue();
		assertNotNull(backText);
		assertFalse(backText.isEmpty());
		assertEquals(QUICK_FOX, backText);
	}
}
