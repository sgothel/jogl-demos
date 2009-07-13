#! /bin/sh

CVM=$1
shift

JOGL_LIB_DIR=$1
shift

BUILD_SUBDIR=$1
shift

if [ ! -x "$CVM" -o -z "$JOGL_LIB_DIR" -o -z "$BUILD_SUBDIR" ] ; then
    echo Usage $0 CVM-Binary JOGL_LIB_DIR BUILD_SUB_PATH
    echo e.g. $0 ../CVM/bin/cvm ../jogl/build-cdcfp-x86/lib build-cdcfp-x86
    exit 1
fi

$CVM -Djava.awt.headless=true -Dsun.boot.library.path=$JOGL_LIB_DIR -Xbootclasspath/a:../gluegen/$BUILD_SUBDIR/gluegen-rt.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/nativewindow/nativewindow.core.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/jogl/jogl.cdcfp.jar -Xbootclasspath/a:../jogl/$BUILD_SUBDIR/newt/newt.cdcfp.jar -Xbootclasspath/a:$BUILD_SUBDIR/jogl-demos.jar com.sun.javafx.newt.util.MainThread $* 2>&1 | tee cvm-dbg-newt.log
