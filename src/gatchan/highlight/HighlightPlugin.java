/*
 * HighlightPlugin.java - The Highlight plugin
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004, 2011 Matthieu Casanova
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

//{{{ Imports
import gatchan.highlight.color.FlexColorPainter;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.DockableWindowUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaPainter;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import java.awt.Color;
import java.io.File;
//}}}

/**
 * The HighlightPlugin. This is my first plugin for jEdit, some parts of my code were inspired by the ErrorList plugin
 *
 * @author Matthieu Casanova
 */
public class HighlightPlugin extends EditPlugin
{
	private static HighlightManager highlightManager;

	public static final String LAYER_PROPERTY = "plugin.highlight";
	public static final String NAME = "highlight";
	public static final String PROPERTY_PREFIX = "plugin.Highlight.";
	public static final String MENU = "highlight.menu";
	public static final String OPTION_PREFIX = "options.highlight.";

	private int layer;
	private float alpha;
	private boolean highlightOverview;
	private boolean highlightOverviewSameColor;
	private Color highlightOverviewColor;

	//{{{ start() method
	/**
	 * Initialize the plugin. When starting this plugin will add an Highlighter on each text area
	 */
	@Override
	public void start()
	{
		layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
		highlightOverview = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW);
		File highlightFile = dataMigration();
		highlightManager = HighlightManagerTableModel.createInstance(highlightFile);
		highlightManager.propertiesChanged();
		jEdit.visit(new TextAreaInitializer());
		jEdit.visit(new ViewInitializer());
		EditBus.addToBus(this);
	} //}}}

	//{{{ stop() method
	/**
	 * uninitialize the plugin. we will remove the Highlighter on each text area
	 */
	@Override
	public void stop()
	{
		EditBus.removeFromBus(this);
		if (highlightManager.countHighlights() == 0 && !highlightManager.isHighlightWordAtCaret())
			jEdit.setProperty("plugin.gatchan.highlight.HighlightPlugin.activate", "defer");
		else
			jEdit.setProperty("plugin.gatchan.highlight.HighlightPlugin.activate", "startup");

		Buffer[] buffers = jEdit.getBuffers();
		for (int i = 0; i < buffers.length; i++)
		{
			buffers[i].unsetProperty(Highlight.HIGHLIGHTS_BUFFER_PROPS);
		}

		jEdit.visit(new TextAreaUninitializer());
		jEdit.visit(new ViewUninitializer());
		highlightManager.dispose();
		highlightManager = null;
	} //}}}

	//{{{ uninitTextArea() method
	/**
	 * Remove the highlighter from a text area.
	 *
	 * @param textArea the textarea from wich we will remove the highlighter
	 * @see #stop()
	 * @see #handleEditPaneUpdate(org.gjt.sp.jedit.msg.EditPaneUpdate) 
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
		FlexColorPainter flexColorPainter = (FlexColorPainter) textArea.getClientProperty(FlexColorPainter.class);
		if (flexColorPainter != null)
		{
			painter.removeExtension(flexColorPainter);
			textArea.putClientProperty(FlexColorPainter.class, null);
		}
		removeHighlightOverview(textArea);
		textArea.removeCaretListener(highlightManager);
	} //}}}

	//{{{ initTextArea() method
	/**
	 * Initialize the textarea with a highlight painter.
	 *
	 * @param textArea the textarea to initialize
	 * @return the new highlighter for the textArea
	 */
	private void initTextArea(JEditTextArea textArea)
	{
		Highlighter highlighter = new Highlighter(textArea);
		highlightManager.addHighlightChangeListener(highlighter);
		TextAreaPainter painter = textArea.getPainter();
		painter.addExtension(layer, highlighter);
		textArea.putClientProperty(Highlighter.class, highlighter);
		textArea.addCaretListener(highlightManager);
		FlexColorPainter flexColorPainter = new FlexColorPainter(textArea);
		textArea.putClientProperty(FlexColorPainter.class, flexColorPainter);
		painter.addExtension(layer-1, flexColorPainter);
		addHighlightOverview(textArea);
		textArea.revalidate();
	} //}}}

	//{{{ addHighlightOverview() method
	private void addHighlightOverview(JEditTextArea textArea)
	{
		HighlightOverview currentOverview = (HighlightOverview) textArea.getClientProperty(HighlightOverview.class);
		if (highlightOverview && currentOverview == null) 
		{
			currentOverview = new HighlightOverview(textArea);
			highlightManager.addHighlightChangeListener(currentOverview);
			textArea.addLeftOfScrollBar(currentOverview);
			textArea.putClientProperty(HighlightOverview.class, currentOverview);
		}
		if (!jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_SAMECOLOR))
			currentOverview.setOverviewColor(jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_COLOR));
		else
			currentOverview.setOverviewColor(null);
	} //}}}

	//{{{ addHighlightOverview() method
	private static void removeHighlightOverview(JEditTextArea textArea)
	{
		HighlightOverview overview = (HighlightOverview) textArea.getClientProperty(HighlightOverview.class);
		if (overview != null)
		{
			textArea.removeLeftOfScrollBar(overview);
			textArea.putClientProperty(HighlightOverview.class, null);
			highlightManager.removeHighlightChangeListener(overview);
			textArea.revalidate();
		}
	} //}}}

	//{{{ initView() method
	/**
	 * Initialize the view with a hypersearch results highlighter.
	 *
	 * @param view the view whose hypersearch results to initialize
	 * @return the new highlighter for the hypersearch results of the view
	 */
	private HighlightHypersearchResults initView(View view)
	{
		HighlightHypersearchResults highlighter = new HighlightHypersearchResults(view);
		highlighter.start();
		view.getDockableWindowManager().putClientProperty(
			HighlightHypersearchResults.class, highlighter);
		return highlighter;
	} //}}}

	//{{{ uninitView() method
	/**
	 * Remove the hypersearch results highlighter from the view.
	 *
	 * @param view the view whose hypersearch results to initialize
	 */
	private static void uninitView(View view)
	{
		HighlightHypersearchResults highlighter = (HighlightHypersearchResults)
			view.getDockableWindowManager().getClientProperty(
					HighlightHypersearchResults.class);
		if (highlighter == null)
			return;
		highlighter.stop();
		view.getDockableWindowManager().putClientProperty(
			HighlightHypersearchResults.class, null);
	} //}}}

	//{{{ handleMessage() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged propertiesChanged)
	{
		boolean newOverview = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW);
		boolean newOverviewSameColor = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_SAMECOLOR);
		Color newOverviewColor = jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_COLOR);
		int layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
		float alpha = ((float)jEdit.getIntegerProperty(HighlightOptionPane.PROP_ALPHA, 50)) / 100f;
		if (this.layer != layer || this.alpha != alpha || newOverview != highlightOverview || newOverviewSameColor != highlightOverviewSameColor ||
			(highlightOverviewColor != null && !highlightOverviewColor.equals(newOverviewColor)))
		{
			highlightOverview = newOverview;
			highlightOverviewSameColor = newOverviewSameColor;
			highlightOverviewColor = newOverviewColor;
			this.layer = layer;
			this.alpha = alpha;
			jEdit.visit(new JEditVisitorAdapter()
			{
				@Override
				public void visit(JEditTextArea textArea)
				{
					TextAreaPainter painter = textArea.getPainter();
					Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
					highlighter.setAlphaComposite(HighlightPlugin.this.alpha);
					painter.removeExtension(highlighter);
					painter.addExtension(HighlightPlugin.this.layer, highlighter);
					if (highlightOverview)
						addHighlightOverview(textArea);
					else
						removeHighlightOverview(textArea);
				}
			});
		}
		highlightManager.propertiesChanged();
	} //}}}


	@EBHandler
	public void handleViewUpdate(ViewUpdate vu)
	{
		View view = vu.getView();
		Object what = vu.getWhat();

		if (what == ViewUpdate.CREATED)
		{
			initView(view);
		}
		else if (what == ViewUpdate.CLOSED)
		{
			uninitView(view);
		}
	}

	//{{{ handleEditPaneMessage() method
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate editPaneUpdate)
	{
		JEditTextArea textArea = editPaneUpdate.getEditPane().getTextArea();
		Object what = editPaneUpdate.getWhat();

		if (what == EditPaneUpdate.CREATED)
		{
			initTextArea(textArea);
		}
		else if (what == EditPaneUpdate.DESTROYED)
		{
			uninitTextArea(textArea);
		}
	} //}}}

	//{{{ handleBufferPaneUpdate() method
	@EBHandler
	public void handleBufferPaneUpdate(BufferUpdate bufferUpdate)
	{
		if (bufferUpdate.getWhat() == BufferUpdate.CLOSED)
		{
			highlightManager.bufferClosed(bufferUpdate.getBuffer());
		}
	} //}}}

	//{{{ handleBufferPaneUpdate() method
	@EBHandler
	public void handleDockableWindowUpdate(DockableWindowUpdate dockableUpdate)
	{
		if (dockableUpdate.getWhat() == DockableWindowUpdate.ACTIVATED &&
			HighlightHypersearchResults.HYPERSEARCH.equals(dockableUpdate.getDockable()))
		{
			View view = ((DockableWindowManager) dockableUpdate.getSource()).getView();
			HighlightHypersearchResults highlighter = (HighlightHypersearchResults)
			view.getDockableWindowManager().getClientProperty(
				HighlightHypersearchResults.class);
			if (highlighter == null)
				initView(view);
			else
				highlighter.start();
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
	private static String getCurrentWord(TextArea textArea)
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
	public static void highlightDialog(View view, TextArea textArea)
	{
		String currentWord = getCurrentWord(textArea);
		HighlightDialog d = new HighlightDialog(view);

		if (currentWord != null && currentWord.length() != 0)
		{
			d.setString(currentWord);
		}
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

	//{{{ highlightHyperSearchResult() method
	public static void highlightHyperSearchResult(View view)
	{
		
		HighlightHypersearchResults h = (HighlightHypersearchResults)
			view.getDockableWindowManager().getClientProperty(
				HighlightHypersearchResults.class);
		if (h == null)
			return;
		h.start();
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

	//{{{ TextAreaInitializer class
	private class TextAreaInitializer extends JEditVisitorAdapter
	{
		public void visit(JEditTextArea textArea)
		{
			initTextArea(textArea);
		}
	} //}}}

	//{{{ TextAreaUninitializer class
	private class TextAreaUninitializer extends JEditVisitorAdapter
	{
		public void visit(JEditTextArea textArea)
		{
			uninitTextArea(textArea);
		}
	} //}}}
	
	//{{{ ViewInitializer class
	private class ViewInitializer extends JEditVisitorAdapter
	{
		public void visit(View view)
		{
			initView(view);
		}
	} //}}}

	//{{{ ViewUninitializer class
	private class ViewUninitializer extends JEditVisitorAdapter
	{
		public void visit(View view)
		{
			uninitView(view);
		}
	} //}}}
}
