/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;

public class CryptoTests extends AbstractTestTools
{
	@BeforeClass
	public static void beforeCryptoTest()
	{
		CryptoManager.getInstance();
	}

	@Test
	public void testHello()
	{
		logTestName();

		Set<String>		set		= CryptoManager.getAlgorithmSet("SecureRandom");
		for (String algorithm : set)
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Algorithm: " + algorithm);
		}
	}

	@Test
	public void testPassword()
	{
		logTestName();

		int[]		lengthList			= { 5, 5, 10, 10, 64, 64};
//		Random		random				= SecureRandomManager.getSecureRandom();
		long		passwordNumber		= 47;

		String[]	passwordList		= validatePasswordList(passwordNumber, lengthList, null);
		validatePasswordList(passwordNumber, lengthList, passwordList);
		validatePasswordList(passwordNumber, lengthList, passwordList);

		lengthList = new int[] { 10 };
		for (int i = 0; i < 10; i++)
		{
			validatePasswordList(RANDOM.nextLong(), lengthList, null);
		}
	}
	private String[] validatePasswordList(long passwordNumber, int[] lengthList, String[] passwordList)
	{
		if (null == passwordList)
		{
			passwordList = new String[lengthList.length];
		}
		for (int i = 0; i < lengthList.length; i++)
		{
			int				length			= lengthList[i];
			String			password		= PasswordGenerator.getPassword(passwordNumber, length);
			getLogger().info(BasicEvent.EVENT_TEST, "Password(%,26d; %2d) = %s", passwordNumber, length, password);

			if(null == passwordList[i])
			{
				passwordList[i] = password;
			}
			else
			{
				assertTrue(passwordList[i].equals(password));
			}
		}
		return passwordList;
	}

	@Test
	public void testBootstrap()
	{
		logTestName();

		validateNamedAlgorithms(CryptoManager.ALGORITHM_BIG_BANG);
		validateNamedAlgorithms(CryptoManager.ALGORITHM_SECRET);
//		validateNamedAlgorithms(CryptoManager.ALGORITHM_PUBLIC);
	}

	private void validateNamedAlgorithms(String name)
	{
		String			originalText			= "The quick brown fox ...";
		String			cipherText				= CryptoManager.getInstance().encrypt(name, originalText);
		String			clearText				= CryptoManager.getInstance().decrypt(cipherText);

		getLogger().info(BasicEvent.EVENT_TEST, "CipherText = " + cipherText);
		assertEquals(originalText, clearText);
	}

	@Test
	public void listAlgorithms()
	{
		String[]		typeList		=
				{ "SecureRandom", "KeyGenerator", "KeyPairGenerator", "SecretKeyFactory", "Cipher" };

		for (String type : typeList)
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Listing algorithms of type %s", type);

			Collection<String>		algorithmList		= CryptoManager.getAlgorithmSet(type);
			for (String algorithm : algorithmList)
			{
				getLogger().info(BasicEvent.EVENT_TEST, "Algorithm: %s", algorithm);
			}
		}
	}

	@Test
	public void testCryptoPerformance()
	{
		logTestName();

		String[]		aliasList		=
				{
						CryptoManager.ALGORITHM_BIG_BANG
						, CryptoManager.ALGORITHM_SECRET
//						, CryptoManager.ALGORITHM_PUBLIC
				};
		Set<String>		aliasSet		= CryptoManager.getInstance().getAliasList();

		int				count			= 1000;
		int				cch				= 235;
		String[]		textList		= new String[count];
		for (int i = 0; i < count; i++)
		{
			textList[i] = PasswordGenerator.getRandomPassword(cch);
		}

		int			iMax		= 2;
		for (int i = 0; i < iMax; i++)
		{
			for (String alias : aliasList)
			{
				measurePerformance(alias, textList);
			}
		}
		getLogger().info(BasicEvent.EVENT_TEST, "**************************************************");

		for (int i = 0; i < iMax; i++)
		{
			for (String alias : aliasSet)
			{
				measurePerformance(alias, textList);
			}
		}

	}

	@SuppressWarnings("PMD.UnusedPrivateMethod")		// Sometimes used
	private double measurePerformance(String alias, String[] textList)
	{
		CryptoManager		manager		= CryptoManager.getInstance();
		long				t1			= System.nanoTime();
		for (String text : textList)
		{
			String		cipher		= manager.encrypt(alias, text);
			String		back		= manager.decrypt(cipher);
			assertEquals(text, back);
		}
		long		t2			= System.nanoTime();

		return reportPerformance(String.format("%-30s (%,5d chars)",
				alias, textList[0].length()), (t2 - t1), textList.length, true);
	}
}
