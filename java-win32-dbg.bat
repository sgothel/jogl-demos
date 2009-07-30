
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

set PATH=%J2RE_HOME%\bin;%LIB_DIR%;%PATH%
set CLASSPATH=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\nativewindow.all.jar;%JAR_DIR%\newt.all.jar;%JAR_DIR%\gluegen-rt.jar;jogl-demos.jar;jogl-demos-util.jar;jogl-demos-data.jar

echo CLASSPATH %CLASSPATH%

echo PATH %PATH%

java "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" "-Djava.library.path=%LIB_DIR%" %1 %2 %3 %4 > java-win32-dbg.log 2>&1


