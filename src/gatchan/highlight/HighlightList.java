package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.EventObject;

import gnu.regexp.REException;

/**
 * The dockable panel that will contains a list of all your highlights.
 *
 * @author Matthieu Casanova
 */
public class HighlightList extends JPanel {

  //private static DefaultListModel listModel;
  private JPopupMenu popupMenu;
  private JMenuItem remove;
  //private JList list;

  private JTable table;
  public static OneColumnTableModel tableModel;
  public HighlightList.RemoveAction removeAction;

  public HighlightList() {
    super(new BorderLayout());
    //  DefaultListModel listModel = getModel();


    tableModel = getTableModel();
    table = new JTable(tableModel);
    final HighlightCellRenderer renderer = new HighlightCellRenderer();
    table.setRowHeight(renderer.getPreferredSize().height);
    table.setDefaultRenderer(Highlight.class, renderer);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultEditor(Highlight.class,new HighlightCellEditor());
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
          //todo a popup menu
        }
      }
    });

    /* list = new JList(listModel);
     list.setCellRenderer(new HighlightCellRenderer2());
     list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

     list.addMouseListener(new MouseAdapter() {
       public void mouseClicked(MouseEvent e) {
         int index = list.locationToIndex(e.getPoint());
         if (index > -1 && index != list.getSelectedIndex()) {
           list.setSelectedIndex(index);
         }
         if (e.getButton() == MouseEvent.BUTTON1) {
           final View view = jEdit.getActiveView();
           JEditTextArea textArea = view.getTextArea();
           final Highlight highlight = (Highlight) list.getSelectedValue();
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
    /*   }
     } else {
       showPopupMenu(e);
       //todo a popup menu
     }
   }
 });
 list.addKeyListener(new KeyAdapter() {
   public void keyTyped(KeyEvent e) {
     e.consume();
     if (e.getKeyCode() == KeyEvent.VK_DELETE) {

       Object selected = list.getSelectedValue();
       if (selected != null) {
         remove(selected);
       }
     }
   }
 });    */
    //add(new JScrollPane(list));
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

  /* public static DefaultListModel getModel() {
     if (listModel == null) {
       listModel = new DefaultListModel();
     }
     return listModel;
   }  */

  public static OneColumnTableModel getTableModel() {
    if (tableModel == null) {
      tableModel = new OneColumnTableModel();
    }
    return tableModel;
  }

  public static void push(Highlight highlight) {
    //  DefaultListModel listModel = getModel();
    final OneColumnTableModel tableModel = getTableModel();
    if (!tableModel.contains(highlight)) {
      // listModel.addElement(highlight);
      tableModel.addElement(highlight);
    }
  }

  public void remove(Object highlight) {
    //  DefaultListModel listModel = getModel();
    final Object selectedObject = table.getValueAt(table.getSelectedRow(), 0);
    if (selectedObject == highlight) {
      final View view = jEdit.getActiveView();
      JEditTextArea textArea = view.getTextArea();
      HighlightPlugin.highlight(textArea, null);
    }
    final OneColumnTableModel tableModel = getTableModel();
    //  listModel.removeElement(highlight);
    tableModel.removeElement(highlight);

  }

  private class HighlightCellRenderer2 extends JPanel implements ListCellRenderer {
    private final JTextField expressionField = new JTextField();
    private final JCheckBox regexp = new JCheckBox("regexp");
    private final ColorWellButton colorBox = new ColorWellButton(Color.black);

    public HighlightCellRenderer2() {
      super(new GridBagLayout());
      GridBagConstraints cons = new GridBagConstraints();
      cons.gridy = 0;

      cons.anchor = GridBagConstraints.WEST;
      final JLabel exprLabel = new JLabel("exp1");
      add(exprLabel, cons);
      cons.fill = GridBagConstraints.HORIZONTAL;
      cons.weightx = 1;
      cons.gridwidth = 2;
      add(expressionField, cons);
      cons.weightx = 0;
      cons.fill = GridBagConstraints.NONE;
      cons.gridy = 1;
      cons.gridwidth = 2;
      add(regexp, cons);
      cons.gridwidth = 1;
      add(colorBox, cons);
      setBorder(new EtchedBorder());
    }


    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      Highlight highlight = (Highlight) value;
      Color background = (isSelected ? list.getSelectionBackground()
                          : list.getBackground());
      setBackground(background);
      regexp.setBackground(background);
      expressionField.setText(highlight.getStringToHighlight());
      regexp.setSelected(highlight.isRegexp());
      colorBox.setSelectedColor(highlight.getColor());
      return this;
    }
  }

  private class HighlightCellEditor extends AbstractCellEditor implements TableCellEditor {
    private Highlight highlight;
    public final HighlightCellRenderer renderer = new HighlightCellRenderer();

    public Object getCellEditorValue() {
      return highlight;
    }

    public boolean stopCellEditing() {
      return renderer.save(highlight);
    }

    public boolean isCellEditable(EventObject e) {
      if (e instanceof MouseEvent) {
        MouseEvent event = (MouseEvent) e;
        if (event.getClickCount() == 2) {
          return true;
        } else {
          return false;
        }
      }
      return true;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      renderer.setBorder(BorderFactory.createEtchedBorder());
      this.highlight = (Highlight) value;
      renderer.setHighlight(new Color(0xcc,0xcc,0xff), highlight);
      return renderer;
    }
  }

  private class HighlightCellRenderer extends JPanel implements TableCellRenderer {
    private final JTextField expressionField = new JTextField();
    private final JCheckBox regexp = new JCheckBox("regexp");
    private final ColorWellButton colorBox = new ColorWellButton(Color.black);

    public HighlightCellRenderer() {
      super(new GridBagLayout());
      GridBagConstraints cons = new GridBagConstraints();
      cons.gridy = 0;

      cons.anchor = GridBagConstraints.WEST;
      final JLabel exprLabel = new JLabel("exp1");
      add(exprLabel, cons);
      cons.fill = GridBagConstraints.HORIZONTAL;
      cons.weightx = 1;
      cons.gridwidth = 2;
      add(expressionField, cons);
      cons.weightx = 0;
      cons.fill = GridBagConstraints.NONE;
      cons.gridy = 1;
      cons.gridwidth = 2;
      add(regexp, cons);
      cons.gridwidth = 1;
      add(colorBox, cons);
      setBorder(new EtchedBorder());
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Highlight highlight = (Highlight) value;
      Color background = (isSelected ? table.getSelectionBackground()
                          : table.getBackground());
      setHighlight(background, highlight);
      return this;
    }

    private void setHighlight(Color background, Highlight highlight) {
      setBackground(background);
      regexp.setBackground(background);
      expressionField.setText(highlight.getStringToHighlight());
      regexp.setSelected(highlight.isRegexp());
      colorBox.setSelectedColor(highlight.getColor());
    }

    private boolean save(Highlight highlight) {
      try {
        highlight.init(expressionField.getText().trim(), regexp.isSelected(), colorBox.getSelectedColor());
        return true;
      } catch (REException e) {
        Log.log(Log.ERROR,this,"Unable to save the highlgiht");
        return false;
      }
    }
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

  private static class OneColumnTableModel extends AbstractTableModel {
    private java.util.List datas = new ArrayList();

    public int getRowCount() {
      return datas.size();
    }

    public int getColumnCount() {
      return 1;
    }

    public String getColumnName(int columnIndex) {
      return "Highlights";
    }

    public Class getColumnClass(int columnIndex) {
      return Highlight.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      return datas.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      datas.set(rowIndex, aValue);
      fireTableCellUpdated(rowIndex, 0);
    }

    public void addElement(Highlight highlight) {
      datas.add(highlight);
      fireTableRowsInserted(datas.size() - 1, datas.size() - 1);
    }

    public void removeElement(Object o) {
      for (int i = 0; i < datas.size(); i++) {
        Highlight highlight = (Highlight) datas.get(i);
        if (highlight.equals(o)) {
          datas.remove(o);
          fireTableRowsDeleted(i, i);
          break;
        }
      }
    }

    public boolean contains(Object o) {
      return datas.contains(o);
    }
  }
}
