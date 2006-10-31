package gatchan.highlight;

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

/**
 * A Button cell editor. It will remove the highlight when clicking on it.
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightTablePanel.java,v 1.14 2006/06/21 09:40:32 kpouer Exp $
 */
public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
{

	private JButton remove = new JButton(GUIUtilities.loadIcon("Clear.png"));
	private JButton search = new JButton(GUIUtilities.loadIcon("Find.png"));

	private HighlightManager highlightManager;

	private int row;

	private Highlight highlight;

	public ButtonCellEditor(HighlightManager highlightManager)
	{
		this.highlightManager = highlightManager;
		remove.addActionListener(this);
		search.addActionListener(this);
	}

	public Object getCellEditorValue()
	{
		return highlight;
	}

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
	}


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
	}
}
