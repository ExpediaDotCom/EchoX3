/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;

import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.FileFinder;
import com.expedia.echox3.basics.file.SimpleFilenameFilter;
import com.expedia.echox3.basics.monitoring.counter.IOperationContext;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicLogger;
import com.expedia.echox3.basics.thread.ThreadSchedule;
import com.expedia.echox3.basics.tools.locks.AbstractReadWriteLock;
import com.expedia.echox3.basics.tools.misc.BasicTools;
import com.expedia.echox3.basics.tools.pubsub.Publisher;
import com.expedia.echox3.basics.tools.time.TimeUnits;

public class ConfigurationManager
{
	public static final String		PUBLISHER_NAME				= ConfigurationManager.class.getSimpleName();
	public static final String		ROOT_FOLDER					= "config";
	public static final String		FILENAME_SUFFIX				= "config.properties";
	public static final SimpleFilenameFilter
									FILENAME_FILTER				= new SimpleFilenameFilter(null, null, FILENAME_SUFFIX);

	public static final String		SETTING_PREFIX				= ConfigurationManager.class.getName();
	public static final String		SETTING_NAME_ENABLED		= ".Enabled";
	public static final String		SETTING_NAME_PERIOD			= ".Period";
	public static final String		SETTING_NAME_OFFSET			= ".Offset";
	public static final String		SETTING_NAME_NUMBER			= "Number";
	public static final String		SETTING_NAME_UNITS			= "Units";

	public static final String		REASON_STARTUP_COMPLETE		= "StartupComplete";
	public static final String		REASON_PROVIDER_CHANGE		= "Change";
	public static final String		REASON_ADD_PROVIDER			= "New provider";
	public static final String		REASON_REM_PROVIDER			= "Remove provider";

	public static final String		SETTING_NAME_STARTING_POINT			= "StartingPoint";
	public static final String		SETTING_NAME_CORE_PER_INCREMENT		= "CorePerIncrement";

	private static final String		COMMA						= ",";
	private static final String		SEMICOLON					= ";";

	private static final BasicLogger			LOGGER					= new BasicLogger(ConfigurationManager.class);
	private static final ConfigurationManager	INSTANCE				= new ConfigurationManager();

	private static final String					FAILED_PARSE_FORMAT		=
			"Setting %s (value = %s) failed to parse as an %s; Using the default value of %s.";

	private static final IConfigurationProvider	WORKING_FOLDER_PROVIDER;		// Must always have highest priority!

	private final AbstractReadWriteLock				m_providerMapLock	= AbstractReadWriteLock.createReadWriteLock();
	private final Deque<IConfigurationProvider>		m_providerList		= new LinkedList<>();
	private Publisher								m_publisher;
	private long									m_lastModifiedTime	= 0;

	static
	{
		// 1. Classpath provider
		ClasspathConfigurationProvider.getInstance();

		// 2. Current directory
		String folderName	= BaseFileHandler.getCurrentFolderName()
									+ BaseFileHandler.FOLDER_SEPARATOR + ROOT_FOLDER;
		File file		= new File(folderName);
		if (file.exists() && file.isDirectory())
		{
			new FolderConfigurationProvider(file.getAbsolutePath());	// Pick-up configuration in the default folder.
		}

		// 3. HotURL
		RemoteHotUrlConfigurationProvider.getInstance();

		// 4. Local Configuration folder list
		FolderListConfigurationProvider.getInstance();

		// 5. Local Working/config folder
		String workingFolderName		= BaseFileHandler.getWorkingFolderName(ROOT_FOLDER);
		File workingFile				= new File(workingFolderName);
		IConfigurationProvider		workingProvider		= null;
		if (workingFile.exists() && workingFile.isDirectory())
		{
			workingProvider = new FolderConfigurationProvider(workingFolderName);
		}
		WORKING_FOLDER_PROVIDER = workingProvider;

		INSTANCE.m_publisher = Publisher.getPublisher(PUBLISHER_NAME);
		INSTANCE.m_publisher.post(REASON_STARTUP_COMPLETE);
	}

	private ConfigurationManager()
	{
		// private for singleton
	}

	public static ConfigurationManager getInstance()
	{
		return INSTANCE;
	}

	public static String getPublisherName()
	{
		return PUBLISHER_NAME;
	}

	public Publisher getPublisher()
	{
		return m_publisher;
	}

	public static BasicLogger getLogger()
	{
		return LOGGER;
	}

	public void addProvider(IConfigurationProvider provider)
	{
		IOperationContext		context			= getProviderMapLock().lockWrite();
		m_providerList.addFirst(provider);

		// Ensure WORKING_FOLDER_PROVIDER always has the highest priority...
		if (null != WORKING_FOLDER_PROVIDER)
		{
			m_providerList.remove(WORKING_FOLDER_PROVIDER);
			m_providerList.addFirst(WORKING_FOLDER_PROVIDER);
		}

		getProviderMapLock().unlockWrite(context, true);

		postChangeEvent(REASON_ADD_PROVIDER, provider);
	}

