package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import javax.swing.*;
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
public class HighlightList extends JPanel {

  private JPopupMenu popupMenu;
  private JMenuItem remove;

  private JTable table;
  public static OneColumnTableModel tableModel;
  public HighlightList.RemoveAction removeAction;

  public HighlightList() {
    super(new BorderLayout());


    tableModel = getTableModel();
    table = new JTable(tableModel);
    final HighlightCellRenderer renderer = new HighlightCellRenderer();
    table.setRowHeight(renderer.getPreferredSize().height);
    table.setDefaultRenderer(Highlight.class, renderer);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultEditor(Highlight.class, new HighlightCellEditor());
    table.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row == -1) return;

        if (e.getButton() == MouseEvent.BUTTON1) {
          final View view = jEdit.getActiveView();
          JEditTextArea textArea = view.getTextArea();
          final Highlight highlight = (Highlight) table.getValueAt(table.getSelectedRow(), 0);
          HighlightPlugin.highlight(textArea, highlight);
          if (e.getClickCount() == 2) {
            /* HighlightDialog d = null;
             try {
               d = new HighlightDialog(view);
               d.init(highlight);
               d.setVisible(true);
             } catch (REException e1) {
               Log.log(Log.ERROR,this,e);
             } */
          }
        } else {
          showPopupMenu(e, row);
        }
      }
    });

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    JButton clear = new JButton(GUIUtilities.loadIcon("Clear.png"));
    clear.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tableModel.removeAll();
      }
    });
    toolBar.add(clear);
    add(toolBar, BorderLayout.NORTH);
    add(new JScrollPane(table));
  }

  private void showPopupMenu(MouseEvent e, int row) {
    if (popupMenu == null) {
      popupMenu = new JPopupMenu();
      removeAction = new RemoveAction();
      remove = popupMenu.add(removeAction);
    }
    remove.setEnabled(tableModel.getRowCount() > 0);
    removeAction.setRow(row);
    GUIUtilities.showPopupMenu(popupMenu, e.getComponent(), e.getX(), e.getY());
    e.consume();
  }

  public static OneColumnTableModel getTableModel() {
    if (tableModel == null) {
      tableModel = new OneColumnTableModel();
    }
    return tableModel;
  }

  public static void push(Highlight highlight) {
    final OneColumnTableModel tableModel = getTableModel();
    if (!tableModel.contains(highlight)) {
      tableModel.addElement(highlight);
    }
  }

  public void remove(Object highlight) {
    final int selectedRow = table.getSelectedRow();
    if (selectedRow != -1) {
      final Object selectedObject = table.getValueAt(selectedRow, 0);
      if (selectedObject == highlight) {
        final View view = jEdit.getActiveView();
        JEditTextArea textArea = view.getTextArea();
        HighlightPlugin.highlight(textArea, null);
      }
    }
    final OneColumnTableModel tableModel = getTableModel();
    tableModel.removeElement(highlight);

  }

  public class RemoveAction extends AbstractAction {

    private int row;

    public RemoveAction() {
      super("remove");
    }

    public void setRow(int row) {
      this.row = row;
    }

    public void actionPerformed(ActionEvent e) {
      Object s = table.getValueAt(row, 0);
      remove(s);
    }
  }
}
