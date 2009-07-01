#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 

ant \
    -Djogl.cg=1 \
    -Drootrel.build=build-x86 \
    $* 2>&1 | tee make.jogl.all.x86.log
