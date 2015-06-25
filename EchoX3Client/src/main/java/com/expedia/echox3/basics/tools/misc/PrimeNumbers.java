/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.misc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.BasicFileReader;
import com.expedia.echox3.basics.file.BasicFileWriter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;

public class PrimeNumbers
{
	private static final BasicLogger LOGGER					= new BasicLogger(PrimeNumbers.class);

	private static final String PRIME_FOLDER			= "Prime";
	private static final String PRIME16_FILENAME		= "Prime16.dat";
	private static final String PRIME32_FILENAME		= "Prime32.dat";

	private static final int				PRIME16_COUNT				= 6542;
	private static final int				PRIME32_COUNT				= 105097565;

	private static final PrimeNumbers INSTANCE				= new PrimeNumbers();

	private int[]		m_primeList;
	private int			m_cPrime		= 0;
	private String m_filename;

	private PrimeNumbers()
	{
		boolean			is32		= BasicTools.getHeapSizeMax() > (1024 * 1024 * 1024);

		setSize(is32);
	}
	private void setSize(boolean is32)
	{
		int				count;
		if (is32)
		{
			m_filename = PRIME32_FILENAME;
			count = PRIME32_COUNT;
		}
		else
		{
			m_filename = PRIME16_FILENAME;
			count = PRIME16_COUNT;
		}
		m_primeList = new int[count];
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static boolean isPrime(long n)
	{
		if (0 == INSTANCE.m_cPrime)
		{
			try
			{
				INSTANCE.read();
			}
			catch (Exception exception)
			{
				INSTANCE.setSize(false);
				INSTANCE.generatePrimeListInternal();
			}
		}

		return INSTANCE.isPrimeInternal(n);
	}

	public static int nextPrime(int n)
	{
		while (!isPrime(n))
		{
			n++;
		}
		return n;
	}
	public static long nextPrime(long n)
	{
		while (!isPrime(n))
		{
			n++;
		}
		return n;
	}

	public static void main(String[] args) throws BasicException
	{
		if (0 != args.length)
		{
			testPrimeList(args);
		}
		else
		{
			generatePrimeList();
		}
	}

	private static void testPrimeList(String[] args)
	{
		timeStamp("Loading the prime file");
		isPrime(7);

		showPrimeList(2, 100);

		timeStamp("Testing the fixed list");
		long[]			fixedList		= {
				5, 47, 97, 101, 103, 107, 109,
				Integer.MAX_VALUE,
				Integer.MAX_VALUE + 2L,
				Long.MAX_VALUE - 2,
				Long.MAX_VALUE,
		};
		testPrimeList(fixedList);

		timeStamp("Testing the parameter list");
		long[]		valueList		= new long[args.length];
		for (int i = 0; i < args.length; i++)
		{
			long		n			= Long.parseLong(args[i]);
			valueList[i] = n;
		}
		testPrimeList(valueList);
	}
	private static void testPrimeList(long[] valueList)
	{
		for (long value : valueList)
		{
			boolean		isPrime		= isPrime(value);
			timeStamp(String.format("isPrime(%,26d) = %s", value, isPrime));
		}
	}
	private static void showPrimeList(long min, long max)
	{
		for (long value = min; value < max; value++)
		{
			boolean		isPrime		= isPrime(value);
			if (isPrime)
			{
				timeStamp(String.format("isPrime(%,26d) = %s", value, isPrime));
			}
		}
	}

	private static void generatePrimeList() throws BasicException
	{
		PrimeNumbers		generator		= new PrimeNumbers();

		timeStamp("Starting");
		generator.generatePrimeListInternal();

		timeStamp("... saving ...");
		generator.save(generator.m_primeList);

		timeStamp("... reading ...");
		PrimeNumbers		validator		= new PrimeNumbers();
		validator.read();

		timeStamp("... validating ...");
		if (validator.m_cPrime != generator.m_cPrime)
		{
			errorStamp("Length validation failed", null);
		}
		if (0 == validator.m_primeList[validator.m_primeList.length - 1])
		{
			errorStamp("Last element validation failed", null);
		}
		timeStamp("Done.");
	}

	private void generatePrimeListInternal()
	{

		addPrime( 2);
		addPrime( 3);
		addPrime( 5);
		addPrime( 7);
		addPrime(11);
		addPrime(13);
		addPrime(17);
		addPrime(19);

//		short		n;
		int		n;
		for (n = 21; n > 0; n += 2)			// Until rollover into negative, to include Integer.MAX_VALUE
		{
			if (1 == n % (100 * 1000 * 1000))
			{
				getLogger().info(BasicEvent.EVENT_PRIME_NUMBERS_FOUND,
						"@ %,14d: Found %,14d primes. Highest prime is %,14d.",
						n, m_cPrime, m_primeList[m_cPrime - 1]);
			}

			if (isPrimeInternal(n))
			{
				addPrime(n);
			}

			if (m_primeList.length == m_cPrime)
			{
				getLogger().debug(BasicEvent.EVENT_DEBUG, "Prime list is full!");
				break;
			}
		}
		getLogger().info(BasicEvent.EVENT_PRIME_NUMBERS_GENERATED,
				"@ %,14d: Generated %,14d primes. Highest prime is %,14d.",
						n, m_cPrime, m_primeList[m_cPrime - 1]);
	}

	private boolean isPrimeInternal(long n) throws BasicRuntimeException
	{
		long		primeMax		= Double.valueOf(Math.sqrt(n)).longValue();
		
		for (int i = 0; i < m_cPrime; i++)
		{
			int		prime		= m_primeList[i];

			if (prime > primeMax)
			{
				break;
			}

			if (0 == (n % prime))
			{
				return false;
			}
		}

		return true;
	}

	private void addPrime(int n)
	{
		m_primeList[m_cPrime++] = n;
	}

	private void save(int[] primeList) throws BasicException
	{
		String					filename		= BaseFileHandler.getWorkingFilename(PRIME_FOLDER, m_filename);
		BasicFileWriter			writer			= new BasicFileWriter();
		ObjectOutputStream		objectStream	= null;

		try
		{
			writer.openStream(filename);
			objectStream = new ObjectOutputStream(writer.getOutputStream());
			objectStream.writeObject(primeList);
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_PRIME_WRITE_FAILED,
					exception, "Failed to write primes to file " + filename);
		}
		finally
		{
			BaseFileHandler.closeSafe(objectStream);
			writer.close();
		}
	}

