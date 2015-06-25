/**
 * Copyright 2011-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.time;

import java.util.Calendar;

public class SntpMessage
{
	public static final int			MAXIMUM_LENGTH				= 384;	// without authentication.

	private static final long		MILLI_SECONDS_1900_1970;

	private byte		m_leapIndicator;
	private byte		m_versionNumber			= 4;
	private byte		m_mode					= 3;
	private byte		m_stratum;
	private byte		m_pollInterval;
	private byte		m_precision;
	private double		m_rootDelay;
	private double		m_rootDispersion;
	private byte[]		m_referenceIdentifier		= "LOCL".getBytes();

	private SntpTime	m_localTransmitTime;
	private SntpTime	m_remoteReceiveTime;
	private SntpTime	m_referenceTime;
	private SntpTime	m_remoteTransmitTime;
	private SntpTime	m_localReceiveTime;

	static
	{
		Calendar c1900	= Calendar.getInstance();
		c1900.set(1900, Calendar.JANUARY, 1);
		Calendar c1970	= Calendar.getInstance();
		c1970.set(1970, Calendar.JANUARY, 1);
		MILLI_SECONDS_1900_1970 = c1970.getTimeInMillis() - c1900.getTimeInMillis();
	}

	public SntpMessage()
	{
		m_localTransmitTime = new SntpTime(0, 0);
		m_remoteReceiveTime = new SntpTime(0, 0);
		m_referenceTime = new SntpTime(0, 0);
		m_remoteTransmitTime = new SntpTime();			// Transmit, returned to me as originate == local transmit
		m_localReceiveTime = new SntpTime(0, 0);
	}

	public long getOffset()
	{
		long		t1		= m_localTransmitTime.getTime();
		long		t2		= m_remoteReceiveTime.getTime();
		long		t3		= m_remoteTransmitTime.getTime();
		long		t4		= m_localReceiveTime.getTime();

		long		offset	= ((t4 - t2) - (t3 - t1)) / 2;

		return offset;
	}

	public byte getLeapIndicator()
	{
		return m_leapIndicator;
	}

	public byte getVersionNumber()
	{
		return m_versionNumber;
	}

	public byte getMode()
	{
		return m_mode;
	}

	public byte getStratum()
	{
		return m_stratum;
	}

	public byte getPollInterval()
	{
		return m_pollInterval;
	}

	public byte getPrecision()
	{
		return m_precision;
	}

	public double getRootDelay()
	{
		return m_rootDelay;
	}

	public double getRootDispersion()
	{
		return m_rootDispersion;
	}

	@SuppressWarnings("PMD.MethodReturnsInternalArray")
	public byte[] getReferenceIdentifier()
	{
		return m_referenceIdentifier;
	}

	public SntpTime getLocalTransmitTime()
	{
		return m_localTransmitTime;
	}

	public SntpTime getRemoteReceiveTime()
	{
		return m_remoteReceiveTime;
	}

	public SntpTime getReferenceTime()
	{
		return m_referenceTime;
	}

	public SntpTime getRemoteTransmitTime()
	{
		return m_remoteTransmitTime;
	}

	public SntpTime getLocalReceiveTime()
	{
		return m_localReceiveTime;
	}

	public void setLeapIndicator(byte leapIndicator)
	{
		m_leapIndicator = leapIndicator;
	}

	public void setVersionNumber(byte versionNumber)
	{
		m_versionNumber = versionNumber;
	}

	public void setMode(byte mode)
	{
		m_mode = mode;
	}

	public void setStratum(byte stratum)
	{
		m_stratum = stratum;
	}

	public void setPollInterval(byte pollInterval)
	{
		m_pollInterval = pollInterval;
	}

	public void setPrecision(byte precision)
	{
		m_precision = precision;
	}

	public void setRootDelay(double rootDelay)
	{
		m_rootDelay = rootDelay;
	}

	public void setRootDispersion(double rootDispersion)
	{
		m_rootDispersion = rootDispersion;
	}

	@SuppressWarnings("PMD.ArrayIsStoredDirectly")
	public void setReferenceIdentifier(byte[] referenceIdentifier)
	{
		m_referenceIdentifier = referenceIdentifier;
	}

	public void setLocalTransmitTime(SntpTime localTransmitTime)
	{
		m_localTransmitTime = localTransmitTime;
	}

	public void setRemoteReceiveTime(SntpTime remoteReceiveTime)
	{
		m_remoteReceiveTime = remoteReceiveTime;
	}

	public void setReferenceTime(SntpTime referenceTime)
	{
		m_referenceTime = referenceTime;
	}

	public void setRemoteTransmitTime(SntpTime remoteTransmitTime)
	{
		m_remoteTransmitTime = remoteTransmitTime;
	}

	public void setLocalReceiveTime(SntpTime localReceiveTime)
	{
		m_localReceiveTime = localReceiveTime;
	}

	public static class SntpTime
	{

		private long		m_integer;
		private long		m_fraction;

		public SntpTime()
		{
			setTime(System.currentTimeMillis());
		}

		public SntpTime(long time)
		{
			setTime(time);
		}

		public SntpTime(long integer, long fraction)
		{
			m_integer = integer;
			m_fraction = fraction;
		}

		public long getInteger()
		{
			return m_integer;
		}

		public long getFraction()
		{
			return m_fraction;
		}

		public final void setTime(long timeMS)
		{
			timeMS += MILLI_SECONDS_1900_1970;
			m_integer = timeMS / 1000;
			m_fraction = ((timeMS % 1000) * 0x100000000L) / 1000;
		}
		public long getTime()
		{
			long		time		= (m_integer * 1000 - MILLI_SECONDS_1900_1970);
			long		ms			= (m_fraction * 1000) / 0x100000000L;
			time += ms;

			return time;
		}
	}
}
