
#set TOPDIR=C:\SUN\JOGL2
set TOPDIR=..
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6

set JAR_DIR=%TOPDIR%\lib-jogl-jars
set LIB_DIR=%TOPDIR%\lib-jogl-win32

set PATH=%J2RE_HOME%\bin;%LIB_DIR%;%PATH%
set CLASSPATH=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\nativewindow.all.jar;%JAR_DIR%\newt.all.jar;%JAR_DIR%\gluegen-rt.jar;%JAR_DIR%\jogl-demos.jar;%JAR_DIR%\jogl-demos-util.jar;%JAR_DIR%\jogl-demos-data.jar

echo CLASSPATH %CLASSPATH%

echo PATH %PATH%

java "-Djava.library.path=%LIB_DIR%" %1 %2 %3 %4 > java-win32.log 2>&1


