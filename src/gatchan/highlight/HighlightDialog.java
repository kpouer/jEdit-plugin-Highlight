package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.gui.EnhancedDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** @author Matthieu Casanova */
public final class HighlightDialog extends EnhancedDialog {
  private final JButton ok = new JButton("ok");
  private final JButton cancel = new JButton("Cancel");

  private final Highlight highlight;
  private final HighlightTablePanel panel = new HighlightTablePanel();
  private final JComboBox scopeCombo = new JComboBox(new Integer[]{new Integer(Highlight.PERMANENT_SCOPE),
    new Integer(Highlight.SESSION_SCOPE), new Integer(Highlight.BUFFER_SCOPE)});
  private JSpinner spinner;

  public HighlightDialog(View owner, Highlight highlight) {
    super(owner, "Highlight", false);
    this.highlight = highlight;
    panel.setDialog(this);
    Container contentPane = getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

    JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    scopePanel.add(new JLabel("scope : "));
    scopeCombo.setRenderer(new MyListCellRenderer());
    scopePanel.add(scopeCombo);

    spinner = new JSpinner(new SpinnerNumberModel(0,0,Integer.MAX_VALUE,5));
    spinner.setToolTipText(jEdit.getProperty("gatchan.highlight.expire.tooltip"));

    MyActionListener myActionListener = new MyActionListener();
    ok.addActionListener(myActionListener);
    cancel.addActionListener(myActionListener);
    JPanel buttonsPanel = new JPanel();

    BoxLayout layout = new BoxLayout(buttonsPanel, BoxLayout.X_AXIS);
    buttonsPanel.setLayout(layout);
    buttonsPanel.add(Box.createGlue());
    buttonsPanel.add(ok);
    buttonsPanel.add(Box.createHorizontalStrut(6));
    buttonsPanel.add(cancel);
    buttonsPanel.add(Box.createGlue());

    contentPane.add(panel);
    contentPane.add(scopePanel);
    contentPane.add(spinner);
    contentPane.add(buttonsPanel);
    pack();
    GUIUtilities.centerOnScreen(this);
  }

  public HighlightDialog(View owner) {
    this(owner, new Highlight());
  }

  public void ok() {
    try {
      panel.save(highlight);
      long expire = ((Number) spinner.getValue()).longValue();
      if (expire != 0) {
        highlight.setDuration(expire*1000);
      }
      Integer selectedItem = (Integer) scopeCombo.getSelectedItem();
      int scope = selectedItem.intValue();
      highlight.setScope(scope);
      if (scope == Highlight.BUFFER_SCOPE) {
        Buffer buffer = jEdit.getActiveView().getBuffer();
        highlight.setBuffer(buffer);
      }
      HighlightManagerTableModel.getManager().addElement(highlight);
      dispose();
    } catch (InvalidHighlightException e) {
      GUIUtilities.error(jEdit.getActiveView(), "gatchan-highlight.errordialog.invalidHighlight", null);
      panel.focus();
    }
  }

  public void cancel() {
    dispose();
  }

  private final class MyActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == ok) {
        ok();
      } else if (e.getSource() == cancel) {
        cancel();
      }
    }
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      int scope = ((Integer) value).intValue();
      switch (scope) {
        case Highlight.PERMANENT_SCOPE :
          setText("permanent");
          break;
        case Highlight.SESSION_SCOPE :
          setText("session");
          break;
        case Highlight.BUFFER_SCOPE:
          setText("buffer");
          break;
      }
      return this;
    }
  }
}
