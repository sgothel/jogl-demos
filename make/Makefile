ROOT=..
# Note: some of these directories are also coded into the .cfg files
SRC_DIR=$(ROOT)/src
BUILD_DIR=$(ROOT)/build
CLASSES_DIR=$(BUILD_DIR)/classes
JAVADOC_DIR=$(ROOT)/javadoc
JAVADOC_LINK=http://java.sun.com/j2se/1.4.1/docs/api/
JAVAC=javac

#----------------------------------------------------------------------
# gleem variables
#

GLEEM_JAVAFILES = \
	$(SRC_DIR)/gleem/BSphere.java \
	$(SRC_DIR)/gleem/BSphereProvider.java \
	$(SRC_DIR)/gleem/CameraParameters.java \
	$(SRC_DIR)/gleem/ExaminerViewer.java \
	$(SRC_DIR)/gleem/HandleBoxManip.java \
	$(SRC_DIR)/gleem/HitPoint.java \
	$(SRC_DIR)/gleem/Manip.java \
	$(SRC_DIR)/gleem/ManipManager.java \
	$(SRC_DIR)/gleem/ManipMotionListener.java \
	$(SRC_DIR)/gleem/ManipPart.java \
	$(SRC_DIR)/gleem/ManipPartCube.java \
	$(SRC_DIR)/gleem/ManipPartGroup.java \
	$(SRC_DIR)/gleem/ManipPartLineSeg.java \
	$(SRC_DIR)/gleem/ManipPartSquare.java \
	$(SRC_DIR)/gleem/ManipPartTransform.java \
	$(SRC_DIR)/gleem/ManipPartTriBased.java \
	$(SRC_DIR)/gleem/ManipPartTwoWayArrow.java \
	$(SRC_DIR)/gleem/MouseButtonHelper.java \
	$(SRC_DIR)/gleem/NormalCalc.java \
	$(SRC_DIR)/gleem/RayTriangleIntersection.java \
	$(SRC_DIR)/gleem/RightTruncPyrMapping.java \
	$(SRC_DIR)/gleem/ScreenToRayMapping.java \
	$(SRC_DIR)/gleem/TestExaminerViewer.java \
	$(SRC_DIR)/gleem/TestHandleBox.java \
	$(SRC_DIR)/gleem/TestMultiWin.java \
	$(SRC_DIR)/gleem/TestTranslate1.java \
	$(SRC_DIR)/gleem/TestTranslate2.java \
	$(SRC_DIR)/gleem/Translate1Manip.java \
	$(SRC_DIR)/gleem/Translate2Manip.java \
	$(SRC_DIR)/gleem/WindowUpdateListener.java \
	$(SRC_DIR)/gleem/linalg/DimensionMismatchException.java \
	$(SRC_DIR)/gleem/linalg/IntersectionPoint.java \
	$(SRC_DIR)/gleem/linalg/Line.java \
	$(SRC_DIR)/gleem/linalg/Mat2f.java \
	$(SRC_DIR)/gleem/linalg/Mat3f.java \
	$(SRC_DIR)/gleem/linalg/Mat4f.java \
	$(SRC_DIR)/gleem/linalg/Matf.java \
	$(SRC_DIR)/gleem/linalg/MathUtil.java \
	$(SRC_DIR)/gleem/linalg/NonSquareMatrixException.java \
	$(SRC_DIR)/gleem/linalg/Plane.java \
	$(SRC_DIR)/gleem/linalg/PlaneUV.java \
	$(SRC_DIR)/gleem/linalg/Rotf.java \
	$(SRC_DIR)/gleem/linalg/SingularMatrixException.java \
	$(SRC_DIR)/gleem/linalg/Vec2f.java \
	$(SRC_DIR)/gleem/linalg/Vec3d.java \
	$(SRC_DIR)/gleem/linalg/Vec3f.java \
	$(SRC_DIR)/gleem/linalg/Vec4f.java \
	$(SRC_DIR)/gleem/linalg/Vecf.java \
	$(SRC_DIR)/gleem/linalg/Veci.java 

#
#----------------------------------------------------------------------

#----------------------------------------------------------------------
# demo files

