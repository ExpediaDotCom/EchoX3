/**
 * Copyright 2012-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.time;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Codec
{

	// Instance methods. **************************************************************************

	/**
	 * Encodes an SNTP message to a byte stream.
	 *
	 * @param message the message.
	 * @param output the byte stream.
	 */
	public static void encodeMessage(final SntpMessage message, final OutputStream output)
			throws IOException
	{
		byte flags=(byte)(message.getLeapIndicator()<<6);
		flags+=(byte)(message.getVersionNumber()<<3);
		flags+=message.getMode();
		output.write(flags);
		output.write(message.getStratum());
		output.write(message.getPollInterval());
		output.write(message.getPrecision());
		encodeFixedPoint(message.getRootDelay(), output);
		encodeFixedPoint(message.getRootDispersion(), output);
		encodeBitString(message.getReferenceIdentifier(), output);
		encodeTimestamp(message.getReferenceTime(), output);
		encodeTimestamp(message.getLocalTransmitTime(), output);
		encodeTimestamp(message.getRemoteReceiveTime(), output);
		encodeTimestamp(message.getRemoteTransmitTime(), output);
	}


	/**
	 * Decodes an SNTP message from a byte stream.
	 *
	 * @param input the byte stream.
	 * @return the message.
	 */
	public static SntpMessage decodeMessage(final InputStream input) throws IOException
	{
		final SntpMessage message=new SntpMessage();
		final byte flags=(byte)input.read();
		message.setLeapIndicator((byte)(flags>>6));
		message.setVersionNumber((byte)((flags>>3) & 0x07));
		message.setMode((byte)(flags & 0x07));
		message.setStratum((byte)input.read());
		message.setPollInterval((byte)input.read());
		message.setPrecision((byte)input.read());
		message.setRootDelay(decodeFixedPoint(input));
		message.setRootDispersion(decodeFixedPoint(input));
		message.setReferenceIdentifier(decodeBitString(input));
		message.setReferenceTime(decodeTimestamp(input));
		message.setLocalTransmitTime(decodeTimestamp(input));
		message.setRemoteReceiveTime(decodeTimestamp(input));
		message.setRemoteTransmitTime(decodeTimestamp(input));

		return message;
	}


	// Helper methods. ****************************************************************************

	/**
	 * Encodes a 32 bit number to a byte stream.
	 *
	 * @param number the number to encode.
	 * @param output the byte stream.
	 * @throws java.io.IOException if an error occurs while writting to the stream.
	 */
	protected static void encode32(final long number, final OutputStream output) throws IOException
	{
		for (int i=3; i>=0; i--)
		{
			output.write((int)((number>>(8*i))&0xFF));
		}
	}


	/**
	 * Decodes a 32 bit number from a byte stream.
	 *
	 * @param input the byte stream.
	 * @return the decoded number.
	 * @throws java.io.IOException if an error occurs while reading from the stream.
	 */
	protected static long decode32(final InputStream input) throws IOException
	{
		long number=0;
		for (int i=0; i<4; i++)
		{
			number=(number<<8)+input.read();
		}

		return number;
	}


	/**
	 * Encodes a 32-bit bitstring to a byte stream.
	 *
	 * @param bitstring the bitstring to encode.
	 * @param output the byte stream.
	 * @throws java.io.IOException if an error occurs while writting to the stream.
	 */
	protected static void encodeBitString(final byte[] bitstring, final OutputStream output)
			throws IOException
	{
		final byte[] temp={0, 0, 0, 0};
		System.arraycopy(bitstring, 0, temp, 0, bitstring.length);
		output.write(temp);
	}


	/**
	 * Decodes a 32-bit bitstring from a byte stream.
	 *
	 * @param input the byte stream.
	 * @return the decoded string.
	 * @throws java.io.IOException if an error occurs while reading from the stream.
	 */
	protected static byte[] decodeBitString(final InputStream input) throws IOException
	{
		final byte[] bitstring=new byte[4];
		input.read(bitstring, 0, 4);

		return bitstring;
	}


	/**
	 * Encodes a 32 bit fixed-point number to a byte stream.
	 *
	 * @param number the fixed-point number to encode.
	 * @param output the byte stream.
	 * @throws java.io.IOException if an error occurs while writting to the stream.
	 */
	protected static void encodeFixedPoint(final double number, final OutputStream output)
			throws IOException
	{
		encode32((long)(number*0x10000L), output);
	}


	/**
	 * Decodes a 32 bit fixed-point number from a byte stream.
	 * The binary point is between bits 15 and 16.
	 *
	 * @param input the byte stream.
	 * @return the decoded fixed-point number.
	 * @throws java.io.IOException if an error occurs while reading from the stream.
	 */
	protected static double decodeFixedPoint(final InputStream input) throws IOException
	{
		return ((double)decode32(input))/0x10000L;
	}


	protected static void encodeTimestamp(SntpMessage.SntpTime time, final OutputStream output)
			throws IOException
	{
		encode32(time.getInteger(), output);
		encode32(time.getFraction(), output);
	}


	protected static SntpMessage.SntpTime decodeTimestamp(final InputStream input) throws IOException
	{
		final long integer=decode32(input);
		final long fraction=decode32(input);

		return new SntpMessage.SntpTime(integer, fraction);
	}
}
