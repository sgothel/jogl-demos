
set J2RE_HOME=c:\jre1.6.0_22_x32
set JAVA_HOME=c:\jdk1.6.0_22_x32

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

set CP_GLUE=%JAR_DIR%\gluegen-rt.jar

set CP_JOGL=%JAR_DIR%\jogl.all-mobile.jar;%JAR_DIR%\jogl.util.jar;%JAR_DIR%\jogl.gles2.dbg.jar;%JAR_DIR%\jogl.util.fixedfuncemu.jar

set CP_DEMO=jogl-demos.jar;jogl-demos-util.jar;jogl-demos-data.jar

set CP_ALL=%CP_GLUE%;%CP_JOGL%;%CP_DEMO%

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -cp %CP_ALL% "-Djava.library.path=%LIB_DIR%" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32-dbg.log 2>&1