DEMO_JAVAFILES = \
	$(SRC_DIR)/demos/gears/Gears.java \
	$(SRC_DIR)/demos/hwShadowmapsSimple/HWShadowmapsSimple.java \
	$(SRC_DIR)/demos/infiniteShadowVolumes/InfiniteShadowVolumes.java \
	$(SRC_DIR)/demos/proceduralTexturePhysics/ProceduralTexturePhysics.java \
	$(SRC_DIR)/demos/proceduralTexturePhysics/Water.java \
	$(SRC_DIR)/demos/testContextSharing/TestContextSharing.java \
	$(SRC_DIR)/demos/vertexArrayRange/VertexArrayRange.java \
	$(SRC_DIR)/demos/vertexProgRefract/VertexProgRefract.java \
	$(SRC_DIR)/demos/vertexProgWarp/VertexProgWarp.java

#
#----------------------------------------------------------------------

#----------------------------------------------------------------------
# demo utility files

DEMO_UTIL_JAVAFILES = \
	$(SRC_DIR)/demos/util/Bunny.java \
	$(SRC_DIR)/demos/util/DDSReader.java \
	$(SRC_DIR)/demos/util/DurationTimer.java \
	$(SRC_DIR)/demos/util/DxTex.java \
	$(SRC_DIR)/demos/util/FloatList.java \
	$(SRC_DIR)/demos/util/IntList.java \
	$(SRC_DIR)/demos/util/LEDataInputStream.java \
	$(SRC_DIR)/demos/util/MD2.java \
	$(SRC_DIR)/demos/util/ObjReader.java \
	$(SRC_DIR)/demos/util/SystemTime.java \
	$(SRC_DIR)/demos/util/TGAImage.java \
	$(SRC_DIR)/demos/util/Time.java \
	$(SRC_DIR)/demos/util/Triceratops.java

#
#----------------------------------------------------------------------

#
# Compilation package rules
#

JAVAFILES = $(GLEEM_JAVAFILES) $(DEMO_JAVAFILES) $(DEMO_UTIL_JAVAFILES)
CLASSFILES=$(patsubst $(SRC_DIR)/%,$(CLASSES_DIR)/%,$(JAVAFILES:.java=.class))

JOGL_DEMOS_JAR=$(BUILD_DIR)/jogl-demos.jar
JOGL_DEMOS_UTIL_JAR=$(BUILD_DIR)/jogl-demos-util.jar
JOGL_DEMOS_DATA_JAR=$(BUILD_DIR)/jogl-demos-data.jar

$(JOGL_DEMOS_JAR) $(JOGL_DEMOS_UTIL_JAR) $(JOGL_DEMOS_DATA_JAR): $(CLASSFILES)
	rm -f $(JOGL_DEMOS_JAR) $(JOGL_DEMOS_DATA_JAR)
	cd $(CLASSES_DIR) ; \
	jar cvf ../jogl-demos.jar \
	  demos/gears \
	  demos/hwShadowmapsSimple \
	  demos/infiniteShadowVolumes \
	  demos/proceduralTexturePhysics \
	  demos/testContextSharing \
	  demos/vertexArrayRange \
	  demos/vertexProgRefract \
	  demos/vertexProgWarp ; \
	jar cvf ../jogl-demos-util.jar \
          demos/util gleem
	cd $(SRC_DIR) ; \
	jar cvf ../build/jogl-demos-data.jar demos/data

$(CLASSFILES) : $(JAVAFILES)
	mkdir -p $(CLASSES_DIR)
	$(JAVAC) -source 1.4 -classpath "$(CLASSES_DIR)$(CP_SEPARATOR)$(CLASSPATH)" -d $(CLASSES_DIR) $(JAVAFILES)

doc:
	mkdir -p $(JAVADOC_DIR)
	javadoc -source 1.4 -d $(JAVADOC_DIR) -link $(JAVADOC_LINK) \
		-public -sourcepath "$(SRC_DIR)" \
		demos.gears \
		demos.hwShadowmapsSimple \
		demos.infiniteShadowVolumes \
		demos.proceduralTexturePhysics \
		demos.util \
		demos.vertexArrayRange \
		demos.vertexProgRefract \
		demos.vertexProgWarp \
		gleem \
		gleem.linalg

clean:
	rm -rf $(BUILD_DIR) $(JAVADOC_DIR)
