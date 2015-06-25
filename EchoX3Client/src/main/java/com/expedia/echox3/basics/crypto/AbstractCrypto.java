/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

import java.security.Key;

import javax.crypto.Cipher;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.monitoring.event.BasicRuntimeException;
import com.expedia.echox3.basics.tools.string.BasicBase64;

public class AbstractCrypto implements ICrypto
{
	private static final BasicLogger LOGGER						= new BasicLogger(CryptoSharedSecret.class);

	private String		m_aliasName;
	private String		m_algorithm;
	private boolean		m_isPublic;

	private Cipher		m_encryptionCipher;	// Final, but set in setCipher, called by the ctor of the extending class.
	private Cipher		m_decryptionCipher;	// Final, but set in setCipher, called by the ctor of the extending class.
	private Key			m_encryptionKey;
	private Key			m_decryptionKey;

	protected AbstractCrypto(String aliasName, String algorithm, boolean isPublic)
	{
		m_aliasName = aliasName;
		m_algorithm = algorithm;
		m_isPublic = isPublic;
	}

	protected void setCipher(Cipher encryptionCipher, Key encryptionKey, Cipher decryptionCipher, Key decryptionKey)
	{
		m_encryptionCipher		= encryptionCipher;
		m_encryptionKey			= encryptionKey;
		m_decryptionCipher		= decryptionCipher;
		m_decryptionKey			= decryptionKey;
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	@Override
	public boolean isPublic()
	{
		return m_isPublic;
	}

	@Override
	public String encrypt(String str)
	{
		// Encode the string into bytes using utf-8
		byte[] utf8;
		try
		{
			utf8 = str.getBytes("UTF8");
		}
		catch (Exception e)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_UTF8,
					e, "Failed getting the UTF8 bytes from (" + str + ")");
		}

		// Encrypt
		byte[] enc;
		try
		{
			//noinspection SynchronizeOnNonFinalField
			synchronized (m_encryptionCipher)
			{
				m_encryptionCipher.init(Cipher.ENCRYPT_MODE, m_encryptionKey);
				enc = m_encryptionCipher.doFinal(utf8);
			}
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_ENCRYPT_FAILED,
					exception, "Failed final encryption step for (" + str + ")");
		}

		// Encode bytes to base64 to get a string
		String cipher;
		try
		{
			cipher = BasicBase64.encode(enc);
			cipher = cipher.replaceAll("\r", "").replaceAll("\n", "");
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_BASE64,
					exception, "Failed final to BasicBase64.encode the cipher for (" + str + ")");
		}

		return cipher;
	}

	@Override
	public String decrypt(String cipherText)
	{
		byte[]		dec;
		byte[]		clearBytes;
		String clearText;

		// Decode base64 to get bytes
		try
		{
			dec = BasicBase64.decode(cipherText);
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_BASE64,
					exception, "BasicBase64.decode the cipher (" + cipherText + ")");
		}

		// Decrypt
		try
		{
			//noinspection SynchronizeOnNonFinalField
			synchronized (m_decryptionCipher)
			{
				m_decryptionCipher.init(Cipher.DECRYPT_MODE, m_decryptionKey);
				clearBytes = m_decryptionCipher.doFinal(dec);
			}
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_CRYPTO_PASSWORD_DECRYPT_FAILED,
					exception, "Failed final decrypt cipher (" + cipherText + ")");
		}

		// Decode using utf-8
		try
		{
			clearText = new String(clearBytes, "UTF8");
		}
		catch (Exception exception)
		{
			throw new BasicRuntimeException(BasicEvent.EVENT_UTF8,
					exception, "Failed extracting the UTF8 bytes from cipher (" + cipherText + ")");
		}

		return clearText;
	}

	@Override
	public String getAlgorithm()
	{
		return m_algorithm;
	}

	@Override
	public String getAliasName()
	{
		return m_aliasName;
	}

	@Override
	public String toString()
	{
		return String.format("%s (%s)", getAliasName(), getAlgorithm());
	}
}
