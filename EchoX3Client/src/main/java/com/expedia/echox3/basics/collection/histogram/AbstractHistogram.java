/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.collection.histogram;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.hash.IHashProvider;

public abstract class AbstractHistogram implements IHistogram
{
	private static final int	ZERO_HASH_CODE		= "Zero".hashCode();

	private double				m_minStep			= 0.0;
	private double				m_maxValue			= 0.0;
	private final BinGroup[]	m_binGroupList;
	private int[]				m_binList;
	private int					m_count				= 0;

	// Normal constructor, extending class will have calculated/constructed an appropriate BinGroup[]
	protected AbstractHistogram(BinGroup[] binGroupList)
	{
		m_binGroupList = binGroupList;

		int			binCount		= 0;
		for (BinGroup binGroup : m_binGroupList)
		{
			binCount += binGroup.getCount();
		}
		m_binList = new int[binCount];

		m_minStep = m_binGroupList[0].getStep();
		m_maxValue = m_binGroupList[m_binGroupList.length - 1].getMax();
	}

	// Constructor for getBlankClone(), inherits the exact m_binGroupList of the parent.
	protected AbstractHistogram(AbstractHistogram histogram)
	{
		this(histogram.m_binGroupList);
	}

	@Override
	public int getBinCount()
	{
		return m_binList.length;
	}
	@Override
	public int getBinValue(int bin)
	{
		return m_binList[bin];
	}

	@Override
	public int getCount()
	{
		return m_count;
	}

	@Override
	public double getMinStep()
	{
		return m_minStep;
	}

	@Override
	public double getMaxValue()
	{
		return m_maxValue;
	}

	@Override
	public void record(double value)
	{
		m_binList[getBinIndex(value)]++;
		m_count++;
	}
	protected int getBinIndex(double value)
	{
		int		binIndex		= 0;
		for (BinGroup binGroup : m_binGroupList)
		{
			if (binGroup.getMin() > value)
			{
				break;
			}
			else if (binGroup.getMax() < value)
			{
				binIndex += binGroup.getCount();
			}
			else
			{
				binIndex += ((value - binGroup.getMin()) / binGroup.getStep());
				break;
			}
		}
		binIndex = Math.min(binIndex, getBinCount() - 1);

		return binIndex;
	}

	public BinData getWaterMarkValue(double wmLevelFraction)
	{
		wmLevelFraction = Math.max(0.0, wmLevelFraction);
		wmLevelFraction = Math.min(1.0, wmLevelFraction);

		int		targetCount		= Double.valueOf((wmLevelFraction * m_count) + 0.5).intValue();

		// Find the bin with the measurement making the water mark...
		int		runningCount	= 0;
		int		targetBin		= -1;
		for (int i = 0; i < m_binList.length; i++)
		{
			runningCount += m_binList[i];
			if (runningCount >= targetCount)
			{
				targetBin = i;
				break;
			}
		}
		if (-1 == targetBin)
		{
			targetBin = m_binList.length - 1;
		}

		// Determine the max value of that bin
		int			binCount		= 0;
		BinData		wmData			= null;
		for (BinGroup binGroup : m_binGroupList)
		{
			if (binCount + binGroup.getCount() > targetBin)
			{
				// Found it: The bin is in this group.
				double		wmMin		= binGroup.getMin() + ((targetBin - binCount) * binGroup.getStep());
				double		wmMax		= wmMin + binGroup.getStep();		// To be at the top of the bin!
				wmData = new BinData(wmMin, wmMax, m_binList[targetBin]);
				break;
			}
			else
			{
				// The bin is not in this group...
				binCount += binGroup.getCount();
			}
		}

		return wmData;
	}

	@Override
	public void record(IHistogram histogram)
	{
		if (m_binList.length != histogram.getBinCount() || !(histogram instanceof AbstractHistogram))
		{
			throw new IllegalArgumentException("BinCount must be the same && histogram must be an AbstractHistogram");
		}

		AbstractHistogram		abstractHistogram		= (AbstractHistogram) histogram;
		for (int i = 0; i < m_binList.length; i++)
		{
			// write only the non-zero values
			int		binValue		= abstractHistogram.m_binList[i];
			if (0 != binValue)
			{
				m_binList[i] += binValue;
				m_count += binValue;
			}
		}
	}

	@Override
	public void reset()
	{
		Arrays.fill(m_binList, 0);
		m_count = 0;
	}

	@Override
	public IHistogram cloneAndReset()
	{
		AbstractHistogram		histogram		= (AbstractHistogram) getBlankClone();
		cloneAndReset(histogram);
		return histogram;
	}

	@Override
	public void cloneAndReset(IHistogram histogramIn)
	{
		AbstractHistogram		histogram		= (AbstractHistogram) histogramIn;

		histogram.m_count = m_count;

		// Swap the m_binList to give the current array to the clone histogram.
		int[]		temp		= histogram.m_binList;
		histogram.m_binList = m_binList;
		m_binList = temp;
		reset();
	}

	@Override
	public List<BinData> getBinData()
	{
		List<BinData>		list		= new LinkedList<>();
		int					iBin		= 0;

		for (BinGroup binGroup : m_binGroupList)
		{
			double		min		= binGroup.getMin();
			double		max		= min + binGroup.getStep();
			for (int i = 0; i < binGroup.getCount(); i++)
			{
				int			count		= m_binList[iBin++];
				if (0 != count)
				{
					BinData		data		= new BinData(min, max, count);
					list.add(data);
				}

				min = max;
				max += binGroup.getStep();
			}
		}

		// Ensure the last bin is always present, so analysing code knows where the maximum is...
		int				count			= m_binList[m_binList.length - 1];
		if (0 == count)
		{
			// The last bin was NOT included in the loop above, include it manually at the end...
			BinGroup		binGroup		= m_binGroupList[m_binGroupList.length - 1];
			double			max				= binGroup.getMax();
			double			min				= max - binGroup.getStep();
			BinData			data			= new BinData(min, max, 0);
			list.add(data);
		}

		return list;
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
		IHashProvider		hashProvider		= HashUtil.getHashProvider();

		for (int i = 0; i < m_binGroupList.length; i++)
		{
			hashProvider.add32(m_binGroupList[i].hashCode());
		}

		for (int i = 0; i < m_binList.length; i++)
		{
			if (0 != m_binList[i])
			{
				hashProvider.add32(m_binList[i]);
			}
			else
			{
				hashProvider.add32(ZERO_HASH_CODE);		// The formula does not like many 0 in a row :(
			}
		}

		int		hashCode	= hashProvider.getHashCode32();
		hashProvider.release();

		return hashCode;
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
		if (!(obj instanceof AbstractHistogram))
		{
			return false;
		}

		AbstractHistogram		histogram		= (AbstractHistogram) obj;
		if (m_binGroupList.length != histogram.m_binGroupList.length)
		{
			return false;
		}
		for (int iBinGroup = 0; iBinGroup < m_binGroupList.length; iBinGroup++)
		{
			if (!m_binGroupList[iBinGroup].equals(histogram.m_binGroupList[iBinGroup]))
			{
				return false;
			}
		}
		if (m_binList.length != histogram.m_binList.length)
		{
			return false;
		}
		for (int i = 0; i < m_binList.length; i++)
		{
			if (m_binList[i] != histogram.m_binList[i])
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("%s(Step = %6.1e; Max = %6.1e) = %,d points over %,d bins (%,d groups)",
				getClass().getSimpleName(), getMinStep(), getMaxValue(),
				getCount(), getBinCount(), m_binGroupList.length);
	}
}
