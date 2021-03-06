/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the Text Trix code.
 *
 * The Initial Developer of the Original Code is
 * Text Flex.
 * Portions created by the Initial Developer are Copyright (C) 2003-8, 2015, 
 * 2018
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s): David Young <david@textflex.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.textflex.texttrix;

import javax.swing.*;
import java.util.prefs.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.filechooser.FileFilter;

import javax.swing.event.*;

/** Stores the user-preferences for Text Trix.
 * Presents a display panel for users to pick from various options to customize
 * their Text Trix experience.  The goal is to make the text tricky, but the text-making easy.
 * 
 * <p>The preferences utilize the Java preference scheme to store settings in a
 * centralized, systematic location for each platform.  The settings can also be exported to 
 * other systems for perpetual Text Trix fun.
 * @author davit
 */
public class Prefs extends JDialog {//JFrame {
	
	// houses and categorizes options
	private JTabbedPane tabbedPane = new JTabbedPane();
	// main prefs
	private Preferences prefs = Preferences.userNodeForPackage(TextTrix.class);
	// default filename for backing up the prefs
	private static final String FILE_PREFS_USER_TREE = "preferences-usertree.xml";






	/* Internal (panel-specific) prefs */
	private Preferences internalPrefs = prefs.node("Internal"); // panel-specific prefs holder
	
	// names the preferences panel as PREFS;
	// the panel will be treated as a plug-in for internal orgnaization
	private static final String PREFS = "prefs";
	private static final String PRGM_WIDTH = "prgmWidth"; // program width
	private static final String PRGM_HEIGHT = "prgmHeight"; // program height
	// program horizontal location
	private static final String PRGM_X_LOC = "prgmXLoc";
	// program vertical location
	private static final String PRGM_Y_LOC = "prgmYLoc";
	
	
	
	
	
	
	
	/* General preferences */
	private Preferences generalPrefs = prefs.node("General"); // general prefs holder
	private static final int GENERAL_PANEL_INDEX = 0; // tab position
	private CreateGeneralPanel createGeneralPanel = new CreateGeneralPanel();
	
	// Install Directory
	private static final String INSTALL_DIR = "installDir";

	// open tabs left last session
	private static final String REOPEN_TABS = "reopenTabs";
	private JCheckBox reopenTabsChk = null; // check box
	// comma-delimited list of paths
	private static final String REOPEN_TABS_LIST = "reopenTabsList";

	// tab name max characters
	private static final String TAB_NAME_MAX_CHARS = "tabNameMaxChars";
	private JCheckBox tabNameMaxCharsChk = null; // check box
	private static final String TAB_NAME_MAX_CHARS_NUM = "tabNameMaxCharsNum";
	private JSpinner tabNameMaxCharsNumSpinner = null; // spinner
	private SpinnerNumberModel tabNameMaxCharsNumMdl; // numerical input

	// prompt before refreshing tabs
	private static final String REFRESH_PROMPT = "refreshPrompt";
	private JCheckBox refreshPromptChk = null;
	
	// file history
	private static final String FILE_HIST_COUNT = "fileHistCount";
	// input max num of files to remember
	private JSpinner fileHistCountSpinner = null;
	private SpinnerNumberModel fileHistCountMdl; // numerical input
	// comma-delimited list of paths
	private static final String FILE_HIST = "fileHist";

	// auto-auto-indent
	private static final String AUTO_INDENT = "autoIndent";
	private JCheckBox autoIndentChk = null; // check box
	// comma-or-space-delimited, period-independent list of file extensions
	private static final String AUTO_INDENT_EXT = "autoIndentExt";
	private JTextField autoIndentExtFld = null; // input list
	
	
	// auto-save
	private static final String AUTO_SAVE = "autoSave";
	private JCheckBox autoSaveChk = null; // check box
	// interval between document change and auto-save
	private static final String AUTO_SAVE_INTERVAL = "autoSaveInterval";
	private JSpinner autoSaveIntervalSpinner = null; // numerical input
	private SpinnerNumberModel autoSaveIntervalMdl = null;
	// dialog prompt to ask if the user wishes to allow the automatic save
	private static final String AUTO_SAVE_PROMPT = "autoSavePrompt";
	private JCheckBox autoSavePromptChk = null; // check box
	
	
	// highlighting
	private static final String HIGHLIGHTING = "highlighting";
	private JCheckBox highlightingChk = null;
	
	
	// spell-checker
	private static final String SPELL_CHECKER = "spellChecker";
	private JCheckBox spellCheckerChk = null;
	
	
	// display font size
	private static final String FONT_SIZE = "fontSize";
	private JSpinner fontSizeSpinner = null; // numerical input
	private SpinnerNumberModel fontSizeMdl = null;
	
	
	/* Shorts preferences--shortcuts */
	private Preferences shortsPrefs = prefs.node("Shorts"); // shorts-specific prefs holder
	private static final int SHORTS_PANEL_INDEX = 1; // tab index
	private CreateShortsPanel createShortsPanel = new CreateShortsPanel();
	
	// keybinding models
	private static final String KEYBINDINGS = "keybindings"; // model
	private static final String STD_KEYBINDINGS_MDL = "Standard"; // standard style
	// mix of standard and single line and character Emacs
	private static final String HYBRID_KEYBINDINGS_MDL = "Hybrid";
	// typical single key Emacs shortcuts
	private static final String EMACS_KEYBINDINGS_MDL = "Emacs";
	// array of models
	private static final String[] KEYBINDINGS_MDLS =
		{ STD_KEYBINDINGS_MDL, HYBRID_KEYBINDINGS_MDL, EMACS_KEYBINDINGS_MDL };
	private JComboBox keybindingsCombo = null; // combo box to select the model
	
	
	
	
	
	/* Plug-ins preferences */
	private Preferences plugInsPrefs = prefs.node("PlugIns"); // plug-ins-specific prefs holder
	private static final int PLUG_INS_PANEL_INDEX = 2; // tab index
	private CreatePlugInsPanel createPlugInsPanel = new CreatePlugInsPanel();
	
	// include all plug-ins
	private static final String ALL_PLUG_INS = "allPlugIns";
	private JCheckBox allPlugInsChk = null;
	
	// include only specific plug-ins
	private JList includePlugInsList = null; // lists plug-ins to include...
	private JList ignorePlugInsList = null; // ...to exclude
	private String[] plugInsList = null; // master list of all plug-ins; must be updated externally
	private String[] includes = null; // list of the plug-ins to include...
	private String[] ignores = null; // ...to exclude
	private static final String INCLUDE_PLUG_INS = "includePlugIns"; // includes
	private static final String IGNORE_PLUG_INS = "ignorePlugIns"; // excludes
	private static final String PLUG_IN_WIDTH = "plugInWidth_";
	private static final String PLUG_IN_HEIGHT = "plugInHeight_";
	private static final String PLUG_IN_X_LOC = "plugInXLoc_";
	private static final String PLUG_IN_Y_LOC = "plugInYLoc_";

