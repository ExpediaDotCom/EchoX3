/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */
package com.expedia.echox3.basics.tools.time;

import java.util.Date;

public interface WallClockMBean
{
	long	getLocalTime();
	String getLocalDate();

	long	getOffsetMS();
	long	getCorrectedTime();
	String getCorrectedDate();

	long	getMeasuredTime();
	Date getMeasuredDate();
	String getMasterName();
}
