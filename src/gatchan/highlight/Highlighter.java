package gatchan.highlight;

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.TextAreaPainter;

import javax.swing.text.Segment;
import java.awt.*;

/**
 * The Highlighter is the TextAreaExtension that will look for some String to
 * highlight in the textarea and draw a rectangle in it's background.
 *
 * @author Matthieu Casanova
 */
final class Highlighter extends TextAreaExtension {
    private final JEditTextArea textArea;
    private final Segment seg;
    private final Point point;
    private final FontMetrics fm;
    private String stringToHighlight;
    private final Color highlightColor = new Color(153, 255, 204);

    public Highlighter(final JEditTextArea textArea) {
        this.textArea = textArea;
        final TextAreaPainter painter = textArea.getPainter();
        fm = painter.getFontMetrics();
        seg = new Segment();
        point = new Point();
    }

    /**
     * Set a string to highlight.
     * If the String is different from the previous string, it will be
     * updated and all lines in the textArea will be invalidated
     *
     * @param stringToHighlight the new string to highlight or null if we want to remove highlight
     */
    public void setStringToHighlight(final String stringToHighlight) {
        if (this.stringToHighlight != stringToHighlight) {
            this.stringToHighlight = stringToHighlight;
            textArea.invalidateLineRange(0, textArea.getLineCount());
        }
    }

    public void paintValidLine(final Graphics2D gfx, final int screenLine, final int physicalLine, final int start, final int end, final int y) {
        if (stringToHighlight != null) {
            highlight(gfx, physicalLine, start, end, y);
        }


    }

    private void highlight(final Graphics2D gfx, final int line, final int _start, final int _end, final int y) {
        final int lineStart = textArea.getLineStartOffset(line);
        final String text = textArea.getLineText(line);
        int start = text.indexOf(stringToHighlight);
        if (start == -1) return;
        _highlight(stringToHighlight, start, line, lineStart, _end, _start, gfx, y);
        while (true) {
            start = text.indexOf(stringToHighlight, start + 1);
            if (start == -1) return;
            _highlight(stringToHighlight, start, line, lineStart, _end, _start, gfx, y);
        }
    }

    private void _highlight(final String stringToHighlight, int start, final int line, final int lineStart, final int _end, final int _start, final Graphics2D gfx, final int y) {
        int end = start + stringToHighlight.length();

        if (start == 0 && end == 0) {
            textArea.getLineText(line, seg);
            for (int j = 0; j < seg.count; j++) {
                if (Character.isWhitespace(seg.array[seg.offset + j])) {
                    start++;
                } else {
                    break;
                }
            }

            end = seg.count;
        }

        if (start + lineStart >= _end || end + lineStart <= _start) {
            return;
        }

      //  Gutter gutter = textArea.getGutter();
        // gutter.setBorder(20,Color.red,Color.green,Color.blue);


        final int startX;

        if (start + lineStart >= _start) {
            startX = textArea.offsetToXY(line, start, point).x;
        } else {
            startX = 0;
        }
        final int endX;

        if (end + lineStart >= _end) {
            endX = textArea.offsetToXY(line, _end - lineStart - 1, point).x;
        } else {
            endX = textArea.offsetToXY(line, end, point).x;
        }

        gfx.setColor(highlightColor);
        gfx.fillRect(startX, y, endX - startX, fm.getHeight());
    }
}
