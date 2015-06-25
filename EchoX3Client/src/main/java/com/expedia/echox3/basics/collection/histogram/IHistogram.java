/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.histogram;

import java.util.HashMap;
import java.util.List;

import com.expedia.echox3.basics.tools.hash.FnvHash;

public interface IHistogram
{
	void			record(double value);
	void 			record(IHistogram histogram);
	BinData			getWaterMarkValue(double wmLevelFraction);

	IHistogram		getBlankClone();
	void			reset();
	IHistogram		cloneAndReset();
	void			cloneAndReset(IHistogram histogram);

	double			getMinStep();
	double			getMaxValue();
	int				getBinCount();
	int				getBinValue(int bin);
	int				getCount();
	List<BinData>	getBinData();

	class BinGroup
	{
		double			m_min;		// inclusive, min bin includes all values less than ...
		double			m_max;		// exclusive; max bin includes all values higher than ...
		double			m_step;
		int				m_count;	// Number of bins

		public BinGroup(double min, double max, double step)
		{
			m_min = min;
			m_max = max;
			m_step = step;
			m_count = Double.valueOf(Math.ceil((max - min) / step)).intValue();
		}

		public double getMin()
		{
			return m_min;
		}

		public double getMax()
		{
			return m_max;
		}

		public double getStep()
		{
			return m_step;
		}

		public int getCount()
		{
			return m_count;
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
			if (!(obj instanceof BinGroup))
			{
				return false;
			}

			BinGroup		binGroup		= (BinGroup) obj;
			if (m_min != binGroup.m_min)
			{
				return false;
			}
			if (m_max != binGroup.m_max)
			{
				return false;
			}
			if (m_count != binGroup.m_count)
			{
				return false;
			}
			return true;
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
			FnvHash		hash		= FnvHash.get();

			// Java 1.8
			hash.add32(Double.hashCode(m_min));
			hash.add32(Double.hashCode(m_max));

			// Java 1.7
//			hash.add32(Double.valueOf(m_min).hashCode());
//			hash.add32(Double.valueOf(m_max).hashCode());

			hash.add32(m_count);

			int			hashCode	= hash.getHashCode32();
			hash.release();

			return hashCode;
		}

		@Override
		public String toString()
		{
			return String.format("%6.1e - %6.1e by %6.1e (%2d steps)", getMin(), getMax(), getStep(), getCount());
		}
	}

	class BinData
	{
		double			m_min;		// exclusive, min bin includes all values less than ...
		double			m_max;		// Inclusive; max bin includes all values higher than ...
		int				m_count;	// Count of hits on this bin

		public BinData(double min, double max, int count)
		{
			m_min = min;
			m_max = max;
			m_count = count;
		}

		public double getMin()
		{
			return m_min;
		}

		public double getMax()
		{
			return m_max;
		}

		public int getCount()
		{
			return m_count;
		}

		public String toTextString()
		{
			return String.format("[%f, %f[ = %,d", getMin(), getMax(), getCount());
		}

		@Override
		public String toString()
		{
			return String.format("%6.1e_%6.1e=%d", getMin(), getMax(), getCount());
		}
	}
}
