#! /bin/sh

SDIR=`dirname $0`

#    -Dc.compiler.debug=true 
#    -Djogl.cg=1 \

ant \
    -Dtarget.sourcelevel=1.6 \
    -Dtarget.targetlevel=1.6 \
    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
