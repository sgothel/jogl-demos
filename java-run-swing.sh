#! /bin/sh

CPOK=0
echo $CLASSPATH | grep jogl && CPOK=1

#SWING_PROPS="-Dsun.java2d.noddraw=true -Dsun.java2d.opengl=false"
SWING_PROPS="-Dsun.java2d.noddraw=true -Dsun.java2d.opengl=true"
SWING_PROPS_DBG="-Djnlp.jogl.debug.GLJPanel=true"

if [ $CPOK -eq 0 ] ; then
    # Only valid for autobuild .. otherwise run manually with build-dir
    . ./setenv-jogl.sh JOGL_ALL
    echo $CLASSPATH | grep jogl && CPOK=1
fi
if [ $CPOK -eq 0 ] ; then
    echo No JOGL in CLASSPATH
else
    java $SWING_PROPS $SWING_PROPS_DBG $* 2>&1 | tee java-run-swing.log
fi

