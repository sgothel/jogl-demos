package demos.dualDepthPeeling;


// Translated from C++ Version see below:
//
// GLSLProgramObject.h - Wrapper for GLSL program objects
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
////////////////////////////////////////////////////////////////////////////////
import com.jogamp.opengl.GL2;

public class GLHelper
{
	public static void setTextureUnit(GL2 gl, int progId, String texname, int texunit)
	{
		int[] params = new int[]{0};
		gl.glGetProgramiv( progId, GL2.GL_LINK_STATUS, params, 0);
		if ( params[0] != 1 ) {
			System.err.println( "Error: setTextureUnit needs program to be linked.");
		}
		int id = gl.glGetUniformLocation(progId, texname );
		if (id == -1) {
			System.err.println( "Warning: Invalid texture " + texname );
			return;
		}
		gl.glUniform1i(id, texunit);
	}


	public static void bindTexture(GL2 gl, int target, int texid, int texUnit)
	{
		gl.glActiveTexture(GL2.GL_TEXTURE0 + texUnit);
		gl.glBindTexture(target, texid);
		gl.glActiveTexture(GL2.GL_TEXTURE0);
	}


	public static void bindTexture2D(GL2 gl, int texid, int texUnit) {
		bindTexture(gl, GL2.GL_TEXTURE_2D, texid, texUnit);
	}

	public static void bindTexture3D(GL2 gl, int texid, int texUnit) {
		bindTexture(gl, GL2.GL_TEXTURE_3D, texid, texUnit);
	}

	// g_shaderDualPeel.bindTextureRECT(gl, g_shaderState, s_DepthBlenderTex, g_dualDepthTexId[prevId]);
	public static void bindTextureRECT(GL2 gl, int texid, int texUnit) {
		bindTexture(gl, GL2.GL_TEXTURE_RECTANGLE_ARB, texid, texUnit);
	}
};
