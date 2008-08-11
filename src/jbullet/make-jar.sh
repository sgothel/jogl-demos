rm -f ragdoll.jar 
cd build/classes/
jar xf ../../lib/vecmath.jar 
jar xf ../../lib/trove.jar
cp -a  ../../lib/java .
rm -rf META-INF
find . -name \*nope -exec rm -fv \{\} \;
jar cf ../../ragdoll.jar .
