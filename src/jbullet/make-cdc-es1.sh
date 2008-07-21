#! /bin/sh

#    -Djavac.bootclasspath.jar=$(pwd)/../../../gluegen/make/lib/cdc_fp.jar \
#ant -Djavac.source=1.5 -Djavac.bootclasspath.jar=/usr/local/projects/SUN/JOGL2/gluegen/make/lib/cdc_fp.jar 2>&1 | tee make-cdc-es1.log
ant -v \
    -Djogl.home=$JOGL_HOME \
    $* 2>&1 | tee make-cdc-es1.log
