/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.protocol;


import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;

public class SocketCounterFamily
{
	private IOperationCounter		m_readMessageCounter;

	private IOperationCounter		m_writeQueueCounter;
	private IOperationCounter		m_writeSingleCounter;
	private IOperationCounter		m_writeTotalCounter;

	public SocketCounterFamily(String protocolName)
	{
		CounterFactory		factory				= CounterFactory.getInstance();
		StringGroup			counterNameList		= new StringGroup(AbstractProtocolHandler.class.getSimpleName());
		counterNameList.append(protocolName);

		StringGroup			readName			= new StringGroup(counterNameList.getStringArray());
		readName.append("ReadMessage");
		m_readMessageCounter = factory.getLogarithmicOperationCounter(readName,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time spent reading a message");


		StringGroup			queueName			= new StringGroup(counterNameList.getStringArray());
		queueName.append("WriteQueue");
		m_writeQueueCounter = factory.getLogarithmicOperationCounter(queueName,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time a message spends in a queue, waiting to be written out");

		StringGroup			singleName			= new StringGroup(counterNameList.getStringArray());
		singleName.append("WriteSingle");
		m_writeSingleCounter = factory.getLogarithmicOperationCounter(singleName,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Time to perform a single socket write, which may  or may not include the entire message");

		StringGroup			totalName			= new StringGroup(counterNameList.getStringArray());
		totalName.append("WriteTotal");
		m_writeTotalCounter = factory.getLogarithmicOperationCounter(totalName,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Total time required to write out the message");
	}

	public IOperationCounter getReadMessageCounter()
	{
		return m_readMessageCounter;
	}

	public IOperationCounter getWriteQueueCounter()
	{
		return m_writeQueueCounter;
	}

	public IOperationCounter getWriteSingleCounter()
	{
		return m_writeSingleCounter;
	}

	public IOperationCounter getWriteTotalCounter()
	{
		return m_writeTotalCounter;
	}

	public void close()
	{
		m_readMessageCounter.close();
		m_writeQueueCounter.close();
		m_writeSingleCounter.close();
		m_writeTotalCounter.close();
	}
}
