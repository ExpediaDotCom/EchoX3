/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.crypto;

/**
 * Created with IntelliJ IDEA.
 * User: Pierre
 * Date: 9/13/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ICrypto
{
	/**
	 *
	 * @return	true for Public/Private key and false for password style encryption
	 */
	boolean isPublic();

	/**
	 * @param clearText		Clear text
	 * @return				Stringized (BasicBase64) version of the cipher for the clear text
	 */
	String encrypt(String clearText);

	/**
	 * @param cipherText	Clear text
	 * @return				Clear text from the stringized (BasicBase64) version of the cipherTetxt
	 */
	String decrypt(String cipherText);

	/**
	 * @return	The cryptography algorithm
	 */
	String getAlgorithm();

	/**
	 *
	 * @return	The name by which this combination algorithm/key are known.
	 */
	String getAliasName();
}
