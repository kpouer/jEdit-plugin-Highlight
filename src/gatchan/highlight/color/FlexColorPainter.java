/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010-2015 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package gatchan.highlight.color;

import gatchan.highlight.HighlightOptionPane;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.util.Log;

import java.awt.*;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import javax.swing.text.Segment;

/**
 * @author Matthieu Casanova
 */
public class FlexColorPainter extends TextAreaExtension
{
	public static final int MAX_LINE_LENGTH = 10000;
	private final TextArea textArea;
	private final Point point = new Point();
	private final Segment seg;
	private final FlexColorScanner flexColor;

	public FlexColorPainter(TextArea textArea)
	{
		this.textArea = textArea;
		seg = new Segment();
		flexColor = new FlexColorScanner(new StringReader(""));
	}

	//{{{ paintScreenLineRange() method
	@Override
	public void paintScreenLineRange(Graphics2D gfx, int firstLine, int lastLine, int[] physicalLines, int[] start, int[] end, int y, int lineHeight)
	{
		if (jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_COLORS))
			super.paintScreenLineRange(gfx, firstLine, lastLine, physicalLines, start, end, y, lineHeight);
	} //}}}

	@Override
	public void paintValidLine(Graphics2D gfx, int screenLine, int physicalLine, int start, int end, int y)
	{
		JEditBuffer buffer = textArea.getBuffer();
		int lineStartOffset = buffer.getLineStartOffset(physicalLine);
		int lineEndOffset = buffer.getLineEndOffset(physicalLine);
		int length = buffer.getLineLength(physicalLine);

		int screenToPhysicalOffset = start - lineStartOffset;
		int l = length - screenToPhysicalOffset - lineEndOffset + end;
		if (l > MAX_LINE_LENGTH)
			l = MAX_LINE_LENGTH;
		if (l == 0)
			return;
		buffer.getText(lineStartOffset + screenToPhysicalOffset, l, seg);

		try (CharArrayReader charArrayReader = new CharArrayReader(seg.array, seg.offset, seg.count))
		{
			flexColor.yyreset(charArrayReader);
			ColorToken token = flexColor.yylex();
			while (token != null)
			{
				paint(token, gfx, physicalLine, start, y);
				try
				{
					token = flexColor.yylex();
				}
				catch (IOException e)
				{
					Log.log(Log.ERROR, this, e);
				}
			}
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, e);
		}
		seg.array = null;
	}

	private void paint(ColorToken token, Graphics2D gfx, int physicalLine, int start, int y)
	{
		int phycialLineStartOffset = textArea.getLineStartOffset(physicalLine);
		int screenStartOffset = start + token.getStart();
		int screenEndOffset = start + token.getEnd();
		Point p = textArea.offsetToXY(physicalLine, screenStartOffset - phycialLineStartOffset, point);

		if (p == null)
		{
			// The start offset was not visible
			return;
		}
		int startX = p.x;

		p = textArea.offsetToXY(physicalLine, screenEndOffset - phycialLineStartOffset, point);
		if (p == null)
		{
			// The end offset was not visible
			return;
		}
		int endX = p.x;
		Color oldColor = gfx.getColor();
		Composite oldComposite = gfx.getComposite();
		gfx.setColor(token.getColor());
		FontMetrics fm = textArea.getPainter().getFontMetrics();

		int y2 = y + fm.getHeight() - 2;
		int y3 = y + fm.getHeight() - 1;
		gfx.drawLine(startX, y2, endX, y2);
		gfx.drawLine(startX, y3, endX, y3);

		gfx.setColor(oldColor);
		gfx.setComposite(oldComposite);
	} //}}}
}
