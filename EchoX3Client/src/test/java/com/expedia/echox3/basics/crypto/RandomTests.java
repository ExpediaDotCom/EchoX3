/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.util.Collection;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;

public class RandomTests extends AbstractTestTools
{
	@Test
	public void testHello()
	{
		String				randomType		= "SecureRandom";

		getLogger().info(BasicEvent.EVENT_TEST, "Listing algorithms of type %s", randomType);
		Collection<String>	algorithmList		= CryptoManager.getAlgorithmSet(randomType);
		Random[]			randomList	= new Random[algorithmList.size() + 1];
		int					iRandom		= 0;
		randomList[iRandom++] = new Random();
		for (String algorithm : algorithmList)
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Algorithm: %s", algorithm);
			randomList[iRandom++] = RandomManager.getSecureRandom(algorithm);
		}

		showHeader(randomList);
		int				count			= 10 * 1000;
		int				cBin			= 100;
		long[]		durationListNS		= new long[randomList.length];
		for (int i = 0; i < 10; i++)
		{
			for (iRandom = 0; iRandom < randomList.length; iRandom++)
			{
				durationListNS[iRandom] = validateDistribution("Random", randomList[iRandom], count, cBin, false);
			}
			showResults(durationListNS);
		}

		validateDistribution("Inform",	randomList[0],		count, cBin, false);
		BasicTools.sleepMS(100);
	}
	private long validateDistribution(String name, Random random, int count, int cBin, boolean inform)
	{
		int			expectedBinCount	= count / cBin;
		int			expectedSTD			= Double.valueOf(Math.sqrt(expectedBinCount)).intValue();
		int			expectedBinMin		= expectedBinCount - (6 * expectedSTD);
		int			expectedBinMax		= expectedBinCount + (6 * expectedSTD);

		long		t1					= System.nanoTime();
		int[]		bins				= new int[cBin];
		for (int i = 0; i < count; i++)
		{
			int		rnd		= random.nextInt(cBin);
			bins[rnd]++;
		}
		long		t2					= System.nanoTime();
		long		durationNS			= reportPerformance(String.format("%-15s", name), t2 - t1, count, inform);

		String		randomName			= RandomManager.getRandomName(random);
		int			min					= Integer.MAX_VALUE;
		int			max					= Integer.MIN_VALUE;
		for (int iBin = 0; iBin < cBin; iBin++)
		{
			assertTrue(String.format("%-10s: Validating bin[%,5d] = %,9d against min = %,9d",
					randomName, iBin, bins[iBin], expectedBinMin), bins[iBin] >= expectedBinMin);
			assertTrue(String.format("%-10s: Validating bin[%,5d] = %,9d against max = %,9d",
					randomName, iBin, bins[iBin], expectedBinMax), bins[iBin] <= expectedBinMax);
			min = Math.min(min, bins[iBin]);
			max = Math.max(max, bins[iBin]);
		}
		if (inform)
		{
			getLogger().info(BasicEvent.EVENT_TEST,
					"Found: Min = %,9d/%,d; expected = %,9d; max = %,9d/%,9d",
					min, expectedBinMin, expectedBinCount, max, expectedBinMax);
		}

		return durationNS;
	}
	public void showHeader(Random[] randomList)
	{
		StringBuilder		sb			= new StringBuilder(200);

		for (int i = 0; i < randomList.length; i++)
		{
			String			randomName		= RandomManager.getRandomName(randomList[i]);
			sb.append(String.format("%20s ", randomName));
		}

		getLogger().info(BasicEvent.EVENT_TEST, sb.toString());
	}
	private void showResults(long[] durationListUS)
	{
		StringBuilder		sb			= new StringBuilder(200);

		for (int i = 0; i < durationListUS.length; i++)
		{
			sb.append(String.format("%20s ", TimeUnits.formatNS(durationListUS[i])));
		}

		getLogger().info(BasicEvent.EVENT_TEST, sb.toString());
	}
}
