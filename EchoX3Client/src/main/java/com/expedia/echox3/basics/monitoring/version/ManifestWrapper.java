/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.version;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.expedia.echox3.basics.collection.simple.StringGroup;
import com.expedia.echox3.basics.file.BaseFileHandler;
import com.expedia.echox3.basics.file.UrlFinder;
import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.monitoring.event.BasicException;
import com.expedia.echox3.basics.tools.hash.HashUtil;
import com.expedia.echox3.basics.tools.hash.IHashProvider;
import com.expedia.echox3.basics.tools.misc.BasicTools;

// TODO Cleanup this class
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.SimpleDateFormatNeedsLocale"})
public class ManifestWrapper implements Serializable, ManifestWrapperMBean
{
	public static final long					serialVersionUID	= 20150601085959L;

	public static final String				MANIFEST_FILENAME		= "META-INF/MANIFEST.MF";

	private static final Comparator<ManifestWrapper>	COMPARATOR_BY_TITLE		=
			Comparator.comparing((ManifestWrapper w)->w.getImplementationTitle().toLowerCase(Locale.US));

	private static final String SPECIFICATION_VERSION	= "Specification-Version";
	private static final String SPECIFICATION_VENDOR	= "Specification-Vendor";
	private static final String SPECIFICATION_TITLE		= "Specification-Title";
	private static final String BUNDLE_VENDOR			= "Bundle-Vendor";
	private static final String IMPLEMENTATION_VENDOR	= "Implementation-Vendor";
	private static final String BUNDLE_NAME				= "Bundle-Name";
	private static final String IMPLEMENTATION_TITLE	= "Implementation-Title";

	private static final String[]			IMPLEMENTATION_VERSION_LIST		=
			{
					"Implementation-Version",
					"Bundle-Version",
					"version"
			};
	private static final String[]			BUILD_JDK_LIST					=
			{
					"Build-Jdk",
					"Created-By"
			};
	private static final String[]			BUILD_DATE_LIST					=
			{
					"Built-On",
					"Bnd-LastModified"
			};

