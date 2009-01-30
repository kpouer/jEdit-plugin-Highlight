package gatchan.highlight;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.Vector;

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

public class HighlightHypersearchResults {

	public static final String HYPERSEARCH = "hypersearch-results";
	private View view;

	public HighlightHypersearchResults(View view) {
		this.view = view;
	}

	private JTree getHyperSearchTree() {
		JComponent dockable = view.getDockableWindowManager().getDockable(HYPERSEARCH);
		if (dockable == null)
			return null;
		if (! (dockable instanceof HyperSearchResults))
			return null;
		HyperSearchResults hrd = (HyperSearchResults) dockable;
		return hrd.getTree();
	}

	public void start() {
		JTree tree = getHyperSearchTree();
		if (tree == null)
			return;
		TreeCellRenderer renderer = tree.getCellRenderer();
		if (! (renderer instanceof HighlightTreeCellRenderer))
			tree.setCellRenderer(new HighlightTreeCellRenderer(renderer));
	}
	
	public void stop() {
		JTree tree = getHyperSearchTree();
		if (tree == null)
			return;
		TreeCellRenderer renderer = tree.getCellRenderer();
		if (renderer instanceof HighlightTreeCellRenderer)
			tree.setCellRenderer(((HighlightTreeCellRenderer)renderer).getOriginal());
	}
	
	@SuppressWarnings("serial")
	class HighlightTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private TreeCellRenderer renderer;
		
		public HighlightTreeCellRenderer(TreeCellRenderer renderer) {
			this.renderer = renderer;
		}
		public TreeCellRenderer getOriginal() {
			return renderer;
		}
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus)
		{
			Component defaultComponent = renderer.getTreeCellRendererComponent(tree,
				value, sel, expanded, leaf, row, hasFocus);
			if (! (value instanceof DefaultMutableTreeNode))
				return defaultComponent;
			if (! jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_HYPERSEARCH_RESULTS))
				return defaultComponent;
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			if (! (obj instanceof HyperSearchResult))
				return defaultComponent;
			HyperSearchResult result = (HyperSearchResult) obj;
			String s = getHighlightedString(result.toString());
			return renderer.getTreeCellRendererComponent(tree,
				new DefaultMutableTreeNode(s), sel, expanded, leaf, row, hasFocus);
		}
		private String getHighlightedString(String s)
		{
			class HighlightPosition implements Comparable<HighlightPosition> {
				public int pos;
				public Highlight highlight;
				public boolean start;
				public HighlightPosition(int p, Highlight h, boolean s)
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
			HighlightManager manager = HighlightManagerTableModel.getManager();
			Vector<HighlightPosition> highlights = new Vector<HighlightPosition>();
			try
			{
				manager.getReadLock();
				for (int hi = 0; hi < manager.countHighlights(); hi++)
				{
					Highlight highlight = manager.getHighlight(hi);
					SearchMatcher matcher = highlight.getSearchMatcher();
					int i = 0;
					Match m = null;
					while ((m = matcher.nextMatch(s.substring(i), true, true, true, false)) != null)
					{
						highlights.add(new HighlightPosition(i + m.start, highlight, true));
						highlights.add(new HighlightPosition(i + m.end, highlight, false));
						i += m.end;
					}
				}
			}
			finally
			{
				manager.releaseLock();
			}
			Collections.sort(highlights);
			StringBuffer sb = new StringBuffer("<html><body>");
			int i = 0;
			Vector<Highlight> stack = new Vector<Highlight>();
			for (HighlightPosition hlPos: highlights)
			{
				appendString2html(sb, s.substring(i, hlPos.pos));
				if (hlPos.start)
				{
					Color c = hlPos.highlight.getColor();
					sb.append("<font style bgcolor=\"#");
					appendColorSpec(sb, c);
					sb.append("\"/>");
					stack.add(hlPos.highlight);
				}
				else
				{
					sb.append("</font>");
					stack.remove(hlPos.highlight);
					if (stack.size() > 0)
					{
						// Intersecting highlights - need to end both, then put back
						// the effective one.
						Highlight h = stack.lastElement();
						Color c = h.getColor();
						sb.append("</font><font style bgcolor=\"#");
						appendColorSpec(sb, c);
						sb.append("\"/>");
					}
				}
				i = hlPos.pos;
			}
			appendString2html(sb, s.substring(i));
			sb.append("</body></html>");
			return sb.toString();
		}
		private void appendColorSpec(StringBuffer sb, Color c)
		{
			sb.append(Integer.toHexString(c.getRed()));
			if (c.getGreen() <= 0xf)
				sb.append("0");
			sb.append(Integer.toHexString(c.getGreen()));
			if (c.getBlue() <= 0xf)
				sb.append("0");
			sb.append(Integer.toHexString(c.getBlue()));			
		}
		private void appendString2html(StringBuffer sb, String s)
		{
			for (int i = 0; i < s.length(); i++)
			{
				char c = s.charAt(i);
				String r;
				switch (c) {
				case '"': r = "&quot;"; break;
				case '\'': r = "&apos;"; break;
				case '&': r = "&amp;"; break;
				case '<': r = "&lt;"; break;
				case '>': r = "&gt;"; break;
				default: r = String.valueOf(c); break;
				}
				sb.append(r);
			}
		}
	}
}
