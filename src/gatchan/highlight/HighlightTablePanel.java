/*
 * HighlightTablePanel.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2010 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gatchan.highlight;

import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.gui.HistoryTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This panel will be used to display and edit an Highlight in the JTable and in a dialog to add highlight.
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightTablePanel.java,v 1.14 2006/06/21 09:40:32 kpouer Exp $
 */
public class HighlightTablePanel extends JPanel
{
	/**
	 * The field where the searched expression will be.
	 */
	private final HistoryTextField expressionField = new HistoryTextField("gatchan-highlight.expression");
	//private final JTextField expressionField = new JTextField(40);

	/**
	 * This checkbox indicate if highlight is a regexp.
	 */
	private final JCheckBox regexp = new JCheckBox("regexp");

	/**
	 * This checkbox indicate if highlight is case sensitive.
	 */
	private final JCheckBox ignoreCase = new JCheckBox("ignore case");

	/**
	 * This button allow to choose the color of the highlight.
	 */
	private final ColorWellButton colorBox = new ColorWellButton(Highlight.getNextColor());
	private HighlightCellEditor highlightCellEditor;

	private boolean initialized;

	private Color permanentScopeColor;
	private static final Color SESSION_SCOPE_COLOR = new Color(0xcc, 0xcc, 0xff);
	private static final Color BUFFER_SCOPE_COLOR = new Color(0xff, 0xfc, 0xc0);

	/**
	 * Instantiate the panel.
	 */
	public HighlightTablePanel()
	{
		super(new GridBagLayout());
		permanentScopeColor = getBackground();
		ignoreCase.setSelected(true);
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = 0;

		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.weightx = 1.0;
		cons.gridwidth = GridBagConstraints.REMAINDER;
		add(expressionField, cons);
		cons.weightx = 0.0;
		cons.fill = GridBagConstraints.NONE;
		cons.gridy = 1;
		cons.gridwidth = 2;
		add(regexp, cons);
		add(ignoreCase, cons);
		cons.gridwidth = GridBagConstraints.REMAINDER;
		add(colorBox, cons);
		setBorder(BorderFactory.createEtchedBorder());
	}

	public void setString(String s)
	{
		expressionField.setText(s);
	}

	public void setDialog(EnhancedDialog dialog)
	{
		expressionField.addActionListener(new MyActionListener(dialog));
	}

	/**
	 * Initialize the panel with an highlight.
	 *
	 * @param highlight the highlight we want to edit
	 */
	public void setHighlight(Highlight highlight)
	{
		expressionField.setText(highlight.getStringToHighlight());
		regexp.setSelected(highlight.isRegexp());
		ignoreCase.setSelected(highlight.isIgnoreCase());
		colorBox.setSelectedColor(highlight.getColor());
		if (highlightCellEditor != null)
		{
			expressionField.getDocument().addDocumentListener(highlightCellEditor);
			if (!initialized)
			{
				regexp.addActionListener(highlightCellEditor);
				ignoreCase.addActionListener(highlightCellEditor);
				ActionListener[] actionListeners = colorBox.getActionListeners();
				if (actionListeners.length == 1)
				{
					ActionListener actionListener = actionListeners[0];
					colorBox.removeActionListener(actionListener);
					colorBox.addActionListener(new SpecialColorWellButtonActionListener(actionListener, highlightCellEditor));
				}
				initialized = true;
			}
		}
		switch (highlight.getScope())
		{
			case Highlight.SESSION_SCOPE:
				changeBackgroundColor(SESSION_SCOPE_COLOR);
				break;
			case Highlight.BUFFER_SCOPE:
				changeBackgroundColor(BUFFER_SCOPE_COLOR);
				break;
			default:
				changeBackgroundColor(permanentScopeColor);
		}
	}

	public void changeBackgroundColor(Color bg)
	{
		setBackground(bg);
		regexp.setBackground(bg);
		ignoreCase.setBackground(bg);
	}

	/**
	 * The panel will request focus for expressionfield.
	 */
	public void focus()
	{
		expressionField.requestFocus();
	}

	/**
	 * Save the fields in the Highlight.
	 *
	 * @param highlight the highlight where we want to save
	 */
	public void save(Highlight highlight) throws InvalidHighlightException
	{
		String stringToHighlight = expressionField.getText();
		if (stringToHighlight.length() == 0)
		{
			throw new InvalidHighlightException("String cannot be empty");
		}
		highlight.init(stringToHighlight, regexp.isSelected(), ignoreCase.isSelected(), colorBox.getSelectedColor());
		expressionField.addCurrentToHistory();
	}

	public void stopEdition()
	{
		if (highlightCellEditor != null)
		{
			expressionField.getDocument().removeDocumentListener(highlightCellEditor);
		}
	}

	public void setCellEditor(HighlightCellEditor highlightCellEditor)
	{
		this.highlightCellEditor = highlightCellEditor;
	}

	private static class SpecialColorWellButtonActionListener implements ActionListener
	{
		private final ActionListener actionListener;
		private final HighlightCellEditor highlightCellEditor;

		SpecialColorWellButtonActionListener(ActionListener actionListener,
						     HighlightCellEditor highlightCellEditor)
		{
			this.actionListener = actionListener;
			this.highlightCellEditor = highlightCellEditor;
		}

		public void actionPerformed(ActionEvent e)
		{
			actionListener.actionPerformed(e);
			highlightCellEditor.stopCellEditing();
		}
	}

	private static class MyActionListener implements ActionListener
	{
		private final EnhancedDialog dialog;

		MyActionListener(EnhancedDialog dialog)
		{
			this.dialog = dialog;
		}

		public void actionPerformed(ActionEvent e)
		{
			dialog.ok();
		}
	}
}
