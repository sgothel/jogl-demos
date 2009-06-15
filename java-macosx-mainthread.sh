#! /bin/sh

java -XstartOnFirstThread -Djava.awt.headless=true com.sun.javafx.newt.util.MainThread $* 2>&1 | tee java-macosx.mainThread.log
