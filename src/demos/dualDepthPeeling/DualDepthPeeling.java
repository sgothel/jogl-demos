package demos.dualDepthPeeling;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import demos.data.DemosDataAnchor;

// Translated from C++ Version see below:
//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Depth peeling is traditionally used to perform order independent transparency (OIT)
// with N geometry passes for N transparency layers. Dual depth peeling enables peeling
// N transparency layers in N/2+1 passes, by peeling from the front and the back
// simultaneously using a min-max depth buffer. This sample performs either normal or
// dual depth peeling and blends on the fly.
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------


public class DualDepthPeeling implements GLEventListener, KeyListener, MouseListener, MouseMotionListener
{
	public final static int DUAL_PEELING_MODE = 0;
	public final static int F2B_PEELING_MODE = 1;
	public final static int WEIGHTED_AVERAGE_MODE = 2;
	public final static int WEIGHTED_SUM_MODE = 3;

	public final static float FOVY = 30.0f;
	public final static float ZNEAR = 0.0001f;
	public final static float ZFAR = 10.0f;
	public final static float FPS_TIME_WINDOW = 1;
	public final static float MAX_DEPTH = 1.0f;

	public int g_numPasses = 4;
	public int g_imageWidth = 1024;
	public int g_imageHeight = 768;

	public Model g_model;
	public int g_quadDisplayList;
	public int[] g_vboId = new int[1];
	public int[] g_eboId = new int[1];

	public boolean g_useOQ = true;
	public int[] g_queryId = new int[1];

	public String MODEL_FILENAME = "models/dragon.obj";

	public static final String s_FrontBlenderTex = "FrontBlenderTex";
	public static final String s_BackBlenderTex  = "BackBlenderTex";
	public static final String s_DepthBlenderTex = "DepthBlenderTex";
	public static final String s_ColorTex        = "ColorTex";
	public static final String s_ColorTex0       = "ColorTex0";
	public static final String s_ColorTex1       = "ColorTex1";
	public static final String s_TempTex         = "TempTex";
	public static final String s_BackgroundColor = "BackgroundColor";

	public GLUniformData g_FrontBlenderTexUnit;
	public GLUniformData g_BackBlenderTexUnit;
	public GLUniformData g_DepthBlenderTexUnit;
	public GLUniformData g_ColorTexUnit;
	public GLUniformData g_ColorTex0Unit;
	public GLUniformData g_ColorTex1Unit;
	public GLUniformData g_TempTexUnit;
	public GLUniformData g_AlphaUni;
	public GLUniformData g_backgroundColorUni;

	public ShaderState g_shaderState;
	public ShaderProgram g_shaderDualInit;
	public ShaderProgram g_shaderDualPeel;
	public ShaderProgram g_shaderDualBlend;
	public ShaderProgram g_shaderDualFinal;

	public ShaderProgram g_shaderFrontInit;
	public ShaderProgram g_shaderFrontPeel;
	public ShaderProgram g_shaderFrontBlend;
	public ShaderProgram g_shaderFrontFinal;

	public ShaderProgram g_shaderAverageInit;
	public ShaderProgram g_shaderAverageFinal;

	public ShaderProgram g_shaderWeightedSumInit;
	public ShaderProgram g_shaderWeightedSumFinal;

	public float g_opacity = 0.6f;
	public char g_mode = DUAL_PEELING_MODE;
	public boolean g_showOsd = true;
	public boolean g_bShowUI = true;
	public int g_numGeoPasses = 0;

	public boolean g_rotating = false;
	public boolean g_panning = false;
	public boolean g_scaling = false;
	public int g_oldX, g_oldY;
	public int g_newX, g_newY;
	public float g_bbScale = 1.0f;
	public float[] g_bbTrans = new float[]{0.0f, 0.0f, 0.0f};
	public float[] g_rot = new float[]{0.0f, 45.0f};
	public float[] g_pos = new float[]{0.0f, 0.0f, 2.0f};

	static final FloatBuffer g_white = Buffers.newDirectFloatBuffer(new float[]{1.0f,1.0f,1.0f});
	static final FloatBuffer g_black = Buffers.newDirectFloatBuffer(new float[]{0.0f,0.0f,0.0f});
	FloatBuffer g_backgroundColor = g_white;

	public int[]  g_dualBackBlenderFboId = new int[1];
	public int[]  g_dualPeelingSingleFboId = new int[1];
	public int[]  g_dualDepthTexId = new int[2];
	public int[]  g_dualFrontBlenderTexId = new int[2];
	public int[]  g_dualBackTempTexId = new int[2];
	public int[]  g_dualBackBlenderTexId = new int[1];

	public int[]  g_frontFboId = new int[2];
	public int[]  g_frontDepthTexId = new int[2];
	public int[]  g_frontColorTexId = new int[2];
	public int[]  g_frontColorBlenderTexId = new int[1];
	public int[]  g_frontColorBlenderFboId = new int[1];

