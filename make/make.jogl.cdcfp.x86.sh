#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 

ant \
    -Djogl.es=1 \
    -Drootrel.build=build-cdcfp-x86 \
    $* 2>&1 | tee make.jogl.cdcfp.x86.log
