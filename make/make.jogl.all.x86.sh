#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 

ant \
    -Djogl.cg=1 \
    -Duser.swt.jar=$HOME/.java/swt-3.5.2-gtk-linux-x86.jar \
    -Drootrel.build=build-x86 \
    $* 2>&1 | tee make.jogl.all.x86.log
