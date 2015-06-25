/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.histogram;

import java.util.LinkedList;
import java.util.List;

public class LogarithmicHistogram extends AbstractHistogram
{
	public enum Precision
	{
		Coarse,
		Normal,
		Fine
	}

	private static final BinGroup[][]		BIN_GROUP_LIST		= new BinGroup[Precision.values().length][];

	static
	{
		BIN_GROUP_LIST[Precision.Coarse.ordinal()] = new BinGroup[3];
		BIN_GROUP_LIST[Precision.Coarse.ordinal()][0] = new BinGroup(1.0, 2.0, 0.20);
		BIN_GROUP_LIST[Precision.Coarse.ordinal()][1] = new BinGroup(2.0, 5.0, 0.50);
		BIN_GROUP_LIST[Precision.Coarse.ordinal()][2] = new BinGroup(5.0, 10., 1.00);

		BIN_GROUP_LIST[Precision.Normal.ordinal()] = new BinGroup[3];
		BIN_GROUP_LIST[Precision.Normal.ordinal()][0] = new BinGroup(1.0, 2.0, 0.05);
		BIN_GROUP_LIST[Precision.Normal.ordinal()][1] = new BinGroup(2.0, 5.0, 0.10);
		BIN_GROUP_LIST[Precision.Normal.ordinal()][2] = new BinGroup(5.0, 10., 0.20);

		BIN_GROUP_LIST[Precision.Fine.ordinal()] = new BinGroup[3];
		BIN_GROUP_LIST[Precision.Fine.ordinal()][0] = new BinGroup(1.0, 2.0, 0.01);
		BIN_GROUP_LIST[Precision.Fine.ordinal()][1] = new BinGroup(2.0, 5.0, 0.02);
		BIN_GROUP_LIST[Precision.Fine.ordinal()][2] = new BinGroup(5.0, 10., 0.05);
	}

	private LogarithmicHistogram(LogarithmicHistogram histogram)
	{
		super(histogram);
	}

	public LogarithmicHistogram(double minStep, double maxValue, Precision precision)
	{
		super(calculateBinGroupList(minStep, maxValue, precision));
	}
	private static BinGroup[] calculateBinGroupList(double minStep, double maxValue, Precision precision)
	{
		if (minStep > maxValue)
		{
			throw new IllegalArgumentException(
					String.format("minStep (%f) must be smaller than maxValue (%f)!", minStep, maxValue));
		}

		BinGroup[]		templateList		= BIN_GROUP_LIST[precision.ordinal()];
		double			minMultiplier		= calculateMinMultiplier(templateList[0].getStep(), minStep);
		int				firstIndex			= findFirstIndex(minStep, templateList, minMultiplier);
		BinGroup[]		binGroupList		= buildBinGroups(templateList, firstIndex, minMultiplier, maxValue);

		return binGroupList;
	}

	@SuppressWarnings("PMD.UnusedPrivateMethod")
	private static double calculateMinMultiplier(double step, double minStep)
	{
		double		multiplier		= 1.0;

		// Make sure multiplier is greater than minStep
		while (multiplier * step < minStep)
		{
			multiplier *= 10.;
		}

		// Make multiplier be 1.0En, where n is the highest possible value with multiplier >= minStep
		while (multiplier * step > minStep)
		{
			multiplier /= 10.0;
		}

		return multiplier;
	}
	private static int findFirstIndex(double minStep, BinGroup[] binGroupList, double multiplier)
	{
		int		iStep		= 0;

		for (int i = 0; i < binGroupList.length; i++)
		{
			BinGroup		binGroup	= binGroupList[i];
//			double			min			= binGroup.getMin() * multiplier;
//			double			max			= binGroup.getMax() * multiplier;
			double			step		= binGroup.getStep() * multiplier;
			if (minStep >= step)
			{
				iStep = i;
			}
		}

		return iStep;
	}
	private static BinGroup[] buildBinGroups(BinGroup[] binGroupList, int index, double multiplier, double maxValue)
	{
		List<BinGroup> list		= new LinkedList<>();

		double		value		= maxValue - 1;		// Ensure at least one BinGroup
		while (value < maxValue)
		{
			double			min				= binGroupList[index].getMin() * multiplier;
			double			max				= binGroupList[index].getMax() * multiplier;
			double			step			= binGroupList[index].getStep() * multiplier;
			BinGroup		binGroup		= new BinGroup(min, max, step);
			list.add(binGroup);
			value = max;

			index++;
			if (index == binGroupList.length)
			{
				index = 0;
				multiplier *= 10;
			}
		}

		// Turn the list into an array ... ensure the list is NOT kept
		// The first BinGroup cover 0 -> The min of the first BinGroup found, using the same step.
		BinGroup[]		array		= new BinGroup[list.size() + 1];
		int				i			= 0;
		BinGroup		binGroup1	= list.get(0);
		array[i++] = new BinGroup(0.0, binGroup1.getMin(), binGroup1.getStep());
		for (BinGroup binGroup : list)
		{
			array[i++] = binGroup;
		}

		return array;
	}


	@Override
	public IHistogram getBlankClone()
	{
		return new LogarithmicHistogram(this);
	}
}
