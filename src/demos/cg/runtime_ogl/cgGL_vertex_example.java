/*
 * Portions Copyright (C) 2003 Sun Microsystems, Inc.
 * All rights reserved.
 */

/*
 *
 * COPYRIGHT NVIDIA CORPORATION 2003. ALL RIGHTS RESERVED.
 * BY ACCESSING OR USING THIS SOFTWARE, YOU AGREE TO:
 *
 *  1) ACKNOWLEDGE NVIDIA'S EXCLUSIVE OWNERSHIP OF ALL RIGHTS
 *     IN AND TO THE SOFTWARE;
 *
 *  2) NOT MAKE OR DISTRIBUTE COPIES OF THE SOFTWARE WITHOUT
 *     INCLUDING THIS NOTICE AND AGREEMENT;
 *
 *  3) ACKNOWLEDGE THAT TO THE MAXIMUM EXTENT PERMITTED BY
 *     APPLICABLE LAW, THIS SOFTWARE IS PROVIDED *AS IS* AND
 *     THAT NVIDIA AND ITS SUPPLIERS DISCLAIM ALL WARRANTIES,
 *     EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED
 *     TO, IMPLIED WARRANTIES OF MERCHANTABILITY  AND FITNESS
 *     FOR A PARTICULAR PURPOSE.
 *
 * IN NO EVENT SHALL NVIDIA OR ITS SUPPLIERS BE LIABLE FOR ANY
 * SPECIAL, INCIDENTAL, INDIRECT, OR CONSEQUENTIAL DAMAGES
 * WHATSOEVER (INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS
 * OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS
 * INFORMATION, OR ANY OTHER PECUNIARY LOSS), INCLUDING ATTORNEYS'
 * FEES, RELATING TO THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package demos.cg.runtime_ogl;

import com.sun.opengl.cg.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * cgGL_vertex_example: simple demo of Nvidia CgGL API. Based upon C version
 * distributed with the NVidia Cg Toolkit. Ported to the Java language by
 * Christopher Kline 5/29/2003.
 */
public class cgGL_vertex_example implements GLEventListener 
{
  
  /******************************************************************************/
  /*** Static Data                                                            ***/
  /******************************************************************************/

  private final int TextureRes = 512;
  
  private GLU glu = new GLU();
  private static CGcontext Context = null;
  private static CGprogram Program = null;
  private static CGparameter KdParam = null;
  private static CGparameter ModelViewProjParam = null;
  private static CGparameter TestColorParam = null;
  private static /*CGprofile*/ int profile;
  
  static
  {
    String os = System.getProperty("os.name").toLowerCase();
    profile = os.startsWith("mac os") ? CgGL.CG_PROFILE_ARBVP1 : CgGL.CG_PROFILE_VP20;
  }
  
  private float LightDiffuse[] = {1.0f, 0.0f, 0.0f, 1.0f};  
  private float LightPosition[] = {1.0f, 1.0f, 1.0f, 0.0f};  

  private float CubeNormals[/*6*/][/*3*/] = 
  {  
    {-1.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f},
    {1.0f, 0.0f, 0.0f}, {0.0f, -1.0f, 0.0f},
    {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, -1.0f} 
  };

  private int CubeFaces[/*6*/][/*4*/] = 
  {  
    {0, 1, 2, 3}, {3, 2, 6, 7}, {7, 6, 5, 4},
    {4, 5, 1, 0}, {5, 6, 2, 1}, {7, 4, 0, 3} 
  };

  private float CubeVertices[][] = new float[8][3];  

/******************************************************************************/

  private int MATRIX_INDEX(int i, int j) { return (j + i * 4); }

  private static void CheckCgError()
  {
    /*CGerror*/ int err = CgGL.cgGetError();

    if (err != CgGL.CG_NO_ERROR)
    {
      System.out.println("CG error: " + CgGL.cgGetErrorString(err));
      System.exit(1);
    }
  }


  private void DrawCube(GL gl)
  {
    int i;

    CgGL.cgGLBindProgram(Program);
    CheckCgError();

    /*
     * Set various uniform parameters including the ModelViewProjection
     * matrix for transforming the incoming position into HPOS.
     */
    if(KdParam != null)
      CgGL.cgGLSetParameter4f(KdParam, 1.0f, 1.0f, 0.0f, 1.0f);
    
    /* Set the concatenate modelview and projection matrices */
    if(ModelViewProjParam != null)
      CgGL.cgGLSetStateMatrixParameter(ModelViewProjParam,
                                  CgGL.CG_GL_MODELVIEW_PROJECTION_MATRIX,
                                  CgGL.CG_GL_MATRIX_IDENTITY);

    CgGL.cgGLEnableProfile(profile);


    /*
     * Create cube with per-vertex varying attributes 
     */
    for(i = 0; i < 6; i++) 
    {
      gl.glBegin(GL.GL_QUADS);

      gl.glNormal3f(CubeNormals[i][0], CubeNormals[i][1], CubeNormals[i][0]);
      CgGL.cgGLSetParameter3f(TestColorParam, 1.0f, 0.0f, 0.0f);
      float[] verts = CubeVertices[CubeFaces[i][0]];
      gl.glVertex3f(verts[0], verts[1], verts[2]);

      CgGL.cgGLSetParameter3f(TestColorParam, 0.0f, 1.0f, 0.0f);
      verts = CubeVertices[CubeFaces[i][1]];
      gl.glVertex3f(verts[0], verts[1], verts[2]);

      CgGL.cgGLSetParameter3f(TestColorParam, 0.0f, 0.0f, 1.0f);
      verts = CubeVertices[CubeFaces[i][2]];
      gl.glVertex3f(verts[0], verts[1], verts[2]);

      CgGL.cgGLSetParameter3f(TestColorParam, 1.0f, 1.0f, 1.0f);
      verts = CubeVertices[CubeFaces[i][3]];
      gl.glVertex3f(verts[0], verts[1], verts[2]);
    
      gl.glEnd();
    }

    CgGL.cgGLDisableProfile(profile);
  }

