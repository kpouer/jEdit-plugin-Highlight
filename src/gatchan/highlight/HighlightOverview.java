/*
 * HighlightOverview.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2009 Szalai Endre
 * Portions Copyright (C) 2009, 2020 Matthieu Casanova
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package gatchan.highlight;

//{{{ Imports
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import javax.swing.JPanel;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.SearchMatcher.Match;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.IntegerArray;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.SegmentBuffer;
//}}}

/**
 * @author Szalai Endre
 */
public class HighlightOverview extends JPanel implements HighlightChangeListener
{
	private final IntegerArray items;

	private final JEditTextArea textArea;

	private static final int ITEM_HEIGHT = 4;
	private static final int OVERVIEW_WIDTH = 12;
	private static final int ITEM_BORDER = 2;
	private static final int ITEM_WIDTH = OVERVIEW_WIDTH - 2 * ITEM_BORDER;
	// To be in the same line as the scrollbar
	private static final int Y_OFFSET = 16;
	private static final Dimension preferredSize = new Dimension(OVERVIEW_WIDTH, 0);
	private Color color;

	//{{{ HighlightOverview constructor
	HighlightOverview(JEditTextArea textArea)
	{
		Font currentFont = getFont();
		Font newFont = new Font(currentFont.getName(), Font.BOLD, 8);
		setFont(newFont);
		this.textArea = textArea;
		items = new IntegerArray(32);
		setRequestFocusEnabled(false);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int lineCount = textArea.getLineCount();
				int line = yToLine(e.getY(), lineCount);
				textArea.setFirstLine(Math.max(line - textArea.getVisibleLines() / 2, 0));
			}
		});
	} //}}}

	//{{{ highlightUpdated() method
	@Override
	public void highlightUpdated(boolean highlightEnabled)
	{
		long start = System.currentTimeMillis();
		items.clear();
		JEditBuffer buffer = textArea.getBuffer();
		int lineCount = buffer.getLineCount();
		if (!highlightEnabled || (!HighlightManagerTableModel.currentWordHighlight.isEnabled() &&
								!HighlightManagerTableModel.selectionHighlight.isEnabled()) ||
				lineCount > jEdit.getIntegerProperty("gatchan.highlight.overview.maxLines", 200000))
		{
			repaint();
			return;
		}

		SearchMatcher matcher = HighlightManagerTableModel.selectionHighlight.isEnabled() ?
				HighlightManagerTableModel.selectionHighlight.getSearchMatcher() :
				HighlightManagerTableModel.currentWordHighlight.getSearchMatcher();

		LongAccumulator accumulator = new LongAccumulator(Long::sum, 0L);
		IntStream lineStream = IntStream.range(0, lineCount);

		lineStream
				.parallel()
				.map(line -> match(buffer, matcher, line))
				.filter(line -> line >= 0)
				.forEach(line -> pushLine(accumulator, line));

		View view = textArea.getView();
		if (view.isActive())
		{
			if (view.getTextArea() == textArea)
				view.getStatus().setMessage(accumulator.longValue() + " lines contains the current word");
		}
		long endTime = System.currentTimeMillis();
		Log.log(Log.MESSAGE, this, "Highlight overview processed in " + (endTime - start) + "ms");
		repaint();
	} //}}}

	//{{{ pushLine() method
	private void pushLine(LongAccumulator accumulator, int line)
	{
		synchronized (items)
		{
			items.add(line);
			accumulator.accumulate(1L);
		}
	} //}}}

	//{{{ match() method
	/**
	 * Search in the buffer
	 * @param buffer the buffer
	 * @param matcher the search matcher
	 * @param line the line to check
	 * @return the given line if the text was found, or -1 if not
	 */
	private static int match(JEditBuffer buffer, SearchMatcher matcher, int line)
	{
		try
		{
			SegmentBuffer segmentBuffer = new SegmentBuffer(0);
			buffer.getLineText(line, segmentBuffer);
			Match match = matcher.nextMatch(segmentBuffer, true, true, true, false);
			if (match != null)
			{
				return line;
			}
		} catch (PatternSyntaxException ignored)
		{
		} catch (InterruptedException e)
		{
		}
		return -1;
	} //}}}

	//{{{ paintComponent() method
	@Override
	public void paintComponent(Graphics gfx)
	{
		super.paintComponent(gfx);
		if (items.getSize() == 0)
			return;


		int lineCount = textArea.getLineCount();
//		gfx.drawString(String.valueOf(count), 0, 10);
		if (color != null)
			gfx.setColor(color);
		else if (HighlightManagerTableModel.selectionHighlight.isEnabled())
		{
			gfx.setColor(HighlightManagerTableModel.selectionHighlight.getColor());
		}
		else
		{
			gfx.setColor(HighlightManagerTableModel.currentWordHighlight.getColor());
		}

		for (int i = 0;i<items.getSize();i++)
		{
			int y = lineToY(items.get(i), lineCount);
			gfx.fillRect(ITEM_BORDER, y, ITEM_WIDTH, ITEM_HEIGHT);
		}
	} //}}}

	//{{{ lineToY() method
	private int lineToY(int line, int lineCount)
	{
		return Y_OFFSET + (getHeight() - 2 * Y_OFFSET) * line / lineCount - ITEM_BORDER;
	} //}}}

	//{{{ yToLine() method
	int yToLine(int y, int lineCount)
	{
		return (y + ITEM_BORDER - Y_OFFSET) * lineCount / (getHeight() - 2 * Y_OFFSET);
	} //}}}

	//{{{ getPreferredSize() method
	@Override
	public Dimension getPreferredSize()
	{
		return preferredSize;
	} //}}}

	public void setOverviewColor(Color color)
	{
		this.color = color;
	}
}