	/** Constructs a preferences interface.
	 * Loads its previous size settings.  Relies on the calling function to
	 * supply an action that generally "okays," or stores, the preferences,
	 * and an action that usually "cancels," or rejects the changes by 
	 * destroying the object.  The calling functin must also 
	 * call <code>show()</code> to display the interface
	 * @param okAction action that generally stores the preferences
	 * @param cancelAction action that generally rejects the changes by
	 * destroying the object
	 */
	public Prefs(JFrame frame, Action okAction, Action applyAction, Action cancelAction) {
		super(frame, "You've Got Options");
		applyPlugInSizeLoc(this, PREFS, 600, 320);
		
		// adds a listener to remember the window size and location
		addComponentListener(new ComponentListener() {
			public void componentMoved(ComponentEvent evt) {
				storePlugInLocation(PREFS, getLocation());
			}

			public void componentResized(ComponentEvent evt) {
				storePlugInSize(PREFS, getWidth(), getHeight());
			}

			public void componentShown(ComponentEvent evt) {
			}

			public void componentHidden(ComponentEvent evt) {
			}
		});

		// sets up the tabbed pane
		tabbedPane.setTabPlacement(JTabbedPane.LEFT);
		tabbedPane.insertTab(
			"General",
			null,
			null,
			"In general...",
			GENERAL_PANEL_INDEX);
		createGeneralPanel.start();
		generalPrefs.put(INSTALL_DIR, LibTTx.getBaseFile().getPath());

		tabbedPane.insertTab(
			"Shorts",
			null,
			null,
			"Shortcuts...",
			SHORTS_PANEL_INDEX);
		createShortsPanel.start();

		tabbedPane.insertTab(
			"Plug-Ins",
			null,
			null,
			"Don't get electricuted...",
			PLUG_INS_PANEL_INDEX);
		// plug-ins panel created while updating plug-ins

		// adds the components to the panel
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;

		// anchor to the top of the panel in case don't have enough optons
		// to fill the tab; otherwise the entire JTabbedPane would become
		// centered, eg?
		constraints.anchor = GridBagConstraints.NORTH;
		LibTTx.addGridBagComponent(
			tabbedPane,
			constraints,
			0,
			0,
			3,
			1,
			100,
			100,
			contentPane);
			
		JButton okBtn = new JButton(okAction);
		constraints.anchor = GridBagConstraints.SOUTH;
		LibTTx.addGridBagComponent(
			okBtn,
			constraints,
			0,
			1,
			1,
			1,
			100,
			0,
			contentPane);

		// immediately stores the preferences in the given tab
		constraints.fill = GridBagConstraints.HORIZONTAL;
		JButton applyBtn = new JButton(applyAction);
		LibTTx.addGridBagComponent(
			applyBtn,
			constraints,
			1,
			1,
			1,
			1,
			100,
			0,
			contentPane);

		// generally used to cancal changes made since the last preferences storage
		JButton cancelBtn = new JButton(cancelAction);
		LibTTx.addGridBagComponent(
			cancelBtn,
			constraints,
			2,
			1,
			1,
			1,
			100,
			0,
			contentPane);
//		resetPrefs();
//		exportPrefs("cleared.xml");
//		importPrefs();
	}
	
	public Prefs() {
	}
	
	/** Gets this Prefs object.
	 */
	private Prefs getThis() {
		return this;
	}
	
