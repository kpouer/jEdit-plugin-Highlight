package gatchan.highlight;

import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.util.CharIndexedSegment;

import javax.swing.text.Segment;
import java.awt.*;

/**
 * The Highlighter is the TextAreaExtension that will look for some String to highlightList in the textarea and draw a
 * rectangle in it's background.
 *
 * @author Matthieu Casanova
 */
final class Highlighter extends TextAreaExtension implements HighlightChangeListener {
  private final JEditTextArea textArea;
  private final Segment seg;
  private final Point point;
  private final FontMetrics fm;

  //private Highlight highlight;

  private final HighlightManager highlightManager;

  Highlighter(JEditTextArea textArea) {
    highlightManager = HighlightManagerTableModel.getManager();
    highlightManager.addHighlightChangeListener(this);
    this.textArea = textArea;
    final TextAreaPainter painter = textArea.getPainter();
    fm = painter.getFontMetrics();
    seg = new Segment();
    point = new Point();
  }

  /**
   * The Highlighter must be removed from the highlight listeners because they can
   * be destroyed when a JEditTextAera is destroyed.
   *
   * @throws Throwable
   */
  protected void finalize() throws Throwable {
    highlightManager.removeHighlightChangeListener(this);
  }

  public void paintValidLine(Graphics2D gfx,
                             int screenLine,
                             int physicalLine,
                             int start,
                             int end,
                             int y) {
    if (highlightManager.isHighlightEnable() && highlightManager.countHighlights() != 0) {
      highlightList(gfx, physicalLine, start, end, y);
    }
  }


  private void highlightList(Graphics2D gfx, int physicalLine, int lineStartOffset, int lineEndOffset, int y) {
    final String lineContent = textArea.getLineText(physicalLine);
    for (int i = 0; i < highlightManager.countHighlights(); i++) {
      final Highlight highlight = highlightManager.getHighlight(i);
      if (highlight.isEnabled()) {
        highlight(highlight, lineContent, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
      }
    }
  }

  private void highlight(Highlight highlight,
                         String lineContent,
                         int physicalLine,
                         int lineStartOffset,
                         int lineEndOffset,
                         Graphics2D gfx,
                         int y) {
    if (highlight.isRegexp()) {
      final SearchMatcher searchMatcher = highlight.getSearchMatcher();
      Segment segment = new Segment(lineContent.toCharArray(), 0, lineContent.length());
      SearchMatcher.Match match = null;
      int i = 0;
      while (true) {
        match = searchMatcher.nextMatch(new CharIndexedSegment(segment, false),
                                        physicalLine == 0,
                                        physicalLine == textArea.getLineCount(),
                                        match == null,
                                        false);
        if (match == null) {
          break;
        }
        final String s = lineContent.substring(match.start, match.end);
        _highlight(highlight.getColor(), s, match.start + i, physicalLine, lineStartOffset, lineEndOffset, gfx, y);

        if (match.end == lineContent.length()) {
          break;
        }
        lineContent = lineContent.substring(match.end);
        i += match.end;
        segment = new Segment(lineContent.toCharArray(), 0, lineContent.length());
      }
    } else {
      highlightStringInLine(highlight.getColor(),
                            lineContent,
                            highlight.getStringToHighlight(),
                            highlight.isIgnoreCase(),
                            physicalLine,
                            lineStartOffset,
                            lineEndOffset,
                            gfx,
                            y);
    }
  }

  private void highlightStringInLine(Color highlightColor,
                                     String lineStringParam,
                                     String stringToHighlightParam,
                                     boolean ignoreCase,
                                     int physicalLine,
                                     int lineStartOffset,
                                     int lineEndOffset,
                                     Graphics2D gfx,
                                     int y) {
    String stringToHighlight;
    String lineString;
    if (ignoreCase) {
      lineString = lineStringParam.toLowerCase();
      stringToHighlight = stringToHighlightParam.toLowerCase();
    } else {
      lineString = lineStringParam;
      stringToHighlight = stringToHighlightParam;
    }
    int start = lineString.indexOf(stringToHighlight);
    if (start == -1) return;
    _highlight(highlightColor, stringToHighlight, start, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
    while (true) {
      start = lineString.indexOf(stringToHighlight, start + 1);
      if (start == -1) return;
      _highlight(highlightColor, stringToHighlight, start, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
    }
  }

  private void _highlight(Color highlightColor,
                          String stringToHighlight,
                          int start,
                          int physicalLine,
                          int lineStartOffset,
                          int lineEndOffset,
                          Graphics2D gfx,
                          int y) {
    int end = start + stringToHighlight.length();

    if (start == 0 && end == 0) {
      textArea.getLineText(physicalLine, seg);
      for (int j = 0; j < seg.count; j++) {
        if (Character.isWhitespace(seg.array[seg.offset + j])) {
          start++;
        } else {
          break;
        }
      }

      end = seg.count;
    }

    if (start + lineStartOffset >= lineEndOffset || end + lineStartOffset <= lineStartOffset) {
      return;
    }

    final int startX;

    if (start + lineStartOffset >= lineStartOffset) {
      startX = textArea.offsetToXY(physicalLine, start, point).x;
    } else {
      startX = 0;
    }
    final int endX;

    if (end + lineStartOffset >= lineEndOffset) {
      endX = textArea.offsetToXY(physicalLine, lineEndOffset - lineStartOffset - 1, point).x;
    } else {
      endX = textArea.offsetToXY(physicalLine, end, point).x;
    }

    gfx.setColor(highlightColor);
    gfx.fillRect(startX, y, endX - startX, fm.getHeight());
  }

  public void highlightUpdated(boolean highlightEnable) {
    textArea.invalidateLineRange(0, textArea.getLineCount());
  }
}
