/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.expedia.echox3.basics.collection.histogram.IHistogram;
import com.expedia.echox3.basics.collection.histogram.IHistogram.BinData;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram.Precision;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager.IEventListener;

public class ManagedByteBufferManager implements ManagedByteBufferManagerMBean
{
	private static final String				SETTING_PREFIX					= ManagedByteBufferManager.class.getName();
	private static final String				SETTING_POOL_EXACT_SIZE_LIST	= SETTING_PREFIX + ".FixedSizeList";
	private static final String				SETTING_POOL_LADDER_SIZE_LIST	= SETTING_PREFIX + ".LadderSizeList";

	private static final ConfigurationManager	CONFIGURATION_MANAGER	= ConfigurationManager.getInstance();
	// Very important: This map MUST ordered in numeric order of the key (i.e. 50, then 100).
	// This is to allow for search from the ObjectPool with small buffers towards the ObjectPool with larger buffers.
	private static final String					CUSTOM_NAME				= "Custom";

	private static final ManagedByteBufferManager	INSTANCE			= new ManagedByteBufferManager();

	private final IHistogram		m_histogramCustom	= new LogarithmicHistogram(1, 100 * 1000, Precision.Normal);
	private final IHistogram		m_histogramExact	= new LogarithmicHistogram(1, 100 * 1000, Precision.Normal);
	private final IHistogram		m_histogramLadder	= new LogarithmicHistogram(1, 100 * 1000, Precision.Normal);

	// Very important: This map MUST ordered in numeric order of the key (i.e. 50, then 100).
	// This is to allow for search from the ObjectPool with small buffers towards the ObjectPool with larger buffers.
	private volatile SizeAndPoolWrapper[]	m_exactPoolList		= null;
	private volatile SizeAndPoolWrapper[]	m_ladderPoolList	= null;

	// For those allocations that are not in the Wrapper list...
	private ObjectPool<ManagedByteBuffer>	m_objectPoolCustom	= new ObjectPool<>(
			new StringGroup(ManagedByteBuffer.class.getSimpleName() + "." + CUSTOM_NAME), ManagedByteBuffer::new);

	private ManagedByteBufferManager()
	{
		updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).register(this::updateConfiguration);

