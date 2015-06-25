/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.configuration.FolderConfigurationProvider;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.BasicFileWriter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;

/**
 * Algorithm usage:
 * AlgorithmBigBang is used to decode all passwords of Bootstrap algorithms (aka Password style algorithms) and only that.
 * AlgorithmBootstrap is used to decode passwords into the keystore.
 * The keystore has a separate password for the keystore itself and for each of the alias in it.
 * These passwords are stored in the crypto folder, in configuration files.
 *
 * AlgorithmSecure is used for anything that requires encryption.
 * The encrypted strings are stored where they belong.
 *
 * For each of these, the file crypto.config.properties contains the mapping to the current algorithm/key(s).
 */
public class CryptoManager
{
	public static final String			ALGORITHM_BIG_BANG			= "AlgorithmBigBang";
	public static final String			ALGORITHM_SECRET			= "AlgorithmSecret";
//	public static final String			ALGORITHM_PUBLIC			= "AlgorithmPublic";

	public static final String			SETTING_PREFIX				= CryptoManager.class.getName();
	public static final String			SETTING_PREFIX_ALIAS		= SETTING_PREFIX + ".alias.";
	public static final String			SETTING_PREFIX_SECRET		= SETTING_PREFIX + ".secret.";
	public static final String			SETTING_PREFIX_PUBLIC		= SETTING_PREFIX + ".public.";
	public static final String			SETTING_KEYSTORE_PASSWORD	= SETTING_PREFIX + ".KeyStore.password";
	public static final String			BOOTSTRAP_SETTING_FORMAT	= "%1$s%2$s=%3$s\t%4$s";

	public static final String			WORKING_FOLDER_NAME			= "Crypto";

	private static final String			WORKING_CONFIG_FILENAME		= "crypto.config.properties";

	private static final BasicLogger	LOGGER						= new BasicLogger(CryptoManager.class);

	private static final String			ALIAS_DELIMITER				= " ";
	private static final String			ALIAS_FORMAT				= "%1$s-%2$tY.%2$tm.%2$td-%2$tH.%2$tM.%2$tS.%2$tL";
	private static final Date			SCRATCH_DATE				= new Date();		// To avoid garbage

	private static final CryptoManager	INSTANCE					= new CryptoManager();

	private Map<String, String>					m_aliasAliasMap			= new HashMap<>();
	private Map<String, ICrypto>				m_aliasCryptoMap		= new TreeMap<>();

/*
	static
	{
		// Force this to happen after INSTANCE is initialized, so it is available to the keystore.
		INSTANCE.loadKeyStoreRW();
	}
*/

	private CryptoManager()
	{
		// Create the Crypto specific configuration provider...
		new FolderConfigurationProvider(BaseFileHandler.getWorkingFolderName(WORKING_FOLDER_NAME));

		// Do this first, so loadAliasTable() can override
		ICrypto		bigBangCrypto		= new CryptoSharedSecret();
		addAlias(ALGORITHM_BIG_BANG, bigBangCrypto.getAliasName());
		addProvider(bigBangCrypto);

		loadAliasTable();
		loadSecretProviderSet();
	}

	public static String getWorkingFilename()
	{
		return BaseFileHandler.getWorkingFilename(WORKING_FOLDER_NAME, WORKING_CONFIG_FILENAME);
	}

	private static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public static CryptoManager getInstance()
	{
		return INSTANCE;
	}

	public static Set<String> getAlgorithmSet(String typeRequested)
	{
		Set<String> algorithmSet			= new TreeSet<>();

		Provider[]			providerList	= Security.getProviders();
		for (Provider provider : providerList)
		{
			Set<Provider.Service> serviceSet		= provider.getServices();
			for (Provider.Service service : serviceSet)
			{
				String type		= service.getType();
				String algorithm	= service.getAlgorithm();
				if (typeRequested.equals(type))
				{
					algorithmSet.add(algorithm);
				}
			}
		}
		return algorithmSet;
	}

