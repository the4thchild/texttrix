/* TextTrix.java    
   Text Trix
   the text tinker
   http://textflex.com/texttrix
   
   Copyright (c) 2002-3, Text Flex
   All rights reserved.
   
   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions 
   are met:
   
   * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
   * Neither the name of the Text Trix nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.
   
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.  
*/

package com.textflex.texttrix;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.filechooser.FileFilter;
import java.net.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**The main <code>TextTrix</code> class.
 * Takes care of most graphical user interface operations, such as 
 * setting up and responding to changes in the <code>Text Pad</code>s, 
 * tool bar, menus, and dialogs.
 */
public class TextTrix extends JFrame {
    private static ArrayList textAreas = new ArrayList(); // all the TextPads
    private static JTabbedPane tabbedPane
	= new JTabbedPane(JTabbedPane.TOP); // multiple TextPads
    private static JPopupMenu popup = new JPopupMenu(); // make popup menu
    private static JFileChooser chooser = new JFileChooser(); // file dialog
    private static JCheckBoxMenuItem autoIndent 
	= new JCheckBoxMenuItem("Auto-indent"); // auto-indent
    private static String openDir = ""; // most recently path opened to
    private static String saveDir = ""; // most recently path saved to
    private static int fileIndex = 0; // for giving each TextPad a unique name
    private static FindDialog findDialog; // find dialog
    //	private static int tabSize = 0; // user-defined tab display size
    private static PlugIn[] plugIns = null; // plugins from jar archives
    private static Action[] plugInActions = null; // plugin invokers
    private static String charsUnavailable = ""; // chars for shorcuts
    private JMenu trixMenu = new JMenu("Trix"); // trix plugins
    private JMenu toolsMenu = new JMenu("Tools"); // tools plugins
    private JToolBar toolBar = new JToolBar("Trix and Tools"); // icons

