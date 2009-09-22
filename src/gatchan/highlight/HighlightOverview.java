/*
 * HighlightOverview.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2009 Szalai Endre
 * Portions Copyright (C) 2009 Matthieu Casanova
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

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.IntegerArray;

import javax.swing.*;
import java.awt.*;

/**
 * @author Szalai Endre
 */
public class HighlightOverview extends JPanel implements HighlightChangeListener
{
	private final IntegerArray items;
	private int count;

	private TextArea textArea;

	private static final int ITEM_HEIGHT = 4;
	private static final int OVERVIEW_WIDTH = 12;
	private static final int ITEM_BORDER = 2;
	private static final int ITEM_WIDTH = OVERVIEW_WIDTH - 2 * ITEM_BORDER;
	// To be in the same line as the scrollbar
	private static int Y_OFFSET = 16;
	private static Dimension preferredSize = new Dimension(OVERVIEW_WIDTH, 0);

	public HighlightOverview(TextArea textArea)
	{
		Font ff = getFont();
		Font f = new Font(ff.getName(), Font.BOLD, 8);
		setFont(f);
		this.textArea = textArea;
		items = new IntegerArray(32);
		setRequestFocusEnabled(false);
	}

	public void highlightUpdated(boolean highlightEnabled)
	{
		items.clear();
		if (!highlightEnabled)
		{
			return;
		}
		int offset = 0;
		JEditBuffer buffer = textArea.getBuffer();
		int end = buffer.getLength();
		boolean endOfLine = buffer.getLineEndOffset(
				buffer.getLineOfOffset(end)) - 1 == end;
		SearchMatcher matcher = HighlightManagerTableModel.currentWordHighlight.getSearchMatcher();
		int lastResult = -1;
		int counter;
		for(counter = 0; ; counter++)
		{
			boolean startOfLine = buffer.getLineStartOffset(
				buffer.getLineOfOffset(offset)) == offset;

			SearchMatcher.Match match = matcher.nextMatch(
				buffer.getSegment(offset, end - offset),
				startOfLine,endOfLine,counter == 0,
				false);
			if(match == null)
				break;

			int newLine = buffer.getLineOfOffset(
				offset + match.start);
			if(lastResult != newLine)
			{
				items.add(newLine);
				lastResult = newLine;
			}
			offset += match.end;
		}
		count = counter;
		repaint();
	}


	public void paintComponent(Graphics gfx)
	{
		super.paintComponent(gfx);
		if (items.getSize() == 0)
			return;

		int lineCount = textArea.getBuffer().getLineCount();
		gfx.setColor(Color.black);
//		gfx.drawString(String.valueOf(count), 0, 10);
		gfx.setColor(jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_COLOR));

		for (int i = 0;i<items.getSize();i++)
		{
			int y = Y_OFFSET + lineToY(items.get(i), lineCount) - ITEM_BORDER;
			gfx.fillRect(ITEM_BORDER, y, ITEM_WIDTH, ITEM_HEIGHT);
		}
	}

	private int lineToY(int line, int lineCount)
	{
//		return (getHeight() - 12 * Y_OFFSET) * line / lineCount;
		return (getHeight() - 2 * Y_OFFSET) * line / lineCount;
	}

	public Dimension getPreferredSize()
	{
		return preferredSize;
	}
}
