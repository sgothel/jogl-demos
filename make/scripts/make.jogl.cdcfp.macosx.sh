#! /bin/sh

. /devtools/etc/profile.ant

#    -Dc.compiler.debug=true 

ant -v \
    -Djogl.es=1 \
    -Drootrel.build=build-cdcfp-macosx \
    $* 2>&1 | tee make.jogl.cdcfp.macosx.log
