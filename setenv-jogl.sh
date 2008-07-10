#! /bin/sh

if [ -z "$1" ] ; then
    echo "Usage: $0 {JOGL_ALL|JOGL_ES1_MIN|JOGL_ES1_MAX|JOGL_ES2_MIN|JOGL_ES2_MAX|JOGL_GL2_MIN|JOGL_GL2_MAX}"
else

JOGL_PROFILE=$1
shift

CVSROOT=":pserver:sgoethel@cvs.dev.java.net:/cvs"
THISDIR="/usr/local/projects/SUN/JOGL2/jogl-demos"
export CVSROOT THISDIR

. /devtools/etc/profile.ant

J2RE_HOME=/opt-linux-x86/jre-dev
JAVA_HOME=/usr/local/projects/SUN/JDK6/control/build/linux-i586/j2sdk-image
CP_SEP=:

export LIBXCB_ALLOW_SLOPPY_LOCK=1

#PATH=/devtools/i686-unknown-linux-gnu/gcc-4.2.1-glibc-2.3.6/i686-unknown-linux-gnu/i686-unknown-linux-gnu/bin:$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
PATH=/devtools/i686-unknown-linux-gnu/gcc-4.2.1-glibc-2.3.6/i686-unknown-linux-gnu/i686-unknown-linux-gnu/bin:$JAVA_HOME/bin:$PATH
export PATH

. $THISDIR/../jogl/etc/profile.jogl $THISDIR/../jogl/build $THISDIR/../jogl/build/obj $JOGL_PROFILE

GLUEGEN_JAR=$THISDIR/../gluegen/build/gluegen-rt.jar
GLUEGEN_OS=$THISDIR/../gluegen/build/obj

LIB=$THISDIR/lib

# CLASSPATH=$JAVA_HOME/jre/lib/rt.jar:.:build/classes
CLASSPATH=.:$THISDIR/build/classes:$GLUEGEN_JAR:$JOGL_CLASSPATH
for i in $LIB/*jar ; do
    CLASSPATH=$CLASSPATH:$i
done
export CLASSPATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GLUEGEN_OS:$THISDIR/../jogl/build/obj

fi


