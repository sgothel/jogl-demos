#! /bin/sh

java -Dgluegen.debug.ProcAddressHelper=true -Dgluegen.debug.NativeLibrary=true -Dnativewindow.debug=all -Djogl.debug=all $* 2>&1 | tee java-dbg.log


