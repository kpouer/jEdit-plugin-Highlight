package gatchan.highlight;

import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import javax.swing.*;
import javax.swing.text.Keymap;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import gnu.regexp.REException;

/** @author Matthieu Casanova */
public class HighlightDialog extends EnhancedDialog {

  private JEditTextArea textArea;
  private final JTextField field;
  private final JCheckBox regex;
  private final JButton ok;
  private final JButton cancel;

  private Highlight highlight;
  private final JTextField colorField = new JTextField(6);

  public HighlightDialog(View owner, Highlight highlight) {
    super(owner, "Highlight", false);
    this.highlight = highlight;
    textArea = owner.getTextArea();
    getContentPane().setLayout(new GridBagLayout());
    field = new JTextField(40);
    regex = new JCheckBox("regex");
    ActionListener actionListener = new ActionListener();
    ok = new JButton("ok");
    cancel = new JButton("Cancel");
    ok.addActionListener(actionListener);
    cancel.addActionListener(actionListener);
    GridBagConstraints cons = new GridBagConstraints();
    cons.gridy = 0;
    cons.gridheight = 1;
    cons.insets = new Insets(1, 1, 1, 1);
    getContentPane().add(new JLabel("Highlight : "), cons);
    getContentPane().add(field, cons);
    cons.gridy = 1;
    getContentPane().add(regex, cons);
    getContentPane().add(colorField, cons);
    JPanel buttonsPanel = new JPanel();
    BoxLayout layout = new BoxLayout(buttonsPanel, BoxLayout.X_AXIS);
    buttonsPanel.setLayout(layout);
    buttonsPanel.add(ok);
    buttonsPanel.add(Box.createHorizontalStrut(6));
    buttonsPanel.add(cancel);
    cons.gridy = 2;
    cons.gridwidth = 2;
    getContentPane().add(buttonsPanel, cons);
    pack();
    GUIUtilities.centerOnScreen(this);
  }

  public HighlightDialog(View owner) throws REException {
    this(owner,new Highlight());
  }

  public void init(Highlight highlight) {
    field.setText(highlight.getStringToHighlight());
    regex.setSelected(highlight.isRegexp());
  }

  public void ok() {
    try {
      //todo add a color

      Color color;
      try {
        color = new Color(Integer.parseInt(colorField.getText(), 16));
      } catch (NumberFormatException e) {
        color = Highlight.DEFAULT_COLOR;
      }
      highlight.init(field.getText().trim(),regex.isSelected(),color);
      HighlightPlugin.highlight(textArea, highlight);
      dispose();
    } catch (REException e) {
      final String message = "Invalid regexp " + e.getMessage();
      JOptionPane.showMessageDialog(this,message,"Invalid regexp",JOptionPane.ERROR_MESSAGE);
      Log.log(Log.MESSAGE, this, message);
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
