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
 * Portions created by the Initial Developer are Copyright (C) 2002-4
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s): David Young <dvd@textflex.com>
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
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.undo.*;
import javax.swing.text.*;
import java.io.*;
import javax.swing.event.*;

/**The writing pad, complete with keyboard shortcuts, auto-indent
 * functions, and text sytle changes.
 */
public class TextPad extends JTextPane implements StateEditable {
	private File file; // the file that the pad displays
	private boolean changed = false; // flag that text changed
	private String path; // file's path
	// allows for multiple and ignored undo operations
	private UndoManagerTTx undoManager = new UndoManagerTTx();
	private Hashtable actions; // table of shortcut actions
	private InputMap imap = null; // map of keyboard inputs
	private ActionMap amap = null; // map of actions
	private boolean autoIndent = false; // flag to auto-indent the text
	private int tabSize = 4; // default tab display size
	private static boolean JVM_15 = false; // flags that running on JVM 1.5
	private StoppableThread autoSaveTimer = null; // timer to auto save the text
	private CompoundEdit compoundEdit = null;
	private boolean compoundEditing = false;

	/**Constructs a <code>TextPad</code> that includes a file
	 * for the text area.
	 * @param aFile file to which the <code>TextPad</code> will
	 * save its text area contents
	 */
	public TextPad(File aFile, Prefs prefs) {
		// TODO: decide whether to check JVM within each TextPad or only once,
		// within TextTrix, with ways to check TextTrix or pass as a parameter 
		JVM_15 = System.getProperty("java.vm.version").indexOf("1.5") != -1;
		file = aFile;
		applyDocumentSettings();
		// to allow multiple undos and listen for events

		// for new key-bindings
		imap = getInputMap(JComponent.WHEN_FOCUSED);
		amap = getActionMap();
		createActionTable(this);

		// (ctrl-backspace) delete from caret to current word start
		// First discard JTextComponent's usual dealing with ctrl-backspace.
		// (enter) Auto-indent if Text Trix's option selected.
		addKeyListener(new KeyAdapter() {
			/**Responds to whatever the current key combination maps to.
			 * Backspaces have already been processed in JVM >= v.1.5.
			 * 
			 */
			public void keyTyped(KeyEvent event) {
				char keyChar = event.getKeyChar();
				if (event.isControlDown()
					&& keyChar == KeyEvent.VK_BACK_SPACE) {
					// TODO: may not be necessary, at least w/ JVM >= v.1.4.2
					event.consume();
				} else if (autoIndent && keyChar == KeyEvent.VK_ENTER) {
					autoIndent();
				} else if (
					autoIndent
						&& keyChar == KeyEvent.VK_TAB
						&& isLeadingTab()) {
					// performs the action after adding the tab
					indentCurrentParagraph(getTabSize());
				}/* else if (
					autoIndent
						&& keyChar == KeyEvent.VK_BACK_SPACE
						&& isLeadingTab(JVM_15)) {
					System.out.println("right here, baby");
					// performs the action before deleting the char, the
					// opposite of the assumption in indentCurrentParagraph
					indentCurrentParagraph(getTabSize(), !JVM_15);
				}
				*/
			}
			
			/**Responds to key events right after the key is pressed.
			 * Unlike keyTyped(KeyEvent), backspaces have not yet
			 * been processed, even for JVM == v.1.5.
			 * 
			 */
			public void keyPressed(KeyEvent evt) {
				int keyCode = evt.getKeyChar();
				if (autoIndent
					&& keyCode == KeyEvent.VK_BACK_SPACE
					&& isLeadingTab()) {
					indentCurrentParagraph(getTabSize(), JVM_15);
				}
				//				System.out.println("keyChar:" + keyCode);
			}

		});
		// applies the user specified set of keybindings
		applyKeybindings(prefs);

	}

	/** Sets the keybindings to the preferred value.
	 * 
	 * @param prefs preferences storage
	 */
	public void applyKeybindings(Prefs prefs) {
		if (prefs.isHybridKeybindings()) {
			hybridKeybindings();
		} else if (prefs.isEmacsKeybindings()) {
			emacsKeybindings();
		} else {
			standardKeybindings();
		}
	}

