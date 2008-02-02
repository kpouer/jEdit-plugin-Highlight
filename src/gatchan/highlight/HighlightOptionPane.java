/*
* HighlightPlugin.java - The Highlight plugin
* :tabSize=8:indentSize=8:noTabs=false:
* :folding=explicit:collapseFolds=1:
*
* Copyright (C) 2004, 2007 Matthieu Casanova
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

//{{{ imports
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.TextAreaPainter;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
//}}}

/**
* The option pane of the Highlight plugin.
*
* @author Matthieu Casanova
* @version $Id: HighlightOptionPane.java,v 1.8 2006/03/15 09:27:21 kpouer Exp $
*/
public class HighlightOptionPane extends AbstractOptionPane 
{
	
	public static final String PROP_COMMON_PROPERTIES = "gatchan.highlight.option-pane.commonProperties.text";
	public static final String PROP_DEFAULT_COLOR = "gatchan.highlight.defaultColor";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET = "gatchan.highlight.caretHighlight";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE = "gatchan.highlight.caretHighlight.ignoreCase";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD = "gatchan.highlight.caretHighlight.entireWord";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_COLOR = "gatchan.highlight.caretHighlight.color";
	public static final String PROP_HIGHLIGHT_CYCLE_COLOR = "gatchan.highlight.cycleColor";
	public static final String PROP_HIGHLIGHT_APPEND = "gatchan.highlight.appendHighlight";
	public static final String PROP_HIGHLIGHT_SUBSEQUENCE = "gatchan.highlight.subsequence";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_SUBSEQUENCE = "gatchan.highlight.caretHighlight.subsequence";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE = "gatchan.highlight.caretHighlight.whitespace";
	public static final String PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS = "gatchan.highlight.caretHighlight.onlyWords";
	
	public static final String PROP_LAYER_PROPERTY = "gatchan.highlight.layer";
	public static final String PROP_ALPHA = "gatchan.highlight.alpha";
	
	private JCheckBox highlightWordAtCaret;
	private JCheckBox wordAtCaretIgnoreCase;
	private JCheckBox entireWord;
	private ColorWellButton wordAtCaretColor;
	private JCheckBox cycleColor;
	private JCheckBox highlightAppend;
	private JCheckBox highlightSubsequence;
	private ColorWellButton defaultColor;
	private JCheckBox caretHighlightSubsequence;
	private JCheckBox wordAtCaretWhitespace;
	private JCheckBox wordAtCaretOnlyWords;
	private TextAreaExtensionLayerChooser layerChooser;
	private JSlider alphaSlider;
	
	//{{{ HighlightOptionPane constructor
	public HighlightOptionPane() 
	{
		super("gatchan.highlight");
	} //}}}
	
	//{{{ _init() method
	protected void _init() 
	{
		addSeparator(PROP_COMMON_PROPERTIES);
		addComponent(highlightSubsequence = createCheckBox(PROP_HIGHLIGHT_SUBSEQUENCE));
		addComponent(highlightAppend = createCheckBox(PROP_HIGHLIGHT_APPEND));
		addComponent(cycleColor = createCheckBox(PROP_HIGHLIGHT_CYCLE_COLOR));
		addComponent(new JLabel(jEdit.getProperty(PROP_DEFAULT_COLOR + ".text")),
			     defaultColor = new ColorWellButton(jEdit.getColorProperty(PROP_DEFAULT_COLOR)));
		cycleColor.addActionListener(new ActionListener() 
					     {
						     public void actionPerformed(ActionEvent e) 
						     {
							     defaultColor.setEnabled(!cycleColor.isSelected());
						     }
					     });
		if (cycleColor.isSelected())
			defaultColor.setEnabled(false);
		
		addSeparator(PROP_HIGHLIGHT_WORD_AT_CARET + ".text");
		addComponent(highlightWordAtCaret = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET));
		addComponent(caretHighlightSubsequence = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_SUBSEQUENCE));
		addComponent(wordAtCaretIgnoreCase = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE));
		addComponent(wordAtCaretWhitespace = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE));
		addComponent(wordAtCaretOnlyWords = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS));
		addComponent(entireWord = createCheckBox(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD));
		addComponent(new JLabel(jEdit.getProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR + ".text")),
			     wordAtCaretColor = new ColorWellButton(jEdit.getColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR)));
		addComponent(new JLabel(jEdit.getProperty(PROP_LAYER_PROPERTY + ".text")), 
			     layerChooser = new TextAreaExtensionLayerChooser(jEdit.getIntegerProperty(PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER)));
		
		addComponent(new JLabel(jEdit.getProperty(PROP_ALPHA + ".text")), 
			     alphaSlider = new JSlider(0,
						       100,
						       jEdit.getIntegerProperty(PROP_ALPHA, 50)));
	} //}}}
	
	//{{{ _save() method
	protected void _save() 
	{
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET, highlightWordAtCaret.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_SUBSEQUENCE, caretHighlightSubsequence.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_IGNORE_CASE, wordAtCaretIgnoreCase.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_ENTIRE_WORD, entireWord.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_WHITESPACE, wordAtCaretWhitespace.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_WORD_AT_CARET_ONLYWORDS, wordAtCaretOnlyWords.isSelected());
		
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_SUBSEQUENCE, highlightSubsequence.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_CYCLE_COLOR, cycleColor.isSelected());
		jEdit.setBooleanProperty(PROP_HIGHLIGHT_APPEND, highlightAppend.isSelected());
		jEdit.setColorProperty(PROP_HIGHLIGHT_WORD_AT_CARET_COLOR, wordAtCaretColor.getSelectedColor());
		jEdit.setColorProperty(PROP_DEFAULT_COLOR, defaultColor.getSelectedColor());
		try 
		{
			jEdit.setIntegerProperty(PROP_LAYER_PROPERTY, layerChooser.getLayer());
		} 
		catch(NumberFormatException e)
		{
		}
		
		jEdit.setIntegerProperty(PROP_ALPHA, alphaSlider.getValue());
	} //}}}

	//{{{ createCheckBox() method
	private static JCheckBox createCheckBox(String property) 
	{
		JCheckBox checkbox = new JCheckBox(jEdit.getProperty(property + ".text"));
		checkbox.setSelected(jEdit.getBooleanProperty(property));
		return checkbox;
	} //}}}
}
