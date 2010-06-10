
set J2RE_HOME=c:\jre1.6.0_20_x32
set JAVA_HOME=c:\jdk1.6.0_20_x32

set JAR_DIR=..\jogl\build-win32\jar
set LIB_DIR=..\jogl\build-win32\lib

set CP_ALL=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\nativewindow.all.jar;%JAR_DIR%\newt.all.jar;%JAR_DIR%\gluegen-rt.jar;build-win32\jogl-demos.jar;build-win32\jogl-demos-util.jar;build-win32\jogl-demos-data.jar

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32.log 2>&1


