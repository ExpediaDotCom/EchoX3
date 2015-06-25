/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.dispatch.user.source;

import com.expedia.echox3.internal.transport.request.MessageType;
import com.expedia.echox3.internal.transport.request.source.AbstractSourceRequest;

/**
 * Although on the Client API, this entry point returns the list of ComputerAddress
 * to access the Admin side of the Dispatchers.
 * This is because, at startup, a new dispatcher only has access to the bootstrap information
 * which only contains the client ComputerAddress of other dispatchers.
 * This entry point allows the dispatcher to access the list of dispatchers in its zone.
 */
public abstract class UserSourceRequest extends AbstractSourceRequest
{
	private String		m_cacheName;

	public UserSourceRequest(MessageType messageType)
	{
		super(messageType);
	}

	public String getCacheName()
	{
		return m_cacheName;
	}

	public void setCacheName(String cacheName)
	{
		m_cacheName = cacheName;
	}
}
