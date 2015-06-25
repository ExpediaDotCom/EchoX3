/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.internal.transport.socket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import com.expedia.echox3.basics.collection.simple.ManagedSet;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;

public class ComputerAddressGroup
{
	private static final BasicLogger			LOGGER					= new BasicLogger(ComputerAddressGroup.class);
	private final String						m_groupName;
	private volatile int						m_laneCount				= 0;
	private final ManagedSet<ComputerAddress>	m_addressSet			= new ManagedSet<>();

	public ComputerAddressGroup(String groupName)
	{
		m_groupName = groupName;
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public String getGroupName()
	{
		return m_groupName;
	}

	public int getVersion()
	{
		return m_addressSet.getVersion();
	}
	public void incrementVersion()
	{
		m_addressSet.incrementVersion();
	}

	public Set<ComputerAddress> getAddressSet()
	{
		return m_addressSet;
	}

	public int getLaneCount()
	{
		return m_laneCount;
	}

	public void setAddressList(Collection<ComputerAddress> addressSet)
	{
		m_addressSet.setTo(addressSet);
		updateLaneCount();
	}
	public boolean addAddress(ComputerAddress address)
	{
		boolean		isModified		= m_addressSet.add(address);
		updateLaneCount();
		return isModified;
	}
	public boolean removeAddress(ComputerAddress address)
	{
		boolean		isModified		= m_addressSet.remove(address);
		updateLaneCount();
		return isModified;
	}
	private void updateLaneCount()
	{
		int		count		= 0;
		for (ComputerAddress address : m_addressSet)
		{
			count += address.getLaneCount();
		}
		m_laneCount = count;
	}

	public void close()
	{
		for (ComputerAddress address : m_addressSet)
		{
			removeAddress(address);
		}
	}

	/**
	 * Returns a hash code value for the object. This method is
	 * supported for the benefit of hash tables such as those provided by
	 * {@link HashMap}.
	 * <p>
	 * The general contract of {@code hashCode} is:
	 * <ul>
	 * <li>Whenever it is invoked on the same object more than once during
	 * an execution of a Java application, the {@code hashCode} method
	 * must consistently return the same integer, provided no information
	 * used in {@code equals} comparisons on the object is modified.
	 * This integer need not remain consistent from one execution of an
	 * application to another execution of the same application.
	 * <li>If two objects are equal according to the {@code equals(Object)}
	 * method, then calling the {@code hashCode} method on each of
	 * the two objects must produce the same integer result.
	 * <li>It is <em>not</em> required that if two objects are unequal
	 * according to the {@link Object#equals(Object)}
	 * method, then calling the {@code hashCode} method on each of the
	 * two objects must produce distinct integer results.  However, the
	 * programmer should be aware that producing distinct integer results
	 * for unequal objects may improve the performance of hash tables.
	 * </ul>
	 * <p>
	 * As much as is reasonably practical, the hashCode method defined by
	 * class {@code Object} does return distinct integers for distinct
	 * objects. (This is typically implemented by converting the internal
	 * address of the object into an integer, but this implementation
	 * technique is not required by the
	 * Java&trade; programming language.)
	 *
	 * @return a hash code value for this object.
	 * @see Object#equals(Object)
	 * @see System#identityHashCode
	 */
	@Override
	public int hashCode()
	{
		int		hash		= m_groupName.hashCode();
		return hash;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see HashMap
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ComputerAddressGroup)
		{
			return m_groupName.equals(((ComputerAddressGroup)obj).m_groupName);
		}
		else
		{
			return false;
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
		return String.format("%s (%,d address, %,d lanes)", m_groupName, m_addressSet.size(), m_laneCount);
	}
}
