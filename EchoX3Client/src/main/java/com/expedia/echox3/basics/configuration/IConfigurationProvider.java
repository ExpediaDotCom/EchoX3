/**
 * Copyright 2013 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.configuration;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Pierre
 * Date: 8/26/13
 * Time: 7:47 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IConfigurationProvider extends Closeable
{
	Object					getSource();
	Map<String, String>		getSettingMap();
	String					getSetting(String name);
	Set<String>				getSettingNameSet(String prefix);
}
