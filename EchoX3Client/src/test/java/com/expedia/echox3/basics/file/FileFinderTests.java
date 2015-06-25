/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.file;

import java.util.List;

import org.junit.Test;
import org.junit.Assert;		// To find classes in this package.
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;

public class FileFinderTests extends AbstractTestTools
{
	@Test
	@SuppressWarnings("rawtypes")
	public void testHello() throws Exception
	{
		logTestName();

		String			packageName		= Assert.class.getPackage().getName();
		long			t1				= System.nanoTime();
		List<Class>		classList		= FileFinder.getClassList(packageName);
		long			t2				= System.nanoTime();
		reportPerformance(String.format("Found %,d classes", classList.size()), (t2 - t1), 1, true);
		assertNotNull(classList);
		assertTrue(!classList.isEmpty());
	}

}
