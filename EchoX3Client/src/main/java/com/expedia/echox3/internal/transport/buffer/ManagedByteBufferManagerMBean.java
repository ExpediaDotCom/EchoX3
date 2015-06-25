/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.buffer;

import java.util.List;

/**
 * Created by Pierre on 11/10/14.
 */
public interface ManagedByteBufferManagerMBean
{
	List<String>		getBinDataExact();
	List<String>		getBinDataLadder();
	List<String>		getBinDataCustom();

	void				resetHistogram();
}
