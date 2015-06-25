/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.expedia.echox3.basics.collection.simple.CopyOnWriteSimpleMap;
import com.expedia.echox3.basics.collection.simple.ObjectPool;
import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.monitoring.counter.CounterFactory;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.counter.IOperationCounter;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;

import com.expedia.echox3.basics.collection.histogram.LogarithmicHistogram;
import com.expedia.echox3.basics.tools.pubsub.PublisherManager;

public class BasicSerial
{
	public static final String			CHARSET_NAME						= "UTF8";
	public static final Charset			CHARSET								= Charset.forName(CHARSET_NAME);

	private enum SerialType
	{
		none,				// e.g. byte[] as input
		serialDefault,
		java,
		javaString
	}
	public enum CompressType
	{
		none,				// No compression
		compressDefault,
		deflate
	}
	public static final SerialType[]					SERIAL_TYPE_LIST						= SerialType.values();
	public static final CompressType[]					COMPRESS_TYPE_LIST						= CompressType.values();


	// COMPRESS_MIN_LENGTH is a trade-off between memory saving (starting at about 1K) and time spent compressing.
	public static final String			SETTING_PREFIX							= BasicSerial.class.getName();
	public static final String			SETTING_NAME_COMPRESS_MIN_LENGTH		= SETTING_PREFIX + ".compressMinLength";
	public static final String			SETTING_DEFAULT_COMPRESS_MIN_LENGTH		= "2,000";

	private static final CopyOnWriteSimpleMap<String, BasicSerial>
															SERIAL_MAP				= new CopyOnWriteSimpleMap<>();
	private static final BasicSerial					UNNAMED_SERIAL							= new BasicSerial();
	private static int									s_compressMinLength						= 1234;		// Will be overwritten at startup with the default

	private static final ObjectPool<InflaterPooledObject>					INFLATER_POOL;
	private static final ObjectPool<DeflaterPooledObject>					DEFLATER_POOL;
	private static final ObjectPool<ByteArrayPooledObject>					BYTE_ARRAY_POOL;
	private static final ObjectPool<ByteArrayOutputStreamPooledObject>		BYTE_OUTPUT_STREAM_POOL;
	private static final ObjectPool<ByteArrayInputStreamPooledObject>		BYTE_INPUT_STREAM_POOL;

	private String					m_name;
	private BasicClassLoader		m_classLoader;

	private IOperationCounter		m_counterSerialize;
	private IOperationCounter		m_counterDeflate;
	private IOperationCounter		m_counterToBytes;

	private IOperationCounter		m_counterDeserialize;
	private IOperationCounter		m_counterInflate;
	private IOperationCounter		m_counterToObject;

	static
	{
		new ConfigurationChangeListener();

		StringGroup baseNameList	= new StringGroup("Serial");
		StringGroup nameList;

		nameList = baseNameList.getCopy();
		nameList.append("InflaterPool");
		INFLATER_POOL				= new ObjectPool<>(nameList, InflaterPooledObject::new);

		nameList = baseNameList.getCopy();
		nameList.append("DeflaterPool");
		DEFLATER_POOL				= new ObjectPool<>(nameList, DeflaterPooledObject::new);

		nameList = baseNameList.getCopy();
		nameList.append("ByteArrayPool");
		BYTE_ARRAY_POOL				= new ObjectPool<>(nameList, ByteArrayPooledObject::new);

		nameList = baseNameList.getCopy();
		nameList.append("OutputStreamPool");
		BYTE_OUTPUT_STREAM_POOL		= new ObjectPool<>(nameList, ByteArrayOutputStreamPooledObject::new);

		nameList = baseNameList.getCopy();
		nameList.append("InputStreamPool");
		BYTE_INPUT_STREAM_POOL		= new ObjectPool<>(nameList, ByteArrayInputStreamPooledObject::new);
	}

	private BasicSerial()
	{
		this("NoName");
	}
	private BasicSerial(String name)
	{
		m_name = name;
		m_classLoader = BasicClassLoaderManager.getClassLoader(m_name);

		StringGroup baseNameList	= new StringGroup(new String[] {"Serial", m_name});
		StringGroup nameList;

		nameList = baseNameList.getCopy();
		nameList.append("Serialize");
		m_counterSerialize = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Serialization only");

		nameList = baseNameList.getCopy();
		nameList.append("Deflate");
		m_counterDeflate = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Compression");

		nameList = baseNameList.getCopy();
		nameList.append("toBytes");
		m_counterToBytes = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Complete transformation of Object to byte[]");

		nameList = baseNameList.getCopy();
		nameList.append("Inflate");
		m_counterInflate = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"De-compression");

