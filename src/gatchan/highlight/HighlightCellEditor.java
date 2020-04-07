/*
 * HighlightCellEditor.java - The Highlight Cell Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004-2020 Matthieu Casanova
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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
//}}}

/**
 * The highlight cell editor used by the JTable containing Highlights.
 *
 * @author Matthieu Casanova
 */
public class HighlightCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, DocumentListener {
	private Highlight highlight;
	private final HighlightTablePanel renderer = new HighlightTablePanel();
	
	//{{{ HighlightCellEditor constructor
	public HighlightCellEditor() 
	{
		renderer.setCellEditor(this);
	} //}}}
	
	//{{{ getCellEditorValue() method
	@Override
	public Object getCellEditorValue()
	{
		return highlight;
	} //}}}
	
	//{{{ stopCellEditing() method
	@Override
	public boolean stopCellEditing()
	{
		try 
		{
			renderer.save(highlight);
			renderer.stopEdition();
			HighlightManagerTableModel.getManager().setHighlightEnable(true);
			fireEditingStopped();
			return true;
		} 
		catch (InvalidHighlightException e)
		{
			GUIUtilities.error(jEdit.getActiveView(), "gatchan-highlight.errordialog.invalidHighlight", null);
			return false;
		}
	} //}}}

	//{{{ isCellEditable() method
	@Override
	public boolean isCellEditable(EventObject e)
	{
		return true;
	} //}}}
	
	//{{{ getTableCellEditorComponent() method 
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		highlight = (Highlight) value;
		renderer.setHighlight(highlight);
		return renderer;
	} //}}}
	
	//{{{ actionPerformed() method
	@Override
	public void actionPerformed(ActionEvent e)
	{
		stopCellEditing();
	} //}}}
	
	//{{{ changedUpdate() method
	@Override
	public void changedUpdate(DocumentEvent e)
	{
		saveHighlight();
	} //}}}
	
	//{{{ insertUpdate() method
	@Override
	public void insertUpdate(DocumentEvent e)
	{
		saveHighlight();
	} //}}}
	
	//{{{ removeUpdate() method
	@Override
	public void removeUpdate(DocumentEvent e)
	{
		saveHighlight();
	} //}}}
	
	//{{{ saveHighlight() method
	private void saveHighlight() 
	{
		try 
		{
			renderer.save(highlight);
			HighlightManagerTableModel.getManager().setHighlightEnable(true);
		} 
		catch (InvalidHighlightException e) 
		{
		}
	} //}}}

}
