package gatchan.highlight;

import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;

import gnu.regexp.REException;

/**
 * The dockable panel that will contains a list of all your highlights.
 *
 * @author Matthieu Casanova
 */
public class HighlightList extends JPanel {

  private static DefaultListModel listModel;
  private JPopupMenu popupMenu;
  private JMenuItem remove;
  private JList list;

  public HighlightList() {
    super(new BorderLayout());
    DefaultListModel listModel = getModel();
    list = new JList(listModel);
    list.setCellRenderer(new HighlightCellRenderer());
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
          }
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
    });
    add(new JScrollPane(list));
  }

  private void showPopupMenu(MouseEvent e) {
    if (popupMenu == null) {
      popupMenu = new JPopupMenu();
      remove = popupMenu.add(new RemoveAction());
    }

    remove.setEnabled(listModel.size() > 0);
    GUIUtilities.showPopupMenu(popupMenu, e.getComponent(),e.getX(), e.getY());
    e.consume();
  }

  public static DefaultListModel getModel() {
    if (listModel == null) {
      listModel = new DefaultListModel();
    }
    return listModel;
  }

  public static void push(Highlight highlight) {
    DefaultListModel listModel = getModel();
    if (!listModel.contains(highlight)) {
      listModel.addElement(highlight);
    }
  }

  public static void remove(Object highlight) {
    DefaultListModel listModel = getModel();
    listModel.removeElement(highlight);
  }

  private class HighlightCellRenderer extends JPanel implements ListCellRenderer {
    private final JTextField expressionField = new JTextField();
    private final JCheckBox regexp = new JCheckBox("regexp");

    public HighlightCellRenderer() {
      super(new GridBagLayout());
      GridBagConstraints cons = new GridBagConstraints();
      cons.gridy = 0;

      cons.anchor = GridBagConstraints.WEST;
      final JLabel exprLabel = new JLabel("exp1");
      add(exprLabel, cons);
      cons.fill = GridBagConstraints.HORIZONTAL;
      cons.weightx = 1;
      add(expressionField, cons);
      cons.weightx = 0;
      cons.fill = GridBagConstraints.NONE;
      cons.gridy = 1;
      cons.gridwidth = 2;
      add(regexp, cons);
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
      return this;
    }
  }

  public class RemoveAction extends AbstractAction {

    public RemoveAction() {
      super("remove");
    }

    public void actionPerformed(ActionEvent e) {
      Object s = list.getSelectedValue();
      if (s!= null) remove(s);
    }
  }
}
