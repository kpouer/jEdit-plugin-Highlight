package gatchan.highlight;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
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

  public HighlightDialog(View owner, Highlight highlight) {
    super(owner, "Highlight", false);
    this.highlight = highlight;

    getContentPane().add(panel);

    final MyActionListener myActionListener = new MyActionListener();
    ok.addActionListener(myActionListener);
    cancel.addActionListener(myActionListener);
    final JPanel buttonsPanel = new JPanel();

    final BoxLayout layout = new BoxLayout(buttonsPanel, BoxLayout.X_AXIS);
    buttonsPanel.setLayout(layout);
    buttonsPanel.add(Box.createGlue());
    buttonsPanel.add(ok);
    buttonsPanel.add(Box.createHorizontalStrut(6));
    buttonsPanel.add(cancel);
    buttonsPanel.add(Box.createGlue());
    getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    pack();
    GUIUtilities.centerOnScreen(this);
  }

  public HighlightDialog(View owner) {
    this(owner, new Highlight());
  }

  public void ok() {
    try {
      panel.save(highlight);
      HighlightManagerTableModel.getManager().addElement(highlight);
      dispose();
    } catch (InvalidHighlightException e) {
      JOptionPane.showMessageDialog(panel, e.getMessage(), "Invalid highlight", JOptionPane.ERROR_MESSAGE);
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
}
