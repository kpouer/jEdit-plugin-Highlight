/*
 * TextAreaExtensionLayerChooser.java - A combo to choose a textarea extension 
 * layer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007, 2009 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gatchan.highlight;

//{{{ imports
import org.gjt.sp.jedit.textarea.TextAreaPainter;

import javax.swing.*;
//}}}

/**
 * @author Matthieu Casanova
 * @version $Id: HighlightPlugin.java,v 1.20 2006/06/21 09:40:32 kpouer Exp $
 */
public class TextAreaExtensionLayerChooser extends JComboBox
{
	private static String[] layers = {	TextAreaPainter.LOWEST_LAYER                 + " (LOWEST_LAYER)" ,
						TextAreaPainter.BACKGROUND_LAYER             + " (BACKGROUND_LAYER)",
						TextAreaPainter.LINE_BACKGROUND_LAYER        + " (LINE_BACKGROUND_LAYER)",
						TextAreaPainter.BELOW_SELECTION_LAYER        + " (BELOW_SELECTION_LAYER)",
						TextAreaPainter.SELECTION_LAYER              + " (SELECTION_LAYER)",
						TextAreaPainter.WRAP_GUIDE_LAYER             + " (WRAP_GUIDE_LAYER)",
						TextAreaPainter.BELOW_MOST_EXTENSIONS_LAYER  + " (BELOW_MOST_EXTENSIONS_LAYER)",
						TextAreaPainter.DEFAULT_LAYER                + " (DEFAULT_LAYER)",
						TextAreaPainter.BLOCK_CARET_LAYER            + " (BLOCK_CARET_LAYER)",
						TextAreaPainter.BRACKET_HIGHLIGHT_LAYER      + " (BRACKET_HIGHLIGHT_LAYER)",
						TextAreaPainter.TEXT_LAYER                   + " (TEXT_LAYER)",
						TextAreaPainter.CARET_LAYER                  + " (CARET_LAYER)",
						TextAreaPainter.HIGHEST_LAYER	              + " (HIGHEST_LAYER)"};
	                                                                                                                
	//{{{ TextAreaExtensionLayerChooser constructor
	/**
	 * Initialize the plugin. When starting this plugin will add an Highlighter on each text area
	 */
	public TextAreaExtensionLayerChooser(int currentLayer)
	{
		super(layers);
		setEditable(true);
		switch (currentLayer)
		{
				case TextAreaPainter.LOWEST_LAYER               	:setSelectedIndex(0);break;
				case TextAreaPainter.BACKGROUND_LAYER           	:setSelectedIndex(1);break;
				case TextAreaPainter.LINE_BACKGROUND_LAYER      	:setSelectedIndex(2);break;
				case TextAreaPainter.BELOW_SELECTION_LAYER      	:setSelectedIndex(3);break;
				case TextAreaPainter.SELECTION_LAYER            	:setSelectedIndex(4);break;
				case TextAreaPainter.WRAP_GUIDE_LAYER           	:setSelectedIndex(5);break;
				case TextAreaPainter.BELOW_MOST_EXTENSIONS_LAYER	:setSelectedIndex(6);break;
				case TextAreaPainter.DEFAULT_LAYER              	:setSelectedIndex(7);break;
				case TextAreaPainter.BLOCK_CARET_LAYER          	:setSelectedIndex(8);break;
				case TextAreaPainter.BRACKET_HIGHLIGHT_LAYER    	:setSelectedIndex(9);break;
				case TextAreaPainter.TEXT_LAYER                 	:setSelectedIndex(10);break;
				case TextAreaPainter.CARET_LAYER 			:setSelectedIndex(11);break;
				case TextAreaPainter.HIGHEST_LAYER			:setSelectedIndex(12);break;
				default							:setSelectedItem(Integer.toString(currentLayer));
		}                                                               
	} //}}}
	
	//{{{ getLayer() method
	public int getLayer() throws NumberFormatException
	{
		String value = (String) getSelectedItem();
		for (String item: layers)
		{
			if (item.equals(value))
			{
				int layer = Integer.parseInt(value.substring(0, value.indexOf(' ')));
				return layer;
			}
		}
		int layer = Integer.parseInt(value);
		return layer;
	} //}}}
}
