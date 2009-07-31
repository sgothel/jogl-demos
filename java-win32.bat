
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

set CP_ALL=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\nativewindow.all.jar;%JAR_DIR%\newt.all.jar;%JAR_DIR%\gluegen-rt.jar;jogl-demos.jar;jogl-demos-util.jar;jogl-demos-data.jar

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 > java-win32.log 2>&1