	private void read()
	{
		String filename		= BaseFileHandler.getWorkingFilename(PRIME_FOLDER, m_filename);
		BasicFileReader reader			= new BasicFileReader();

		ObjectInputStream objectStream	= null;
		try
		{
			reader.openStream(filename);
			objectStream = new ObjectInputStream(reader.getInputStream());
			Object object		= objectStream.readObject();
			m_primeList = (int[]) object;
			m_cPrime = m_primeList.length;
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_PRIME_READ_FAILED,
					exception, "Failed to read primes from file " + filename);
		}
		finally
		{
			BaseFileHandler.closeSafe(objectStream);
			reader.close();
		}
		getLogger().info(BasicEvent.EVENT_PRIME_NUMBERS_READ,
				"Read %,14d primes. Highest prime is %,14d.",
						m_cPrime, m_primeList[m_cPrime - 1]);
	}

	@SuppressWarnings("PMD.SystemPrintln")		// This IS a command line application!
	public static void timeStamp(String message)
	{
		Date		date			= new Date();
		String		fullMessage		= String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL %2$s", date, message);

		System.out.println(fullMessage);
	}

	@SuppressWarnings("PMD.SystemPrintln")		// This IS a command line application!
	public static void errorStamp(String operation, Throwable throwable)
	{
		String message		= operation;
		if (null != throwable)
		{
			message = String.format("Exception in '%s' -> %s: %s",
					operation, throwable.getClass().getSimpleName(), throwable.getMessage());
		}

		Date date			= new Date();
		String fullMessage		= String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL %2$s", date, message);

		System.err.println(fullMessage);
		if (null != throwable)
		{
			throwable.printStackTrace(System.err);
		}
	}
}
