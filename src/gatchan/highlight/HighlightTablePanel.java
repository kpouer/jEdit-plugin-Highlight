package gatchan.highlight;

import gnu.regexp.REException;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.*;

/**
 * This panel will be used to display and edit an Highlight in the JTable.
 *
 * @author Matthieu Casanova
 */
public final class HighlightTablePanel extends JPanel {

  private final JTextField expressionField = new JTextField();
  private final JCheckBox regexp = new JCheckBox("regexp");
  private final ColorWellButton colorBox = new ColorWellButton(Color.black);

  public HighlightTablePanel() {
    super(new GridBagLayout());
    final GridBagConstraints cons = new GridBagConstraints();
    cons.gridy = 0;

    cons.anchor = GridBagConstraints.WEST;
    final JLabel exprLabel = new JLabel("expr");
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
    setBorder(BorderFactory.createEtchedBorder());
  }

  public void setHighlight(Highlight highlight) {
    expressionField.setText(highlight.getStringToHighlight());
    regexp.setSelected(highlight.isRegexp());
    colorBox.setSelectedColor(highlight.getColor());
  }

  public boolean save(Highlight highlight) {
      try {
        highlight.init(expressionField.getText().trim(), regexp.isSelected(), colorBox.getSelectedColor());
        return true;
      } catch (REException e) {
        Log.log(Log.ERROR, this, "Unable to save the highlight");
        return false;
      }
    }
}
