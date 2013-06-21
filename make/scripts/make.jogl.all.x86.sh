#! /bin/sh

SDIR=`dirname $0`

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh
fi

. $SDIR/../../../jogl/etc/profile.jogl JOGL_ALL $SDIR/../../../jogl/build-x86

#    -Dc.compiler.debug=true 

ant \
    -Dtarget.sourcelevel=1.6 \
    -Dtarget.targetlevel=1.6 \
    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
    -Djogl.cg=1 \
    -Duser.swt.jar=$SWT_CLASSPATH \
    -Drootrel.build=build-x86 \
    $* 2>&1 | tee make.jogl.all.x86.log
