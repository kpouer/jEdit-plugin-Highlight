/*
 * ButtonCellEditor.java - The Button cell editor.
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
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.search.CurrentBufferSet;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
//}}}

/**
 * A Button cell editor. It will remove the highlight when clicking on it.
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightTablePanel.java,v 1.14 2006/06/21 09:40:32 kpouer Exp $
 */
public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
{

	private final JButton remove = new JButton(GUIUtilities.loadIcon("Clear.png"));
	private final JButton search = new JButton(GUIUtilities.loadIcon("Find.png"));

	private final HighlightManager highlightManager;

	private int row;

	private Highlight highlight;

	//{{{ ButtonCellEditor() method
	public ButtonCellEditor(HighlightManager highlightManager)
	{
		this.highlightManager = highlightManager;
		remove.addActionListener(this);
		search.addActionListener(this);
	} //}}}

	//{{{ getCellEditorValue() method
	public Object getCellEditorValue()
	{
		return highlight;
	} //}}}

	//{{{ getTableCellEditorComponent() method
	public Component getTableCellEditorComponent(JTable table,
						     Object value,
						     boolean isSelected,
						     int row,
						     int column)
	{
		this.row = row;
		highlight = (Highlight) value;
		if (column == 2)
			return remove;
		return search;
	} //}}}

	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent e)
	{
		stopCellEditing();
		if (e.getSource() == remove)
			highlightManager.removeRow(row);
		else if (e.getSource() == search)
		{
			String text = highlight.getStringToHighlight();
			SearchAndReplace.setRegexp(highlight.isRegexp());
			// if(highlight.isRegexp())
			//    text = SearchAndReplace.escapeRegexp(text,false);
			HistoryModel.getModel("find").addItem(text);
			SearchAndReplace.setSearchString(text);
			SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
			SearchAndReplace.hyperSearch(jEdit.getActiveView());
		}
	} //}}}
}