	/** Exports the user preferences from Text Trix to an XML
	 * file for backup and import.
	 * Exports the entire subtree.
	 * @param file exports the preferences to this file
	 */
	public void exportPrefs(File file) {
		try {
			OutputStream outTree = 
				new BufferedOutputStream(
					new FileOutputStream(file));
			// exports the entire subtree, not just a node
			prefs.exportSubtree(outTree);
			outTree.close();
//			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BackingStoreException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Imports preferences from an XML file.
	 * Any preferences bearing the same identifiers will be overwritten.
	 * The preferences panel should be updated if the dialog is
	 * currently open.
	 * @param file the file that holds the preferences.
	 * @see #updatePanels
	 */
	public void importPrefs(File file) {
		try {
			InputStream in =
				new BufferedInputStream(
					new FileInputStream(file));
			// imports the preferences
			prefs.importPreferences(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidPreferencesFormatException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Updates all the panels according to the current preferences 
	 * values.
	 */
	public void updatePanels() {
		createGeneralPanel.start();
		createShortsPanel.start();
		createPlugInsPanel.start();
	}
	
	/** Resets the preferences to their default values.
	 * All values are cleared, and the settings will pick up their default
	 * values when this Prefs attempts to locate them.
	 */
	public void resetPrefs() {
		try {
			// gets all the child node names, eg "General",
			// "Shorts", etc.
			String[] childNodes = prefs.childrenNames();
			for (int i = 0; i < childNodes.length; i++) {
				// each node must be cleared individually
				Preferences node = prefs.node(childNodes[i]);
				node.clear();
//				System.out.println("children names: " + chil[i]);
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Sets the component to the stored size and location.
	 * The component is generally a plugin, but can really be
	 * any Swing (@link Component).
	 * @param comp the component
	 * @param name the name to reference the component
	 * @param defaultWidth the width of the component, in pixels
	 * @param defaultHeight the height of the component, in pixels
	 */
	public void applyPlugInSizeLoc(Component comp, String name, 
		int defaultWidth, int defaultHeight) {
		
		int width = getPlugInWidth(name);
		int height = getPlugInHeight(name);
		if (width == 0 || height == 0) {
			comp.setSize(defaultWidth, defaultHeight);
		} else {
			comp.setSize(width, height);
		}
		int xLoc = getPlugInXLoc(name);
		int yLoc = getPlugInYLoc(name);
		if (xLoc != 0 && yLoc != 0) {
			comp.setLocation(new Point(xLoc, yLoc));
		}
	}
	
	/** Updates the plug-ins panel with the new list of plug-ins.
	 * Re-creates the panel.  This list must be updated or else the panel will
	 * have no idea which plug-ins are loaded.
	 * @param list list of all loaded plug-ins, regardless of whether included
	 * or ignored
	 */
	public void updatePlugInsPanel(String[] list) {
		setPlugInsList(list);
		updatePlugInsList(list);
		createPlugInsPanel.start();
	}
	
	/** Updates the list of plug-ins.
	 * 
	 * @param list list of all loaded plug-ins, regardless of whether included
	 * or ignored
	 */
	private void updatePlugInsList(String[] list) {
		if (list == null) return;
		includes = LibTTx.createArrayFromString(getIncludePlugIns());
		ignores = LibTTx.createArrayFromString(getIgnorePlugIns());
		String[] updatedIncludes = new String[list.length];
		int updatedInclInd = 0;
		String[] updatedIgnores = new String[list.length];
		int updatedIgnInd = 0;
		// runs through the list of loaded plug-ins and adds those previously
		// ignored to the "ignores" list and adding all the rest to the "includes"
		// list; any plug-ins previously in the lists but no longer loaded are not
		// included in either list
		for (int i = 0; i < list.length; i++) {
			if (LibTTx.inUnsortedList(list[i], ignores)) {
				updatedIgnores[updatedIgnInd++] = list[i];
			} else {
				updatedIncludes[updatedInclInd++] = list[i];
			}
		}
		includes = updatedIncludes;
		ignores = updatedIgnores;
	}

	/** Front-end to storing all the preferences at once.
	 * Includes the panel-specific preferences.
	 *
	 */
	public void storePrefs() {
		//storeInternalPrefs();
		storeGeneralPrefs();
		storeShortsPrefs();
		storePlugInsPrefs();
	}

	/** Stores the program's internal preferences.
	 * Does not store program size or location information, but does store
	 * the panel's size.
	 *
	 *
	public void storeInternalPrefs() {
		internalPrefs.putInt(PREFS_WIDTH, getWidth());
		internalPrefs.putInt(PREFS_HEIGHT, getHeight());
	}
	*/

	/** Stores the General preferences.
	 * General preferences include settings that affect Text Trix as a whole.
	 *
	 */
	public void storeGeneralPrefs() {
		generalPrefs.put(INSTALL_DIR, LibTTx.getBaseFile().getPath());
		generalPrefs.putBoolean(REOPEN_TABS, reopenTabsChk.isSelected());
		generalPrefs.putBoolean(TAB_NAME_MAX_CHARS, 
			tabNameMaxCharsChk.isSelected());
		generalPrefs.putInt(TAB_NAME_MAX_CHARS_NUM, 
			tabNameMaxCharsNumMdl.getNumber().intValue());
		generalPrefs.putBoolean(REFRESH_PROMPT, refreshPromptChk.isSelected());
		storeFileHistCount(fileHistCountMdl.getNumber().intValue());
		generalPrefs.putBoolean(AUTO_INDENT, autoIndentChk.isSelected());
		generalPrefs.put(AUTO_INDENT_EXT, autoIndentExtFld.getText());
		generalPrefs.putBoolean(AUTO_SAVE, autoSaveChk.isSelected());
		generalPrefs.putInt(AUTO_SAVE_INTERVAL, 
			autoSaveIntervalMdl.getNumber().intValue());
		generalPrefs.putBoolean(AUTO_SAVE_PROMPT, autoSavePromptChk.isSelected());
		generalPrefs.putBoolean(HIGHLIGHTING, highlightingChk.isSelected());
		generalPrefs.putBoolean(SPELL_CHECKER, spellCheckerChk.isSelected());
		generalPrefs.putInt(FONT_SIZE, fontSizeMdl.getNumber().intValue());
	}
	
	/** Stores the Shorts preferences.
	 * Shorts preferences are those referring to shortcuts settings.
	 *
	 */
	public void storeShortsPrefs() {
		shortsPrefs.put(
			KEYBINDINGS,
			(String) keybindingsCombo.getSelectedItem());
	}
	
	/** Stores the PlugIns preferences.
	 * Plug-ins preferences manage which plug-ins are loaded.
	 *
	 */
	public void storePlugInsPrefs() {
		plugInsPrefs.putBoolean(ALL_PLUG_INS, allPlugInsChk.isSelected());
		plugInsPrefs.put(INCLUDE_PLUG_INS, getListAsString(includePlugInsList));
		plugInsPrefs.put(IGNORE_PLUG_INS, getListAsString(ignorePlugInsList));
	}
	
	/** Gets the contents of a <code>JList</code> as a comma-delimited string.
	 * Many Text Trix preferences for multiple but an unknown number of settings.
	 * @param list list with contents to be recorded as a <code>String</code>
	 * @return the string
	 */
	public String getListAsString(JList list) {
		String s = "";
		DefaultListModel mdl = (DefaultListModel) list.getModel();
		Object[] elts = mdl.toArray();
		for (int i = 0; i < elts.length; i++) {
			// add a comma only internally in the list
			if (i == 0) {
				s = (String) elts[i];
			} else {
				s = s + "," + (String) elts[i];
			}
		}
		return s;
	}

	/** Stores the program window size.
	 * Size values correspond to <code>java.awt.window.getWidth()</code>
	 * and similar functions' return values
	 * @param width program window width
	 * @param height program window height
	 */
	public void storeSize(int width, int height) {
		internalPrefs.putInt(PRGM_WIDTH, width);
		internalPrefs.putInt(PRGM_HEIGHT, height);
	}

	/** Stores the program's window location.
	 * 
	 * @param p upper-left-hand corner of window relative to the upper-left-hand
	 * corner of the screen
	 */
	public void storeLocation(Point p) {
		internalPrefs.putInt(PRGM_X_LOC, (int) p.getX());
		internalPrefs.putInt(PRGM_Y_LOC, (int) p.getY());
	}
	
	/**Stores the given plug-in's window size.
	 * The storage parameter's name includes the plug-in's <code>.jar</code>
	 * name.
	 * 
	 * @param filename the <code>.jar</code> name of the plug-in
	 * @param width the width of the plug-in's window, in pixels
	 * @param height the height of the plug-in's window, in pixesl
	 */
	public void storePlugInSize(String filename, int width, int height) {
		plugInsPrefs.putInt(PLUG_IN_WIDTH + filename, width);
		plugInsPrefs.putInt(PLUG_IN_HEIGHT + filename, height);
	}
	
	/**Stores the given plug-in's window location.
	 * The storage parameter's name includes the plug-in's <code>.jar</code>
	 * filname
	 * 
	 * @param filename the <code>.jar</code> name of the plug-in
	 * @param p the distance from the upper left corner of the display,
	 * in pixels
	 */
	public void storePlugInLocation(String filename, Point p) {
		plugInsPrefs.putInt(PLUG_IN_X_LOC + filename, (int) p.getX());
		plugInsPrefs.putInt(PLUG_IN_Y_LOC + filename, (int) p.getY());
	}
	
	/** Stores a list of files left open at exit.
	 * The list consists of comma-delimited paths, with no spaces 
	 * in-between paths.
	 * @param paths list of paths
	 */
	public void storeReopenTabsList(String paths) {
		generalPrefs.put(REOPEN_TABS_LIST, paths);
	}

	/** Stores the number of recently opened files to remember quick re-opening.
	 * If the new count is less than the previous one, the extra entries are removed.
	 * @param newCount updated number files to remember
	 */
	public void storeFileHistCount(int newCount) {
		int oldCount = generalPrefs.getInt(FILE_HIST_COUNT, newCount);
		if (newCount < oldCount) {
			for (int i = newCount; i < oldCount; i++) {
				System.out.println(
					"Removing file history entry: "
						+ generalPrefs.get(FILE_HIST + i, ""));
				generalPrefs.remove(FILE_HIST + i);
			}
		}
		generalPrefs.putInt(FILE_HIST_COUNT, newCount);
	}

	/** Stores a history of the most recently opened files.
	 * The earlier the file in the history, the more recently the file
	 * was opened.  When a file is added that already exists in
	 * the list, the file is simply shifted to the head of the list
	 * rather than duplicated.
	 * @param file path of file to add to list
	 */
	public void storeFileHist(String file) {
		String[] files = retrieveFileHist(); // gets the current history array
		boolean shift = false;
		// shift files if moving one entry to top of list
		// shifts the records as necessary to move a potential
		// duplicate to the front of the history;
		// traverses backward through the list since the entries are shifted
		// in the opposite direction, meaning that each record can be
		// checked before replacing it with a future entry
		for (int i = files.length - 1; i >= 0; i--) {
			if (shift) { // shift the records, overwriting the duplicate record
				// move the current record to the next spot
				files[i + 1] = files[i];
			} else if (files[i].equals(file)) {
				// find where to start shifting, if necessary
				shift = true;
			}
		}
		// if already shifted files, regenerate the stored list
		if (shift) {
			// no need to check that below fileHistCount since merely substituting an entry
			files[0] = file;
			int i = 0;
			for (i = 0; i < files.length; i++) {
				generalPrefs.put(FILE_HIST + i, files[i]);
			}
		} else { // shift all records back, discarding the last one if no more room
			int fileHistCount = getFileHistCount();
			for (int i = fileHistCount - 1; i > 0; i--) {
				generalPrefs.put(
					FILE_HIST + i,
					generalPrefs.get(FILE_HIST + (i - 1), ""));
			}
			generalPrefs.put(FILE_HIST + 0, file); // add new entry to head
		}

	}

	/** Retrieves the file history as an array.
	 * 
	 * @return array of paths from each file history entry, where every element
	 * of the array is occupied by a file path
	 */
	public String[] retrieveFileHist() {
		int fileHistCount = getFileHistCount();
		String[] files = new String[fileHistCount];
		for (int i = 0; i < files.length; i++) {
			files[i] = generalPrefs.get(FILE_HIST + i, "");
			if (files[i] == "") {
				// ensure that every array element is occuped by a path
				return (String[]) LibTTx.truncateArray(files, i);
			}
		}
		return files;
	}

	/** Checks whether the the given preferences are the set of
	 * "General" preferences, or holistic settings for Text Trix.
	 * @param p
	 * @return <code>true</code> if the preferences are the set of
	 * "General" ones
	 */
	public boolean isGeneralPrefs(Preferences p) {
		return p == generalPrefs;
	}

	/** Checks whether the the given preferences are the set of
	 * Shorts preferences, or shortcut settings for Text Trix.
	 * @param p
	 * @return <code>true</code> if the preferences are the set of
	 * Shorts ones
	 */
	public boolean isShortsPrefs(Preferences p) {
		return p == shortsPrefs;
	}

	/** Checks whether the keybindings are set for the "Standard" shortcuts,
	 * or those in applications on most desktop systems.
	 * @return <code>true</code> if the keybindings are the set of
	 * "Standard" ones
	 */
	public boolean isStandardKeybindings() {
		return getKeybindings().equals(STD_KEYBINDINGS_MDL);
	}

	/** Checks whether the keybindings are set for the "Hybrid" shortcuts,
	 * or a mesh of "Standard" shortcuts and those of Emacs single character
	 * and line navigation, where the Emacs shortcuts take precedence.
	 * @return <code>true</code> if the keybindings are the set of
	 * "Hybrid" ones
	 */
	public boolean isHybridKeybindings() {
		return getKeybindings().equals(HYBRID_KEYBINDINGS_MDL);
	}

	/** Checks whether the keybindings are set for the "Emacs" shortcuts,
	 * or typical Emacs single key shortcuts layered on top of the "Hybrid" shortcuts. 
	 * @return <code>true</code> if the keybindings are the set of
	 * "Emacs" ones
	 */
	public boolean isEmacsKeybindings() {
		return getKeybindings().equals(EMACS_KEYBINDINGS_MDL);
	}

	/** Sets the list of plug-ins to the given list to display in the plug-ins
	 * choice box.
	 * 
	 * @param s array of plug-ins
	 */
	public void setPlugInsList(String[] s) {
		plugInsList = s;
	}
	
	/** Gets the stored width for the program.
	 * 
	 * @return width in pixels
	 */
	public int getPrgmWidth() {
		return internalPrefs.getInt(PRGM_WIDTH, 500);
	}
	/** Gets the stored height for the program.
	 * 
	 * @return height in pixels
	 */
	public int getPrgmHeight() {
		return internalPrefs.getInt(PRGM_HEIGHT, 600);
	}
	/** Gets the stored horizontal location for the program.
	 * 
	 * @return horizontal location in pixels of upper-left corner of the 
	 * program window, relative to the upper-left corner of the screen
	 */
	public int getPrgmXLoc() {
		return internalPrefs.getInt(PRGM_X_LOC, 0);
	}
	/** Gets the stored vertical location for the program.
	 * 
	 * @return vertical location in pixels of upper-left corner of the 
	 * program window, relative to the upper-left corner of the screen
	 */
	public int getPrgmYLoc() {
		return internalPrefs.getInt(PRGM_Y_LOC, 0);
	}
	/** Gets the stored width of the plug-in's window.
	 * 
	 * @param filename the <code>.jar</code> filename of the plug-in
	 * @return width in pixels
	 */
	public int getPlugInWidth(String filename) {
		return plugInsPrefs.getInt(PLUG_IN_WIDTH + filename, 0);
	}
	/** Gets the stored height of the plug-in's window.
	 * 
	 * @param filename the <code>.jar</code> filename of the plug-in
	 * @return height in pixels
	 */
	public int getPlugInHeight(String filename) {
		return plugInsPrefs.getInt(PLUG_IN_HEIGHT + filename, 0);
	}
	/** Gets the stored horizontal location of the plug-in's window.
	 * 
	 * @param filename the <code>.jar</code> filename of the plug-in
	 * @return horizontal location in pixels of upper-left corner of the 
	 * program window, relative to the upper-left corner of the screen
	 */
	public int getPlugInXLoc(String filename) {
		return plugInsPrefs.getInt(PLUG_IN_X_LOC + filename, 0);
	}
	/** Gets the stored vertical location of the plug-in's window.
	 * 
	 * @param filename the <code>.jar</code> filename of the plug-in
	 * @return vertical  location in pixels of upper-left corner of the 
	 * program window, relative to the upper-left corner of the screen
	 */
	public int getPlugInYLoc(String filename) {
		return plugInsPrefs.getInt(PLUG_IN_Y_LOC + filename, 0);
	}
	
	/** Gets the location of the installation directory for the latest
	 * installation of Text Trix.
	 * @return path to the latest Text Trix install directory
	 */
	public String getInstallDir() {
		return generalPrefs.get(INSTALL_DIR, "");
	}
	
	
	
	/** Gets the stored flag for re-opening tabs.
	 * 
	 * @return width If <code>true</code>, the pogram will try
	 * to open all the files left open in the program during its last session
	 * @see #getReopenTabsList()
	 */
	public boolean getReopenTabs() {
		return generalPrefs.getBoolean(REOPEN_TABS, true);
	}
	/** Gets the stored list of last-opened tabs.
	 * 
	 * @return comma-delimited list of file paths
	 * @see #getReopenTabs()
	 */
	public String getReopenTabsList() {
		return generalPrefs.get(REOPEN_TABS_LIST, "");
	}
	/**Gets the stored flag for whether the program should cap the number of
	 * characters displayed in tab names.
	 * 
	 * @return <code>true</code> if the program should apply a max number of 
	 * characters in tab names
	 */
	public boolean getTabNameMaxChars() {
		return generalPrefs.getBoolean(TAB_NAME_MAX_CHARS, false);
	}
	/**Gets the max number of characters to display in tab names.
	 * 
	 * @return max number of characters
	 */
	public int getTabNameMaxCharsNum() {
		return generalPrefs.getInt(TAB_NAME_MAX_CHARS_NUM, 5);
	}
	/** Gets the stored flag for prompting before freshing tabs.
	 * 
	 * @return true to prompt before refreshing tabs
	 */
	public boolean getRefreshPrompt() {
		return generalPrefs.getBoolean(REFRESH_PROMPT, true);
	}
	/** Gets the stored number of files paths to remember.
	 * 
	 * @return the number of paths to store in the most-recently-opened-files
	 * history
	 */
	public int getFileHistCount() {
		return generalPrefs.getInt(FILE_HIST_COUNT, 7);
	}
	/** Gets the stored flag for automatically turning on auto-indent.
	 * 
	 * @return If <code>true</code>, the program will automatically
	 * auto-indent files with particular extensions when opening or saving them
	 * @see #getAutoIndentExt()
	 */
	public boolean getAutoIndent() {
		return generalPrefs.getBoolean(AUTO_INDENT, true);
	}
	/** Gets the stored list of file extensions of files to automatically
	 * turn on auto-indent for.
	 * 
	 * @return comma-or-space-delimited list of file extensions, where the
	 * period is assumed and optional
	 */
	public String getAutoIndentExt() {
		return generalPrefs.get(AUTO_INDENT_EXT, 	
				TextPad.collectHighlightExtDotList());
	}
	/**Gets the stored flag for whether the windows should become active 
	 * together.
	 * 
	 * @return <code>true</code> if activating one window should bring
	 * all the other Text Trix windows to the foreground
	 *
	public boolean getActivateWindowsTogether() {
		return generalPrefs.getBoolean(ACTIVATE_WINDOWS_TOGETHER, true);
	}
	*/
	
	/**Gets the stored flag for whether the program should automatically
	 * save changed files.
	 * 
	 * @return <code>true</code> if the program should auto-save files
	 */
	public boolean getAutoSave() {
		return generalPrefs.getBoolean(AUTO_SAVE, true);
	}
	/**Gets the stored interval between the first change in a file and
	 * its automatic save.
	 * 
	 * @return the duration in minutes
	 */
	public int getAutoSaveInterval() {
		return generalPrefs.getInt(AUTO_SAVE_INTERVAL, 5);
	}
	/**Gets the flag for whether the program should prompt the user for
	 * save confirmation before the program automatically saves the file.
	 * 
	 * @return <code>true</code> if the program should prompt the user
	 */
	public boolean getAutoSavePrompt() {
		return generalPrefs.getBoolean(AUTO_SAVE_PROMPT, false);
	}
	
	public boolean getHighlighting() {
//		System.out.println("highlighting pref: " + generalPrefs.getBoolean(HIGHLIGHTING, true));
		return generalPrefs.getBoolean(HIGHLIGHTING, true);
	}
	
	public boolean getSpellChecker() {
		return generalPrefs.getBoolean(SPELL_CHECKER, true);
	}
	
	public int getFontSize() {
		return generalPrefs.getInt(FONT_SIZE, 11);
	}
	
	
	
	
	/** Gets the stored keybindings preference.
	 * 
	 * @return the keybindings preference
	 */
	public String getKeybindings() {
		return shortsPrefs.get(KEYBINDINGS, "");
	}
	/** Gets the preference to include all the plug-ins.
	 * 
	 * @return <code>true</code> if all the plug-ins should be included
	 */
	public boolean getAllPlugIns() {
		return plugInsPrefs.getBoolean(ALL_PLUG_INS, true);
	}
	/** Gets the list of included plug-ins.
	 * @return list of plug-ins to include
	 */
	public String getIncludePlugIns() {
		return plugInsPrefs.get(INCLUDE_PLUG_INS, "");
	}
	/** Gets the stored list of plug-ins to include.
	 * 
	 * @return the list of plug-ins to make usable in Text Trix
	 */
	public String[] getIncludePlugInsList() {
		return includes;
	}
	/** Gets the stored list of plug-ins to ignore.
	 * The plug-ins will still be loaded, but not used.
	 * 
	 * @return the list of plug-ins to ignore in Text Trix
	 */
	public String getIgnorePlugIns() {
		return plugInsPrefs.get(IGNORE_PLUG_INS, "");
	}
	
	/** Worker thread to create the preferences panel.
	 * 
	 * @author davit
	 */
	private class CreateGeneralPanel implements Runnable {
		
		private JLabel tabNameMaxCharsNumLbl = null;
		private JLabel autoSaveIntervalLbl = null;
		private JLabel fontSizeLbl = null;

		/** Starts the thread.
		 * 
		 */
		public void start() {
			(new Thread(this, "thread")).start();
		}
		

		/** Makes a panel for the "General" preferences tab.
		 * Displays options related to the program as a whole.
		 * @return the panel
		 */
		public void run() {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.fill = GridBagConstraints.HORIZONTAL;
					constraints.anchor = GridBagConstraints.NORTH;
					JPanel panel = new JPanel();
					panel.setLayout(new GridBagLayout());

					// re-opens the tabs left open during the previous session
					String reopenTabsTxt = "Reopen tabs from last session";
					String reopenTabsTipTxt =
						"<html>Reopen the files left open the last time<br> you used the program.</html>";
					reopenTabsChk =
						new JCheckBox(reopenTabsTxt, getReopenTabs());
					reopenTabsChk.setToolTipText(reopenTabsTipTxt);

					// keeps the given number of last-opened files in memory for quick re-opening
					JLabel fileHistCountLbl =
						new JLabel("Number of files in history area:");
					String fileHistCountTipTxt =
						"<html>The number of files to include in the File menu"
							+ "<br>history area to quickly open recent files.</html>";
					fileHistCountLbl.setToolTipText(fileHistCountTipTxt);
					fileHistCountMdl =
						new SpinnerNumberModel(getFileHistCount(), 0, 100, 1);
					fileHistCountSpinner = new JSpinner(fileHistCountMdl);

					// max number of characters to display in tab names
					String tabNameMaxCharsTxt =
						"Cap tab name length";
					String tabNameMaxCharsTipTxt =
						"<html>Limit the number of characters displayed in tab"
							+ "<br>names to pack in more tabs in each row."
							+ "</html>";
					tabNameMaxCharsChk = 
						new JCheckBox(tabNameMaxCharsTxt, getTabNameMaxChars());
					tabNameMaxCharsChk.setToolTipText(tabNameMaxCharsTipTxt);
					tabNameMaxCharsChk.addChangeListener(new ChangeListener() {
						public void stateChanged(ChangeEvent e) {
							setTabNameMaxCharsSelectors(
								tabNameMaxCharsChk.isSelected());
						}
					});
					
					// max characters selector
					tabNameMaxCharsNumLbl = 
						new JLabel("Max characters:");
					String tabNameMaxCharsNumTipTxt =
						"<html>The max number of characters to display per tab"
						+ "<br>title.</html>";
					tabNameMaxCharsNumLbl.setToolTipText(
						tabNameMaxCharsNumTipTxt);
					// the numerical model for the spinner
					tabNameMaxCharsNumMdl =
						new SpinnerNumberModel(getTabNameMaxCharsNum(), 1, 100, 1);
					tabNameMaxCharsNumSpinner = new JSpinner(tabNameMaxCharsNumMdl);
					
					
					
					// prompt before refreshing tabs
					String refreshPromptTxt = "Prompt before refreshing tabs";
					String refreshPromptTipTxt =
						"<html>When a file is changed in the background, prompt"
						+ "<br>before refreshing the tab.</html>";
					refreshPromptChk =
						new JCheckBox(refreshPromptTxt, getRefreshPrompt());
					refreshPromptChk.setToolTipText(refreshPromptTipTxt);
					
					
					
					// automatically auto-indents files whose extensions match any of those in
					// the customizable list
					String autoIndentTxt = "Auto-wrap-indent file types:";
					String autoIndentTipTxt =
						"<html>Auto-wrap-indent files with the following extensions."
							+ "<br>Including the period and space is optional,"
							+ "<br>but be sure to separate each extension with a comma.</html>";
					autoIndentChk =
						new JCheckBox(autoIndentTxt, getAutoIndent());
					autoIndentChk.setToolTipText(autoIndentTipTxt);
					// default list of extension
					String autoIndentExt = getAutoIndentExt();
					/*
					if (autoIndentExt == null || autoIndentExt.equals("")) {
						autoIndentExt = 
							".java, .c, .cpp, .html, .htm, .css, .shtml, .xhtml, .xml, .sh";
					}
					*/
					autoIndentExtFld = new JTextField(autoIndentExt, 100);
					
					
					// auto-saves files
					
					// auto-save check box;
					// selecting this box enables the user to alter the other
					// auto-save parameters 
					String autoSaveTxt =
						"Save changed files automatically:";
					String autoSaveTipTxt =
						"<html>Saves files automatically after the specified"
							+ "<br>interval.  To prevent unwanted changes from"
							+ "<br>being saved, the user can choose to be"
							+ "<br>prompted before any saves.</html>";
					autoSaveChk = 
						new JCheckBox(autoSaveTxt, getAutoSave());
					autoSaveChk.setToolTipText(autoSaveTipTxt);
					autoSaveChk.addChangeListener(new ChangeListener() {
						public void stateChanged(ChangeEvent e) {
							setAutoSaveSelectors(autoSaveChk.isSelected());
						}
					});
					
					// the time interval selector to 
					autoSaveIntervalLbl = 
						new JLabel("Minutes between auto saves:");
					String autoSaveIntervalTipTxt =
						"<html>The number of minutes between first changing"
						+ "<br>changing a file and automatically"
						+ "<br>saving it.  If the file is manually"
						+ "<br>saved, the interval timer is reset.</html>";
					autoSaveIntervalLbl.setToolTipText(autoSaveIntervalTipTxt);
					// the numerical model for the spinner
					autoSaveIntervalMdl =
						new SpinnerNumberModel(getAutoSaveInterval(), 1, 100, 1);
					autoSaveIntervalSpinner = new JSpinner(autoSaveIntervalMdl);
					
					// the prompt for confirmation from the user before automatically
					// saving a file
					String autoSavePromptTxt =
						"Prompt";
					String autoSavePromptTipTxt =
						"<html>Asks the user whether to save any changes before"
						+ "<br>automatically saving them.  Otherwise, the"
						+ "<br>current file will be overwritten, and the original"
						+ "<br>file will be no more.</html>";
					autoSavePromptChk = 
						new JCheckBox(autoSavePromptTxt, getAutoSavePrompt());
					autoSavePromptChk.setToolTipText(autoSavePromptTipTxt);
					
					// enables the auto-save settings if the main auto-save
					// function is active
					setAutoSaveSelectors(autoSaveChk.isSelected());
					
					
					// syntax highlighting
					String highlightingTxt =
						"Highlight syntax source code";
					String highlightingTipTxt =
						"<html>Highlights syntax in source code files."
							+ "<br>Syntax will be colored on the fly."
							+ "<br>Uncheck this box to improve performance.</html>";
					highlightingChk = 
						new JCheckBox(highlightingTxt, getHighlighting());
					highlightingChk.setToolTipText(highlightingTipTxt);
					
					// spell-checker
					String spellCheckerTxt =
						"Automatically spell-check text";
					String spellCheckerTipTxt =
						"<html>Underlines misspelled words."
							+ "<br>Words can be corrected by right-clicking"
							+ "<br>and choosing the correct spelling.</html>";
					spellCheckerChk = 
						new JCheckBox(spellCheckerTxt, getSpellChecker());
					spellCheckerChk.setToolTipText(spellCheckerTipTxt);
					
					// display font size
					fontSizeLbl = 
						new JLabel("Font size:");
					String fontSizeTipTxt =
						"<html>Default font size for typed text.</html>";
					fontSizeLbl.setToolTipText(fontSizeTipTxt);
					// the numerical model for the spinner
					fontSizeMdl =
						new SpinnerNumberModel(getFontSize(), 4, 24, 1);
					fontSizeSpinner = new JSpinner(fontSizeMdl);
					
					
					
					
					/* Import/Export */
					
					// Label for import/export operations
					JLabel importExportPrefsLbl = new JLabel("Import/Export Preferences:");
					String importExportPrefsTipTxt = 
							"<html>Imports or exports a set of preferences from"
							+ "<br>an XMLfile.  All preferences can also be"
							+ "<br>erased and set to default parameters using"
							+ "<br>the reset button.</html>";
					importExportPrefsLbl.setToolTipText(importExportPrefsTipTxt);
					
					// file browser for locating an import/export file name and location
					final JFileChooser chooser = new JFileChooser();
					// default, all-files filter
					FileFilter allFilter = chooser.getFileFilter();
					// filter for ".xml" files
					ExtensionFileFilter xmlFilter = new ExtensionFileFilter();
					xmlFilter.addExtension("xml");
					xmlFilter.setDescription("XML files (*.xml)");
					chooser.setFileFilter(xmlFilter);
					
					
					// Export preferences
					
					// creates a single-file browser at the user's root directory
					// TODO: default to the program's root directory
					Action exportPrefsAction = new BrowseSingleFile(getThis(), "Export", 
						LibTTx.makeIcon("images/arrow-up.png"), new File("/"), 
						new File(FILE_PREFS_USER_TREE), chooser) {
						
						/** Adds to the action by adjusting the approve button
						 * text applying the prefs export to the given file.
						 */
						public void actionPerformed(ActionEvent evt) {
							chooser.setApproveButtonText("Export");
							this.setSaveDialog(true);
							super.actionPerformed(evt);
							File file = this.getSelectedFile();
							// exports the prefs if user selects a file; the file must actually
							// be selected, not merely given as the default file and loc
							if (file != null) {
								exportPrefs(file);
//								System.out.println("file: " + file.getPath());
							}
						}
					};
					// default tool tip text
					LibTTx.setAction(exportPrefsAction, "Exports preferences from XML file", 'I');
					// button for the panel
					JButton exportPrefsButton = new JButton(exportPrefsAction);
					// new tool tip text
					exportPrefsButton.setToolTipText("<html>Exports the last applied preferences"
						+ "<br>to an XML file.  The last applied settings are saved,"
						+ "<br>so Apply Now should be pressed to save any"
						+ "<br>unapplied settings.</html>>");
					
					
					
					// Imports preferences
					
					// creates a single-file browser at the user's root directory
					// TODO: default to the program's root directory
					File importPrefsDefaultFile =  new File(FILE_PREFS_USER_TREE);
					if (!importPrefsDefaultFile.exists()) importPrefsDefaultFile = new File("");
					Action importPrefsAction = new BrowseSingleFile(getThis(), "Import", 
						LibTTx.makeIcon("images/arrow-down.png"), new File("/"), 
						importPrefsDefaultFile, chooser) {
						
						/** Adds to the action by adjusting the approve button
						 * text applying the prefs import from the given file.
						 */
						public void actionPerformed(ActionEvent evt) {
							chooser.setApproveButtonText("Import");
							super.actionPerformed(evt);
							File file = this.getSelectedFile();
							// checks whether user selected a file
							if (file != null && file.exists()) {
								// verifies import with user before erasing all the current 
								// user settings
								if (LibTTx.yesNoDialog(
										getThis(),
										"This will completely replace your current settings with."
											+ "\nthose from the selected file.  Are you sure?",
										"Import preferences?")) {
									importPrefs(file);
									updatePanels();
//									System.out.println("file: " + file.getPath());
								}
							}
						}
					};
					// tool tip text
					LibTTx.setAction(importPrefsAction, "Imports preferences from XML file", 'I');
					// button for the panel
					JButton importPrefsButton = new JButton(importPrefsAction);
					
					
					
					// resets preferences
					
					// resets all user preferences to their default values
					Action resetPrefsAction = new AbstractAction("Reset") {
						
						/** Verifies with the user before resetting all the values.
						 */
						public void actionPerformed(ActionEvent evt) {
							if (LibTTx.yesNoDialog(
									getThis(),
									"This will completely clear your preferences for Text Trix."
										+ "\nand restore all factory settings.  Are you sure?",
									"Reset all preferences?")) {
								resetPrefs();
							}
						}
					};
					// tool tip text
					LibTTx.setAction(resetPrefsAction, "Resets all user preferences", 'I');
					// button for the panel
					JButton resetPrefsButton = new JButton(resetPrefsAction);
					
					
					
					
					// add components to a grid-bag layout
					panel.setLayout(new GridBagLayout());
					int row = 0;
					LibTTx.addGridBagComponent(
						reopenTabsChk,
						constraints,
						0,
						row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						tabNameMaxCharsChk,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						tabNameMaxCharsNumLbl,
						constraints,
						0,
						++row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						tabNameMaxCharsNumSpinner,
						constraints,
						1,
						row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						refreshPromptChk,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						fileHistCountLbl,
						constraints,
						0,
						++row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						fileHistCountSpinner,
						constraints,
						2,
						row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoIndentChk,
						constraints,
						0,
						++row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoIndentExtFld,
						constraints,
						1,
						row,
						2,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoSaveChk,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoSaveIntervalLbl,
						constraints,
						0,
						++row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoSaveIntervalSpinner,
						constraints,
						1,
						row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						autoSavePromptChk,
						constraints,
						2,
						row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						highlightingChk,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						spellCheckerChk,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						fontSizeLbl,
						constraints,
						0,
						++row,
						1,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						fontSizeSpinner,
						constraints,
						1,
						row,
						1,
						1,
						0,
						0,
						panel);
						
					
					// Export/Import Preferences
					
					LibTTx.addGridBagComponent(
						importExportPrefsLbl,
						constraints,
						0,
						++row,
						3,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						exportPrefsButton,
						constraints,
						0,
						++row,
						1,
						1,
						100,
						0,
						panel);
					tabbedPane.setComponentAt(GENERAL_PANEL_INDEX, panel);
					LibTTx.addGridBagComponent(
						importPrefsButton,
						constraints,
						1,
						row,
						1,
						1,
						100,
						0,
						panel);
					LibTTx.addGridBagComponent(
						resetPrefsButton,
						constraints,
						2,
						row,
						1,
						1,
						100,
						100,
						panel);
					
					// adds the panel to the tabbed pane
					tabbedPane.setComponentAt(GENERAL_PANEL_INDEX, panel);
				}
			});
		}
		
		/**Enables or disables the auto-save selectors.
		 * 
		 * @param b <code>true</code> to enable the selectors,
		 * allowing the user to adjust them
		 */
		public void setAutoSaveSelectors(boolean b) {
			autoSaveIntervalLbl.setEnabled(b);
			autoSaveIntervalSpinner.setEnabled(b);
			autoSavePromptChk.setEnabled(b);
			
		}
		
		/**Enables or disables the tab name max characters selectors.
		 * 
		 * @param b <code>true</code> to enable the selectors,
		 * allowing the user to adjust them
		 */
		public void setTabNameMaxCharsSelectors(boolean b) {
			tabNameMaxCharsNumLbl.setEnabled(b);
			tabNameMaxCharsNumSpinner.setEnabled(b);
			
		}
	}

	/** Worker thread to create the "Shorts" panel of shortcuts options.
	 * 
	 * @author davit
	 */
	private class CreateShortsPanel implements Runnable {//extends Thread {
		
		/** Starts the thread.
		 * 
		 */
		public void start() {
			(new Thread(this, "thread")).start();
		}

		/** Creates the panel.
		 * 
		 */
		public void run() {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.fill = GridBagConstraints.HORIZONTAL;
					constraints.anchor = GridBagConstraints.NORTH;
					JPanel panel = new JPanel();
					panel.setLayout(new GridBagLayout());

					// Keybindings models selector
					JLabel keybindingsLbl = new JLabel("Keybindings model");
					String keybindingsTipTxt =
						"<html>\'Standard\' shortcuts are the ones found on most desktops."
							+ "<br>\'Hybrid\' refers to Emacs shortcuts for all single line and character navigation."
							+ "<br>\'Emacs\' adds more single-key Emacs shortcuts for hard-core Emacs fans."
							+ "<br>See <i>Help &gt; Shortcuts</i> for a table of the keybindings.</html>";
					keybindingsLbl.setToolTipText(keybindingsTipTxt);
					keybindingsCombo = new JComboBox(KEYBINDINGS_MDLS);
					keybindingsCombo.setSelectedItem(getKeybindings());

					// Add the components to the panel
					LibTTx.addGridBagComponent(
						keybindingsLbl,
						constraints,
						0,
						0,
						1,
						1,
						100,
						100,
						panel);
					LibTTx.addGridBagComponent(
						keybindingsCombo,
						constraints,
						1,
						0,
						1,
						1,
						100,
						0,
						panel);

					tabbedPane.setComponentAt(SHORTS_PANEL_INDEX, panel);
				}
			});

		}
	}

	/** Worker thread to create the "PlugIns" panel of plug-in options.
	 * 
	 * @author davit
	 */
	private class CreatePlugInsPanel implements Runnable {//extends Thread {

		JButton moveToIgnoresBtn = null;
		JButton moveToIncludesBtn = null;
		JLabel plugInsSelectionLbl = new JLabel("Which plug-ins do you want?");

		/** Starts the thread.
		 * 
		 */
		public void start() {
			(new Thread(this, "thread")).start();
		}

		/** Creates the panel.
		 * 
		 */
		public void run() {

			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JPanel panel = new JPanel();
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.fill = GridBagConstraints.HORIZONTAL;
					constraints.anchor = GridBagConstraints.NORTH;
					panel.setLayout(new GridBagLayout());
					

					// Option to include all the plug-ins, which disables the plug-in
					// selection components
					String allPlugInsTxt = "Include all the plug-ins";
					allPlugInsChk =
						new JCheckBox(allPlugInsTxt, getAllPlugIns());
					allPlugInsChk.addChangeListener(new ChangeListener() {
						public void stateChanged(ChangeEvent evet) {
							setPlugInsSelectorsEnabled(
								!allPlugInsChk.isSelected());
						}
					});

					// Plug-in selectors

					// list of plug-ins to include
					DefaultListModel includeListModel = new DefaultListModel();
					includePlugInsList = createList(includeListModel, includes);
					JScrollPane includeListPane =
						new JScrollPane(includePlugInsList);
					includeListPane.setPreferredSize(new Dimension(250, 80));

					// list of plug-ins to ignore
					DefaultListModel ignoreListModel = new DefaultListModel();
					ignorePlugInsList = createList(ignoreListModel, ignores);
					JScrollPane ignoreListPane =
						new JScrollPane(ignorePlugInsList);
					ignoreListPane.setPreferredSize(new Dimension(250, 80));

					// button to ignore the selected plug-ins from the list of included plug-ins
					String moveToIgnoresTxt = "Ignore";
					Action moveToIgnoresAction =
						createMoveAction(
							includePlugInsList,
							moveToIgnoresBtn,
							moveToIgnoresTxt,
							ignorePlugInsList,
							"images/arrow-down.png");
					moveToIgnoresBtn = new JButton(moveToIgnoresAction);
					String moveToIgnoresTip =
						"Move the selected plug-ins to the list of those to ignore, below";
					moveToIgnoresBtn.setToolTipText(moveToIgnoresTip);
					addEnabledListener(includePlugInsList, moveToIgnoresBtn);
					moveToIgnoresBtn.setEnabled(
						includeListModel.getSize() != 0);

					// button to include the selected plug-ins from the list of ignored plug-ins
					String moveToIncludesTxt = "Include";
					Action moveToIncludesAction =
						createMoveAction(
							ignorePlugInsList,
							moveToIncludesBtn,
							moveToIncludesTxt,
							includePlugInsList,
							"images/arrow-up.png");
					moveToIncludesBtn = new JButton(moveToIncludesAction);
					String moveToIncludesTip =
						"Move the selected plug-ins to the list of those to include, above";
					moveToIncludesBtn.setToolTipText(moveToIncludesTip);
					addEnabledListener(ignorePlugInsList, moveToIncludesBtn);
					moveToIncludesBtn.setEnabled(
						ignoreListModel.getSize() != 0);

					// initial setting to disable the plug-in selectors if the option 
					//to incluce all plug-ins is checked
					setPlugInsSelectorsEnabled(!allPlugInsChk.isSelected());

					// add the components to the panel
					LibTTx.addGridBagComponent(
						allPlugInsChk,
						constraints,
						0,
						0,
						2,
						1,
						100,
						0,
						panel);
					LibTTx.addGridBagComponent(
						plugInsSelectionLbl,
						constraints,
						0,
						1,
						2,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						includeListPane,
						constraints,
						0,
						2,
						2,
						1,
						0,
						0,
						panel);
					LibTTx.addGridBagComponent(
						moveToIgnoresBtn,
						constraints,
						0,
						3,
						1,
						1,
						100,
						0,
						panel);
					LibTTx.addGridBagComponent(
						moveToIncludesBtn,
						constraints,
						1,
						3,
						1,
						1,
						100,
						0,
						panel);
					LibTTx.addGridBagComponent(
						ignoreListPane,
						constraints,
						0,
						4,
						2,
						1,
						0,
						100,
						panel);
					tabbedPane.setComponentAt(PLUG_INS_PANEL_INDEX, panel);
				}
			});
		}

