package gatchan.highlight;

import org.gjt.sp.util.Log;

import javax.swing.table.AbstractTableModel;
import javax.swing.event.TableModelEvent;
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

  private List highlightChangeListeners = new ArrayList(2);
  private boolean highlightEnable = true;

  public static HighlightManagerTableModel getInstance() {
    if (highlightManagerTableModel == null) {
      highlightManagerTableModel = new HighlightManagerTableModel();
    }
    return highlightManagerTableModel;
  }

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
    return 2;
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
    fireTableCellUpdated(rowIndex, 0);
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
    Log.log(Log.WARNING, this, "An unknown highlight was asked to be removed from list" + o);
  }

  /**
   * Check if the list contains a highlight.
   *
   * @param o the highlight
   *
   * @return true if the highlight is in the list
   */
  public boolean contains(Object o) {
    return datas.contains(o);
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
    fireHighlightChangeListener();
  }

  public void addHighlightChangeListener(HighlightChangeListener listener) {
    if (!highlightChangeListeners.contains(listener)) {
      highlightChangeListeners.add(listener);
    }
  }

  public void removeHighlightChangeListener(HighlightChangeListener listener) {
    highlightChangeListeners.remove(listener);
  }


  public void fireHighlightChangeListener() {
    for (int i = 0; i < highlightChangeListeners.size(); i++) {
      final HighlightChangeListener listener = (HighlightChangeListener) highlightChangeListeners.get(i);
      listener.highlightUpdated();

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
    fireHighlightChangeListener();
  }
}
