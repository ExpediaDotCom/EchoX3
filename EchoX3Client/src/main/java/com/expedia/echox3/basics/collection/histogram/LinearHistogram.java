/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.histogram;

public class LinearHistogram extends AbstractHistogram
{
	private LinearHistogram(LinearHistogram histogram)
	{
		super(histogram);
	}

	public LinearHistogram(double min, double max, double step)
	{
		super(calculateBinGroupList(min, max, step));
	}

	private static BinGroup[] calculateBinGroupList(double min, double max, double step)
	{
		BinGroup	binGroup		= new BinGroup(min, max, step);
		BinGroup[]	binGroupList	= new BinGroup[1];
		binGroupList[0] = binGroup;

		return binGroupList;
	}

	@Override
	public IHistogram getBlankClone()
	{
		return new LinearHistogram(this);
	}
}
