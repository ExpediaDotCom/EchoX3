/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.request;

public class MessageType
{
	public static final int					MESSAGE_COUNT_MAX		= 200;
	private static final MessageType[]		MESSAGE_TYPE_LIST		= new MessageType[MESSAGE_COUNT_MAX];

	// NOTE: All message type numbers are defined in the appropriate *ClientMessageHandler
	//   1 -   9	Special responses
	//  10 -  19	Common requests, answered by first server receiving the message.
	//  20 -  59	Dispatcher admin
	//  60 -  99	Store admin
	// 100 - 149	Dispatcher Client (1st hop)
	// 150 = 199	Store customer (2nd hop)	== Client (1st hop) + 150

	// 0 - 5	Status codes
	//CHECKSTYLE:OFF
	public static final MessageType Success					= new MessageType(  0, "Success");
	public static final MessageType Failure					= new MessageType(  1, "Failure");
	public static final MessageType UnknownMessageType		= new MessageType(  2, "UnknownMessageType");

	// 10 - 19 == Direct action messages
	public static final MessageType GetTime					= new MessageType( 11, "GetTime");
	public static final MessageType Echo					= new MessageType( 12, "Echo");
	public static final MessageType Work					= new MessageType( 13, "Work");
	public static final MessageType GetVersion				= new MessageType( 14, "GetVersion");

	public static final MessageType GetHighwayNumber		= new MessageType( 18, "GetHighway");
	public static final MessageType SetHighwayNumber		= new MessageType( 19, "SetHighway");
	//CHECKSTYLE:ON


	private int			m_number;
	private String		m_name;

	public MessageType(int number, String name)
	{
		m_number = number;
		m_name = name;

		MESSAGE_TYPE_LIST[number] = this;
	}

	public static MessageType getMessageType(int number)
	{
		MessageType		messageType		= null;
		if (0 >= number && MESSAGE_COUNT_MAX > number)
		{
			messageType = MESSAGE_TYPE_LIST[number];
		}
		if (null == messageType)
		{
			messageType = new MessageType(number, UnknownMessageType.getName());
		}
		return messageType;
	}

	public int getNumber()
	{
		return m_number;
	}

	public String getName()
	{
		return m_name;
	}

	@Override
	public String toString()
	{
		return String.format("(%d, %s)", getNumber(), getName());
	}
}
