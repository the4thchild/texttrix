/* Txtrx.java - standalone command-line version of Text Trix
 * 
 * Text Trix
 * Meaningful Mistakes
 * http://texttrix.sourceforge.net
 * http://sourceforge.net/projects/texttrix
 *
 * Copyright (c) 2002, David Young
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Text Trix nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package net.sourceforge.texttrix;

import java.io.*;

/**The command-line version of Text Trix.
 * Takes an arguments specifying the practical or goofy functions to apply
 * to the subsequent list of text files.
 * If no command is specified, defaults to "verbose", which simply
 * outputs the file on screen.
 */
public class Txtrx {

	public Txtrx() {
	}

	/**Invokes Txtrx and filters the parameters.
	 * @param args array of the commands with a leading dash,
	 * followed by the filenames to manipulate
	 */
	public static void main(String[] args) {
		// more than 1 argument or a single, non-command argument
		if (args.length > 1 || 
				(args.length == 1 
				 && (!args[0].startsWith("-") || args[0].startsWith("--")))) {
			applyCmds(args);
		// only argument is the commands
		} else if (args.length == 1) {
			System.out.println("Please supply files to goof with.");
		} else {
			// likely want to replace by automatically calling --help
			System.out.println("Type \"txtrx --help\" for more information.");
		}
	}

	/**Applies the selected commands to the given files.
	 * Commands are preceded by a dash, "-", and files come afterware.
	 * Specifying no commands defaults to the "v" command, "verbose" operation.
	 * @param args array of commands and files.  Commands are optional
	 * and come before the filenames.  Assumes that args specifies at least
	 * one file.  Also takes care of all full-word, "--"-preceded commands.
	 */
	public static void applyCmds(String[] args) {
		String cmds = args[0];
		String cmd = "";
		boolean verbose = false;
		int fileIndex = 1;
		
		// one or more args: files
		if (!cmds.startsWith("-")) {
			verbose = true;
			cmd = "v";
			fileIndex = 0;
		// show help for the list of commands.
		// Not in a separate file so can combine Txtrx into 1 file total
		} else if (cmds.indexOf("h") != -1 || cmds.equals("--help")) {
			ShowText.showHelp();
			fileIndex = args.length;
		// show license info
		} else if (cmds.equals("--license")) {
			ShowText.showLicense();
			fileIndex = args.length;
		// show version info
		} else if (cmds.equals("--version")) {
			ShowText.showVersion();
			fileIndex = args.length;
		// unrecognized full-word command
		} else if (cmds.startsWith("--")) {
			System.out.println("I'm sorry, " + cmds + " was not recognized.");
		// one or more args: commands that include "v", files
		} else if (cmds.indexOf("v") != -1) {
			verbose = true;
//			cmds = removeSubstring(cmds, "-");
//			cmds = removeSubstring(cmds, "v");
			cmds = cmds.replace('-', '\0');
			cmds = cmds.replace('v', '\0');
		} else {
			cmds = cmds.replace('-', '\0');
//			cmds = removeSubstring(cmds, "-");
		}
	
		for (int i = fileIndex; i < args.length; i++) {
			String path = args[i];
			String text;
			
			try {
				File file = new File(path);
				if (file.canRead()) {
					BufferedReader reader =
						new BufferedReader(new FileReader(path));
					text = readFile(reader, path);
					reader.close();
					if (cmd == "v") {
						System.out.println(text);
					} else {
						while (cmds != null) {
							if (cmds.length() > 1) {
								cmd = cmds.substring(0, 1);
								text = applyCmd(cmd, text);
								cmds = cmds.substring(1);
							} else {
								text = applyCmd(cmds, text);
								cmds = null;
							}
						}
						
						if (file.canWrite()) {
							String bakPath = path + "~";
							file.renameTo(new File(bakPath));
							writeFile(text, path);
							if (verbose) {
								System.out.println(text);
								System.out.println('\n' + path
										+ "has been backed up to " 
										+ bakPath);
							}
						} else {
							System.out.println(path + " cannot be modified");
						}
					}
				} else {
					System.out.println(path + " is not readable");
				}
			} catch(IOException e) {
				System.out.println(path + " is not a file");
			}	
		}
	}

	
	/**Applies a single command to a given string.
	 * @param cmd goofy or practical command to apply:
	 * "r" is the Extra Hard Return Remover (practical function),
	 * "h" is the HTML tag replacer (practical function).
	 * @param text text to modify
	 */
	public static String applyCmd(String cmd, String aText) {
		String text = aText;

		// extra hard Return remover
		if (cmd.equals("r")) {
			return Practical.removeExtraHardReturns(text);
		// Html replacer
		} else if (cmd.equals("h")) {
			return Practical.replaceHTMLTags(text);
		// unrecognized command
		} else {
			System.out.println("I'm sorry, -" + cmd + " was not recognized.");
			return text;
		}
	}

	/**Extracts the contents of a text stream.
	 * @param reader buffered stream for text input from a file
	 * @return the file's text as a string
	 */
	public static String readFile(BufferedReader reader, String path) {
		try {
			String text = "";
			String line;
			while ((line = reader.readLine()) != null) {
				text = text + line + "\n";
			}
			return text;
		} catch(IOException e) {
			System.out.println(path + " is apparently not a text file");
			return "";
		}
	}

	/**Writes text to a file.
	 * @param text the text to write to the file
	 * @param path the file's path to write to
	 */
	public static void writeFile(String text, String path) {
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(path), true);
			writer.print(text);
			writer.close();
		} catch(IOException e) {
			System.out.println(path + " is not accessible");
		}
	}
}
