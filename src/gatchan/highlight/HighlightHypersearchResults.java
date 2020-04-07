/*
 * HighlightHypersearchResults.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2020 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

	@Override
	public void highlightUpdated(boolean highlightEnabled)
	{
		JTree tree = getHyperSearchTree();
		if (tree != null)
			tree.repaint();
	}

	private static class HighlightTreeCellRenderer extends DefaultTreeCellRenderer
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
			List<HighlightPosition> highlights = new LinkedList<>();
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
				appendString2html(sb, s.substring(i, hlPos.getPos()));
				if (hlPos.isStart())
				{
					sb.append("<font style bgcolor=\"#");
					Color c = hlPos.getHighlight().getColor();
					sb.append(Integer.toHexString(c.getRGB()).substring(2));
					sb.append("\">");
				}
				else
				{
					sb.append("</font>");
				}
				i = hlPos.getPos();
			}
			appendString2html(sb, s.substring(i));
			sb.append("</body></html>");
			return sb.toString();
		}

		private void addHighlight(Collection<HighlightPosition> highlights, String s, Highlight highlight)
		{
			SearchMatcher matcher = highlight.getSearchMatcher();
			try
			{
				Match m;
				int i = 0;
				while ((m = matcher.nextMatch(s.substring(i), true, true, true, false)) != null)
				{
					highlights.add(new HighlightPosition(i + m.start, highlight, true));
					highlights.add(new HighlightPosition(i + m.end, highlight, false));
					i += m.end;
				}
			}
			catch (InterruptedException ie) 
			{
			}
		}

		private static void appendString2html(StringBuilder sb, CharSequence s)
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
			private final int pos;
			private final Highlight highlight;
			private final boolean start;

			HighlightPosition(int p, Highlight h, boolean s)
			{
				pos = p;
				highlight = h;
				start = s;
			}

			public int getPos()
			{
				return pos;
			}

			public Highlight getHighlight()
			{
				return highlight;
			}

			public boolean isStart()
			{
				return start;
			}

			@Override
			public int compareTo(HighlightPosition hlPos)
			{
				return Integer.compare(pos, hlPos.pos);
			}
		}
	}
}
