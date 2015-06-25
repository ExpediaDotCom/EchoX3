/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.collection.simple;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.expedia.echox3.basics.file.BaseFileHandler;


/**
 * A ManagedSet is a standard Set (using TreeSet as its underlying store)
 * that can be used to understand what has changed from one state to the next.
 * The normal set method work as usual.
 *
 * New methods are added and should be used as follows...
 *
 *		managedSet.setTo(collection)
 *		Set		setAdded		= managedSet.getSetAdded();
 *		Set		setRemoved		= managedSet.getSetRemoved();
 *		...
 *		// Optionally
 *		managedSet.clearHistory();		// clears setAdded and setRemoved to free the memory
 *
 * @param <T>		The type of objects being managed by the the set (e.g. String)
 */
public class ManagedSet<T> extends TreeSet<T>
{
	public static final long	serialVersionUID				= 20150630235959L;

	private AtomicInteger			m_version					= new AtomicInteger(0);
	private final Set<T>			m_setAdded;
	private final Set<T>			m_setRemoved;
	private final Set<T>			m_setRetained;

	/**
	 * Constructs a new, empty tree set, sorted according to the
	 * natural ordering of its elements.  All elements inserted into
	 * the set must implement the {@link Comparable} interface.
	 * Furthermore, all such elements must be <i>mutually
	 * comparable</i>: {@code e1.compareTo(e2)} must not throw a
	 * {@code ClassCastException} for any elements {@code e1} and
	 * {@code e2} in the set.  If the user attempts to add an element
	 * to the set that violates this constraint (for example, the user
	 * attempts to add a string element to a set whose elements are
	 * integers), the {@code add} call will throw a
	 * {@code ClassCastException}.
	 */
	public ManagedSet()
	{
		m_setAdded			= new TreeSet<>();
		m_setRemoved		= new TreeSet<>();
		m_setRetained		= new TreeSet<>();
	}

	/**
	 * Constructs a new, empty tree set, sorted according to the specified
	 * comparator.  All elements inserted into the set must be <i>mutually
	 * comparable</i> by the specified comparator: {@code comparator.compare(e1,
	 *e2)} must not throw a {@code ClassCastException} for any elements
	 * {@code e1} and {@code e2} in the set.  If the user attempts to add
	 * an element to the set that violates this constraint, the
	 * {@code add} call will throw a {@code ClassCastException}.
	 *
	 * @param comparator the comparator that will be used to order this set.
	 *                   If {@code null}, the {@linkplain Comparable natural
	 *                   ordering} of the elements will be used.
	 */
	public ManagedSet(Comparator<? super T> comparator)
	{
		super(comparator);

		m_setAdded			= new TreeSet<>(comparator);
		m_setRemoved		= new TreeSet<>(comparator);
		m_setRetained		= new TreeSet<>(comparator);
	}

	/**
	 * Adds the specified element to this set if it is not already present.
	 * More formally, adds the specified element {@code e} to this set if
	 * the set contains no element {@code e2} such that
	 * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
	 * If this set already contains the element, the call leaves the set
	 * unchanged and returns {@code false}.
	 *
	 * @param t element to be added to this set
	 * @return {@code true} if this set did not already contain the specified
	 * element
	 * @throws ClassCastException   if the specified object cannot be compared
	 *                              with the elements currently in this set
	 * @throws NullPointerException if the specified element is null
	 *                              and this set uses natural ordering, or its comparator
	 *                              does not permit null elements
	 */
	@Override
	public boolean add(T t)
	{
		boolean		isModified		= super.add(t);
		if (isModified)
		{
			m_version.incrementAndGet();
		}
		return isModified;
	}

	/**
	 * Removes the specified element from this set if it is present.
	 * More formally, removes an element {@code e} such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>,
	 * if this set contains such an element.  Returns {@code true} if
	 * this set contained the element (or equivalently, if this set
	 * changed as a result of the call).  (This set will not contain the
	 * element once the call returns.)
	 *
	 * @param o object to be removed from this set, if present
	 * @return {@code true} if this set contained the specified element
	 * @throws ClassCastException   if the specified object cannot be compared
	 *                              with the elements currently in this set
	 * @throws NullPointerException if the specified element is null
	 *                              and this set uses natural ordering, or its comparator
	 *                              does not permit null elements
	 */
	@Override
	public boolean remove(Object o)
	{
		boolean		isModified		= super.remove(o);
		if (isModified)
		{
			m_version.incrementAndGet();
		}
		return isModified;
	}

	/**
	 * Removes all of the elements from this set.
	 * The set will be empty after this call returns.
	 */
	@Override
	public void clear()
	{
		m_version.incrementAndGet();
		super.clear();
	}

	/**
	 * Adds all of the elements in the specified collection to this set.
	 *
	 * @param c collection containing elements to be added to this set
	 * @return {@code true} if this set changed as a result of the call
	 * @throws ClassCastException   if the elements provided cannot be compared
	 *                              with the elements currently in the set
	 * @throws NullPointerException if the specified collection is null or
	 *                              if any element is null and this set uses natural ordering, or
	 *                              its comparator does not permit null elements
	 */
	@Override
	public boolean addAll(Collection<? extends T> c)
	{
		boolean		isModified		= super.addAll(c);
		if (isModified)
		{
			m_version.incrementAndGet();
		}
		return isModified;
	}

	/**
	 * In practice, equivalent to clear(), addAll(), but where the caller will be able to ask
	 * what has changed using the new methods getSetAdded() and getSetRemoved().
	 * Note that these sets can be cleared with clearHistory() or a subsequent call to setTo().
	 *
	 * @param collection		New collection of object that the set should reflect
	 */
	public void setTo(Collection<T> collection)
	{
		clearHistory();

		// New are the ones that are in the new collection, but not in current
		m_setAdded.addAll(collection);
		m_setAdded.removeAll(this);

		// Obsolete are the ones in current, but not in the new collection
		m_setRemoved.addAll(this);
		m_setRemoved.removeAll(collection);

		m_setRetained.addAll(this);
		m_setRetained.retainAll(collection);

		addAll(m_setAdded);
		removeAll(m_setRemoved);

		if (!m_setAdded.isEmpty() || !m_setRemoved.isEmpty())
		{
			m_version.incrementAndGet();
		}
	}

	public Set<T> getSetAdded()
	{
		return m_setAdded;
	}

	public Set<T> getSetRemoved()
	{
		return m_setRemoved;
	}

	public Set<T> getSetRetained()
	{
		return m_setRetained;
	}

	public String getChangeText()
	{
		StringBuilder message			= new StringBuilder(1000);
		for (T object : m_setAdded)
		{
			message.append("Added ");
			message.append(object.toString());
			message.append(BaseFileHandler.LINE_SEPARATOR);
		}
		for (T object : m_setRemoved)
		{
			message.append("Removed ");
			message.append(object.toString());
			message.append(BaseFileHandler.LINE_SEPARATOR);
		}
		return message.toString();
	}

	public void clearHistory()
	{
		m_setAdded.clear();
		m_setRemoved.clear();
		m_setRetained.clear();
	}

	public int getVersion()
	{
		return m_version.get();
	}
	public void incrementVersion()
	{
		m_version.incrementAndGet();
	}
}
