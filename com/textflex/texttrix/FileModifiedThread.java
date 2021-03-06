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
 * Portions created by the Initial Developer are Copyright (C) 2002-10, 2018
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
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.undo.*;
import javax.swing.text.*;
import java.io.*;
import javax.swing.event.*;

/**
 * File modification poller.  Flags files that have been opened in Text Trix
 * but modified elsewhere, such as source code that is open in the editor
 * but which gets updated by a concurrent version update.
 */
public class FileModifiedThread extends StoppableThread {
	private Thread thread = null;
	private long lastModifiedWithTTx = 0;
	private File file = null;
	private TextPad pad = null;
	private boolean prompt = true;
	
	public FileModifiedThread(TextPad aPad, boolean aPrompt) {
		pad = aPad;
		prompt = aPrompt;
	}
	
	public void start() {
		setStopped(false);
//		System.out.println("starting file modification thread");
		thread = new Thread(this, "thread");
		thread.start();
	}
	
	// refresh the tab, prompting the user beforehand for confirmation if 
	// necessary
	private Runnable refreshTab = new Runnable() {
		public void run() {
			TextPad textPad = getTextPad();
			// skip if file doesn't exist b/c will ask for file
			// name later, when can still cancel the save
			if (textPad.getFile().exists()) {
				long lastMod = textPad.getFile().lastModified();
				if (lastMod != lastModifiedWithTTx) {
					String lastModTime = LibTTx.calcTime(lastMod);
					String currModTime = LibTTx.calcTime(lastModifiedWithTTx);
					System.out.println("file last modified: " 
							+ lastModTime + ", current TTx file last modified: "
							+ currModTime);
					requestStop();
					boolean refresh = true;
					if (prompt) {
						// prompt with save dialog
						int choice = JOptionPane.showConfirmDialog(
								textPad,
								textPad.getFile().getPath() 
										+ "\nhas been modified elsewhere."
										+ "\nRefresh the file?",
								"File Modified Elsewhere",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);
						refresh = choice == JOptionPane.YES_OPTION;
					}
					if (refresh) {
						textPad.refresh();
						// replace this object with a new one rather than 
						// simply replacing thread since otherwise could 
						// potentially end up with 2 concurrently running 
						// threads
						FileModifiedThread modThread = new FileModifiedThread(
								pad, prompt);
						modThread.setLastModifiedWithTTx(lastMod);
						pad.setFileModifiedThread(modThread);
						modThread.start();
					}
				}
			}
		}
	};
	
	public void run() { 
		while(!isStopped()) {
			interrupted(); // clears any interrupt during a previous run
	//		System.out.println("starting to check file modification on " + file.getName());
			try {
				sleep(5000);
				// don't need getPrefs().getAutoSave() && b/c only start
				// timer if auto-save pref set, and interrupt already called
				// if unset while timer running
				if (!interrupted()) {
					// to avoid breaking the single thread rule, invokeLater
					// runs all of the UI code to ensure that it synchronizes
					// with events in the main dispatch thread
					EventQueue.invokeLater(refreshTab);
				}
			} catch (InterruptedException e) {
				// ensures that an interrupt during the sleep is still flagged
				setStopped(true);
				Thread.currentThread().interrupt();
//				System.out.println("file modification thread interrupted");
			}
		}
	}
	
	/**
	 * Requests the thread to stop by setting the <code>stopped</code>
	 * flag, interrupting the running thread, and setting the current thread
	 * to <code>null</code>. Note that the <code>StoppableThread</code>
	 * object remains, only the <ocde>Thread</code> object created at the
	 * starting of the <code>StoppableThread</code> is destroyed
	 *  
	 */
	public void requestStop() {
		setStopped(true);
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}
	
	
	
	public void setLastModifiedWithTTx(long aLastModifiedWithTTx) {
		lastModifiedWithTTx = aLastModifiedWithTTx;
	}
	
	public void setTextPad(TextPad aPad) {
		pad = aPad;
	}
	
	public void setPrompt(boolean val) {
		prompt = val;
	}
	
	
	
	public TextPad getTextPad() {
		return pad;
	}
	
	public long getLastModifiedWithTTx() {
		return lastModifiedWithTTx;
	}
	
	public FileModifiedThread getThis() {
		return this;
	}
	
}

