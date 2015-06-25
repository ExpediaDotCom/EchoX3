/**
 * Copyright 2012 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */

package com.expedia.echox3.basics.tools.time;


import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

/**
 * Determine if a computer displays a problem with its high-performance counters.
 * This is seen on AMD computers running Windows-XP.
 * The problem is caused by an excessive software OS optimization that bypasses synchronization on multiple
 * processors machines. The result is that multiple continuous calls to the high performance counter
 * (e.g. java's System.nanoTime()) will see the clock move backwards.
 * The fix is a single line in boot.ini, with which Expedia's ops team is very familiar with.
 * All Expedia's lab and production SHOULD have the fix; some may not have it...
 * This problem was believed to be eradicated in 2010. However, it was seen again in the winter of 2012.
 *
 * Usage of this class is usually totally transparent.
 * It is activated automatically (within MultiCache code) and will only report itself if there is an issue.
 */
public class TimeTraveler extends Thread
{
	private static final BasicLogger		LOGGER					= new BasicLogger(TimeTraveler.class);
	private static final int 				DURATION_MS				= 5000;
	private static volatile Boolean			s_isTimeTraveler		= null;

	static
	{
		new TimeTraveler();
	}

	private TimeTraveler()
	{
		setDaemon(true);
		start();
	}

	/**
	 * Causes the class to be loaded, the static block does the work.
	 */
	public static void measureTimeTravel()
	{
		// No work to do here.
	}

	/**
	 * If true, indicates the nano clock, System.nanoTime(), can move backwards.
	 * This occurs on some XP/AMD systems and can generally be fixed with a boot.ini fix.
	 * In any case, it is undesirable and causes problems to the counters code...
	 * It should be corrected.
	 *
	 * The value is only measured once, then it is cached. The cached value is measured.
	 * The fix requires a reboot.
	 *
	 * @return		True = bad: The clock can move backwards; False = good: The clock continuous moves forward.
	 */
	public static boolean isTimeTraveler()
	{
		while (null == s_isTimeTraveler)
		{
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
				// Keep waiting
			}
		}

		return s_isTimeTraveler;
	}

	@Override
	public void run()
	{
		String threadName		= getClass().getSimpleName();
		setName(threadName);

		long		startTime		= System.currentTimeMillis();
		long		endTime			= startTime + DURATION_MS;
		long		timeNanoSav		= System.nanoTime();

		while (System.currentTimeMillis() < endTime)
		{
			long	timeNano		= System.nanoTime();
			if (timeNano < timeNanoSav)		// == allowed on REALLY fast systems :)
											// OR on systems who only report time to the us.
			{
				s_isTimeTraveler = true;
				String message		=
						  "This computer can travel backwards in time. "
						+ "The high precision clock on this computer can move backwards. "
						+ "This will have a negative impact on counters and other high-precision measurements. "
						+ "Please have the high precision clock of this computer set "
						+ "appropriately for a multi-core environment.";
				LOGGER.error(BasicEvent.EVENT_TIME_TRAVELER, message);
				return;
			}
			timeNanoSav = timeNano;
		}

		s_isTimeTraveler = false;
	}
}