    /**Constructs a new <code>TextTrix</code> frame and starting 
     * <code>TextPad</code>.
     */
    public TextTrix(String[] paths) {
	setTitle("Text Trix");
	// pre-set window size
	setSize(500, 600); // TODO: adjust to user's screen size
	ImageIcon im = makeIcon("images/minicon-16x16.png"); // set frame icon
	if (im !=null) 
	    setIconImage(im.getImage());		
	// make first tab and text area
	addTextArea(textAreas, tabbedPane, makeNewFile());
	// adds a change listener to listen for tab switches and display
	// the options of the tab's TextPad
	tabbedPane.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent evt) {
		    TextPad t = getSelectedTextPad();
		    if (t != null) 
			setAutoIndent(t.getAutoIndent());
		}
	    });		

	// make menu bar and menus
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	fileMenu.setMnemonic('I'); // not 'F' since Alt-f for word-forward
	JMenu editMenu = new JMenu("Edit");
	editMenu.setMnemonic('E');
	JMenu viewMenu = new JMenu("View");
	viewMenu.setMnemonic('V');
	trixMenu.setMnemonic('T');
	toolsMenu.setMnemonic('O');
	JMenu helpMenu = new JMenu("Help");
	helpMenu.setMnemonic('H');

	// make tool bar
	toolBar.addMouseListener(new PopupListener());
	toolBar.setFloatable(false); // necessary since not BorderLayout




	/* File menu items */

	// make new tab and text area
	Action newAction = new AbstractAction("New") {
		public void actionPerformed(ActionEvent evt) {
		    addTextArea(textAreas, tabbedPane, makeNewFile());
		}
	    };
	// per Mozilla keybinding
	setAcceleratedAction(newAction, "New", 'T', 
			     KeyStroke.getKeyStroke("ctrl T"));//KeyEvent.VK_T,
	//					 InputEvent.CTRL_MASK));
	fileMenu.add(newAction);
	/* tab shifts defined under View menu section;
	 * alternatively, move to tabs using default Java key-bindings:
	 * -Ctrl-up to tabs
	 * -Lt, rt to switch tabs
	 * -Tab back down to TextPad */

	// (ctrl-o) open file; use selected tab if empty
	Action openAction 
	    = new FileOpenAction(TextTrix.this, "Open", 
				 makeIcon("images/openicon-16x16.png"));
	setAcceleratedAction(openAction, "Open", 'O', 
			     KeyStroke.getKeyStroke("ctrl O"));//KeyEvent.VK_O,
	//					 InputEvent.CTRL_MASK));
	fileMenu.add(openAction);
	JButton openButton = toolBar.add(openAction);
	openButton.setBorderPainted(false);
	setRollover(openButton, "images/openicon-roll-16x16.png");

	// set text and web file filters for open/save dialog boxes
	final ExtensionFileFilter webFilter = new ExtensionFileFilter();
	webFilter.addExtension("html");
	webFilter.addExtension("htm");
	webFilter.addExtension("xhtml");
	webFilter.addExtension("shtml");
	webFilter.addExtension("css");
	webFilter.setDescription("Web files (*.html, *.htm, " 
				 + "*.xhtml, *.shtml, *.css)");
	chooser.setFileFilter(webFilter);
		
	final ExtensionFileFilter rtfFilter = new ExtensionFileFilter();
	rtfFilter.addExtension("rtf");
	rtfFilter.setDescription("RTF files (*.rtf)");
	chooser.setFileFilter(rtfFilter);

	final ExtensionFileFilter txtFilter = new ExtensionFileFilter();
	txtFilter.addExtension("txt");
	txtFilter.setDescription("Text files (*.txt)");
	chooser.setFileFilter(txtFilter);

	
	// close file; check if saved
	Action closeAction = 
	    new AbstractAction("Close", 
			       makeIcon("images/closeicon-16x16.png")) {
		public void actionPerformed(ActionEvent evt) {
		    int i = tabbedPane.getSelectedIndex();
		    if (i >= 0) 
			closeTextArea(i, textAreas, tabbedPane);
		}
	    };
	setAcceleratedAction(closeAction, "Close", 'C', 
		  KeyStroke.getKeyStroke("ctrl W"));//KeyEvent.VK_W, InputEvent.CTRL_MASK));
	fileMenu.add(closeAction);

	// (ctrl-s) save file; no dialog if file already created
	Action saveAction = 
	    new AbstractAction("Save", makeIcon("images/saveicon-16x16.png")) {
		public void actionPerformed(ActionEvent evt) {
		    // can't use tabbedPane.getSelectedComponent() 
		    // b/c returns JScrollPane
		    int tabIndex = tabbedPane.getSelectedIndex();
		    TextPad t = null;
		    if (tabIndex != -1) {
			t = (TextPad)textAreas.get(tabIndex);
			// save directly to file if already created one
			if (t.fileExists())
			    saveFile(t.getPath());
			// otherwise, request filename for new file
			else
			    fileSaveDialog(TextTrix.this);
		    }
		}
	    };
	setAcceleratedAction(saveAction, "Save", 'S', 
			     KeyStroke.getKeyStroke("ctrl S"));//KeyEvent.VK_S, InputEvent.CTRL_MASK));
	fileMenu.add(saveAction);
	JButton saveButton = toolBar.add(saveAction);
	saveButton.setBorderPainted(false);
	setRollover(saveButton, "images/saveicon-roll-16x16.png");

	// save w/ file save dialog
	Action saveAsAction 
	    = new FileSaveAction(TextTrix.this, "Save as...", 
				 makeIcon("images/saveasicon-16x16.png"));
	setAction(saveAsAction, "Save as...", '.');
	fileMenu.add(saveAsAction);
	
	// Start exit functions
	fileMenu.addSeparator();
	
	// (ctrl-q) exit file; close each tab separately, checking for saves
	Action exitAction = new AbstractAction("Exit") {
		public void actionPerformed(ActionEvent evt) {
		    exitTextTrix();
		}
	    };
	// Doesn't work if close all tabs unless click ensure window focused, 
	// such as clicking on menu
	setAcceleratedAction(exitAction, "Exit", 'X', 
		  KeyStroke.getKeyStroke("ctrl Q"));//KeyEvent.VK_Q, InputEvent.CTRL_MASK));
	fileMenu.add(exitAction);

	/* Edit menu items */

	// (ctrl-z) undo; multiple undos available
	Action undoAction = new AbstractAction("Undo") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas
		     .get(tabbedPane.getSelectedIndex())).undo();
		}
	    };
	setAcceleratedAction(undoAction, "Undo", 'U', 
		  KeyStroke.getKeyStroke("ctrl Z"));//KeyEvent.VK_Z, InputEvent.CTRL_MASK));
	editMenu.add(undoAction);

	// (ctrl-y) redo; multiple redos available
	Action redoAction = new AbstractAction("Redo") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas.get(tabbedPane.getSelectedIndex()))
			.redo();
		}
	    };
	setAcceleratedAction(redoAction, "Redo", 'R', 
			     KeyStroke.getKeyStroke("ctrl R"));//KeyEvent.VK_Y, InputEvent.CTRL_MASK));
	editMenu.add(redoAction);

	// Start Cut, Copy, Paste actions
	editMenu.addSeparator();
	
	// (ctrl-x) cut
	Action cutAction = new AbstractAction("Cut") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas
		     .get(tabbedPane.getSelectedIndex())).cut();
		}
	    };
	setAcceleratedAction(cutAction, "Cut", 'C', 
			     KeyStroke.getKeyStroke("ctrl X"));//KeyEvent.VK_X, InputEvent.CTRL_MASK));
	editMenu.add(cutAction);
	popup.add(cutAction);

	// (ctrl-c) copy
	Action copyAction = new AbstractAction("Copy") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas
		     .get(tabbedPane.getSelectedIndex())).copy();
		}
	    };
	setAcceleratedAction(copyAction, "Copy", 'O', 
			     KeyStroke.getKeyStroke("ctrl C"));//KeyEvent.VK_C, InputEvent.CTRL_MASK));
	editMenu.add(copyAction);
	popup.add(copyAction);

	// (ctrl-v) paste
	Action pasteAction = new AbstractAction("Paste") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas
		     .get(tabbedPane.getSelectedIndex())).paste();
		}
	    };
	setAcceleratedAction(pasteAction, "Paste", 'P', 
			     KeyStroke.getKeyStroke("ctrl V"));//KeyEvent.VK_V, InputEvent.CTRL_MASK));
	editMenu.add(pasteAction);
	popup.add(pasteAction);

	// Start selection items
	editMenu.addSeparator();

	// select all text in current text area
	Action selectAllAction = new AbstractAction("Select all") {
		public void actionPerformed(ActionEvent evt) {
		    ((TextPad)textAreas
		     .get(tabbedPane.getSelectedIndex())).selectAll();
		}
	    };
	setAcceleratedAction(selectAllAction, "Select all", 'S', 
			     KeyStroke.getKeyStroke("ctrl L"));//KeyEvent.VK_L, InputEvent.CTRL_MASK));
	editMenu.add(selectAllAction);
	popup.add(selectAllAction);

	// edit menu preferences separator
	editMenu.addSeparator();

	// options sub-menu
	JMenu optionsMenu = new JMenu("Options");
	editMenu.add(optionsMenu);

	// auto-indent
	// apply the selection to the current TextPad
	autoIndent.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent evt) {
		    TextPad t = getSelectedTextPad();
		    if (t != null) 
			t.setAutoIndent(autoIndent.isSelected());
		}
	    });
	optionsMenu.add(autoIndent);

	Action autoIndentAllAction = 
	    new AbstractAction("Auto-indent all current files") {
		public void actionPerformed(ActionEvent evt) {
		    for (int i = 0; i < textAreas.size(); i++) {
			TextPad t = (TextPad)textAreas.get(i);
			if (t != null) 
			    t.setAutoIndent(true);
		    }
		    setAutoIndent(true);
		}
	    };
	optionsMenu.add(autoIndentAllAction);
		



	/* View menu items */
	
	// (ctrl-[) switch to the preceding tab
	Action prevTabAction = new AbstractAction("Preceeding tab") {
		public void actionPerformed(ActionEvent evt) {
		    int tab = tabbedPane.getSelectedIndex();
		    if (tab > 0) {
			tabbedPane.setSelectedIndex(tab - 1);
		    } else if (tab == 0) {
			tabbedPane.setSelectedIndex(tabbedPane
						    .getTabCount() - 1);
		    }
		}
	    };
	setAcceleratedAction(prevTabAction, "Preeceding tab", 'P', 
			     KeyStroke.getKeyStroke("ctrl OPEN_BRACKET"));//KeyEvent.VK_OPEN_BRACKET, 
	//					 InputEvent.CTRL_MASK));
	viewMenu.add(prevTabAction);

	// (ctrl-]) switch to the next tab
	Action nextTabAction = new AbstractAction("Next tab") {
		public void actionPerformed(ActionEvent evt) {
		    int tab = tabbedPane.getSelectedIndex();
		    if ((tab != -1) && (tab == tabbedPane.getTabCount() - 1)) {
			tabbedPane.setSelectedIndex(0);
		    } else if (tab >= 0) {
			tabbedPane.setSelectedIndex(tab + 1);
		    }
		}
	    };
	setAcceleratedAction(nextTabAction, "Next tab", 'N', 
			     KeyStroke.getKeyStroke("ctrl CLOSE_BRACKET"));//KeyEvent.VK_CLOSE_BRACKET, 
	//					 InputEvent.CTRL_MASK));
	viewMenu.add(nextTabAction);

	viewMenu.addSeparator();

	// view as plain text
	Action togglePlainViewAction 
	    = new AbstractAction("Toggle plain text view") {
		public void actionPerformed(ActionEvent evt) {
		    viewPlain();
		}
	    };
	viewMenu.add(togglePlainViewAction);

	// view as HTML formatted text
	Action toggleHTMLViewAction = new AbstractAction("Toggle HTML view") {
		public void actionPerformed(ActionEvent evt) {
		    viewHTML();
		}
	    };
	viewMenu.add(toggleHTMLViewAction);
	
	// view as RTF formatted text
	Action toggleRTFViewAction = new AbstractAction("Toggle RTF view") {
		public void actionPerformed(ActionEvent evt) {
		    viewRTF();
		}
	    };
	viewMenu.add(toggleRTFViewAction);
		
	/* Help menu items */

	// about Text Trix, incl copyright notice and version number
	Action aboutAction = new AbstractAction("About...") {
		public void actionPerformed(ActionEvent evt) {
		    String text = "";
		    text = readText("about.txt");
		    String iconPath = "images/texttrixsignature.png";
		    JOptionPane
			.showMessageDialog(null, 
					   text, 
					   "About Text Trix", 
					   JOptionPane.PLAIN_MESSAGE, 
					   makeIcon(iconPath));
		}
	    };
	setAction(aboutAction, "About...", 'A');
	helpMenu.add(aboutAction);
	
	// shortcuts description; opens new tab
	Action shortcutsAction = new AbstractAction("Shortcuts") {
		public void actionPerformed(ActionEvent evt) {
		    // reads from "shortcuts.txt" in same dir as this class
		    String path = "shortcuts.txt";
		    displayFile(path);
		}
	    };
	setAction(shortcutsAction, "Shortcuts", 'S');
	helpMenu.add(shortcutsAction);

	// features descriptions; opens new tab
	Action featuresAction = new AbstractAction("Features descriptions") {
		public void actionPerformed(ActionEvent evt) {
		    // reads from "features.txt" in same dir as this class
		    String path = "features.txt";
		    displayFile(path);
		}
	    };
	setAction(featuresAction, "Features descriptions", 'F');
	helpMenu.add(featuresAction);

	// license; opens new tab
	Action licenseAction = new AbstractAction("License") {
		public void actionPerformed(ActionEvent evt) {
		    // reads from "license.txt" in same directory as this class
		    String path = "license.txt";
		    displayFile(path);
		}
	    };
	setAction(licenseAction, "License", 'L');
	helpMenu.add(licenseAction);





	/* Trix and Tools menus */

	// Find and Replace: fixed tool, not a plugin
	// (ctrl-shift-F) find and replace Tools feature
	Action findAction 
	    = new AbstractAction("Find and replace", 
				 makeIcon("images/find-16x16.png")) {
		public void actionPerformed(ActionEvent evt) {
		    if (findDialog == null) 
			findDialog = new FindDialog(TextTrix.this);
		    findDialog.show();
		}
	    };
	// need capital "F" b/c "shift"
	setAcceleratedAction(findAction, "Find and replace", 'F', 
		  KeyStroke.getKeyStroke("ctrl shift F")); 
	toolsMenu.add(findAction);
	JButton findButton = toolBar.add(findAction);
	findButton.setBorderPainted(false);
	setRollover(findButton, "images/find-roll-16x16.png");
	findButton.setToolTipText(readText("findbutton.html"));


	// Load plugins; add to appropriate menu

	/* The code has a relatively elaborate mechanism to locate
	   the plugins folder and its JAR files.  Why not use
	   the URL that the Text Trix class supplies?
	   Text Trix needs to locate the specific, absolute path and name
	   of each JAR plugin to load it.  Text Trix's URL must
	   be truncated to its root directory's location and built
	   back up through the plugins directory.  Using getParentFile()
	   to the program's root and appending the path from there to
	   the plugins allows one to use URLClassLoader directly with
	   the resulting URL.

	   Unfortunately, some systems do not
	   locate local files with this method.  The following
	   elaborate system works around this apparent JRE bug by
	   further breaking the URL into a normal path and loading
	   a file from it.

	   Unfortunately again, a new feature from JRE v.1.4
	   prevents the JRE causes spaces to be converted to "%20"
	   turning URL's into strings.  The JRE cannot load files
	   with "%20" in them, however; hus 
	   "c:\Program Files\texttrix-x.y.z\plugins"
	   never gets loaded.  The workaround is to replace all "%20"'s in
	   the string with " ".  Along with v.1.4 comes new String regex
	   tools to make the operation simple, but prior versions
	   give crash after a NoSuchMethodError.  The replacement must be done 
	   manually.
	*/

	// TODO: add additional plugins on the fly
	// this class's location
	String relClassLoc = "com/textflex/texttrix/TextTrix.class";
       	URL urlClassDir = ClassLoader.getSystemResource(relClassLoc);
	String strClassDir = urlClassDir.getPath(); // to check whether JAR
	//	System.out.println(strClassDir);
	File fileClassDir = new File(urlClassDir.getPath());
	File baseDir = null;
	// move into JAR's parent directory only if launched from a JAR
	if (strClassDir.indexOf(".jar!/" + relClassLoc) != -1) {
	    baseDir = fileClassDir.getParentFile().getParentFile()
		.getParentFile().getParentFile().getParentFile();
	} else { // not from JAR; one less parent directory
	    baseDir = fileClassDir.getParentFile().getParentFile()
		.getParentFile().getParentFile();
	}
	// convert "%20", the escape character for a space, into " ";
	// required for starting with JRE v.1.4.0
	// (see http://developer.java.sun.com/developer/ //
	// bugParade/bugs/4466485.html)
	String strBaseDir = baseDir.toString();
	int space = 0;
	while ((space = strBaseDir.indexOf("%20")) != -1) {
	    if (strBaseDir.length() > space + 3) {
		strBaseDir = strBaseDir.substring(0, space) 
		    + " " + strBaseDir.substring(space + 3);
	    } else {
		strBaseDir = strBaseDir.substring(0, space) + " ";
	    }
	}
	/* Though simpler, this method crashes after a NoSuchMethodError
	   under JRE <= 1.3.
	baseDir = new File(baseDir.toString().replaceAll("%20", " "));
	File pluginsFile = new File(baseDir, "plugins");
	*/
	//	System.out.println(strBaseDir);
	//	File f = new File("/home/share");
	//	System.out.println(baseDir.getPath());
	//	String plugInsPath = "";
	//	try {
	//	    plugInsPath = baseDir.getCanonicalPath() + "/plugins";
	//	} catch (IOException e) {}
	//	String sep = System.getProperty("file.separator");
	//	plugInsPath = baseDir.toString() + sep + "plugins";
	//	String[] a = baseDir.list();
	//	System.out.println(plugInsPath);

	// plugins directory;
	// considered nonexistent since baseDir's path in URL syntax
       	File pluginsFile = new File(strBaseDir, "plugins");
	String pluginsPath = pluginsFile.getPath();

	// directory path given as URL; need to parse into normal syntax
	String protocol = "file:";
	int pathStart = pluginsPath.indexOf(protocol);
	// check if indeed given as URL;
	// if so, delete protocal and any preceding info
	if (pathStart != -1)
	    pluginsPath = pluginsPath.substring(pathStart + protocol.length());
	/*
	try {
	    pluginsPath = URLEncoder.encode(pluginsPath, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	}
	*/
	//	System.out.println(pluginsPath);
	// pluginsPath now in normal syntax
	pluginsFile = new File(pluginsPath); // the actual file



	/* A possible workaround for an apparent JRE v.1.4 bug that
	   fails to open files with spaces in their paths.
	   This workaround convert any file or directory names
	   with their "8.3" formatted equivalents.
	   For example, "Program Files" is converted to 
	   "PROGRA~1", which some systems might map to the intended file.
	*/
	/*
	if (!pluginsFile.exists()) {
	    String seg = "";
	    StringTokenizer tok = new StringTokenizer(pluginsPath, "/\\");
	    StringBuffer buf = new StringBuffer(pluginsPath.length());
	    for (int i = 0; tok.hasMoreTokens(); i++) {
		seg = tok.nextToken();
		if (seg.length() > 8) 
		    seg = seg.substring(0, 6).toUpperCase() + "~1";
		buf.append(File.separator + seg);
	    }
	    pluginsPath = buf.toString();
	    pluginsFile = new File(pluginsPath); // the actual file
	    //	    System.out.println(pluginsPath);
	}
	*/

	//	System.out.println(f.getPath());
	//	try { f = new File(new URI(f.toString())); } catch (URISyntaxException e) {}
	//	System.out.println(f.exists());
	//	System.out.println(plugInsPath);
	// load in plugins from plugins directory
	plugIns = LibTTx.loadPlugIns(pluginsFile);
	if (plugIns != null) {
	    for (int i = 0; i < plugIns.length; i++) {
		makePlugInAction(plugIns[i]);
	    }
	}


	/* Place menus and other UI components */

	// add menu bar and menus
	setJMenuBar(menuBar);
	menuBar.add(fileMenu);
	menuBar.add(editMenu);
	menuBar.add(viewMenu);
	menuBar.add(trixMenu);
	menuBar.add(toolsMenu);
	menuBar.add(helpMenu);
	
	// add components to frame; "add" function to set GridBag parameters
	Container contentPane = getContentPane();
	GridBagLayout layout = new GridBagLayout();
	contentPane.setLayout(layout);
	
	GridBagConstraints constraints = new GridBagConstraints();
	
	// add toolbar menu
	constraints.fill = GridBagConstraints.BOTH;
	constraints.anchor = GridBagConstraints.CENTER;
	add(toolBar, constraints, 0, 0, 1, 1, 0, 0);
	
	// add tabbed pane
	constraints.fill = GridBagConstraints.BOTH;
	constraints.anchor = GridBagConstraints.CENTER;
	add(tabbedPane, constraints, 0, 2, 1, 1, 100, 100);

	if (paths != null) {
	    for (int i = 0; i < paths.length; i++) {
		openFile(new File(paths[i]));
	    }
	}

    }
	








    /**Publically executable starter method.
     * Creates the <code>TextTrix</code> object, displays it,
     * an makes sure that it will still undergo its
     * exit routine when closed manually.
     * @param args command-line arguments; not yet used
     */
    public static void main(String[] args) {
	TextTrix textTrix = new TextTrix(args);
	textTrix.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	// make sure still goes through the exit routine if close
	// window manually
	textTrix.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    exitTextTrix();
		}
		/*
		  public void windowGainedFocus(WindowEvent e) {
		  TextPad t;
		  if ((t = getSelectedTextPad()) != null) {
		  t.requestFocusInWindow();
		  }
		  }
		*/
	    });
	textTrix.show();
       	textTrix.getSelectedTextPad().requestFocusInWindow();
    }











    public void makePlugInAction(final PlugIn pl) {
	String name = pl.getName();
	String category = pl.getCategory();
	String description = pl.getDescription();
	String detailedDescription 
	    = LibTTx.readText(pl.getDetailedDescription());
	ImageIcon icon = pl.getIcon();
	ImageIcon rollIcon = pl.getRollIcon();
	Action action = 
	    new AbstractAction(name, icon) {
		public void actionPerformed(ActionEvent evt) {
		    int tabIndex = tabbedPane.getSelectedIndex();
		    if (tabIndex != -1) {
			// may want to automatically apply HTML replacer 
			// after converting to plain
			viewPlain();
			TextPad t = (TextPad)textAreas
			    .get(tabbedPane.getSelectedIndex());
			Document doc = t.getDocument();
			//			String text = t.getText();
			String text = null;

			// only modify the selected text, and make 
			// the action undoable
			int start = t.getSelectionStart();
			int end = t.getSelectionEnd();
			try {
			    if (start == end) {
				// may need to add original text to history buffer
				// before making the change
				//			    t.setUndoableText(pl.run(text, 0, text.length()));
				text = doc.getText(0, doc.getLength());
				text = pl.run(text);
				doc.remove(0, doc.getLength());
				doc.insertString(0, text, null);
				// approximates the original caret position
				if (start > doc.getLength()) {
				    t.setCaretPosition(doc.getLength());
				} else {
				    t.setCaretPosition(start);
				}
			    } else {
				//				t.setUndoableText(pl.run(text, start, end));
				int len = end - start;
				text = doc.getText(start, len);
				text = pl.run(text);
				doc.remove(start, len);
				doc.insertString(start, text, null);
			    }
			    
			    
			} catch (BadLocationException e) {
			    e.printStackTrace();
			}
		    }
		}
	    };
	setAction(action, name);
	if (category.equalsIgnoreCase("tools")) {
	    toolsMenu.add(action);
	} else {
	    trixMenu.add(action);
	}
	JButton button = toolBar.add(action);
	button.setBorderPainted(false);
	setRollover(button, rollIcon);
	button.setToolTipText(detailedDescription);
    }

    /**Gets the last path for opening a file.
     * @return most recent path for opening a file
     */
    public static String getOpenDir() {
	return openDir;
    }

    /**Gets the last path for saving a file.
     * @return most recent path for saving a file
     */
    public static String getSaveDir() {
	return saveDir;
    }

    /**Gets the currently selected <code>TextPad</code>
     * @return <code>TextPad</code> whose tab is currently selected
     */
    public static TextPad getSelectedTextPad() {
	int i = tabbedPane.getSelectedIndex();
	if (i != -1) {
	    return (TextPad)textAreas.get(i);
	} else {
	    return null;
	}
    }

    /**Gets whether the auto-indent function is selected.
     * @return <code>true</code> if auto-indent is selected
     */
    public static boolean getAutoIndent() {
	return autoIndent.isSelected();
    }

    /**Sets the given path as the most recently one used
     * to open a file.
     * @param anOpenPath path to set as last opened location
     */
    public static void setOpenDir(String anOpenDir) {
	openDir = anOpenDir;
    }

    /**Sets the given path as the most recently one used
     * to save a file.
     * @param aSaveDir path to set as last saved location.
     */
    public static void setSaveDir (String aSaveDir) {
	saveDir = aSaveDir;
    }

    public static void setAutoIndent(boolean b) {
	autoIndent.setSelected(b);
    }

    /**Enable button rollover icon change.
     * @param button <code>JButton</code> to display icon rollover change
     * @param iconPath location of icon to change to
     */
    public void setRollover(JButton button, String iconPath) {
	button.setRolloverIcon(makeIcon(iconPath));
	button.setRolloverEnabled(true);
    }
		
    /**Enable button rollover icon change.
     * @param button <code>JButton</code> to display icon rollover change
     * @param iconPath location of icon to change to
     */
    public void setRollover(JButton button, ImageIcon icon) {
	button.setRolloverIcon(icon);
	button.setRolloverEnabled(true);
    }

    /**Set an action's properties.
     * @param action action to set
     * @param description tool tip
     * @param mnemonic menu shortcut
     * @param keyStroke accelerator key shortcut
     */
    public void setAcceleratedAction(Action action, String description, 
			  char mnemonic, KeyStroke keyStroke) {
	action.putValue(Action.SHORT_DESCRIPTION, description);
	action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
	action.putValue(Action.ACCELERATOR_KEY, keyStroke);
    }
	
    /**Sets an action's properties.
     * @param action action to set
     * @param description tool tip
     * @param mnemonic menu shortcut
     */
    public void setAction(Action action, String description, char mnemonic) {
	action.putValue(Action.SHORT_DESCRIPTION, description);
	action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
    }

    /**Sets an action's properties.
     * @param action action to set
     * @param description tool tip
     * @param mnemonic menu shortcut
     */
    public void setAction(Action action, String description) {
	char mnemonic = 0;
	int i = 0;
	for (i = 0; i < description.length()
		 && charsUnavailable
		 .indexOf((mnemonic = description.charAt(i)))
		 != -1;
	     i++);
	action.putValue(Action.SHORT_DESCRIPTION, description);
	// otherwise haven't found a suitable char
	if (i < description.length()) { 
	    action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
	    charsUnavailable += mnemonic;
	}
    }

    /**Creates an image icon.
     * Retrieves the image file from a jar archive.
     * @param path image file location relative to TextTrix.class
     * @return icon from archive; null if the file cannot be retrieved
     */
    public ImageIcon makeIcon(String path) {
	URL iconURL = TextTrix.class.getResource(path);
	return (iconURL != null) ? new ImageIcon(iconURL) : null;
    }

    /**Makes new file with next non-existent file of name format,
     * <code>NewFile<i>n</i>.txt</code>, where <code>n</code>
     * is the next number that Text Trix has not made for 
     * a text area and that is not a curently existing file.
     * @return file with unique, non-existing name
     */
    public File makeNewFile() {
	File file;
	do {
	    fileIndex++; // ensures that no repeat of name in current session
	    // check exisiting files to ensure unique name
	} while ((file = new File("NewFile" + fileIndex + ".txt")).exists());
	// okay to return mutable file since created it anew in this fn?
	// true--prob only unwise if accessor, which gives instance field
	return file;
    }

    /**Displays a read-only file in a <code>Text Pad</code>.  
     * May change name to <code>readDocumentation</code> for 
     * consistency with <code>readText</code>.
     * @param path read-only text file's path.  Can also display
     * non-read-only text files, but can't edit.
     */
    public void displayFile(String path) {
	try {
	    // getResourceAsStream to make future usable as an applet
	    BufferedReader reader = 
		new BufferedReader(new InputStreamReader(TextTrix.class.
							 getResourceAsStream(path)));
	    String text = readText(reader);
	    reader.close();
	    addTextArea(textAreas, tabbedPane, new File(path));
	    TextPad t = (TextPad)textAreas.get(tabbedPane.getSelectedIndex());
	    t.setEditable(false); // so appropriate for read-only
	    t.setText(text);
	    t.setChanged(false);
	    updateTitle(textAreas, tabbedPane);
	    t.setCaretPosition(0);
	} catch(IOException exception) {
	    exception.printStackTrace();
	}
    }
	
    /**Exits <code>TextTrix</code> by closing each tab individually,
     * checking for unsaved text areas in the meantime.
     */
    public static boolean exitTextTrix() {
	boolean b = true;
	int i = tabbedPane.getTabCount();
	// closes each tab individually, using the function that checks
	// for unsaved changes
	while (i > 0 && b) {
	    b = closeTextArea(i - 1, textAreas, tabbedPane);
	    i = tabbedPane.getTabCount();
	}
	if (b == true)
	    System.exit(0);
	return b;
    }
	
    /**Closes a text area.  Checks if the text area is unsaved;
     * if so, evokes a dialog asking whether the user wants to
     * save the text area, discard it, or cancel the closure.
     * If the file has not been saved before, a <code>Save as...</code>
     * dialog appears.  Canceling the <code>Save as...</code>
     * dialog discards the text area, though maybe not so in
     * future releases.
     * @param tabIndex tab to close
     * @param textAreas array of <code>TextPad</code>s
     * @param tabbedPane pane holding a tab to be closed
     * @return <code>true</code> if the tab successfully closes
     */
    public static boolean closeTextArea(int tabIndex, ArrayList textAreas,
					JTabbedPane tabbedPane) {
	boolean successfulClose = false;
		
	TextPad t = (TextPad)textAreas.get(tabIndex);
	// check if unsaved text area
	if (t.getChanged()) {
	    String s = "Please save first.";
	    tabbedPane.setSelectedIndex(tabIndex);
	    // dialog with 3 choices: save, discard, cancel
	    String msg = "This file has not yet been saved."
		+ "\nWhat would you like me to do with this new version?";
	    int choice = 
		JOptionPane.showOptionDialog(null,
					     msg,
					     "Save before close",
					     JOptionPane.WARNING_MESSAGE,
					     JOptionPane.DEFAULT_OPTION,
					     null,
					     new String[] { 
						 "Save", "Toss it out", 
						 "Cancel" },
					     "Save");
	    switch (choice) {
		// save the text area's contents
	    case 0:
		// bring up "Save as..." dialog if never saved file before
		if (t.fileExists()) {
		    successfulClose = saveFile(t.getPath());
		} else {
		    // still closes tab if cancel "Save as..." dialog
		    // may need to change in future releases
		    successfulClose = fileSaveDialog(null);
		}
		if (successfulClose) {
		    removeTextArea(tabIndex, textAreas, tabbedPane);
		}
		break;
		// discard the text area's contents
	    case 1:
		removeTextArea(tabIndex, textAreas, tabbedPane);
		successfulClose = true;
		break;
		// cancel the dialog and return unsuccessful closure;
		// could likely remove default case as well as case 2's break
	    case 2:
		successfulClose = false;
		break;
	    default:
		successfulClose = false;
		break;
	    }
	    // if unchanged, simply remove the tab
	} else {
	    removeTextArea(tabIndex, textAreas, tabbedPane);
	    successfulClose = true;
	}
	return successfulClose;
    }

    /**Read in text from a file and return the text as a string.
     * Differs from <code>displayFile(String path)</code> because
     * allows editing.
     * @param reader text file stream
     * @return text from file
     */
    public String readText(String path) {
	String text = "";
	try {
	    InputStream in = TextTrix.class.getResourceAsStream(path);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    String line;
	    while ((line = reader.readLine()) != null)
		text = text + line + "\n";
	} catch(IOException exception) {
	    exception.printStackTrace();
	}
	return text;
    }
	
    /**Read in text from a file and return the text as a string.
     * Differs from <code>displayFile(String path)</code> because
     * allows editing.
     * @param reader text file stream
     * @return text from file
     */
    public String readText(BufferedReader reader) {
	String text = "";
	String line;
	try {
	    while ((line = reader.readLine()) != null)
		text = text + line + "\n";
	} catch(IOException exception) {
	    exception.printStackTrace();
	}
	return text;
    }

    /**Creates a new <code>TextPad</code> object, a text area 
     * for writing, and gives it a new tab.  Can call for
     * each new file; names the tab, <code>Filen.txt</code>,
     * where <code>n</code> is the tab number.
     */
    public void addTextArea(ArrayList arrayList, JTabbedPane tabbedPane, File file) {
	TextPad textPad = new TextPad(file);
	// final variables so can use in inner class;
			
	JScrollPane scrollPane = 
	    new JScrollPane(textPad, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
			    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	//		scrollPane.setRequestFocusEnabled(false);
	DocumentListener listener = new TextPadDocListener();
			
	// 1 more than highest tab index since will add tab
	int i = tabbedPane.getTabCount();
	tabbedPane.addTab(file.getName() + "  ", scrollPane);
	//		textPad.setLineWrap(true);
	//		textPad.setWrapStyleWord(true);
	textPad.getDocument().addDocumentListener(listener);
	textPad.addMouseListener(new PopupListener());
	// show " *" in tab title when text changed
	arrayList.add(textPad);
	tabbedPane.setSelectedIndex(i);
	tabbedPane.setToolTipTextAt(i, textPad.getPath());
    }

    /**Changes tabbed pane title to indicate whether the file's changes 
     * have been changed or not.
     * Appends "<code> *</code>" if the file has unsaved changes; 
     * appends "<code>  </code>" otherwise.
     * @param arrayList array of <code>TextPad</code>s that the 
     * tabbed pane displays
     * @param tabbedPane tabbed pane to update
     */
    public void updateTitle(ArrayList arrayList, JTabbedPane tabbedPane) {
	int i = tabbedPane.getSelectedIndex();
	TextPad textPad = (TextPad)arrayList.get(i);
	//	String title = tabbedPane.getTitleAt(i);
	String title = textPad.getName();
	// convert to filename; -2 b/c added 2 spaces
	if (textPad.getChanged()) {
	    tabbedPane.setTitleAt(i, title + " *");
	} else {
	    tabbedPane.setTitleAt(i, title + "  ");
		//		setTitleAt(i, title.substring(0, title.length() - 1) + " ");
	}
    }

    /**Adds additional listeners and other settings to a <code>TextPad</code>.
     * Useful to apply on top of the <code>TextPad</code>'s 
     * <code>applyDocumentSettings</code> function.
     * @param TextPad <code>TextPad</code> requiring applied settings
     */
    public void addExtraTextPadDocumentSettings(TextPad textPad) {
	textPad.getDocument().addDocumentListener(new TextPadDocListener());
	textPad.setChanged(true);
	updateTitle(textAreas, tabbedPane);
    }

    /**Displays the given <code>TextPad</code> in plain text format.
     * Calls the <code>TextPad</code>'s <code>viewPlain</code> function
     * before adding <code>TextTrix</code>-specific settings, such as 
     * a <code>TextPadDocListener</code>.
     */
    public void viewPlain() {
	TextPad t = getSelectedTextPad();
	if (t != null) {
	    if (!t.getContentType().equals("text/plain")) {
		t.viewPlain();
		addExtraTextPadDocumentSettings(t);
	    }
	}
    }
	
    /**Displays the given <code>TextPad</code> in html text format.
     * Calls the <code>TextPad</code>'s <code>viewHTML</code> function
     * before adding <code>TextTrix</code>-specific settings, such as 
     * a <code>TextPadDocListener</code>.
     */
    public void viewHTML() {
	TextPad t = getSelectedTextPad();
	if (t != null) {
	    if (!t.getContentType().equals("text/html")) {
		t.viewHTML();
		addExtraTextPadDocumentSettings(t);
	    }
	}
    }
	
    /**Displays the given <code>TextPad</code> in RTF text format.
     * Calls the <code>TextPad</code>'s <code>viewRTF</code> function
     * before adding <code>TextTrix</code>-specific settings, such as 
     * a <code>TextPadDocListener</code>.
     */
    public void viewRTF() {
	TextPad t = getSelectedTextPad();
	if (t != null) {
	    if (!t.getContentType().equals("text/rtf")) {
		t.viewRTF();
		addExtraTextPadDocumentSettings(t);
	    }
	}
    }

    /**Front-end to the <code>TextPad</code>'s <code>read</code> method from 
     * <code>JTextPane</code>.
     * Reads in a file, applies <code>TextPad</code>'s settings, and
     * finally adds <code>TextTrix</code>-specific settings.
     * @param TextPad <code>TextPad</code> to read a file into
     * @param in file reader
     * @param desc reader stream description
     */
    public void read(TextPad textPad, Reader in, Object desc) throws IOException {
	textPad.read(in, desc);
	textPad.applyDocumentSettings();
	addExtraTextPadDocumentSettings(textPad);
    }

    /**Removes a tab containing a text area.
     * @param i tab index
     * @param l text area array list
     * @param tp tabbed pane from which to remove a tab
     */
    public static void removeTextArea(int i, ArrayList l, JTabbedPane tp) {
	l.remove(i);
	tp.remove(i);
    }

    /**Adds a new component to the <code>GridBagLayout</code>
     * manager.
     * @param c component to add
     * @param constraints layout constraints object
     * @param x column number
     * @param y row number
     * @param w number of columns to span
     * @param h number of rows to span
     * @param wx column weight
     * @param wy row weight
     */
    public void add(Component c, GridBagConstraints constraints,
		    int x, int y, int w, int h, 
		    int wx, int wy) {
	constraints.gridx = x;
	constraints.gridy = y;
	constraints.gridwidth = w;
	constraints.gridheight = h;
	constraints.weightx = wx;
	constraints.weighty = wy;
	getContentPane().add(c, constraints);
    }

    /**Saves text area contents to a given path.
     * @param path file path in which to save
     * @return true for a successful save, false if otherwise
     */
    public static boolean saveFile(String path) {
	//	System.out.println("printing");
	TextPad t = getSelectedTextPad();
	if (t != null) {
	    try {
		PrintWriter out = new 
		    PrintWriter(new FileWriter(path), true);
		out.print(t.getText());
		out.close();
		t.setChanged(false);
		t.setFile(path);
		tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), 
				      t.getName() + "  ");
		return true;
	    } catch(IOException exception) {
		exception.printStackTrace();
		return false;
	    }
	} else {
	    return false;
	}
    }

    public boolean openFile(File file) {
	if (file.exists()) {
	    TextPad t = getSelectedTextPad();
	    String path = file.getPath();
	    try {
		BufferedReader reader = 
		    new BufferedReader(new FileReader(path));
		    
		// check if tabs exist; get TextPad if true
		/* t.getText() != null, even if have typed nothing 
		   in it.
		   Add tab and set its text if no tabs exist or 
		   if current tab has tokens; set current tab's 
		   text otherwise
		*/
		if (t == null || !t.isEmpty()) { 
		    addTextArea(textAreas, tabbedPane, file);
		    t = (TextPad)textAreas.get(tabbedPane
					       .getSelectedIndex());
		    read(t, reader, path);
		} else {
		    read(t, reader, path);
		}
		t.setCaretPosition(0);
		t.setChanged(false);
		t.setFile(path);
		//			tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), 
		//	      files[i].getName());
		updateTitle(textAreas, tabbedPane);
		reader.close();
		setOpenDir(file.getParent());
		return true;
	    } catch(IOException exception) {
		exception.printStackTrace();
	    }
	}
	return false;
    }


    /**Evokes a save dialog, with a filter for text files.
     * Sets the tabbed pane tab to the saved file name.
     * @return true if the approve button is chosen, false if otherwise
     */
    public static boolean fileSaveDialog(JFrame owner) {
	int tabIndex = tabbedPane.getSelectedIndex();
	if (tabIndex != -1) {
	    TextPad t = (TextPad)textAreas.get(tabIndex);
	    if (t.fileExists()) {
		chooser.setCurrentDirectory(new File(t.getDir()));
		// save to file's current location
		chooser.setSelectedFile(new File(t.getPath()));
	    } else {
		// if the file hasn't been created, default to the directory
		// last saved to
		chooser.setCurrentDirectory(new File(saveDir));
	    }
	    // can't save to multiple files;
	    // if set to true, probably have to use double-quotes
	    // when typing names
	    chooser.setMultiSelectionEnabled(false); 
	    int result = chooser.showSaveDialog(owner);
	    if (result == JFileChooser.APPROVE_OPTION) {
		String path = chooser.getSelectedFile().getPath();
		saveFile(path);
		setSaveDir(chooser.getSelectedFile().getParent());
		tabbedPane.setToolTipTextAt(tabIndex, path);
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    /**Evokes a open file dialog, from which the user can
     * select a file to display in the currently selected tab's
     * text area.  Filters for text files, though provides
     * option to display all files.
     */
    private class FileOpenAction extends AbstractAction {
	JFrame owner;
	
	public FileOpenAction(JFrame aOwner, String name, Icon icon) {
	    owner = aOwner;
	    putValue(Action.NAME, name);
	    putValue(Action.SMALL_ICON, icon);
	}
	
	public void actionPerformed(ActionEvent evt) {
	    /*
	    TextPad t = null;
	    int tabIndex = tabbedPane.getSelectedIndex();
	    if (tabIndex != -1) 
		t = (TextPad)textAreas.get(tabIndex);
	    */
	    TextPad t = getSelectedTextPad();
	    // File("") evidently brings file dialog to last path, 
	    // whether last saved or opened path
	    String dir = openDir;
	    if (t != null && (dir = t.getDir()) == "") 
		dir = openDir;
	    chooser.setCurrentDirectory(new File(dir));
	    chooser.setMultiSelectionEnabled(true); // allow opening multiple files
	    
	    int result = chooser.showOpenDialog(owner);
	    if (result == JFileChooser.APPROVE_OPTION) {
		File[] files = chooser.getSelectedFiles();
		for (int i = 0; i < files.length; i++) {
		    //		    String path = files[i].getPath();
		    //		    if (files[i].exists())
		    openFile(files[i]);
		}
	    }
	}
    }
	
    /**Responds to user input calling for a save dialog.
     */
    private class FileSaveAction extends AbstractAction {
	JFrame owner;
		
	public FileSaveAction(JFrame aOwner, String name, Icon icon) {
	    owner = aOwner;
	    putValue(Action.NAME, name);
	    putValue(Action.SMALL_ICON, icon);
	}
		
	public void actionPerformed(ActionEvent evt) {
	    fileSaveDialog(owner);
	}
    }

    /**Find and replace dialog.
     * Creates a dialog box accepting input for search and replacement 
     * expressions as well as options to tailor the search.
     */
    private class FindDialog extends JDialog {
	JTextField find; // search expression input
	JTextField replace; // replacement expression input
	JCheckBox word; // treat the search expression as a separate word
	JCheckBox wrap; // search to the bottom and start again from the top
	JCheckBox selection; // search only within a highlighted section
	JCheckBox replaceAll; // replace all instances of search expression
	JCheckBox ignoreCase; // ignore upper/lower case
		
	/**Construct a find/replace dialog box
	 * @param owner frame to which the dialog box will be attached; 
	 * can be null
	 */
	public FindDialog(JFrame owner) {
	    super(owner, "Find and Replace", false);
	    setSize(400, 150);
	    Container contentPane = getContentPane();
	    contentPane.setLayout(new GridBagLayout());
	    GridBagConstraints constraints = new GridBagConstraints();
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.anchor = GridBagConstraints.CENTER;
			
	    // search expression input
	    add(new JLabel("Find:"), constraints, 0, 0, 1, 1, 100, 0);
	    add(find = new JTextField(20), constraints, 1, 0, 2, 1, 100, 0);
	    find.addKeyListener(new KeyAdapter() {
		    public void keyPressed(KeyEvent evt) {
			if (evt.getKeyCode() == KeyEvent.VK_ENTER) 
			    find();
		    }
		});		

	    // replace expression input
	    add(new JLabel("Replace:"), constraints, 0, 1, 1, 1, 100, 0);
	    add(replace = new JTextField(20), constraints, 1, 1, 2, 1, 100, 0);
	    replace.addKeyListener(new KeyAdapter() {
		    public void keyPressed(KeyEvent evt) {
			if (evt.getKeyCode() == KeyEvent.VK_ENTER) 
			    findReplace();
		    }
		});
			
	    // treat search expression as a separate word
	    add(word = new JCheckBox("Whole word only"), 
		constraints, 0, 2, 1, 1, 100, 0);
	    word.setMnemonic(KeyEvent.VK_N);
	    word.setToolTipText("Search for the expression as a separate word");
			
	    // wrap search through start of text if necessary
	    add(wrap = new JCheckBox("Wrap"), constraints, 2, 2, 1, 1, 100, 0);
	    wrap.setMnemonic(KeyEvent.VK_A);
	    wrap.setToolTipText("Start searching from the cursor and wrap back to it");
			
	    // replace all instances within highlighted section
	    add(selection = new JCheckBox("Replace within selection"), 
		constraints, 1, 2, 1, 1, 100, 0);
	    selection.setMnemonic(KeyEvent.VK_S);
	    selection.setToolTipText("Search and replace text within the entire highlighted section");
			
	    // replace all instances from cursor to end of text unless 
	    // combined with wrap, where replace all instances in whole text
	    add(replaceAll = new JCheckBox("Replace all"), 
		constraints, 0, 3, 1, 1, 100, 0);
	    replaceAll.setMnemonic(KeyEvent.VK_L);
	    replaceAll.setToolTipText("Replace all instances of the expression");
			
	    // ignore upper/lower case while searching
	    add(ignoreCase = new JCheckBox("Ignore case"), 
		constraints, 1, 3, 1, 1, 100, 0);
	    ignoreCase.setMnemonic(KeyEvent.VK_I);
	    ignoreCase.setToolTipText("Search for both lower and upper case versions of the expression");

	    // find action, using the appropriate options above
	    Action findAction = new AbstractAction("Find", null) {
		    public void actionPerformed(ActionEvent e) {
			find();
		    }
		};
	    setAcceleratedAction(findAction, "Find", 'F', 
				 KeyStroke.getKeyStroke("alt F"));//KeyEvent.VK_F, 
	    //					     InputEvent.ALT_MASK));
	    add(new JButton(findAction), constraints, 0, 4, 1, 1, 100, 0);

	    // find and replace action, using appropriate options above
	    Action findReplaceAction = new AbstractAction("Find and Replace", null) {
		    public void actionPerformed(ActionEvent e) {
			findReplace();
		    }
		};
	    setAcceleratedAction(findReplaceAction, "Find and replace", 'R', 
				 KeyStroke.getKeyStroke("alt R"));//KeyEvent.VK_R,
	    //					     InputEvent.ALT_MASK));
	    add(new JButton(findReplaceAction), constraints, 1, 4, 1, 1, 100, 0);
	}

		
	/**Adds a new component to the <code>GridBagLayout</code> manager.
	 * @param c component to add
	 * @param constraints layout constraints object
	 * @param x column number
	 * @param y row number
	 * @param w number of columns to span
	 * @param h number of rows to span
	 * @param wx column weight
	 * @param wy row weight
	 * */
	public void add(Component c, GridBagConstraints constraints,
			int x, int y, int w, int h, 
			int wx, int wy) {
	    constraints.gridx = x;
	    constraints.gridy = y;
	    constraints.gridwidth = w;
	    constraints.gridheight = h;
	    constraints.weightx = wx;
	    constraints.weighty = wy;
	    getContentPane().add(c, constraints);
	}
		
	/**Finds the give search pattern.
	 */
	public void find() {
	    TextPad t = getSelectedTextPad();
	    if (t != null) {
		String findText = find.getText();
		// search from the current carat position
		int n = t.getCaretPosition();
		//		Document doc = t.getDocument();
		//		String text = doc.getText(
		n = Tools.find(t.getText(), findText, n, 
			       word.isSelected(), ignoreCase.isSelected());
		// wrap if wrap-enabled
		if (n == -1 && wrap.isSelected()) {
		    n = Tools.find(t.getText(), findText, 0, 
				   word.isSelected(), ignoreCase.isSelected());
		}
		// highlight the quarry if found
		if (n != -1) {
		    t.setCaretPosition(n);
		    t.moveCaretPosition(n + findText.length());
		    t.getCaret().setSelectionVisible(true); // to ensure selection visibility
		}
	    }
	}
		
	/**Finds and replaces the given search pattern.
	 * Allows the replacements to be undone.
	 */
	public void findReplace() {
	    TextPad t = getSelectedTextPad();
	    if (t != null) {
		String findText = find.getText();
		String replaceText = replace.getText();
		Document doc = t.getDocument();
		String text = null;
		int start = t.getSelectionStart();
		int end = t.getSelectionEnd();
		// works within the selected range
		try {
		    if (selection.isSelected()) {
			int len = end - start;
			//			System.out.println("start: " + start + ", end: " + end);
			text = doc.getText(start, len);
			text = Tools.findReplace(text, findText, replaceText,
						 word.isSelected(), 
						 true, 
						 false,
						 ignoreCase.isSelected());
			doc.remove(start, len);
			doc.insertString(start, text, null);
			/*
			  t.setUndoableText(Tools.findReplace(text, findText, 
							replaceText,
							start, 
							, 
							word.isSelected(), 
							true, false, 
							ignoreCase
							.isSelected()));
			*/
			// if no range is chosen, works within the whole text
		    } else {
			text = doc.getText(0, doc.getLength());
			text = Tools.findReplace(text, findText, replaceText,
						 t.getCaretPosition(),
						 text.length(),
						 word.isSelected(),
						 replaceAll.isSelected(),
						 wrap.isSelected(),
						 ignoreCase.isSelected());
			doc.remove(0, doc.getLength());
			doc.insertString(0, text, null);
			// approximates the original caret position
			if (start > doc.getLength()) {
			    t.setCaretPosition(doc.getLength());
			} else {
			    t.setCaretPosition(start);
			}
			/*
			t.setUndoableText(Tools.findReplace(text, findText, replaceText,
							    t.getCaretPosition(), text.length(), 
							    word.isSelected(), replaceAll.isSelected(), 
							    wrap.isSelected(), ignoreCase.isSelected()));
			*/
		    }
		} catch (BadLocationException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
	
    /**Responds to changes in the <code>TextPad</code> text areas.
     * Updates the titles to reflect text alterations.
     */
    private class TextPadDocListener implements DocumentListener {
	
	/**Flags a text insertion.
	 * @param e insertion event
	 */
	public void insertUpdate(DocumentEvent e) {
	    ((TextPad)textAreas.get(tabbedPane.getSelectedIndex())).setChanged(true);
	    updateTitle(textAreas, tabbedPane);
	}

	/**Flags a text removal.
	 * @param e removal event
	 */
	public void removeUpdate(DocumentEvent e) {
	    ((TextPad)textAreas.get(tabbedPane.getSelectedIndex())).setChanged(true);
	    updateTitle(textAreas, tabbedPane);
	}

	/**Flags any sort of text change.
	 * @param e any text change event
	 */
	public void changedUpdate(DocumentEvent e) {
	}
    }

    private class PopupListener extends MouseAdapter {
	public void mousePressed(MouseEvent e) {
	    if (e.isPopupTrigger()) {
		popup.show(e.getComponent(), e.getX(), e.getY());
	    }
	}

	public void mouseReleased(MouseEvent e) {
	    if (e.isPopupTrigger()) {
		popup.show(e.getComponent(), e.getX(), e.getY());
	    }
	}
    }

    /**Responds to tab changes in the JTabbedPane.
     * Not presently working.
     *
     private class TabSwitchListener implements ChangeListener {
     public void stateChanged(ChangeEvent e) {
     if (tabbedPane.getTabCount() > 0) {
     getSelectedTextPad().requestFocusInWindow();
     setPrevTabIndex(getCurrentTabIndex());
     setCurrentTabIndex(tabbedPane.getSelectedIndex());
     }
     }
     }
    */
    /*	
      private class TextPadTabbedPane extends JTabbedPane {
      public TextPadTabbedPane(int tabPlacement) {
      super(tabPlacement);
      }

      public static ChangeListener createChangeListener() {
      return new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
      System.out.println("I'm here: " + tabbedPane.getSelectedIndex());
      TextPad t = null;
      if ((t = getSelectedTextPad()) != null) {
      t.setVisible(true);
      t.requestFocus();
      }
      }
      };
      }
      }
    */
}

/**Filters for files with specific extensions.
 */
class ExtensionFileFilter extends FileFilter {
    private String description = "";
    private ArrayList extensions = new ArrayList();

    /**Add extension to include for file display.
     * May need to modify to check whether extension has already
     * been added.
     * @param file extension, such as <code>.txt</code>, though
     * the period is optional
     */
    public void addExtension(String extension) {
	if (!extension.startsWith("."))
	    extension = "." + extension;
	extensions.add(extension.toLowerCase());
    }

    /**Sets the file type description.
     * @param aDescription file description, such as <code>text
     * files</code> for <code>.txt</code> files
     */
    public void setDescription(String aDescription) {
	description = aDescription;
    }

    /**Gets file type's description.
     * @return file type description.
     */
    public String getDescription() {
	return description;
    }

    /**Accept a given file to display if it has the extension
     * currently being filtered for.
     * @param f file whose extension need to check
     * @return <code>true</code> if accepts file, <code>false</code>
     * if don't
     */
    public boolean accept(File f) {
	if (f.isDirectory()) 
	    return true;
	String name = f.getName().toLowerCase();

	for (int i = 0; i < extensions.size(); i++)
	    if (name.endsWith((String)extensions.get(i)))
		return true;
	return false;
    }
}

