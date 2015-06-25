/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport;

import org.junit.Test;
import static org.junit.Assert.*;

import com.expedia.echox3.basics.AbstractTestTools;
import com.expedia.echox3.internal.transport.socket.ComputerAddress;

public class MiscTransportTests extends AbstractTestTools
{
	@Test
	public void testComputerAddress() throws Exception
	{
		logTestName();

		ComputerAddress			address			= new ComputerAddress(getClass().getName());
		address.resolve();
		assertNotNull(address);
		validateNotNull(address.getDisplayName());
		validateNotNull(address.getUniqueName());
		validateNotNull(address.toString());
	}
	private void validateNotNull(String text)
	{
		assertNotNull(text);
	}
}
