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

import net.java.games.jogl.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/**
   Simple space-warp/distortion vertex program demo<br>
   (Press the space bar to switch through programs)<br><p>

   sgreen@nvidia.com 9/2000, based on Cass's vtxprog_silhouette<br><p>

        Ported to Java by Kenneth Russell
*/

public class VertexProgWarp {
  private GLCanvas canvas;
  private Frame    frame;
  private Animator animator;
  private volatile boolean quit;

  private DurationTimer timer = new DurationTimer();
  private boolean  firstRender = true;
  private int      frameCount;

  public static void main(String[] args) {
    new VertexProgWarp().run(args);
  }

  public void run(String[] args) {
    canvas = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities());
    canvas.addGLEventListener(new Listener());

    animator = new Animator(canvas);

    frame = new Frame();
    frame.setLayout(new BorderLayout());
    canvas.setSize(512, 512);
    frame.add(canvas, BorderLayout.CENTER);
    frame.pack();
    frame.show();
    canvas.requestFocus();

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          animator.stop();
          System.exit(0);
        }
      });

    animator.start();
  }

  class Listener implements GLEventListener {
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

    private ExaminerViewer viewer;

    public void init(GLDrawable drawable) {
      GL gl = drawable.getGL();
      GLU glu = drawable.getGLU();

      float cc = 0.0f;
      gl.glClearColor(cc, cc, cc, 1);

      gl.glColor3f(1,1,1);
      gl.glEnable(GL.GL_DEPTH_TEST);
      gl.glDisable(GL.GL_CULL_FACE);

      try {
        initExtension(gl, "GL_NV_vertex_program");
      } catch (RuntimeException e) {
        runExit();
        throw(e);
      }

      for(int i=0; i<NUM_OBJS; i++) {
        gl.glNewList(i+1, GL.GL_COMPILE);
        drawObject(gl, glu, i);
        gl.glEndList();
      }    

      for(int i=0; i<NUM_PROGS; i++) {
        int[] vtxProgTmp = new int[1];
        gl.glGenProgramsNV(1, vtxProgTmp);
        programs[i] = vtxProgTmp[0];
        gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, programs[i]);
        gl.glLoadProgramNV(GL.GL_VERTEX_PROGRAM_NV, programs[i], programTexts[i].length(), programTexts[i]);
      }

      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 0, GL.GL_MODELVIEW_PROJECTION_NV, GL.GL_IDENTITY_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 4,               GL.GL_MODELVIEW, GL.GL_INVERSE_TRANSPOSE_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 8,               GL.GL_MODELVIEW, GL.GL_IDENTITY_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 12,             GL.GL_PROJECTION, GL.GL_IDENTITY_NV);

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 32, 0.0f, 0.0f, 1.0f, 0.0f);   // light position/direction
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 35, 0.0f, 1.0f, 0.0f, 0.0f);   // diffuse color
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 36, 1.0f, 1.0f, 1.0f, 0.0f);   // specular color

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 64, 0.0f, 1.0f, 2.0f, 3.0f);   // smoothstep constants

      // sin Taylor series constants - 1, 1/3!, 1/5!, 1/7!
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 63, 1.0f, 1.0f / (3*2), 1.0f / (5*4*3*2), 1.0f / (7*6*5*4*3*2));

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 62, 1.0f / (2.0f * SIN_PERIOD), 2.0f * SIN_PERIOD, SIN_PERIOD, SIN_PERIOD/2.0f);

      // sin wave frequency, amplitude
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 61, 1.0f, 0.2f, 0.0f, 0.0f);

      // phase animation
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 60, 0.0f, 0.0f, 0.0f, 0.0f);

      // fisheye sphere radius
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 59, 1.0f, 0.0f, 0.0f, 0.0f);

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 58, 0.0f, 0.0f, -2.0f / (zFar - zNear), -(zFar+zNear)/(zFar-zNear) );
      setWindowTitle();

      b['p'] = true;
      
      drawable.addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            dispatchKey(e.getKeyCode(), e.getKeyChar());
          }
        });

      // Register the window with the ManipManager
      ManipManager manager = ManipManager.getManipManager();
      manager.registerWindow(drawable);

      viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
      viewer.setNoAltKeyMode(true);
      viewer.attach(drawable, new BSphereProvider() {
	  public BSphere getBoundingSphere() {
	    return new BSphere(new Vec3f(0, 0, 0), 1.0f);
	  }
	});
      viewer.setVertFOV((float) Math.toRadians(60));
      viewer.setZNear(zNear);
      viewer.setZFar(zFar);
    }

    public void display(GLDrawable drawable) {
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
      GLU glu = drawable.getGLU();

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

      gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, programs[program]);
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 60, anim, 0.0f, 0.0f, 0.0f);

      if (program==6)
        gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 61, (float) Math.sin(anim)*amp*50.0f, 0.0f, 0.0f, 0.0f);
      else
        gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 61, freq, amp, d, d+1);

      if (b['p'])
        gl.glEnable(GL.GL_VERTEX_PROGRAM_NV);

      gl.glDisable(GL.GL_TEXTURE_2D);
      gl.glCallList(obj+1);

      gl.glDisable(GL.GL_VERTEX_PROGRAM_NV);

      gl.glPopMatrix();
    }

    // Unused routines
    public void reshape(GLDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    //----------------------------------------------------------------------
    // Internals only below this point
    //
    private void initExtension(GL gl, String glExtensionName) {
      if (!gl.isExtensionAvailable(glExtensionName)) {
        throw new RuntimeException("OpenGL extension \"" + glExtensionName + "\" not available");
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
          runExit();
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
      frame.setTitle("SpaceWarp - " + programNames[program]);
    }

    private void drawObject(GL gl, GLU glu, int which) {
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
            runExit();
            throw new RuntimeException(e);
          }
          break;

	case 3:
          drawCube(gl);
          break;

	case 4:
          drawCylinder(gl, glu);
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

    private void drawCylinder(GL gl, GLU glu) {
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

  private static final String[] programTexts = new String[] {
    //
    // Transform with diffuse lighting
    //
    "!!VP1.0\n" +
    "#Simple transform and diffuse lighting\n" +
    "\n" +
    "DP4   o[HPOS].x, c[0], v[OPOS] ;   # object x MVP -> clip\n" +
    "DP4   o[HPOS].y, c[1], v[OPOS] ;\n" +
    "DP4   o[HPOS].z, c[2], v[OPOS] ;\n" +
    "DP4   o[HPOS].w, c[3], v[OPOS] ;\n" +
    "\n" +
    "DP3   R1.x, c[4], v[NRML] ;        # normal x MV-1T -> lighting normal\n" +
    "DP3   R1.y, c[5], v[NRML] ;\n" +
    "DP3   R1.z, c[6], v[NRML] ;\n" +
    "\n" +
    "DP3   R0, c[32], R1 ;              # L.N\n" +
    "MUL   o[COL0].xyz, R0, c[35] ;     # col = L.N * diffuse\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "END",

    //
    // Pulsate
    //
    "!!VP1.0\n" +
    "#Displace geometry along normal based on sine function of distance from origin\n" +
    "#(in object space)\n" +
    "#c[61].x = wave frequency\n" +
    "#c[61].y = wave amplitude\n" +
    "#c[62]   = PI constants\n" +
    "#c[63]   = Taylor series constants (see below)\n" +
    "\n" +
    "MOV   R0, v[OPOS]; \n" +
    "\n" +
    "#calculate distance from (0, 0, 0)\n" +
    "DP3   R3.x, R0, R0;\n" +
    "RSQ   R3.x, R3.x;\n" +
    "RCP   R3.x, R3.x;\n" +
    "\n" +
    "MUL   R3.x, R3.x, c[61].x; # wave frequency\n" +
    "ADD   R3.x, R3.x, c[60].x; # phase animation\n" +
    "\n" +
    "#reduce to period of 2*PI\n" +
    "MUL   R2, R3.x, c[62].x;\n" +
    "EXP   R4, R2.x;            # R4.y = R2.x - floor(R2.x)\n" +
    "MUL   R3.x, R4.y, c[62].y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   R3.x, R3.x, -c[62].z;\n" +
    "\n" +
    "#Sine approximation using Taylor series (accurate between -PI and PI) :\n" +
    "#sin(x)  = x - (x^3)/3! + (x^5)/5! - (x^7)/7! + ...\n" +
    "#sin(x) ~= x*(1 - (x^2)*(1/3! - (x^2)(1/5! - (x^2)/7! )))\n" +
    "#        = x * (a - y*(b - y*(c - y*d)))\n" +
    "#where\n" +
    "#a = 1.0    c[63].x\n" +
    "#b = 1/3!   c[63].y\n" +
    "#c = 1/5!   c[63].z\n" +
    "#d = 1/7!   c[63].w\n" +
    "#y = x^2    R2\n" +
    "\n" +
    "#R1.x = sin(R3.x);\n" +
    "\n" +
    "MUL   R2, R3.x, R3.x;\n" +
    "MAD   R1, -R2, c[63].w, c[63].z;\n" +
    "MAD   R1, R1, -R2, c[63].y;\n" +
    "MAD   R1, R1, -R2, c[63].x;\n" +
    "MUL   R1, R1, R3.x;\n" +
    "\n" +
    "#displace vertex along normal\n" +
    "MUL   R1.x, R1.x, c[61].y;\n" +
    "MAX   R1.x, R1.x, c[64].x;     # r1.x = max(r1.x, 0.0);\n" +
    "MUL   R2.xyz, v[NRML], R1.x;\n" +
    "ADD   R0.xyz, R0, R2;\n" +
    "\n" +
    "#simple lighting\n" +
    "DP3   R1.x, c[4], v[NRML] ;    # normal x MV-1T -> lighting normal\n" +
    "DP3   R1.y, c[5], v[NRML] ;\n" +
    "DP3   R1.z, c[6], v[NRML] ;\n" +
    "\n" +
    "DP3   R2, c[32], R1 ;          # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R2, c[35] ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "DP4   o[HPOS].x, c[0], R0 ;    # object x MVP -> clip\n" +
    "DP4   o[HPOS].y, c[1], R0 ;\n" +
    "DP4   o[HPOS].z, c[2], R0 ;\n" +
    "DP4   o[HPOS].w, c[3], R0 ;\n" +
    "\n" +
    "END",

    //
    // Wave
    //
    "!!VP1.0\n" +
    "# Perturb vertices in clip space with sine wave\n" +
    "# x += sin((y*freq)+anim) * amp\n" +
    "DP4   R0.x, c[0], v[OPOS] ;\n" +
    "DP4   R0.y, c[1], v[OPOS] ;\n" +
    "DP4   R0.z, c[2], v[OPOS] ;\n" +
    "DP4   R0.w, c[3], v[OPOS] ;\n" +
    "\n" +
    "MUL   R3.x, R0.y, c[61].x;    # wave frequency\n" +
    "ADD   R3.x, R3.x, c[60].x;    # phase animation\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   R2, R3.x, c[62].x;\n" +
    "EXP   R4, R2.x;               # R4.y = R2.x - floor(R2.x)\n" +
    "MUL   R3.x, R4.y, c[62].y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   R3.x, R3.x, -c[62].z;\n" +
    "\n" +
    "# R1.x = sin(R3.x);\n" +
    "MUL   R2,   R3.x, R3.x;\n" +
    "MAD   R1, -R2, c[63].w, c[63].z;\n" +
    "MAD   R1, R1, -R2, c[63].y;\n" +
    "MAD   R1, R1, -R2, c[63].x;\n" +
    "MUL   R1, R1, R3.x;\n" +
    "\n" +
    "MAD   R0.x, R1.x, c[61].y, R0.x;\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   R1.x, c[4], v[NRML] ;    # normal x MV-1T -> lighting normal\n" +
    "DP3   R1.y, c[5], v[NRML] ;\n" +
    "DP3   R1.z, c[6], v[NRML] ;\n" +
    "DP3   R2, c[32], R1 ;          # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R2, c[35] ; # col = ldotn * diffuse\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "MOV   o[HPOS], R0;\n" +
    "\n" +
    "END",

    //
    // Fisheye
    //
    "!!VP1.0\n" +
    "#Fisheye distortion based on function:\n" +
    "#f(x)=(d+1)/(d+(1/x))\n" +
    "#maps the [0,1] interval monotonically onto [0,1]\n" +
    "\n" +
    "#c[61].z = d\n" +
    "#c[61].w = d+1\n" +
    "\n" +
    "DP4   R0.x, c[0], v[OPOS] ;\n" +
    "DP4   R0.y, c[1], v[OPOS] ;\n" +
    "DP4   R0.z, c[2], v[OPOS] ;\n" +
    "DP4   R0.w, c[3], v[OPOS] ;\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   R1, R0.w;\n" +
    "MUL   R0, R0, R1.w;\n" +
    "\n" +
    "MAX   R1, R0, -R0;            # r1 = abs(r0)\n" +
    "\n" +
    "SLT   R2, R0, c[64].x;        # r2 = (r0 < 0.0) ? 1.0 : 0.0\n" +
    "SGE   R3, R0, c[64].x;        # r3 = (r0 >= 0.0) ? 1.0 : 0.0\n" +
    "\n" +
    "# distort x\n" +
    "# h(x)=(d+1)/(d+(1/x))\n" +
    "RCP   R1.x, R1.x;             # r1 = 1 / r1\n" +
    "ADD   R1.x, R1.x, c[61].z;    # r1 += d\n" +
    "RCP   R1.x, R1.x;             # r1 = 1 / r1\n" +
    "MUL   R1.x, R1.x, c[61].w;    # r1 *= d + 1\n" +
    "\n" +
    "# distort y\n" +
    "RCP   R1.y, R1.y;             # r1 = 1 / r1\n" +
    "ADD   R1.y, R1.y, c[61].z;    # r1 += d\n" +
    "RCP   R1.y, R1.y;             # r1 = 1 / r1\n" +
    "MUL   R1.y, R1.y, c[61].w;    # r1 *= d + 1\n" +
    "\n" +
    "# handle negative cases\n" +
    "MUL   R4.xy, R1, R3;          # r4 = r1 * r3\n" +
    "MAD   R1.xy, R1, -R2, R4;     # r1 = r1 * -r2 + r4\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   R2.x, c[4], v[NRML] ;   # normal x MV-1T -> lighting normal\n" +
    "DP3   R2.y, c[5], v[NRML] ;\n" +
    "DP3   R2.z, c[6], v[NRML] ;\n" +
    "DP3   R3, c[32], R2 ;         # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R3, c[35] ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "MOV   o[HPOS], R1;\n" +
    "\n" +
    "END",

    //
    // Spherize
    //
    "!!VP1.0\n" +
    "# Spherical fish-eye distortion\n" +
    "# in clip space\n" +
    "DP4   R0.x, c[0], v[OPOS];\n" +
    "DP4   R0.y, c[1], v[OPOS];\n" +
    "DP4   R0.z, c[2], v[OPOS];\n" +
    "DP4   R0.w, c[3], v[OPOS];\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   R1.x, R0.w;\n" +
    "MUL   R2, R0, R1.x;\n" +
    "\n" +
    "# calculate distance from centre\n" +
    "MUL   R1.x, R2.x, R2.x;\n" +
    "MAD   R1.x, R2.y, R2.y, R1.x;\n" +
    "RSQ   R1.x, R1.x; # r1.x = 1 / sqrt(x*x+y*y)\n" +
    "\n" +
    "# calculate r3 = normalized direction vector\n" +
    "MUL   R3.xy, R0, R1.x;\n" +
    "\n" +
    "RCP   R1.x, R1.x;             # r1.x = actual distance\n" +
    "MIN   R1.x, R1.x, c[64].y;    # r1.x = min(r1.x, 1.0)\n" +
    "\n" +
    "# remap based on: f(x) = sqrt(1-x^2)\n" +
    "ADD   R1.x, c[64].y, -R1.x;\n" +
    "MAD   R1.x, -R1.x, R1.x, c[64].y;\n" +
    "RSQ   R1.x, R1.x;\n" +
    "RCP   R1.x, R1.x;\n" +
    "\n" +
    "# move vertex to new distance from centre\n" +
    "MUL   R0.xy, R3, R1.x;\n" +
    "\n" +
    "# simple lighting\n" +
    "DP3   R2.x, c[4], v[NRML];    # normal x MV-1T -> lighting normal\n" +
    "DP3   R2.y, c[5], v[NRML];\n" +
    "DP3   R2.z, c[6], v[NRML];\n" +
    "DP3   R3, c[32], R2 ;         # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R3, c[35] ; # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "MOV   o[HPOS], R0;\n" +
    "\n" +
    "END",

    //
    // Ripple
    //
    "!!VP1.0\n" +
    "# Ripple distortion\n" +
    "DP4   R0.x, c[0], v[OPOS];\n" +
    "DP4   R0.y, c[1], v[OPOS];\n" +
    "DP4   R0.z, c[2], v[OPOS];\n" +
    "DP4   R0.w, c[3], v[OPOS];\n" +
    "\n" +
    "# do perspective divide\n" +
    "RCP   R1.x, R0.w;\n" +
    "MUL   R4, R0, R1.x;\n" +
    "\n" +
    "# calculate distance from centre\n" +
    "MUL   R1.x, R4.x, R4.x;\n" +
    "MAD   R1.x, R4.y, R4.y, R1.x;\n" +
    "RSQ   R1.x, R1.x; " +
    "\n" +
    "RCP   R1.x, R1.x; " +
    "\n" +
    "MUL   R1.x, R1.x, c[61].x;    # wave frequency\n" +
    "ADD   R1.x, R1.x, c[60].x;    # phase animation\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   R2, R1.x, c[62].x;      # R2 = R1 / 2.0 * PI\n" +
    "EXP   R4, R2.x;               # R4.y = R2.x - floor(R2.x)\n" +
    "MUL   R1.x, R4.y, c[62].y;\n" +
    "\n" +
    "# offset to -PI - PI\n" +
    "ADD   R1.x, R1.x, -c[62].z;\n" +
    "\n" +
    "# R3.x = sin(R1.x)\n" +
    "MUL   R2, R1.x, R1.x;\n" +
    "MAD   R3, -R2, c[63].w, c[63].z;\n" +
    "MAD   R3, R3, -R2, c[63].y;\n" +
    "MAD   R3, R3, -R2, c[63].x;\n" +
    "MUL   R3, R3, R1.x;\n" +
    "\n" +
    "MUL   R3.x, R3.x, c[61].y;\n" +
    "\n" +
    "# move vertex towards centre based on distance\n" +
    "MAD   R0.xy, R0, -R3.x, R0;\n" +
    "\n" +
    "# lighting\n" +
    "DP3   R2.x, c[4], v[NRML];     # normal x MV-1T -> lighting normal\n" +
    "DP3   R2.y, c[5], v[NRML];\n" +
    "DP3   R2.z, c[6], v[NRML];\n" +
    "DP3   R3, c[32], R2;           # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R3, c[35];  # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "MOV   o[HPOS], R0;\n" +
    "\n" +
    "END",

    //
    // Twist
    //
    "!!VP1.0 # Twist\n" +
    "MOV   R0, v[OPOS];\n" +
    "\n" +
    "MUL   R1.x, R0.x, c[61].x;        # frequency\n" +
    "\n" +
    "# calculate sin(angle) and cos(angle)\n" +
    "ADD   R1.y, R1.x, -c[62].w;       # R1.y = R1.x + PI/2.0\n" +
    "\n" +
    "# reduce to period of 2*PI\n" +
    "MUL   R2, R1, c[62].x;            # R2 = R1 / 2.0 * PI\n" +
    "EXP   R3.y, R2.x;                 # R2.y = R2.x - floor(R2.x)\n" +
    "MOV   R3.x, R3.y;\n" +
    "EXP   R3.y, R2.y;                 # R2.y = R2.x - floor(R2.x)\n" +
    "MAD   R2, R3, c[62].y, -c[62].z;  # R2 = (R3 * 2.0*PI) - M_PI\n" +
    "\n" +
    "# R4.x = sin(R2.x);\n" +
    "# R4.y = cos(R2.y);\n" +
    "# parallel taylor series\n" +
    "MUL   R3,   R2, R2;\n" +
    "MAD   R4, -R3, c[63].w, c[63].z;\n" +
    "MAD   R4, R4, -R3, c[63].y;\n" +
    "MAD   R4, R4, -R3, c[63].x;\n" +
    "MUL   R4, R4, R2;\n" +
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
    "MOV   R1, R0;\n" +
    "\n" +
    "MUL   R1.y, R0.y, R4.y;\n" +
    "MAD   R1.y, R0.z, -R4.x, R1.y;    # ny = y*cos(a) - z*sin(a)\n" +
    "\n" +
    "MUL   R1.z, R0.y, R4.x;\n" +
    "MAD   R1.z, R0.z, R4.y, R1.z;     # nz = y*sin(a) + z*cos(a)\n" +
    "\n" +
    "DP4   o[HPOS].x, c[0], R1;        # object x MVP -> clip\n" +
    "DP4   o[HPOS].y, c[1], R1;\n" +
    "DP4   o[HPOS].z, c[2], R1;\n" +
    "DP4   o[HPOS].w, c[3], R1;\n" +
    "\n" +
    "# rotate normal\n" +
    "MOV   R2, v[NRML];\n" +
    "MUL   R2.y, v[NRML].y, R4.y;\n" +
    "MAD   R2.y, v[NRML].z, -R4.x, R2.y;   # ny = y*cos(a) - z*sin(a)\n" +
    "\n" +
    "MUL   R2.z, v[NRML].y, R4.x;\n" +
    "MAD   R2.z, v[NRML].z, R4.y, R2.z;    # nz = y*sin(a) + z*cos(a)\n" +
    "\n" +
    "# diffuse lighting\n" +
    "DP3   R1.x, c[4], R2;             # normal x MV-1T -> lighting normal\n" +
    "DP3   R1.y, c[5], R2;\n" +
    "DP3   R1.z, c[6], R2;\n" +
    "\n" +
    "DP3   R3, c[32], R1;              # light position DOT normal\n" +
    "MUL   o[COL0].xyz, R3, c[35];     # col = ldotn * diffuse\n" +
    "\n" +
    "MOV   o[TEX0], v[TEX0];\n" +
    "\n" +
    "END"
  };

  private void runExit() {
    quit = true;
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
