/*
 * HighlightManagerTableModel.java - The Highlight manager implementation
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2010 Matthieu Casanova
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

//{{{ imports
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
//}}}

/**
 * The tableModel that will contains the highlights. It got two columns,
 * the first is a checkbox to enable/disable the
 * highlight, the second is the highlight view
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightManagerTableModel.java,v 1.21 2006/07/05 21:35:17 kpouer Exp $
 */
public class HighlightManagerTableModel extends AbstractTableModel implements HighlightManager
{
	private static final String ENABLED_PROP = HighlightPlugin.PROPERTY_PREFIX + "enabled";

	private final List<Highlight> datas = new ArrayList<Highlight>();
	private static HighlightManagerTableModel highlightManagerTableModel;

	private final List<HighlightChangeListener> highlightChangeListeners = new ArrayList<HighlightChangeListener>(2);
	private final File highlights;

	private final ReentrantReadWriteLock lock;

	public static Highlight currentWordHighlight;
	public static Highlight selectionHighlight;

	private boolean highlightWordAtCaret;
	private boolean highlightWordAtCaretEntireWord;
	private boolean highlightWordAtCaretWhitespace;
	private boolean highlightWordAtCaretOnlyWords;

	private boolean highlightSelection;

	/**
	 * If true the highlight will be appended, if false the highlight will replace the previous one.
	 */
	private boolean appendHighlight = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_APPEND);

	public static final String FILE_VERSION = "Highlight file v2";
	private Timer timer;

	//{{{ createInstance() method
	/**
	 * This method is only called by the HighlightPlugin during startup.
	 *
	 * @param highlightFile the highlight file. If it is null, no file will be loaded or saved
	 * @return the Highlight manager
	 */
	static HighlightManager createInstance(File highlightFile)
	{
		highlightManagerTableModel = new HighlightManagerTableModel(highlightFile);
		return highlightManagerTableModel;
	} //}}}

	//{{{ getInstance() method
	/**
	 * Returns the instance of the HighlightManagerTableModel.
	 *
	 * @return the instance
	 */
	public static HighlightManagerTableModel getInstance()
	{
		return highlightManagerTableModel;
	} //}}}

	//{{{ getManager() method
	/**
	 * Returns the HighlightManager.
	 *
	 * @return the HighlightManager
	 */
	public static HighlightManager getManager()
	{
		return getInstance();
	} //}}}

	//{{{ HighlightManagerTableModel constructor
	private HighlightManagerTableModel(File highlightFile)
	{
		lock = new ReentrantReadWriteLock();
		highlights = highlightFile;
		currentWordHighlight = new Highlight();
		selectionHighlight = new Highlight();
		if (highlights != null && highlights.exists())
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
						addElement(Highlight.unserialize(line, getStatus), false);
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
		highlightSelection = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION);
		highlightWordAtCaretEntireWord = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD);
		highlightWordAtCaretWhitespace = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE);
		highlightWordAtCaretOnlyWords = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS);
		currentWordHighlight.init(" ",
					  highlightWordAtCaretEntireWord,
					  jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE),
					  jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_COLOR));
		selectionHighlight.init(" ",
					false,
					jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION_IGNORE_CASE),
					jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION_COLOR));
		currentWordHighlight.setEnabled(false);
		selectionHighlight.setEnabled(false);
		timer = new Timer(1000, new RemoveExpired());
		timer.start();
	} //}}}

	//{{{ getRowCount() method
	/**
	 * Returns the number of highlights in the list.
	 *
	 * @return the number of highlights
	 */
	public int getRowCount()
	{
		try
		{
			lock.readLock().lock();
			return datas.size();
		}
		finally
		{
			lock.readLock().unlock();
		}
	} //}}}

	//{{{ getColumnCount() method
	/**
	 * @return 4
	 */
	public int getColumnCount()
	{
		return 4;
	} //}}}

	//{{{ getColumnClass() method
	@Override
	public Class getColumnClass(int columnIndex)
	{
		return columnIndex == 0 ? Boolean.class : Highlight.class;
	} //}}}

	//{{{ isCellEditable() method
	/**
	 * All cells are editable.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return true
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return true;
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Object o;
		try
		{
			lock.readLock().lock();
			o = datas.get(rowIndex);
		}
		finally
		{
			lock.readLock().unlock();
		}
		if (columnIndex == 0)
		{
			return ((Highlight) o).isEnabled();
		}
		return o;
	} //}}}

	//{{{ setValueAt() method
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex == 0)
		{
			Highlight highlight;
			try
			{
				lock.readLock().lock();
				highlight = datas.get(rowIndex);
			}
			finally
			{
				lock.readLock().unlock();
			}
			highlight.setEnabled((Boolean) aValue);
		}
		else
		{
			try
			{
				lock.writeLock().lock();
				datas.set(rowIndex, (Highlight) aValue);
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}
		fireTableCellUpdated(rowIndex, columnIndex);
	} //}}}

	//{{{ getHighlight() method
	/**
	 * Return the Highlight at index i.
	 * It must be called under the rwLock
	 *
	 * @param i the index of the highlight
	 * @return a highlight
	 */
	public Highlight getHighlight(int i)
	{
		return datas.get(i);
	} //}}}

	//{{{ addElement() methods
	/**
	 * Add a Highlight in the list. Also enables highlighting.
	 *
	 * @param highlight the highlight to be added
	 */
	public void addElement(Highlight highlight)
	{
		addElement(highlight, true);
	}

	/**
	 * Add a Highlight in the list.
	 *
	 * @param highlight the highlight to be added
	 * @param enable whether to enable highlighting
	 */
	private void addElement(Highlight highlight, boolean enable)
	{
		if (indexOf(highlight) == -1)
		{
			if (appendHighlight || datas.isEmpty())
			{
				int firstRow;
				try
				{
					lock.writeLock().lock();
					datas.add(highlight);
					firstRow = datas.size() - 1;
				}
				finally
				{
					lock.writeLock().unlock();
				}
				fireTableRowsInserted(firstRow, firstRow);
			}
			else
			{
				int firstRow;
				Highlight replacedHighlight;
				try
				{
					lock.readLock().lock();
					firstRow = datas.size() - 1;
					replacedHighlight = datas.get(firstRow);
				}
				finally
				{
					lock.readLock().unlock();
				}
				replacedHighlight.init(highlight.getStringToHighlight(),
						       highlight.isRegexp(),
						       highlight.isIgnoreCase(),
						       highlight.getColor());

				fireTableRowsUpdated(firstRow, firstRow);
			}
		}
		if (enable)
			setHighlightEnable(true);
	} //}}}

	//{{{ removeRow() methods
	/**
	 * Remove an element at the specified index.
	 *
	 * @param index the index
	 */
	public void removeRow(int index)
	{
		try
		{
			lock.writeLock().lock();
			datas.remove(index);
		}
		finally
		{
			lock.writeLock().unlock();
		}
		fireTableRowsDeleted(index, index);
	}

	/**
	 * Remove an item.
	 *
	 * @param item the item to be removed
	 */
	private void removeRow(Highlight item)
	{
		int index = indexOf(item);
		if (index != -1)
			removeRow(index);
	} //}}}

	//{{{ bufferClosed() method
	private int indexOf(Highlight highlight)
	{
		try
		{
			lock.readLock().lock();
			int i = datas.indexOf(highlight);
			return i;
		}
		finally
		{
			lock.readLock().unlock();
		}
	} //}}}

	//{{{ bufferClosed() method
	/**
	 * A buffer is closed, we will remove all highlights from this buffer.
	 *
	 * @param buffer the closed buffer
	 */
	public void bufferClosed(Buffer buffer)
	{
		List<Highlight> highlights = (List<Highlight>) buffer.getProperty(Highlight.HIGHLIGHTS_BUFFER_PROPS);
		if (highlights != null)
		{
			for (int i = 0; i < highlights.size(); i++)
			{
				removeRow(highlights.get(i));
			}
		}
	} //}}}

	//{{{ removeAll() method
	/**
	 * remove all Highlights.
	 */
	public void removeAll()
	{
		int rowMax;
		try
		{
			lock.writeLock().lock();
			rowMax = datas.size();
			datas.clear();
		}
		finally
		{
			lock.writeLock().unlock();
		}
		if (rowMax != 0)
		{
			fireTableRowsDeleted(0, rowMax - 1);
		}
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		timer.stop();
		timer = null;
		highlightManagerTableModel = null;
		selectionHighlight = null;
		save();
	} //}}}

	//{{{ save() method
	private void save()
	{
		if (highlights != null)
		{
			PrintWriter writer = null;
			try
			{
				File parentFile = highlights.getParentFile();
				if (!parentFile.isDirectory())
					parentFile.mkdirs();
				writer = new PrintWriter(highlights);
				writer.println(FILE_VERSION);
				try
				{
					lock.writeLock().lock();
					ListIterator<Highlight> listIterator = datas.listIterator();
					while (listIterator.hasNext())
					{
						Highlight highlight = listIterator.next();
						if (highlight.getScope() == Highlight.PERMANENT_SCOPE)
						{
							writer.println(highlight.serialize());
						}
						else
						{
							listIterator.remove();
						}
					}
				}
				finally
				{
					lock.writeLock().unlock();
				}
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, this, e);
			}
			finally
			{
				IOUtilities.closeQuietly(writer);
			}
		}
		else
			Log.log(Log.ERROR, this, "No settings");
	} //}}}

	//{{{ fireTableChanged() method
	@Override
	public void fireTableChanged(TableModelEvent e)
	{
		super.fireTableChanged(e);
		fireHighlightChangeListener(isHighlightEnable());
	} //}}}

	//{{{ HighlightChangeListener methods
	//{{{ addHighlightChangeListener() method
	public void addHighlightChangeListener(HighlightChangeListener listener)
	{
		if (!highlightChangeListeners.contains(listener))
		{
			highlightChangeListeners.add(listener);
		}
	} //}}}

	//{{{ removeHighlightChangeListener() method
	public void removeHighlightChangeListener(HighlightChangeListener listener)
	{
		highlightChangeListeners.remove(listener);
	} //}}}

	//{{{ fireHighlightChangeListener() method
	public void fireHighlightChangeListener(boolean highlightEnable)
	{
		for (int i = 0; i < highlightChangeListeners.size(); i++)
		{
			HighlightChangeListener listener = highlightChangeListeners.get(i);
			listener.highlightUpdated(highlightEnable);
		}
	} //}}}
	//}}}

	//{{{ countHighlights() method
	/**
	 * Returns the number of highlights.
	 *
	 * @return how many highlights are in
	 */
	public int countHighlights()
	{
		return getRowCount();
	} //}}}

	//{{{ isHighlightEnable() method
	/**
	 * If the highlights must not be displayed it will returns false.
	 *
	 * @return returns true if highlights are displayed, false otherwise
	 */
	public boolean isHighlightEnable()
	{
		return jEdit.getBooleanProperty(ENABLED_PROP);
	} //}}}

	//{{{ setHighlightEnable() method
	/**
	 * Enable or disable the highlights.
	 *
	 * @param highlightEnable the news status
	 */
	public void setHighlightEnable(boolean highlightEnable)
	{
		jEdit.setBooleanProperty(ENABLED_PROP, highlightEnable);
		fireHighlightChangeListener(highlightEnable);
	} //}}}

	//{{{ RemoveExpired class
	private class RemoveExpired implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			List<Highlight> expired = null;
			try
			{
				lock.readLock().lock();
				for (int i = 0; i < datas.size(); i++)
				{
					Highlight highlight = datas.get(i);
					if (highlight.isExpired())
					{
						if (expired == null)
						{
							expired = new ArrayList<Highlight>();
						}
						expired.add(highlight);
					}
				}
			}
			finally
			{
				lock.readLock().unlock();
			}
			if (expired != null)
			{
				for (int i = 0; i < expired.size(); i++)
				{
					Highlight highlight = expired.get(i);
					removeRow(highlight);
				}
			}
		}
	} //}}}

	//{{{ caretUpdate() method
	public void caretUpdate(CaretEvent e)
	{
		JEditTextArea textArea = (JEditTextArea) e.getSource();
		int line = textArea.getCaretLine();
		boolean updated = false;
		if (highlightWordAtCaret)
		{
			if (textArea.getLineLength(line) == 0 || textArea.getSelectionCount() != 0)
			{
				if (currentWordHighlight.isEnabled())
				{
					updated = true;
					currentWordHighlight.setEnabled(false);
				}
			}
			else
			{
				int lineStart = textArea.getLineStartOffset(line);
				int offset = textArea.getCaretPosition() - lineStart;

				JEditBuffer buffer = textArea.getBuffer();
				CharSequence lineText = buffer.getLineSegment(line);
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
					if (currentWordHighlight.isEnabled())
					{
						updated = true;
						currentWordHighlight.setEnabled(false);
					}
				}
				else
				{
					int wordEnd = TextUtilities.findWordEnd(lineText, offset + 1, noWordSep);

					if (wordEnd - wordStart < 2)
					{
						if (currentWordHighlight.isEnabled())
						{
							updated = true;
							currentWordHighlight.setEnabled(false);
						}
					}
					else
					{
						if (!currentWordHighlight.isEnabled())
						{
							updated = true;
							currentWordHighlight.setEnabled(true);
						}
						String stringToHighlight = lineText.subSequence(wordStart, wordEnd).toString();
						if (highlightWordAtCaretEntireWord)
						{
							stringToHighlight = "\\b" + stringToHighlight + "\\b";
							if (!stringToHighlight.equals(currentWordHighlight.getStringToHighlight()))
							{
								updated = true;
								currentWordHighlight.init(stringToHighlight,
									true,
									currentWordHighlight.isIgnoreCase(),
									currentWordHighlight.getColor());
							}

						}
						else
						{
							if (!stringToHighlight.equals(currentWordHighlight.getStringToHighlight()))
							{
								updated = true;
								currentWordHighlight.setStringToHighlight(stringToHighlight);
							}
						}
					}
				}
			}
		}
		if (highlightSelection)
		{
			Selection selectionatOffset = textArea.getSelectionAtOffset(e.getDot());
			if (textArea.getLineLength(line) == 0 ||
			    selectionatOffset == null ||
			    selectionatOffset.getStartLine() != selectionatOffset.getEndLine() ||
				selectionatOffset.getEnd() - selectionatOffset.getStart() == 0)
			{
				if (selectionHighlight.isEnabled())
				{
					updated = true;
					selectionHighlight.setEnabled(false);
				}
			}
			else
			{
				String stringToHighlight = textArea.getSelectedText(selectionatOffset);
				if (!selectionHighlight.isEnabled() ||
					!stringToHighlight.equals(selectionHighlight.getStringToHighlight()))
				{
					updated = true;
					selectionHighlight.setEnabled(true);
					selectionHighlight.setStringToHighlight(stringToHighlight);
				}
			}
		}
		if (updated)
			fireHighlightChangeListener(isHighlightEnable());
	} //}}}

	//{{{ isHighlightWordAtCaret() method
	public boolean isHighlightWordAtCaret()
	{
		return highlightWordAtCaret;
	} //}}}


	//{{{ isHighlightSelection() method
	public boolean isHighlightSelection()
	{
		return highlightSelection;
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		//{{{ PROP_HIGHLIGHT_CYCLE_COLOR
		if (jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_CYCLE_COLOR))
		{
			Highlight.setDefaultColor(null);
		}
		else
		{
			Highlight.setDefaultColor(jEdit.getColorProperty(HighlightOptionPane.PROP_DEFAULT_COLOR));
		} //}}}

		appendHighlight = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_APPEND);
		boolean changed = false;
		boolean changedSelection = false;

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET
		boolean highlightWordAtCaret = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET);
		if (this.highlightWordAtCaret != highlightWordAtCaret)
		{
			changed = true;
			this.highlightWordAtCaret = highlightWordAtCaret;
			if (!highlightWordAtCaret)
				currentWordHighlight.setEnabled(false);
		} //}}}

		//{{{ PROP_HIGHLIGHT_SELECTION
		boolean highlightSelection = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION);
		if (this.highlightSelection != highlightSelection)
		{
			changedSelection = true;
			this.highlightSelection = highlightSelection;
			if (!highlightSelection)
				selectionHighlight.setEnabled(false);
		} //}}}

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD
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
		} //}}}

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE
		boolean whitespace = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE);
		if (highlightWordAtCaretWhitespace != whitespace)
		{
			changed = true;
			highlightWordAtCaretWhitespace = whitespace;
		} //}}}

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS
		boolean onlyWords = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS);
		if (highlightWordAtCaretOnlyWords != onlyWords)
		{
			changed = true;
			highlightWordAtCaretOnlyWords = onlyWords;
		} //}}}

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE
		boolean ignoreCase = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE);
		if (currentWordHighlight.isIgnoreCase() != ignoreCase)
		{
			changed = true;
		} //}}}

		//{{{ PROP_HIGHLIGHT_SELECTION_IGNORE_CASE
		boolean selectionIgnoreCase = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION_IGNORE_CASE);
		if (selectionHighlight.isIgnoreCase() != ignoreCase)
		{
			changedSelection = true;
		} //}}}

		//{{{ PROP_HIGHLIGHT_WORD_AT_CARET_COLOR
		Color newColor = jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_WORD_AT_CARET_COLOR);
		if (!currentWordHighlight.getColor().equals(newColor))
		{
			changed = true;
		} //}}}

		//{{{ PROP_HIGHLIGHT_SELECTION_COLOR
		Color selectionNewColor = jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_SELECTION_COLOR);
		if (!selectionHighlight.getColor().equals(selectionNewColor))
		{
			changedSelection = true;
		} //}}}

		Highlighter.square = jEdit.getBooleanProperty(HighlightOptionPane.PROP_SQUARE);
		Highlighter.squareColor = jEdit.getColorProperty(HighlightOptionPane.PROP_SQUARE_COLOR);

		if (changed)
		{
			currentWordHighlight.init(currentWordHighlight.getStringToHighlight(), entireWord, ignoreCase, newColor);
		}
		if (changedSelection)
		{
			selectionHighlight.init(selectionHighlight.getStringToHighlight(), false, selectionIgnoreCase, selectionNewColor);
		}
		if (changed || changedSelection)
			fireHighlightChangeListener(isHighlightEnable());
	} //}}}

	//{{{ getReadLock() method
	public void getReadLock()
	{
		lock.readLock().lock();
	} //}}}

	//{{{ releaseLock() method
	public void releaseLock()
	{
		lock.readLock().unlock();
	} //}}}
}