	private static final SimpleDateFormat[]	DATE_TIME_FORMATTER_LIST	=
			{
					new SimpleDateFormat("yyyyMMddHHmm"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
			};

	// we need this folder separator as the URI from jar files comes with linux style folder separators and
	// BaseFileHandler.FOLDER_SEPARATOR will have an OS dependent value
	private static final String FOLDER_SEPARATOR		= "/";

	// NOTE: ALWAYS_ALLOW determined before ALWAYS_REJECT.
	private static final String[]			CONTAINS_ALWAYS_ALLOW	=
			{
//					FOLDER_SEPARATOR + "servlet-api.jar!",
					FOLDER_SEPARATOR + "rt.jar!"
			};
	private static final String[]			CONTAINS_ALWAYS_REJECT	=
			{
					// Multitude of Java JARs
					FOLDER_SEPARATOR + "jdk",
					FOLDER_SEPARATOR + "java" + FOLDER_SEPARATOR + "extensions",

					// Multitude of Tomcat JARs
					FOLDER_SEPARATOR + "bin" + FOLDER_SEPARATOR + "bootstrap",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "annotations-api.jar!",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "catalina",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "ecj",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "el-api",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "jasper",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "jsp",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "apache-tomcat-",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "tomcat-",
					FOLDER_SEPARATOR + "lib" + FOLDER_SEPARATOR + "websocket-api.jar!",
					"intellij"
			};

	private static final Map<String, ManifestWrapper>	MANIFEST_MAP		= new HashMap<>();

	private URL				m_url;
	private String			m_pathName;
	private String			m_fileName;
	private Manifest		m_manifest;
	private Attributes		m_attributes;

	static
	{
		// This block is required so instantiating the class will create the necessary MBeans.
		try
		{
			Map<String, ManifestWrapper>	map		= getManifestMapInternal();
			MANIFEST_MAP.putAll(map);
		}
		catch (BasicException e)
		{
			// Do nothing, try again later.
		}
	}

	public ManifestWrapper(URL url, Manifest manifest)
	{
		init(url, manifest);

		StringGroup		beanName		= new StringGroup(ManifestWrapper.class.getSimpleName());
		beanName.append(m_fileName);
		BasicTools.registerMBean(this, null, beanName);
	}
	private void init(URL url, Manifest manifest)
	{
		m_url			= url;
		String	name	= url.toString();
		m_pathName		= name.replace("!/META-INF/MANIFEST.MF", " ");
		//the full path is making the screen go big so moving the full path to tooltip
		m_fileName		= m_pathName.substring(m_pathName.lastIndexOf('/') + 1);
		m_manifest		= manifest;
		m_attributes	= manifest.getMainAttributes();
	}

	public static Comparator<ManifestWrapper> getComparatorByImplementationTitle()
	{
		return COMPARATOR_BY_TITLE;
	}

	public static Map<String, ManifestWrapper> getManifestMap() throws BasicException
	{
		synchronized (MANIFEST_MAP)
		{
			if (MANIFEST_MAP.isEmpty())
			{
				Map<String, ManifestWrapper>	map		= getManifestMapInternal();
				MANIFEST_MAP.putAll(map);
			}
		}

		return MANIFEST_MAP;
	}
	public static Map<String, ManifestWrapper> getManifestMapInternal() throws BasicException
	{
		Set<URL> set;
		try
		{
			set = UrlFinder.getFileUriList(MANIFEST_FILENAME);
		}
		catch (Exception e)
		{
			throw new BasicException(BasicEvent.EVENT_MANIFEST_GET_VERSION_ERROR,
					e, String.format("List of manifest files '%s' not available.", MANIFEST_FILENAME));
		}

		Map<String, ManifestWrapper> manifestMap = new TreeMap<>();
		for (URL url : set)
		{
			try
			{
				String urlName		= URLDecoder.decode(url.toString(), "UTF8");
				if (!isIncluded(urlName.toLowerCase(Locale.US)))
				{
					continue;
				}

				ManifestWrapper		wrapper		= loadManifest(url);
				manifestMap.put(url.toString(), wrapper);
			}
			catch (Exception exception)
			{
				// Nothing to do.
			}
		}
		return manifestMap;
	}
	public static ManifestWrapper loadManifest(URL url) throws BasicException
	{
		InputStream			inputStream		= null;
		Manifest			manifest;
		ManifestWrapper		wrapper			= null;
		try
		{
			inputStream		= url.openStream();
			manifest		= new Manifest(inputStream);
			wrapper			= new ManifestWrapper(url, manifest);
		}
		catch (Exception exception)
		{
			throw new BasicException(BasicEvent.EVENT_DATABASE_LOAD_MANIFEST_EXCEPTION, exception,
					"Failed to load manifest from %s", url.toString());
		}
		finally
		{
			BaseFileHandler.closeSafe(inputStream);
		}
		return wrapper;
	}

	@Override
	public URL getUrl()
	{
		return m_url;
	}

	@Override
	public String getPathName()
	{
		return m_pathName;
	}

	@Override
	public String getFileName()
	{
		return m_fileName;
	}

	private static boolean isIncluded(String urlName)
	{
		for (String text : CONTAINS_ALWAYS_ALLOW)
		{
			if (urlName.contains(text))
			{
				return true;
			}
		}
		for (String text : CONTAINS_ALWAYS_REJECT)
		{
			if (urlName.contains(text))
			{
				return false;
			}
		}
		return true;
	}

	public Manifest getManifest()
	{
		return m_manifest;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeObject(m_url);
		m_manifest.write(out);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		URL			url			= (URL) in.readObject();
		Manifest	manifest	= new Manifest(in);

		init(url, manifest);
	}

	@SuppressWarnings("PMD.UnusedPrivateMethod")        // This is the signature, as per Serializable.
	private void readObjectNoData() throws ObjectStreamException
	{
		// Nothing to do here.
	}

	public String getImplementationTitle()
	{
		Attributes attributes		= m_manifest.getMainAttributes();
		String title			= attributes.getValue(IMPLEMENTATION_TITLE);

		//choose bundle name if implementation title is null
		title = (null == title) ? attributes.getValue(BUNDLE_NAME) : title;

		//choose jar file name if bundle name is null
		title = (null == title) ? getFileName() : title;

		return title;
	}

	@Override
	public String getBuildJdk()
	{
		String buildJdk				= getAttributeValue(BUILD_JDK_LIST);
		return buildJdk;
	}

	@Override
	public long getBuildTime()
	{
		String			dateText		= getAttributeValue(BUILD_DATE_LIST);
		long			returnValue		= parseDate(dateText);
		return returnValue;
	}
	@Override
	public Date getBuildDate()
	{
		return new Date(getBuildTime());
	}

	@Override
	public String getImplementationVendor()
	{
		Attributes attributes = m_manifest.getMainAttributes();
		String vendor = attributes.getValue(IMPLEMENTATION_VENDOR);

		//choose bundle vendor if implementation vendor is null
		vendor = (null == vendor) ? attributes.getValue(BUNDLE_VENDOR) : vendor;

		return vendor;
	}

	@Override
	public String getImplementationVersion()
	{
		String version		= this.getAttributeValue(IMPLEMENTATION_VERSION_LIST);
		return version;
	}

	public String getImplementationTitleTT()
	{
		Attributes		attributes		= m_manifest.getMainAttributes();
		String			title			= attributes.getValue(SPECIFICATION_TITLE);
		StringBuilder	titleString		= new StringBuilder(100);
		titleString.append("Specification-Title = ");
		titleString.append((null != title) ? title : "");
		titleString.append("Jar file name = ");
		titleString.append(m_pathName);
		return titleString.toString();
	}

	public String getImplementationVendorTT()
	{
		Attributes attributes = m_manifest.getMainAttributes();
		String vendor = attributes.getValue(SPECIFICATION_VENDOR);
		return (null != vendor) ? SPECIFICATION_VENDOR + " = " + vendor : "";
	}

	public String getImplementationVersionTT()
	{
		Attributes attributes = m_manifest.getMainAttributes();
		String version = attributes.getValue(SPECIFICATION_VERSION);
		return (null != version) ? SPECIFICATION_VERSION + " = " + version : "";
	}

	private String getAttributeValue(String[] attributeNameList)
	{
		String attributeValue		= null;
		for (String attributeName : attributeNameList)
		{
			attributeValue		= m_attributes.getValue(attributeName);
			if (null != attributeValue)
			{
				break;
			}
		}
		return attributeValue;
	}
	private long parseDate(String text)
	{
		long		timeMS		= 0;
		for (SimpleDateFormat formatter : DATE_TIME_FORMATTER_LIST)
		{
			try
			{
				synchronized (formatter)
				{
					Date		date	= formatter.parse(text);
					timeMS			= date.getTime();
					Calendar	calendar	= Calendar.getInstance();
					calendar.setTimeInMillis(timeMS);
					int			year		= calendar.get(Calendar.YEAR);
					if (year < 2000 || year > 2100)
					{
						timeMS = 0;
						continue;
					}
				}
				break;	// Found it.
			}
			catch (Exception exception)
			{
				//Ignore and try the next parser
			}
		}
		if (0 == timeMS)
		{
			try
			{
				timeMS = Long.parseLong(text);
			}
			catch (Exception exception)
			{
				// Ignore, time to give-up
			}
		}

		return timeMS;
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
		IHashProvider hashProvider		= HashUtil.getHashProvider();
		hashProvider.add32(m_manifest.hashCode());
		hashProvider.add32(m_url.hashCode());
		int					hashCode			= hashProvider.getHashCode32();
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
		if (!(obj instanceof ManifestWrapper))
		{
			return false;
		}

		ManifestWrapper		wrapper		= (ManifestWrapper) obj;
		return m_manifest.equals(wrapper.getManifest()) && m_url.equals(wrapper.m_url);
	}

	@Override
	public String toString()
	{
		return m_url.toString();
	}
}
