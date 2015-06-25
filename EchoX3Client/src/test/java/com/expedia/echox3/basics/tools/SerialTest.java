/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools;

import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.BasicFileReader;
import com.expedia.echox3.basics.file.SimpleFilenameFilter;
import com.expedia.echox3.basics.file.UrlFinder;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.monitoring.version.ManifestWrapper;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.serial.BasicClassLoader;
import com.expedia.echox3.basics.tools.serial.BasicClassLoaderManager;
import com.expedia.echox3.basics.tools.serial.BasicSerial;
import com.expedia.echox3.basics.tools.serial.ByteArrayWrapper;

public class SerialTest extends AbstractTestTools
{
	private static final String			TEST_NAME			= SerialTest.class.getSimpleName();
	private static final String			BROWN_FOX			= "The quick brown fox jumps over the lazy dog.";

	@Test
	public void testSimple() throws BasicException
	{
		logTestName();

		assertNull(BasicSerial.toBytes(null, null));
		assertNull(BasicSerial.toObject(null, (byte[]) null));

		assertNotNull(BasicSerial.getSerial(TEST_NAME));
		assertNotNull(BasicSerial.getSerial(TEST_NAME).getName());
		assertNotNull(BasicSerial.getClassLoader(TEST_NAME));

		validateSerial(BROWN_FOX);

		validateSerial(BasicTools.generateRandomString(100));
		validateSerial(BasicTools.generateRandomString(1100));
		validateSerial(BasicTools.generateRandomString(11 * 1000));
		validateSerial(BasicTools.generateRandomString(25 * 1000));

		validateSerialBytes(BROWN_FOX.getBytes());

		String[]		textList		= new String[3];
		textList[0] = BasicTools.generateRandomString(10);
		textList[1] = BasicTools.generateRandomString(10);
		textList[2] = BasicTools.generateRandomString(10);
		byte[]			bytes			= BasicSerial.toBytes(TEST_NAME, textList);
		assertNotNull(bytes);

		Serializable	back			= BasicSerial.toObject(TEST_NAME, bytes);
		assertNotNull(back);
		assertTrue(back instanceof String[]);
		String[]		textListBack	= (String[]) back;
		assertArrayEquals(textList, textListBack);
	}
	private void validateSerial(Serializable object) throws BasicException
	{
		byte[]				bytes1			= BasicSerial.toBytes(TEST_NAME, object);

		byte[]				bytes			= BasicSerial.toBytes(TEST_NAME, object);
		assertNotNull(bytes);
		assertTrue(Arrays.equals(bytes1, bytes));

		Serializable		back			= BasicSerial.toObject(TEST_NAME, bytes);
		assertNotNull(back);
		assertEquals(object, back);

		int					offset			= 5;
		byte[]				wrappedBytes	= new byte[bytes.length + (2 * offset)];
		System.arraycopy(bytes, 0, wrappedBytes, offset, bytes.length);
		ByteArrayWrapper	wrapper			= new ByteArrayWrapper();
		wrapper.set(wrappedBytes, offset, bytes.length);
		Serializable		wrappedBack		= BasicSerial.toObject(TEST_NAME, wrapper);
		assertNotNull(wrappedBack);
		assertEquals(object, wrappedBack);
	}
	private void validateSerialBytes(byte[] bytes) throws BasicException
	{
		byte[]			bytesMiddle		= BasicSerial.toBytes(TEST_NAME, bytes);
		assertNotNull(bytes);

		Serializable	back			= BasicSerial.toObject(TEST_NAME, bytesMiddle);
		assertNotNull(back);

		assertTrue(back instanceof byte[]);
		byte[]			bytesBack		= (byte[]) back;
		assertNotNull(bytesBack);
		assertArrayEquals(bytes, bytesBack);
	}

	@Test
	public void testConfiguration()
	{
		Publisher.getPublisher(ConfigurationManager.PUBLISHER_NAME).post(ConfigurationManager.REASON_PROVIDER_CHANGE);
		BasicTools.sleepMS(2000);		// Let the publication get through ... and nothing crashed :)
	}