		BasicTools.registerMBean(this, null, new StringGroup(
				ObjectPool.class.getSimpleName() + "." + ManagedByteBufferManager.class.getSimpleName()));
	}

	public static ManagedByteBufferManager getInstance()
	{
		return INSTANCE;
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private void updateConfiguration(String publisherName, long timeMS, Object event)
	{
		m_exactPoolList = updateConfiguration(SETTING_POOL_EXACT_SIZE_LIST, m_exactPoolList);
		m_ladderPoolList = updateConfiguration(SETTING_POOL_LADDER_SIZE_LIST, m_ladderPoolList);
	}
	private SizeAndPoolWrapper[] updateConfiguration(String settingName, SizeAndPoolWrapper[] poolWrapperListPrev)
	{
		// Note: The ObjectPool update themselves once created.

		List<String>			settingNameList		= CONFIGURATION_MANAGER.getSettingValueList(settingName);
		SizeAndPoolWrapper[]	poolWrapperList		= new SizeAndPoolWrapper[settingNameList.size()];
		int						index				= 0;
		for (String settingValue : settingNameList)
		{
			int						cbPool			= Integer.parseInt(settingValue);
			String					poolName		= String.format("%s.%,d",
																	ManagedByteBuffer.class.getSimpleName(), cbPool);
			poolName = poolName.replace(',', '_');

			SizeAndPoolWrapper		wrapper		= findWrapperBySize(poolWrapperListPrev, cbPool);
			if (null == wrapper)
			{
				wrapper = new SizeAndPoolWrapper(
										cbPool, new ObjectPool<>(new StringGroup(poolName), ManagedByteBuffer::new));
			}
			poolWrapperList[index++] = wrapper;
		}

		if (null != poolWrapperListPrev)
		{
			for (int i = 0; i < poolWrapperListPrev.length; i++)
			{
				SizeAndPoolWrapper		wrapperSav	= poolWrapperListPrev[i];
				SizeAndPoolWrapper		wrapper		= findWrapperBySize(poolWrapperListPrev, wrapperSav.getSize());
				if (null == wrapper)
				{
					// The wrapper in the previous list is not in the new list, release it.
					wrapperSav.getObjectPool().release();
				}
			}
		}

		return poolWrapperList;
	}
	private SizeAndPoolWrapper findWrapperBySize(SizeAndPoolWrapper[] poolWrapperList, int cb)
	{
		SizeAndPoolWrapper		wrapper		= null;

		if (null != poolWrapperList)
		{
			for (int i = 0; i < poolWrapperList.length; i++)
			{
				SizeAndPoolWrapper		wrapperT		= poolWrapperList[i];
				if (null != wrapperT && wrapperT.getSize() == cb)
				{
					wrapper = wrapperT;
					break;
				}
			}
		}

		return wrapper;
	}

	public static ManagedByteBuffer get(int cb)
	{
		return getInstance().getInternal(cb);
	}
	public ManagedByteBuffer getInternal(int cb)
	{
		ManagedByteBuffer		managedByteBuffer		= getInstance().getExact(cb);
		if (null != managedByteBuffer)
		{
			m_histogramExact.record(cb);
		}
		else
		{
			managedByteBuffer = getLadder(cb);
		}


		if (null != managedByteBuffer)
		{
			m_histogramLadder.record(cb);
		}
		else
		{
			// If fall through, means to use the "CUSTOM" object pool for a large ByteBuffer.
			managedByteBuffer		= m_objectPoolCustom.get();
			managedByteBuffer.setByteBuffer(ByteBuffer.allocate(cb));
			m_histogramCustom.record(cb);
		}

		return managedByteBuffer;
	}
	public ManagedByteBuffer getExact(int cb)
	{
		ManagedByteBuffer		managedByteBuffer		= null;
		for (int index = 0; index < m_exactPoolList.length; index++)
		{
			SizeAndPoolWrapper		wrapper		= m_exactPoolList[index];
			if (wrapper.getSize() == cb)
			{
				// Found the ObjectPool
				managedByteBuffer		= wrapper.getObjectPool().get();
				if (null == managedByteBuffer.getByteBuffer())
				{
					// Not allocated yet means the first time this managedByteBuffer is used, allocate it
					managedByteBuffer.setByteBuffer(ByteBuffer.allocate(wrapper.getSize()));
					managedByteBuffer.setCustom(false);
				}
				break;
			}
		}

		return managedByteBuffer;
	}
	public ManagedByteBuffer getLadder(int cb)
	{
		ManagedByteBuffer		managedByteBuffer		= null;
		for (int index = 0; index < m_ladderPoolList.length; index++)
		{
			SizeAndPoolWrapper		wrapper		= m_ladderPoolList[index];
			if (wrapper.getSize() >= cb)
			{
				// Found the ObjectPool
				managedByteBuffer		= wrapper.getObjectPool().get();
				if (null == managedByteBuffer.getByteBuffer())
				{
					// Not allocated yet means the first time this managedByteBuffer is used, allocate it
					managedByteBuffer.setByteBuffer(ByteBuffer.allocate(wrapper.getSize()));
					managedByteBuffer.setCustom(false);
				}
				managedByteBuffer.setOffset(managedByteBuffer.getByteBuffer().capacity() - cb);
				managedByteBuffer.getByteBuffer().position(managedByteBuffer.getOffset());
				break;
			}
		}

		return managedByteBuffer;
	}

	public IHistogram getHistogramExact()
	{
		return m_histogramExact;
	}

	public IHistogram getHistogramLadder()
	{
		return m_histogramLadder;
	}

	public IHistogram getHistogramCustom()
	{
		return m_histogramCustom;
	}

	@Override
	public List<String> getBinDataExact()
	{
		return getBinDataListText(m_histogramExact.getBinData());
	}

	@Override
	public List<String> getBinDataLadder()
	{
		return getBinDataListText(m_histogramLadder.getBinData());
	}

	@Override
	public List<String> getBinDataCustom()
	{
		return getBinDataListText(m_histogramCustom.getBinData());
	}
	private List<String> getBinDataListText(List<BinData> list)
	{
		List<String>		textList		= new ArrayList<>(list.size());
		for (BinData binData : list)
		{
			textList.add(binData.toTextString());
		}
		return textList;
	}

	@Override
	public void resetHistogram()
	{
		m_histogramExact.reset();
		m_histogramLadder.reset();
		m_histogramCustom.reset();
	}





	private static class SizeAndPoolWrapper
	{
		private int								m_size;
		private ObjectPool<ManagedByteBuffer>	m_objectPool;

		private SizeAndPoolWrapper(int size, ObjectPool<ManagedByteBuffer> objectPool)
		{
			m_size = size;
			m_objectPool = objectPool;
		}

		public int getSize()
		{
			return m_size;
		}

		public ObjectPool<ManagedByteBuffer> getObjectPool()
		{
			return m_objectPool;
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
			return m_objectPool.toString();
		}
	}
}
