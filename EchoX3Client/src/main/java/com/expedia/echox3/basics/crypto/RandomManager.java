/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.security.SecureRandom;
import java.util.Random;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

public class RandomManager
{
	private static final String SETTING_PREFIX			= RandomManager.class.getName();
	private static final String SETTING_ALGORITHM		= SETTING_PREFIX + ".algorithm";
	private static final String DEFAULT_ALGORITHM		= "SHA1PRNG";
	private static String s_algorithmName;

	static
	{
		s_algorithmName = ConfigurationManager.getInstance().getSetting(SETTING_ALGORITHM, DEFAULT_ALGORITHM);
		new ConfigurationListener();
	}


	public static String getAlgorithmName()
	{
		return s_algorithmName;
	}

	public static Random getRandom()
	{
		return new Random();
	}

	public static Random getRandom(long seed)
	{
		return new Random(seed);
	}

	public static SecureRandom getSecureRandom()
	{
		return getSecureRandom(getAlgorithmName());
	}

	public static SecureRandom getSecureRandom(String algorithm)
	{
		try
		{
			return SecureRandom.getInstance(algorithm);
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_NO_SECURE_RANDOM_ALGORITHM, exception,
					"No SecureRandom algorithm found in the system");
		}
	}

	public static SecureRandom getSecureRandom(String algorithm, String providerName)
	{
		try
		{
			return SecureRandom.getInstance(algorithm, providerName);
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_NO_SECURE_RANDOM_ALGORITHM, exception,
					"No SecureRandom algorithm found in the system");
		}
	}
	public static String getRandomName(Random random)
	{
		String randomName;
		if (random instanceof SecureRandom)
		{
			SecureRandom secureRandom		= (SecureRandom) random;
			randomName = secureRandom.getAlgorithm();
		}
		else
		{
			randomName = random.getClass().getSimpleName();
		}
		return randomName;
	}

	private static class ConfigurationListener
	{
		public ConfigurationListener()
		{
			PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);
		}

		public void updateConfiguration(String publisherName, long timeMS, Object event)
		{
			s_algorithmName = ConfigurationManager.getInstance().getSetting(SETTING_ALGORITHM, s_algorithmName);
		}
	}
}
