/*
 * HighlightCellRenderer.java - The Highlight Cell Renderer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Matthieu Casanova
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
import org.gjt.sp.jedit.GUIUtilities;

import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.*;
//}}}

/**
 * The cell renderer that will render the Highlights in the JTable.
 *
 * @author Matthieu Casanova
 */
public class HighlightCellRenderer implements TableCellRenderer
{

	private final HighlightTablePanel highlightTablePanel = new HighlightTablePanel();
	private final JCheckBox enabled = new JCheckBox();
	private final JButton remove = new JButton(GUIUtilities.loadIcon("Clear.png"));
	private final JButton hypersearch = new JButton(GUIUtilities.loadIcon("Find.png"));

	//{{{ getTableCellRendererComponent() method
	public Component getTableCellRendererComponent(JTable table,
						       Object value,
						       boolean isSelected,
						       boolean hasFocus,
						       int row,
						       int column)
	{
		Highlight highlight = (Highlight) value;
		if (column == 0)
		{
			enabled.setSelected(highlight.isEnabled());
			return enabled;
		}
		else if (column == 1)
		{
			highlightTablePanel.setHighlight(highlight);
			return highlightTablePanel;
		}
		else if (column == 2)
		{
			return remove;
		}
		else
		{
			return hypersearch;
		}
	} //}}}

	//{{{ getPreferredSize() method
	public Dimension getPreferredSize()
	{
		return highlightTablePanel.getPreferredSize();
	} //}}}

}
