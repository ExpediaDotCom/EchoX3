/**
 * Copyright 2015 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.thread;

import java.util.concurrent.Executor;

/**
 * A ITrellisThreadPool is an Executor with some visibility on what is going on inside ...
 * ... and that can be terminated.
 * If a new/different kind of Executor is needed, it should be simple to wrap it
 * in something that has these simple methods.
 */
public interface IEchoThreadPool extends Executor
{
	String		getName();

	int			getPoolSizeMax();
	int			getQueueSizeMax();
	int			getQueueSize();

	void		shutdown();
}
