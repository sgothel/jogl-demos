#! /bin/sh

# -Dgluegen.debug.ProcAddressHelper=true -Dgluegen.debug.NativeLibrary=true -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all
# -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all

java -XstartOnFirstThread -Djava.awt.headless=true $* 2>&1 | tee java-macosx.log
