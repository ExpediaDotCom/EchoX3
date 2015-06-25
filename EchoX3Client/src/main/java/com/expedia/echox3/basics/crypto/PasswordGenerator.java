/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.util.Random;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;

/**
 * @author Pierre
 *
 *	Generates random passwords. This class includes a main() that runs as a command line
 *	application to facilitate the bootstrap process.
 */
public class PasswordGenerator
{
	public static final int			PASSWORD_CHARCOUNT_MIN		= 5;
	public static final int			PASSWORD_CHARCOUNT			= 64;
	public static final int			PASSWORD_CHARCOUNT_MAX		= 256;

	public static final Random		RANDOM;
	// Got to start somewhere.
	// The last (Long.SIZE/8 == 8) bytes will be replaced with the passwordNumber
	private static final long		RANDOM_SEED			= 4868073327564345421L + System.nanoTime();

	private static final String PASSWORD_VALID_CHARACTERS
				= "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%*abcdefghijklmnopqrstuvwxyz";

	static
	{
		RANDOM = RandomManager.getRandom(RANDOM_SEED);
	}

	@SuppressWarnings("PMD")
	public static void main(String[] args) throws Exception
	{
		for (String arg : args)
		{
			for (int i = 0; i < 5; i++)
			{
				int				cch			= Integer.parseInt(arg.trim());
				String			password	= getRandomPassword(cch);
				System.out.println(String.format("Password(%-10s) = %s", "ClearText", password));
				processAlgorithm(CryptoManager.ALGORITHM_BIG_BANG, password);
				processAlgorithm(CryptoManager.ALGORITHM_SECRET, password);
				System.out.println();
			}
		}
	}
	@SuppressWarnings("PMD.SystemPrintln")
	private static void processAlgorithm(String algorithm, String password)
	{
		CryptoManager	manager				= CryptoManager.getInstance();
		String			algorithmName		= manager.getAlgorithmName(algorithm);
		String			cipher				= manager.encrypt(algorithm, password);
		System.out.println(String.format("Cipher   =%s\t%s", algorithmName, cipher));
	}

	public static String getRandomPassword(int charCount)
	{
		return getRandomPassword(RANDOM, charCount);
	}

	public static String getPassword(String numberString, String charCountText) throws Exception
	{
		long	number		= Long.parseLong(numberString);
		int		charCount	= Integer.parseInt(charCountText);

		return getPassword(number, charCount);
	}

	public static String getPassword(long passwordNumber, int charCount)
	{
		if (0 >= charCount)
		{
			charCount = PASSWORD_CHARCOUNT;
		}

		if (PASSWORD_CHARCOUNT_MIN > charCount || PASSWORD_CHARCOUNT_MAX <= charCount)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_PASSWORD_INVALID_PARAMETER,
					String.format("Invalid parameter to PasswordGenerator.getPassword(%d)."
									+ " charCount must be between %d and %d (incl.)",
							charCount, PASSWORD_CHARCOUNT_MIN, PASSWORD_CHARCOUNT_MAX
					)
			);
		}

		// This random MUST be predictable!
		Random random		= RandomManager.getRandom(passwordNumber);

		return getRandomPassword(random, charCount);
	}

	private static String getRandomPassword(Random random, int charCount)
	{
		StringBuilder	password	= new StringBuilder(charCount + 1);
		for (int i = 0; i < charCount; i++)
		{
			int			n			= random.nextInt(PASSWORD_VALID_CHARACTERS.length());
			int			charNumber	= n % PASSWORD_VALID_CHARACTERS.length();
			char		c			= PASSWORD_VALID_CHARACTERS.charAt(charNumber);
			password.append(c);
		}
		return password.toString();
	}
}
