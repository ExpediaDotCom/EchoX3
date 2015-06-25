/**
 * Copyright 2012 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Author  <mailto:pcote@expedia.com>Pierre Cote</mailto>
 */

package com.expedia.echox3.basics.tools.string;

/**
* Utility class to parse strings.
* @author Pierre
*
*/
public class StringParser
{
	private String m_str;

	/**
	* Initialize an object with the string to be parsed
	* @param str
	*/
	public StringParser(String str)
	{
		m_str = str;
	}

	/**
	* Obtain the current string, what is left after all the skipping.
	*
	* @return
	*/
	public String getString()
	{
		return m_str;
	}

	/**
	 * Skip to the next line
	 * @return
	 */
	public String skipLine()
	{
		return skipTo("\n");
	}

	/**
	 * Skips n characters.
	 * @param n
	 * @return
	 */
	public String skip(int n)
	{
		int	len		= m_str.length();
		String temp;
		if (n >= len)
		{
			temp	= m_str;
			m_str = "";
		}
		else
		{
			temp = m_str.substring(0, n);
			m_str = m_str.substring(n + 1);
		}
		return temp;
	}

	/**
	* Skips the content of the string, up to the specified string.
	* The characters skipped are returned, the to string is removed and
	* the string now contains only what comes after the to string.
	* if the string is "abcdefghi" and you call skipTo("def"), then
	*	   The returned value will be "abc"
	*  The remaining string will be "ghi"
	*
	* If the to string is not found, the entire string is skipped.
	*
	* @param to	String to search
	* @return
	*/
	public String skipTo(String to)
	{
		String temp;

		int			 i	   = m_str.indexOf(to);
		if (-1 == i)
		{
			temp = m_str;
			m_str = "";
		}
		else
		{
			temp = m_str.substring(0, i);
			m_str = m_str.substring(i + to.length());
		}

		return temp;
	}

	/**
	* Cleans up by removing the blank lines in the string.
	*
	*/
	public void cleanup()
	{
		m_str = m_str.replaceAll("\r", "\n");
		m_str = m_str.replaceAll("\n\n", "\n");
		m_str = m_str.replaceAll("\n\n", "\n");
	}

	/**

	 * Determines if the remaining string is empty (== "")

	 * @return	true if the string is empty.

	 */
	public boolean isEmpty()
	{
		return "".equals(m_str);
	}
}
