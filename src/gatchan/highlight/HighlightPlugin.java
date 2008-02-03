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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gatchan.highlight;

//{{{ imports
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import java.io.File;
//}}}

/**
 * The HighlightPlugin. This is my first plugin for jEdit, some parts of my code were inspired by the ErrorList plugin
 *
 * @author Matthieu Casanova
 * @version $Id: HighlightPlugin.java,v 1.20 2006/06/21 09:40:32 kpouer Exp $
 */
public class HighlightPlugin extends EBPlugin
{
	private static HighlightManager highlightManager;

	public static final String LAYER_PROPERTY = "plugin.highlight";
	public static final String NAME = "highlight";
	public static final String PROPERTY_PREFIX = "plugin.Highlight.";
	public static final String MENU = "highlight.menu";
	public static final String OPTION_PREFIX = "options.highlight.";

	private int layer;
	private float alpha;

	//{{{ start() method
	/**
	 * Initialize the plugin. When starting this plugin will add an Highlighter on each text area
	 */
	public void start()
	{
		layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
		File highlightFile = dataMigration();
		highlightManager = HighlightManagerTableModel.createInstance(highlightFile);
		highlightManager.propertiesChanged();
		View view = jEdit.getFirstView();
		while (view != null)
		{
			EditPane[] panes = view.getEditPanes();
			for (int i = 0; i < panes.length; i++)
			{
				JEditTextArea textArea = panes[i].getTextArea();
				initTextArea(textArea);
			}
			view = view.getNext();
		}
	} //}}}

	//{{{ stop() method
	/**
	 * uninitialize the plugin. we will remove the Highlighter on each text area
	 */
	public void stop()
	{
		highlightManager.dispose();
		if (highlightManager.countHighlights() == 0 && !highlightManager.isHighlightWordAtCaret())
			jEdit.setProperty("plugin.gatchan.highlight.HighlightPlugin.activate", "defer");
		else
			jEdit.setProperty("plugin.gatchan.highlight.HighlightPlugin.activate", "startup");

		Buffer[] buffers = jEdit.getBuffers();
		for (int i = 0; i < buffers.length; i++)
		{
			buffers[i].unsetProperty("highlights");
		}

		View view = jEdit.getFirstView();
		while (view != null)
		{
			EditPane[] panes = view.getEditPanes();
			for (int i = 0; i < panes.length; i++)
			{
				JEditTextArea textArea = panes[i].getTextArea();
				uninitTextArea(textArea);
			}
			view = view.getNext();
		}
		highlightManager = null;
	} //}}}

	//{{{ uninitTextArea() method
	/**
	 * Remove the highlighter from a text area.
	 *
	 * @param textArea the textarea from wich we will remove the highlighter
	 * @see #stop()
	 * @see #handleEditPaneMessage(EditPaneUpdate)
	 */
	private static void uninitTextArea(JEditTextArea textArea)
	{
		TextAreaPainter painter = textArea.getPainter();
		Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
		if (highlighter != null)
		{
			painter.removeExtension(highlighter);
			textArea.putClientProperty(Highlighter.class, null);
			highlightManager.removeHighlightChangeListener(highlighter);
		}
		textArea.removeCaretListener(highlightManager);
	} //}}}

