package gatchan.highlight;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.util.SegmentCharSequence;

import javax.swing.text.Segment;
import java.awt.*;
import java.util.regex.PatternSyntaxException;

/**
 * The Highlighter is the TextAreaExtension that will look for some String to highlightList in the textarea and draw a
 * rectangle in it's background.
 *
 * @author Matthieu Casanova
 * @version $Id: Highlighter.java,v 1.19 2006/07/24 09:26:52 kpouer Exp $
 */
class Highlighter extends TextAreaExtension implements HighlightChangeListener
{
    private final JEditTextArea textArea;
    private final Segment tempLineContent = new Segment();
    private final Segment lineContent = new Segment();
    private final Point point = new Point();
    private final FontMetrics fm;

    private final HighlightManager highlightManager;
    private final AlphaComposite blend = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    Highlighter(JEditTextArea textArea)
    {
        highlightManager = HighlightManagerTableModel.getManager();
        this.textArea = textArea;
        TextAreaPainter painter = textArea.getPainter();
        fm = painter.getFontMetrics();
    }

    public void paintValidLine(Graphics2D gfx,
                               int screenLine,
                               int physicalLine,
                               int start,
                               int end,
                               int y)
    {
        if (highlightManager.isHighlightEnable() &&
            highlightManager.countHighlights() != 0 ||
            HighlightManagerTableModel.currentWordHighlight.isEnabled())
        {
            highlightList(gfx, physicalLine, y);
        }
    }


    private void highlightList(Graphics2D gfx, int physicalLine, int y)
    {
        textArea.getLineText(physicalLine, lineContent);
        if (lineContent.count == 0)
            return;

        JEditBuffer buffer = textArea.getBuffer();
        tempLineContent.array = lineContent.array;
        tempLineContent.offset = lineContent.offset;
        tempLineContent.count = lineContent.count;
        for (int i = 0; i < highlightManager.countHighlights(); i++)
        {
            Highlight highlight = highlightManager.getHighlight(i);
            highlight(highlight, buffer, physicalLine, gfx, y);
            tempLineContent.offset = lineContent.offset;
            tempLineContent.count = lineContent.count;
        }
        highlight(HighlightManagerTableModel.currentWordHighlight, buffer, physicalLine, gfx, y);
    }

    private void highlight(Highlight highlight,
                           JEditBuffer buffer,
                           int physicalLine,
                           Graphics2D gfx,
                           int y)
    {
        if (highlight.isEnabled() &&
            highlight.isValid() &&
            (highlight.getScope() != Highlight.BUFFER_SCOPE ||
              highlight.getBuffer() == buffer))
        {
            highlight(highlight, physicalLine, gfx, y);
        }
    }

    private void highlight(Highlight highlight,
                           int physicalLine,
                           Graphics2D gfx,
                           int y)
    {
        SearchMatcher searchMatcher = highlight.getSearchMatcher();
        SearchMatcher.Match match = null;
        boolean isFirstLine = physicalLine == 0;
        boolean isLastLine = physicalLine == textArea.getLineCount();
		boolean subsequence = highlight.isHighlightSubsequence();
		try
        {
            int i = 0;
            while (true)
            {
                match = searchMatcher.nextMatch(new SegmentCharSequence(tempLineContent, false),
                                                isFirstLine,
                                                isLastLine,
                                                match == null,
                                                false);
                if (match == null || match.end == match.start)
                    break;
                _highlight(highlight.getColor(), match.start + i, match.end + i, physicalLine, gfx, y);
                highlight.updateLastSeen();
				if (subsequence)
				{
					tempLineContent.count -= match.start + 1;
					tempLineContent.offset += match.start + 1;
					i += match.start + 1;
				}
				else
				{
					tempLineContent.count -= match.end;

					tempLineContent.offset += match.end;
					i += match.end;
				}

				if (tempLineContent.count <= 0)
					break;
            }
        }
        catch (PatternSyntaxException e)
        {
            // the regexp was invalid
            highlight.setValid(false);
        }
    }

    private void _highlight(Color highlightColor,
                            int startOffset,
                            int endOffset,
                            int physicalLine,
                            Graphics2D gfx,
                            int y)
    {
        Point p = textArea.offsetToXY(physicalLine, startOffset, point);
        if (p == null)
        {
            // The start offset was not visible
            return;
        }
        int startX = p.x;

        p = textArea.offsetToXY(physicalLine, endOffset, point);
        if (p == null)
        {
            // The end offset was not visible
            return;
        }
        int endX = p.x;
        Color oldColor = gfx.getColor();
        Composite oldComposite = gfx.getComposite();
        gfx.setColor(highlightColor);
        gfx.setComposite(blend);
        gfx.fillRect(startX, y, endX - startX, fm.getHeight());

        gfx.setColor(oldColor);
        gfx.setComposite(oldComposite);
    }

    public void highlightUpdated(boolean highlightEnabled)
    {
        int firstLine = textArea.getFirstPhysicalLine();
        textArea.invalidateLineRange(firstLine, firstLine + textArea.getVisibleLines());
    }
}
