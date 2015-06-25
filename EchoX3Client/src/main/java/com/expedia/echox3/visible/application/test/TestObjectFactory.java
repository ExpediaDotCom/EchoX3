/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.test;

import java.io.IOException;
import java.io.Serializable;

import com.expedia.echox3.visible.trellis.ICacheObject;
import com.expedia.echox3.visible.trellis.IObjectFactory;
import com.expedia.echox3.visible.trellis.ObjectCacheConfiguration;

public class TestObjectFactory implements IObjectFactory
{
	@Override
	public void updateConfiguration(ObjectCacheConfiguration configuration)
	{
		// Nothing to read, this is a trivial Factory/object
	}

	@Override
	public ICacheObject createObject()
	{
		return new TestObject();
	}

	@Override
	public ICacheObject createObject(Serializable key)
	{
		return new TestObject();
	}

	/**
	 * Closes this stream and releases any system resources associated
	 * with it. If the stream is already closed then invoking this
	 * method has no effect.
	 * <p>
	 * <p> As noted in {@link AutoCloseable#close()}, cases where the
	 * close may fail require careful attention. It is strongly advised
	 * to relinquish the underlying resources and to internally
	 * <em>mark</em> the {@code Closeable} as closed, prior to throwing
	 * the {@code IOException}.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close()
	{
		// Nothing to do.
	}
}