	@Test
	public void testArray() throws BasicException
	{
		logTestName();

		String[]		textList		= new String[3];
		textList[0] = BasicTools.generateRandomString(10);
		textList[1] = BasicTools.generateRandomString(10);
		textList[2] = BasicTools.generateRandomString(10);

		byte[][]		bytes			= BasicSerial.toBytesArray(TEST_NAME, textList);
		assertNotNull(bytes);

		Serializable[]	back			= BasicSerial.toObjectArray(TEST_NAME, bytes);
		assertNotNull(back);
		assertArrayEquals(textList, back);
	}

	@Test
	public void testManifestWrapper() throws BasicException
	{
		String							testName	= logTestName();

		Map<String, ManifestWrapper>	map			= ManifestWrapper.getManifestMap();

		for (ManifestWrapper wrapper : map.values())
		{
			byte[]			bytes		= BasicSerial.toBytes(testName, wrapper);
			Serializable	back		= BasicSerial.toObject(testName, bytes);
			assertEquals(wrapper, back);
		}

		byte[]			bytes		= BasicSerial.toBytes(testName, (Serializable) map);
		Serializable	back		= BasicSerial.toObject(testName, bytes);

		assertEquals(map, back);
		@SuppressWarnings("unchecked")
		Map<String, ManifestWrapper>	mapBack		= (Map<String, ManifestWrapper>) back;
		assertEquals(map.size(), mapBack.size());
		for (Map.Entry<String, ManifestWrapper> entry : map.entrySet())
		{
			String				key				= entry.getKey();
			ManifestWrapper		wrapper			= entry.getValue();
			ManifestWrapper		wrapperBack		= mapBack.get(key);
			assertNotNull(wrapperBack);
			assertEquals(wrapper, wrapperBack);
		}
	}

	@Test
	public void testClassLoader() throws Exception
	{
		String						testName		= logTestName();

		BasicClassLoader			classLoader		= BasicClassLoaderManager.getClassLoader(testName);
		assertEquals(testName, classLoader.getName());
		classLoader.unregisterMBean();

		FilenameFilter				filenameFilter	= new SimpleFilenameFilter(null, null, ".class");
		Set<URL>					urlSet			= UrlFinder.getFileUriListRecursive("data/classes", filenameFilter);
		Map<String, byte[]>			classMap		= new HashMap<>();
		for (URL url : urlSet)
		{
			String		className		= url.getFile();
			className = className.substring(className.lastIndexOf(BaseFileHandler.FOLDER_SEPARATOR) + 1);
			className = className.replace(".class", "");
			className = "com.expedia.echox3.smart." + className;
			try
			{
				InputStream inputStream		= url.openStream();
				byte[]			bytes			= BasicFileReader.readInputStream(inputStream);
				classMap.put(className, bytes);
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_DEBUG, exception, "Exception reading " + url.toString());
				assertNull(exception);
			}
		}