	private void loadAliasTable()
	{
		ConfigurationManager		config				= ConfigurationManager.getInstance();
		Set<String>					settingNameList		= config.getSettingNameSet(SETTING_PREFIX_ALIAS);
		for (String settingName : settingNameList)
		{
			String		alias			= settingName.replace(SETTING_PREFIX_ALIAS, "");
			String		name				= config.getSetting(settingName, null);

			if (null != name)
			{
				addAlias(alias, name);
			}
		}
	}

	// Only used at startup
	private void loadSecretProviderSet()
	{
		ConfigurationManager	config				= ConfigurationManager.getInstance();
		Set<String>				settingNameList		= config.getSettingNameSet(SETTING_PREFIX_SECRET);
		for (String settingName : settingNameList)
		{
			String		alias			= settingName.replace(SETTING_PREFIX_SECRET, "");
			String		settingValue	= config.getSetting(settingName, null);
			String[]	settingParts	= settingValue.split("\t");
			String		algorithm		= settingParts[0];
			String		cipherPassword	= settingParts[1];
			String		password		= decrypt(cipherPassword);
			ICrypto		crypto			= new CryptoSharedSecret(alias, algorithm, password);
			addProvider(crypto);
		}
	}

	public void addAlias(String alias, String name)
	{
		m_aliasAliasMap.put(alias.trim(), name.trim());
	}
	public void addProvider(ICrypto crypto)
	{
		m_aliasCryptoMap.put(crypto.getAliasName(), crypto);
	}

	public Set<String> getAliasList()
	{
		return m_aliasCryptoMap.keySet();
	}

	public String getAlgorithmName(String aliasName)
	{
		String			alias		= getRealAlias(aliasName);
		ICrypto			crypto		= m_aliasCryptoMap.get(alias);
		if (null == crypto)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_UNKNOWN_ALIAS, "Unknown alias (" + aliasName + ")");
		}
		return crypto.getAlgorithm();
	}

	public String encrypt(String aliasName, String clearText)
	{
		String			alias		= getRealAlias(aliasName);
		ICrypto			crypto		= m_aliasCryptoMap.get(alias);
		if (null == crypto)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_UNKNOWN_ALIAS, "Unknown alias (" + aliasName + ")");
		}
		String			cipherText	= crypto.encrypt(clearText);

		StringBuilder	sb			= new StringBuilder(cipherText.length() + alias.length() + 2);
		sb.append(crypto.getAliasName());
		sb.append(ALIAS_DELIMITER);
		sb.append(cipherText);

		return sb.toString();
	}

	public String decrypt(String cipherText)
	{
		String[]		parts		= cipherText.split(ALIAS_DELIMITER);
		String			alias		= parts[0];
		String			realCipher	= parts[1];
		ICrypto			crypto		= m_aliasCryptoMap.get(alias);
		if (null == crypto)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_UNKNOWN_ALIAS, "Unknown alias (" + alias + ")");
		}
		String clearText	= crypto.decrypt(realCipher);

		return clearText;
	}

	public String getRealAlias(String aliasName)
	{
		while (true)
		{
			String aliasTemp		= m_aliasAliasMap.get(aliasName);
			if (null == aliasTemp)
			{
				break;
			}
			else
			{
				aliasName = aliasTemp;
			}
		}

		return aliasName;
	}

	public static String formatAliasName(String text, long timeMS)
	{
		synchronized (SCRATCH_DATE)
		{
			SCRATCH_DATE.setTime(timeMS);
			return String.format(ALIAS_FORMAT, text, SCRATCH_DATE);
		}
	}

	public static void addLineToConfiguration(String line) throws BasicException
	{
		String				filename		= getWorkingFilename();
		String				settingName		= line.substring(0, line.indexOf('='));

		try (BasicFileWriter writer = new BasicFileWriter())
		{
			writer.appendFile(filename);
			writer.println(line);
			getLogger().info(BasicEvent.EVENT_KEYSTORE_SAVE_SUCCESS,
					"Added new setting to key file %s: %s",
					filename, settingName);
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_KEYSTORE_SAVE_FAILED,
					exception, "Failed to save configuration to file " + filename);
		}
	}
}
