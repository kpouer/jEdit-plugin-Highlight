/*
 * HighlightPlugin.java - The Highlight plugin
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
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.browser.VFSFileChooserDialog;
import org.gjt.sp.jedit.io.VFSManager;
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
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
//}}}

/**
 * The HighlightPlugin. This is my first plugin for jEdit, some parts of my code were inspired by the ErrorList plugin
 *
 * @author Matthieu Casanova
 */
public class HighlightPlugin extends EditPlugin
{
	private static HighlightManager highlightManager;

	private int layer;
	private float alpha;
	private boolean roundCorner;
	private boolean highlightOverview;
	private boolean highlightOverviewSameColor;
	private Color highlightOverviewColor;
	private int extraLineSpacing;

	//{{{ start() method
	/**
	 * Initialize the plugin. When starting this plugin will add an Highlighter on each text area
	 */
	@Override
	public void start()
	{
		layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
		highlightOverview = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW);
		var highlightFile = getDataFile();

		highlightManager = HighlightManagerTableModel.createInstance(highlightFile.orElse(null));
		highlightManager.propertiesChanged();
		jEdit.getEditPaneManager().forEach(editPane -> initTextArea(editPane.getTextArea()));
		jEdit.getViewManager().forEach(HighlightPlugin::initView);
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
		jEdit.resetProperty("plugin.gatchan.highlight.HighlightPlugin.activate");

		jEdit.getBufferManager().forEach(buffer -> buffer.unsetProperty(Highlight.HIGHLIGHTS_BUFFER_PROPS));
		jEdit.getEditPaneManager().forEach(editPane -> uninitTextArea(editPane.getTextArea()));
		jEdit.getViewManager().forEach(HighlightPlugin::uninitView);
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
		var painter = textArea.getPainter();
		var highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
		if (highlighter != null)
		{
			painter.removeExtension(highlighter);
			textArea.putClientProperty(Highlighter.class, null);
			highlightManager.removeHighlightChangeListener(highlighter);
		}
		var flexColorPainter = (FlexColorPainter) textArea.getClientProperty(FlexColorPainter.class);
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
	 */
	private void initTextArea(JEditTextArea textArea)
	{
		var highlighter = new Highlighter(textArea);
		highlightManager.addHighlightChangeListener(highlighter);
		var painter = textArea.getPainter();
		painter.addExtension(layer, highlighter);
		textArea.putClientProperty(Highlighter.class, highlighter);
		textArea.addCaretListener(highlightManager);
		var flexColorPainter = new FlexColorPainter(textArea);
		textArea.putClientProperty(FlexColorPainter.class, flexColorPainter);
		painter.addExtension(layer-1, flexColorPainter);
		if (highlightOverview)
			addHighlightOverview(textArea);
		textArea.revalidate();
	} //}}}

