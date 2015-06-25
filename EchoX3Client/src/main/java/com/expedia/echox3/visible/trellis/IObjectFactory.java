/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.trellis;

import java.io.Serializable;

/**
 * The object factory is used to create blank object when a write operation (e.g. ICacheObject.writeOnly)
 * is sent to a key for which there is no object. In this situation, a blank object is created using the factory.
 */
public interface IObjectFactory
{
	/**
	 * Called on the Factory object to inform it of the new configuration.
	 * This is called when the factory object is first created and when the configuration changes.
	 * The factory is responsible for passing the configuration to objects it creates;
	 * The EchoX3 code will update the objects when the configuration changes later.
	 *
	 * In addition to the standard configuration settings required by EchoX3,
	 * the cache object may read properties from the configuration's properties.
	 * There is optimization to ensure the methods getInt(), getFloat(), ...
	 * do not require string parsing on every call.
	 *
	 * @param configuration		The newly (probably) changed configuration object
	 */
	void updateConfiguration(ObjectCacheConfiguration configuration);

	/**
	 * The method createObject is called when a write request is processed for an object that is not available.
	 * A new (blank) object is created via the factory.
	 * This method is called first. If all the objects in the cache are of the same type, it can return an object.
	 * However, if objects can be of multiple types and the key is required to determine
	 * the type of object to create, this method should return null.
	 * In this situation, createObject(key) will be called. The draw-back is that de-serializing the key
	 * is slower. Therefore, it should be done only when necessary.w
	 *
	 * @return		A standard cache object or null if the factory requires a key to create a new object
	 */
	ICacheObject createObject();

	/**
	 * The method createObject is called when a write request is processed for an object that is not available
	 * AND the object was not created with createObject(<Parameterless>).
	 * Creates an object given the (de-serialized) key to help determine the type of object to create.
	 * This method is called when a w
	 *
	 * @param key	The de-serialized key to the object being created
	 * @return		A standard cache object or null if the factory requires a key to create a new object
	 */
	ICacheObject createObject(Serializable key);

	/**
	 * Called when the factory is closed (deleted).
	 * This may occur when the cache is closed or when the factory changes.
	 * The factory should clean-up after itself (e.g. de-register JMX objects, close files)
	 */
	void					close();
}
