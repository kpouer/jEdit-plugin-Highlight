package gatchan.highlight;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * The tableModel that will contains the highlights. It got two columns, the first is a checkbox to enable/disable the
 * highlight, the second is the highlight view
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightManagerTableModel.java,v 1.21 2006/07/05 21:35:17 kpouer Exp $
 */
public class HighlightManagerTableModel extends AbstractTableModel implements HighlightManager
{
    private final List datas = new ArrayList();
    private static HighlightManagerTableModel highlightManagerTableModel;

    private final List highlightChangeListeners = new ArrayList(2);
    private boolean highlightEnable = true;
    private final String PROJECT_DIRECTORY = jEdit.getSettingsDirectory() + File.separator + "HighlightPlugin" + File.separator;
    private final File projectDirectory = new File(PROJECT_DIRECTORY);
    private final File highlights = new File(projectDirectory, "highlights.ser");

    private RWLock rwLock = new RWLock();

    public static Highlight currentWordHighlight;
    private boolean highlightWordAtCaret;
    private boolean highlightWordAtCaretEntireWord;
    private boolean highlightWordAtCaretWhitespace;
    private boolean highlightWordAtCaretOnlyWords;

    /**
     * If true the highlight will be appended, if false the highlight will replace the previous one.
     */
    private boolean appendHighlight = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_APPEND);

    public static final String FILE_VERSION = "Highlight file v2";

    /**
     * Returns the instance of the HighlightManagerTableModel.
     *
     * @return the instance
     */
    public static HighlightManagerTableModel getInstance()
    {
        if (highlightManagerTableModel == null)
        {
            highlightManagerTableModel = new HighlightManagerTableModel();
        }
        return highlightManagerTableModel;
    }

    /**
     * Returns the HighlightManager.
     *
     * @return the HighlightManager
     */
    public static HighlightManager getManager()
    {
        return getInstance();
    }

    private HighlightManagerTableModel()
    {
        currentWordHighlight = new Highlight();
        if (highlights.exists())
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(highlights));
                String line = reader.readLine();
                boolean getStatus = false;
                if (FILE_VERSION.equals(line))
                {
                    getStatus = true;
                    line = reader.readLine();
                }
                while (line != null)
                {
                    try
                    {
                        addElement(Highlight.unserialize(line, getStatus));
                    }
                    catch (InvalidHighlightException e)
                    {
                        Log.log(Log.WARNING, this, "Unable to read this highlight, please report it : " + line);
                    }
                    line = reader.readLine();
                }
            }
            catch (FileNotFoundException e)
            {
                Log.log(Log.ERROR, this, e);
            }
            catch (IOException e)
            {
                Log.log(Log.ERROR, this, e);
            }
            finally
            {
                IOUtilities.closeQuietly(reader);
            }
        }
        highlightWordAtCaret = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET);
        highlightWordAtCaretEntireWord = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD);
        highlightWordAtCaretWhitespace = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE);
        highlightWordAtCaretOnlyWords = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS);
        currentWordHighlight.init(" ",
                                  highlightWordAtCaretEntireWord,
                                  jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE),
                                  jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_COLOR));
        currentWordHighlight.setEnabled(false);
        Timer timer = new Timer(1000, new RemoveExpired());
        timer.start();
    }

    private static boolean checkProjectDirectory(File projectDirectory)
    {
        return projectDirectory.isDirectory() || projectDirectory.mkdirs();
    }

    /**
     * Returns the number of highlights in the list.
     *
     * @return the number of highlights
     */
    public int getRowCount()
    {
        try
        {
            rwLock.getReadLock();
            return datas.size();
        }
        finally
        {
            rwLock.releaseLock();
        }
    }

    /**
     * Returns 2 because there is only two column.
     *
     * @return 2
     */
    public int getColumnCount()
    {
        return 3;
    }

    public Class getColumnClass(int columnIndex)
    {
        return columnIndex == 0 ? Boolean.class : Highlight.class;
    }

    /**
     * All cells are editable.
     *
     * @param rowIndex
     * @param columnIndex
     * @return true
     */
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return true;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Object o;
        try
        {
            rwLock.getReadLock();
            o = datas.get(rowIndex);
        }
        finally
        {
            rwLock.releaseLock();
        }
        if (columnIndex == 0)
        {
            return Boolean.valueOf(((Highlight) o).isEnabled());
        }
        return o;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (columnIndex == 0)
        {
            Highlight highlight;
            try
            {
                rwLock.getReadLock();
                highlight = (Highlight) datas.get(rowIndex);
            }
            finally
            {
                rwLock.releaseLock();
            }
            highlight.setEnabled(((Boolean) aValue).booleanValue());
        }
        else
        {
            try
            {
                rwLock.getWriteLock();
                datas.set(rowIndex, aValue);
            }
            finally
            {
                rwLock.releaseLock();
            }
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    /**
     * Return the Highlight at index i.
     *
     * @param i the index of the highlight
     * @return a highlight
     */
    public Highlight getHighlight(int i)
    {
        Highlight highlight;
        try
        {
            rwLock.getReadLock();
            highlight = (Highlight) datas.get(i);
        }
        finally
        {
            rwLock.releaseLock();
        }
        return highlight;
    }

    /**
     * Add a Highlight in the list.
     *
     * @param highlight the highlight to be added
     */
    public void addElement(Highlight highlight)
    {
        rwLock.getWriteLock();
        if (datas.contains(highlight))
        {
            rwLock.releaseLock();
        }
        else
        {
            if (appendHighlight || datas.isEmpty())
            {
                datas.add(highlight);
                int firstRow = datas.size() - 1;
                rwLock.releaseLock();
                fireTableRowsInserted(firstRow, firstRow);
            }
            else
            {
                int firstRow = datas.size() - 1;
                Highlight replacedHighlight = (Highlight) datas.get(firstRow);
                rwLock.releaseLock();
                replacedHighlight.init(highlight.getStringToHighlight(),
                                       highlight.isRegexp(),
                                       highlight.isIgnoreCase(),
                                       highlight.getColor());

                fireTableRowsUpdated(firstRow, firstRow);
            }
        }
        setHighlightEnable(true);
    }

    /**
     * Remove an element at the specified index.
     *
     * @param index the index
     */
    public void removeRow(int index)
    {
        try
        {
            rwLock.getWriteLock();
            datas.remove(index);
        }
        finally
        {
            rwLock.releaseLock();
        }
        fireTableRowsDeleted(index, index);
    }

    /**
     * Remove an item.
     *
     * @param item the item to be removed
     */
    private void removeRow(Object item)
    {
        int index;
        try
        {
            rwLock.getReadLock();
            index = datas.indexOf(item);
        }
        finally
        {
            rwLock.releaseLock();
        }
        removeRow(index);
    }

    /**
     * A buffer is closed, we will remove all highlights from this buffer.
     *
     * @param buffer the closed buffer
     */
    public void bufferClosed(Buffer buffer)
    {
        List highlights = (List) buffer.getProperty("highlights");
        if (highlights != null)
        {
            for (int i = 0; i < highlights.size(); i++)
            {
                removeRow(highlights.get(i));
            }
        }
    }

    /**
     * remove all Highlights.
     */
    public void removeAll()
    {
        int rowMax;
        try
        {
            rwLock.getWriteLock();
            rowMax = datas.size();
            datas.clear();
        }
        finally
        {
            rwLock.releaseLock();
        }
        if (rowMax != 0)
        {
            fireTableRowsDeleted(0, rowMax - 1);
        }
    }

    public void dispose()
    {
        highlightManagerTableModel = null;
        currentWordHighlight = null;
        save();
    }

    private void save()
    {
        if (checkProjectDirectory(projectDirectory))
        {
            BufferedWriter writer = null;
            try
            {
                writer = new BufferedWriter(new FileWriter(highlights));
                writer.write(FILE_VERSION);
                writer.write('\n');
                try
                {
                    rwLock.getWriteLock();
                    ListIterator listIterator = datas.listIterator();
                    while (listIterator.hasNext())
                    {
                        Highlight highlight = (Highlight) listIterator.next();
                        if (highlight.getScope() == Highlight.PERMANENT_SCOPE)
                        {
                            writer.write(highlight.serialize());
                            writer.write('\n');
                        }
                        else
                        {
                            listIterator.remove();
                        }
                    }
                }
                finally
                {
                    rwLock.releaseLock();
                }
            }
            catch (IOException e)
            {
                Log.log(Log.ERROR, this, e);
            }
            finally
            {
                if (writer != null)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }
        else
        {
            Log.log(Log.ERROR, this, "Unable to create directory " + projectDirectory.getAbsolutePath());
            GUIUtilities.error(jEdit.getActiveView(),
                               "gatchan-highlight.errordialog.unableToAccessProjectDirectory",
                               new String[]{projectDirectory.getAbsolutePath()});
        }
    }

    public void fireTableChanged(TableModelEvent e)
    {
        super.fireTableChanged(e);
        fireHighlightChangeListener(highlightEnable);
    }

    public void addHighlightChangeListener(HighlightChangeListener listener)
    {
        if (!highlightChangeListeners.contains(listener))
        {
            highlightChangeListeners.add(listener);
        }
    }

    public void removeHighlightChangeListener(HighlightChangeListener listener)
    {
        highlightChangeListeners.remove(listener);
    }


    public void fireHighlightChangeListener(boolean highlightEnable)
    {
        for (int i = 0; i < highlightChangeListeners.size(); i++)
        {
            HighlightChangeListener listener = (HighlightChangeListener) highlightChangeListeners.get(i);
            listener.highlightUpdated(highlightEnable);
        }
    }

    /**
     * Returns the number of highlights.
     *
     * @return how many highlights are in
     */
    public int countHighlights()
    {
        return getRowCount();
    }

    /**
     * If the highlights must not be displayed it will returns false.
     *
     * @return returns true if highlights are displayed, false otherwise
     */
    public boolean isHighlightEnable()
    {
        return highlightEnable;
    }

    /**
     * Enable or disable the highlights.
     *
     * @param highlightEnable the news status
     */
    public void setHighlightEnable(boolean highlightEnable)
    {
        this.highlightEnable = highlightEnable;
        fireHighlightChangeListener(highlightEnable);
    }

    private class RemoveExpired implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            List expired = null;
            try
            {
                rwLock.getReadLock();
                for (int i = 0; i < datas.size(); i++)
                {
                    Highlight highlight = (Highlight) datas.get(i);
                    if (highlight.isExpired())
                    {
                        if (expired == null)
                        {
                            expired = new ArrayList();
                        }
                        expired.add(highlight);
                    }
                }
            }
            finally
            {
                rwLock.releaseLock();
            }
            if (expired != null)
            {
                for (int i = 0; i < expired.size(); i++)
                {
                    Highlight highlight = (Highlight) expired.get(i);
                    removeRow(highlight);
                }
            }
        }
    }

    public void caretUpdate(CaretEvent e)
    {
        JEditTextArea textArea = (JEditTextArea) e.getSource();
        if (highlightWordAtCaret)
        {
            int line = textArea.getCaretLine();

            if (textArea.getLineLength(line) == 0 || textArea.getSelectionCount() != 0)
            {
                currentWordHighlight.setEnabled(false);
                fireHighlightChangeListener(highlightEnable);
                return;
            }

            int lineStart = textArea.getLineStartOffset(line);
            int offset = textArea.getCaretPosition() - lineStart;

            JEditBuffer buffer = textArea.getBuffer();
            String lineText = buffer.getLineText(line);
            String noWordSep = buffer.getStringProperty("noWordSep");

            if (offset != 0)
                offset--;

            int wordStart = TextUtilities.findWordStart(lineText, offset, noWordSep);
            char ch = lineText.charAt(wordStart);
            if ((!highlightWordAtCaretWhitespace && Character.isWhitespace(ch)) ||
                (highlightWordAtCaretOnlyWords &&
                 !Character.isLetterOrDigit(ch) &&
                 noWordSep.indexOf(ch) == -1))
            {
                currentWordHighlight.setEnabled(false);
            }
            else
            {


                int wordEnd = TextUtilities.findWordEnd(lineText, offset + 1, noWordSep);

                if (wordEnd - wordStart < 2)
                {
                    currentWordHighlight.setEnabled(false);
                }
                else
                {

                    currentWordHighlight.setEnabled(true);
                    String stringToHighlight = lineText.substring(wordStart, wordEnd);
                    if (highlightWordAtCaretEntireWord)
                    {
                        stringToHighlight = "\\b" + stringToHighlight + "\\b";
                        currentWordHighlight.init(stringToHighlight,
                                                  true,
                                                  currentWordHighlight.isIgnoreCase(),
                                                  currentWordHighlight.getColor());

                    }
                    else
                    {
                        currentWordHighlight.setStringToHighlight(stringToHighlight);
                    }
                }
            }
            fireHighlightChangeListener(highlightEnable);
        }
    }

    public boolean isHighlightWordAtCaret()
    {
        return highlightWordAtCaret;
    }

    public void propertiesChanged()
    {
        if (jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_CYCLE_COLOR))
        {
            Highlight.setDefaultColor(null);
        }
        else
        {
            Highlight.setDefaultColor(jEdit.getColorProperty(HighlightOptionPane.PROP_DEFAULT_COLOR));
        }
        appendHighlight = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_APPEND);
        boolean changed = false;

        boolean highlightWordAtCaret = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET);
        if (this.highlightWordAtCaret != highlightWordAtCaret)
        {
            changed = true;
            this.highlightWordAtCaret = highlightWordAtCaret;
            if (!highlightWordAtCaret)
                currentWordHighlight.setEnabled(false);
        }

        boolean entireWord = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD);
        if (highlightWordAtCaretEntireWord != entireWord)
        {
            changed = true;
            highlightWordAtCaretEntireWord = entireWord;
            if (entireWord)
            {
                String s = currentWordHighlight.getStringToHighlight();
                currentWordHighlight.setStringToHighlight("\\b" + s + "\\b");
            }
        }

        boolean whitespace = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE);
        if (highlightWordAtCaretWhitespace != whitespace)
        {
            changed = true;
            highlightWordAtCaretWhitespace = whitespace;
        }

        boolean onlyWords = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS);
        if (highlightWordAtCaretOnlyWords != onlyWords)
        {
            changed = true;
            highlightWordAtCaretOnlyWords = onlyWords;
        }

        boolean ignoreCase = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE);
        if (currentWordHighlight.isIgnoreCase() != ignoreCase)
        {
            changed = true;
        }
        Color newColor = jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_COLOR);
        if (!currentWordHighlight.getColor().equals(newColor))
        {
            changed = true;
        }

        if (currentWordHighlight.setHighlightSubsequence(jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_SUBSEQUENCE)))
        {
            changed = true;
        }

        if (changed)
        {
            currentWordHighlight.init(currentWordHighlight.getStringToHighlight(), entireWord, ignoreCase, newColor);
            fireHighlightChangeListener(highlightEnable);
        }
    }
}
