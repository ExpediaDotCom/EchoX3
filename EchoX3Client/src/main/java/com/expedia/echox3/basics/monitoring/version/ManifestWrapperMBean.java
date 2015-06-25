/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.version;

import java.net.URL;
import java.util.Date;

public interface ManifestWrapperMBean
{
	URL			getUrl();
	String		getPathName();
	String		getFileName();

	String		getImplementationTitle();
	String		getBuildJdk();
	long		getBuildTime();
	Date getBuildDate();
	String		getImplementationVendor();
	String		getImplementationVersion();

	String		toString();
}
