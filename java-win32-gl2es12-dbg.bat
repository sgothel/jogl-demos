
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6
REM set J2RE_HOME=c:\jdk1.4.2_21
REM set JAVA_HOME=c:\jdk1.4.2_21

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

set CP_GLUE=%JAR_DIR%\gluegen-rt.jar

REM set CP_JOGL=%JAR_DIR%\jogl.core.jar;%JAR_DIR%\jogl.util.jar;%JAR_DIR%\jogl.gl2es12.win.jar;%JAR_DIR%\jogl.util.fixedfuncemu.jar;%JAR_DIR%\jogl.gl2es12.dbg.jar;%JAR_DIR%\jogl.glu.tess.jar;%JAR_DIR%\jogl.glu.mipmap.jar;%JAR_DIR%\jogl.awt.jar;%JAR_DIR%\jogl.util.awt.jar
set CP_JOGL=%JAR_DIR%\jogl.core.jar;%JAR_DIR%\jogl.gl2es12.win.jar;%JAR_DIR%\jogl.util.jar;%JAR_DIR%\jogl.gles2.dbg.jar;%JAR_DIR%\jogl.util.fixedfuncemu.jar
REM set CP_JOGL=%JAR_DIR%\jogl.core.jar;%JAR_DIR%\jogl.gl2.win.jar;%JAR_DIR%\jogl.util.jar;%JAR_DIR%\jogl.gles2.dbg.jar

REM set CP_NEWT=%JAR_DIR%\newt.all.jar
set CP_NEWT=%JAR_DIR%\newt.core.jar;%JAR_DIR%\newt.win.jar;%JAR_DIR%\newt.ogl.jar

REM set CP_NWI=%JAR_DIR%\nativewindow.all.jar
set CP_NWI=%JAR_DIR%\nativewindow.core.jar

set CP_DEMO=jogl-demos.jar;jogl-demos-util.jar;jogl-demos-data.jar

set CP_ALL=%CP_GLUE%;%CP_NWI%;%CP_JOGL%;%CP_NEWT%;%CP_DEMO%

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -cp %CP_ALL% "-Djava.library.path=%LIB_DIR%" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 > java-win32-dbg.log 2>&1


