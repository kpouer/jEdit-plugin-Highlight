package gatchan.highlight;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.EventObject;
import java.awt.event.MouseEvent;
import java.awt.*;

/**
 * The highlight cell editor used by the JTable containing Highlights.
 *
 * @author Matthieu Casanova
 */
public final class HighlightCellEditor extends AbstractCellEditor implements TableCellEditor {
  private Highlight highlight;
  private final HighlightTablePanel renderer = new HighlightTablePanel();
  private final Color background = new Color(0xcc, 0xcc, 0xff);

  public Object getCellEditorValue() {
    return highlight;
  }

  public boolean stopCellEditing() {
    return renderer.save(highlight);
  }

  public boolean isCellEditable(EventObject e) {
    if (e instanceof MouseEvent) {
      final MouseEvent mouseEvent = (MouseEvent) e;
      return mouseEvent.getClickCount() == 2;
    }
    return true;
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    renderer.setBorder(BorderFactory.createLoweredBevelBorder());
    highlight = (Highlight) value;
    renderer.setHighlight(background, highlight);
    return renderer;
  }
}
