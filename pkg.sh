#!/bin/sh
# ***** BEGIN LICENSE BLOCK *****
# Version: MPL 1.1/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Mozilla Public License Version
# 1.1 (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# http://www.mozilla.org/MPL/
#
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
# for the specific language governing rights and limitations under the
# License.
#
# The Original Code is Text Trix code.
#
# The Initial Developer of the Original Code is
# Text Flex.
# Portions created by the Initial Developer are Copyright (C) 2003-4
# the Initial Developer. All Rights Reserved.
#
# Contributor(s): David Young <dvd@textflex.com>
#
# Alternatively, the contents of this file may be used under the terms of
# either the GNU General Public License Version 2 or later (the "GPL"), or
# the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the MPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the MPL, the GPL or the LGPL.
#
# ***** END LICENSE BLOCK *****

# Text Trix packager

HELP="
Packages both binary and source code archives of Text Trix.

Syntax:
	pkg.sh [ --java java-compiler-binaries-path ] [ --help ]
(\"sh \" might need to precede the command on the same line, in case
the file pkg.sh does not have executable permissions.)

Parameters:
	--java java-compiler-binaries-path: Specifies the path to javac, 
	jar, and other Java tools necessary for compilation.  
	Alternatively, the JAVA variable in pkg.sh can be hand-edited 
	to specify the path, which would override any command-line 
	specification.
	
	--help: Lends a hand by displaying yours truly.
	
Copyright:
	Copyright (c) 2003-4 Text Flex

Last updated:
	2004-05-28
"

##############################
# User-defined variables
# Check them!
##############################

VER="0.3.5" # version info
DEST="/home/share" # final destination
JAVA="" # compiler
BASE_DIR="" # source directories

##############################
# System setup
##############################
SYSTEM=`uname -s`
CYGWIN="false"
if [ `expr "$SYSTEM" : "CYGWIN"` -eq 6 ]
then
	CYGWIN="true"
fi
READ_PARAMETER=0
for arg in "$@"
do
	if [ $READ_PARAMETER -eq 1 ]
	then
		if [ "x$JAVA" = "x" ]
		then
			# no output b/c assuming plug.sh will be called
			JAVA="$arg"
		fi
		READ_PARAMETER=0
	fi
	if [ "x$arg" = "x--help" -o "x$arg" = "x-h" ]
	then
		echo "$HELP"
		exit 0
	elif [ "x$arg" = "x--java" ]
	then
		READ_PARAMETER=1
	fi
done

if [ `expr index "$JAVA" "/"` -ne ${#JAVA} ]
then
	JAVA="$JAVA"/
fi

# Determine the base dir if not specified above
if [ "x$BASE_DIR" = "x" ] # continue if BASE_DIR is empty string
then
	if [ `expr index "$0" "/"` -eq 1 ] # use script path if absolute
	then
		BASE_DIR="$0"
	else # assume that script path is relative to current dir
		BASE_DIR="$PWD/$0"
	fi
	BASE_DIR="${BASE_DIR%/texttrix/pkg.sh}" # set base dir to main Text Trix dir
fi
BLD_DIR="$BASE_DIR/build" # initial output directory
TTX_DIR="$BASE_DIR/texttrix" # root directory of main Text Trix source files
PLGS_DIR="$BASE_DIR/plugins" # root directory of Text Trix plug-in source files

##############################
# Build operations
##############################
NAME="texttrix" # UNIX program name
DIR="com/textflex/$NAME" # Java package directory structure
PKGDIR="$NAME-$VER" # name of binary package
PKG=$PKGDIR.zip # name of compressed binary package
SRCPKGDIR="$PKGDIR-src" # name of source package
SRCPKG="$SRCPKGDIR.zip" # name of compressed package of source
JAR="TextTrix.jar" # executable jar
ALL="$PKGDIR $PKG $SRCPKGDIR $SRCPKG"

if [ ! -d $BLD_DIR ]
then
	mkdir $BLD_DIR
fi
cd $BLD_DIR # base of operations

##########
# Packaging

echo "Packaging the files..."

# remove old build packages and set up new ones
rm -rf $PKGDIR $PKG $SRCPKGDIR $SRCPKG
mkdir $PKGDIR
mkdir $PKGDIR/plugins
cp -rf $TTX_DIR/com $TTX_DIR/readme.txt $TTX_DIR/readme-src.txt \
	$TTX_DIR/todo.txt $TTX_DIR/changelog.txt \
	$TTX_DIR/$DIR/license.txt $TTX_DIR/logo.ico $PKGDIR

# create the master package, which will eventually become the binary package
cd $PKGDIR
# remove unnecessary files and directories
rm -rf com/CVS com/textflex/CVS $DIR/CVS $DIR/images/CVS $DIR/images/bak $DIR/*~
# make files readable in all sorts of systems
unix2dos *.txt $DIR/*.txt
chmod 664 *.txt $DIR/*.txt # prevent execution of text files

# create the source package from the master package
cd $BLD_DIR
cp -rf $PKGDIR texttrix # master --> one folder within source package
mkdir $SRCPKGDIR # create empty source package to hold copy of master
mv texttrix $SRCPKGDIR # copy to source package
cp $TTX_DIR/plug.sh $TTX_DIR/pkg.sh \
	$TTX_DIR/manifest-additions.mf \
	$SRCPKGDIR/texttrix # copy scripts to source pkg
cp $TTX_DIR/plugins/*.jar $PKGDIR/plugins # only want jars in binary package
cp -rf $PLGS_DIR $SRCPKGDIR/plugins
rm $PKGDIR/readme-src.txt # remove source-specific files for binary package

# create binary package

# create binaries
cd $BLD_DIR/$PKGDIR
# self-executable jar via "java -jar [path to jar]/$JAR.jar", where $JAR is named above
if [ "$CYGWIN" = "true" ]
then
	"$JAVA"jar -cvfm $JAR "`cygpath -p -w $TTX_DIR/manifest-additions.mf`" $DIR/*.class $DIR/*.txt $DIR/images/*.png
else
	"$JAVA"jar -cvfm $JAR "$TTX_DIR/manifest-additions.mf" $DIR/*.class $DIR/*.txt $DIR/images/*.png
fi
rm -rf com

# convert master package to binary one
cd $BLD_DIR/$SRCPKGDIR # remove all but source and related files
rm -rf texttrix/$DIR/*.class
# remove non-source or src-related files from plugins;
# assumes that all the directories in the "plugins" are just that--plugins
rm -rf plugins/CVS plugins/*/CVS plugins/*/com/CVS plugins/*/com/textflex/CVS \
	plugins/*/$DIR/CVS plugins/*/$DIR/*.class plugins/*/$DIR/*~ \
	plugins/*.jar
mv texttrix/*.txt .

# Add PKGDIR-specific files
cp $TTX_DIR/$DIR/images/minicon-32x32.png $BLD_DIR/$PKGDIR/icon.png

# zip up and move to destination
cd $BLD_DIR
cp $PKGDIR/$JAR $TTX_DIR
zip -r $PKG $PKGDIR
zip -r $SRCPKG $SRCPKGDIR
echo ""
if [ -d "$DEST" ]
then
	rm -rf $DEST/$PKGDIR*
	mv $ALL $DEST
	cd $DEST
	echo "Packages output to $DEST"
else
	echo "Packages output to $BLD_DIR"
fi
ls -l $ALL
#sh $WIN_MOUNT

exit 0