	//{{{ addHighlightOverview() method
	private static void addHighlightOverview(JEditTextArea textArea)
	{
		var currentOverview = (HighlightOverview) textArea.getClientProperty(HighlightOverview.class);
		if (currentOverview == null)
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
		var overview = (HighlightOverview) textArea.getClientProperty(HighlightOverview.class);
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
	 */
	private static void initView(View view)
	{
		var highlighter = new HighlightHypersearchResults(view);
		highlighter.start();
		view.getDockableWindowManager().putClientProperty(HighlightHypersearchResults.class, highlighter);
	} //}}}

	//{{{ uninitView() method
	/**
	 * Remove the hypersearch results highlighter from the view.
	 *
	 * @param view the view whose hypersearch results to initialize
	 */
	private static void uninitView(View view)
	{
		var highlighter = (HighlightHypersearchResults)
			view.getDockableWindowManager().getClientProperty(
					HighlightHypersearchResults.class);
		if (highlighter == null)
			return;
		highlighter.stop();
		view.getDockableWindowManager().putClientProperty(HighlightHypersearchResults.class, null);
	} //}}}

	//{{{ handleMessage() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged propertiesChanged)
	{
		var newOverview = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW);
		var newOverviewSameColor = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_SAMECOLOR);
		var newOverviewColor = jEdit.getColorProperty(HighlightOptionPane.PROP_HIGHLIGHT_OVERVIEW_COLOR);
		var layer = jEdit.getIntegerProperty(HighlightOptionPane.PROP_LAYER_PROPERTY, TextAreaPainter.HIGHEST_LAYER);
		var alpha = ((float)jEdit.getIntegerProperty(HighlightOptionPane.PROP_ALPHA, 50)) / 100f;
		var roundCorner = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_ROUND_CORNER);
		var extraLineSpacing = jEdit.getIntegerProperty("options.textarea.extraLineSpacing");
		if (this.extraLineSpacing != extraLineSpacing || this.roundCorner != roundCorner || this.layer != layer ||
			this.alpha != alpha || newOverview != highlightOverview ||
			newOverviewSameColor != highlightOverviewSameColor ||
			(highlightOverviewColor != null && !highlightOverviewColor.equals(newOverviewColor)))
		{
			highlightOverview = newOverview;
			highlightOverviewSameColor = newOverviewSameColor;
			highlightOverviewColor = newOverviewColor;
			this.layer = layer;
			this.alpha = alpha;
			this.roundCorner = roundCorner;
			this.extraLineSpacing = extraLineSpacing;
			jEdit.visit(new JEditVisitorAdapter()
			{
				@Override
				public void visit(JEditTextArea textArea)
				{
					TextAreaPainter painter = textArea.getPainter();
					Highlighter highlighter = (Highlighter) textArea.getClientProperty(Highlighter.class);
					highlighter.setAlphaComposite(HighlightPlugin.this.alpha);
					highlighter.setRoundcorner(HighlightPlugin.this.roundCorner);
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
		var view = vu.getView();
		var what = vu.getWhat();

		if (what == ViewUpdate.CREATED)
		{
			initView(view);
		}
		else if (what == ViewUpdate.CLOSED)
		{
			uninitView(view);
		}
		else if (what == ViewUpdate.EDIT_PANE_CHANGED)
		{
			highlightManager.caretUpdate(view.getTextArea());
		}
	}

	//{{{ handleEditPaneMessage() method
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate editPaneUpdate)
	{
		var textArea = editPaneUpdate.getEditPane().getTextArea();
		var what = editPaneUpdate.getWhat();

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
			var view = ((DockableWindowManager) dockableUpdate.getSource()).getView();
			var highlighter = (HighlightHypersearchResults)
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
		var text = getCurrentWord(textArea);
		if (text == null) return;
		var highlight = new Highlight(text);
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
		var text = textArea.getSelectedText();
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
		var text = getCurrentWord(textArea);
		if (text == null) return;
		var highlight = new Highlight("\\b" + text + "\\b", true, false);
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
		var h = new Highlight();
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
		var currentWord = getCurrentWord(textArea);
		var d = new HighlightDialog(view);

		if (currentWord != null && !currentWord.isEmpty())
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

		var h = (HighlightHypersearchResults)
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

	//{{{ getDataFile() method
	/**
	 * @return the saved datas file.
	 */
	public Optional<Path> getDataFile()
	{
		var pluginHome = getPluginHome();
		if (pluginHome == null)
			return Optional.empty();
		var pluginHomePath = pluginHome.toPath();
		try
		{
			Files.createDirectories(pluginHomePath);
			return Optional.of(Path.of(pluginHomePath.toString(), "highlights.ser"));
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, this, "unable to create plugin home folder", e);
		}
		return Optional.empty();
	} //}}}

	public static void exportFile()
	{
		Log.log(Log.MESSAGE, HighlightPlugin.class, "exportFile");
		var string = highlightManager.exportToString();
		var vfsFileChooserDialog = new VFSFileChooserDialog(jEdit.getActiveView(),
			System.getProperty("user.home"),
			VFSBrowser.SAVE_DIALOG,
			false,
			true);
		var selectedFiles = vfsFileChooserDialog.getSelectedFiles();
		if (selectedFiles.length == 1)
		{
			var savePath = selectedFiles[0];
			var vfs = VFSManager.getVFSForPath(savePath);
			var vfsSession = vfs.createVFSSession(savePath, jEdit.getActiveView());
			try (var outputStream = vfs._createOutputStream(vfsSession, savePath, jEdit.getActiveView()))
			{
				IOUtilities.copyStream(null, new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)), outputStream,  false);
			}
			catch (IOException e)
			{
				Log.log(Log.ERROR, HighlightPlugin.class, e);
			}
			finally
			{
				try
				{
					vfs._endVFSSession(vfsSession, jEdit.getActiveView());
				}
				catch (IOException e)
				{
					Log.log(Log.ERROR, HighlightPlugin.class, e);
				}
			}
		}
	}

	public static void importFile()
	{
		Log.log(Log.MESSAGE, HighlightPlugin.class, "importFile");
		var selectedFiles = GUIUtilities.showVFSFileDialog(jEdit.getActiveView(),
			System.getProperty("user.home"),
			VFSBrowser.OPEN_DIALOG, false);
		if (selectedFiles.length == 1)
		{
			var loadPath = selectedFiles[0];
			var tmpBuffer = jEdit.openTemporary(jEdit.getActiveView(), null, loadPath, false);
			highlightManager.importFromString(tmpBuffer.getText());
		}
	}
}
