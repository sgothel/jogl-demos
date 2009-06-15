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

J2RE_HOME=/opt-linux-x86/jre6
JAVA_HOME=/opt-linux-x86/j2se6
CP_SEP=:

export LIBXCB_ALLOW_SLOPPY_LOCK=1

. $THISDIR/../jogl/etc/profile.jogl $THISDIR/../jogl/build-x86 $JOGL_PROFILE

GLUEGEN_JAR=$THISDIR/../gluegen/build-x86/gluegen-rt.jar
GLUEGEN_OS=$THISDIR/../gluegen/build-x86/obj

LIB=$THISDIR/lib

# CLASSPATH=$JAVA_HOME/jre/lib/rt.jar:.:build-x86/classes
CLASSPATH=.:$THISDIR/build-x86/jogl-demos.jar:$THISDIR/build-x86/jogl-demos-util.jar:$THISDIR/build-x86/jogl-demos-data.jar:$GLUEGEN_JAR:$JOGL_CLASSPATH
for i in $LIB/*jar ; do
    CLASSPATH=$CLASSPATH:$i
done
export CLASSPATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR

echo JOGL_CLASSPATH: $JOGL_CLASSPATH

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH

fi


