#! /bin/sh

CVM=$1
shift

BUILD_SUBDIR=$1
shift

if [ ! -x "$CVM" -o -z "$BUILD_SUBDIR" ] ; then
    echo "Usage $0 CVM-Binary BUILD_SUB_PATH [-cpu <arch>] Main-Class"
    echo "e.g. $0 ../CVM/bin/cvm build-x86 -cpu x86 test"
    exit 1
fi

X_FLAGS=
if [ "$1" = "-cpu" ] ; then
    shift
    X_FLAGS="-Dos.arch=$1"
    shift
fi

$CVM $X_FLAGS -Djava.awt.headless=true -Dsun.boot.library.path=../jogl/$BUILD_SUBDIR/lib -Xbootclasspath/a:../gluegen/$BUILD_SUBDIR/gluegen-rt-cdc.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/nativewindow/nativewindow.all.cdc.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/jogl/jogl.all.cdc.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/newt/newt.all.cdc.jar -Xbootclasspath/a:$BUILD_SUBDIR/jogl-demos.jar com.sun.javafx.newt.util.MainThread $* 2>&1 | tee cvm-run-newt.log
