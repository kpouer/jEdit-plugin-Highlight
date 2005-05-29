package gatchan.highlight;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.io.*;

/**
 * The tableModel that will contains the highlights. It got two columns, the first is a checkbox to enable/disable the
 * highlight, the second is the highlight view
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public final class HighlightManagerTableModel extends AbstractTableModel implements HighlightManager {
  private final List datas = new ArrayList();
  private static HighlightManagerTableModel highlightManagerTableModel;

  private final List highlightChangeListeners = new ArrayList(2);
  private boolean highlightEnable = true;
  private final String PROJECT_DIRECTORY = jEdit.getSettingsDirectory() + File.separator + "HighlightPlugin" + File.separator;
  private final File projectDirectory = new File(PROJECT_DIRECTORY);
  private final File highlights = new File(projectDirectory, "highlights.ser");

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
    if (highlights.exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(highlights));
        String line = reader.readLine();
        while (line != null) {
          try {
            addElement(Highlight.unserialize(line));
          } catch (InvalidHighlightException e) {
            Log.log(Log.WARNING, this, "Unable to read this highlight, please report it : " + line);
          }
          line = reader.readLine();
        }
      } catch (FileNotFoundException e) {
        Log.log(Log.ERROR, this, e);
      } catch (IOException e) {
        Log.log(Log.ERROR, this, e);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
          }
        }
      }
    }
  }

  private static boolean checkProjectDirectory(File projectDirectory) {
    if (!projectDirectory.isDirectory()) {
      return projectDirectory.mkdirs();
    }
    return true;
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
      Highlight highlight = (Highlight) datas.get(rowIndex);
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
      int firstRow = datas.size() - 1;
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

  /**
   * Remove an item.
   *
   * @param item the item to be removed
   */
  private void removeRow(Object item) {
    removeRow(datas.indexOf(item));
  }

  /**
   * A buffer is closed, we will remove all highlights from this buffer.
   *
   * @param buffer the closed buffer
   */
  public void bufferClosed(Buffer buffer) {
    List highlights = (List) buffer.getProperty("highlights");
    if (highlights != null) {
      for (int i = 0; i < highlights.size(); i++) {
        removeRow(highlights.get(i));
      }
    }
  }

  /** remove all Highlights. */
  public void removeAll() {
    int rowMax = datas.size();
    datas.clear();
    if (rowMax != 0) {
      fireTableRowsDeleted(0, rowMax - 1);
    }
  }

  public void dispose() {
    highlightManagerTableModel = null;
    save();
  }

  private void save() {
    if (checkProjectDirectory(projectDirectory)) {
      BufferedWriter writer = null;
      try {
        writer = new BufferedWriter(new FileWriter(highlights));
        ListIterator listIterator = datas.listIterator();
        while (listIterator.hasNext()) {
          Highlight highlight = (Highlight) listIterator.next();
          if (highlight.getScope() == Highlight.PERMANENT_SCOPE) {
            writer.write(highlight.serialize());
            writer.write('\n');
          } else {
            listIterator.remove();
          }
        }
      } catch (IOException e) {
        Log.log(Log.ERROR, this, e);
      } finally {
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException e) {
          }
        }
      }
    } else {
      Log.log(Log.ERROR, this, "Unable to create directory " + projectDirectory.getAbsolutePath());
      GUIUtilities.error(jEdit.getActiveView(),
                         "gatchan-highlight.errordialog.unableToAccessProjectDirectory",
                         new String[]{projectDirectory.getAbsolutePath()});
    }
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
      HighlightChangeListener listener = (HighlightChangeListener) highlightChangeListeners.get(i);
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
