package gatchan.highlight;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * The highlight cell editor used by the JTable containing Highlights.
 *
 * @author Matthieu Casanova
 */
public final class HighlightCellEditor extends AbstractCellEditor implements TableCellEditor {
  private Highlight highlight;
  private final HighlightTablePanel renderer = new HighlightTablePanel();

  public Object getCellEditorValue() {
    return highlight;
  }

  public boolean stopCellEditing() {
    final boolean ret = renderer.save(highlight);
    if (ret) {
      HighlightManagerTableModel.getInstance().fireHighlightChangeListener();
    }
    return ret;
  }

  public boolean isCellEditable(EventObject e) {
    if (e instanceof MouseEvent) {
      final MouseEvent mouseEvent = (MouseEvent) e;
      return mouseEvent.getClickCount() == 2;
    }
    return true;
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    highlight = (Highlight) value;
    renderer.setBorder(BorderFactory.createLoweredBevelBorder());
    renderer.setHighlight(highlight);
    return renderer;
  }
}
