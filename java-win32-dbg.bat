
REM set TOPDIR=C:\SUN\JOGL2
set TOPDIR=..
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6

set JAR_DIR=%TOPDIR%\lib-jogl-jars
set LIB_DIR=%TOPDIR%\lib-jogl-win32

set PATH=%J2RE_HOME%\bin;%LIB_DIR%;%PATH%
set CLASSPATH=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\nativewindow.all.jar;%JAR_DIR%\newt.all.jar;%JAR_DIR%\gluegen-rt.jar;%JAR_DIR%\jogl-demos.jar;%JAR_DIR%\jogl-demos-util.jar;%JAR_DIR%\jogl-demos-data.jar

echo CLASSPATH %CLASSPATH%

echo PATH %PATH%

REM java "-Dgluegen.debug.ProcAddressHelper" "-Dgluegen.debug.NativeLibrary=true" "-Djava.library.path=%LIB_DIR%" "-Dnativewindow.debug=all" "-Djogl.debug=all" %1 %2 %3 %4 > java-win32-dbg.log 2>&1
REM java "-Dgluegen.debug.NativeLibrary=true" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" "-Djava.library.path=%LIB_DIR%" %1 %2 %3 %4 > java-win32-dbg.log 2>&1
java "-Dnewt.debug.Window" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" "-Djava.library.path=%LIB_DIR%" %1 %2 %3 %4 > java-win32-dbg.log 2>&1


