/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.string;

import java.io.IOException;
import java.util.Base64;


/**
 * @author Pierre
 * 
 * Stringizes byte arrays using the BASE64 algorithm
 *
 */
public abstract class BasicBase64
{
	static private Base64.Encoder		s_encoder	= Base64.getEncoder();
	static private Base64.Decoder		s_decoder	= Base64.getDecoder();

	/**
	 * Decodes the string into the original bytes
	 * 
	 * @param str BasicBase64 encoded string to decode in to bytes.
	 * @return		The original byte[]
	 * @throws IOException  If something goes wrong with the decode.
	 */
	public static byte[] decode(String str) throws IOException
	{
		return s_decoder.decode(str);
	}

	/**
	 * Perform the "correct" Base 64 encoding
	 * 
	 * @param byteList  Bytes to encode into a string.
	 * @return	a BasicBase64 string
	 */
	public static String encode(byte[] byteList)
	{
		return s_encoder.encodeToString(byteList);
	}
}
