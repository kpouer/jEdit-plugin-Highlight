package gatchan.highlight;

import org.gjt.sp.jedit.*;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The dockable panel that will contains a list of all your highlights.
 *
 * @author Matthieu Casanova
 */
public final class HighlightList extends JPanel implements HighlightChangeListener {

  private JPopupMenu popupMenu;
  private JMenuItem remove;

  private final JTable table;
  private final HighlightManagerTableModel tableModel;
  private HighlightList.RemoveAction removeAction;
  private final JCheckBox enableHighlights = new JCheckBox("enable");

  public HighlightList() {
    super(new BorderLayout());


    tableModel = HighlightManagerTableModel.getInstance();
    table = new JTable(tableModel);
    table.setDragEnabled(false);
    final HighlightCellRenderer renderer = new HighlightCellRenderer();
    table.setRowHeight(renderer.getPreferredSize().height);

    table.setDefaultRenderer(Highlight.class, renderer);

    final TableColumnModel columnModel = table.getColumnModel();

    columnModel.getColumn(2).setCellEditor(new ButtonCellEditor(tableModel));

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setShowGrid(false);
    table.setIntercellSpacing(new Dimension(0, 0));
    final TableColumn col1 = columnModel.getColumn(0);
    col1.setPreferredWidth(26);
    col1.setMinWidth(26);
    col1.setMaxWidth(26);
    col1.setResizable(false);

    final TableColumn col3 = columnModel.getColumn(2);
    col3.setPreferredWidth(26);
    col3.setMinWidth(26);
    col3.setMaxWidth(26);
    col3.setResizable(false);

    table.setDefaultEditor(Highlight.class, new HighlightCellEditor());
    table.setDefaultEditor(Boolean.class, table.getDefaultEditor(Boolean.class));
    table.setTableHeader(null);

    table.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        final int row = table.rowAtPoint(e.getPoint());
        if (row == -1) return;
        if (GUIUtilities.isRightButton(e.getModifiers())) {
          showPopupMenu(e, row);
        }
      }
    });

    final JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    final JButton newButton = new JButton(GUIUtilities.loadIcon("New.png"));
    newButton.setToolTipText("Add an highlight");
    final JButton clear = new JButton(GUIUtilities.loadIcon("Clear.png"));
    clear.setToolTipText("Remove all highlights");
    enableHighlights.setSelected(true);
    enableHighlights.setToolTipText("Enable / disable highlights");
    final MyActionListener actionListener = new MyActionListener(tableModel, newButton, clear, enableHighlights);
    newButton.addActionListener(actionListener);
    clear.addActionListener(actionListener);
    enableHighlights.addActionListener(actionListener);
    toolBar.add(newButton);
    toolBar.add(clear);
    toolBar.add(enableHighlights);

    add(toolBar, BorderLayout.NORTH);
    final JScrollPane scroll = new JScrollPane(table);
    add(scroll);
  }

  /**
   * Show the popup menu of the highlight panel.
   *
   * @param e   the mouse event
   * @param row the selected row
   */
  private void showPopupMenu(MouseEvent e, int row) {
    if (popupMenu == null) {
      popupMenu = new JPopupMenu();
      removeAction = new RemoveAction(tableModel);
      remove = popupMenu.add(removeAction);
    }
    remove.setEnabled(tableModel.getRowCount() > 0);
    removeAction.setRow(row);
    GUIUtilities.showPopupMenu(popupMenu, e.getComponent(), e.getX(), e.getY());
    e.consume();
  }

  public void addNotify() {
    super.addNotify();
    HighlightManagerTableModel.getManager().addHighlightChangeListener(this);
  }

  public void removeNotify() {
    super.removeNotify();
    HighlightManagerTableModel.getManager().removeHighlightChangeListener(this);
  }

  public void highlightUpdated(boolean highlightEnable) {
    enableHighlights.setSelected(highlightEnable);
  }

  /**
   * The remove action that will remove an highlight from the table.
   *
   * @author Matthieu Casanova
   */
  public static final class RemoveAction extends AbstractAction {

    private int row;

    private final HighlightManagerTableModel tableModel;

    private RemoveAction(HighlightManagerTableModel tableModel) {
      super("remove");
      this.tableModel = tableModel;
    }

    private final void setRow(int row) {
      this.row = row;
    }

    public final void actionPerformed(ActionEvent e) {
      tableModel.removeRow(row);
    }
  }

  /**
   * The actionListener that will handle buttons and checkbox of the HighlightList.
   *
   * @author Matthieu Casanova
   */
  private static final class MyActionListener implements ActionListener {
    private final JButton newButton;
    private final JButton clear;
    private final JCheckBox enableHighlights;

    private final HighlightManagerTableModel tableModel;

    private MyActionListener(HighlightManagerTableModel tableModel,
                             JButton newButton,
                             JButton clear,
                             JCheckBox enableHighlights) {
      this.tableModel = tableModel;
      this.newButton = newButton;
      this.clear = clear;
      this.enableHighlights = enableHighlights;
    }

    public final void actionPerformed(ActionEvent e) {
      final Object source = e.getSource();
      if (clear.equals(source)) {
        tableModel.removeAll();
      } else if (newButton.equals(source)) {
        HighlightPlugin.highlightDialog(jEdit.getActiveView());
      } else if (enableHighlights.equals(source)) {
        tableModel.setHighlightEnable(enableHighlights.isSelected());
      }
    }
  }
}
