package gatchan.highlight;

import org.gjt.sp.jedit.Buffer;

import javax.swing.event.CaretListener;

/**
 * The highlight manager.
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightManager.java,v 1.6 2005/09/12 20:07:37 kpouer Exp $
 */
public interface HighlightManager extends CaretListener {

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
}
