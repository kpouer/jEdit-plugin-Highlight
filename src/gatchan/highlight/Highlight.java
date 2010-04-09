/*
 * Highlight.java - An Highlight
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2010 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gatchan.highlight;

//{{{ Imports
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.PatternSearchMatcher;
import org.gjt.sp.jedit.search.BoyerMooreSearchMatcher;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;

import java.awt.*;
import java.util.ArrayList;
//}}}

/**
 * A Highlight defines the string to highlight.
 *
 * @author Matthieu Casanova
 * @version $Id: Highlight.java,v 1.23 2006/06/21 09:40:32 kpouer Exp $
 */
public class Highlight
{
	/**
	* This scope will be saved when you exit jEdit.
	*/
	public static final int PERMANENT_SCOPE = 0;
	/**
	* This scope is global but will not be saved.
	*/
	public static final int SESSION_SCOPE = 1;
	/**
	* This scope will not be saved and is for one buffer only.
	*/
	public static final int BUFFER_SCOPE = 2;

	private String stringToHighlight;

	private boolean regexp;

	private boolean ignoreCase = true;

	private boolean valid;

	private SearchMatcher searchMatcher;

	private static final int HIGHLIGHT_VERSION = 1;

	private boolean highlightSubsequence = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SUBSEQUENCE);

	/**
	* The default color. If null we will cycle the colors.
	*/
	private static Color defaultColor = jEdit.getColorProperty(HighlightOptionPane.PROP_DEFAULT_COLOR);

	private static final Color[] COLORS = {new Color(153, 255, 204),
		new Color(0x66, 0x66, 0xff),
		new Color(0xff, 0x66, 0x66),
		new Color(0xff, 0xcc, 0x66),
		new Color(0xcc, 0xff, 0x66),
		new Color(0xff, 0x33, 0x99),
		new Color(0xff, 0x33, 0x00),
		new Color(0x66, 0xff, 0x00),
		new Color(0x99, 0x00, 0x99),
		new Color(0x99, 0x99, 0x00),
	new Color(0x00, 0x99, 0x66)};

	private static int colorIndex;

	private Color color;

	private boolean enabled = true;

	private int scope = PERMANENT_SCOPE;

	private JEditBuffer buffer;

	/**
	* The time to live in ms.
	*/
	private long duration = Long.MAX_VALUE;

	/**
	* The date where the highlight was last seen.
	*/
	private long lastSeen = System.currentTimeMillis();
	public static final String HIGHLIGHTS_BUFFER_PROPS = "highlights";

	//{{{ Highlight constructor
	public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase, int scope)
	{
		this.scope = scope;
		init(stringToHighlight, regexp, ignoreCase, getNextColor());
	}

	public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase)
	{
		this(stringToHighlight, regexp, ignoreCase, PERMANENT_SCOPE);
	}

	public Highlight(String s)
	{
		this(s, false, true, PERMANENT_SCOPE);
	}

	public Highlight()
	{
	} //}}}

	//{{{ init() method
	public void init(String s, boolean regexp, boolean ignoreCase, Color color)
	{
		if (s.length() == 0)
		{
			valid = false;
			throw new IllegalArgumentException("The search string cannot be empty");
		}
		valid = true;
		if (regexp)
		{
			if (searchMatcher == null ||
				!s.equals(stringToHighlight) ||
				!this.regexp ||
				ignoreCase != this.ignoreCase)
			{
				searchMatcher = new PatternSearchMatcher(s, ignoreCase);
			}
		}
		else if (searchMatcher == null ||
			!s.equals(stringToHighlight) ||
			this.regexp ||
			ignoreCase != this.ignoreCase)
		{
			searchMatcher = new BoyerMooreSearchMatcher(s, ignoreCase);
		}
		stringToHighlight = s;
		this.regexp = regexp;
		this.ignoreCase = ignoreCase;
		this.color = color;
	} //}}}

	//{{{ getStringToHighlight() method
	public String getStringToHighlight()
	{
		return stringToHighlight;
	} //}}}

	//{{{ isRegexp() method
	public boolean isRegexp()
	{
		return regexp;
	} //}}}

	//{{{ isIgnoreCase() method
	public boolean isIgnoreCase()
	{
		return ignoreCase;
	} //}}}

	//{{{ setIgnoreCase() method
	public void setIgnoreCase(boolean ignoreCase)
	{
		this.ignoreCase = ignoreCase;
	} //}}}

	//{{{ isValid() method
	public boolean isValid()
	{
		return valid;
	} //}}}

	//{{{ setValid() method
	public void setValid(boolean valid)
	{
		this.valid = valid;
	} //}}}

	//{{{ getSearchMatcher() method
	public SearchMatcher getSearchMatcher()
	{
		return searchMatcher;
	} //}}}

	//{{{ getColor() method
	/**
	* Returns the color of the highlight.
	*
	* @return the color
	*/
	public Color getColor()
	{
		return color;
	} //}}}

	//{{{ setColor() method
	/**
	* Set the color of the highlight.
	*
	* @param color the new color
	*/
	public void setColor(Color color)
	{
		this.color = color;
	} //}}}

	//{{{ equals() method
	public boolean equals(Object obj)
	{
		if (obj instanceof Highlight)
		{
			Highlight highlight = (Highlight) obj;
			return highlight.stringToHighlight.equals(stringToHighlight) &&
			highlight.regexp == regexp &&
			highlight.ignoreCase == ignoreCase;
		}
		return false;
	} //}}}

	//{{{ isEnabled() method
	public boolean isEnabled()
	{
		return enabled;
	} //}}}

	//{{{ setEnabled() method
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	} //}}}

	//{{{ setDefaultColor() method
	/**
	* Set the default color.
	*
	* @param defaultColor the new default color. If null we will cycle the colors
	*/
	public static void setDefaultColor(Color defaultColor)
	{
		Highlight.defaultColor = defaultColor;
	} //}}}

	//{{{ getNextColor() method
	public static Color getNextColor()
	{
		if (defaultColor != null)
		{
			return defaultColor;
		}
		colorIndex = ++colorIndex % COLORS.length;
		return COLORS[colorIndex];
	} //}}}

	//{{{ serialize() method
	/**
	 * Serialize the highlight like that : {@link #HIGHLIGHT_VERSION};regexp ignorecase color;stringToHighlight (no space
	 * between regexp, ignorecase and color
	 *
	 * @return the serialized string
	 */
	public String serialize()
	{
		StringBuffer buff = new StringBuffer(stringToHighlight.length() + 20);
		buff.append(HIGHLIGHT_VERSION).append(';');
		serializeBoolean(buff, regexp);
		serializeBoolean(buff, ignoreCase);
		serializeBoolean(buff, enabled);
		buff.append(color.getRGB());
		buff.append(';');
		buff.append(stringToHighlight);
		return buff.toString();
	} //}}}

	//{{{ serializeBoolean() method
	private static void serializeBoolean(StringBuffer buff, boolean bool)
	{
		if (bool)
		{
			buff.append(1);
		}
		else
		{
			buff.append(0);
		}
	} //}}}

	//{{{ gatchan.highlight.HighlightPluginize() method
	/**
	* Unserialize the highlight.
	*
	* @param s the string to parse.
	* @return the highlight unserialized
	* @throws InvalidHighlightException exception if the highlight is invalid
	*/
	public static Highlight unserialize(String s, boolean getStatus) throws InvalidHighlightException
	{
		try
		{
			int index = s.indexOf(';');
			boolean regexp = s.charAt(index + 1) == '1';
			boolean ignoreCase = s.charAt(index + 2) == '1';
			boolean enabled = !getStatus || s.charAt((index + 3)) == '1';
			int i = s.indexOf(';', index + 4);
			Color color = Color.decode(s.substring(index + 4, i));

			// When using String.substring() the new String uses the same char[] so the new String is as big as the first one.
			// This is minor optimization
			String searchString = new String(s.substring(i + 1));
			Highlight highlight = new Highlight();
			highlight.setEnabled(enabled);
			highlight.init(searchString, regexp, ignoreCase, color);
			return highlight;
		}
		catch (Exception e)
		{
			throw new InvalidHighlightException(e);
		}
	} //}}}

	//{{{ getScope() method
	/**
	* Get the scope of the highlight.
	*
	* @return the scope of the highlight
	*/
	public int getScope()
	{
		return scope;
	} //}}}

	//{{{ setScope() method
	/**
	* Set the scope of the highlight.
	*
	* @param scope the new scope
	*/
	public void setScope(int scope)
	{
		this.scope = scope;
	} //}}}

	//{{{ getBuffer() method
	/**
	* Returns the buffer associated to this highlight. It will be null if the scope is not {@link #BUFFER_SCOPE}
	*
	* @return the buffer associated
	*/
	public JEditBuffer getBuffer()
	{
		return buffer;
	} //}}}

	//{{{ setBuffer() method
	/**
	* Associate the highlight to a buffer. It must only be used for {@link #BUFFER_SCOPE}
	*
	* @param buffer the buffer
	*/
	public void setBuffer(JEditBuffer buffer)
	{
		if (this.buffer != null)
		{
			java.util.List<Highlight> highlights = (java.util.List<Highlight>) this.buffer.getProperty(HIGHLIGHTS_BUFFER_PROPS);
			highlights.remove(this);
			if (highlights.isEmpty())
			{
				this.buffer.unsetProperty(HIGHLIGHTS_BUFFER_PROPS);
			}
		}
		this.buffer = buffer;
		if (buffer != null)
		{
			java.util.List<Highlight> highlights = (java.util.List<Highlight>) buffer.getProperty(HIGHLIGHTS_BUFFER_PROPS);
			if (highlights == null)
			{
				highlights = new ArrayList<Highlight>();
				buffer.setProperty(HIGHLIGHTS_BUFFER_PROPS, highlights);
			}
			highlights.add(this);
		}
	} //}}}

	//{{{ setDuration() method
	/**
	* Set the time to live of the highlight.
	* This time to live is the time until the highlight expires if it wasn't be seen
	*
	* @param duration the duration in ms.
	*/
	public void setDuration(long duration)
	{
		this.duration = duration;
	} //}}}

	//{{{ updateLastSeen() method
	/**
	* This method is called each time the highlight is seen.
	*/
	public void updateLastSeen()
	{
		lastSeen = System.currentTimeMillis();
	} //}}}

	//{{{ isExpired() method
	/**
	* Check if the highlight is expired.
	* The highlight is expired if it hasn't been seen for more that {@link #duration} field
	*
	* @return true if the highlight is expired
	*/
	public boolean isExpired()
	{
		return System.currentTimeMillis() - lastSeen > duration;
	} //}}}

	//{{{ setStringToHighlight() method
	public void setStringToHighlight(String stringToHighlight)
	{
		init(stringToHighlight, regexp, ignoreCase, color);
	} //}}}

	//{{{ isHighlightSubsequence() method
	public boolean isHighlightSubsequence()
	{
		return highlightSubsequence;
	} //}}}

	//{{{ setHighlightSubsequence() method
	/**
	* Activate or deactivate the feature "highlight subsequence"
	* Highlight subsequences (if checked, the same highlight can be found several times inside the same word)
	*
	* @param highlightSubsequence true to activate the feature, false otherwise
	* @return <code>true</code> if the value was changed
	*/
	public boolean setHighlightSubsequence(boolean highlightSubsequence)
	{
		if (this.highlightSubsequence != highlightSubsequence)
		{
			this.highlightSubsequence = highlightSubsequence;
			return true;
		}
		return false;
	} //}}}
}
