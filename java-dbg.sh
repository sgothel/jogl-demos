#! /bin/sh

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
# D_ARGS="-Djogamp.debug.JNILibLoader=true -Djogamp.debug.NativeLibrary=true -Djogamp.debug.NativeLibrary.Lookup=true -Djogl.debug.GLProfile=true"
D_ARGS="-Djogl.debug=all -Dnewt.debug=all -Dnativewindow.debug=all"

    java $D_ARGS $* 2>&1 | tee java-dbg.log
fi

