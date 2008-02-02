package gatchan.highlight;

//{{{ imports
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.util.SegmentCharSequence;

import javax.swing.text.Segment;
import java.awt.*;
import java.util.regex.PatternSyntaxException;
//}}}

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
	private FontMetrics fm;

	private final HighlightManager highlightManager;
	private AlphaComposite blend;

	Highlighter(JEditTextArea textArea)
	{
        blend = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 
                                           ((float)jEdit.getIntegerProperty(HighlightOptionPane.PROP_ALPHA, 50)) / 100f);
		highlightManager = HighlightManagerTableModel.getManager();
		this.textArea = textArea;
	}

	public void paintScreenLineRange(Graphics2D gfx, int firstLine, int lastLine, int[] physicalLines, int[] start, int[] end, int y, int lineHeight)
	{
		fm = textArea.getPainter().getFontMetrics();
		if (highlightManager.isHighlightEnable() &&
		    highlightManager.countHighlights() != 0 ||
		    HighlightManagerTableModel.currentWordHighlight.isEnabled())
			super.paintScreenLineRange(gfx, firstLine, lastLine, physicalLines, start, end, y, lineHeight);
	}

	/**
	 * Called by the text area when the extension is to paint a
	 * screen line which has an associated physical line number in
	 * the buffer. Note that since one physical line may consist of
	 * several screen lines due to soft wrap, the start and end
	 * offsets of the screen line are passed in as well.
	 *
	 * @param gfx The graphics context
	 * @param screenLine The screen line number
	 * @param physicalLine The physical line number
	 * @param start The offset where the screen line begins, from
	 * the start of the buffer
	 * @param end The offset where the screen line ends, from the
	 * start of the buffer
	 * @param y The y co-ordinate of the top of the line's
	 * bounding box
	 * @since jEdit 4.0pre4
	 */
	public void paintValidLine(Graphics2D gfx,
				   int screenLine,
				   int physicalLine,
				   int start,
				   int end,
				   int y)
	{
		textArea.getLineText(physicalLine, lineContent);
		if (lineContent.count == 0)
			return;

		int startOffset = textArea.getLineStartOffset(physicalLine);
		int endOffset = textArea.getLineEndOffset(physicalLine);
		int screenToPhysicalOffset = start - startOffset;
		lineContent.offset += screenToPhysicalOffset;
		lineContent.count -= screenToPhysicalOffset;
		lineContent.count -= endOffset - end;

		JEditBuffer buffer = textArea.getBuffer();
		tempLineContent.array = lineContent.array;
		tempLineContent.offset = lineContent.offset;
		tempLineContent.count = lineContent.count;
		try
		{
			highlightManager.getReadLock();
			for (int i = 0; i < highlightManager.countHighlights(); i++)
			{
				Highlight highlight = highlightManager.getHighlight(i);
				highlight(highlight, buffer, gfx, screenLine, physicalLine, y, screenToPhysicalOffset);
				tempLineContent.offset = lineContent.offset;
				tempLineContent.count = lineContent.count;
			}
		}
		finally
		{
			highlightManager.releaseLock();
		}
		tempLineContent.offset = lineContent.offset;
		tempLineContent.count = lineContent.count;

		highlight(HighlightManagerTableModel.currentWordHighlight, buffer, gfx, screenLine, physicalLine, y, screenToPhysicalOffset);
	}

	private void highlight(Highlight highlight,
			       JEditBuffer buffer,
			       Graphics2D gfx,
			       int screenLine,
			       int physicalLine,
			       int y,
			       int screenToPhysicalOffset)
	{
		if (!highlight.isEnabled() ||
		    !highlight.isValid() ||
		    (highlight.getScope() == Highlight.BUFFER_SCOPE &&
		     highlight.getBuffer() != buffer))
		{
			return;
		}

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
				_highlight(highlight.getColor(), gfx, physicalLine, match.start + i + screenToPhysicalOffset, match.end + i + screenToPhysicalOffset, y);
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
				Graphics2D gfx,
				int physicalLine,
				int startOffset,
				int endOffset,
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
