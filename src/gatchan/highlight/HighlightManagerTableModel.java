package gatchan.highlight;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The tableModel that will contains the highlights. It got two columns, the first is a checkbox to enable/disable the
 * highlight, the second is the highlight view
 *
 * @author Matthieu Casanova
 */
public final class HighlightManagerTableModel extends AbstractTableModel implements HighlightManager {
  private final List datas = new ArrayList();
  private static HighlightManagerTableModel highlightManagerTableModel;

  private final List highlightChangeListeners = new ArrayList(2);
  private boolean highlightEnable = true;

  /**
   * Returns the instance of the HighlightManagerTableModel.
   *
   * @return the instance
   */
  public static HighlightManagerTableModel getInstance() {
    if (highlightManagerTableModel == null) {
      highlightManagerTableModel = new HighlightManagerTableModel();
    }
    return highlightManagerTableModel;
  }

  /**
   * Returns the HighlightManager.
   *
   * @return the HighlightManager
   */
  public static HighlightManager getManager() {
    return getInstance();
  }

  private HighlightManagerTableModel() {
  }

  /**
   * Returns the number of highlights in the list.
   *
   * @return the number of highlights
   */
  public int getRowCount() {
    return datas.size();
  }

  /**
   * Returns 2 because there is only two column.
   *
   * @return 2
   */
  public int getColumnCount() {
    return 3;
  }

  public Class getColumnClass(int columnIndex) {
    if (columnIndex == 0) return Boolean.class;
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
    if (columnIndex == 0) {
      return Boolean.valueOf(((Highlight) datas.get(rowIndex)).isEnabled());
    }
    return datas.get(rowIndex);
  }

  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      final Highlight highlight = (Highlight) datas.get(rowIndex);
      highlight.setEnabled(((Boolean) aValue).booleanValue());
    } else {
      datas.set(rowIndex, aValue);
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  /**
   * Return the Highlight at index i.
   *
   * @param i the index of the highlight
   *
   * @return a highlight
   */
  public Highlight getHighlight(int i) {
    return (Highlight) datas.get(i);
  }

  /**
   * Add a Highlight in the list.
   *
   * @param highlight the highlight to be added
   */
  public void addElement(Highlight highlight) {
    if (!datas.contains(highlight)) {
      datas.add(highlight);
      final int firstRow = datas.size() - 1;
      fireTableRowsInserted(firstRow, firstRow);
    }
    setHighlightEnable(true);
  }

  /**
   * Remove an element at the specified index.
   *
   * @param index the index
   */
  public void removeRow(int index) {
    datas.remove(index);
    fireTableRowsDeleted(index, index);
  }

  /** remove all Highlights. */
  public void removeAll() {
    final int rowMax = datas.size();
    datas.clear();
    if (rowMax != 0) {
      fireTableRowsDeleted(0, rowMax - 1);
    }
  }

  public void dispose() {
    highlightManagerTableModel = null;
  }

  public void fireTableChanged(TableModelEvent e) {
    super.fireTableChanged(e);
    fireHighlightChangeListener(highlightEnable);
  }

  public void addHighlightChangeListener(HighlightChangeListener listener) {
    if (!highlightChangeListeners.contains(listener)) {
      highlightChangeListeners.add(listener);
    }
  }

  public void removeHighlightChangeListener(HighlightChangeListener listener) {
    highlightChangeListeners.remove(listener);
  }


  public void fireHighlightChangeListener(boolean highlightEnabled) {
    for (int i = 0; i < highlightChangeListeners.size(); i++) {
      final HighlightChangeListener listener = (HighlightChangeListener) highlightChangeListeners.get(i);
      listener.highlightUpdated(highlightEnabled);
    }
  }

  /**
   * Returns the number of highlights.
   *
   * @return how many highlights are in
   */
  public int countHighlights() {
    return getRowCount();
  }

  /**
   * If the highlights must not be displayed it will returns false.
   *
   * @return returns true if highlights are displayed, false otherwise
   */
  public boolean isHighlightEnable() {
    return highlightEnable;
  }

  /**
   * Enable or disable the highlights.
   *
   * @param highlightEnable the news status
   */
  public void setHighlightEnable(boolean highlightEnable) {
    this.highlightEnable = highlightEnable;
    fireHighlightChangeListener(highlightEnable);
  }
}
