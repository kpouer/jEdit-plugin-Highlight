/*
 * HighlightHypersearchResults.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2021 Matthieu Casanova
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
package gatchan.highlight.hypersearch;

import gatchan.highlight.Highlight;
import gatchan.highlight.HighlightManager;
import gatchan.highlight.HighlightManagerTableModel;
import org.gjt.sp.jedit.search.SearchMatcher;

import java.awt.*;
import java.util.*;
import java.util.List;

public class HyperSearchResultValue
{
	private String originalText;
	private String str;

	public HyperSearchResultValue(String originalText)
	{
		this.originalText = originalText;
	}

	@Override
	public String toString()
	{
		if (str == null)
		{
			str = getHighlightedString(originalText);
			originalText = null;
		}
		return str;
	}

	private static String getHighlightedString(String s)
	{
		HighlightManager manager = HighlightManagerTableModel.getManager();
		List<HighlightPosition> highlights = new LinkedList<>();
		try
		{
			manager.getReadLock();
			int highlightCount = manager.countHighlights();
			for (int hi = 0; hi < highlightCount; hi++)
			{
				Highlight highlight = manager.getHighlight(hi);
				addHighlight(highlights, s, highlight);
			}
		}
		finally
		{
			manager.releaseLock();
		}
		if (manager.isHighlightWordAtCaret())
			addHighlight(highlights, s, HighlightManagerTableModel.currentWordHighlight);
		if (manager.isHighlightSelection())
			addHighlight(highlights, s, HighlightManagerTableModel.selectionHighlight);

		highlights.sort(Comparator.comparingInt(HighlightPosition::getPos));
		StringBuilder sb = new StringBuilder("<html><body>");
		int i = 0;
		for (HighlightPosition hlPos : highlights)
		{
			appendString2html(sb, s.substring(i, hlPos.getPos()));
			if (hlPos.isStart())
			{
				sb.append("<font style bgcolor=\"#");
				Color c = hlPos.getHighlight().getColor();
				sb.append(Integer.toHexString(c.getRGB()).substring(2));
				sb.append("\">");
			}
			else
			{
				sb.append("</font>");
			}
			i = hlPos.getPos();
		}
		appendString2html(sb, s.substring(i));
		sb.append("</body></html>");
		return sb.toString();
	}

	private static void addHighlight(Collection<? super HighlightPosition> highlights, String s, Highlight highlight)
	{
		SearchMatcher matcher = highlight.getSearchMatcher();
		try
		{
			SearchMatcher.Match m;
			int i = 0;
			while ((m = matcher.nextMatch(s.substring(i), true, true, true, false)) != null)
			{
				highlights.add(new HighlightPosition(i + m.start, highlight, true));
				highlights.add(new HighlightPosition(i + m.end, highlight, false));
				i += m.end;
			}
		}
		catch (InterruptedException ie)
		{
		}
	}

	private static void appendString2html(StringBuilder sb, CharSequence s)
	{
		int length = s.length();
		for (int i = 0; i < length; i++)
		{
			char c = s.charAt(i);
			String r;
			switch (c)
			{
				case '"':
					r = "&quot;";
					break;
					/*case '\'':
						r = "&apos;";
						break;  */
				case '&':
					r = "&amp;";
					break;
				case '<':
					r = "&lt;";
					break;
				case '>':
					r = "&gt;";
					break;
				default:
					r = String.valueOf(c);
					break;
			}
			sb.append(r);
		}
	}

}
