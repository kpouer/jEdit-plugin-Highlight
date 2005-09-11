package gatchan.highlight;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

  private RWLock rwLock = new RWLock();

  public static final Highlight currentWordHighlight = new Highlight();
  private boolean shouldHighlightCaret = false;
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
    shouldHighlightCaret = jEdit.getBooleanProperty("gatchan.highlight.caretHighlight");
    currentWordHighlight.setEnabled(false);
    Timer  timer = new Timer(1000, new RemoveExpired());
    timer.start();
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
    try {
      rwLock.getReadLock();
      return datas.size();
    } finally {
      rwLock.releaseLock();
    }
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
      Object o;
      try {
        rwLock.getReadLock();
        o = datas.get(rowIndex);
      } finally {
        rwLock.releaseLock();
      }
      return Boolean.valueOf(((Highlight) o).isEnabled());
    }
    rwLock.getReadLock();
    Object o = datas.get(rowIndex);
    rwLock.releaseLock();
    return o;
  }

  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      rwLock.getReadLock();
      Highlight highlight = (Highlight) datas.get(rowIndex);
      rwLock.releaseLock();
      highlight.setEnabled(((Boolean) aValue).booleanValue());
    } else {
      rwLock.getWriteLock();
      datas.set(rowIndex, aValue);
      rwLock.releaseLock();
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
    rwLock.getReadLock();
    Highlight highlight = (Highlight) datas.get(i);
    rwLock.releaseLock();
    return highlight;
  }

  /**
   * Add a Highlight in the list.
   *
   * @param highlight the highlight to be added
   */
  public void addElement(Highlight highlight) {
    rwLock.getWriteLock();
    if (datas.contains(highlight)) {
      rwLock.releaseLock();
    } else {
      datas.add(highlight);
      int firstRow = datas.size() - 1;
      rwLock.releaseLock();
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
    rwLock.getWriteLock();
    datas.remove(index);
    rwLock.releaseLock();
    fireTableRowsDeleted(index, index);
  }

  /**
   * Remove an item.
   *
   * @param item the item to be removed
   */
  private void removeRow(Object item) {
    rwLock.getReadLock();
    int index = datas.indexOf(item);
    rwLock.releaseLock();
    removeRow(index);
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
    rwLock.getWriteLock();
    int rowMax = datas.size();
    datas.clear();
    rwLock.releaseLock();
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
        rwLock.getWriteLock();
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
        rwLock.releaseLock();
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

  private class RemoveExpired implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      List expired = null;
      rwLock.getReadLock();
      for (int i = 0; i < datas.size(); i++) {
        Highlight highlight = (Highlight) datas.get(i);
        if (highlight.isExpired()) {
          if (expired == null) {
            expired = new ArrayList();
          }
          expired.add(highlight);
        }
      }
      rwLock.releaseLock();
      if (expired != null) {
        for (int i = 0; i < expired.size(); i++) {
          Highlight highlight = (Highlight) expired.get(i);
          removeRow(highlight);
        }
      }
    }
  }

  public void setShouldHighlightCaret(boolean shouldHighlightCaret) {
    this.shouldHighlightCaret = shouldHighlightCaret;
    if (!shouldHighlightCaret)
      currentWordHighlight.setEnabled(false);
  }

  public void caretUpdate(CaretEvent e) {
    JEditTextArea textArea = (JEditTextArea) e.getSource();
    if (shouldHighlightCaret) {
      int line = textArea.getCaretLine();
      int lineStart = textArea.getLineStartOffset(line);
      int offset = textArea.getCaretPosition() - lineStart;

      if (textArea.getLineLength(line) == 0)
        return;

      JEditBuffer buffer = textArea.getBuffer();
      String lineText = textArea.getLineText(line);
      String noWordSep = buffer.getStringProperty("noWordSep");

      if (offset == textArea.getLineLength(line))
        offset--;

      int wordStart = TextUtilities.findWordStart(lineText, offset, noWordSep);
      int wordEnd = TextUtilities.findWordEnd(lineText, offset + 1, noWordSep);

      if (wordEnd - wordStart < 2) {
        currentWordHighlight.setEnabled(false);
      } else {
        currentWordHighlight.setEnabled(true);
        String stringToHighlight = lineText.substring(wordStart, wordEnd);
        currentWordHighlight.setStringToHighlight(stringToHighlight);
      }
      fireHighlightChangeListener(true);
    }
  }

  public boolean isShouldHighlightCaret() {
    return shouldHighlightCaret;
  }
}
