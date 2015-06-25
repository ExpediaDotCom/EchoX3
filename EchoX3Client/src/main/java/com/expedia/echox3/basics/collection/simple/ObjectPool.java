/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.AbstractScheduledThread;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;

/**
 * This class serves double duty as manager of a pool of blank PendingRequests
 * AND manager of outstanding PendingRequests.
 *
 * The key here is to have no allocation in the normal case.
 * The "abnormal" case is if the pending list has to grow.
 *
 * Usage:
 * 		Object		pooledObject		= pool.get();
 * 		try
 * 		{
 * 			// do stuff
 * 		}
 * 		finally
 * 		{
 * 			pooledObject.release();
 * 		}
 *
 * 		pool.release();		// destroys the pool and the objects it contains
 *
 * The pool will grow automatically as needed from the initial size (it never shrinks).
 *
 * NOTE:		The pooled object MUST extend AbstractPooledObject, as this class maintains the IOperationContext.
 */
@SuppressWarnings("unchecked")
public class ObjectPool<T extends ObjectPool.AbstractPooledObject> implements ObjectPoolMBean
{
	public static final String SETTING_PREFIX					= ObjectPool.class.getName() + ".%s";
	public static final String SETTING_INITIAL_SIZE				= SETTING_PREFIX + ".initialSize";
	public static final String SETTING_MAX_SIZE					= SETTING_PREFIX + ".maxSize";

	// OBJECT_POOL_LIST maintained to walk the pools when looking for excessive number of objects in the list.
	// and reset the list size when m_outstandingCount is too high.
	private static final List<ObjectPool>		OBJECT_POOL_LIST		= new LinkedList<>();

	private static final int					DEFAULT_INITIAL_SIZE			= 10;
	private static final ConfigurationManager	CONFIGURATION_MANAGER			= ConfigurationManager.getInstance();
	private static final BasicLogger			LOGGER							= new BasicLogger(ObjectPool.class);

	public abstract static class AbstractPooledObject
	{
		private ObjectPool<? super AbstractPooledObject>		m_objectPool;

		// sic package level access
		void setObjectPool(@SuppressWarnings("rawtypes") ObjectPool objectPool)
		{
			m_objectPool = objectPool;
		}

		public void release()
		{
			// Allow null m_objectPool to allow creation of object outside of the pool.
			// For example, such objects may be used for testing.
			if (null != m_objectPool)
			{
				m_objectPool.release(this);
			}
		}
	}

	private final String				m_name;
//	private Object[]					m_constructorArgs;
//	private Constructor<T>				m_constructor;
	private Supplier<T>					m_supplier;

	private StringGroup						m_mbeanNameList;
	private int								m_maxSize;
	private T[]								m_blankList;
	private int								m_initialSize;
	private int								m_blankCount;
	private int								m_waitingCount;
	private int								m_outstandingCount;
	private int								m_outstandingCountMax		= 0;
	private long							m_notifyCount				= 0;

	static
	{
		new ObjectPoolCleanupThread();
	}

	public ObjectPool(StringGroup name, Supplier<T> supplier)
	{
		m_name						= name.getString(".");

		m_initialSize				= CONFIGURATION_MANAGER.getInt(
				getSettingName(SETTING_INITIAL_SIZE), Integer.toString(DEFAULT_INITIAL_SIZE));
		m_initialSize				= Math.max(DEFAULT_INITIAL_SIZE, m_initialSize);
		m_blankCount				= m_initialSize;
		m_maxSize					= CONFIGURATION_MANAGER.getInt(
				getSettingName(SETTING_MAX_SIZE), Integer.toString(m_blankCount * 10));
		m_blankList					= (T[]) new AbstractPooledObject[m_blankCount];
		m_outstandingCount			= 0;

		m_supplier = supplier;
		for (int i = 0; i < m_blankCount; i++)
		{
			m_blankList[i] = m_supplier.get();
			m_blankList[i].setObjectPool(this);
		}

		m_mbeanNameList = new StringGroup(name.toString());
		m_mbeanNameList.prepend(ObjectPool.class.getSimpleName());
		BasicTools.registerMBean(this, null, m_mbeanNameList);
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);

		synchronized (OBJECT_POOL_LIST)
		{
			OBJECT_POOL_LIST.add(this);
		}
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void release()
	{
		if (null != m_mbeanNameList)
		{
			try
			{
				BasicTools.unregisterMBean(null, m_mbeanNameList);
			}
			catch (Throwable throwable)
			{
				// Ignore exceptions.
			}
			m_mbeanNameList = null;
		}

		synchronized (OBJECT_POOL_LIST)
		{
			OBJECT_POOL_LIST.remove(this);
		}
	}

	// Methods for the MBean///
	@Override
	public int getOutstandingCount()
	{
		return m_outstandingCount;
	}

	@Override
	public int getBlankCount()
	{
		return m_blankCount;
	}

	@Override
	public int getAllocatedCount()
	{
		return m_blankList.length;
	}

	@Override
	public int getWaitingCount()
	{
		return m_waitingCount;
	}

	@Override
	public int getMaxCount()
	{
		return m_maxSize;
	}

	@Override
	public long getNotifyCount()
	{
		return m_notifyCount;
	}

	@Override
	public void reset()
	{
		synchronized (this)
		{
			int			maxSizeSav		= m_maxSize;
			setMaxSize(m_outstandingCount);
			setMaxSize(maxSizeSav);
		}
	}