	//{{{ initTextArea() method
	/**
	 * Initialize the textarea with a highlight painter.
	 *
	 * @param textArea the textarea to initialize
	 * @return the new highlighter for the textArea
	 */
	private Highlighter initTextArea(JEditTextArea textArea)
	{
		Highlighter highlighter = new Highlighter(textArea);
		highlightManager.addHighlightChangeListener(highlighter);
		TextAreaPainter painter = textArea.getPainter();
		painter.addExtension(layer, highlighter);
		textArea.putClientProperty(Highlighter.class, highlighter);
		textArea.addCaretListener(highlightManager);
		return highlighter;
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage message)
	{
		if (message instanceof EditPaneUpdate)
		{
			handleEditPaneMessage((EditPaneUpdate) message);
		}
		else if (message instanceof BufferUpdate)
		{
			BufferUpdate bufferUpdate = (BufferUpdate) message;
			if (bufferUpdate.getWhat() == BufferUpdate.CLOSED)
			{
				highlightManager.bufferClosed(bufferUpdate.getBuffer());
			}
		}
		else if (message instanceof PropertiesChanged)
		{
			int layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
			float alpha = ((float)jEdit.getIntegerProperty(HighlightOptionPane.PROP_ALPHA, 50)) / 100f;
			
			if (this.layer != layer || this.alpha != alpha)
			{
				this.layer = layer;
				this.alpha = alpha;
				View view = jEdit.getFirstView();
				while (view != null)
				{
					EditPane[] panes = view.getEditPanes();
					for (int i = 0; i < panes.length; i++)
					{
						JEditTextArea textArea = panes[i].getTextArea();
						TextAreaPainter painter = textArea.getPainter();
						Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
						highlighter.setAlphaComposite(alpha);
						if (highlighter != null)
						{
							painter.removeExtension(highlighter);
							painter.addExtension(highlighter);
						}
					}
					view = view.getNext();
				}
			}
			highlightManager.propertiesChanged();
		}
	} //}}}

	//{{{ handleEditPaneMessage() method
	private void handleEditPaneMessage(EditPaneUpdate message)
	{
		JEditTextArea textArea = message.getEditPane().getTextArea();
		Object what = message.getWhat();

		if (what == EditPaneUpdate.CREATED)
		{
			initTextArea(textArea);
		}
		else if (what == EditPaneUpdate.DESTROYED)
		{
			uninitTextArea(textArea);
		}
	} //}}}

	//{{{ highlightThis() methods
	/**
	 * Highlight a word in a textarea with PERMANENT_SCOPE. If a text is selected this text will be highlighted, if no
	 * text is selected we will ask the textarea to select a word
	 *
	 * @param textArea the textarea
	 */
	public static void highlightThis(JEditTextArea textArea)
	{
		highlightThis(textArea, Highlight.PERMANENT_SCOPE);
	}

	/**
	 * Highlight a word in a textarea. If a text is selected this text will be highlighted, if no text is selected we will
	 * ask the textarea to select a word
	 *
	 * @param textArea the textarea
	 * @param scope    the scope {@link Highlight#BUFFER_SCOPE},{@link Highlight#PERMANENT_SCOPE},{@link
	 *                 Highlight#SESSION_SCOPE}
	 */
	public static void highlightThis(JEditTextArea textArea, int scope)
	{
		String text = getCurrentWord(textArea);
		if (text == null) return;
		Highlight highlight = new Highlight(text);
		highlight.setScope(scope);
		if (scope == Highlight.BUFFER_SCOPE)
		{
			highlight.setBuffer(textArea.getBuffer());
		}
		highlightManager.addElement(highlight);
	} //}}}

	//{{{ getCurrentWord() method
	/**
	 * Get the current word. If nothing is selected, it will select it.
	 *
	 * @param textArea the textArea
	 * @return the current word
	 */
	private static String getCurrentWord(JEditTextArea textArea)
	{
		String text = textArea.getSelectedText();
		if (text == null)
		{
			textArea.selectWord();
			text = textArea.getSelectedText();
		}
		return text;
	} //}}}

	///{{{ highlightEntireWord() method
	/**
	 * Highlight a word in a textarea with PERMANENT_SCOPE. If a text is selected this text will be highlighted, if no
	 * text is selected we will ask the textarea to select a word. only the entire word will be highlighted
	 *
	 * @param textArea the textarea
	 */
	public static void highlightEntireWord(JEditTextArea textArea)
	{
		highlightEntireWord(textArea, Highlight.PERMANENT_SCOPE);
	} //}}}

	//{{{ highlightEntireWord() method
	/**
	 * Highlight a word in a textarea. If a text is selected this text will be highlighted, if no text is selected we will
	 * ask the textarea to select a word. only the entire word will be highlighted
	 *
	 * @param textArea the textarea
	 * @param scope    the scope {@link Highlight#BUFFER_SCOPE},{@link Highlight#PERMANENT_SCOPE},{@link
	 *                 Highlight#SESSION_SCOPE}
	 */
	public static void highlightEntireWord(JEditTextArea textArea, int scope)
	{
		String text = getCurrentWord(textArea);
		if (text == null) return;
		Highlight highlight = new Highlight("\\b" + text + "\\b", true, false);
		highlight.setScope(scope);
		if (scope == Highlight.BUFFER_SCOPE)
			highlight.setBuffer(textArea.getBuffer());

		highlightManager.addElement(highlight);
	} //}}}

