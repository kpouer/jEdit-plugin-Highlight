package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * A Button cell editor. It will remove the highlight when clicking on it.
 *
 * @author Matthieu Casanova
 */
public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

  private JButton remove = new JButton(GUIUtilities.loadIcon("Clear.png"));

  private HighlightManager highlightManager;

  private int row;

  public ButtonCellEditor(HighlightManager highlightManager) {
    this.highlightManager = highlightManager;
    remove.addActionListener(this);
  }

  public Object getCellEditorValue() {
    return null;
  }

  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column) {
    this.row = row;
    return remove;
  }


  public void actionPerformed(ActionEvent e) {
    stopCellEditing();
    highlightManager.removeRow(row);
  }
}
