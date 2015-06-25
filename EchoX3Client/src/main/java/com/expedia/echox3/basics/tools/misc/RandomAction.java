/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
// Expedia code copied/used with permission from original author/copyright owner:
/**
 * Copyright (c) 2014 LogicielCote.COM All rights reserved.
 *
 * @Author  <mailto:Pierre@LogicielCote.COM>Pierre Cote</mailto>
 */

package com.expedia.echox3.basics.tools.misc;

import java.util.Arrays;
import java.util.Random;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.configuration.ConfigurationManager;
import com.expedia.echox3.basics.monitoring.counter.BaseCounter;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.basics.tools.time.WallClock.FormatType;
import com.expedia.echox3.basics.tools.time.WallClock.FormatSize;


/**
 * Reads the odds of each action from setting String.format("%s.%s", settingPrefix, randomAction.toString())
 * for each name in the nameList.
 *
 * Note that the actionList is a list of "Object", the toString method is called on them.
 * For example, the actual list could be an enum, and enum.values() could be passed as the actionList.
 *
 * The odds are interpreted as "Number of chances" of this action, out of sum of the chance for all the action.
 * For example:
 *
 * enum MyAction
 * {
 *     head,
 *     tail,
 *     side
 * }
 *
 * with configuration
 * experiment.head=499,999
 * experiment.tail=499,999
 * experiment.side=2
 *
 * will result in about 50/50 changes of head/tail, and 2 in a million change of the coin falling on its side.
 *
 * The typical call is getActionIndex(),
 * returning an random index into actionList at the appropriate probability level.
 * Other utility methods are available.
 *
 * The user is responsible for calling updateConfiguration!!!
 * This is to ensure the configuration does not change in the middle of a caller's experiment
 *
 */
public class RandomAction extends BaseCounter implements RandomActionMBean
{
	private final Random		m_random			= new Random();	// independent, randomly seeded generator
	private final String		m_settingPrefix;
	private final String[]		m_nameList;
	private final int[]			m_oddsList;
	private int					m_oddsTotal;
	private final int[]			m_countList;
	private int					m_countTotal;
	private long				m_latchedTime;
	private final int[]			m_countListLatched;
	private int					m_countLatched;

	public RandomAction(String name, Object[] actionList)
	{
		super(new StringGroup(new String[] { RandomAction.class.getSimpleName(), name }), CounterVisibility.JMX);

		m_settingPrefix = RandomAction.class.getName() + "." + name;
		m_nameList = new String[actionList.length];
		for (int i = 0; i < actionList.length; i++)
		{
			m_nameList[i] = actionList[i].toString();
		}
		m_oddsList			= new int[m_nameList.length];
		m_countList			= new int[m_nameList.length];
		m_countListLatched	= new int[m_nameList.length];

		updateConfiguration();

		addCounter(getName().toString(), this);
	}

	public Random getRandom()
	{
		return m_random;
	}

	@Override
	public void doBeanUpdate(long durationMS)
	{
		synchronized (m_oddsList)
		{
			System.arraycopy(m_countList, 0, m_countListLatched, 0, m_countListLatched.length);
			Arrays.fill(m_countList, 0);
			m_countLatched = m_countTotal;
			m_countTotal = 0;
		}
		m_latchedTime = WallClock.getCurrentTimeMS();
	}

	@Override
	public void doLogUpdate(BasicLogger logger, long durationMS)
	{
		// Do nothing
	}

	@Override
	public final void updateConfiguration()
	{
		// NOTE: Odds of [0] (== Merged) are always 0.0 %.
		synchronized (m_oddsList)
		{
			int		sum		= 0;
			for (int i = 0; i < m_nameList.length; i++)
			{
				String		settingName		= String.format("%s.%s", m_settingPrefix, m_nameList[i]);
				m_oddsList[i] = ConfigurationManager.getInstance().getInt(settingName, "0");
				sum += m_oddsList[i];
			}
			if (0 == sum)
			{
				for (int i = 0; i < m_nameList.length; i++)
				{
					m_oddsList[i] = 1;
				}
				sum = m_nameList.length;
			}
			m_oddsTotal = sum;
		}

		super.updateConfiguration();
	}

	public String getSettingPrefix()
	{
		return m_settingPrefix;
	}

	public double getOddsPercent(int index)
	{
		return m_oddsList[index] * 100.0 / m_oddsTotal;
	}

	public String getRandomActionName()
	{
		return m_nameList[getRandomAction()];
	}
	public int getRandomAction()
	{
		int			action	= 0;
		synchronized (m_oddsList)
		{
			int			odds		= getRandom().nextInt(m_oddsTotal);
			while (true)
			{
				if (m_oddsList[action] > odds)
				{
					break;
				}
				odds -= m_oddsList[action++];
			}
		}
		m_countList[action]++;
		m_countTotal++;
		return action;
	}

	public String[] getStatistics()
	{
		String[]		countList		= new String[m_nameList.length + 1];
		countList[0] = WallClock.formatTime(FormatType.DateTime, FormatSize.Medium, m_latchedTime);
		synchronized (m_oddsList)
		{
			for (int i = 0; i < m_nameList.length; i++)
			{
				double		odds		= m_oddsList[i] * 100.0 / m_oddsTotal;
				double		actual		= m_countListLatched[i] * 100.0 / m_countLatched;
				countList[i + 1] = String.format("%-15s: %,7d %5.1f%% (%5.1f%%)\n",
						m_nameList[i], m_countListLatched[i], actual, odds);
			}
		}
		return countList;
	}
}
