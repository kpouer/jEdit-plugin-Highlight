package gatchan.highlight;

import gnu.regexp.REException;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.EnhancedDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/** @author Matthieu Casanova */
public class HighlightDialog extends EnhancedDialog {

  private final JButton ok;
  private final JButton cancel;

  private Highlight highlight;
  private HighlightTablePanel panel;

  public HighlightDialog(View owner, Highlight highlight) {
    super(owner, "Highlight", false);
    this.highlight = highlight;

    panel = new HighlightTablePanel();
    getContentPane().add(panel);

    final ActionListener actionListener = new ActionListener();
    ok = new JButton("ok");
    cancel = new JButton("Cancel");
    ok.addActionListener(actionListener);
    cancel.addActionListener(actionListener);
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

  public HighlightDialog(View owner) throws REException {
    this(owner,new Highlight());
  }

  public void init(Highlight highlight) {
    panel.setHighlight(highlight);
  }

  public void ok() {
    if (panel.save(highlight)) {
      HighlightManagerTableModel.getManager().addElement(highlight);
      dispose();
    } else {
      panel.focus();
    }
  }

  public void cancel() {
    dispose();
  }

  private class ActionListener implements java.awt.event.ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == ok) {
        ok();
      } else if (e.getSource() == cancel) {
        cancel();
      }
    }
  }
}
