#! /bin/sh

SDIR=`dirname $0`

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

. $SDIR/../../../jogl/etc/profile.jogl JOGL_ALL $SDIR/../../../jogl/build-x86_64

#    -Dc.compiler.debug=true 
#    -Djogl.cg=1 \
#    -Djogl.redbook=true \

ant \
    -Djogl.cg=1 \
    -Duser.swt.jar=$SWT_CLASSPATH \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee make.jogl.all.x86_64.log
