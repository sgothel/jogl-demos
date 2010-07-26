#! /bin/sh

. ../../setenv-build-jogl-x86_64.sh

#    -Dc.compiler.debug=true 
#    -Djogl.cg=1 \
#    -Djogl.redbook=true \
#    -DskipSourceZIP=true

ant \
    -DskipSourceZIP=true \
    -Djogl.cg=1 \
    -Duser.swt.jar=$HOME/.java/swt-3.6-gtk-linux-x86_64.jar \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee make.jogl.all.x86_64.log
