/**
 * Copyright 2012-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.time;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SntpReader extends Thread
{
	private DatagramSocket m_socket		= null;
	private volatile boolean		m_isListening	= false;
	private ISntpReaderListener		m_listener		= null;

	public SntpReader(ISntpReaderListener listener) throws SocketException
	{
		m_socket = new DatagramSocket();
		m_listener = listener;
	}

	public DatagramSocket getSocket()
	{
		return m_socket;
	}

	public void startListening(int timeout) throws SocketException
	{
		m_socket.setSoTimeout(timeout);
		setName(SntpReader.class.getSimpleName());
		setDaemon(true);
		start();

		m_isListening = true;
	}

	public void stopListening()
	{
		m_isListening = false;
	}

	@Override
	public void run()
	{
		while (m_isListening)
		{
			try
			{
				int 				cb			= SntpMessage.MAXIMUM_LENGTH;
				DatagramPacket packet		= new DatagramPacket(new byte[cb], cb);
				m_socket.receive(packet);
				long				time		= System.currentTimeMillis();
				m_listener.onSuccess(this, packet, time);
			}
			catch (Throwable throwable)
			{
				if (m_isListening)
				{
					m_listener.onFailure(this, throwable);
				}
			}
		}
		m_socket.close();
	}

	public interface ISntpReaderListener
	{
		void onSuccess(SntpReader reader, DatagramPacket packet, long time);
		void onFailure(SntpReader reader, Throwable throwable);
	}
}