		nameList = baseNameList.getCopy();
		nameList.append("Deserialize");
		m_counterDeserialize = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"De-serialization only");

		nameList = baseNameList.getCopy();
		nameList.append("toObject");
		m_counterToObject = CounterFactory.getInstance().getLogarithmicOperationCounter(nameList,
				LogarithmicHistogram.Precision.Normal, CounterFactory.CounterRange.us,
				"Complete transformation of byte[] to Object");
	}

	public static byte[][] toBytesArray(String name, Serializable[] objectList) throws BasicException
	{
		return toBytesArray(name, objectList, CompressType.compressDefault);
	}
	public static byte[][] toBytesArray(String name, Serializable[] objectList, CompressType compressType)
			throws BasicException
	{
		byte[][]		bytesList		= new byte[objectList.length][];
		for (int i = 0; i < objectList.length; i++)
		{
			bytesList[i] = toBytes(name, objectList[i], compressType);
		}
		return bytesList;
	}
	public static byte[] toBytes(String name, Serializable object) throws BasicException
	{
		return toBytes(name, object, CompressType.compressDefault);
	}
	public static byte[] toBytes(String name, Serializable object, CompressType compressType) throws BasicException
	{
		if (null == object)
		{
			return null;
		}

		BasicSerial				serial				= getSerial(name);
		IOperationContext		contextToBytes		= serial.m_counterToBytes.begin();

		byte[]					flateBytes			= null;
		try
		{
			byte[]					serialBytes;
			byte					serialType;
			CompressType			compressTypeObject;
			if (object instanceof byte[])
			{
				serialBytes = (byte[]) object;
				serialType = (byte) SerialType.none.ordinal();
				compressTypeObject = CompressType.none;
			}
			else if (object instanceof String)
			{
				serialBytes = ((String) object).getBytes(CHARSET);
				serialType = (byte) SerialType.javaString.ordinal();
				compressTypeObject = CompressType.deflate;
			}
			else
			{
				serialBytes = serial.serializeJava(object);
				serialType = (byte) SerialType.java.ordinal();
				compressTypeObject = CompressType.deflate;
			}
			if (CompressType.compressDefault.equals(compressType))
			{
				compressType = compressTypeObject;
			}

			// The compression call writes bytes[1] with the compression algorithm;
			// leaves byte[0] to be written with the serialization algorithm.
			flateBytes = serial.compress(serialBytes, compressType);
			flateBytes[0] = serialType;
		}
		finally
		{
			contextToBytes.end(null != flateBytes);
		}

		return flateBytes;
	}

	public static Serializable[] toObjectArray(String name, byte[][] bytesList) throws BasicException
	{
		Serializable[]		objectList		= new Serializable[bytesList.length];
		for (int i = 0; i < bytesList.length; i++)
		{
			objectList[i] = toObject(name, bytesList[i], 0, bytesList[i].length);
		}
		return objectList;
	}
	public static Serializable toObject(String name, byte[] bytesIn) throws BasicException
	{
		if (null == bytesIn)
		{
			return null;
		}
		else
		{
			return toObject(name, bytesIn, 0, bytesIn.length);
		}
	}
	public static Serializable[] toObjectArray(String name, ByteArrayWrapper[] wrapperList) throws BasicException
	{
		Serializable[]		objectList		= new Serializable[wrapperList.length];
		for (int i = 0; i < wrapperList.length; i++)
		{
			objectList[i] = toObject(name,
					wrapperList[i].getByteArray(), wrapperList[i].getIndexMin(), wrapperList[i].getLength());
		}
		return objectList;
	}
	public static Serializable toObject(String name, ByteArrayWrapper wrapper) throws BasicException
	{
		return toObject(name, wrapper.getByteArray(), wrapper.getIndexMin(), wrapper.getLength());
	}
	public static Serializable toObject(String name, byte[] bytesIn, int indexMin, int length) throws BasicException
	{
		if (null == bytesIn)
		{
			return null;
		}

		BasicSerial				serial				= getSerial(name);
		IOperationContext		contextToBytes		= serial.m_counterToObject.begin();

		Serializable			object				= null;
		try
		{
			byte[]				serialBytes			= serial.decompress(bytesIn, indexMin, length);
			object = serial.toObject(bytesIn[indexMin], serialBytes);
		}
		finally
		{
			contextToBytes.end(null != object);
		}

		return object;
	}

	public static int getCompressMinLength()
	{
		return s_compressMinLength;
	}

	public String getName()
	{
		return m_name;
	}

	public static ClassLoader getClassLoader(String name)
	{
		BasicSerial		serial		= getSerial(name);
		ClassLoader		loader		= serial.m_classLoader;
		return loader;
	}

	public static BasicSerial getSerial(String name)
	{
		if (null == name)
		{
			return UNNAMED_SERIAL;
		}

		BasicSerial				serial			= SERIAL_MAP.get(name);
		if (null == serial)
		{
			synchronized (SERIAL_MAP)
			{
				serial = SERIAL_MAP.get(name);
				if (null == serial)
				{
					serial = new BasicSerial(name);
					SERIAL_MAP.put(name, serial);
				}
			}
		}
		return serial;
	}
	public static void flushSerial(String name)
	{
		BasicSerial				serial;
		synchronized (SERIAL_MAP)
		{
			serial = SERIAL_MAP.remove(name);
		}

		if (null != serial)
		{
			BasicClassLoaderManager.flushClassLoader(serial.getName());

			serial.m_counterSerialize.close();
			serial.m_counterDeserialize.close();
			serial.m_counterInflate.close();
			serial.m_counterDeflate.close();
			serial.m_counterToBytes.close();
			serial.m_counterToObject.close();
		}
	}

	private byte[] serializeJava(Serializable object) throws BasicException
	{
		IOperationContext					context				= m_counterSerialize.begin();

		byte[]								bytes				= null;
		ByteArrayOutputStreamPooledObject	pooledObject		= BYTE_OUTPUT_STREAM_POOL.get();
		ByteArrayOutputStream				byteStream			= pooledObject.getByteStream();
		ObjectOutputStream					objectStream		= null;
		try
		{
			objectStream = new ObjectOutputStream(byteStream);

			objectStream.writeObject(object);
			objectStream.flush();

			bytes = byteStream.toByteArray();
		}
		catch (Throwable throwable)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_JAVA_SERIAL_FAILED,
					String.format(
							"Serialization failed on object of class %s",
							object.getClass().getName()), throwable);
		}
		finally
		{
			if (null != context)
			{
				context.end(null != bytes);
			}
			byteStream.reset();
			pooledObject.release();
			BaseFileHandler.closeSafe(objectStream);
		}

		return bytes;
	}

	@SuppressWarnings("fallthrough")
	private byte[] compress(byte[] bytesIn, CompressType compressType) throws BasicException
	{
		if (bytesIn.length < getCompressMinLength())
		{
			compressType = CompressType.none;
		}


		byte[]				bytesOut	= null;
		IOperationContext	context		= m_counterDeflate.begin();
		try
		{
		switch (compressType)
		{
		case none:
			bytesOut = compressNone(bytesIn);
			break;

		case compressDefault:
			compressType = CompressType.deflate;
		case deflate:
			bytesOut = compressDeflate(bytesIn);
			break;
		}
		bytesOut[1] = (byte) compressType.ordinal();
		}
		finally
		{
			if (null != context)
			{
				context.end(null != bytesOut);
			}
		}

		return bytesOut;
	}
	private byte[] compressNone(byte[] bytesIn)
	{
		int					cb				= bytesIn.length;
		byte[]				bytesOut		= new byte[cb + 2];

		// Even at 1K, this extra arrayCopy costs less than 0.05 us; less than 0.1 us at 2K
		// Anything more than 2K will be compressed.
		// No point in complicating things to avoid this arrayCopy.
		// (see BasicsTests.tools.MiscTests.testSystemArrayCopy)
		System.arraycopy(bytesIn, 0, bytesOut, 2, cb);

		return bytesOut;
	}
	private byte[] compressDeflate(byte[] bytesIn) throws BasicException
	{
		byte[]					bytesOut;

		DeflaterPooledObject				pooledDeflater		= DEFLATER_POOL.get();
		Deflater							deflater			= pooledDeflater.getDeflater();
		ByteArrayPooledObject				pooledByteArray		= BYTE_ARRAY_POOL.get();
		ByteArrayOutputStreamPooledObject	pooledStream		= BYTE_OUTPUT_STREAM_POOL.get();
		ByteArrayOutputStream				byteStream			= pooledStream.getByteStream();
		try
		{
			deflater.setLevel(Deflater.BEST_SPEED);
			deflater.setInput(bytesIn);
			deflater.finish();

			// Write the header bytes
			byte			zero		= '\0';
			byteStream.write(zero);
			byteStream.write(zero);

			byte[] buf = pooledByteArray.getBuffer();
			while ( !deflater.finished() )
			{
				int cb = deflater.deflate(buf);
				byteStream.write(buf, 0, cb);
			}
			bytesOut = byteStream.toByteArray();
		}
		catch (Throwable throwable)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_COMPRESS_FAILED,
					String.format("Compression failed on object of length %,d", bytesIn.length), throwable);
		}
		finally
		{
			deflater.reset();
			pooledDeflater.release();
			pooledByteArray.release();
			byteStream.reset();
			pooledStream.release();
		}

		return bytesOut;
	}



	private byte[] decompress(byte[] bytesIn, int indexMin, int length) throws BasicException
	{
		Integer				ordinal				= Byte.valueOf(bytesIn[indexMin + 1]).intValue();
		if (COMPRESS_TYPE_LIST.length <= ordinal)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_UNKNOWN_COMPRESSION,
					String.format("Unknown compression type %d found.", ordinal));
		}
		CompressType		compressType		= COMPRESS_TYPE_LIST[ordinal];

		byte[]				bytesOut			= null;
		IOperationContext	context				= m_counterInflate.begin();
		try
		{
			switch (compressType)
			{
			default:
			case none:
				bytesOut = decompressNone(bytesIn, indexMin, length);
				break;
			case deflate:
				bytesOut = decompressInflate(bytesIn, indexMin, length);
				break;
			}
		}
		finally
		{
			if (null != context)
			{
				context.end(null != bytesOut);
			}
		}

		return bytesOut;
	}


	private byte[] decompressNone(byte[] bytesIn, int indexMin, int length) throws BasicException
	{
		int			actualStart		= indexMin + 2;
		byte[]		bytesOut		= new byte[length - 2];

		System.arraycopy(bytesIn, actualStart, bytesOut, 0, length - 2);

		return bytesOut;
	}
	private byte[] decompressInflate(byte[] bytesIn, int indexMin, int length) throws BasicException
	{
		int			actualStart		= indexMin + 2;
		byte[]		bytesOut;

		InflaterPooledObject		pooledInflater		= INFLATER_POOL.get();
		ByteArrayPooledObject		pooledByteArray		= BYTE_ARRAY_POOL.get();
		Inflater					inflater			= pooledInflater.getInflater();
		try
		{
			inflater.setInput(bytesIn, actualStart, length - 2);
			byte[]					result			= pooledByteArray.getBuffer();
			ByteArrayOutputStream	byteStream 		= new ByteArrayOutputStream();
			while (!inflater.finished())
			{
				int			cb				= inflater.inflate(result);
				byteStream.write(result, 0, cb);
			}
			bytesOut = byteStream.toByteArray();
		}
		catch (Throwable t)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_DECOMPRESS_FAILED,
					String.format("De-serialization error on  %,d bytes", bytesIn.length), t);
		}
		finally
		{
			inflater.reset();
			pooledInflater.release();
			pooledByteArray.release();
		}

		return bytesOut;
	}

	private Serializable toObject(int ordinal, byte[] bytesIn) throws BasicException
	{
		if (SerialType.serialDefault.ordinal() == ordinal || SERIAL_TYPE_LIST.length <= ordinal)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_UNKNOWN_SERIALIZATION,
					String.format("Unknown serialization type %d found.", ordinal));
		}
		SerialType			serialType			= SERIAL_TYPE_LIST[ordinal];

		Serializable		object				= null;
		IOperationContext	context				= m_counterDeserialize.begin();
		try
		{
			switch (serialType)
			{
			case none:
				object = bytesIn;
				break;
			case javaString:
				object = new String(bytesIn, CHARSET);
				break;
			case java:
				object = deserializeJava(bytesIn);
				break;
			}
		}
		finally
		{
			if (null != context)
			{
				context.end(null != object);
			}
		}

		return object;
	}

	private Serializable deserializeJava(byte[] bytesIn) throws BasicException
	{
		ByteArrayInputStreamPooledObject	byteStreamObject		= null;
		ObjectInputStream					objectStream			= null;
		Serializable						object					= null;
		try
		{
			byteStreamObject = BYTE_INPUT_STREAM_POOL.get();
			ReusableByteArrayInputStream byteStream = byteStreamObject.getByteStream();
			byteStream.setBuffer(bytesIn);
			objectStream = new PrivateObjectInputStream(byteStream, m_classLoader);
			object											= (Serializable) objectStream.readObject();
		}
		catch (ClassNotFoundException e)
		{
			// The message for EVENT_CLASS_LOADER_CLASS_NOT_FOUND must only contain
			// the class name so that the client doesn't have to parse the
			// message for the name.
			throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_CLASS_NOT_FOUND, e.getMessage(), e);
		}
		catch (NoClassDefFoundError error)
		{
			String		className		= error.getMessage();
			className = className.replace("/", ".");
			throw new BasicException(BasicEvent.EVENT_CLASS_LOADER_CLASS_NOT_FOUND, className, error);
		}
		catch (Throwable t)
		{
			throw new BasicException(BasicEvent.EVENT_SERIAL_JAVA_DESERIAL_FAILED,
					String.format("De-serialization of %,d bytes failed.", bytesIn.length), t);
		}
		finally
		{
			BaseFileHandler.closeSafe(objectStream);
			if (null != byteStreamObject)
			{
				byteStreamObject.release();
			}
		}

		return object;
	}


	/*
	 * ObjectInputStream coerced to use an external class loader
	 *
	 */
	private static class PrivateObjectInputStream extends ObjectInputStream
	{
		private final ClassLoader		m_classLoader;

		public PrivateObjectInputStream(InputStream inputStream, ClassLoader classLoader) throws IOException
		{
			super(inputStream);

			m_classLoader = classLoader;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
		{
			// m_classLoader replaces the native call latestUserDefinedLoader()
			// from java.io.ObjectInputStream that looks up the call stack for
			// the nearest class loader.
			return Class.forName(desc.getName(), false, m_classLoader);
		}
	}

	/*
	 * Object to listen to config change for s_compressMinLength
	 */
	private static class ConfigurationChangeListener
	{
		public ConfigurationChangeListener()
		{
			updateConfiguration(ConfigurationManager.PUBLISHER_NAME, 0, null);
			PublisherManager.register(ConfigurationManager.PUBLISHER_NAME, this::updateConfiguration);
		}

		@SuppressWarnings("PMD.UnusedFormalParameter")
		private void updateConfiguration(String publisherName, long timeMS, Object event)
		{
			s_compressMinLength = ConfigurationManager.getInstance().getInt(
										SETTING_NAME_COMPRESS_MIN_LENGTH, SETTING_DEFAULT_COMPRESS_MIN_LENGTH);
		}
	}

	public static class DeflaterPooledObject extends ObjectPool.AbstractPooledObject
	{
		private Deflater		m_deflater			= new Deflater();

		public Deflater getDeflater()
		{
			return m_deflater;
		}
	}
	public static class InflaterPooledObject extends ObjectPool.AbstractPooledObject
	{
		private Inflater		m_inflater			= new Inflater();

		public Inflater getInflater()
		{
			return m_inflater;
		}
	}
	public static class ByteArrayPooledObject extends ObjectPool.AbstractPooledObject
	{
		private byte[]		m_buffer			= new byte[10 * 1024];

		public byte[] getBuffer()
		{
			return m_buffer;
		}
	}

	public static class ByteArrayInputStreamPooledObject extends ObjectPool.AbstractPooledObject
	{
		private ReusableByteArrayInputStream		m_byteStream		= new ReusableByteArrayInputStream(new byte[0]);

		public ReusableByteArrayInputStream getByteStream()
		{
			return m_byteStream;
		}
	}

	public static class ByteArrayOutputStreamPooledObject extends ObjectPool.AbstractPooledObject
	{
		private ByteArrayOutputStream		m_byteStream		= new ByteArrayOutputStream();

		public ByteArrayOutputStream getByteStream()
		{
			return m_byteStream;
		}
	}
}
