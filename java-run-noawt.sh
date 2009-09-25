#! /bin/sh

MOSX=0
uname -a | grep -i Darwin && MOSX=1

CPOK=0
echo $CLASSPATH | grep jogl && CPOK=1

if [ $CPOK -eq 0 ] ; then
    # Only valid for autobuild .. otherwise run manually with build-dir
    . ./setenv-jogl.sh JOGL_ALL
    echo $CLASSPATH | grep jogl && CPOK=1
fi
if [ $CPOK -eq 0 ] ; then
    echo No JOGL in CLASSPATH
else
    if [ $MOSX -eq 1 ] ; then
        X_ARGS="-XstartOnFirstThread"
    fi

    java $X_ARGS -Djava.awt.headless=true $* 2>&1 | tee java-run-noawt.log
fi
