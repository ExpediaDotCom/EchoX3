/**
 * Copyright 2012-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.time;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SntpClient implements SntpReader.ISntpReaderListener
{
	private static final int	DEFAULT_PORT		= 123;
	private static final int	TIMEOUT				= 3 * 1000;			// On the local network, this is plenty of time.
	private final InetAddress	m_address;
	private SntpMessage			m_response			= null;
	private Throwable			m_throwable			= null;

	public SntpClient(String host) throws UnknownHostException
	{
		m_address = InetAddress.getByName(host);
	}

	public void measure() throws IOException
	{
		SntpReader			reader		= new SntpReader(this);
		reader.startListening(TIMEOUT);

		SntpMessage			request		= new SntpMessage();
		send(reader.getSocket(), request, m_address, DEFAULT_PORT);

		synchronized (reader)
		{
			try
			{
				reader.wait(TIMEOUT + 1000);
			}
			catch (InterruptedException e)
			{
				// Timeout
				m_throwable = e;
			}
			finally
			{
				reader.stopListening();
			}
		}
	}

	public SntpMessage getResponse()
	{
		return m_response;
	}

	public Throwable getThrowable()
	{
		return m_throwable;
	}

	private void send(DatagramSocket socket, SntpMessage message, InetAddress address, int port)
			throws IOException
	{
		ByteArrayOutputStream output		= new ByteArrayOutputStream();
		Codec.encodeMessage(message, output);

		byte[]						data		= output.toByteArray();
		output.close();

		DatagramPacket packet		= new DatagramPacket(data, data.length, address, port);
		socket.send(packet);
	}

	@Override
	public void onSuccess(SntpReader reader, DatagramPacket packet, long time)
	{
		ByteArrayInputStream stream		= new ByteArrayInputStream(packet.getData());
		try
		{
			m_response = Codec.decodeMessage(stream);
			m_response.setLocalReceiveTime(new SntpMessage.SntpTime(time));
		}
		catch (IOException e)
		{
			m_throwable = e;
		}
		synchronized (reader)
		{
			reader.notify();
		}
	}

	@Override
	public void onFailure(SntpReader reader, Throwable throwable)
	{
		m_throwable = throwable;
		synchronized (reader)
		{
			reader.notify();
		}
	}
}
