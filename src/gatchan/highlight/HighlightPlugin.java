package gatchan.highlight;

import gnu.regexp.REException;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.util.Log;

/**
 * The HighlightPlugin. This is my first plugin for jEdit, some parts of my code were inspired by the ErrorList plugin
 *
 * @author Matthieu Casanova
 */
public final class HighlightPlugin extends EBPlugin {

  private static HighlightManager highlightManager;

  public static final String NAME = "highlight";
  public static final String PROPERTY_PREFIX = "plugin.Highlight.";
  public static final String MENU = "highlight.menu";
  public static final String OPTION_PREFIX = "options.highlight.";

  /** Initialize the plugin. When starting this plugin will add an Highlighter on each text area */
  public void start() {
    highlightManager = HighlightManagerTableModel.getManager();
    View view = jEdit.getFirstView();
    while (view != null) {
      final EditPane[] panes = view.getEditPanes();
      for (int i = 0; i < panes.length; i++) {
        final JEditTextArea textArea = panes[i].getTextArea();
        initTextArea(textArea);
      }
      view = view.getNext();
    }
  }


  /** uninitialize the plugin. we will remove the Highlighter on each text area */
  public void stop() {
    highlightManager.dispose();
    highlightManager = null;
    View view = jEdit.getFirstView();
    while (view != null) {
      final EditPane[] panes = view.getEditPanes();
      for (int i = 0; i < panes.length; i++) {
        final JEditTextArea textArea = panes[i].getTextArea();
        uninitTextArea(textArea);
      }
      view = view.getNext();
    }
  }

  /**
   * Remove the highlighter from a text area.
   *
   * @param textArea the textarea from wich we will remove the highlighter
   *
   * @see #stop()
   * @see #handleEditPaneMessage(org.gjt.sp.jedit.msg.EditPaneUpdate)
   */
  private static void uninitTextArea(JEditTextArea textArea) {
    final TextAreaPainter painter = textArea.getPainter();
    final Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
    if (highlighter != null) {
      painter.removeExtension(highlighter);
      textArea.putClientProperty(Highlighter.class, null);
    }
  }

  private static Highlighter initTextArea(JEditTextArea textArea) {
    final Highlighter highlighter = new Highlighter(textArea);
    final TextAreaPainter painter = textArea.getPainter();
    painter.addExtension(TextAreaPainter.BELOW_SELECTION_LAYER,highlighter);
    textArea.putClientProperty(Highlighter.class, highlighter);
    return highlighter;
  }


  public void handleMessage(EBMessage message) {
    if (message instanceof EditPaneUpdate) {
      handleEditPaneMessage((EditPaneUpdate) message);
    }
  }


  private static void handleEditPaneMessage(EditPaneUpdate message) {
    final JEditTextArea textArea = message.getEditPane().getTextArea();
    final Object what = message.getWhat();

    if (what == EditPaneUpdate.CREATED) {
      initTextArea(textArea);
    } else if (what == EditPaneUpdate.DESTROYED) {
      uninitTextArea(textArea);
    }
  }

  /**
   * Highlight a word in a textarea. If a text is selected this text will be highlighted, if no text is selected we will
   * ask the textarea to select a word
   *
   * @param textArea the textarea
   */
  public static void highlightThis(JEditTextArea textArea) {
    String text = textArea.getSelectedText();
    if (text == null) {
      textArea.selectWord();
      text = textArea.getSelectedText();
    }

    try {
      highlightManager.addElement(new Highlight(text));
    } catch (REException e) {
      Log.log(Log.MESSAGE, HighlightPlugin.class, "This should never happens here " + e.getMessage());
    }
  }

  public static void highlightDialog(View view) {
    try {
      final HighlightDialog d = new HighlightDialog(view);
      d.setVisible(true);
    } catch (REException e) {
      Log.log(Log.ERROR,HighlightPlugin.class,e);
    }
  }

  public static void removeAllHighlights() {
    highlightManager.removeAll();
  }

  public static void enableHighlights() {
    highlightManager.setHighlightEnable(true);
  }

  public static void disableHighlights() {
    highlightManager.setHighlightEnable(false);
  }

  public static void toggleHighlights() {
    highlightManager.setHighlightEnable(!highlightManager.isHighlightEnable());
  }

  public static boolean isHighlightEnable() {
    return highlightManager.isHighlightEnable();
  }
}
