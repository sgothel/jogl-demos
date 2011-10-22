#! /bin/sh

SDIR=`dirname $0`

#    -Dc.compiler.debug=true 
#    -Djogl.cg=1 \

ant -v \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
