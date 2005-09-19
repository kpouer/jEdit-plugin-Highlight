package gatchan.highlight;

import org.gjt.sp.jedit.buffer.JEditBuffer;
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
 * @version $Id$
*/
final class Highlighter extends TextAreaExtension implements HighlightChangeListener {
  private final JEditTextArea textArea;
  private final Segment seg;
  private final Point point;
  private final FontMetrics fm;

  private final HighlightManager highlightManager;
  private final AlphaComposite blend = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

  Highlighter(JEditTextArea textArea) {
    highlightManager = HighlightManagerTableModel.getManager();
    this.textArea = textArea;
    TextAreaPainter painter = textArea.getPainter();
    fm = painter.getFontMetrics();
    seg = new Segment();
    point = new Point();
  }

  public void paintValidLine(Graphics2D gfx,
                             int screenLine,
                             int physicalLine,
                             int start,
                             int end,
                             int y) {
    if (highlightManager.isHighlightEnable() &&
        highlightManager.countHighlights() != 0 ||
        HighlightManagerTableModel.currentWordHighlight.isEnabled()) {
      highlightList(gfx, physicalLine, start, end, y);
    }
  }


  private void highlightList(Graphics2D gfx, int physicalLine, int lineStartOffset, int lineEndOffset, int y) {
    String lineContent = textArea.getLineText(physicalLine);
    JEditBuffer buffer = textArea.getBuffer();
    for (int i = 0; i < highlightManager.countHighlights(); i++) {
      Highlight highlight = highlightManager.getHighlight(i);
      highlight(highlight, buffer, lineContent, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
    }
    highlight(HighlightManagerTableModel.currentWordHighlight, buffer, lineContent, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
  }

  private void highlight(Highlight highlight,
                         JEditBuffer buffer,
                         String lineContent,
                         int physicalLine,
                         int lineStartOffset,
                         int lineEndOffset,
                         Graphics2D gfx,
                         int y) {
    if (highlight.isEnabled() && (highlight.getScope() != Highlight.BUFFER_SCOPE ||
                                  highlight.getBuffer() == buffer)) {
      highlight(highlight, lineContent, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
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
      SearchMatcher searchMatcher = highlight.getSearchMatcher();
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
        String s = lineContent.substring(match.start, match.end);
        _highlight(highlight.getColor(), s, match.start + i, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
        highlight.updateLastSeen();
        if (match.end == lineContent.length()) {
          break;
        }
        lineContent = lineContent.substring(match.end);
        i += match.end;
        segment = new Segment(lineContent.toCharArray(), 0, lineContent.length());
      }
    } else {
      boolean found = highlightStringInLine(highlight.getColor(),
                                            lineContent,
                                            highlight.getStringToHighlight(),
                                            highlight.isIgnoreCase(),
                                            highlight.isHighlightSubsequence(),
                                            physicalLine,
                                            lineStartOffset,
                                            lineEndOffset,
                                            gfx,
                                            y);
      if (found) {
        highlight.updateLastSeen();
      }
    }
  }

  private boolean highlightStringInLine(Color highlightColor,
                                        String lineStringParam,
                                        String stringToHighlightParam,
                                        boolean ignoreCase,
                                        boolean highlightSubsequence,
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
    int start = lineString.indexOf(stringToHighlight, textArea.getLineStartOffset(physicalLine) - lineStartOffset);
    if (start == -1) return false;
    _highlight(highlightColor, stringToHighlight, start, physicalLine, lineStartOffset, lineEndOffset, gfx, y);
    while (true) {
      if (highlightSubsequence) {
        start = lineString.indexOf(stringToHighlight, start + 1);
      }   else {
        start = lineString.indexOf(stringToHighlight, start + stringToHighlight.length());
      }
      if (start == -1) return true;
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
    int lineStart = textArea.getLineStartOffset(physicalLine);
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

    if (start + lineStart >= lineEndOffset || end + lineStart <= lineStartOffset) {
      return;
    }

    int startX;

    if (start + lineStart >= lineStartOffset) {
      startX = textArea.offsetToXY(physicalLine, start, point).x;
    } else {
      startX = 0;
    }
    int endX;

    if (end + lineStart >= lineEndOffset) {
      endX = textArea.offsetToXY(physicalLine, lineEndOffset - lineStart - 1, point).x;
    } else {
      endX = textArea.offsetToXY(physicalLine, end, point).x;
    }
    Color oldColor = gfx.getColor();
    Composite oldComposite = gfx.getComposite();
    gfx.setColor(highlightColor);
    gfx.setComposite(blend);
    gfx.fillRect(startX, y, endX - startX, fm.getHeight());

    gfx.setColor(oldColor);
    gfx.setComposite(oldComposite);
  }

  public void highlightUpdated(boolean highlightEnable) {
    int firstLine = textArea.getFirstPhysicalLine();
    textArea.invalidateLineRange(firstLine, firstLine + textArea.getVisibleLines());
  }
}
