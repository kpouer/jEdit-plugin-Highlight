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

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import gatchan.highlight.hypersearch.HyperSearchResultValue;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.search.HyperSearchResult;
import org.gjt.sp.jedit.search.HyperSearchResults;

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
			if (value instanceof DefaultMutableTreeNode)
			{
				if (jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_HYPERSEARCH_RESULTS))
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
					Object obj = node.getUserObject();

					if (obj instanceof HyperSearchResult)
					{
						HyperSearchResult result = (HyperSearchResult) obj;
						return renderer.getTreeCellRendererComponent(tree,
							new DefaultMutableTreeNode(new HyperSearchResultValue(result.toString())), sel, expanded, leaf, row, hasFocus);
					}
				}
			}
			return defaultComponent;
		}
	}
}
