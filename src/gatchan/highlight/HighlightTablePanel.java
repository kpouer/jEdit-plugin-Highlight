package gatchan.highlight;

import gnu.regexp.REException;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;

/**
 * This panel will be used to display and edit an Highlight in the JTable and in a dialog to add highlight.
 *
 * @author Matthieu Casanova
 */
public final class HighlightTablePanel extends JPanel {

  /** The field where the searched expression will be. */
  private final JTextField expressionField = new JTextField(40);

  /** This checkbox indicate if highlight is a regexp. */
  private final JCheckBox regexp = new JCheckBox("regexp");

  /** This checkbox indicate if highlight is case sensitive. */
  private final JCheckBox ignoreCase = new JCheckBox("ignore case");

  /** This button allow to choose the color of the highlight. */
  private final ColorWellButton colorBox = new ColorWellButton(Highlight.getNextColor());

  /** Instantiate the panel. */
  public HighlightTablePanel() {
    super(new GridBagLayout());
    ignoreCase.setSelected(true);
    final GridBagConstraints cons = new GridBagConstraints();
    cons.gridy = 0;

    cons.anchor = GridBagConstraints.WEST;
    final JLabel exprLabel = new JLabel("expr");
    add(exprLabel, cons);
    cons.fill = GridBagConstraints.HORIZONTAL;
    cons.weightx = 1;
    cons.gridwidth = GridBagConstraints.REMAINDER;
    add(expressionField, cons);
    cons.weightx = 0;
    cons.fill = GridBagConstraints.NONE;
    cons.gridy = 1;
    cons.gridwidth = 2;
    add(regexp, cons);
    add(ignoreCase, cons);
    cons.gridwidth = GridBagConstraints.REMAINDER;
    add(colorBox, cons);
    setBorder(BorderFactory.createEtchedBorder());
  }

  /**
   * Initialize the panel with an highlight.
   *
   * @param highlight the highlight we want to edit
   */
  public void setHighlight(Highlight highlight) {
    expressionField.setText(highlight.getStringToHighlight());
    regexp.setSelected(highlight.isRegexp());
    ignoreCase.setSelected(highlight.isIgnoreCase());
    colorBox.setSelectedColor(highlight.getColor());
  }

  /** The panel will request focus for expressionfield. */
  public void focus() {
    expressionField.requestFocus();
  }

  /**
   * Save the fields in the Highlight.
   *
   * @param highlight the highlight where we want to save
   *
   * @return true if it was saved, false otherwise
   */
  public boolean save(Highlight highlight) {
    try {
      final String stringToHighlight = expressionField.getText().trim();
      if (stringToHighlight.length() == 0) {
        JOptionPane.showMessageDialog(this, "String cannot be empty", "Invalid string", JOptionPane.ERROR_MESSAGE);
        return false;
      }
      highlight.init(stringToHighlight, regexp.isSelected(), ignoreCase.isSelected(), colorBox.getSelectedColor());
      return true;
    } catch (REException e) {
      final String message = "Invalid regexp " + e.getMessage();
      JOptionPane.showMessageDialog(this, message, "Invalid regexp", JOptionPane.ERROR_MESSAGE);
      Log.log(Log.MESSAGE, this, message);
      return false;
    }
  }
}
