/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import java.net.UnknownHostException;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.tools.hash.FnvHash;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.hash.IHashProvider;
import com.expedia.echox3.basics.tools.hash.JenkinsHash;

public class HashProviderTests extends AbstractTestTools
{
	private static final byte[]		QUICK_FOX		= "The quick brown fox jumps over the lazy dog.".getBytes();

	@Test
	public void testUnnamed() throws UnknownHostException
	{
		logTestName();

		IHashProvider	provider		= HashUtil.getHashProvider();
		validate(provider, FnvHash.hash32(QUICK_FOX));
		provider.release();
	}

	private void validate(IHashProvider provider, int hashExpected)
	{
		provider.add32(QUICK_FOX, 0, QUICK_FOX.length);
		int				hash			= provider.getHashCode32();
		assertEquals(hashExpected, hash);
	}

	@Test
	public void testFvn() throws UnknownHostException
	{
		logTestName();

		IHashProvider	provider		= FnvHash.get();
		validate(provider, FnvHash.hash32(QUICK_FOX));
		provider.release();
	}

	@Test
	public void testJenkins() throws UnknownHostException
	{
		logTestName();

		IHashProvider	provider		= JenkinsHash.get();
		validate(provider, JenkinsHash.hash32(QUICK_FOX));
		provider.release();
	}

	@Test
	public void testPerformance() throws UnknownHostException
	{
		for (int n : new int[] { 10, 20, 50, 100, 200, 10 })
		{
			HashUtil.measure(n);
		}
	}
}
