#! /bin/sh

SDIR=`dirname $0`

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh
fi

. $SDIR/../../../jogl/etc/profile.jogl JOGL_ALL $SDIR/../../../jogl/build-x86

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#    -Dc.compiler.debug=true 

ant \
    -Djogl.cg=1 \
    -Duser.swt.jar=$SWT_CLASSPATH \
    -Drootrel.build=build-x86 \
    $* 2>&1 | tee make.jogl.all.x86.log
