/*
 * HighlightDialog.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2020 Matthieu Casanova
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

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.gui.EnhancedDialog;

import javax.swing.*;
import java.awt.*;

/**
 * @author Matthieu Casanova
 * @version $Id: PluginJAR.java 8660 2007-01-17 21:25:11Z kpouer $
 */
public class HighlightDialog extends EnhancedDialog
{
	private final Highlight highlight;
	private final HighlightTablePanel panel = new HighlightTablePanel();
	private final JComboBox<Integer> scopeCombo = new JComboBox<>(new Integer[]{Highlight.PERMANENT_SCOPE, Highlight.SESSION_SCOPE, Highlight.BUFFER_SCOPE});
	private final JSpinner spinner;

	//{{{ HighlightDialog constructors
	public HighlightDialog(View owner, Highlight highlight)
	{
		super(owner, "Highlight", false);
		this.highlight = highlight;
		panel.setDialog(this);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		scopePanel.add(new JLabel("scope : "));
		scopeCombo.setRenderer(new MyListCellRenderer());
		scopePanel.add(scopeCombo);

		spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 5));
		spinner.setToolTipText(jEdit.getProperty("gatchan.highlight.expire.tooltip"));

		JButton ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(e -> ok());
		JButton cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(e -> cancel());
		JPanel buttonsPanel = new JPanel();

		LayoutManager layout = new BoxLayout(buttonsPanel, BoxLayout.X_AXIS);
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
		setLocationRelativeTo(owner);
	}

	public HighlightDialog(View owner)
	{
		this(owner, new Highlight());
	} //}}}

	public void setString(String s)
	{
		panel.setString(s);
	}

	//{{{ ok() method
	@Override
	public void ok()
	{
		try
		{
			panel.save(highlight);
			long expire = ((Number) spinner.getValue()).longValue();
			if (expire != 0)
			{
				highlight.setDuration(expire * 1000);
			}
			Integer selectedItem = (Integer) scopeCombo.getSelectedItem();
			int scope = selectedItem;
			highlight.setScope(scope);
			if (scope == Highlight.BUFFER_SCOPE)
			{
				Buffer buffer = jEdit.getActiveView().getBuffer();
				highlight.setBuffer(buffer);
			}
			HighlightManagerTableModel.getManager().addElement(highlight);
			dispose();
		}
		catch (InvalidHighlightException e)
		{
			GUIUtilities.error(jEdit.getActiveView(), "gatchan-highlight.errordialog.invalidHighlight", null);
			panel.focus();
		}
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ MyListCellRenderer class
	private static class MyListCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list,
							      Object value,
							      int index,
							      boolean isSelected,
							      boolean cellHasFocus)
		{
			if (isSelected)
			{
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else
			{
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			int scope = (Integer) value;
			switch (scope)
			{
				case Highlight.PERMANENT_SCOPE:
					setText("permanent");
					break;
				case Highlight.SESSION_SCOPE:
					setText("session");
					break;
				case Highlight.BUFFER_SCOPE:
					setText("buffer");
					break;
			}
			return this;
		}
	} //}}}

}
