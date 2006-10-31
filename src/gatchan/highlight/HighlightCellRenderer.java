package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;

import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * The cell renderer that will render the Highlights in the JTable.
 *
 * @author Matthieu Casanova
 */
public class HighlightCellRenderer implements TableCellRenderer
{

	private final HighlightTablePanel highlightTablePanel = new HighlightTablePanel();
	private JCheckBox enabled = new JCheckBox();
	private JButton remove = new JButton(GUIUtilities.loadIcon("Clear.png"));
	private JButton hypersearch = new JButton(GUIUtilities.loadIcon("Find.png"));


	public Component getTableCellRendererComponent(JTable table,
						       Object value,
						       boolean isSelected,
						       boolean hasFocus,
						       int row,
						       int column)
	{
		Highlight highlight = (Highlight) value;
		if (column == 0)
		{
			enabled.setSelected(highlight.isEnabled());
			return enabled;
		}
		else if (column == 1)
		{
			highlightTablePanel.setHighlight(highlight);
			return highlightTablePanel;
		}
		else if (column == 2)
		{
			return remove;
		}
		else
		{
			return hypersearch;
		}
	}

	public Dimension getPreferredSize()
	{
		return highlightTablePanel.getPreferredSize();
	}
}
