package gatchan.highlight;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaPainter;

/**
 * The HighlightPlugin.
 * This is my first plugin for jEdit, some parts of my code were inspired
 * by the ErrorList plugin
 *
 * @author Matthieu Casanova
 */
public final class HighlightPlugin extends EBPlugin {

    public static final String NAME = "highlight";
    public static final String PROPERTY_PREFIX = "plugin.Highlight.";
    public static final String MENU = "highlight.menu";
    public static final String OPTION_PREFIX = "options.highlight.";

    /**
     * Initialize the plugin.
     * When starting this plugin will add an Highlighter on each text area
     */
    public void start() {
        View view = jEdit.getFirstView();
        while (view != null) {
            final EditPane[] panes = view.getEditPanes();
            for (int i = 0; i < panes.length; i++) {
                final JEditTextArea textArea = panes[i].getTextArea();
                HighlightPlugin.initTextArea(textArea);
            }
            view = view.getNext();
        }

    }


    /**
     * uninitialize the plugin.
     * we will remove the Highlighter on each text area
     */
    public void stop() {
        View view = jEdit.getFirstView();
        while (view != null) {
            final EditPane[] panes = view.getEditPanes();
            for (int i = 0; i < panes.length; i++) {
                final JEditTextArea textArea = panes[i].getTextArea();
                HighlightPlugin.uninitTextArea(textArea);
            }
            view = view.getNext();
        }
    }

    /**
     * Remove the highlighter from a text area.
     *
     * @param textArea the textarea from wich we will remove the highlighter
     * @see #stop()
     * @see #handleEditPaneMessage(org.gjt.sp.jedit.msg.EditPaneUpdate)
     */
    private static void uninitTextArea(final JEditTextArea textArea) {
        final TextAreaPainter painter = textArea.getPainter();
        final Highlighter highlighter = (Highlighter) painter.getClientProperty(Highlighter.class);
        if (highlighter != null) {
            painter.removeExtension(highlighter);
            textArea.putClientProperty(Highlighter.class, null);
        }
    }

    private static Highlighter initTextArea(final JEditTextArea textArea) {
        final Highlighter highlighter = new Highlighter(textArea);
        final TextAreaPainter painter = textArea.getPainter();
        painter.addExtension(highlighter);
        textArea.putClientProperty(Highlighter.class, highlighter);
        return highlighter;
    }


    public void handleMessage(final EBMessage message) {
        if (message instanceof EditPaneUpdate) {
            HighlightPlugin.handleEditPaneMessage((EditPaneUpdate) message);
        }
    }


    private static void handleEditPaneMessage(final EditPaneUpdate message) {
        final JEditTextArea textArea = message.getEditPane().getTextArea();
        final Object what = message.getWhat();

        if (what == EditPaneUpdate.CREATED) {
            HighlightPlugin.initTextArea(textArea);
        } else if (what == EditPaneUpdate.DESTROYED) {
            HighlightPlugin.uninitTextArea(textArea);
        }

    }

    /**
     * Highlight a word in a textarea.
     * If a text is selected this text will be highlighted, if no
     * text is selected we will ask the textarea to select a word
     *
     * @param textArea the textarea
     */
    public static void highlightThis(final JEditTextArea textArea) {
        String text = textArea.getSelectedText();
        if (text == null) {
            textArea.selectWord();
            text = textArea.getSelectedText();
        }

        final Highlighter highlighter = HighlightPlugin.getHighlighterForTextArea(textArea);
        highlighter.setStringToHighlight(text);
    }

    /**
     * Cancels the highlight on a textarea.
     *
     * @param textArea the textarea
     */
    public static void cancelHighlight(final JEditTextArea textArea) {
        final Highlighter highlighter = HighlightPlugin.getHighlighterForTextArea(textArea);
        highlighter.setStringToHighlight(null);
    }

    /**
     * Returns the Highlighter for a JEditTextArea.
     * if there is no highlighter an error log is set and one Highlighter is created.
     *
     * @param textArea the JEditTextArea
     * @return a Highlighter
     */
    private static Highlighter getHighlighterForTextArea(final JEditTextArea textArea) {
        final Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
        return highlighter;
    }
}