	public T get()
	{
		T		object;

		synchronized (this)
		{
			if (0 == m_blankCount)
			{
				grow(2 * getAllocatedCount());
			}

			while (0 == m_blankCount)
			{
				try
				{
					m_waitingCount++;
					//noinspection WaitOrAwaitWithoutTimeout
					this.wait();
					m_waitingCount--;
				}
				catch (InterruptedException e)
				{
					// Fall through the loop and see if a blank is available.
				}
			}

			--m_blankCount;
			object = m_blankList[m_blankCount];
			m_blankList[m_blankCount] = null;		// Keep things clean, redundant, at negligible cost.
			m_outstandingCount++;
			if (m_outstandingCount > m_outstandingCountMax)
			{
				m_outstandingCountMax = m_outstandingCount;
			}
		}

		return object;
	}

	public void setMaxSize(int maxSize)
	{
		synchronized (this)
		{
			// maxSize >= m_maxSize: Just set m_maxSize to maxSize; m_blankList will grow as needed
			// maxSize <  m_maxSize: Shrink m_blankList; m_blankCount will be negative if m_outstandingCount > maxSize
			//                       and will increment as objects are returned to the pool, in which case returned
			//                       objects will be put in m_blankList only after m_blankCount reaches 0. If
			//                       m_outstandingCount < maxSize then the entire blank list will be discarded,
			//                       reallocated, and left empty with a a size of m_outstandingCount; doing so creates
			//                       garbage but is much than salvaging the already-constructed objects in m_blankList.
			int			maxSizePrev		= m_maxSize;
			if(maxSize < m_maxSize)
			{
				if(m_outstandingCount > maxSize)
				{
					m_blankList				= (T[]) new AbstractPooledObject[maxSize];
					m_blankCount			= maxSize - m_outstandingCount;
				}
				else
				{
					m_blankList				= (T[]) new AbstractPooledObject[m_outstandingCount];
					m_blankCount			= 0;
				}
			}
			if (maxSize != m_maxSize)
			{
				m_maxSize = maxSize;
				getLogger().info(BasicEvent.EVENT_OBJECT_POOL_MAX_SIZE,
						"%s (max size changed from %,d)", toString(), maxSizePrev);
			}
		}
	}

	private void release(T object)
	{
		synchronized (this)
		{
			m_outstandingCount--;
			if(m_blankCount >= 0)		// Let it become garbage if no room for it -> Due to setMaxSize shrinking.
			{
				m_blankList[m_blankCount] = object;
				if (0 != m_waitingCount)
				{
					// Notify is expensive, notify() only if someone is actually waiting
					//noinspection CallToNotifyInsteadOfNotifyAll
					this.notify();
					m_notifyCount++;
				}
			}
			m_blankCount++;
		}
	}

	public void grow(int newCount)
	{
		// The caller is responsible to ensure this is called within a synchronized.
		int					prevCount		= m_blankList.length;
		newCount		= Math.min(m_maxSize, newCount);
		newCount		= Math.max(m_initialSize, newCount);
		if (newCount <= prevCount)
		{
			return;
		}

		m_blankList = (T[]) new AbstractPooledObject[newCount];
		m_blankCount = newCount - prevCount;
		for (int i = 0; i < m_blankCount; i++)
		{
			try
			{
				m_blankList[i] = m_supplier.get();
				m_blankList[i].setObjectPool(this);
			}
			catch (Exception e)
			{
				// This will never happen, as the ctor was tested in the ctor of the class.
			}
		}

		getLogger().debug(BasicEvent.EVENT_OBJECT_POOL_GROW, "%s (grown from %,d)", toString(), prevCount);
	}
	public void shrink(int newCount)
	{
		synchronized (this)
		{
			int			prevCount		= m_blankList.length;
			newCount = Math.max(m_initialSize, newCount);		// Don't shrink smaller than initial size.
			newCount = Math.max(m_outstandingCount, newCount);	// Never shrink to less than how many are outstanding
			if (newCount >= prevCount)
			{
				return;
			}

			// Resize the array of blanks to the new size
			m_blankList = Arrays.copyOf(m_blankList, newCount);
			// The last m_outstandingCount slots should be null
			for (int i = 0; i < m_outstandingCount; i++)
			{
				m_blankList[newCount - i - 1] = null;
			}
			m_blankCount = newCount - m_outstandingCount;
			// All non-referenced objects become garbage

			getLogger().debug(BasicEvent.EVENT_OBJECT_POOL_SHRINK, "%s (shrunk from %,d)", toString(), prevCount);
		}
	}

	protected String getSettingName(String settingLongName)
	{
		return String.format(settingLongName, m_name);
	}

	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString()
	{
		return String.format("ObjectPool %s: [used = %,d; alloc = %,d; max = %,d]",
				m_name, getOutstandingCount(), getAllocatedCount(), getMaxCount());
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, final long timeMS, final Object event)
	{
		int									objectPoolMaxSize			=
				CONFIGURATION_MANAGER.getInt(getSettingName(SETTING_MAX_SIZE), Integer.toString(m_maxSize));

		setMaxSize(objectPoolMaxSize);
	}

	private void cleanup()
	{
		if (m_outstandingCountMax > (10 * m_outstandingCount))
		{
			shrink(m_outstandingCount * 3);
		}
		m_outstandingCountMax = m_outstandingCount;
	}



	private static class ObjectPoolCleanupThread extends AbstractScheduledThread
	{
		public ObjectPoolCleanupThread()
		{
			super(false);

			setName(getClass().getSimpleName());
			setDaemon(true);
			start();
		}

		@Override
		protected void runOnce(long timeMS)
		{
			synchronized (OBJECT_POOL_LIST)
			{
				for (ObjectPool objectPool : OBJECT_POOL_LIST)
				{
					objectPool.cleanup();
				}
			}
		}
	}
}
