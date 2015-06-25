/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.simple;

public interface IAccumulator
{
	void			record(double value);
	void 			record(IAccumulator accumulator);

	IAccumulator	getBlankClone();
	void			reset();
	IAccumulator	cloneAndReset();
	void			cloneAndReset(IAccumulator accumulator);

	int				getCount();
	double			getMin();
	double			getMax();
	double			getAvg();
	double			getStd();
	double			getSum();
}
