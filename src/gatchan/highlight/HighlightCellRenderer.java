package gatchan.highlight;

import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * The cell renderer that will render the Highlights in the JTable.
 *
 * @author Matthieu Casanova
 */
public final class HighlightCellRenderer implements TableCellRenderer {

    private final HighlightTablePanel highlightTablePanel = new HighlightTablePanel();

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      final Highlight highlight = (Highlight) value;
      final Color background = isSelected ? table.getSelectionBackground() : table.getBackground();
      highlightTablePanel.setHighlight(background, highlight);
      return highlightTablePanel;
    }

    public Dimension getPreferredSize() {
      return highlightTablePanel.getPreferredSize();
    }
  }