	public void removeProvider(IConfigurationProvider provider)
	{
		IOperationContext		context			= getProviderMapLock().lockWrite();
		m_providerList.remove(provider);
		getProviderMapLock().unlockWrite(context, true);

		postChangeEvent(REASON_REM_PROVIDER, provider);
	}

	private void calculateLastModified(String rootFolder)
	{
		Set<File> set				= FileFinder.getFilenameSet(rootFolder, null);
		long			lastModified	= 0;
		for (File file : set)
		{
			lastModified = Math.max(lastModified, file.lastModified());
		}
		m_lastModifiedTime = lastModified;
	}

	public long getLastModifiedTime()
	{
		if (0 == m_lastModifiedTime)
		{
			calculateLastModified(BaseFileHandler.getCurrentFolderName());
		}
		return m_lastModifiedTime;
	}

	public void postChangeEvent(String reason, IConfigurationProvider provider)
	{
		if (null != m_publisher)
		{
			m_publisher.post(String.format("%s from %s", reason, provider.toString()));
		}
	}

	protected AbstractReadWriteLock getProviderMapLock()
	{
		return m_providerMapLock;
	}

	public Set<String> getSettingNameSet(String prefix)
	{
		Set<String> nameSet		= new TreeSet<>();

		IOperationContext		context		= getProviderMapLock().lockRead();
		try
		{
			for (IConfigurationProvider provider : m_providerList)
			{
				Set<String> set		= provider.getSettingNameSet(prefix);
				nameSet.addAll(set);
			}
		}
		finally
		{
			getProviderMapLock().unlockRead(context, true);
		}

		return nameSet;
	}

	public List<String> getSettingValueList(String settingName)
	{
		String rawString			= getSettingInternal(settingName, null);
		if(null == rawString || rawString.isEmpty())
		{
			return null;
		}
		String			withCommas			= rawString.replaceAll(SEMICOLON, COMMA);
		String[]		pieces				= withCommas.split(COMMA);
		List<String>	settingValueList	= new ArrayList<>(pieces.length);
		for(int i = 0 ; i < pieces.length ; i++)
		{
			settingValueList.add(pieces[i].trim());
		}
		return settingValueList;
	}

	public String getSetting(String settingName, String defaultValue)
	{
		return getSettingInternal(settingName, defaultValue);
	}

	public String getSettingInternal(String settingName, String defaultValue)
	{
		String					settingValue	= null;
		IOperationContext		context			= getProviderMapLock().lockRead();
		try
		{
			settingValue = getSettingInternalSimple(settingName);

			if (null == settingValue)
			{
				String[]		settingNameParts		= settingName.split("\\.");
				for (int i = settingNameParts.length - 2; i > 0; i--)
				{
					StringBuilder sb			= new StringBuilder(settingName.length());
					for (int j = 0; j < i; j++)
					{
						sb.append(settingNameParts[j]);
						sb.append('.');
					}
					sb.append(settingNameParts[settingNameParts.length - 1]);
					settingName = sb.toString();
					settingValue = getSettingInternalSimple(settingName);
					if (null != settingValue)
					{
						break;
					}
				}
			}
		}
		finally
		{
			getProviderMapLock().unlockRead(context, true);
		}

		if (null == settingValue)
		{
			settingValue = defaultValue;
		}

		return settingValue;
	}
	public String getSettingInternalSimple(String settingName)
	{
		String settingValue		= null;

		for (IConfigurationProvider provider : m_providerList)
		{
			settingValue = provider.getSetting(settingName);
			if (null != settingValue)
			{
				break;
			}
		}

		return settingValue;
	}

