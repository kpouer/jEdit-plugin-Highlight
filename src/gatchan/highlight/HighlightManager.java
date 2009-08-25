/*
 * HighlightManager.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Matthieu Casanova
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

import org.gjt.sp.jedit.Buffer;

import javax.swing.event.CaretListener;

/**
 * The highlight manager.
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightManager.java,v 1.6 2005/09/12 20:07:37 kpouer Exp $
 */
public interface HighlightManager extends CaretListener
{

  /**
   * Add a Highlight listener.
   *
   * @param listener the new listener
   */
  void addHighlightChangeListener(HighlightChangeListener listener);

  /**
   * Remove a highlight listener.
   *
   * @param listener the listener to be removed
   */
  void removeHighlightChangeListener(HighlightChangeListener listener);

  /**
   * alert the listeners that the highlights changed
   */
  void fireHighlightChangeListener(boolean highlightEnable);

  /**
   * Returns the number of highlights.
   *
   * @return how many highlights are in
   */
  int countHighlights();

  /**
   * Return the Highlight at index i.
   *
   * @param i the index of the highlight
   * @return a highlight
   */
  Highlight getHighlight(int i);

  /**
   * Add a Highlight in the list.
   *
   * @param highlight the highlight to be added
   */
  void addElement(Highlight highlight);

  void dispose();

  /**
   * Enable or disable the highlights.
   *
   * @param highlightEnable the news status
   */
  void setHighlightEnable(boolean highlightEnable);

  /**
   * If the highlights must not be displayed it will returns false.
   *
   * @return returns true if highlights are displayed, false otherwise
   */
  boolean isHighlightEnable();

  /** remove all Highlights. */
  void removeAll();

  /**
   * Remove an element at the specified index.
   *
   * @param index the index
   */
  void removeRow(int index);

  /**
   * A buffer is closed. We must remove all highlights associated.
   *
   * @param buffer the removed buffer
   */
  void bufferClosed(Buffer buffer);

  boolean isHighlightWordAtCaret();

  void propertiesChanged();

  void getReadLock();
  void releaseLock();

    //{{{ isHighlightSelection() method
    boolean isHighlightSelection() //}}}
            ;
}