	/** Sets the keybindings to standard shortcuts.
	 * These shortcuts consist of those typically found on most desktop
	 * systems.  The old set of keybindings are replaced with this set.
	 *
	 */
	public void standardKeybindings() {
		createActionTable(this);
		universalShortcuts();
	}

	/** Sets the keybindings to a mesh of standard and Emacs shortcuts.
	 * The keybinding uses Emacs shortcuts are those for most single line or 
	 * character navigation, but standard shortcuts for everything else.  In case
	 * of a conflict, the Emacs shortcuts take precedence.
	 *
	 */
	public void hybridKeybindings() {
		createActionTable(this);
		universalShortcuts();
		partialEmacsShortcuts();
	}

	/** Sets the keybindings to Emacs shortcuts for most typical single-key
	 * combinations.  
	 * The keybindings essentially uses a hierarchy of standard, hybrid,
	 * and Emacs combinations, where set of shortcuts takes precedence
	 * over the previous set in case of conflict.  The limit of the shortcuts is
	 * a set of common single-key Emacs combinations.
	 */
	public void emacsKeybindings() {
		createActionTable(this);
		universalShortcuts();
		partialEmacsShortcuts();
		emacsShortcuts();
	}

	/** Creates the universal shortcuts, common to standard, partial-Emacs,
	 * and Emacs keybindings.
	 * Currently the standard set consists of only the universal shortcuts.
	 *
	 */
	private void universalShortcuts() {
		// Next apply own action
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Event.CTRL_MASK),
			"deleteWord");
		amap.put("deleteWord", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				int wordPos = getWordPosition();
				// delete via the document methods rather than building
				// string manually, a slow task.  Uses document rather than
				// AccessibleJTextComponent in case used for serialization:
				// Serialized objects of this class won't be compatible w/
				// future releases
				try {
					// call getDocument() each time since doc may change
					// with call to viewPlain, viewHTML, or viewRTF
					getDocument().remove(wordPos, getCaretPosition() - wordPos);
					setCaretPosition(wordPos);
				} catch (BadLocationException b) {
					System.out.println("Deletion out of range.");
				}
				/* Alternate method, essentially same as document except
				 * using the AccessibleJTextComponent class, whose serializable
				 * objects may not be compatible w/ future releases access the 
				 * text component itself to tell it to perform the deletions rather 
				 * than building string manually, a slow task
				 */
				/*
				(new JTextComponent.AccessibleJTextComponent())
				.delete(wordPos, getCaretPosition());
				*/
			}
		});
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Event.SHIFT_MASK),
			"deletePrevChar");
		amap.put(
			"deletePrevChar",
			getActionByName(DefaultEditorKit.deletePrevCharAction));
	}

	/** Creates the partial-Emacs shortcuts, consisting of single character and
	 * line navigation.
	 *
	 */
	private void partialEmacsShortcuts() {
		// (ctrl-f) advance a character
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK),
			"forwardChar");
		amap.put(
			"forwardChar",
			getActionByName(DefaultEditorKit.forwardAction));

		// (ctrl-b) go back a character
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK),
			"backwardChar");
		amap.put(
			"backwardChar",
			getActionByName(DefaultEditorKit.backwardAction));

		// (ctrl-n) advance a line
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK),
			"downChar");
		amap.put("downChar", getActionByName(DefaultEditorKit.downAction));

		// (ctrl-p) go up a line
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK),
			"upChar");
		amap.put("upChar", getActionByName(DefaultEditorKit.upAction));

		// (ctrl-a) go to the beginning of the line
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK),
			"beginLine");
		amap.put(
			"beginLine",
			getActionByName(DefaultEditorKit.beginLineAction));

		// (ctrl-e) go to the end of the line
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK),
			"endLineChar");
		amap.put(
			"endLineChar",
			getActionByName(DefaultEditorKit.endLineAction));

		// (ctrl-d) delete next char		
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK),
			"deleteNextChar");
		amap.put(
			"deleteNextChar",
			getActionByName(DefaultEditorKit.deleteNextCharAction));

		// (alt-b) go to beginning of current word
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.ALT_MASK),
			"wordStart");
		amap.put("wordStart", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				setCaretPosition(getWordPosition());
			}
		});

		// (alt-f) go to beginning of next word
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.ALT_MASK),
			"nextWord");
		amap.put("nextWord", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				setCaretPosition(getNextWordPosition());
			}
		});

	}

	/** Sets the Emacs shortcuts, consisting of common single-key Emacs keybindings.
	 * These shortcuts should be applied on top of those from
	 * <code>partialEmacsShortcuts()</code>; <code>emacsShortcuts()</code>
	 * only applies additional key combinations.
	 *
	 */
	private void emacsShortcuts() {
		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK),
			"pageDown");
		amap.put("pageDown", getActionByName(DefaultEditorKit.pageDownAction));

		imap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.ALT_MASK),
			"pageUp");
		amap.put("pageUp", getActionByName(DefaultEditorKit.pageUpAction));

		imap.put(
			KeyStroke.getKeyStroke(
				KeyEvent.VK_COMMA,
				Event.ALT_MASK | Event.SHIFT_MASK),
			"begin");
		amap.put("begin", getActionByName(DefaultEditorKit.beginAction));

		imap.put(
			KeyStroke.getKeyStroke(
				KeyEvent.VK_PERIOD,
				Event.ALT_MASK | Event.SHIFT_MASK),
			"end");
		amap.put("end", getActionByName(DefaultEditorKit.endAction));

	}

	/** Sets the default displayed tab sizes.
	 * Only affects the displayed size, since the text itself represents the tab simply
	 * as "\t".
	 * @param tabChars number of spaces for the tab to represent
	 * @see #setNoTabs()
	 * @see #setIndentTabs(int) 
	 */
	public void setDefaultTabs(int tabChars) {
		int charWidth = getFontMetrics(getFont()).charWidth(' ');
		int tabWidth = charWidth * tabChars;

		TabStop[] tabs = new TabStop[30]; // just enough to fit default frame
		for (int i = 0; i < tabs.length; i++)
			tabs[i] = new TabStop((i + 1) * tabWidth);
		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setTabSet(attribs, tabSet);
		StyleConstants.setLeftIndent(attribs, 0);
		//StateEdit stateEdit = new StateEdit(this);
		undoManager.setIgnoreNextStyleChange(true);
		getStyledDocument()
			.setParagraphAttributes(0, getDocument().getLength() + 1,
		// next char
		attribs, false); // false to preserve default font
		//stateEdit.end();
		//undoManager.addEdit((UndoableEdit) stateEdit);
	}

	/** Sets the displayed tab size to 0.
	 * Only affects the displayed size, since the text still represents the tab simply as "\t".
	 * Useful when representing tabs through other means, such as styled indents.
	 * @see #setDefaultTabs(int)
	 * @see #setIndentTabs(int)
	 */
	public void setNoTabs() { //int offset, int length) {
		//	System.out.println("set no tabs at position " + offset + " for " + length + " chars");
		TabStop[] tabs = new TabStop[1];
		tabs[0] = new TabStop(0);
		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setTabSet(attribs, tabSet);
		//StateEdit stateEdit = new StateEdit(this);
		undoManager.setIgnoreNextStyleChange(true);
		getStyledDocument()
			.setParagraphAttributes(0, getDocument().getLength() + 1,
		// next char
		attribs, false); // false to preserve default font
		//stateEdit.end();
		//undoManager.addEdit((UndoableEdit) stateEdit);
	}

	/** Sets the displayed indentation for the entire text. 
	 * Only affects the displayed size.  
	 * Useful to represent tabs as styled indents.
	 * @param tabChars the number of characters that each tab represents
	 * @see #setDefaultTabs(int)
	 * @see #setIndentTabs(int, int, int)
	 */
	public void setIndentTabs(int tabChars) {
		setIndentTabs(tabChars, 0, getAllText().length());
	}

	/** Sets the displayed indentation for the region beginning at 
	 * <code>start</code> through any paragraph that begins at <code>end</code>.
	 * By only affecting the displayed size, this method is useful 
	 * to represent tabs as styled indents.
	 * @param tabChars the number of characters that each tab represents
	 * @param start the position from which to start indenting
	 * @param end the first position from which to no longer indent
	 * @see #setDefaultTabs(int)
	 * @see #setIndentTabs(int)
	 */
	public void setIndentTabs(int tabChars, int start, int end) {
		//		System.out.println("start: " + start + ", end: " + end);
		int i = start;
		int j = 0;
		String s = getAllText();
		while (i < end) {
			//			System.out.println("i: " + i + ", j: " + j);
			j = s.indexOf("\n", i);
			if (j == -1) j = end - 1;
			int tabs = leadingTabsCount(s, i);
			if (tabs > 0)
				indent(tabChars, tabs, i, j - i + 1);
			i = j + 1;
		}
		/*
		int tabs = leadingTabsCount(s, i);
		indent(tabChars, tabs, i, s.length() + 1);
		*/
	}

	/** Counts the number of continuous tabs from a given position.
	 * Useful when determining the number of tabs in the current line to auto-indent
	 * the same number for the next line, for example.
	 * @param s text to count for tabs
	 * @param offset position to start counting
	 * @return number of continuous tabs from a given position
	 */
	public int leadingTabsCount(String s, int offset) {
		int tabs = 0;
		for (int i = offset; i < s.length() && s.charAt(i) == '\t'; i++)
			++tabs;
		return tabs;
	}

	/** Indents a paragraph by a given number of tabs and size per tab.
	 * Indents the entire region, not just the first line, though each tab remains the size of
	 * one space.
	 * @param tabChars number of spaces to represent for each tab
	 * @param tabs number of tabs
	 * @param offset position in text at which to start indenting
	 * @param length position in text at which to stop indenting
	 */
	public void indent(int tabChars, int tabs, int offset, int length) {
		int charWidth = getFontMetrics(getFont()).charWidth(' ');
		int tabWidth = charWidth * tabChars;

		SimpleAttributeSet attribs = new SimpleAttributeSet();
		StyleConstants.setLeftIndent(attribs, tabs * tabWidth);
		undoManager.setIgnoreNextStyleChange(true);
		getStyledDocument().setParagraphAttributes(offset, length, // next char
		attribs, false); // false to preserve default font
	}

	/** Indents the current paragraph, no matter where the caret is within it.
	 * Renders tabs as spaces, but indents the entire region a given number of spaces
	 * per tab.
	 * @param tabChars number of spaces for each tab to represent
	 * @param unindentNotJVM_15 an unindent has occurred within a Java Virtual
	 * Machine whose version precedes 1.5
	 * @see #indentCurrentParagraph(int)
	 */
	/* JVM v.1.5 simply processes tabs according to the
	 * number currently present, and indentCurrentParagraph methods operate 
	 * accordingly:
	 * both methods assume that the tab character change has already been 
	 * accomplished.  Tab deletions in JVM < v.1.5 appear not to be processed 
	 * until the KeyListener methods finish, requiring a the tab count
	 * in these methods to be decremented.
	 * Rather than create two separate indentation methods, a single indent
	 * method now assumes that the tabs have been processed but also allows
	 * for a parameter to flag when a tab deletion takes place in JVM < v.1.5. 
	 */
	public void indentCurrentParagraph(
		int tabChars,
		boolean decrementTab) {
		String s = getAllText();
		int start = LibTTx.reverseIndexOf(s, "\n", getCaretPosition()) + 1;
//		System.out.println("caret position: " + getCaretPosition());
//		if (start == -1) start = 0;
		int tabs = leadingTabsCount(s, start);
		// the tab has already been deleted in JVM >= v.1.5;
		// tabs should equal the final number of tab characters, so tabs
		// must be decremented in JVM < v.1.5
		if (decrementTab) {
			--tabs;
		}
		int end = s.indexOf("\n", start);
		indent(tabChars, tabs, start, (end == -1 ? s.length() : end + 1) - start);
	}

	/** Indents the current paragraph, no matter where the caret is within it.
	 * Renders tabs as spaces, but indents the entire region according to the 
	 * currently set number of spaces per tab.  
	 * Assumes that either an indent, as opposed to an unindent,
	 * has occurred, or that the Java Virtual Machine is >= v.1.5, or both. 
	 * @see #indentCurrentParagraph(int, boolean)
	 * @see #indentCurrentParagraph(int)
	 */
	public void indentCurrentParagraph() {
		indentCurrentParagraph(getTabSize(), false);
	}

	/** Indents the current paragraph, no matter where the caret is within it.
	 * Renders tabs as spaces, but indents the entire region a given number of spaces
	 * per tab.  Assumes that either an indent, as opposed to an unindent,
	 * has occurred, or that the Java Virtual Machine is >= v.1.5, or both. 
	 * @param tabChars number of spaces for each tab to represent
	 * @see #indentCurrentParagraph(int, boolean)
	 */
	public void indentCurrentParagraph(int tabChars) {
		indentCurrentParagraph(tabChars, false);
	}

	/** Determines whether the tab is at the start of a given line.
	 * The tab must be either at the head of the line or connected to it by a continuous string
	 * of tabs.  Useful to prevent inner tabs from influencing indents.
	 * @return <code>true</code> if the tab is a leading tab
	 */
	public boolean isLeadingTab() {
		int i = getCaretPosition() - 1;
		try {
			while (i > 0 && getDocument().getText(i, 1).equals("\t"))
				i--;
			return i == 0 || i >= 0 && getDocument().getText(i, 1).equals("\n");
		} catch (BadLocationException e) {
			System.out.println("Can't find no tabs, dude.");
			return false;
		}
	}
	

	/**Gets the value showing whether the text in the text area
	 * has changed.
	 * @return <code>true</code> if the text has changed;
	 * <code>false</code> if otherwise
	 */
	public boolean getChanged() {
		return changed;
	}

	/**Sets the value showing whether the text in the text
	 * area has changed.
	 * @param b <code>true</code> if the text has changed;
	 * <code>false</code> if otherwise
	 */
	public void setChanged(boolean b) {
		changed = b;
	}

	/**Gets the file's path.
	 * @return path, formatted for the system and streamlined
	 */
	public String getPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	/**Gets the file in the text pad.
	 * 
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**Gets the file's name.
	 * @return filename
	 */
	public String getFilename() {
		return file.getName();
	}

	/**Gets the path to the file's directory.
	 * @return path to directory
	 */
	public String getDir() {
		String dir = "";
		return ((dir = file.getParent()) != null) ? dir : "";
	}

	/**Gets the auto-indent selection.
	 * @return <code>true</code> if auto-indent is selected.
	 */
	public boolean getAutoIndent() {
		return autoIndent;
	}

	/**Sets the file to a file object.
	 * @param aFile file to hold the text area's contents.
	 */
	public void setFile(File aFile) {
		file = aFile;
	}

	/**Sets the file to a path.
	 * @param path path of file to save to
	 */
	public void setFile(String path) {
		file = new File(path);
	}

	/**Execute an edit, capture the state changes, and make the changes
	 * undoable.
	 * @param text text to edit
	 *
	public void setUndoableText(String text) {
		StateEdit stateEdit = new StateEdit(this);
		setText(text);
		stateEdit.end();
		undoManager.addEdit((UndoableEdit) stateEdit);
	}
	
	
	public StateEdit startUndoableEdit() {
		return new StateEdit(this);
	}
	
	public void endUndoableEdit(StateEdit stateEdit) {
		stateEdit.end();
		undoManager.addEdit((UndoableEdit) stateEdit);
	}
	*/

	/**Sets the auto-indent selection.
	 * @param b <code>true</code> to auto-indent
	 */
	public void setAutoIndent(boolean b) {
		if (autoIndent = b) {
			setNoTabs();
			setIndentTabs(getTabSize());
		} else {
			setDefaultTabs(getTabSize());
		}
	}

	/**Check whether the file has been created.
	 * @return <code>true</code> if the file has been created;
	 * <code>false</code> if otherwise
	 */
	public boolean fileExists() {
		return file.exists();
	}

	/**Undoes the last text change.
	 * May need to make include the goofy features' text manipulations.
	 */
	public void undo() {
		if (undoManager.canUndo())
			undoManager.undo();
	}

	/**Redoes the last undone text change.
	 * May need to make include the goofy features' text manipulations
	 */
	public void redo() {
		if (undoManager.canRedo())
			undoManager.redo();
	}

	/**Applies the current <code>UndoManager</code> as well as the tab size, 
	 * if appropriate.
	 */
	public void applyDocumentSettings() {
		Document doc = getDocument();
		doc.addUndoableEditListener(undoManager);
		if (autoIndent) {
			setIndentTabs(getTabSize());
			setNoTabs();
		} else {
			setDefaultTabs(getTabSize());
		}
	}

	/**Tells whether the pad has any characters in it.
	 * @return boolean <code>true</code> if the pad is empty
	 */
	public boolean isEmpty() {
		String text = getText();
		return ((text == null) || !(new StringTokenizer(text)).hasMoreTokens())
			? true
			: false;
	}

	/** Gets all the current text.
	 * 
	 * @return the text
	 */
	public String getAllText() {
		try {
			Document doc = getDocument();
			int len = doc.getLength();
			return doc.getText(0, len);
		} catch (BadLocationException e) {
			System.out.println("Couldn't get it all.");
			return "";
		}
	}

	public void replaceAllText(String s) {
		try {
			//	    System.out.println(s);
			Document doc = getDocument();
			int len = doc.getLength();
			doc.remove(0, len);
			doc.insertString(0, s, null);
		} catch (BadLocationException e) {
			System.out.println("Couldn't replace it all.");
		}
	}

	/**Converts the pad to a plain text view.
	 * Undoable.
	 */
	public void viewPlain() {
		StateEdit stateEdit = new StateEdit(this);
		String text = getText();
		setEditorKit(createDefaultEditorKit()); // revert to default editor kit
		// use the default editor kit's <code>DefaultStyledDocument</code>
		setDocument(getEditorKit().createDefaultDocument());
		setText(text);
		applyDocumentSettings();
		stateEdit.end();
		undoManager.addEdit((UndoableEdit) stateEdit);
	}

	/**Converts the pad to an HTML text view.
	 * The underlying text may have HTML tags added.
	 */
	public void viewHTML() {
		StateEdit stateEdit = new StateEdit(this);
		String text = getText();
		setDocument(getEditorKit().createDefaultDocument());
		setContentType("text/html");
		setText(text);
		applyDocumentSettings();
		stateEdit.end();
		undoManager.addEdit((UndoableEdit) stateEdit);
	}

	/**Converts the pad to an RTF tex view, if possible.
	* If the document is not in RTF format, the pad reverts to its original setting.
	*/
	public void viewRTF() {
		StateEdit stateEdit = new StateEdit(this);
		String text = getText();
		setDocument(getEditorKit().createDefaultDocument());
		setContentType("text/rtf");
		setText(text);
		applyDocumentSettings();
		stateEdit.end();
		undoManager.addEdit((UndoableEdit) stateEdit);
		if (getText() == null) {
			undo();
		}
	}

	/**Gets the index of the current word's first character.
	 * @return string index of either the current word or the previous
	 * word if only delimiters intervene between the word and
	 * the caret position.
	 */
	public int getWordPosition() {
		int caretPos = getCaretPosition(); // current caret position
		int newCaretPos = caretPos - 1; // moving caret position
		String delimiters = " .,;:-\\\'\"/_\t\n";

		try {
			while (newCaretPos > 0
				&& delimiters.indexOf(getDocument().getText(newCaretPos, 1))
					!= -1) {
				newCaretPos--;
			}
			while (newCaretPos > 0
				&& (delimiters.indexOf(getDocument().getText(newCaretPos - 1, 1))
					== -1)) {
				newCaretPos--;
			}
		} catch (BadLocationException e) {
			System.out.println("Ack!  Can't find the beginning of the word.");
		}

		return (newCaretPos <= 0) ? 0 : newCaretPos;
	}

	/**Gets the index of the next word's first character.
	 * @return string index of the next word's beginning, past
	 * any delimiters
	 */
	public int getNextWordPosition() {
		int caretPos = getCaretPosition(); // current caret position
		int newCaretPos = caretPos; // moving caret position
		int textLen = getText().length(); // end of text
		String delimiters = " .,;:-\\\'\"/_\t\n";

		// check that new caret not at end of string; 
		// skip forward as long as not delimiters
		try {
			while (newCaretPos < textLen
				&& delimiters.indexOf(getDocument().getText(newCaretPos, 1))
					== -1) {
				newCaretPos++;
			}
			// check that new caret not at end of string; 
			// now skip forward as long as delimiters; bring to next letter, etc
			while (newCaretPos < textLen
				&& (delimiters.indexOf(getDocument().getText(newCaretPos, 1))
					!= -1)) {
				newCaretPos++;
			}
		} catch (BadLocationException e) {
			System.out.println("Arrgh!  Can't find the beginning of the word.");
		}

		return newCaretPos;
	}

	/**Auto-indent to the previous line's tab position.
	 */
	public void autoIndent() {
		// go back 2 to skip the hard return that makes the new line
		// requiring indentation
		try {
			int n = 0;
			int tabs = 0;
			
			// finds the previous newline that created the previous paragraph
			for (n = getCaretPosition() - 2;
				n >= 0 && !getDocument().getText(n, 1).equals("\n");
				n--);
//				System.out.println("n char: " + getDocument().getText(n, 1)); }
			/*
			if (n == 0)
				n = -1;
			*/
			// counts the number of tabs at the start of the previous paragraph
			for (tabs = 0; getDocument().getText(++n, 1).equals("\t"); tabs++);
//			System.out.println("tab count: " + tabs);

			// construct a string of all the tabs
			StringBuffer tabStr = new StringBuffer(tabs);
			for (int i = 0; i < tabs; i++) {
				tabStr.append('\t');
			}
			// add the tabs
			getDocument().insertString(
				getCaretPosition(),
				tabStr.toString(),
				null);
		} catch (BadLocationException e) {
			System.out.println(
				"Insert location " + getCaretPosition() + " does not exist");
		}
	}

	/**Fills an array with the component's possible actions.
	 * @param txtComp <code>Java Swing</code> text component
	 */
	private void createActionTable(JTextComponent txtComp) {
		imap.clear();
		amap.clear();
		actions = new Hashtable();
		Action actionsArray[] = txtComp.getActions();
		for (int i = 0; i < actionsArray.length; i++) {
			Action a = actionsArray[i];
			actions.put(a.getValue(Action.NAME), a);
		}
	}

	/**Gets an action from a hash table when give the name of the action.
	 * @param name name of action
	 * @return action
	 */
	private Action getActionByName(String name) {
		return (Action) (actions.get(name));
	}

	/**Stores the editor kit and document during a state change.
	 * Called at both the start and end of the state change for separate hashtables.
	 * @param state table storing the object's current state
	 */
	public void storeState(Hashtable state) {
		state.put("editorkit", getEditorKit());
		state.put("doc", getStyledDocument());
	}

	/**Restores the editor kit and document from the table's stored settings.
	 * Called from an undo event.
	 * @param state tabled of stored settings
	 */
	public void restoreState(Hashtable state) {
		EditorKit editorKit = (EditorKit) state.get("editorkit");
		StyledDocument doc = (StyledDocument) state.get("doc");
		if (editorKit != null)
			setEditorKit(editorKit);
		// set document after setting the editor kit so can apply the doc's settings
		if (doc != null)
			setDocument(doc);
	}

	/**Pastes in text, auto-indenting it as necessary.
	 * If the text pad is in auto-indent mode, the pasted text is graphically
	 * indented to match each of its tabbed paragraphs.
	 * 
	 */
	/* The JVM automatically captures ctrl-V paste events, overriding
	 * any ctrl-V paste shortcut.  To add events to a paste action,
	 * the actual paste method in the text pane needs to be overridden.
	 * Here the new method checks for auto-indentation and graphically
	 * indents the newly spliced in paragraphs as necessary.
	 * 
	 */
	public void paste() {
		int start = getCaretPosition(); // to mark beginning of pasted region
		super.paste();
		
		// refreshes the graphical indents in the pasted region
		if (autoIndent) {
			int end = getCaretPosition(); // marks end of region
			int prevBreak = end; // position of next preceding "\n"
			String text = getAllText(); // pad's text
			// indents each paragraph in pasted region individually;
			// stops once finds a hard return outside the region
			do {
				// indentCurrentParagraph relies on caret position
				setCaretPosition(prevBreak); 
				indentCurrentParagraph();
			} while (
				start
					< (prevBreak = LibTTx.reverseIndexOf(text, "\n", prevBreak)));
			setCaretPosition(end); // returns caret position to end of region
		}
	}
	
	/**Starts a compound edit sequence, which tracks multiple edits to
	 * allow them to be undone in one fell swoop.
	 * Does nothing if a compound edit is already in progress.
	 * 
	 * @see #stopCompoundEdit()
	 */
	public void startCompoundEdit() {
		if (!isCompoundEditing()) {
			compoundEdit = new CompoundEdit();
			compoundEditing = true;
		}
	}
	
	/**Stores a compound edit sequence in the undo manager.
	 * 
	 * @see #startCompoundEdit()
	 */
	public void stopCompoundEdit() {
		compoundEdit.end();
		compoundEditing = false;
		undoManager.addEdit(compoundEdit);
	}
	
	
	
	
	
	
	
	
	
	
	/**Checks the value of the auto-indent flag.
	 * 
	 * @return <code>true</code> if the pad is flagged to auto-indent
	 */
	public boolean isAutoIndent() {
		return autoIndent;
	}
	/**Checks the value of the compound edit flag.
	 * 
	 * @return <code>true</code> if the text pad is currently tracking
	 * a compound edit
	 */
	public boolean isCompoundEditing() {
		return compoundEditing;
	}
	
	
	
	
	

	/** Sets the number of characters that each tab represents.
	 * The tab is still represented by a '/t' rather than the given number
	 * of characters; this setting is merely for display purposes.
	 * 
	 * @param aTabSize number of characters
	 */
	public void setTabSize(int aTabSize) {
		tabSize = aTabSize;
	}

	/** Gets the number of characters that each tab represents.
	 * 
	 * @return the number of characters
	 * @see #setTabSize(int)
	 */
	public int getTabSize() {
		return tabSize;
	}

	/**Creates a print pad for this <code>TextPad</code> object.
	 * The text pad needs to create its own print pad so that the 
	 * text pad can pass its contents array of each visible line
	 * and also pass the current font. 
	 * @return the print pad, including the current text, broken
	 * up according to the visible, soft breaks in the 
	 * <code>TextPad</code>, and the current font
	 */
	public PrintPad createPrintPad() {
		return new PrintPad(
			LibTTx.getVisibleLines(this),
			new Font(getFont().getAttributes()));
	}

	/**Gets the current auto-save timer.
	 * <code>TextTrix</code> attaches a timer to each <code>TextPad</code>
	 * with changed content if the user has enabled the feature.
	 * @return the auto-save timer
	 */
	public StoppableThread getAutoSaveTimer() {
		return autoSaveTimer;
	}
	/**Sets the current auto-save timer.
	 * <code>TextTrix</code> attaches a timer to each <code>TextPad</code>
	 * with changed content if the user has enabled the feature.
	 */
	public void setAutoSaveTimer(StoppableThread aAutoSaveTimer) {
		autoSaveTimer = aAutoSaveTimer;
	}
	
	
	
	
	
	
	
	
	
	
	/** Subclass of the <code>UndoManager</code>.
	 * Allows the code to ignore undos.
	 * @author davit
	 */
	private class UndoManagerTTx extends UndoManager {
		private boolean ignoreNextStyleChange = false;

		/** Sublcassed method to add ability to ignore undos when flagged.
		 * If <code>ignoreNextStyleChange</code> is set to <code>true</code>,
		 * the method doesn't call the superclass' corresponding method.
		 */
		public synchronized boolean addEdit(UndoableEdit anEdit) {
			// check if undoable
			if (anEdit instanceof AbstractDocument.DefaultDocumentEvent) {
				AbstractDocument.DefaultDocumentEvent de =
					(AbstractDocument.DefaultDocumentEvent) anEdit;
				// ignore if flag set and a change event
				if (de.getType() == DocumentEvent.EventType.CHANGE
					&& ignoreNextStyleChange) {
					ignoreNextStyleChange = false; // reset the ignore flag
					return false;
				}
			}
			// call superclass' method
			return super.addEdit(anEdit);
		}
		
		/**Subclassed method to check for compound edits.
		 * If a compound edit is in progress, the edit is added to the 
		 * compound edit object; otherwise, the method operates as normally.
		 * 
		 * @param evt the edit event
		 */
		public void undoableEditHappened(UndoableEditEvent evt) {
			if (isCompoundEditing()) {
				compoundEdit.addEdit(evt.getEdit());
			} else {
				super.undoableEditHappened(evt);
			}
		}

		/** Flags the manager to ignore the next change in style.
		 * For example, stylistic auto-indentation indents should maybe be ignored
		 * @param b <code>true</code> to ignore the next style change
		 */
		public void setIgnoreNextStyleChange(boolean b) {
			ignoreNextStyleChange = b;
		}

		/** Gets the state of the flag for ignoring the next style change.
		 * 
		 * @return <code>true</code> if the next style will be ignored
		 */
		public boolean getIgnoreNextStyleChange() {
			return ignoreNextStyleChange;
		}
	}

}

