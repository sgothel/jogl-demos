#! /bin/sh

function print_usage() {
    echo "Usage: $0 {JOGL_ALL|JOGL_ALL_NOAWT|JOGL_ALL_MOBILE} [jogl-build-dir]"
}

if [ -z "$1" ] ; then
    echo JOGL PROFILE missing
    print_usage
    return
fi

JOGL_PROFILE=$1
shift

JOGL_BUILDDIR=
if [ ! -z "$1" ] ; then
    JOGL_BUILDDIR=$1
    shift
fi

THISDIR=`pwd`
AUTOBUILD=0

if [ -e "$JOGL_BUILDDIR" ] ; then
    JOGL_DIR=$JOGL_BUILDDIR/..
    JOGL_BUILDDIR_BASE=`basename $JOGL_BUILDDIR`
else
    AUTOBUILD=1
    jpf=`find jogl/etc -name profile.jogl`
    if [ -z "$jpf" ] ; then
        jpf=`find . -name profile.jogl`
    fi
    if [ -z "$jpf" ] ; then
        echo JOGL_DIR not found
        echo JOGL_BUILDDIR $JOGL_BUILDDIR not exist or not given
        print_usage
        return
    fi
    JOGL_DIR=`dirname $jpf`/..
    JOGL_BUILDDIR=$JOGL_DIR/lib
    JOGL_BUILDDIR_BASE="."
fi

if [ $AUTOBUILD -eq 0 ] ; then
    gpf=`find ../gluegen/make -name dynlink-unix.cfg`
    if [ -z "$gpf" ] ; then
        gpf=`find .. -name dynlink-unix.cfg`
    fi
    if [ -z "$gpf" ] ; then
        echo GLUEGEN_BUILDDIR not found
        print_usage
        return
    fi
    GLUEGEN_DIR=`dirname $gpf`/..
    GLUEGEN_BUILDDIR=$GLUEGEN_DIR/$JOGL_BUILDDIR_BASE
    if [ ! -e "$GLUEGEN_BUILDDIR" ] ; then
        echo GLUEGEN_BUILDDIR $GLUEGEN_BUILDDIR does not exist
        print_usage
        return
    fi
    GLUEGEN_JAR=$GLUEGEN_BUILDDIR/gluegen-rt.jar
    GLUEGEN_OS=$GLUEGEN_BUILDDIR/obj
    JUNIT_JAR=$GLUEGEN_DIR/make/lib/junit-4.5.jar
else
    GLUEGEN_BUILDDIR=$JOGL_BUILDDIR
    GLUEGEN_JAR=$JOGL_BUILDDIR/gluegen-rt.jar
    GLUEGEN_OS=$JOGL_BUILDDIR
    JUNIT_JAR=$GLUEGEN_DIR/junit-4.5.jar
fi

if [ -z "$ANT_PATH" ] ; then
    ANT_JARS=
else
    ANT_JARS=$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar
fi

DEMOS_BUILDDIR=$THISDIR/$JOGL_BUILDDIR_BASE

echo JOGL AUTOBUILD: $AUTOBUILD
echo GLUEGEN BUILDDIR: $GLUEGEN_BUILDDIR
echo JOGL DIR: $JOGL_DIR
echo JOGL BUILDDIR: $JOGL_BUILDDIR
echo JOGL BUILDDIR BASE: $JOGL_BUILDDIR_BASE
echo JOGL PROFILE: $JOGL_PROFILE
echo DEMOS BUILDDIR: $DEMOS_BUILDDIR

J2RE_HOME=$(which java)
JAVA_HOME=$(which javac)
CP_SEP=:

. $JOGL_DIR/etc/profile.jogl $JOGL_PROFILE $JOGL_BUILDDIR 

SWT_CLASSPATH=$HOME/.java/swt.jar
LIB=$THISDIR/lib

CLASSPATH=.:$DEMOS_BUILDDIR/jogl-demos.jar:$DEMOS_BUILDDIR/jogl-demos-util.jar:$DEMOS_BUILDDIR/jogl-demos-data.jar:$GLUEGEN_JAR:$JOGL_CLASSPATH:$SWT_CLASSPATH:$JUNIT_JAR:$ANT_JARS
for i in $LIB/*jar ; do
    CLASSPATH=$CLASSPATH:$i
done
export CLASSPATH
# export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR
# export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR

echo CLASSPATH: $CLASSPATH
echo
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true" for running demos without AWT, e.g. NEWT
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true com.jogamp.newt.util.MainThread" for running demos with NEWT

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH


