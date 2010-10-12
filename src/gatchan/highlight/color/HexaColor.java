/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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

package gatchan.highlight.color;

import org.gjt.sp.jedit.textarea.TextArea;

import java.awt.*;


/**
 * @author Matthieu Casanova
 */
public class HexaColor implements ColorHighlighter
{
	private Point point;

	public HexaColor()
	{
		point = new Point();
	}

	public void paintColor(TextArea textArea, Graphics2D gfx, int physicalLine, int y, CharSequence lineContent, AlphaComposite blend, FontMetrics fm)
	{
		int startColor = 0;
		boolean colorPrefix = false;
		char[] currentColor = new char[8];
		int j = 2;
		for (int i = 0;i<lineContent.length();i++)
		{
			char c = lineContent.charAt(i);
			if (c == '#')
			{
				colorPrefix = true;
				startColor = i;
			}
			else if (colorPrefix)
			{
				if ((c >='0' && c <= '9') ||
					(c >= 'a' && c <= 'f')||
					(c >= 'A' && c <= 'F'))
				{
					currentColor[j++] = c;
					if (j == 8)
					{
						currentColor[0] = '0';
						currentColor[1] = 'x';
						Color color = Color.decode(new String(currentColor));
						if (color != null)
							paint(color, textArea, gfx, physicalLine, startColor, i+1, y, blend, fm);
						j = 2;
						colorPrefix = false;
					}
				}
				else
				{
					j = 2;
					colorPrefix = false;
				}
			}
		}
	}

	private void paint(Color color, TextArea textArea, Graphics2D gfx, int physicalLine, int startOffset, int endOffset, int y, AlphaComposite blend, FontMetrics fm)
	{
		Point p = textArea.offsetToXY(physicalLine, startOffset, point);
		if (p == null)
		{
			// The start offset was not visible
			return;
		}
		int startX = p.x;

		p = textArea.offsetToXY(physicalLine, endOffset, point);
		if (p == null)
		{
			// The end offset was not visible
			return;
		}
		int endX = p.x;
		Color oldColor = gfx.getColor();
		Composite oldComposite = gfx.getComposite();
		gfx.setColor(color);
		gfx.fillRect(startX, y, endX - startX, fm.getHeight() - 1);
		gfx.setColor(oldColor);
		gfx.setComposite(oldComposite);
	}
}