	public int[] g_accumulationTexId = new int[2];
	public int[] g_accumulationFboId = new int[1];

	int g_drawBuffers[] = {GL2.GL_COLOR_ATTACHMENT0,
			GL2.GL_COLOR_ATTACHMENT1,
			GL2.GL_COLOR_ATTACHMENT2,
			GL2.GL_COLOR_ATTACHMENT3,
			GL2.GL_COLOR_ATTACHMENT4,
			GL2.GL_COLOR_ATTACHMENT5,
			GL2.GL_COLOR_ATTACHMENT6
	};

	boolean reloadShaders = false;

	public DualDepthPeeling()
	{
		InitGL();
	}


	public void InitDualPeelingRenderTargets(final GL2 gl)
	{
		gl.glGenTextures(2, g_dualDepthTexId, 0);
		gl.glGenTextures(2, g_dualFrontBlenderTexId, 0);
		gl.glGenTextures(2, g_dualBackTempTexId, 0);
		gl.glGenFramebuffers(1, g_dualPeelingSingleFboId, 0);
		for (int i = 0; i < 2; i++)
		{
			gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualDepthTexId[i]);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);

			//gl.glEnable( GL2.GL_PIXEL_UNPACK_BUFFER );

			gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL2.GL_FLOAT_RG32_NV, g_imageWidth, g_imageHeight,
					0,  GL.GL_RGB,  GL.GL_FLOAT, null);

			gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualFrontBlenderTexId[i]);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
			gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL.GL_RGBA, g_imageWidth, g_imageHeight,
					0,  GL.GL_RGBA,  GL.GL_FLOAT, null);

			gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackTempTexId[i]);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
			gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL.GL_RGBA, g_imageWidth, g_imageHeight,
					0,  GL.GL_RGBA,  GL.GL_FLOAT, null);
		}

		gl.glGenTextures(1, g_dualBackBlenderTexId, 0);
		gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackBlenderTexId[0]);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
		gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL.GL_RGB, g_imageWidth, g_imageHeight,
				0, GL.GL_RGB, GL.GL_FLOAT, null);

		gl.glGenFramebuffers(1, g_dualBackBlenderFboId, 0);
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, g_dualBackBlenderFboId[0]);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackBlenderTexId[0], 0);

		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, g_dualPeelingSingleFboId[0]);

		int j = 0;
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_COLOR_ATTACHMENT0,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualDepthTexId[j], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT1,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualFrontBlenderTexId[j], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT2,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackTempTexId[j], 0);

		j = 1;
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT3,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualDepthTexId[j], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT4,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualFrontBlenderTexId[j], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT5,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackTempTexId[j], 0);

		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT6,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_dualBackBlenderTexId[0], 0);
	}

	//--------------------------------------------------------------------------
	void DeleteDualPeelingRenderTargets(final GL2 gl)
	{
		gl.glDeleteFramebuffers(1, g_dualBackBlenderFboId, 0);
		gl.glDeleteFramebuffers(1, g_dualPeelingSingleFboId, 0);
		gl.glDeleteTextures(2, g_dualDepthTexId, 0);
		gl.glDeleteTextures(2, g_dualFrontBlenderTexId, 0);
		gl.glDeleteTextures(2, g_dualBackTempTexId, 0);
		gl.glDeleteTextures(1, g_dualBackBlenderTexId, 0);
	}

	//--------------------------------------------------------------------------
	void InitFrontPeelingRenderTargets(final GL2 gl)
	{
		gl.glGenTextures(2, g_frontDepthTexId, 0);
		gl.glGenTextures(2, g_frontColorTexId, 0);
		gl.glGenFramebuffers(2, g_frontFboId, 0);

		for (int i = 0; i < 2; i++)
		{
			gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[i]);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
			gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL2ES3.GL_DEPTH_COMPONENT32F,
					g_imageWidth, g_imageHeight, 0,  GL2ES2.GL_DEPTH_COMPONENT,  GL.GL_FLOAT, null);

			gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorTexId[i]);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T,  GL2.GL_CLAMP);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
			gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
			gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL.GL_RGBA, g_imageWidth, g_imageHeight,
					0,  GL.GL_RGBA,  GL.GL_FLOAT, null);

			gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, g_frontFboId[i]);
			gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_DEPTH_ATTACHMENT,
					GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[i], 0);
			gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_COLOR_ATTACHMENT0,
					GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorTexId[i], 0);
		}

		gl.glGenTextures(1, g_frontColorBlenderTexId, 0);
		gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorBlenderTexId[0]);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
		gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL. GL_RGBA, g_imageWidth, g_imageHeight,
				0,  GL.GL_RGBA,  GL.GL_FLOAT, null);

		gl.glGenFramebuffers(1, g_frontColorBlenderFboId, 0);
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_DEPTH_ATTACHMENT,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[0], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_COLOR_ATTACHMENT0,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorBlenderTexId[0], 0);
	}

	//--------------------------------------------------------------------------
	void DeleteFrontPeelingRenderTargets(final GL2 gl)
	{
		gl.glDeleteFramebuffers(2, g_frontFboId, 0);
		gl.glDeleteFramebuffers(1, g_frontColorBlenderFboId, 0);
		gl.glDeleteTextures(2, g_frontDepthTexId, 0);
		gl.glDeleteTextures(2, g_frontColorTexId, 0);
		gl.glDeleteTextures(1, g_frontColorBlenderTexId, 0);
	}

	//--------------------------------------------------------------------------
	void InitAccumulationRenderTargets(final GL2 gl)
	{
		gl.glGenTextures(2, g_accumulationTexId, 0);

		gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[0]);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S,  GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T,  GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
		gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL.GL_RGBA16F,
				g_imageWidth, g_imageHeight, 0,  GL.GL_RGBA,  GL.GL_FLOAT, null);

		gl.glBindTexture( GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[1]);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_S,  GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_WRAP_T,  GL2.GL_CLAMP);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MIN_FILTER,  GL.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_RECTANGLE_ARB,  GL.GL_TEXTURE_MAG_FILTER,  GL.GL_NEAREST);
		gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL2.GL_FLOAT_R32_NV,
				g_imageWidth, g_imageHeight, 0,  GL.GL_RGBA,  GL.GL_FLOAT, null);

		gl.glGenFramebuffers(1, g_accumulationFboId, 0);
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, g_accumulationFboId[0]);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL.GL_COLOR_ATTACHMENT0,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[0], 0);
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER,  GL2ES2.GL_COLOR_ATTACHMENT1,
				GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[1], 0);

	}

	//--------------------------------------------------------------------------
	void DeleteAccumulationRenderTargets(final GL2 gl)
	{
		gl.glDeleteFramebuffers(1, g_accumulationFboId, 0);
		gl.glDeleteTextures(2, g_accumulationTexId, 0);
	}

	//--------------------------------------------------------------------------
	void MakeFullScreenQuad(final GL2 gl)
	{
		final GLU glu = GLU.createGLU(gl);

		g_quadDisplayList = gl.glGenLists(1);
		gl.glNewList(g_quadDisplayList, GL2.GL_COMPILE);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0.0f, 1.0f, 0.0f, 1.0f);
		gl.glBegin(GL2ES3.GL_QUADS);
		{
			gl.glVertex2f(0.0f, 0.0f);
			gl.glVertex2f(1.0f, 0.0f);
			gl.glVertex2f(1.0f, 1.0f);
			gl.glVertex2f(0.0f, 1.0f);
		}
		gl.glEnd();
		gl.glPopMatrix();

		gl.glEndList();
	}

	//--------------------------------------------------------------------------
	void LoadModel( final GL2 gl, final String model_filename)
	{
		g_model = new Model();
		System.err.println("loading OBJ...\n");

		g_model.loadModelFromFile(DemosDataAnchor.class, model_filename );

		System.err.println("compiling mesh...\n");
		g_model.compileModel();

		System.err.println(g_model.getPositionCount() + " vertices");
		System.err.println((g_model.getIndexCount()/3) + " triangles");
		final int totalVertexSize = g_model.getCompiledVertexCount() * Buffers.SIZEOF_FLOAT;
		final int totalIndexSize = g_model.getCompiledIndexCount() * Buffers.SIZEOF_INT;

		gl.glGenBuffers(1, g_vboId, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, g_vboId[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, totalVertexSize, g_model.getCompiledVertices(), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

		gl.glGenBuffers(1, g_eboId, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, g_eboId[0]);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, totalIndexSize, g_model.getCompiledIndices(), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

		final float[] modelMin = new float[3];
		final float[] modelMax = new float[3];
		g_model.computeBoundingBox(modelMin, modelMax);

		final float[] diag = new float[]{ modelMax[0] - modelMin[0],
				modelMax[1] - modelMin[1],
				modelMax[2] - modelMin[2] };
		g_bbScale = (float)(1.0 / Math.sqrt(diag[0]*diag[0] + diag[1]*diag[1] + diag[2]*diag[2]) * 1.5);
		g_bbTrans = new float[]{ (float)( -g_bbScale * (modelMin[0] + 0.5 * diag[0])),
				(float)( -g_bbScale * (modelMin[1] + 0.5 * diag[1]) ),
				(float)( -g_bbScale * (modelMin[2] + 0.5 * diag[2]) ) };
	}

	//--------------------------------------------------------------------------
	void DrawModel(final GL2 gl)
	{
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, g_vboId[0]);
		gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, g_eboId[0]);
		final int stride = g_model.getCompiledVertexSize() * Buffers.SIZEOF_FLOAT;
		final int normalOffset = g_model.getCompiledNormalOffset() * Buffers.SIZEOF_FLOAT;
		gl.glVertexPointer(g_model.getPositionSize(), GL.GL_FLOAT, stride, 0);
		gl.glNormalPointer(GL.GL_FLOAT, stride, normalOffset);
		gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);

		gl.glDrawElements( GL.GL_TRIANGLES, g_model.getCompiledIndexCount(), GL.GL_UNSIGNED_INT, 0);

		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);

		g_numGeoPasses++;
	}

    public GLCanvas GetCanvas()
    {
        return m_kCanvas;
    }

    ShaderProgram build(final GL2ES2 gl, final String basename, final boolean link) {
    	final ShaderProgram sp = new ShaderProgram();
    	final ShaderCode vp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, DualDepthPeeling.class,
                "shader", null, basename, false);
    	final ShaderCode fp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, DualDepthPeeling.class,
				  "shader", null, basename, false);
    	sp.add(vp);
    	sp.add(fp);
    	if(link && !sp.link(gl, System.err)) {
    		throw new GLException("Couldn't link program: "+sp);
    	}
    	return sp;
    }
    ShaderProgram build(final GL2ES2 gl, final String[] basenames, final boolean link) {
    	final ShaderProgram sp = new ShaderProgram();
		final ShaderCode vp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, basenames.length, DualDepthPeeling.class,
				"shader", basenames, null, null, false);
    	sp.add(vp);
    	final ShaderCode fp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, basenames.length, DualDepthPeeling.class,
				  "shader", basenames, null, null, false);
    	sp.add(fp);
    	if(link && !sp.link(gl, System.err)) {
    		throw new GLException("Couldn't link program: "+sp);
    	}
    	return sp;
    }

	//--------------------------------------------------------------------------
	void BuildShaders(final GL2 gl)
	{
		System.err.println("\nloading shaders...\n");

		g_shaderState = new ShaderState();
		// g_shaderState.setVerbose(true);

		g_shaderDualInit = build(gl, "dual_peeling_init", true);
		g_shaderDualPeel = build(gl, new String[] { "shade", "dual_peeling_peel" }, true);
		g_shaderDualBlend = build(gl, "dual_peeling_blend", true);
		g_shaderDualFinal = build(gl, "dual_peeling_final", true);

		g_shaderFrontInit = build(gl, new String[] { "shade", "front_peeling_init" }, true);
		g_shaderFrontPeel = build(gl, new String[] { "shade", "front_peeling_peel" }, true);
		g_shaderFrontBlend = build(gl, "front_peeling_blend", true);
		g_shaderFrontFinal = build(gl, "front_peeling_final", true);

		g_shaderAverageInit = build(gl, new String[] { "shade", "wavg_init" }, true);
		g_shaderAverageFinal = build(gl, "wavg_final", true);

		g_shaderWeightedSumInit = build(gl, new String[] { "shade", "wsum_init" }, true);
		g_shaderWeightedSumFinal = build(gl, "wsum_final", true);

		g_DepthBlenderTexUnit = new GLUniformData(s_DepthBlenderTex, 0);
		g_shaderState.manage(g_DepthBlenderTexUnit, true);

		g_FrontBlenderTexUnit = new GLUniformData(s_FrontBlenderTex, 1);
		g_shaderState.manage(g_FrontBlenderTexUnit, true);

		g_BackBlenderTexUnit = new GLUniformData(s_BackBlenderTex, 2);
		g_shaderState.manage(g_BackBlenderTexUnit, true);

		g_TempTexUnit = new GLUniformData(s_TempTex, 0);
		g_shaderState.manage(g_TempTexUnit, true);

		g_AlphaUni = new GLUniformData("Alpha", g_opacity);
		g_shaderState.manage(g_AlphaUni, true);

		g_backgroundColorUni = new GLUniformData(s_BackgroundColor, 3, g_backgroundColor);
		g_shaderState.manage(g_backgroundColorUni, true);

		g_ColorTexUnit = new GLUniformData(s_ColorTex, 0);
		g_shaderState.manage(g_ColorTexUnit, true);

		g_ColorTex0Unit = new GLUniformData(s_ColorTex0, 0);
		g_shaderState.manage(g_ColorTex0Unit, true);

		g_ColorTex1Unit = new GLUniformData(s_ColorTex1, 1);
		g_shaderState.manage(g_ColorTex1Unit, true);
	}

	//--------------------------------------------------------------------------
	void DestroyShaders(final GL2 gl)
	{
		g_shaderState.release(gl, false, false, false);
		g_shaderDualInit.destroy(gl);
		g_shaderDualPeel.destroy(gl);
		g_shaderDualBlend.destroy(gl);
		g_shaderDualFinal.destroy(gl);

		g_shaderFrontInit.destroy(gl);
		g_shaderFrontPeel.destroy(gl);
		g_shaderFrontBlend.destroy(gl);
		g_shaderFrontFinal.destroy(gl);

		g_shaderAverageInit.destroy(gl);
		g_shaderAverageFinal.destroy(gl);

		g_shaderWeightedSumInit.destroy(gl);
		g_shaderWeightedSumFinal.destroy(gl);
	}

	//--------------------------------------------------------------------------
	void ReloadShaders(final GL2 gl)
	{
		DestroyShaders(gl);
		BuildShaders(gl);
	}

	/** GLCanvas for Java/JOGL */
	private GLCanvas m_kCanvas;

	void InitGL()
	{

	  	final GLProfile kProfile = GLProfile.getMaxProgrammable(true);
		final GLCapabilities kGlCapabilities = new GLCapabilities(kProfile);
		kGlCapabilities.setHardwareAccelerated(true);
		m_kCanvas = new GLCanvas(kGlCapabilities);
		m_kCanvas.setSize(g_imageWidth, g_imageHeight);
		m_kCanvas.addGLEventListener( this );
		m_kCanvas.addKeyListener( this );
		m_kCanvas.addMouseListener( this );
		m_kCanvas.addMouseMotionListener( this );
	}

	//--------------------------------------------------------------------------
	void RenderDualPeeling(final GL2 gl)
	{
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);

		// ---------------------------------------------------------------------
		// 1. Initialize Min-Max Depth Buffer
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_dualPeelingSingleFboId[0]);

		// Render targets 1 and 2 store the front and back colors
		// Clear to 0.0 and use MAX blending to filter written color
		// At most one front color and one back color can be written every pass
		gl.glDrawBuffers(2, g_drawBuffers, 1);
		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		// Render target 0 stores (-minDepth, maxDepth, alphaMultiplier)
		gl.glDrawBuffer(g_drawBuffers[0]);
		gl.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glBlendEquation(GL2ES3.GL_MAX);

		g_shaderState.attachShaderProgram(gl, g_shaderDualInit, true);
		DrawModel(gl);
		g_shaderState.useProgram(gl, false);

		// ---------------------------------------------------------------------
		// 2. Dual Depth Peeling + Blending
		// ---------------------------------------------------------------------

		// Since we cannot blend the back colors in the geometry passes,
		// we use another render target to do the alpha blending
		//glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, g_dualBackBlenderFboId);
		gl.glDrawBuffer(g_drawBuffers[6]);
		gl.glClearColor(g_backgroundColor.get(0), g_backgroundColor.get(1), g_backgroundColor.get(2), 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		int currId = 0;

		for (int pass = 1; g_useOQ || pass < g_numPasses; pass++) {
			currId = pass % 2;
			final int prevId = 1 - currId;
			final int bufId = currId * 3;

			//glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, g_dualPeelingFboId[currId]);

			gl.glDrawBuffers(2, g_drawBuffers, bufId+1);
			gl.glClearColor(0, 0, 0, 0);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);

			gl.glDrawBuffer(g_drawBuffers[bufId+0]);
			gl.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);

			// Render target 0: RG32F MAX blending
			// Render target 1: RGBA MAX blending
			// Render target 2: RGBA MAX blending
			gl.glDrawBuffers(3, g_drawBuffers, bufId+0);
			gl.glBlendEquation(GL2ES3.GL_MAX);

			// uses g_DepthBlenderTexUnit
			// uses g_FrontBlenderTexUnit
			// uses g_AlphaUni
			g_shaderState.attachShaderProgram(gl, g_shaderDualPeel, true);
			GLHelper.bindTextureRECT(gl, g_dualDepthTexId[prevId], g_DepthBlenderTexUnit.intValue());
			GLHelper.bindTextureRECT(gl, g_dualFrontBlenderTexId[prevId], g_FrontBlenderTexUnit.intValue());
			DrawModel(gl);
			g_shaderState.useProgram(gl, false);

			// Full screen pass to alpha-blend the back color
			gl.glDrawBuffer(g_drawBuffers[6]);

			gl.glBlendEquation(GL.GL_FUNC_ADD);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

			if (g_useOQ) {
				gl.glBeginQuery(GL2GL3.GL_SAMPLES_PASSED, g_queryId[0]);
			}

			g_TempTexUnit.setData(0);
			g_shaderState.attachShaderProgram(gl, g_shaderDualBlend, true);
			GLHelper.bindTextureRECT(gl, g_dualBackTempTexId[currId], g_TempTexUnit.intValue());
			gl.glCallList(g_quadDisplayList);
			g_shaderState.useProgram(gl, false);

			if (g_useOQ) {
				gl.glEndQuery(GL2GL3.GL_SAMPLES_PASSED);
				final int[] sample_count = new int[]{0};
				gl.glGetQueryObjectuiv(g_queryId[0], GL2ES2.GL_QUERY_RESULT, sample_count, 0);
				if (sample_count[0] == 0) {
					break;
				}
			}
		}

		gl.glDisable(GL.GL_BLEND);

		// ---------------------------------------------------------------------
		// 3. Final Pass
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glDrawBuffer(GL.GL_BACK);

		// use g_FrontBlenderTexUnit
		// use g_BackBlenderTexUnit
		g_shaderState.attachShaderProgram(gl, g_shaderDualFinal, true);
		GLHelper.bindTextureRECT(gl, g_dualFrontBlenderTexId[currId], g_FrontBlenderTexUnit.intValue());
		GLHelper.bindTextureRECT(gl, g_dualBackBlenderTexId[0], g_BackBlenderTexUnit.intValue());
		gl.glCallList(g_quadDisplayList);
		g_shaderState.useProgram(gl, false);
	}

	//--------------------------------------------------------------------------
	void RenderFrontToBackPeeling(final GL2 gl)
	{
		// ---------------------------------------------------------------------
		// 1. Initialize Min Depth Buffer
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
		gl.glDrawBuffer(g_drawBuffers[0]);

		gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glEnable(GL.GL_DEPTH_TEST);

		// uses g_AlphaUni
		g_shaderState.attachShaderProgram(gl, g_shaderFrontInit, true);
		DrawModel(gl);
		g_shaderState.useProgram(gl, false);

		// ---------------------------------------------------------------------
		// 2. Depth Peeling + Blending
		// ---------------------------------------------------------------------

		final int numLayers = (g_numPasses - 1) * 2;
		for (int layer = 1; g_useOQ || layer < numLayers; layer++) {
			final int currId = layer % 2;
			final int prevId = 1 - currId;

			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_frontFboId[currId]);
			gl.glDrawBuffer(g_drawBuffers[0]);

			gl.glClearColor(0, 0, 0, 0);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

			gl.glDisable(GL.GL_BLEND);
			gl.glEnable(GL.GL_DEPTH_TEST);

			if (g_useOQ) {
				gl.glBeginQuery(GL2GL3.GL_SAMPLES_PASSED, g_queryId[0]);
			}

			// uses g_DepthBlenderTexUnit
			// uses g_FrontBlenderTexUnit
			// uses g_AlphaUni
			g_shaderState.attachShaderProgram(gl, g_shaderDualPeel, true);
			GLHelper.bindTextureRECT(gl, g_frontDepthTexId[prevId], g_DepthBlenderTexUnit.intValue());
			DrawModel(gl);
			g_shaderState.useProgram(gl, false);

			if (g_useOQ) {
				gl.glEndQuery(GL2GL3.GL_SAMPLES_PASSED);
			}


			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
			gl.glDrawBuffer(g_drawBuffers[0]);

			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glEnable(GL.GL_BLEND);

			gl.glBlendEquation(GL.GL_FUNC_ADD);
			gl.glBlendFuncSeparate(GL.GL_DST_ALPHA, GL.GL_ONE,
					GL.GL_ZERO, GL.GL_ONE_MINUS_SRC_ALPHA);

			// uses g_TempTexUnit
			g_shaderState.attachShaderProgram(gl, g_shaderDualBlend, true);
			GLHelper.bindTextureRECT(gl, g_frontColorTexId[currId], g_TempTexUnit.intValue());
			gl.glCallList(g_quadDisplayList);
			g_shaderState.useProgram(gl, false);

			gl.glDisable(GL.GL_BLEND);

			if (g_useOQ) {
				final int[] sample_count = new int[]{0};
				gl.glGetQueryObjectuiv(g_queryId[0], GL2ES2.GL_QUERY_RESULT, sample_count, 0);
				if (sample_count[0] == 0) {
					break;
				}
			}
		}

		// ---------------------------------------------------------------------
		// 3. Final Pass
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glDrawBuffer(GL.GL_BACK);
		gl.glDisable(GL.GL_DEPTH_TEST);

		// uses g_backgroundColorUni
		g_shaderState.attachShaderProgram(gl, g_shaderFrontFinal, true);
		GLHelper.bindTextureRECT(gl, g_frontColorBlenderTexId[0], g_ColorTexUnit.intValue());
		gl.glCallList(g_quadDisplayList);
		g_shaderState.useProgram(gl, false);
	}

	//--------------------------------------------------------------------------
	void RenderAverageColors(final GL2 gl)
	{
		gl.glDisable(GL.GL_DEPTH_TEST);

		// ---------------------------------------------------------------------
		// 1. Accumulate Colors and Depth Complexity
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_accumulationFboId[0]);
		gl.glDrawBuffers(2, g_drawBuffers, 0);

		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		gl.glBlendEquation(GL.GL_FUNC_ADD);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glEnable(GL.GL_BLEND);

		// uses g_AlphaUni
		g_shaderState.attachShaderProgram(gl, g_shaderAverageInit, true);
		DrawModel(gl);
		g_shaderState.useProgram(gl, false);

		gl.glDisable(GL.GL_BLEND);


		// ---------------------------------------------------------------------
		// 2. Approximate Blending
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glDrawBuffer(GL.GL_BACK);

		// uses g_backgroundColorUni
		g_shaderState.attachShaderProgram(gl, g_shaderAverageFinal, true);
		GLHelper.bindTextureRECT(gl, g_accumulationTexId[0], g_ColorTex0Unit.intValue());
		GLHelper.bindTextureRECT(gl, g_accumulationTexId[1], g_ColorTex1Unit.intValue());
		gl.glCallList(g_quadDisplayList);
		g_shaderState.useProgram(gl, false);
	}

	//--------------------------------------------------------------------------
	void RenderWeightedSum(final GL2 gl)
	{
		gl.glDisable(GL.GL_DEPTH_TEST);

		// ---------------------------------------------------------------------
		// 1. Accumulate (alpha * color) and (alpha)
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, g_accumulationFboId[0]);
		gl.glDrawBuffer(g_drawBuffers[0]);

		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		gl.glBlendEquation(GL.GL_FUNC_ADD);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glEnable(GL.GL_BLEND);

		g_shaderState.attachShaderProgram(gl, g_shaderWeightedSumInit, true);
		DrawModel(gl);
		g_shaderState.useProgram(gl, false);

		gl.glDisable(GL.GL_BLEND);

		// ---------------------------------------------------------------------
		// 2. Weighted Sum
		// ---------------------------------------------------------------------

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glDrawBuffer(GL.GL_BACK);

		g_shaderState.attachShaderProgram(gl, g_shaderWeightedSumFinal, true);
		GLHelper.bindTextureRECT(gl, g_accumulationTexId[0], this.g_ColorTexUnit.intValue());
		gl.glCallList(g_quadDisplayList);
		g_shaderState.useProgram(gl, false);
	}


	public static void main(final String[] args) {
		System.out.println("dual_depth_peeling - sample comparing multiple order independent transparency techniques\n");
		System.out.println("  Commands:\n");
		System.out.println("     A/D       - Change uniform opacity\n");
		System.out.println("     1         - Dual peeling mode\n");
		System.out.println("     2         - Front to back peeling mode\n");
		System.out.println("     3         - Weighted average mode\n");
		System.out.println("     4         - Weighted sum mode\n");
		System.out.println("     R         - Reload all shaders\n");
		System.out.println("     B         - Change background color\n");
		System.out.println("     Q         - Toggle occlusion queries\n");
		System.out.println("     +/-       - Change number of geometry passes\n\n");


		final DualDepthPeeling kWorld = new DualDepthPeeling();
		final Frame frame = new Frame("Dual Depth Peeling");
		frame.add( kWorld.GetCanvas() );
		frame.setSize(kWorld.GetCanvas().getWidth(), kWorld.GetCanvas().getHeight());
		/* Animator serves the purpose of the idle function, calls display: */
		final Animator animator = new Animator( kWorld.GetCanvas() );
		frame.addWindowListener(new WindowAdapter() {
			@Override
            public void windowClosing(final WindowEvent e) {
				// Run this on another thread than the AWT event queue to
				// avoid deadlocks on shutdown on some platforms
				new Thread(new Runnable() {
					@Override
                    public void run() {
						animator.stop();
						System.exit(0);
					}
				}).start();
			}
		});
		frame.setVisible(true);
		animator.start();
	}


	@Override
	public void keyTyped(final KeyEvent e) {
		// TODO Auto-generated method stub

	}


	@Override
	public void keyPressed(final KeyEvent e) {
		// TODO Auto-generated method stub

	}


	@Override
	public void keyReleased(final KeyEvent e) {
		switch(e.getKeyChar())
		{
		case 8:
			g_bShowUI = !g_bShowUI;
			break;
		case 'q':
			g_useOQ = !g_useOQ;
			break;
		case '+':
			g_numPasses++;
			break;
		case '-':
			g_numPasses--;
			break;
		case 'b':
			g_backgroundColor = (g_backgroundColor == g_white) ? g_black : g_white;
			g_backgroundColorUni.setData(g_backgroundColor);
			break;
		case 'o':
			g_showOsd = !g_showOsd;
			break;
		case 'r':
			reloadShaders = true;
			break;
		case '1':
			g_mode = DUAL_PEELING_MODE;
			break;
		case '2':
			g_mode = F2B_PEELING_MODE;
			break;
		case '3':
			g_mode = WEIGHTED_AVERAGE_MODE;
			break;
		case '4':
			g_mode = WEIGHTED_SUM_MODE;
			break;
		case 'a':
			g_opacity -= 0.05;
			g_opacity = (float)Math.max(g_opacity, 0.0);
			g_AlphaUni.setData(g_opacity);
			break;
		case 'd':
			g_opacity += 0.05;
			g_opacity = (float)Math.min(g_opacity, 1.0);
			g_AlphaUni.setData(g_opacity);
			break;
		}
	}


	@Override
	public void display(final GLAutoDrawable arg0) {
		final GL2 gl = arg0.getGL().getGL2();

		if(reloadShaders) {
			reloadShaders = false;
			ReloadShaders(gl);
		}
		final GLU glu = GLU.createGLU(gl);

		g_numGeoPasses = 0;

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();
		glu.gluLookAt(g_pos[0], g_pos[1], g_pos[2], g_pos[0], g_pos[1], 0, 0, 1, 0);
		gl.glRotatef(g_rot[0], 1, 0, 0);
		gl.glRotatef(g_rot[1], 0, 1, 0);
		gl.glTranslatef(g_bbTrans[0], g_bbTrans[1], g_bbTrans[2]);
		gl.glScalef(g_bbScale, g_bbScale, g_bbScale);

		switch (g_mode) {
		case DUAL_PEELING_MODE:
			RenderDualPeeling(gl);
			break;
		case F2B_PEELING_MODE:
			RenderFrontToBackPeeling(gl);
			break;
		case WEIGHTED_AVERAGE_MODE:
			RenderAverageColors(gl);
			break;
		case WEIGHTED_SUM_MODE:
			RenderWeightedSum(gl);
			break;
		}

        /* Call swapBuffers to render on-screen: */
		arg0.swapBuffers();
	}


	@Override
	public void dispose(final GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void init(final GLAutoDrawable drawable) {
		System.err.println( "init" );
		final GL2 gl = drawable.getGL().getGL2();

		m_kCanvas.setAutoSwapBufferMode( false );

		// Allocate render targets first
		try {
			InitDualPeelingRenderTargets(gl);
		} catch ( final GLException e )
		{
			try {
				InitDualPeelingRenderTargets(gl);
			} catch ( final GLException e1 )
			{
				System.err.println( e1.getStackTrace() );
			}
		}
		InitFrontPeelingRenderTargets(gl);
		InitAccumulationRenderTargets(gl);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

		BuildShaders(gl);
		LoadModel(gl, MODEL_FILENAME);
		MakeFullScreenQuad(gl);

		gl.glDisable(GL.GL_CULL_FACE);
		gl.glDisable(GLLightingFunc.GL_LIGHTING);
		gl.glDisable(GLLightingFunc.GL_NORMALIZE);

		gl.glGenQueries(1, g_queryId, 0);
	}


	@Override
	public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2 gl = drawable.getGL().getGL2();

		if (g_imageWidth != width || g_imageHeight != height)
		{
			g_imageWidth = width;
			g_imageHeight = height;

			DeleteDualPeelingRenderTargets(gl);
			InitDualPeelingRenderTargets(gl);

			DeleteFrontPeelingRenderTargets(gl);
			InitFrontPeelingRenderTargets(gl);

			DeleteAccumulationRenderTargets(gl);
			InitAccumulationRenderTargets(gl);
		}

		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glLoadIdentity();

		final GLU glu = GLU.createGLU(gl);
		glu.gluPerspective(FOVY, (float)g_imageWidth/(float)g_imageHeight, ZNEAR, ZFAR);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

		gl.glViewport(0, 0, g_imageWidth, g_imageHeight);
	}


	@Override
	public void mouseDragged(final MouseEvent e) {

		g_oldX = g_newX; g_oldY = g_newY;
		g_newX = e.getX();
		g_newY = e.getY();

		final float rel_x = (g_newX - g_oldX) / (float)g_imageWidth;
		final float rel_y = (g_newY - g_oldY) / (float)g_imageHeight;
		if (g_rotating)
		{
			g_rot[1] += (rel_x * 180);
			g_rot[0] += (rel_y * 180);
		}
		else if (g_panning)
		{
			g_pos[0] -= rel_x;
			g_pos[1] += rel_y;
		}
		else if (g_scaling)
		{
			g_pos[2] -= rel_y * g_pos[2];
		}

	}


	@Override
	public void mouseMoved(final MouseEvent e) {}


	@Override
	public void mouseClicked(final MouseEvent e) {}


	@Override
	public void mousePressed(final MouseEvent e) {

		g_newX = e.getX();
		g_newY = e.getY();

		g_scaling = false;
		g_panning = false;
		g_rotating = false;

		if (e.getButton() == MouseEvent.BUTTON1)
		{
			if (e.isShiftDown()) {
				g_scaling = true;
			} else if (e.isControlDown()) {
				g_panning = true;
			} else {
				g_rotating = true;
			}
		}
	}


	@Override
	public void mouseReleased(final MouseEvent e) {}


	@Override
	public void mouseEntered(final MouseEvent e) {}


	@Override
	public void mouseExited(final MouseEvent e) {}
}