	public boolean getBoolean(String settingName, boolean defaultValue)
	{
		return getBoolean(settingName, Boolean.toString(defaultValue));
	}
	public boolean getBoolean(String settingName, String defaultValue)
	{
		String settingValue		= getSetting(settingName, defaultValue);

		return "true".equalsIgnoreCase(settingValue) || "1".equals(settingValue);
	}
	public int getInt(String settingName, int defaultValue)
	{
		return getInt(settingName, Integer.toString(defaultValue));
	}
	public int getInt(String settingName, String defaultValue)
	{
		String settingValue		= getSetting(settingName, defaultValue).replace(COMMA, "");

		int			value;
		try
		{
			value = Integer.parseInt(settingValue.trim());
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_PARSE_ERROR,
					FAILED_PARSE_FORMAT, settingName, settingValue, Integer.class.getSimpleName(), defaultValue);
			value = Integer.parseInt(defaultValue);
		}
		return value;
	}
	public long getLong(String settingName, long defaultValue)
	{
		return getLong(settingName, Long.toString(defaultValue));
	}
	public long getLong(String settingName, String defaultValue)
	{
		String settingValue		= getSetting(settingName, defaultValue).replace(COMMA, "");

		long			value;
		try
		{
			value = Long.parseLong(settingValue.trim());
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_PARSE_ERROR,
					FAILED_PARSE_FORMAT, settingName, settingValue, Long.class.getSimpleName(), defaultValue);
			value = Integer.parseInt(defaultValue);
		}
		return value;
	}
	public float getFloat(String settingName, String defaultValue)
	{
		String settingValue		= getSetting(settingName, defaultValue);

		float		value;
		try
		{
			value = Float.parseFloat(settingValue.trim());
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_PARSE_ERROR,
					FAILED_PARSE_FORMAT, settingName, settingValue, Float.class.getSimpleName(), defaultValue);
			value = Integer.parseInt(defaultValue);
		}
		return value;
	}
	public double getDouble(String settingName, String defaultValue)
	{
		String settingValue		= getSetting(settingName, defaultValue);

		double			value;
		try
		{
			value = Double.parseDouble(settingValue.trim());
		}
		catch (Exception exception)
		{
			getLogger().error(BasicEvent.EVENT_CONFIGURATION_PARSE_ERROR,
					FAILED_PARSE_FORMAT, settingName, settingValue, Double.class.getSimpleName(), defaultValue);
			value = Integer.parseInt(defaultValue);
		}
		return value;
	}

	public long getTimeInterval(String settingPrefix, String defaultNumber, String defaultUnits)
	{
		String numberName		= settingPrefix + SETTING_NAME_NUMBER;
		String unitsName		= settingPrefix + SETTING_NAME_UNITS;

		long		number			= getLong(numberName, defaultNumber);
		String		units			= getSetting(unitsName, defaultUnits);

		long		timeMS			= TimeUnits.getTimeMS(number, units);
		return timeMS;
	}

	public ThreadSchedule getThreadSchedule(String settingPrefix, ThreadSchedule defaultSchedule)
	{
		String enabledName		= settingPrefix + SETTING_NAME_ENABLED;
		String periodName		= settingPrefix + SETTING_NAME_PERIOD;
		String offsetName		= settingPrefix + SETTING_NAME_OFFSET;

		boolean		isEnabled		= getBoolean(enabledName, Boolean.toString(defaultSchedule.isEnabled()));
		long		periodNumber	= getLong(periodName + SETTING_NAME_NUMBER,
																Long.toString(defaultSchedule.getPeriodNumber()));
		String periodUnits		= getSetting(periodName + SETTING_NAME_UNITS, defaultSchedule.getPeriodUnits());
		long		offsetNumber	= getLong(offsetName + SETTING_NAME_NUMBER,
				Long.toString(defaultSchedule.getOffsetNumber()));
		String offsetUnits		= getSetting(offsetName + SETTING_NAME_UNITS, defaultSchedule.getOffsetUnits());

		return new ThreadSchedule(isEnabled, periodNumber, periodUnits, offsetNumber, offsetUnits);
	}

	public Level getLoggingLevel(String settingName, Level level)
	{
		String text		= getSetting(settingName, level.toString());
		if (null != text)
		{
			level = Level.toLevel(text, level);
		}
		return level;
	}

	public int getSelfTuningInteger(String settingPrefix, String defaultStarting, String defaultCorePerIncrement)
	{
		String		startingName	= settingPrefix + SETTING_NAME_STARTING_POINT;
		String		corePerName		= settingPrefix + SETTING_NAME_CORE_PER_INCREMENT;

		int			starting		= getInt(startingName, defaultStarting);
		int			corePer			= getInt(corePerName, defaultCorePerIncrement);
		int			value			= starting + (BasicTools.getNumberOfProcessors() / corePer);

		return value;
	}

	public static class SelfTuningInteger
	{
		private static final int		CORE_COUNT		= BasicTools.getNumberOfProcessors();

		private final String	m_settingNameStarting;
		private final String	m_settingNameIncrement;
		private int				m_startingValue;
		private int				m_corePerIncrement;
		private int				m_currentValue;

		public SelfTuningInteger(String settingPrefix)
		{
			m_settingNameStarting	= settingPrefix + SETTING_NAME_STARTING_POINT;
			m_settingNameIncrement	= settingPrefix + SETTING_NAME_CORE_PER_INCREMENT;

			updateConfiguration();
		}

		/**
		 * Picks-up change (if any) from configuration.
		 *
		 * @return		true if the value has changed.
		 */
		public final boolean updateConfiguration()
		{
			m_startingValue		= ConfigurationManager.getInstance().getInt(m_settingNameStarting, m_startingValue);
			m_corePerIncrement	= ConfigurationManager.getInstance().getInt(m_settingNameIncrement, m_corePerIncrement);
			int		current		= m_startingValue + (0 == m_corePerIncrement ? 0 : (CORE_COUNT / m_corePerIncrement));
			if (current != m_currentValue)
			{
				m_currentValue = current;
				return true;
			}
			else
			{
				return false;
			}
		}

		public int getCurrentValue()
		{
			return m_currentValue;
		}

		public int getStartingValue()
		{
			return m_startingValue;
		}

		public int getCorePerIncrement()
		{
			return m_corePerIncrement;
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
			return String.format("%,d + %,d cores / %,d = %,d",
					m_startingValue, CORE_COUNT, m_corePerIncrement, m_currentValue);
		}
	}
}
