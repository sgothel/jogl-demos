#! /bin/sh

. ../../setenv-build-jogl-x86_64.sh

#    -Dc.compiler.debug=true 
#    -Djogl.cg=1 \

ant \
    -Djogl.cg=1 \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee make.jogl.all.x86_64.log
