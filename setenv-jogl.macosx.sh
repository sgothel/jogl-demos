#! /bin/sh

if [ -z "$1" ] ; then
    echo "Usage: $0 {JOGL_ALL|JOGL_ES1_MIN|JOGL_ES1_MAX|JOGL_ES2_MIN|JOGL_ES2_MAX|JOGL_GL2ES12_MIN|JOGL_GL2ES12_MAX|JOGL_GL2_MIN|JOGL_GL2_MAX}"
else

JOGL_PROFILE=$1
shift

echo JOGL PROFILE: $JOGL_PROFILE

CVSROOT=":pserver:sgoethel@cvs.dev.java.net:/cvs"
THISDIR=`pwd`
export CVSROOT THISDIR

if [ -x /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi

J2RE_HOME=$(which java)
JAVA_HOME=$(which javac)
CP_SEP=:

export LIBXCB_ALLOW_SLOPPY_LOCK=1

. $THISDIR/../jogl/etc/profile.jogl $THISDIR/../jogl/build-macosx $JOGL_PROFILE

GLUEGEN_JAR=$THISDIR/../gluegen/build-macosx/gluegen-rt.jar
GLUEGEN_OS=$THISDIR/../gluegen/build-macosx/obj

LIB=$THISDIR/lib

# CLASSPATH=$JAVA_HOME/jre/lib/rt.jar:.:build-macosx/classes
CLASSPATH=.:$THISDIR/build-macosx/jogl-demos.jar:$THISDIR/build-macosx/jogl-demos-util.jar:$THISDIR/build-macosx/jogl-demos-data.jar:$GLUEGEN_JAR:$JOGL_CLASSPATH
for i in $LIB/*jar ; do
    CLASSPATH=$CLASSPATH:$i
done
export CLASSPATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR

echo REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true" for running demos without AWT, e.g. NEWT
echo JOGL_CLASSPATH: $JOGL_CLASSPATH

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH

fi


