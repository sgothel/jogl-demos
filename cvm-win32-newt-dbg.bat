
set CVM_HOME=c:\cvm

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

%CVM_HOME%\bin\cvm "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all" "-Djava.awt.headless=true" "-Dsun.boot.library.path=%LIB_DIR%" "-Xbootclasspath/a:%JAR_DIR%\gluegen-rt-cdc.jar" "-Xbootclasspath/a:%JAR_DIR%\nativewindow.all.cdc.jar" "-Xbootclasspath/a:%JAR_DIR%\jogl.all.cdc.jar" "-Xbootclasspath/a:%JAR_DIR%\newt.all.cdc.jar" "-Xbootclasspath/a:jogl-demos.jar" %1 %2 %3 %4 > cvm-win32-newt-dbg.log 2>&1

