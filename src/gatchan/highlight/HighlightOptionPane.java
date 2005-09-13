package gatchan.highlight;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.jEdit;

import javax.swing.*;

/**
 * The option pane of the Highlight plugin.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public class HighlightOptionPane extends AbstractOptionPane {

  public static final String PROP_HIGHLIGHT_WORD_AT_CARET = "gatchan.highlight.caretHighlight";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE = "gatchan.highlight.caretHighlightIgnoreCase";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD = "gatchan.highlight.caretHighlightEntireWord";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_COLOR = "gatchan.highlight.caretHighlightColor";
  private JCheckBox highlightWordAtCaret;
  private JCheckBox wordAtCaretIgnoreCase;
  private JCheckBox entireWord;
  private ColorWellButton wordAtCaretColor;

  public HighlightOptionPane() {
    super("gatchan.highlight");
  }

  protected void _init() {
    addComponent(new JLabel(jEdit.getProperty(PROP_HIGHLIGHT_WORD_AT_CARET+".text")));
    addComponent(highlightWordAtCaret = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET));
    addComponent(wordAtCaretIgnoreCase = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE));
    addComponent(entireWord = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD));
    addComponent(wordAtCaretColor = new ColorWellButton(jEdit.getColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR)));
  }

  protected void _save() {
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET, highlightWordAtCaret.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE, wordAtCaretIgnoreCase.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD, entireWord.isSelected());
    jEdit.setColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR, wordAtCaretColor.getSelectedColor());
  }

  private static JCheckBox createCheckBox(String property) {
    JCheckBox checkbox = new JCheckBox(jEdit.getProperty(property + ".text"));
    checkbox.setSelected(jEdit.getBooleanProperty(property));
    return checkbox;
  }
}
