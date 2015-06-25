/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractBaseThread;
import com.expedia.echox3.internal.transport.protocol.AbstractProtocolHandler;

@SuppressWarnings("PMD.CyclomaticComplexity")
public class SelectorThread extends AbstractBaseThread
{
	private static final BasicLogger	LOGGER				= new BasicLogger(SelectorThread.class);
	private static final int			SELECTOR_PRIORITY	= Math.min(Thread.NORM_PRIORITY + 2, Thread.MAX_PRIORITY);

	private static final Map<Integer, String>		KEY_OPS_NAME_MAP		= new TreeMap<>();

	private final Selector					m_selector;
	private final AbstractProtocolHandler	m_handler;

	private final List<Runnable>			m_taskList		= new LinkedList<>();

	static
	{
		KEY_OPS_NAME_MAP.put(SelectionKey.OP_ACCEPT,	"Accept");
		KEY_OPS_NAME_MAP.put(SelectionKey.OP_CONNECT,	"Connect");
		KEY_OPS_NAME_MAP.put(SelectionKey.OP_READ,		"Read");
		KEY_OPS_NAME_MAP.put(SelectionKey.OP_WRITE,		"Write");
	}

	public SelectorThread(AbstractProtocolHandler handler, int index) throws BasicException
	{
		try
		{
			m_selector = Selector.open();
		}
		catch (IOException e)
		{
			String		reason		= "Failed to create a selector for " + handler.getName();
			throw new BasicException(BasicEvent.EVENT_SELECTOR_OPEN_FAILED, reason, e);
		}

		m_handler = handler;
		setName("Selector: " + handler.getProtocolName() + "-" + index);
		setPriority(SELECTOR_PRIORITY);
		setDaemon(true);
		start();
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public Selector getSelector()
	{
		return m_selector;
	}

	public AbstractProtocolHandler getHandler()
	{
		return m_handler;
	}

	public void addTask(Runnable runnable)
	{
		synchronized (m_taskList)
		{
			m_taskList.add(runnable);
		}
	}

	@Override
	public void run()
	{
		setThreadStatus(ThreadStatus.Running);

		int			timeoutMS		= 10;
		while (!ThreadStatus.ExitRequested.equals(getThreadStatus()))
		{
			int		count		= 0;
			try
			{
				// select locks the queue (for write). Don't lock it too long.
				// The timeout is required for connect on the client side which does not seem to cause a wakeup
				// by itself. Without the timeout, the (first) connect hands.
				count = m_selector.select(timeoutMS);
			}
			catch (IOException e)
			{
// Fall through and see if any key is available.
//				MultiUtil.sleep(1);
//				continue;
			}

			if (0 != count)
			{
				Set<SelectionKey>			set		= m_selector.selectedKeys();
				for (SelectionKey key : set)
				{
					processSelectionKey(key);
				}
				set.clear();
			}


			synchronized (m_taskList)
			{
				// Don't create an iterator when the list is empty
				if (!m_taskList.isEmpty())
				{
					for (Runnable runnable : m_taskList)
					{
						runnable.run();
					}
					m_taskList.clear();
				}
			}
		}

		setThreadStatus(ThreadStatus.Terminated);
	}

	private void processSelectionKey(SelectionKey key)		// NOPMD CyclomaticComplexity - As simple as it gets.
	{
		SelectableChannel		socketChannel	= key.channel();
		Object					attachment		= key.attachment();

		try
		{
			if (key.isAcceptable())
			{
				if (getLogger().isDebugEnabled())
				{
					getLogger().debug(BasicEvent.EVENT_SELECTOR_EVENT_ACCEPTABLE,
							"isAcceptable(%s, %s)", socketChannel.toString(), attachment.toString());
				}

				// getHandler().getServerChannel() == channelAccepting
				getHandler().processIsAcceptable();
			}
			else if (key.isConnectable())
			{
				if (getLogger().isDebugEnabled())
				{
					getLogger().debug(BasicEvent.EVENT_SELECTOR_EVENT_CONNECTABLE,
							"isConnectable(%s, %s)", socketChannel.toString(), attachment.toString());
				}

				getHandler().processIsConnectable(socketChannel, attachment);
			}
			else if (key.isWritable())
			{
				if (getLogger().isDebugEnabled())
				{
					getLogger().debug(BasicEvent.EVENT_SELECTOR_EVENT_WRITABLE,
							"isWritable(%s, %s)", socketChannel.toString(), attachment.toString());
				}

				// The attachment is a AbstractTransportLane
				if (attachment instanceof AbstractTransportLane)
				{
					AbstractTransportLane		transportLane		= (AbstractTransportLane) attachment;
					getHandler().processIsWritable(transportLane);
				}
			}
			else if (key.isReadable())
			{
				if (getLogger().isDebugEnabled())
				{
					getLogger().debug(BasicEvent.EVENT_SELECTOR_EVENT_READABLE,
							"isReadable(%s, %s)", socketChannel.toString(), attachment.toString());
				}

				// The attachment is a AbstractTransportLane
				if (attachment instanceof AbstractTransportLane)
				{
					AbstractTransportLane		transportLane		= (AbstractTransportLane) attachment;
					getHandler().processIsReadable(transportLane);
				}
			}
		}
		catch (Exception exception)
		{
			String		message		= String.format(
					"Unexpected Throwable processing key OPS(%s) on SocketChannel %s with attachment %s",
					getKeyOpName(key.readyOps()),
					null == socketChannel ? "null" : socketChannel.toString(),
					null == attachment ? "null" : attachment.toString());
			getLogger().warn(BasicEvent.EVENT_SELECTOR_EVENT_EXCEPTION, exception, message);

			// Play it safe and close the TransportLane (if socket)
			if (attachment instanceof AbstractTransportLane)
			{
				AbstractTransportLane 		transportLane		= (AbstractTransportLane) attachment;
				transportLane.close(new BasicException(BasicEvent.EVENT_SELECTOR_EVENT_EXCEPTION, exception, message),
						"Unexpected exception places the connection in an unknown state.");
			}
			else
			{
				BaseFileHandler.closeSafe(socketChannel);
			}
		}
	}

	private static String getKeyOpName(int ops)
	{
		StringBuilder		sb			= new StringBuilder(100);
		String				prefix		= "";

		for (Map.Entry<Integer, String> entry : KEY_OPS_NAME_MAP.entrySet())
		{
			int			opCode		= entry.getKey();
			if (0 != (opCode & ops))
			{
				sb.append(prefix);
				sb.append(entry.getValue());
				prefix = ", ";
			}
		}

		return sb.toString();
	}
}
