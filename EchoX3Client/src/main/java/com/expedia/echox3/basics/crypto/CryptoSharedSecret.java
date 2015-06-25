/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.string.BasicBase64;
import com.expedia.echox3.basics.tools.time.WallClock;


/**
 * @author Pierre
 *
 */
public class CryptoSharedSecret extends AbstractCrypto
{
	private static final String			BIGBANG_ALIAS_TEXT			= "BigBang";
	private static final long			BIGBANG_ALIAS_TIME			= getBigBangTime();
	private static final String			BIGBANG_ALIAS				=
											CryptoManager.formatAliasName(BIGBANG_ALIAS_TEXT, BIGBANG_ALIAS_TIME);

	private static final String			SHARED_SECRET_ALIAS_TEXT	= "Secret";
	private static final String			SETTING_PREFIX				= CryptoSharedSecret.class.getName();
	private static final String			SETTING_ALGORITHM			= SETTING_PREFIX + ".algorithm";
	private static final String			SETTING_CCH					= SETTING_PREFIX + ".cch";

	private static final String			BIGBANG_ALGORITHM				= "AES";
																		// 123456789012345678901234
	private static final long			BIGBANG_KEY						= 8760699040211348027L;
	private static final int			BIGBANG_PASSWORD_CCH			= 24;

	private final SecretKey				m_secretKey;

	/**
	 * Returns the bootstrap ICrypto
	 */
	public CryptoSharedSecret()
	{
		super(BIGBANG_ALIAS, BIGBANG_ALGORITHM, false);

		m_secretKey = create(BIGBANG_ALGORITHM, BIGBANG_KEY, BIGBANG_PASSWORD_CCH);
	}
	public CryptoSharedSecret(String aliasName, String algorithm, int cch)
	{
		super(aliasName, algorithm, false);

		m_secretKey = create(algorithm, 0, cch);
	}

	public CryptoSharedSecret(String aliasName, String algorithm, String keyText)
	{
		super(aliasName, algorithm, false);

		init(keyText);
		m_secretKey = null;			// Not needed, as it is read and given to the en/decryption Ciphers.
	}

	private static long getBigBangTime()
	{
		Calendar calendar		= Calendar.getInstance();
		calendar.setTimeInMillis(0);
		calendar.set(2015, Calendar.JUNE, 1, 8, 59, 59);

		return calendar.getTimeInMillis();
	}

	private void init(String keyText)
	{
//		byte[]					passwordBytes	= password.getBytes();
//		char[]					passwordChars	= password.toCharArray();

		SecretKey				key				= null;
		try
		{
			byte[]			keybytes	= BasicBase64.decode(keyText);
			key			= (SecretKey) BasicSerial.toObject("Crypto", keybytes);
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_INIT_FAILED,
					exception, String.format("Failed to obtain the Key for alias %s.",
					getAliasName()));
		}

		Cipher encryptionCipher;
		Cipher decryptionCipher;
		try
		{
			encryptionCipher = Cipher.getInstance(key.getAlgorithm());
			decryptionCipher = Cipher.getInstance(key.getAlgorithm());
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_INIT_FAILED,
					exception, String.format("Failed to obtain a Cipher instance with (%s)", getAliasName()));
		}

		setCipher(encryptionCipher, key, decryptionCipher, key);
	}
	private SecretKey create(String algorithm, long seed, int cch)
	{
		SecretKeyFactory		keyFactory		= null;
		try
		{
			keyFactory		= SecretKeyFactory.getInstance(algorithm);
		}
		catch (Exception exception)
		{
			// Fall through and try a KeyGenerator
		}

		SecretKey key;
		if (null != keyFactory)
		{
			try
			{
				String			password		= PasswordGenerator.getPassword(seed, cch);
				byte[]			passwordBytes	= password.getBytes();
				KeySpec			keySpec			= new SecretKeySpec(passwordBytes, algorithm);
				key = keyFactory.generateSecret(keySpec);
			}
			catch (Exception exception)
			{
				throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_INIT_FAILED,
						exception, String.format("Failed to obtain a Key instance for algorithm (%s)", algorithm));
			}
		}
		else
		{
			try
			{
				// TODO Does not use the password; Current implementation cannot decrypt!
				// Need to serialize the key.
				KeyGenerator		generator			= KeyGenerator.getInstance(algorithm);
				if (0 != seed)
				{
					SecureRandom random = RandomManager.getSecureRandom();
					random.setSeed(seed);
					generator.init(random);
				}
				key = generator.generateKey();
			}
			catch (Exception exception)
			{
				throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_INIT_FAILED,
						exception, String.format(
						"Failed to obtain a SecretKeyFactory or KeyGenerator instance for algorithm (%s)", algorithm));
			}
		}

		Cipher encryptionCipher;
		Cipher decryptionCipher;
		try
		{
			encryptionCipher = Cipher.getInstance(key.getAlgorithm());
			decryptionCipher = Cipher.getInstance(key.getAlgorithm());
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_INIT_FAILED,
					exception, String.format("Failed to obtain a Cipher instance with (%s)", getAliasName()));
		}

		setCipher(encryptionCipher, key, decryptionCipher, key);

		return key;
	}

	public static String makeKey(String algorithm, int cch) throws BasicException
	{
		ConfigurationManager	config			= ConfigurationManager.getInstance();
		CryptoManager			manager			= CryptoManager.getInstance();
		String					bigBang			= CryptoManager.ALGORITHM_BIG_BANG;

		String		alias	= CryptoManager.formatAliasName(SHARED_SECRET_ALIAS_TEXT, WallClock.getCurrentTimeMS());

		if (null == algorithm)
		{
			algorithm		= config.getSetting(SETTING_ALGORITHM, BIGBANG_ALGORITHM);
			cch				= config.getInt(SETTING_CCH, Integer.toString(BIGBANG_PASSWORD_CCH));
		}

		// Ensure these parameters can create a Crypto ...
		CryptoSharedSecret					crypto			= new CryptoSharedSecret(alias, algorithm, cch);
		byte[]								keyBytes		= BasicSerial.toBytes("Crypto", crypto.m_secretKey);
		String								keyClearText	= BasicBase64.encode(keyBytes);
		String								keyCipherText	= manager.encrypt(bigBang, keyClearText);

		String					line			= String.format(CryptoManager.BOOTSTRAP_SETTING_FORMAT,
				CryptoManager.SETTING_PREFIX_SECRET, alias, algorithm, keyCipherText);
		try
		{
			CryptoManager.addLineToConfiguration(line);
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_KEYSTORE_SAVE_FAILED,
					exception, String.format(
							"Failed to save new SharedSecret crypto using algorithm %s as %s to configuration",
							algorithm, alias)
			);
		}

		// Make the crypto active immediately.
		CryptoManager.getInstance().addProvider(crypto);

		return alias;
	}
}
