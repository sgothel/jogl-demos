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

package demos.vertexProgWarp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import demos.common.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/**
   Simple space-warp/distortion vertex program demo<br>
   (Press the space bar to switch through programs)<br><p>

   sgreen@nvidia.com 9/2000, based on Cass's vtxprog_silhouette<br><p>

        Ported to Java by Kenneth Russell
*/

public class VertexProgWarp extends Demo {
  private Frame    frame;
  private Animator animator;
  private volatile boolean quit;

  private GLAutoDrawable drawable;
  private DurationTimer timer = new DurationTimer();
  private boolean  firstRender = true;
  private int      frameCount;

  public static void main(String[] args) {
    new VertexProgWarp().run(args);
  }

  public void run(String[] args) {
    GLCanvas canvas = new GLCanvas();
    VertexProgWarp demo = new VertexProgWarp();
    canvas.addGLEventListener(demo);

    final Animator animator = new Animator(canvas);
    demo.setDemoListener(new DemoListener() {
        public void shutdownDemo() {
          runExit(animator);
        }
        public void repaint() {}
      });

    final Frame frame = new Frame();
    demo.setTitleSetter(new VertexProgWarp.TitleSetter() {
        public void setTitle(String title) {
          frame.setTitle(title);
        }
      });
    frame.setLayout(new BorderLayout());
    canvas.setSize(512, 512);
    frame.add(canvas, BorderLayout.CENTER);
    frame.pack();
    frame.show();
    canvas.requestFocus();

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          runExit(animator);
        }
      });

    animator.start();
  }

  public static abstract class TitleSetter {
    public abstract void setTitle(String title);
  }

  public void setTitleSetter(TitleSetter setter) {
    titleSetter = setter;
  }

  private TitleSetter titleSetter;
  private boolean initComplete;
  
  // period of 4-term Taylor approximation to sin isn't quite 2*M_PI
  private static final float    SIN_PERIOD   = 3.079f;
  private static final int      NUM_OBJS     = 5;
  private static final int      NUM_PROGS    = 7;
  private              int[]    programs     = new int[NUM_PROGS];
  private float zNear = 0.1f;
  private float zFar  = 10.0f;
  private int program = 2;
  private int obj = 2;
  private boolean[] b = new boolean[256];
  private boolean wire = false;
  private boolean toggleWire = false;
  private boolean animating = true;
  private boolean doViewAll = true;

  private Time  time = new SystemTime();
  private float anim = 0.0f;
  private float animScale = 7.0f;
  private float amp  = 0.05f;
  private float freq = 8.0f;
  private float d    = 4.0f;

  private GLU glu = new GLU();
  private ExaminerViewer viewer;

  public void init(GLAutoDrawable drawable) {
    initComplete = false;
    GL gl = drawable.getGL();

    float cc = 0.0f;
    gl.glClearColor(cc, cc, cc, 1);

    gl.glColor3f(1,1,1);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDisable(GL.GL_CULL_FACE);

    try {
      initExtension(gl, "GL_ARB_vertex_program");
    } catch (RuntimeException e) {
      shutdownDemo();
      throw(e);
    }

    for(int i=0; i<NUM_OBJS; i++) {
      gl.glNewList(i+1, GL.GL_COMPILE);
      drawObject(gl, i);
      gl.glEndList();
    }    

    for(int i=0; i<NUM_PROGS; i++) {
      int[] vtxProgTmp = new int[1];
      gl.glGenProgramsARB(1, vtxProgTmp, 0);
      programs[i] = vtxProgTmp[0];
      gl.glBindProgramARB(GL.GL_VERTEX_PROGRAM_ARB, programs[i]);
      gl.glProgramStringARB(GL.GL_VERTEX_PROGRAM_ARB, GL.GL_PROGRAM_FORMAT_ASCII_ARB, programTexts[i].length(), programTexts[i]);
    }

    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 0, 0.0f, 0.0f, 1.0f, 0.0f);   // light position/direction
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 1, 0.0f, 1.0f, 0.0f, 0.0f);   // diffuse color
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 2, 1.0f, 1.0f, 1.0f, 0.0f);   // specular color

    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 3, 0.0f, 1.0f, 2.0f, 3.0f);   // smoothstep constants

    // sin Taylor series constants - 1, 1/3!, 1/5!, 1/7!
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 4, 1.0f, 1.0f / (3*2), 1.0f / (5*4*3*2), 1.0f / (7*6*5*4*3*2));

    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 5, 1.0f / (2.0f * SIN_PERIOD), 2.0f * SIN_PERIOD, SIN_PERIOD, SIN_PERIOD/2.0f);

    // sin wave frequency, amplitude
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 6, 1.0f, 0.2f, 0.0f, 0.0f);

    // phase animation
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 7, 0.0f, 0.0f, 0.0f, 0.0f);

    // fisheye sphere radius
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 8, 1.0f, 0.0f, 0.0f, 0.0f);

    setWindowTitle();

    doViewAll = true;

    b['p'] = true;
      
    drawable.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          dispatchKey(e.getKeyCode(), e.getKeyChar());
        }
      });

    // Register the window with the ManipManager
    ManipManager manager = ManipManager.getManipManager();
    manager.registerWindow(drawable);
    this.drawable = drawable;

    viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
    viewer.setNoAltKeyMode(true);
    viewer.setAutoRedrawMode(false);
    viewer.attach(drawable, new BSphereProvider() {
        public BSphere getBoundingSphere() {
          return new BSphere(new Vec3f(0, 0, 0), 1.0f);
        }
      });
    viewer.setVertFOV((float) Math.toRadians(60));
    viewer.setZNear(zNear);
    viewer.setZFar(zFar);
    initComplete = true;
  }

  public void display(GLAutoDrawable drawable) {
    if (!initComplete) {
      return;
    }

    if (!firstRender) {
      if (++frameCount == 30) {
        timer.stop();
        System.err.println("Frames per second: " + (30.0f / timer.getDurationAsSeconds()));
        timer.reset();
        timer.start();
        frameCount = 0;
      }
    } else {
      firstRender = false;
      timer.start();
    }

    time.update();

    GL gl = drawable.getGL();

    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    if (toggleWire) {
      wire = !wire;
      if (wire)
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
      else
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
      toggleWire = false;
    }

    gl.glPushMatrix();

    if (doViewAll) {
      viewer.viewAll(gl);
      doViewAll = false;
    }

    if (animating) {
      anim -= (float) (animScale * time.deltaT());
    }

    viewer.update(gl);
    ManipManager.getManipManager().updateCameraParameters(drawable, viewer.getCameraParameters());
    ManipManager.getManipManager().render(drawable, gl);

    gl.glBindProgramARB(GL.GL_VERTEX_PROGRAM_ARB, programs[program]);
    gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 7, anim, 0.0f, 0.0f, 0.0f);

    if (program==6)
      gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 6, (float) Math.sin(anim)*amp*50.0f, 0.0f, 0.0f, 0.0f);
    else
      gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, 6, freq, amp, d, d+1);

    if (b['p'])
      gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);

    gl.glDisable(GL.GL_TEXTURE_2D);
    gl.glCallList(obj+1);

    gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);

    gl.glPopMatrix();
  }

  // Unused routines
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  public void shutdownDemo() {
    ManipManager.getManipManager().unregisterWindow(drawable);
    drawable.removeGLEventListener(this);
    super.shutdownDemo();
  }

  private void initExtension(GL gl, String glExtensionName) {
    if (!gl.isExtensionAvailable(glExtensionName)) {
      final String message = "OpenGL extension \"" + glExtensionName + "\" not available";
      new Thread(new Runnable() {
          public void run() {
            JOptionPane.showMessageDialog(null, message, "Unavailable extension", JOptionPane.ERROR_MESSAGE);
            shutdownDemo();
          }
        }).start();
      throw new RuntimeException(message);
    }
  }

  private void dispatchKey(int keyCode, char k) {
    if (k < 256)
      b[k] = !b[k];

    switch (keyCode) {
    case KeyEvent.VK_HOME:
    case KeyEvent.VK_R:
      anim = 0.0f;
      amp = 0.05f;
      freq = 8.0f;
      d = 4.0f;
      doViewAll = true;
      break;

    case KeyEvent.VK_LEFT:
    case KeyEvent.VK_KP_LEFT:
      program--;
      if (program < 0)
        program = NUM_PROGS-1;
      setWindowTitle();
      break;

    case KeyEvent.VK_RIGHT:
    case KeyEvent.VK_KP_RIGHT:
      program = (program + 1) % NUM_PROGS;
      setWindowTitle();
      break;

    case KeyEvent.VK_F1:
    case KeyEvent.VK_H:
      String endl = System.getProperty("line.separator");
      endl = endl + endl;
      String msg = ("F1/h - Help" + endl +
                    "Home - Reset" + endl +
                    "Left Button & Mouse - Rotate viewpoint" + endl +
                    "1..5 - Switch object (Sphere, Torus, Triceratop, Cube, Cylinder)" + endl +
                    "- / + - Change amplitude" + endl +
                    "[ / ] - Change frequency" + endl +
                    ", / . - Change square fisheye size" + endl +
                    "Left - Next vertex program" + endl +
                    "Right - Previous vertex program" + endl +
                    "W - Toggle wireframe" + endl +
                    "Space - Toggle animation" + endl +
                    "Esc/q - Exit program" + endl);
      JOptionPane.showMessageDialog(null, msg, "Help", JOptionPane.INFORMATION_MESSAGE);
      break;

    case KeyEvent.VK_ESCAPE:
    case KeyEvent.VK_Q:
      shutdownDemo();
      return;

    case KeyEvent.VK_W:
      toggleWire = true;
      break;

    case KeyEvent.VK_EQUALS:
    case KeyEvent.VK_PLUS:
      amp += 0.01;
      break;

    case KeyEvent.VK_MINUS:
      amp -= 0.01;
      break;

    case KeyEvent.VK_CLOSE_BRACKET:
      freq += 0.5;
      break;

    case KeyEvent.VK_OPEN_BRACKET:
      freq -= 0.5;
      break;

    case KeyEvent.VK_PERIOD:
      d += 0.1;
      break;

    case KeyEvent.VK_COMMA:
      d -= 0.1;
      break;

    case KeyEvent.VK_SPACE:
      // Could also start/stop Animator here
      animating = !animating;
      break;

    case KeyEvent.VK_1:
      obj = 0;
      break;

    case KeyEvent.VK_2:
      obj = 1;
      break;

    case KeyEvent.VK_3:
      obj = 2;
      break;

    case KeyEvent.VK_4:
      obj = 3;
      break;

    case KeyEvent.VK_5:
      obj = 4;
      break;
    }
  }

  private void setWindowTitle() {
    titleSetter.setTitle("SpaceWarp - " + programNames[program]);
  }

  private void drawObject(GL gl, int which) {
    switch(which) {
    case 0:
      drawSphere(gl, 0.5f, 100, 100);
      break;

    case 1:
      drawTorus(gl, 0.25f, 0.5f, 100, 100);
      break;

    case 2:
      try {
        Triceratops.drawObject(gl);
      } catch (IOException e) {
        shutdownDemo();
        throw new RuntimeException(e);
      }
      break;

    case 3:
      drawCube(gl);
      break;

    case 4:
      drawCylinder(gl);
      break;
    }
  }

  private void drawSphere(GL gl, float radius, int slices, int stacks) {
    int J = stacks;
    int I = slices;
    for(int j = 0; j < J; j++) {
      float v = j/(float) J;
      float phi = (float) (v * 2 * Math.PI);
      float v2 = (j+1)/(float) J;
      float phi2 = (float) (v2 * 2 * Math.PI);

      gl.glBegin(GL.GL_QUAD_STRIP);
      for(int i = 0; i < I; i++) {	
        float u = i/(I-1.0f);
        float theta = (float) (u * Math.PI);
        float x,y,z,nx,ny,nz;

        nx = (float) (Math.cos(theta)*Math.cos(phi));
        ny = (float) (Math.sin(theta)*Math.cos(phi));
        nz = (float) (Math.sin(phi));
        x = radius * nx;
        y = radius * ny;
        z = radius * nz;

        gl.glColor3f ( u,  v, 0.0f);
        gl.glNormal3f(nx, ny, nz);
        gl.glVertex3f( x,  y, z);

        nx = (float) (Math.cos(theta)*Math.cos(phi2));
        ny = (float) (Math.sin(theta)*Math.cos(phi2));
        nz = (float) (Math.sin(phi2));
        x = radius * nx;
        y = radius * ny;
        z = radius * nz;

        gl.glColor3f ( u,  v+(1.0f/(J-1.0f)), 0.0f);
        gl.glNormal3f(nx,                 ny,   nz);
        gl.glVertex3f( x,                  y,    z);
      }
      gl.glEnd();
    }
  }

  private void drawTorus(GL gl, float meridian_radius, float core_radius, 
                         int meridian_slices, int core_slices) {
    int J = meridian_slices;
    int I = core_slices;
    for(int j = 0; j < J-1; j++) {
      float v = j/(J-1.0f);
      float rho = (float) (v * 2.0f * Math.PI);
      float v2 = (j+1)/(J-1.0f);
      float rho2 = (float) (v2 * 2.0f * Math.PI);
      gl.glBegin(GL.GL_QUAD_STRIP);
      for(int i = 0; i < I; i++) {	
        float u = i/(I-1.0f);
        float theta = (float) (u * 2.0f * Math.PI);
        float x,y,z,nx,ny,nz;

        x  = (float) (core_radius*Math.cos(theta) + meridian_radius*Math.cos(theta)*Math.cos(rho));
        y  = (float) (core_radius*Math.sin(theta) + meridian_radius*Math.sin(theta)*Math.cos(rho));
        z  = (float) (meridian_radius*Math.sin(rho));
        nx = (float) (Math.cos(theta)*Math.cos(rho));
        ny = (float) (Math.sin(theta)*Math.cos(rho));
        nz = (float) (Math.sin(rho));	

        gl.glColor3f ( u,  v, 0.0f);
        gl.glNormal3f(nx, ny, nz);
        gl.glVertex3f( x,  y,  z);

        x  = (float) (core_radius*Math.cos(theta) + meridian_radius*Math.cos(theta)*Math.cos(rho2));
        y  = (float) (core_radius*Math.sin(theta) + meridian_radius*Math.sin(theta)*Math.cos(rho2));
        z  = (float) (meridian_radius*Math.sin(rho2));
        nx = (float) (Math.cos(theta)*Math.cos(rho2));
        ny = (float) (Math.sin(theta)*Math.cos(rho2));
        nz = (float) (Math.sin(rho2));	

        gl.glColor3f ( u,  v, 0.0f);
        gl.glNormal3f(nx, ny, nz);
        gl.glVertex3f( x,  y,  z);
      }
      gl.glEnd();
    }
  }

  private void drawCube(GL gl) {
    int cr = 40;
    float scaleFactor = 0.5f;

    // back
    gl.glColor3f(1.0f, 0.0f, 0.0f);
    gl.glNormal3f(0.0f, 0.0f, -1.0f);
    drawGrid(gl, cr, cr, scaleFactor, -1.0f, -1.0f, -1.0f, 2.0f, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f);

    // front
    gl.glColor3f(1.0f, 0.0f, 0.0f);
    gl.glNormal3f(0.0f, 0.0f, 1.0f);
    drawGrid(gl, cr, cr, scaleFactor, -1.0f, -1.0f, 1.0f, 2.0f, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f);

    // left
    gl.glColor3f(0.0f, 1.0f, 0.0f);
    gl.glNormal3f(-1.0f, 0.0f, 0.0f);
    drawGrid(gl, cr, cr, scaleFactor, -1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f, 0.0f);

    // right
    gl.glColor3f(0.0f, 0.0f, 1.0f);
    gl.glNormal3f(1.0f, 0.0f, 0.0f);
    drawGrid(gl, cr, cr, scaleFactor, 1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f, 0.0f);

    // bottom
    gl.glColor3f(1.0f, 1.0f, 0.0f);
    gl.glNormal3f(0.0f,-1.0f, 0.0f);
    drawGrid(gl, cr, cr, scaleFactor, -1.0f, -1.0f, -1.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 2.0f);

    // top
    gl.glColor3f(0.0f, 1.0f, 1.0f);
    gl.glNormal3f(0.0f, 1.0f, 0.0f);
    drawGrid(gl, cr, cr, scaleFactor, -1.0f, 1.0f, -1.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 2.0f);
  }

  private void drawGrid(GL gl, int rows, int cols,
                        float scaleFactor,
                        float sx, float sy, float sz,
                        float ux, float uy, float uz,
                        float vx, float vy, float vz) {
    int x, y;

    for(y=0; y<rows; y++) {
      gl.glBegin(GL.GL_QUAD_STRIP);
      for(x=0; x<=cols; x++) {
        float u = x / (float) cols;
        float v = y / (float) rows;
        float v2 = v + (1.0f / (float) rows);

        gl.glTexCoord2f(u, v);
        gl.glVertex3f(scaleFactor * (sx + (u*ux) + (v*vx)),
                      scaleFactor * (sy + (u*uy) + (v*vy)),
                      scaleFactor * (sz + (u*uz) + (v*vz)));

        gl.glTexCoord2f(u, v2);
        gl.glVertex3f(scaleFactor * (sx + (u*ux) + (v2*vx)),
                      scaleFactor * (sy + (u*uy) + (v2*vy)),
                      scaleFactor * (sz + (u*uz) + (v2*vz)));
      }
      gl.glEnd();
    }
  }

  private void drawCylinder(GL gl) {
    GLUquadric quad;

    quad = glu.gluNewQuadric();
    glu.gluQuadricDrawStyle  (quad, GLU.GLU_FILL);
    glu.gluQuadricOrientation(quad, GLU.GLU_OUTSIDE);
    glu.gluQuadricNormals    (quad, GLU.GLU_SMOOTH);
    glu.gluQuadricTexture    (quad, true);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glPushMatrix();
    gl.glTranslatef(-1.0f, 0.0f, 0.0f);
    gl.glRotatef   (90.0f, 0.0f, 1.0f, 0.0f);

    glu.gluCylinder(quad, 0.25f, 0.25f, 2.0f, 60, 30);
    gl.glPopMatrix();

    glu.gluDeleteQuadric(quad);
  }

  private static final String[] programNames = new String[] {
    "Normal",
    "Pulsate",
    "Wave",
    "Square fisheye",
    "Spherical fisheye",
    "Ripple",
    "Twist"
  };

  private static final String programSetup =
    "PARAM mvp [4]          = { state.matrix.mvp };                # modelview projection matrix\n" +
    "PARAM mvit[4]          = { state.matrix.modelview.invtrans }; # modelview matrix inverse transpose\n" +
    "PARAM mv  [4]          = { state.matrix.modelview };          # modelview matrix\n" +
    "PARAM proj[4]          = { state.matrix.projection };         # projection matrix\n" +
    "PARAM lightPos         = program.env[0];                      # light position/direction\n" +
    "PARAM diffuseCol       = program.env[1];                      # diffuse color\n" +
    "PARAM specularCol      = program.env[2];                      # specular color\n" +
    "PARAM smoothstep       = program.env[3];                      # smoothstep constants\n" +
    "PARAM sinTaylorConst1  = program.env[4];                      # sin Taylor series constants 1 of 2\n" +
    "PARAM sinTaylorConst2  = program.env[5];                      # sin Taylor series constants 2 of 2\n" +
    "PARAM sinFreqAmplitude = program.env[6];                      # sin wave frequency, amplitude\n" +
    "PARAM phaseAnim        = program.env[7];                      # phase animation\n" +
    "PARAM fisheyeRadius    = program.env[8];                      # fisheye sphere radius\n" +
    "\n" +
    "# Per vertex inputs\n" +
    "ATTRIB iPos            = vertex.position;                     # position\n" +
    "ATTRIB iTex            = vertex.texcoord;                     # tex coord\n" +
    "ATTRIB iNorm           = vertex.normal;                       # normal\n" +
    "\n" +
    "# Outputs\n" +
    "OUTPUT oPos            = result.position;                     # position\n" +
    "OUTPUT oCol0           = result.color;                        # color\n" +
    "OUTPUT oTex0           = result.texcoord;                     # tex coord\n" +
    "\n" +
    "# Temporaries\n" +
    "TEMP r0;\n" +
    "TEMP r1;\n" +
    "TEMP r2;\n" +
    "TEMP r3;\n" +
    "TEMP r4;\n";

  private static final String[] programTexts = new String[] {
    //
    // Transform with diffuse lighting
    //
    "!!ARBvp1.0\n" +
    "#Simple transform and diffuse lighting\n" +
    programSetup +
    "DP4   oPos.x, mvp[0], iPos ;   # object x MVP -> clip\n" +
    "DP4   oPos.y, mvp[1], iPos ;\n" +
    "DP4   oPos.z, mvp[2], iPos ;\n" +
    "DP4   oPos.w, mvp[3], iPos ;\n" +
    "\n" +
    "DP3   r1.x, mvit[0], iNorm ;        # normal x MV-1T -> lighting normal\n" +
    "DP3   r1.y, mvit[1], iNorm ;\n" +
    "DP3   r1.z, mvit[2], iNorm ;\n" +
    "\n" +
    "DP3   r0, lightPos, r1 ;              # L.N\n" +
    "MUL   oCol0.xyz, r0, diffuseCol ;     # col = L.N * diffuse\n" +
    "MOV   oTex0, iTex;\n" +
    "END\n",

    //
    // Pulsate
    //
    "!!ARBvp1.0\n" +
    "#Displace geometry along normal based on sine function of distance from origin\n" +
    "#(in object space)\n" +
    "#sinFreqAmplitude.x = wave frequency\n" +
    "#sinFreqAmplitude.y = wave amplitude\n" +
    "#sinTaylorConst2    = PI constants\n" +
    "#sinTaylorConst1    = Taylor series constants (see below)\n" +
    "\n" +
    programSetup +
    "MOV   r0, iPos; \n" +
    "\n" +
    "#calculate distance from (0, 0, 0)\n" +
    "DP3   r3.x, r0, r0;\n" +
    "RSQ   r3.x, r3.x;\n" +
    "RCP   r3.x, r3.x;\n" +
    "\n" +
    "MUL   r3.x, r3.x, sinFreqAmplitude.x; # wave frequency\n" +
    "ADD   r3.x, r3.x, phaseAnim.x; # phase animation\n" +
    "\n" +
    "#reduce to period of 2*PI\n" +
    "MUL   r2, r3.x, sinTaylorConst2.x;\n" +
    "EXP   r4, r2.x;            # r4.y = r2.x - floor(r2.x)\n" +
    "MUL   r3.x, r4.y, sinTaylorConst2.y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   r3.x, r3.x, -sinTaylorConst2.z;\n" +
    "\n" +
    "#Sine approximation using Taylor series (accurate between -PI and PI) :\n" +
    "#sin(x)  = x - (x^3)/3! + (x^5)/5! - (x^7)/7! + ...\n" +
    "#sin(x) ~= x*(1 - (x^2)*(1/3! - (x^2)(1/5! - (x^2)/7! )))\n" +
    "#        = x * (a - y*(b - y*(c - y*d)))\n" +
    "#where\n" +
    "#a = 1.0    sinTaylorConst1.x\n" +
    "#b = 1/3!   sinTaylorConst1.y\n" +
    "#c = 1/5!   sinTaylorConst1.z\n" +
    "#d = 1/7!   sinTaylorConst1.w\n" +
    "#y = x^2    r2\n" +
    "\n" +
    "#r1.x = sin(r3.x);\n" +
    "\n" +
    "MUL   r2, r3.x, r3.x;\n" +
    "MAD   r1, -r2, sinTaylorConst1.w, sinTaylorConst1.z;\n" +
    "MAD   r1, r1, -r2, sinTaylorConst1.y;\n" +
    "MAD   r1, r1, -r2, sinTaylorConst1.x;\n" +
    "MUL   r1, r1, r3.x;\n" +
    "\n" +
    "#displace vertex along normal\n" +
    "MUL   r1.x, r1.x, sinFreqAmplitude.y;\n" +
    "MAX   r1.x, r1.x, smoothstep.x;     # r1.x = max(r1.x, 0.0);\n" +
    "MUL   r2.xyz, iNorm, r1.x;\n" +
    "ADD   r0.xyz, r0, r2;\n" +
    "\n" +
    "#simple lighting\n" +
    "DP3   r1.x, mvit[0], iNorm ;    # normal x MV-1T -> lighting normal\n" +
    "DP3   r1.y, mvit[1], iNorm ;\n" +
    "DP3   r1.z, mvit[2], iNorm ;\n" +
    "\n" +
    "DP3   r2, lightPos, r1 ;          # light position DOT normal\n" +
    "MUL   oCol0.xyz, r2, diffuseCol ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "DP4   oPos.x, mvp[0], r0 ;    # object x MVP -> clip\n" +
    "DP4   oPos.y, mvp[1], r0 ;\n" +
    "DP4   oPos.z, mvp[2], r0 ;\n" +
    "DP4   oPos.w, mvp[3], r0 ;\n" +
    "\n" +
    "END\n",

    //
    // Wave
    //
    "!!ARBvp1.0\n" +
    "# Perturb vertices in clip space with sine wave\n" +
    "# x += sin((y*freq)+anim) * amp\n" +
    programSetup +
    "DP4   r0.x, mvp[0], iPos ;\n" +
    "DP4   r0.y, mvp[1], iPos ;\n" +
    "DP4   r0.z, mvp[2], iPos ;\n" +
    "DP4   r0.w, mvp[3], iPos ;\n" +
    "\n" +
    "MUL   r3.x, r0.y, sinFreqAmplitude.x;    # wave frequency\n" +
    "ADD   r3.x, r3.x, phaseAnim.x;    # phase animation\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   r2, r3.x, sinTaylorConst2.x;\n" +
    "EXP   r4, r2.x;               # r4.y = r2.x - floor(r2.x)\n" +
    "MUL   r3.x, r4.y, sinTaylorConst2.y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   r3.x, r3.x, -sinTaylorConst2.z;\n" +
    "\n" +
    "# r1.x = sin(r3.x);\n" +
    "MUL   r2,   r3.x, r3.x;\n" +
    "MAD   r1, -r2, sinTaylorConst1.w, sinTaylorConst1.z;\n" +
    "MAD   r1, r1, -r2, sinTaylorConst1.y;\n" +
    "MAD   r1, r1, -r2, sinTaylorConst1.x;\n" +
    "MUL   r1, r1, r3.x;\n" +
    "\n" +
    "MAD   r0.x, r1.x, sinFreqAmplitude.y, r0.x;\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   r1.x, mvit[0], iNorm ;    # normal x MV-1T -> lighting normal\n" +
    "DP3   r1.y, mvit[1], iNorm ;\n" +
    "DP3   r1.z, mvit[2], iNorm ;\n" +
    "DP3   r2, lightPos, r1 ;          # light position DOT normal\n" +
    "MUL   oCol0.xyz, r2, diffuseCol ; # col = ldotn * diffuse\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "MOV   oPos, r0;\n" +
    "\n" +
    "END\n",

    //
    // Fisheye
    //
    "!!ARBvp1.0\n" +
    "#Fisheye distortion based on function:\n" +
    "#f(x)=(d+1)/(d+(1/x))\n" +
    "#maps the [0,1] interval monotonically onto [0,1]\n" +
    "\n" +
    "#sinFreqAmplitude.z = d\n" +
    "#sinFreqAmplitude.w = d+1\n" +
    programSetup +
    "\n" +
    "DP4   r0.x, mvp[0], iPos ;\n" +
    "DP4   r0.y, mvp[1], iPos ;\n" +
    "DP4   r0.z, mvp[2], iPos ;\n" +
    "DP4   r0.w, mvp[3], iPos ;\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   r1, r0.w;\n" +
    "MUL   r0, r0, r1.w;\n" +
    "\n" +
    "MAX   r1, r0, -r0;            # r1 = abs(r0)\n" +
    "\n" +
    "SLT   r2, r0, smoothstep.x;        # r2 = (r0 < 0.0) ? 1.0 : 0.0\n" +
    "SGE   r3, r0, smoothstep.x;        # r3 = (r0 >= 0.0) ? 1.0 : 0.0\n" +
    "\n" +
    "# distort x\n" +
    "# h(x)=(d+1)/(d+(1/x))\n" +
    "RCP   r1.x, r1.x;             # r1 = 1 / r1\n" +
    "ADD   r1.x, r1.x, sinFreqAmplitude.z;    # r1 += d\n" +
    "RCP   r1.x, r1.x;             # r1 = 1 / r1\n" +
    "MUL   r1.x, r1.x, sinFreqAmplitude.w;    # r1 *= d + 1\n" +
    "\n" +
    "# distort y\n" +
    "RCP   r1.y, r1.y;             # r1 = 1 / r1\n" +
    "ADD   r1.y, r1.y, sinFreqAmplitude.z;    # r1 += d\n" +
    "RCP   r1.y, r1.y;             # r1 = 1 / r1\n" +
    "MUL   r1.y, r1.y, sinFreqAmplitude.w;    # r1 *= d + 1\n" +
    "\n" +
    "# handle negative cases\n" +
    "MUL   r4.xy, r1, r3;          # r4 = r1 * r3\n" +
    "MAD   r1.xy, r1, -r2, r4;     # r1 = r1 * -r2 + r4\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   r2.x, mvit[0], iNorm ;   # normal x MV-1T -> lighting normal\n" +
    "DP3   r2.y, mvit[1], iNorm ;\n" +
    "DP3   r2.z, mvit[2], iNorm ;\n" +
    "DP3   r3, lightPos, r2 ;         # light position DOT normal\n" +
    "MUL   oCol0.xyz, r3, diffuseCol ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "MOV   oPos, r1;\n" +
    "\n" +
    "END\n",

    //
    // Spherize
    //
    "!!ARBvp1.0\n" +
    "# Spherical fish-eye distortion\n" +
    "# in clip space\n" +
    programSetup +
    "DP4   r0.x, mvp[0], iPos;\n" +
    "DP4   r0.y, mvp[1], iPos;\n" +
    "DP4   r0.z, mvp[2], iPos;\n" +
    "DP4   r0.w, mvp[3], iPos;\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   r1.x, r0.w;\n" +
    "MUL   r2, r0, r1.x;\n" +
    "\n" +
    "# calculate distance from centre\n" +
    "MUL   r1.x, r2.x, r2.x;\n" +
    "MAD   r1.x, r2.y, r2.y, r1.x;\n" +
    "RSQ   r1.x, r1.x; # r1.x = 1 / sqrt(x*x+y*y)\n" +
    "\n" +
    "# calculate r3 = normalized direction vector\n" +
    "MUL   r3.xy, r0, r1.x;\n" +
    "\n" +
    "RCP   r1.x, r1.x;             # r1.x = actual distance\n" +
    "MIN   r1.x, r1.x, smoothstep.y;    # r1.x = min(r1.x, 1.0)\n" +
    "\n" +
    "# remap based on: f(x) = sqrt(1-x^2)\n" +
    "ADD   r1.x, smoothstep.y, -r1.x;\n" +
    "MAD   r1.x, -r1.x, r1.x, smoothstep.y;\n" +
    "RSQ   r1.x, r1.x;\n" +
    "RCP   r1.x, r1.x;\n" +
    "\n" +
    "# move vertex to new distance from centre\n" +
    "MUL   r0.xy, r3, r1.x;\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   r2.x, mvit[0], iNorm;    # normal x MV-1T -> lighting normal\n" +
    "DP3   r2.y, mvit[1], iNorm;\n" +
    "DP3   r2.z, mvit[2], iNorm;\n" +
    "DP3   r3, lightPos, r2 ;         # light position DOT normal\n" +
    "MUL   oCol0.xyz, r3, diffuseCol ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "MOV   oPos, r0;\n" +
    "\n" +
    "END\n",
    
    //
    // Ripple
    //
    "!!ARBvp1.0\n" +
    "# Ripple distortion\n" +
    programSetup +
    "DP4   r0.x, mvp[0], iPos;\n" +
    "DP4   r0.y, mvp[1], iPos;\n" +
    "DP4   r0.z, mvp[2], iPos;\n" +
    "DP4   r0.w, mvp[3], iPos;\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   r1.x, r0.w;\n" +
    "MUL   r4, r0, r1.x;\n" +
    "\n" +
    "# calculate distance from centre\n" +
    "MUL   r1.x, r4.x, r4.x;\n" +
    "MAD   r1.x, r4.y, r4.y, r1.x;\n" +
    "RSQ   r1.x, r1.x;\n" +
    "\n" +
    "RCP   r1.x, r1.x;\n" +
    "\n" +
    "MUL   r1.x, r1.x, sinFreqAmplitude.x;    # wave frequency\n" +
    "ADD   r1.x, r1.x, phaseAnim.x;    # phase animation\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   r2, r1.x, sinTaylorConst2.x;      # r2 = r1 / 2.0 * PI\n" +
    "EXP   r4, r2.x;               # r4.y = r2.x - floor(r2.x)\n" +
    "MUL   r1.x, r4.y, sinTaylorConst2.y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   r1.x, r1.x, -sinTaylorConst2.z;\n" +
    "\n" +
    "# r3.x = sin(r1.x)\n" +
    "MUL   r2, r1.x, r1.x;\n" +
    "MAD   r3, -r2, sinTaylorConst1.w, sinTaylorConst1.z;\n" +
    "MAD   r3, r3, -r2, sinTaylorConst1.y;\n" +
    "MAD   r3, r3, -r2, sinTaylorConst1.x;\n" +
    "MUL   r3, r3, r1.x;\n" +
    "\n" +
    "MUL   r3.x, r3.x, sinFreqAmplitude.y;\n" +
    "\n" +
    "# move vertex towards centre based on distance\n" +
    "MAD   r0.xy, r0, -r3.x, r0;\n" +
    "\n" +
    "# lighting\n" +
    "DP3   r2.x, mvit[0], iNorm;     # normal x MV-1T -> lighting normal\n" +
    "DP3   r2.y, mvit[1], iNorm;\n" +
    "DP3   r2.z, mvit[2], iNorm;\n" +
    "DP3   r3, lightPos, r2;           # light position DOT normal\n" +
    "MUL   oCol0.xyz, r3, diffuseCol;  # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "MOV   oPos, r0;\n" +
    "\n" +
    "END\n",

    //
    // Twist
    //
    "!!ARBvp1.0\n" +
    "# Twist\n" +
    programSetup +
    "MOV   r0, iPos;\n" +
    "\n" +
    "MUL   r1.x, r0.x, sinFreqAmplitude.x;        # frequency\n" +
    "\n" +
    "# calculate sin(angle) and cos(angle)\n" +
    "ADD   r1.y, r1.x, -sinTaylorConst2.w;       # r1.y = r1.x + PI/2.0\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   r2, r1, sinTaylorConst2.x;            # r2 = r1 / 2.0 * PI\n" +
    "EXP   r3.y, r2.x;                 # r2.y = r2.x - floor(r2.x)\n" +
    "MOV   r3.x, r3.y;\n" +
    "EXP   r3.y, r2.y;                 # r2.y = r2.x - floor(r2.x)\n" +
    "MAD   r2, r3, sinTaylorConst2.y, -sinTaylorConst2.z;  # r2 = (r3 * 2.0*PI) - M_PI\n" +
    "\n" +
    "# r4.x = sin(r2.x);\n" +
    "# r4.y = cos(r2.y);\n" +
    "# parallel taylor series\n" +
    "MUL   r3,   r2, r2;\n" +
    "MAD   r4, -r3, sinTaylorConst1.w, sinTaylorConst1.z;\n" +
    "MAD   r4, r4, -r3, sinTaylorConst1.y;\n" +
    "MAD   r4, r4, -r3, sinTaylorConst1.x;\n" +
    "MUL   r4, r4, r2;\n" +
    "\n" +
    "# x    y    z    w\n" +
    "# R:\n" +
    "# 1    0    0    0\n" +
    "# 0    c   -s    0\n" +
    "# 0    s    c    0\n" +
    "# 0    0    0    1\n" +
    "\n" +
    "# c = cos(a)\n" +
    "# s = sin(a)\n" +
    "\n" +
    "# calculate rotation around X\n" +
    "MOV   r1, r0;\n" +
    "\n" +
    "MUL   r1.y, r0.y, r4.y;\n" +
    "MAD   r1.y, r0.z, -r4.x, r1.y;    # ny = y*cos(a) - z*sin(a)\n" +
    "\n" +
    "MUL   r1.z, r0.y, r4.x;\n" +
    "MAD   r1.z, r0.z, r4.y, r1.z;     # nz = y*sin(a) + z*cos(a)\n" +
    "\n" +
    "DP4   oPos.x, mvp[0], r1;        # object x MVP -> clip\n" +
    "DP4   oPos.y, mvp[1], r1;\n" +
    "DP4   oPos.z, mvp[2], r1;\n" +
    "DP4   oPos.w, mvp[3], r1;\n" +
    "\n" +
    "# rotate normal\n" +
    "MOV   r2, iNorm;\n" +
    "MUL   r2.y, iNorm.y, r4.y;\n" +
    "MAD   r2.y, iNorm.z, -r4.x, r2.y;   # ny = y*cos(a) - z*sin(a)\n" +
    "\n" +
    "MUL   r2.z, iNorm.y, r4.x;\n" +
    "MAD   r2.z, iNorm.z, r4.y, r2.z;    # nz = y*sin(a) + z*cos(a)\n" +
    "\n" +
    "# diffuse lighting\n" +
    "DP3   r1.x, mvit[0], r2;             # normal x MV-1T -> lighting normal\n" +
    "DP3   r1.y, mvit[1], r2;\n" +
    "DP3   r1.z, mvit[2], r2;\n" +
    "\n" +
    "DP3   r3, lightPos, r1;              # light position DOT normal\n" +
    "MUL   oCol0.xyz, r3, diffuseCol;     # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   oTex0, iTex;\n" +
    "\n" +
    "END\n"
  };

  private static void runExit(final Animator animator) {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Instead run
    // the exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          animator.stop();
          System.exit(0);
        }
      }).start();
  }
}
