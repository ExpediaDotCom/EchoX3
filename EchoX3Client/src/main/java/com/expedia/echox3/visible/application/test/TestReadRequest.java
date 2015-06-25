/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.visible.application.test;

import java.io.Serializable;

import com.expedia.echox3.basics.tools.misc.BasicTools;

public class TestReadRequest implements Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private final int			m_burnUS;		// Used to simulate CPU utilization
	private final int			m_sleepMS;		// Used to simulate delays (e.g. to test the timeout code)
	private final boolean		m_doThrow;

	public TestReadRequest(int burnUS, int sleepMS, boolean doThrown)
	{
		m_burnUS = burnUS;
		m_sleepMS = sleepMS;
		m_doThrow = doThrown;
	}

	public int getBurnUS()
	{
		return m_burnUS;
	}

	public int getSleepMS()
	{
		return m_sleepMS;
	}

	public boolean doThrow()
	{
		return m_doThrow;
	}

	public void work()
	{
		BasicTools.burnCpuUS(getBurnUS());
		BasicTools.sleepMS(getSleepMS());
		if (doThrow())
		{
			throw new IllegalStateException("Client requested an exception!");
		}
	}

	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString()
	{
		return String.format("%s(%,d us; %,d ms, %s)",
				getClass().getSimpleName(), getBurnUS(), getSleepMS(), doThrow());
	}
}
