
set J2RE_HOME=c:\jre1.6.0_22_x64
set JAVA_HOME=c:\jdk1.6.0_22_x64

set JAR_DIR=..\jogl\build-win64\jar
set LIB_DIR=..\jogl\build-win64\lib

set CP_ALL=.;%JAR_DIR%\jogl.all.jar;%JAR_DIR%\gluegen-rt.jar;build-win64\jogl-demos.jar;build-win64\jogl-demos-util.jar;build-win64\jogl-demos-data.jar

echo CP_ALL %CP_ALL%

REM "-Djogamp.debug.ProcAddressHelper=true" 
REM "-Djogamp.debug.NativeLibrary=true" 
REM "-Dnativewindow.debug=all" 
REM "-Djogl.debug=all" 
REM "-Dnewt.debug=all" 

set D_ARGS="-Djogamp.debug.JNILibLoader=true" "-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true" "-Djogl.debug.GLProfile=true"
REM D_ARGS="-Djogl.debug=all -Dnewt.debug=all -Dnativewindow.debug=all"

set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %D_ARGS% %X_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win64-dbg.log 2>&1


