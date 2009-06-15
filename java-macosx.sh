#! /bin/sh

java -XstartOnFirstThread -Djava.awt.headless=true $* 2>&1 | tee java-macosx.log
