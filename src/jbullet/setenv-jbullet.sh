#! /bin/sh

JOGLPROF=$1
shift

if [ -z "$JOGLPROF" ] ; then
    JOGLPROF=JOGL_ES1_MIN
fi
THISHOME=`pwd`

cd ../..
. ./setenv-jogl.sh $JOGLPROF

cd $THISHOME

THISDIR=`pwd`

PATH=$JAVA_HOME:$J2RE_HOME:$PATH
export PATH

export LIBXCB_ALLOW_SLOPPY_LOCK=1

LIB=$THISDIR/lib

JOGL_HOME=$THISDIR/../../../jogl
CLASSPATH_TROVE=$LIB/trove.jar
CLASSPATH_VECM=$LIB/vecmath.jar
CLASSPATH=$CLASSPATH:$THISDIR/build/classes:$CLASSPATH_TROVE:$CLASSPATH_VECM
export JOGL_HOME CLASSPATH_TROVE CLASSPATH_VECM CLASSPATH 

#
# java javabullet.demos.genericjoint.GenericJointDemo
#