		try
		{
			classLoader.putClassMap(classMap);
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_DEBUG, exception, "Exception loading classes.");
			assertNull(exception);
		}

		for (String className : classMap.keySet())
		{
			try
			{
				Class		clazz	= Class.forName(className, false, classLoader);
				assertNotNull(clazz);
			}
			catch (Exception exception)
			{
				assertNull(exception);
			}
		}
	}

	@Test
	public void testPerformance() throws BasicException
	{
		logTestName();

		int				count			= 100 * 1000;
		int				length			= 25;
		String[]		textList		= new String[count];
		Serializable[]	bytesList		= new byte[count][];
		byte[][]		answerList		= new byte[2][];
		for (int i = 0; i < textList.length; i++)
		{
			textList[i] = BasicTools.generateRandomString(length);
			bytesList[i] = textList[i].getBytes(BasicSerial.CHARSET);
		}

		String		testName	= String.format("1 thread,");
		int			iMax		= 10;
		for (int i = 0; i < iMax; i++)
		{
			measurePerformance(testName, textList, answerList);
		}
		for (int i = 0; i < iMax; i++)
		{
			measurePerformance(testName, bytesList, answerList);
		}
	}
	private static void measurePerformance(String testName, Serializable[] list, byte[][] bytesList)		// NOPMD
	{
		System.gc();
		System.gc();
		long			t1				= System.nanoTime();
		int				length			= 0;
		for (int i = 0; i < list.length; i++)
		{
			try
			{
				length = BasicSerial.toBytes(TEST_NAME, list[i]).length;
			}
			catch (BasicException e)
			{
				getLogger().error(e.getBasicEvent(), e, "Unexpected exception during test");
				assertNull(e);
			}
		}
		long			t2				= System.nanoTime();
//		for (int i = 0; i < list.length; i++)
//		{
//			BasicSerial.toObject(TEST_NAME, bytesList[i]);
//		}
//		long			t3				= System.nanoTime();

		reportPerformance(String.format("%s toBytes(%s; %,d chars)",
				testName, list[0].getClass().getSimpleName(), length), t2 - t1, list.length, true);
//		reportPerformance(String.format("toObject(%s; %,d chars)",
//				list[0].getClass().getSimpleName(), length), t3 - t2, list.length, true);
	}

	@Test
	public void testMultiPerformance()
	{
		logTestName();

		int				passCount		= 5;
		int				itemCount		= 100 * 1000;
		int				length			= 25;
		String[]		textList		= new String[itemCount];
		Serializable[]	bytesList		= new byte[itemCount][];
		byte[][]		answerList		= new byte[2][];
		for (int i = 0; i < textList.length; i++)
		{
			textList[i] = BasicTools.generateRandomString(length);
			bytesList[i] = textList[i].getBytes(BasicSerial.CHARSET);
		}

		int						cThread		= 4;
		String					testName		= String.format("%,d threads,", cThread);
		PerformanceThread[]		threadList		= new PerformanceThread[cThread];

		for (int i = 0; i < cThread; i++)
		{
			threadList[i] = new PerformanceThread(testName, textList, bytesList, answerList, passCount);
		}

		for (int i = 0; i < cThread; i++)
		{
			PerformanceThread		thread		= threadList[i];
			while (0 < thread.m_passCount)
			{
				BasicTools.sleepMS(1000);
			}
		}
	}

	private static class PerformanceThread extends Thread
	{
		private static final AtomicInteger		COUNTER		= new AtomicInteger(0);

		int					m_number		= COUNTER.incrementAndGet();
		String				m_testName;
		String[]			m_textList;
		Serializable[]		m_bytesList;
		byte[][]			m_answerList;
		private int			m_passCount;

		public PerformanceThread(
				String testName, String[] textList, Serializable[] bytesList, byte[][] answerList, int passCount)
		{
			m_testName = testName;
			m_textList = textList;
			m_bytesList = bytesList;
			m_answerList = answerList;
			m_passCount = passCount;

			setName(String.format("%s-%,d", getClass().getSimpleName(), m_number));
			setDaemon(true);
			start();
		}

		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		@Override
		public void run()
		{
			getLogger().info(BasicEvent.EVENT_TEST, "Thread starting");
			while (0 < --m_passCount)
			{
				measurePerformance(m_testName, m_textList, m_answerList);
//				measurePerformance(m_bytesList, m_answerList);
			}
			getLogger().info(BasicEvent.EVENT_TEST, "Thread done");
		}
	}

