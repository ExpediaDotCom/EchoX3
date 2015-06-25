/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.crypto.CryptoManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.time.TimeUnits;

public class MiscTests extends AbstractTestTools
{
	@Test
	public void testHello() throws UnknownHostException
	{
		logTestName();

		Map<Thread, StackTraceElement[]> map		= Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet())
		{
			Thread thread		= entry.getKey();
			getLogger().info(BasicEvent.EVENT_TEST,
					"%s/%s", thread.getThreadGroup().getName(), thread.getName());
		}
	}

	@Test
	public void testLogger()
	{
		logTestName();

		getLogger().info(BasicEvent.EVENT_TEST, "Test message");
	}

	@Test
	public void testBasicException()
	{
		logTestName();

		BasicException exception		= new BasicException(BasicEvent.EVENT_TEST,
				new IllegalArgumentException("This is a test"), "%s", "Test");
		String messageChain	= exception.getMessageChain();
		assertNotNull(messageChain);
		String stackTrace		= exception.getCallStackChain();
		assertNotNull(stackTrace);
	}

	@Test
	public void testRuntimeException()
	{
		logTestName();

		BasicRuntimeException			error		 = new BasicRuntimeException(BasicEvent.EVENT_TEST,
				new IllegalArgumentException("This is a test"), "%s", "Test");

		String messageChain	= error.getMessageChain();
		assertNotNull(messageChain);

		String stackTrace		= error.getCallStackChain();
		assertNotNull(stackTrace);
	}

	@Test
	public void testCrypto() throws Exception
	{
		logTestName();

		String clear		= "The quick brown fox jumps over the lazy dog.";
		String cipher		= CryptoManager.getInstance().encrypt(CryptoManager.ALGORITHM_BIG_BANG, clear);
		String back			= CryptoManager.getInstance().decrypt(cipher);
		assertEquals(clear, back);

		String cipher2		= CryptoManager.getInstance().encrypt(CryptoManager.ALGORITHM_BIG_BANG, "password");
		getLogger().info(BasicEvent.EVENT_TEST, "'%s'", cipher2);
	}
	
	@Test
	public void testBurn100US()
	{
		logTestName();

		final long burnUS = 100L;
		final long burned = BasicTools.burnCpuUS(burnUS);
		assertTrue(burned >= burnUS);
		// pcote: Missing a test to ensure it does not burn too much!
		// Technically, you would be better using ThreadMXBean to get the actual CPU used by this thread,
		// but that would be overkill.
		// I would simply suggest you use isTestEnabled(false) and validate (toBurn - 5%) < burned < (toBurn + 5%)
	}

/*
	@Test
	public void testSleep2Milliseconds()
	{
		if (!isTestEnabled(false))
		{
			return;
		}

		// pcote
		// Java's documentation says the thread will "sleep" or "wait"...
		// ..."The specified amount of real time has elapsed, more or less."
		// If the wait time for sleep(2) is exactly 1.95 ms, I would say that qualifies, but your test case will fail.
		final long start = BasicTools.getCurrentTimeMS();
		final int millisecondsToSleep = 2;
		BasicTools.sleep(millisecondsToSleep);
		final long endBasicTools.getCurrentTimeMS()s();
		assertTrue(end - start >= millisecondsToSleep);
	}
*/

	// Example of a test case that would run correctly on all machines
	@Test
	public void testSleep()
	{
		logTestName();

		validateSleep(  5);
		validateSleep( 10);
		validateSleep(255);
	}
	private void validateSleep(int sleepMS)
	{
		long		beginNS			= System.nanoTime();
		BasicTools.wait(sleepMS);
		long		endNS			= System.nanoTime();
		long		durationNS		= endNS - beginNS;
		double		durationMS		= durationNS / (1000. * 1000);

		String message			= String.format("SleepMS = %,d/%.3f", sleepMS, durationMS);
		getLogger().debug(BasicEvent.EVENT_DEBUG, message);
		assertTrue(message, durationMS > (sleepMS -  1.0));		// Sleep is not that precise
		assertTrue(message, durationMS < (sleepMS + 10.0));		// Can sometime sleep a bit too long.
	}

// Adjusted BasicTools.sleep() to NOT short-cut 0: AbstractScheduledThread needs sleep(0) to wait.
/*
	@Test
	public void testSleep0Milliseconds()
	{
		if (!isTestEnabled(false))
		{
			return;
		}

		BasicTools.sleep(0);
	}
*/
	@Test(expected = IllegalArgumentException.class)
	public void testSleepNegativeMilliseconds()
	{
		logTestName();

		// pcote: Here, you are testing java.lang.thread
		// IMHO, this is a superfluous test case.
		BasicTools.wait(-1);
	}

	@Test
	public void testFormatTimeUnits()
	{
		logTestName();

		assertEquals("1 sec 230 ms",	TimeUnits.formatMS(1230));
		assertEquals("1 sec 230 ms",	TimeUnits.formatUS(1230456));
		assertEquals("1 sec 230 ms",	TimeUnits.formatNS(1230456789));
		assertEquals("230 ms",			TimeUnits.formatNS(230000000));
		assertEquals("230 ms 456 us",	TimeUnits.formatNS(230456789));
		assertEquals("456 us 789 ns",	TimeUnits.formatNS(456789));
	}

	@Test
	public void testParseTimeUnits()
	{
		logTestName();

		assertEquals(300 * 1000, TimeUnits.getTimeMS(5, "min"));
	}


	enum Foo
	{
		Foo1,
		Foo2
	}
	@Test
	public void testEnumName()
	{
		logTestName();

		for (Foo foo : Foo.values())
		{
			Foo		bar		= Foo.valueOf(foo.name());
			assertTrue(foo.equals(bar));

			getLogger().info(BasicEvent.EVENT_TEST, foo.name());
		}
	}

	@Test
	public void testByteBuffer()
	{
		int			cb				= 100;
		int			cbMin			= 20;
		int			cbMax			= 30;
		ByteBuffer	byteBuffer		= ByteBuffer.allocate(100);
		byte[]		bytes			= byteBuffer.array();
		for (int i = 0; i < cb; i++)
		{
			bytes[i] = (byte) i;
		}

		ByteBuffer	smallBuffer		= ByteBuffer.wrap(bytes, cbMin, cbMax - cbMin);
		int			i				= cbMin;
		while (smallBuffer.hasRemaining())
		{
			int		theByte			= smallBuffer.get();
			getLogger().info(BasicEvent.EVENT_DEBUG, "Test %d = %d", i, theByte);
			assert(i == theByte);

			i++;
		}
		assertEquals(i, cbMax);
	}

	@Test
	public void testSystemArrayCopy()
	{
		for (int size : new int[] {50, 120, 500, 1000, 2000 })
		{
			for (int i = 0; i < 5; i++)
			{
				measureArrayCopy(size, 10 * 1000);
			}
		}
	}
	private void measureArrayCopy(int cb, int count)
	{
		byte[]		bytesFrom		= new byte[cb];
		byte[]		bytesTo			= new byte[cb + 2];

		long		t1				= System.nanoTime();
		for (int i = 0; i < count; i++)
		{
			System.arraycopy(bytesFrom, 0, bytesTo, 2, bytesFrom.length);
		}
		long		t2				= System.nanoTime();
		long		durationNS		= t2 - t1;
		reportPerformance(String.format("System.ArrayCopy(%,d)", cb), durationNS, count, true);
	}
}
