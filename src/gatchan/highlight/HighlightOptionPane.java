package gatchan.highlight;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.jEdit;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * The option pane of the Highlight plugin.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public class HighlightOptionPane extends AbstractOptionPane {

  public static final String PROP_COMMON_PROPERTIES = "gatchan.highlight.option-pane.commonProperties.text";
  public static final String PROP_DEFAULT_COLOR = "gatchan.highlight.defaultColor";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET = "gatchan.highlight.caretHighlight";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE = "gatchan.highlight.caretHighlightIgnoreCase";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD = "gatchan.highlight.caretHighlightEntireWord";
  public static final String PROP_HIGHLIGHT_WORD_AT_CARET_COLOR = "gatchan.highlight.caretHighlightColor";
  public static final String PROP_HIGHLIGHT_CYCLE_COLOR = "gatchan.highlight.cycleColor";
  public static final String PROP_HIGHLIGHT_APPEND = "gatchan.highlight.appendHighlight";
  private JCheckBox highlightWordAtCaret;
  private JCheckBox wordAtCaretIgnoreCase;
  private JCheckBox entireWord;
  private ColorWellButton wordAtCaretColor;
  private JCheckBox cycleColor;
  private JCheckBox highlightAppend;
  private ColorWellButton defaultColor;

  public HighlightOptionPane() {
    super("gatchan.highlight");
  }

  protected void _init() {
    addComponent(new JLabel(jEdit.getProperty(PROP_COMMON_PROPERTIES)));
    addComponent(highlightAppend = createCheckBox(PROP_HIGHLIGHT_APPEND));
    addComponent(cycleColor = createCheckBox(PROP_HIGHLIGHT_CYCLE_COLOR));
    addComponent(new JLabel(jEdit.getProperty(PROP_DEFAULT_COLOR + ".text")),
                 defaultColor = new ColorWellButton(jEdit.getColorProperty(PROP_DEFAULT_COLOR)));
    cycleColor.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        defaultColor.setEnabled(!cycleColor.isSelected());
      }
    });
    if (cycleColor.isSelected())
      defaultColor.setEnabled(false);

    addComponent(new JLabel(jEdit.getProperty(PROP_HIGHLIGHT_WORD_AT_CARET + ".text")));
    addComponent(highlightWordAtCaret = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET));
    addComponent(wordAtCaretIgnoreCase = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE));
    addComponent(entireWord = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD));
    addComponent(new JLabel(jEdit.getProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR + ".text")),
                 wordAtCaretColor = new ColorWellButton(jEdit.getColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR)));
  }

  protected void _save() {
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET, highlightWordAtCaret.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE, wordAtCaretIgnoreCase.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD, entireWord.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_CYCLE_COLOR, cycleColor.isSelected());
    jEdit.setBooleanProperty(PROP_HIGHLIGHT_APPEND, highlightAppend.isSelected());
    jEdit.setColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR, wordAtCaretColor.getSelectedColor());
    jEdit.setColorProperty(PROP_DEFAULT_COLOR, defaultColor.getSelectedColor());
  }

  private static JCheckBox createCheckBox(String property) {
    JCheckBox checkbox = new JCheckBox(jEdit.getProperty(property + ".text"));
    checkbox.setSelected(jEdit.getBooleanProperty(property));
    return checkbox;
  }
}
