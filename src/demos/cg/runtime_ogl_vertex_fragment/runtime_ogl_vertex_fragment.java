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

package demos.cg.runtime_ogl_vertex_fragment;

import com.sun.opengl.cg.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Basic example of the use of the Cg runtime in a simple OpenGL program.
 * Ported to Java from NVidia's original C source by Christopher Kline, 06
 * June 2003. Original NVidia copyright is preserved in the source code.
 */
public class runtime_ogl_vertex_fragment implements GLEventListener 
{

  // Global variables: hold the Cg context that we're storing our programs
  // in as well as handles to the vertex and fragment program used in this
  // demo.

  private GLU glu = new GLU();
  CGcontext context;
  CGprogram vertexProgram, fragmentProgram;

  ///////////////////////////////////////////////////////////////////////////

  // Main program; do basic GLUT and Cg setup, but leave most of the work
  // to the display() function.

  public static void main(String[] argv)
  {    
    Frame frame = new Frame("Cg demo (runtime_ogl_vertex_fragment)");
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new runtime_ogl_vertex_fragment());

    frame.add(canvas);
    frame.setSize(512, 512);
    final Animator animator = new Animator(canvas);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          // Run this on another thread than the AWT event queue to
          // make sure the call to Animator.stop() completes before
          // exiting
          new Thread(new Runnable() {
              public void run() {
                animator.stop();
                System.exit(0);
              }
            }).start();
        }
      });
    frame.show();
    animator.start();

    // and all the rest happens in the display function...
  }

  public void init(GLAutoDrawable drawable) 
  {
    // Use debug pipeline
    // drawable.setGL(new DebugGL(drawable.getGL()));

    GL gl = drawable.getGL();
    
    // Basic Cg setup; register a callback function for any errors
    // and create an initial context
    //cgSetErrorCallback(handleCgError); // not yet exposed in Cg binding
    context = CgGL.cgCreateContext();
  
    // Do one-time setup only once; setup Cg programs and textures
    // and set up OpenGL state.
    ChooseProfiles();
    LoadCgPrograms();
    LoadTextures(gl);

    gl.glEnable(GL.GL_DEPTH_TEST);
  }

  private void CheckCgError()
  {
    /*CGerror*/ int err = CgGL.cgGetError();

    if (err != CgGL.CG_NO_ERROR)
    {
      throw new RuntimeException("CG error: " + CgGL.cgGetErrorString(err));
    }
  }

  private static int curTime = 0;
  
  // display callback function
  public void display(GLAutoDrawable drawable) 
  {

    GL gl = drawable.getGL();
    
    // The usual OpenGL stuff to clear the screen and set up viewing.
    gl.glClearColor(.25f, .25f, .25f, 1.0f);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(30.0f, 1.0f, .1f, 100);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    glu.gluLookAt(4, 4, -4, 0, 0, 0, 0, 1, 0);

    // Make the object rotate a bit each time the display function
    // is called
    gl.glRotatef(curTime, 0, 1, 0);

    // Now make sure that the vertex and fragment programs, loaded
    // in LoadCgPrograms() are bound.
    CgGL.cgGLBindProgram(vertexProgram);
    CgGL.cgGLBindProgram(fragmentProgram);

    // Bind uniform parameters to vertex shader
    CgGL.cgGLSetStateMatrixParameter(CgGL.cgGetNamedParameter(vertexProgram, "ModelViewProj"),
                                     CgGL.CG_GL_MODELVIEW_PROJECTION_MATRIX,
                                     CgGL.CG_GL_MATRIX_IDENTITY);
    CgGL.cgGLSetStateMatrixParameter(CgGL.cgGetNamedParameter(vertexProgram, "ModelView"),
                                     CgGL.CG_GL_MODELVIEW_MATRIX,
                                     CgGL.CG_GL_MATRIX_IDENTITY);
    CgGL.cgGLSetStateMatrixParameter(CgGL.cgGetNamedParameter(vertexProgram, "ModelViewIT"),
                                     CgGL.CG_GL_MODELVIEW_MATRIX,
                                     CgGL.CG_GL_MATRIX_INVERSE_TRANSPOSE);

    // We can also go ahead and bind varying parameters to vertex shader
    // that we just want to have the same value for all vertices.  The
    // vertex shader could be modified so that these were uniform for
    // better efficiency, but this gives us flexibility for the future.
    float Kd[] = { .7f, .2f, .2f }, Ks[] = { .9f, .9f, .9f };
    CgGL.cgGLSetParameter3fv(CgGL.cgGetNamedParameter(vertexProgram, "diffuse"), Kd, 0);
    CgGL.cgGLSetParameter3fv(CgGL.cgGetNamedParameter(vertexProgram, "specular"), Ks, 0);

    // Now bind uniform parameters to fragment shader
    float lightPos[] = { 3, 2, -3 };
    CgGL.cgGLSetParameter3fv(CgGL.cgGetNamedParameter(fragmentProgram, "Plight"), lightPos, 0);
    float lightColor[] = { 1, 1, 1 };
    CgGL.cgGLSetParameter3fv(CgGL.cgGetNamedParameter(fragmentProgram, "lightColor"), 
                             lightColor, 0);
    CgGL.cgGLSetParameter1f(CgGL.cgGetNamedParameter(fragmentProgram, "shininess"), 40);

    // And finally, enable the approprate texture for fragment shader; the
    // texture was originally set up in LoadTextures().
    CgGL.cgGLEnableTextureParameter(CgGL.cgGetNamedParameter(fragmentProgram,
                                                             "diffuseMap"));
    // And go ahead and draw the scene geometry
    DrawGeometry(gl);

    // Disable the texture now that we're done with it.
    CgGL.cgGLDisableTextureParameter(CgGL.cgGetNamedParameter(fragmentProgram,
                                                              "diffuseMap"));

    ++curTime;
  }


  // Choose the vertex and fragment profiles to use.  Try to use
  // CG_PROFILE_ARBVFP1 and CG_PROFILE_ARBFP1, depending on hardware support.
  // If those aren't available, fall back to CG_PROFILE_VP30 and
  // CG_PROFILE_FP30, respectively.

  int /*CGprofile*/ vertexProfile, fragmentProfile;

  void ChooseProfiles()
  {
    // Make sure that the appropriate profiles are available on the
    // user's system.
    if (CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_ARBVP1))
      vertexProfile = CgGL.CG_PROFILE_ARBVP1;
    else {
      // try VP30
      if (CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_VP30))
        vertexProfile = CgGL.CG_PROFILE_VP30;
      else {
        System.out.println("Neither arbvp1 or vp30 vertex profiles supported on this system.\n");
        System.exit(1);
      }
    }

    if (CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_ARBFP1))
      fragmentProfile = CgGL.CG_PROFILE_ARBFP1;
    else {
      // try FP30
      if (CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_FP30))
        fragmentProfile = CgGL.CG_PROFILE_FP30;
      else {
        System.out.println("Neither arbfp1 or fp30 fragment profiles supported on this system.\n");
        System.exit(1);
      }
    }
  }


  void LoadCgPrograms()
  {
    assert(CgGL.cgIsContext(context));

    // Load and compile the vertex program from demo_vert.cg; hold on to the
    // handle to it that is returned.
    try {
      vertexProgram = CgGL.cgCreateProgramFromStream(context, CgGL.CG_SOURCE,
                                                     getClass().getClassLoader().getResourceAsStream("demos/cg/runtime_ogl_vertex_fragment/demo_vert.cg"),
                                                     vertexProfile, null, null);
    } catch (IOException e) {
      throw new RuntimeException("Error loading Cg vertex program", e);
    }
    if (!CgGL.cgIsProgramCompiled(vertexProgram))
      CgGL.cgCompileProgram(vertexProgram);

    // Enable the appropriate vertex profile and load the vertex program.
    CgGL.cgGLEnableProfile(vertexProfile);
    CgGL.cgGLLoadProgram(vertexProgram);

    // And similarly set things up for the fragment program.
    try {
      fragmentProgram = CgGL.cgCreateProgramFromStream(context, CgGL.CG_SOURCE,
                                                       getClass().getClassLoader().getResourceAsStream("demos/cg/runtime_ogl_vertex_fragment/demo_frag.cg"),
                                                       fragmentProfile, null, null);
    } catch (IOException e) {
      throw new RuntimeException("Error loading Cg fragment program", e);
    }
    if (!CgGL.cgIsProgramCompiled(fragmentProgram)) {
      CgGL.cgCompileProgram(fragmentProgram);
    }
    
    CgGL.cgGLEnableProfile(fragmentProfile);
    CgGL.cgGLLoadProgram(fragmentProgram);
  }

  void LoadTextures(GL gl)
  {
    // There is only one texture needed here--we'll set up a basic
    // checkerboard--which is used to modulate the diffuse channel in the
    // fragment shader.
    int[] handle = new int[1];
    gl.glGenTextures(1, handle, 0);

    // Basic OpenGL texture state setup
    gl.glBindTexture(GL.GL_TEXTURE_2D, handle[0]);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    // Fill in the texture map.
    final int RES = 512;
    float[] data = new float[RES*RES*4];
    int dp = 0;
    for (int i = 0; i < RES; ++i) {
      for (int j = 0; j < RES; ++j) {
        if ((i/32+j/32) % 2 != 0) {
          data[dp++] = .7f;
          data[dp++] = .7f;
          data[dp++] = .7f;
        }
        else {
          data[dp++] = .1f;
          data[dp++] = .1f;
          data[dp++] = .1f;
        }
        data[dp++] = 1.0f;
      }
    }

    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, RES, RES, 0, GL.GL_RGBA, GL.GL_FLOAT, FloatBuffer.wrap(data));

    // Tell Cg which texture handle should be associated with the sampler2D
    // parameter to the fragment shader.
    CgGL.cgGLSetTextureParameter(CgGL.cgGetNamedParameter(fragmentProgram, "diffuseMap"),
                                 handle[0]);
  }
  
  private int VERTEX(int u, int v, int nu) { return (u + v * nu); }

  // Geometry creation and drawing function; we'll just draw a sphere.

  private static FloatBuffer P, N, uv;
  private static IntBuffer indices;
  void DrawGeometry(GL gl)
  {
    // Cache the sphere positions, normals, texture coordinates, and 
    // vertex indices in a local array; we only need to fill them in the
    // first time through this function.
    int nu  = 30, nv = 30;
    int nTris = 2*(nu-1)*(nv-1), nVerts = nu*nv;
    if (P == null) {
      int u, v;

      P = BufferUtil.newFloatBuffer(3*nVerts);
      N = BufferUtil.newFloatBuffer(3*nVerts);
      uv = BufferUtil.newFloatBuffer(2*nVerts);

      // Fill in the position, normal, and texture coordinate arrays.
      // Just loop over all of the vertices, compute their parametreic
      // (u,v) coordinates (which we use for texture coordinates as
      // well), and call the ParametricEval() function, which turns (u,v)
      // coordinates into positions and normals on the surface of the
      // object.
      int pp = 0, np = 0, uvp = 0;
      for (v = 0; v < nv; ++v) {
        float fv = (float)v / (float)(nv-1);
        for (u = 0; u < nu; ++u) {
          float fu = (float)u / (float)(nu-1);
          uv.put(uvp, fu);
          uv.put(uvp+1, fv);
          ParametricEval(fu, fv, pp, P, np, N);
          pp += 3;
          np += 3;
          uvp += 2;
        }
      }

      // Now fill in the vertex index arrays
      indices = BufferUtil.newIntBuffer(3*nTris);
      int ip = 0;
      for (v = 0; v < nv-1; ++v) {
        for (u = 0; u < nu-1; ++u) {
          indices.put(ip++, VERTEX(u, v, nu));
          indices.put(ip++, VERTEX(u+1, v, nu));
          indices.put(ip++, VERTEX(u+1, v+1, nu));

          indices.put(ip++, VERTEX(u, v, nu));
          indices.put(ip++, VERTEX(u+1, v+1, nu));
          indices.put(ip++, VERTEX(u, v+1, nu));
        }
      }
      // Tell Cg which of these data pointers are associated with which
      // parameters to the vertex shader, so that when we call
      // cgGLEnableClientState() and then glDrawElements(), the shader
      // gets the right input information.
      CGparameter param = CgGL.cgGetNamedParameter(vertexProgram, "Pobject");
      CgGL.cgGLSetParameterPointer(param, 3, GL.GL_FLOAT, 0, P);
      param = CgGL.cgGetNamedParameter(vertexProgram, "Nobject");
      CgGL.cgGLSetParameterPointer(param, 3, GL.GL_FLOAT, 0, N);
      param = CgGL.cgGetNamedParameter(vertexProgram, "TexUV");
      CgGL.cgGLSetParameterPointer(param, 2, GL.GL_FLOAT, 0, uv);
    }

    // And now, each time through, enable the bindings to the parameters
    // that we set up the first time through
    CGparameter param = CgGL.cgGetNamedParameter(vertexProgram, "Pobject");
    CgGL.cgGLEnableClientState(param);
    param = CgGL.cgGetNamedParameter(vertexProgram, "Nobject");
    CgGL.cgGLEnableClientState(param);
    param = CgGL.cgGetNamedParameter(vertexProgram, "TexUV");
    CgGL.cgGLEnableClientState(param);

    // Enable the texture parameter as well.
    param = CgGL.cgGetNamedParameter(fragmentProgram, "diffuseMap");
    CgGL.cgGLEnableTextureParameter(param);

    // And now, draw the geometry.
    gl.glDrawElements(GL.GL_TRIANGLES, 3*nTris, GL.GL_UNSIGNED_INT, indices);

    // Be a good citizen and disable the various bindings we set up above.
    param = CgGL.cgGetNamedParameter(vertexProgram, "Pobject");
    CgGL.cgGLDisableClientState(param);
    param = CgGL.cgGetNamedParameter(vertexProgram, "Nobject");
    CgGL.cgGLDisableClientState(param);
    param = CgGL.cgGetNamedParameter(vertexProgram, "TexUV");
    CgGL.cgGLDisableClientState(param);

    param = CgGL.cgGetNamedParameter(fragmentProgram, "diffuseMap");
    CgGL.cgGLDisableTextureParameter(param);
  }

  void ParametricEval(float u, float v, int offsetP, FloatBuffer p, int offsetN, FloatBuffer N)
  {
    float theta = (float)Math.PI * u, phi = (float)(2.0 * Math.PI * v);
    P.put(offsetP + 0, (float)(Math.sin(theta) * Math.sin(phi)));
    P.put(offsetP + 1, (float)(Math.sin(theta) * Math.cos(phi)));
    P.put(offsetP + 2, (float)(Math.cos(theta)));

    N.put(offsetN + 0, P.get(offsetP + 0));
    N.put(offsetN + 1, P.get(offsetP + 1));
    N.put(offsetN + 2, P.get(offsetP + 2));
  }

  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
  {
    // nothing
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
  {
    // do nothing
  }

}