	//{{{ highlightCurrentSearch() method
	/**
	 * Highlight the current search.
	 */
	public static void highlightCurrentSearch()
	{
		highlightCurrentSearch(Highlight.PERMANENT_SCOPE);
	} //}}}

	//{{{ highlightCurrentSearch() method
	/**
	 * Highlight the current serach with scope.
	 *
	 * @param scope the scope {@link Highlight#BUFFER_SCOPE},{@link Highlight#PERMANENT_SCOPE},{@link
	 *              Highlight#SESSION_SCOPE}
	 */
	public static void highlightCurrentSearch(int scope)
	{
		Highlight h = new Highlight();
		h.setScope(scope);
		if (scope == Highlight.BUFFER_SCOPE)
		{
			h.setBuffer(jEdit.getActiveView().getBuffer());
		}
		h.init(SearchAndReplace.getSearchString(),
		       SearchAndReplace.getRegexp(),
		       SearchAndReplace.getIgnoreCase(),
		       Highlight.getNextColor());
		addHighlight(h);
	} //}}}

	//{{{ highlightDialog() method
	/**
	 * Show an highlight dialog.
	 *
	 * @param view the current view
	 */
	public static void highlightDialog(View view)
	{
		HighlightDialog d = new HighlightDialog(view);
		d.setVisible(true);
	} //}}}

	//{{{ addHighlight() method
	public static void addHighlight(Highlight highlight)
	{
		highlightManager.addElement(highlight);
	} //}}}

	//{{{ removeAllHighlights() method
	public static void removeAllHighlights()
	{
		highlightManager.removeAll();
	} //}}}

	//{{{ enableHighlights(= method
	public static void enableHighlights()
	{
		highlightManager.setHighlightEnable(true);
	} //}}}

	//{{{ disableHighlights() method
	public static void disableHighlights()
	{
		highlightManager.setHighlightEnable(false);
	} //}}}

	//{{{ toggleHighlights() method 
	public static void toggleHighlights()
	{
		highlightManager.setHighlightEnable(!highlightManager.isHighlightEnable());
	} //}}}

	//{{{ isHighlightEnable() method
	public static boolean isHighlightEnable()
	{
		return highlightManager.isHighlightEnable();
	} //}}}

	//{{{ dataMigration() method
	/**
	 * Move the files and returns the new saved datas file.
	 * @return the saved datas file. It can be null
	 */
	public File dataMigration()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if (settingsDirectory == null)
			return null;
		// workaround until 4.3pre10
		File file = new File(settingsDirectory, "plugins");
		String home = new File(file, getClass().getName()).getPath();
		if (home == null)
			return null;

		String PROJECT_DIRECTORY = jEdit.getSettingsDirectory() + File.separator + "HighlightPlugin" + File.separator;
		File projectDirectory = new File(PROJECT_DIRECTORY);
		File highlights = new File(projectDirectory, "highlights.ser");
		File homeFolder = new File(home);
		if (!homeFolder.exists())
		{
			Log.log(Log.DEBUG, this, "Home doesn't exist, trying to create it " + home);
			if (!homeFolder.mkdirs())
			{
				Log.log(Log.ERROR, this, "Unable to create home directory, running Highlight plugin with no Home");
				return null;
			}
		}
		if (!homeFolder.isDirectory() || !homeFolder.canWrite())
		{
			Log.log(Log.ERROR, this, "Unable to write in home folder");
			return null;
		}

		File newFile = new File(homeFolder, "highlights.ser");
		if (highlights.isFile())
		{
			Log.log(Log.DEBUG, this, "Moving data to new home");
			IOUtilities.moveFile(highlights, newFile);
			highlights.delete();
			projectDirectory.delete();
			return newFile;
		}
		return newFile;
	} //}}}
}