  public void display(GLAutoDrawable drawable) 
  {
    GL gl = drawable.getGL();
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    DrawCube(gl);
    //glutSwapBuffers();
  }

  void InitializeCube(float v[/*8*/][/*3*/])
  {
    /* Setup cube vertex data. */
    v[0][0] = v[1][0] = v[2][0] = v[3][0] = -1;
    v[4][0] = v[5][0] = v[6][0] = v[7][0] = 1;
    v[0][1] = v[1][1] = v[4][1] = v[5][1] = -1;
    v[2][1] = v[3][1] = v[6][1] = v[7][1] = 1;
    v[0][2] = v[3][2] = v[4][2] = v[7][2] = 1;
    v[1][2] = v[2][2] = v[5][2] = v[6][2] = -1;
  }
  
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
  {
    // nothing
  }

  public void init(GLAutoDrawable drawable) 
  {
    // Note: we initialize Cg in this init() method instead of main()
    // because Cg (apparently) requires an active OpenGL context in order to
    // initialize correctly (otherwise the CheckCgError() call after
    // cgGLLoadProgram() will fail).
    
    /* Test cgContext creation */
    Context = CgGL.cgCreateContext();
    CheckCgError();

    /* Test adding source text to context */
    try {
      Program = CgGL.cgCreateProgramFromStream(
        Context, CgGL.CG_SOURCE,
        getClass().getClassLoader().getResourceAsStream("demos/cg/runtime_ogl/cgGL_vertex_example.cg"),
        profile, null, null);
    } catch (IOException e) {
      throw new RuntimeException("Error loading Cg vertex program", e);
    }
    CheckCgError();

    System.err.println(
      "LAST LISTING----" + CgGL.cgGetLastListing(Context) + "----\n");

    System.err.println(
      "---- PROGRAM BEGIN ----\n"+
      CgGL.cgGetProgramString(Program, CgGL.CG_COMPILED_PROGRAM)+
      "---- PROGRAM END ----\n");

    if(Program != null)
    {
      CgGL.cgGLLoadProgram(Program);
      CheckCgError();

      KdParam = CgGL.cgGetNamedParameter(Program, "Kd");
      CheckCgError();

      ModelViewProjParam = CgGL.cgGetNamedParameter(Program, "ModelViewProj");
      CheckCgError();

      TestColorParam = CgGL.cgGetNamedParameter(Program, "IN.TestColor");
      CheckCgError();
    }
    GL gl = drawable.getGL();

    InitializeCube(CubeVertices);

    /* Enable a single OpenGL light. */
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, LightDiffuse, 0);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, LightPosition, 0);
    gl.glEnable(GL.GL_LIGHT0);
    if (false) { // #if 0
      gl.glEnable(GL.GL_LIGHTING);
    } else {     // #else
      gl.glDisable(GL.GL_LIGHTING);
    }            // #endif

    /* Use depth buffering for hidden surface elimination. */
    gl.glEnable(GL.GL_DEPTH_TEST);

    /* Setup the view of the cube. */
    gl.glMatrixMode(GL.GL_PROJECTION);
    glu.gluPerspective( /* field of view in degree */ 40.0f,
                    /* aspect ratio */ 1.0f,
                    /* Z near */ 1.0f, /* Z far */ 10.0f);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    glu.gluLookAt(0.0f, 0.0f, 5.0f,  /* eye is at (0,0,5) */
              0.0f, 0.0f, 0.0f,  /* center is at (0,0,0) */
              0.0f, 1.0f, 0.);  /* up is in positive Y direction */

    /* Adjust cube position to be asthetic angle. */
    gl.glTranslatef(0.0f, 0.0f, -1.0f);
    if (true) { // #if 1
      gl.glRotatef(60, 1.0f, 0.0f, 0.0f);
      gl.glRotatef(-20, 0.0f, 0.0f, 1.0f);
    } //#endif
  
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
  {
    // do nothing
  }

  public static void main(String[] argv)
  {    
    Frame frame = new Frame("NVidia Cg Toolkit \"cgGL_vertex_example\" demo");
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new cgGL_vertex_example());

    frame.add(canvas);
    frame.setSize(500, 500);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          // Run this on another thread than the AWT event queue to
          // avoid deadlocks on shutdown on some platforms
          new Thread(new Runnable() {
              public void run() {
                System.exit(0);
              }
            }).start();
        }
      });
    frame.show();
  }
}   
