package gatchan.highlight;

import java.awt.Color;
import java.awt.Component;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.search.HyperSearchResult;
import org.gjt.sp.jedit.search.HyperSearchResults;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.SearchMatcher.Match;

public class HighlightHypersearchResults implements HighlightChangeListener
{
	public static final String HYPERSEARCH = "hypersearch-results";
	private final View view;

	public HighlightHypersearchResults(View view)
	{
		this.view = view;
	}

	private JTree getHyperSearchTree()
	{
		JComponent dockable = view.getDockableWindowManager().getDockable(HYPERSEARCH);
		if (dockable == null)
			return null;
		if (!(dockable instanceof HyperSearchResults))
			return null;
		HyperSearchResults hrd = (HyperSearchResults) dockable;
		return hrd.getTree();
	}

	public void start()
	{
		HighlightManagerTableModel.getManager().addHighlightChangeListener(this);
		JTree tree = getHyperSearchTree();
		if (tree == null)
			return;
		TreeCellRenderer renderer = tree.getCellRenderer();
		if (!(renderer instanceof HighlightTreeCellRenderer))
			tree.setCellRenderer(new HighlightTreeCellRenderer(renderer));
	}

	public void stop()
	{
		HighlightManagerTableModel.getManager().removeHighlightChangeListener(this);
		JTree tree = getHyperSearchTree();
		if (tree == null)
			return;
		TreeCellRenderer renderer = tree.getCellRenderer();
		if (renderer instanceof HighlightTreeCellRenderer)
			tree.setCellRenderer(((HighlightTreeCellRenderer) renderer).getOriginal());
	}

	public void highlightUpdated(boolean highlightEnabled)
	{
		JTree tree = getHyperSearchTree();
		if (tree != null)
			tree.repaint();
	}

	@SuppressWarnings("serial")
	class HighlightTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private final TreeCellRenderer renderer;

		HighlightTreeCellRenderer(TreeCellRenderer renderer)
		{
			this.renderer = renderer;
		}

		public TreeCellRenderer getOriginal()
		{
			return renderer;
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
							      boolean sel, boolean expanded, boolean leaf, int row,
							      boolean hasFocus)
		{
			Component defaultComponent = renderer.getTreeCellRendererComponent(tree,
				value, sel, expanded, leaf, row, hasFocus);
			if (!(value instanceof DefaultMutableTreeNode))
				return defaultComponent;
			if (!jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_HYPERSEARCH_RESULTS))
				return defaultComponent;
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			if (!(obj instanceof HyperSearchResult))
				return defaultComponent;
			HyperSearchResult result = (HyperSearchResult) obj;
			String s = getHighlightedString(result.toString());
			return renderer.getTreeCellRendererComponent(tree,
				new DefaultMutableTreeNode(s), sel, expanded, leaf, row, hasFocus);
		}

		private String getHighlightedString(String s)
		{
			HighlightManager manager = HighlightManagerTableModel.getManager();
			List<HighlightPosition> highlights = new LinkedList<HighlightPosition>();
			try
			{
				manager.getReadLock();
				int highlightCount = manager.countHighlights();
				for (int hi = 0; hi < highlightCount; hi++)
				{
					Highlight highlight = manager.getHighlight(hi);
					addHighlight(highlights, s, highlight);
				}
			}
			finally
			{
				manager.releaseLock();
			}
			if (manager.isHighlightWordAtCaret())
				addHighlight(highlights, s, HighlightManagerTableModel.currentWordHighlight);
			if (manager.isHighlightSelection())
				addHighlight(highlights, s, HighlightManagerTableModel.selectionHighlight);

			Collections.sort(highlights);
			StringBuilder sb = new StringBuilder("<html><body>");
			int i = 0;
			for (HighlightPosition hlPos : highlights)
			{
				appendString2html(sb, s.substring(i, hlPos.pos));
				if (hlPos.start)
				{
					sb.append("<font style bgcolor=\"#");
					Color c = hlPos.highlight.getColor();
					sb.append(Integer.toHexString(c.getRGB()).substring(2));
					sb.append("\">");
				}
				else
				{
					sb.append("</font>");
				}
				i = hlPos.pos;
			}
			appendString2html(sb, s.substring(i));
			sb.append("</body></html>");
			return sb.toString();
		}

		private void addHighlight(Collection<HighlightPosition> highlights, String s, Highlight highlight)
		{
			SearchMatcher matcher = highlight.getSearchMatcher();
			int i = 0;
			Match m;
			while ((m = matcher.nextMatch(s.substring(i), true, true, true, false)) != null)
			{
				highlights.add(new HighlightPosition(i + m.start, highlight, true));
				highlights.add(new HighlightPosition(i + m.end, highlight, false));
				i += m.end;
			}
		}

		private void appendString2html(StringBuilder sb, String s)
		{
			int length = s.length();
			for (int i = 0; i < length; i++)
			{
				char c = s.charAt(i);
				String r;
				switch (c)
				{
					case '"':
						r = "&quot;";
						break;
					/*case '\'':
						r = "&apos;";
						break;  */
					case '&':
						r = "&amp;";
						break;
					case '<':
						r = "&lt;";
						break;
					case '>':
						r = "&gt;";
						break;
					default:
						r = String.valueOf(c);
						break;
				}
				sb.append(r);
			}
		}

		private class HighlightPosition implements Comparable<HighlightPosition>
		{
			public int pos;
			public Highlight highlight;
			public boolean start;

			HighlightPosition(int p, Highlight h, boolean s)
			{
				pos = p;
				highlight = h;
				start = s;
			}

			public int compareTo(HighlightPosition hlPos)
			{
				if (pos < hlPos.pos)
					return (-1);
				if (pos > hlPos.pos)
					return 1;
				return 0;
			}
		}
	}
}