/*
	@Test
	public void testWireTime() throws BasicException
	{
		int			compressMinLengthSav	= BasicSerial.getCompressMinLength();
		int			keySize					= 10;
		int			valueSize				= 25;
		int[]		keyCountList			=
				{															  // Run longer ones only on demand.
					1, 2, 3, 5, 7, 10, 20, 30, 50, 70, 100, 200, 300, 500, 700// , 1000, 2000, 3000, 5000, 7000, 10000
				};
		FireHoseConfiguration			hoseConfiguration		= new FireHoseConfiguration("Test", keySize, valueSize);
		TestConfigurationProvider		provider				= new TestConfigurationProvider();

		StringBuilder					sb						= new StringBuilder(10 * 1024);
		// CHECKSTYLE:OFF
		sb.append("KeyCount	Bytes(None)	Bytes(Comp)	T(None)	T(Comp)	T(N-Wire1)	T(C-Wire1)	T(N-Total1)	T(N-Total2)	T(C-Total1)	T(C-Total2)");
		// CHECKSTYLE:ON
		sb.append(BaseFileHandler.LINE_SEPARATOR);

		for (int i = 0; i < 4; i++)
		{
			setCompressMinLength(provider, 100 * 1000 * 1000);
			double[][]		durationNoneMS			= measureWireTime(hoseConfiguration, keyCountList);
			setCompressMinLength(provider, 10);
			double[][]		durationCompressMS		= measureWireTime(hoseConfiguration, keyCountList);
			sb.append(displayWireTime(keyCountList, durationNoneMS, durationCompressMS));
		}
		// System.out.println() to facilitate copy/paste into MS-Excel.
		System.out.println(sb.toString());

		provider.addSetting(BasicSerial.SETTING_NAME_COMPRESS_MIN_LENGTH, Integer.toString(compressMinLengthSav));
	}
	private void setCompressMinLength(TestConfigurationProvider provider, int length)
	{
		provider.addSetting(BasicSerial.SETTING_NAME_COMPRESS_MIN_LENGTH, Integer.toString(length));
		while (length != BasicSerial.getCompressMinLength())
		{
			BasicDelay.sleepMS(10);
		}
	}
	private double[][] measureWireTime(FireHoseConfiguration hoseConfiguration, int[] keyCountList)
			throws BasicException
	{
		System.gc();
		// [keyCountIndex][0 = durationSerial; 1 = Size]
		double[][]	durationListMS		= new double[keyCountList.length][];

		// Build the map ...
		HashMap<String, String>		map		= new HashMap<>();
		for (int keyCountIndex = 0; keyCountIndex < keyCountList.length; keyCountIndex++)
		{
			map.clear();
			System.gc();
			System.gc();
			for (int keyIndex = 0; keyIndex < keyCountList[keyCountIndex]; keyIndex++)
			{
				String		key		= hoseConfiguration.generateKey(keyIndex).getText();
				String		value	= hoseConfiguration.generateValue(keyIndex).getText();
				map.put(key, value);
			}

			int			iMax		= 100;
			long		t1			= System.nanoTime();
			byte[]		bytes		= null;
			for (int i = 0; i < iMax; i++)
			{
				bytes = BasicSerial.toBytes("Test", map);
			}
			long		t2					= System.nanoTime();
			double		durationSerialMS	= (t2 - t1) / (iMax * 1000.0 * 1000);
			durationListMS[keyCountIndex] = new double[2];
			durationListMS[keyCountIndex][0] = durationSerialMS;
			durationListMS[keyCountIndex][1] = bytes.length;
		}



		return durationListMS;
	}

	// System.out.println() on purpose, so the output can be copy/pasted into MS-Excel for pretty graphs.
	@SuppressWarning("PMD")
	private String displayWireTime(int[] keyCountList, double[][] durationNoneMS, double[][] durationCompressMS)
	{
		StringBuilder		sb			= new StringBuilder(10 * 1024);
		for (int keyCountIndex = 0; keyCountIndex < keyCountList.length; keyCountIndex++)
		{
			double		timeNWire1		= getWireTimeMS(durationNoneMS[keyCountIndex][1]);
			double		timeCWire1		= getWireTimeMS(durationCompressMS[keyCountIndex][1]);
			sb.append(String.format("%d\t%.0f\t%.0f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
					keyCountList[keyCountIndex],
					durationNoneMS[keyCountIndex][1],
					durationCompressMS[keyCountIndex][1],
					durationNoneMS[keyCountIndex][0],
					durationCompressMS[keyCountIndex][0],
					timeNWire1,
					timeCWire1,
					durationNoneMS[keyCountIndex][0] + timeNWire1,
					durationNoneMS[keyCountIndex][0] + timeNWire1 + timeNWire1,
					durationCompressMS[keyCountIndex][0] + timeCWire1,
					durationCompressMS[keyCountIndex][0] + timeCWire1 + timeCWire1
			));
			sb.append(BaseFileHandler.LINE_SEPARATOR);
		}
		return sb.toString();
	}
	private double getWireTimeMS(double byteCount)
	{
		return byteCount / 1e5;		// 1 MB = 1e6 bytes = 10 ms
	}
*/
}
