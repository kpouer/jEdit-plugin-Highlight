package gatchan.highlight;

import org.gjt.sp.util.Log;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: Kupee Date: 14 janv. 2005 Time: 23:38:52 To change this template use File | Settings
 * | File Templates.
 */

/**
 * The tableModel that will contains the highlights. It got only one column and the datas are stored in a Vector
 *
 * @author Matthieu Casanova
 */
public final class OneColumnTableModel extends AbstractTableModel {
  private final List datas = new ArrayList();

  /**
   * Returns the number of highlights in the list.
   *
   * @return the number of highlights
   */
  public int getRowCount() {
    return datas.size();
  }

  /**
   * Returns 1 because there is only one column.
   *
   * @return 1
   */
  public int getColumnCount() {
    return 1;
  }

  public String getColumnName(int columnIndex) {
    return "Highlights";
  }

  public Class getColumnClass(int columnIndex) {
    return Highlight.class;
  }

  /**
   * All cells are editable.
   *
   * @param rowIndex
   * @param columnIndex
   *
   * @return true
   */
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    return datas.get(rowIndex);
  }

  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    datas.set(rowIndex, aValue);
    fireTableCellUpdated(rowIndex, 0);
  }

  /**
   * Add a Highlight in the list.
   *
   * @param highlight the highlight to be added
   */
  public void addElement(Highlight highlight) {
    datas.add(highlight);
    final int firstRow = datas.size() - 1;
    fireTableRowsInserted(firstRow, firstRow);
  }

  /**
   * Remove a highlight from the list.
   *
   * @param o the highlight to be removed
   */
  public void removeElement(Object o) {
    for (int i = 0; i < datas.size(); i++) {
      final Highlight highlight = (Highlight) datas.get(i);
      if (highlight.equals(o)) {
        datas.remove(o);
        fireTableRowsDeleted(i, i);
        break;
      }
    }
    Log.log(Log.WARNING,this,"An unknown highlight was asked to be removed from list" + o);
  }

  /**
   * Check if the list contains a highlight.
   *
   * @param o the highlight
   * @return true if the highlight is in the list
   */
  public boolean contains(Object o) {
    return datas.contains(o);
  }

  /**
   * remove all Highlights.
   */
  public void removeAll() {
    final int rowMax = datas.size();
    datas.clear();
    fireTableRowsDeleted(0, rowMax - 1);
  }
}
