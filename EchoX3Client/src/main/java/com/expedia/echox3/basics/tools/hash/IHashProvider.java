/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.tools.hash;

public interface IHashProvider
{
	void	release();

	void	add32(byte[] byteList, int indexMin, int length);
	void	add32(int n);
	void	add32(long n);
	int		getHashCode32();

	void	add64(byte[] byteList, int indexMin, int length);
	void	add64(int n);
	void	add64(long n);
	long	getHashCode64();
}