		/** Enables or disables the plug-ins boxes and buttons.
		 * 
		 * @param b <code>true</code> to enable all of the boxes and buttons,
		 * necessary to pick individual plug-ins when the check box to include all 
		 * plug-ins is unchecked.
		 */
		private void setPlugInsSelectorsEnabled(boolean b) {
			plugInsSelectionLbl.setEnabled(b);
			includePlugInsList.setEnabled(b);
			ignorePlugInsList.setEnabled(b);
			moveToIgnoresBtn.setEnabled(b);
			moveToIncludesBtn.setEnabled(b);
		}

		/** Creates the list of plug-ins.
		 * 
		 * @param listMdl display list model
		 * @param listElts names of plug-ins to include
		 * @return the list
		 */
		private JList createList(DefaultListModel listMdl, String[] listElts) {
			String addElt = null;
			if (listElts != null) {
				int listEltsLen = listElts.length;
				for (int i = 0;
					i < listEltsLen && (addElt = listElts[i]) != null;
					i++) {
					listMdl.addElement(addElt);
				}
			}
			JList list = new JList(listMdl);
			list.setSelectionMode(
				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			list.setLayoutOrientation(JList.VERTICAL);
			list.setVisibleRowCount(-1);
			return list;
		}

		/** Creates an action to move a list item from one list to another.
		 * 
		 * @param fromList source list
		 * @param btn
		 * @param txt
		 * @param toList
		 * @return
		 */
		private Action createMoveAction(
			final JList fromList,
			final JButton btn,
			String txt,
			final JList toList,
			String iconPath) {
			Action action =
				new AbstractAction(txt, LibTTx.makeIcon(iconPath)) {
				public void actionPerformed(ActionEvent evt) {
					Object[] selected = fromList.getSelectedValues();
					DefaultListModel fromListModel =
						(DefaultListModel) fromList.getModel();
					for (int i = 0; i < selected.length; i++) {
						fromListModel.removeElement(selected[i]);
					}
					for (int i = 0; i < selected.length; i++) {
						((DefaultListModel) toList.getModel()).addElement(
							selected[i]);
					}
				}
			};
			return action;
		}

		/** Adds a listener to enable a button once an empty list is gains
		 * an item.
		 * @param list the list
		 * @param btn the button
		 */
		private void addEnabledListener(JList list, final JButton btn) {
			final DefaultListModel mdl = (DefaultListModel) list.getModel();
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent evt) {
					btn.setEnabled(mdl.getSize() != 0);
				}
			});
		}
	}
}
