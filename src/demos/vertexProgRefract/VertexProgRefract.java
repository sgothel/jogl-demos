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

package demos.vertexProgRefract;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

import net.java.games.jogl.*;
import net.java.games.jogl.util.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/**
  Wavelength-dependent refraction demo<br>
  It's a chromatic aberration!<br>
  sgreen@nvidia.com 4/2001<br><p>

  Currently 3 passes - could do it in 1 with 4 texture units<p>

  Cubemap courtesy of Paul Debevec<p>

  Ported to Java by Kenneth Russell
*/

public class VertexProgRefract {
  private GLCanvas canvas;
  private Animator animator;
  private volatile boolean quit;

  public static void main(String[] args) {
    new VertexProgRefract().run(args);
  }

  public void run(String[] args) {
    canvas = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities());
    canvas.addGLEventListener(new Listener());

    animator = new Animator(canvas);

    Frame frame = new Frame("Refraction Using Vertex Programs");
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
    private int vtxProg;
    private int cubemap;
    private int bunnydl;
    private int obj;

    private GLUT glut = new GLUT();

    private ExaminerViewer viewer;
    private boolean doViewAll = true;

    private Time  time = new SystemTime();
    private float animRate = (float) Math.toRadians(-6.0f); // Radians / sec

    private float refract = 1.1f;           // ratio of indicies of refraction
    private float wavelengthDelta = 0.05f;  // difference in refraction for each "wavelength" (R,G,B)
    private float fresnel = 2.0f;           // Fresnel multiplier

    private boolean wire = false;
    private boolean toggleWire = false;

    private static final String transformRefract = 
      "!!VP1.0 # Refraction\n" +
      "# transform vertex position to eye space\n" +
      "DP4    R9.x, c[8], v[OPOS] ;\n" +
      "DP4    R9.y, c[9], v[OPOS] ;\n" +
      "DP4    R9.z, c[10], v[OPOS] ;\n" +
      "DP4    R9.w, c[11], v[OPOS] ;\n" +
      "\n" +
      "# transform normal to eye space\n" +
      "DP3    R11.x, c[4], v[NRML] ;\n" +
      "DP3    R11.y, c[5], v[NRML] ;\n" +
      "DP3    R11.z, c[6], v[NRML] ;\n" +
      "\n" +
      "# vertex->eye vector\n" +
      "ADD    R0, -R9, c[59];     # c[59] = eye position in eye space, usually (0,0,0,1)\n" +
      "\n" +
      "# normalize\n" +
      "DP3    R8.w, R0, R0;\n" +
      "RSQ    R8.w, R8.w;\n" +
      "MUL    R8, R0, R8.w;       # r8 = eye/incident vector\n" +
      "\n" +
      "# refraction, Renderman style\n" +
      "\n" +
      "# float IdotN = I.N;\n" +
      "# float k = 1 - eta*eta*(1 - IdotN*IdotN);\n" +
      "# return k < 0 ? (0,0,0) : eta*I - (eta*IdotN + sqrt(k))*N;\n" +
      "\n" +
      "DP3    R0.x, R11, -R8;             # r0 = N.I\n" +
      "\n" +
      "MAD    R1.x, -R0.x, R0.x, c[64].y; # r1 = -IdotN*IdotN + 1\n" +
      "MUL    R1.x, R1.x, c[58].y;        # r1 = -(r1*eta*eta)+1\n" +
      "ADD    R1.x, c[64].y, -R1.x;\n" +
      "\n" +
      "RSQ    R2.x, R1.x;\n" +
      "RCP    R2.x, R2.x;\n" +
      "MAD    R2.x, c[58].x, R0.x, R2.x;\n" +
      "MUL    R2, R11, R2.x;\n" +
      "MAD    R2, c[58].x, -R8, R2;\n" +
      "\n" +
      "# transform refracted ray by cubemap transform\n" +
      "DP3    o[TEX0].x, c[12], R2;\n" +
      "DP3    o[TEX0].y, c[13], R2;\n" +
      "DP3    o[TEX0].z, c[14], R2;\n" +
      "\n" +
      "# calculate reflection\n" +
      "\n" +
      "MUL    R0, R11, c[64].z;\n" +
      "DP3    R3.w, R11, R8;\n" +
      "MAD    R3, R3.w, R0, -R8;\n" +
      "\n" +
      "# transform reflected ray by cubemap transform\n" +
      "DP3    o[TEX1].x, c[12], R3;\n" +
      "DP3    o[TEX1].y, c[13], R3;\n" +
      "DP3    o[TEX1].z, c[14], R3;\n" +
      "\n" +
      "# cheesy Fresnel approximation = (1-(I.N))^p\n" +
      "DP3    R0.x, R8, R11;\n" +
      "ADD    R0.x, c[64].y, -R0.x;\n" +
      "MUL    R0.x, R0.x, R0.x;\n" +
      "MUL    o[COL0], R0.x, c[62];\n" +
      "\n" +
      "# transform vertex to clip space\n" +
      "DP4	o[HPOS].x, c[0], v[OPOS] ;\n" +
      "DP4	o[HPOS].y, c[1], v[OPOS] ;\n" +
      "DP4	o[HPOS].z, c[2], v[OPOS] ;\n" +
      "DP4	o[HPOS].w, c[3], v[OPOS] ;\n" +
      "\n" +
      "END";

    public void init(GLDrawable drawable) {
      GL gl = drawable.getGL();
      GLU glu = drawable.getGLU();
      float cc = 1.0f;
      gl.glClearColor(cc, cc, cc, 1);
      gl.glColor3f(1,1,1);
      gl.glEnable(GL.GL_DEPTH_TEST);

      try {
        initExtension(gl, "GL_NV_vertex_program");
        initExtension(gl, "GL_NV_register_combiners");
        initExtension(gl, "GL_NV_register_combiners");
      } catch (RuntimeException e) {
        runExit();
        throw(e);
      }

      b[' '] = true; // animate by default

      int[] vtxProgTmp = new int[1];
      gl.glGenProgramsNV(1, vtxProgTmp);
      vtxProg = vtxProgTmp[0];
      gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, vtxProg);
      gl.glLoadProgramNV(GL.GL_VERTEX_PROGRAM_NV, vtxProg, transformRefract.length(), transformRefract);

      // FIXME: is this necessary? Only for error checking?
      //      nvparse((const char *)vprog::transform_refract);
      
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 0,  GL.GL_MODELVIEW_PROJECTION_NV, GL.GL_IDENTITY_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 4,  GL.GL_MODELVIEW,               GL.GL_INVERSE_TRANSPOSE_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 8,  GL.GL_MODELVIEW,               GL.GL_IDENTITY_NV);
      gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 12, GL.GL_TEXTURE,                 GL.GL_IDENTITY_NV);

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 59, 0.0f, 0.0f, 0.0f, 1.0f);    // eye position

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 62, fresnel, fresnel, fresnel, 1.0f);    // fresnel multiplier

      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 63, 1.0f, -1.0f, 1.0f, 0.0f);   // texture scale
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 64, 0.0f, 1.0f, 2.0f, 3.0f);    // misc constants

      int[] cubemapTmp = new int[1];
      gl.glGenTextures(1, cubemapTmp);
      cubemap = cubemapTmp[0];
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, cubemap);

      gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
      gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

      try {
        loadPNGCubemap(gl, glu, "demos/data/cubemaps/uffizi", true);
      } catch (IOException e) {
        runExit();
        throw new RuntimeException(e);
      }

      gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

      gl.glDisable(GL.GL_CULL_FACE);

      initCombiners(gl);

      try {
        bunnydl = Bunny.gen3DObjectList(gl);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      drawable.addKeyListener(new KeyAdapter() {
          public void keyTyped(KeyEvent e) {
            dispatchKey(e.getKeyChar());
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
      viewer.setVertFOV((float) (15.0f * Math.PI / 32.0f));
      viewer.setZNear(0.1f);
      viewer.setZFar(10.0f);
    }

    public void display(GLDrawable drawable) {
      if (quit) {
        return;
      }

      time.update();

      GL gl = drawable.getGL();
      GLU glu = drawable.getGLU();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);

      if (doViewAll) {
	viewer.viewAll(gl);
	doViewAll = false;
      }

      if (getFlag(' ')) {
        viewer.rotateAboutFocalPoint(new Rotf(Vec3f.Y_AXIS, (float) (time.deltaT() * animRate)));
      }

      if (toggleWire) {
        toggleWire = false;
        wire = !wire;
        if (wire) {
          gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
        } else {
          gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        }
      }

      // draw background
      gl.glDisable(GL.GL_DEPTH_TEST);
      drawSkyBox(gl, glu);
      gl.glEnable(GL.GL_DEPTH_TEST);

      gl.glPushMatrix();

      viewer.update(gl);
      ManipManager.getManipManager().updateCameraParameters(drawable, viewer.getCameraParameters());
      ManipManager.getManipManager().render(drawable, gl);

      gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, vtxProg);

      gl.glEnable(GL.GL_VERTEX_PROGRAM_NV);
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 62, fresnel, fresnel, fresnel, 1.0f);

      // set texture transforms
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, cubemap);
      gl.glEnable(GL.GL_TEXTURE_CUBE_MAP_ARB);
      gl.glMatrixMode(GL.GL_TEXTURE);
      gl.glLoadIdentity();
      gl.glScalef(1.0f, -1.0f, 1.0f);
      viewer.updateInverseRotation(gl);

      gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, cubemap);
      gl.glEnable(GL.GL_TEXTURE_CUBE_MAP_ARB);
      gl.glMatrixMode(GL.GL_TEXTURE);
      gl.glLoadIdentity();
      gl.glScalef(1.0f, -1.0f, 1.0f);
      viewer.updateInverseRotation(gl);

      gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);

      gl.glColor3f(1.0f, 1.0f, 1.0f);

      if (getFlag('s')) {
        // single pass
        setRefraction(gl, refract);
        drawObj(gl, glu, obj);

      } else {
        // red pass
        gl.glColorMask(true, false, false, false);
        setRefraction(gl, refract);
        drawObj(gl, glu, obj);
  
        gl.glDepthMask(false);
        gl.glDepthFunc(GL.GL_EQUAL);

        // green pass
        gl.glColorMask(false, true, false, false);
        setRefraction(gl, refract + wavelengthDelta);
        drawObj(gl, glu, obj);

        // blue pass
        gl.glColorMask(false, false, true, false);
        setRefraction(gl, refract + (wavelengthDelta * 2));
        drawObj(gl, glu, obj);

        gl.glDepthMask(true);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glColorMask(true, true, true, false);
      }

      gl.glDisable(GL.GL_REGISTER_COMBINERS_NV);
      gl.glDisable(GL.GL_VERTEX_PROGRAM_NV);

      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glPopMatrix();
    }

    // Unused routines
    public void reshape(GLDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    //----------------------------------------------------------------------
    // Internals only below this point
    //
    private boolean[] b = new boolean[256];
    private void dispatchKey(char k) {
      setFlag(k, !getFlag(k));
      // Quit on escape or 'q'
      if ((k == (char) 27) || (k == 'q')) {
        runExit();
	return;
      }

      switch (k) {
        case '1':
	  obj = 0;
	  break;

        case '2':
	  obj = 1;
	  break;

        case '3':
	  obj = 2;
	  break;

        case '4':
	  obj = 3;
	  break;

        case 'v':
	  doViewAll = true;
	  break;

        case 'w':
          toggleWire = true;
          break;

        default:
	  break;
      }
    }

    private void setFlag(char key, boolean val) {
      b[((int) key) & 0xFF] = val;
    }

    private boolean getFlag(char key) {
      return b[((int) key) & 0xFF];
    }

    // FIXME: note we found that we had to swap the negy and posy portions of the cubemap.
    // Not sure why this is the case. Vertical flip in the image read? Possible, but doesn't
    // appear to be the case (have tried this and produced wrong results at the time).
    String[] suffixes = { "posx", "negx", "negy", "posy", "posz", "negz" };
    int[] targets = { GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X_ARB,
                      GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X_ARB,
                      GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y_ARB,
                      GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_ARB,
                      GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z_ARB,
                      GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_ARB };
    private void loadPNGCubemap(GL gl, GLU glu, String baseName, boolean mipmapped) throws IOException {
      for (int i = 0; i < suffixes.length; i++) {
        String resourceName = baseName + "_" + suffixes[i] + ".png";
	BufferedImage img = ImageIO.read(getClass().getClassLoader().getResourceAsStream(resourceName));
	if (img == null) {
	  throw new RuntimeException("Error reading PNG image " + resourceName);
	}
        makeRGBTexture(gl, glu, img, targets[i], mipmapped);
      }
    }

    private void makeRGBTexture(GL gl, GLU glu, BufferedImage img, int target, boolean mipmapped) {
      ByteBuffer dest = null;
      switch (img.getType()) {
        case BufferedImage.TYPE_3BYTE_BGR:
        case BufferedImage.TYPE_CUSTOM: {
          byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
          dest = ByteBuffer.allocateDirect(data.length);
          dest.order(ByteOrder.nativeOrder());
          dest.put(data, 0, data.length);
          break;
        }

        case BufferedImage.TYPE_INT_RGB: {
          int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
          dest = ByteBuffer.allocateDirect(data.length * BufferUtils.SIZEOF_INT);
          dest.order(ByteOrder.nativeOrder());
          dest.asIntBuffer().put(data, 0, data.length);
          break;
        }

        default:
          throw new RuntimeException("Unsupported image type " + img.getType());
      }

      if (mipmapped) {
        glu.gluBuild2DMipmaps(target, GL.GL_RGB8, img.getWidth(), img.getHeight(), GL.GL_RGB,
                              GL.GL_UNSIGNED_BYTE, dest);
      } else {
        gl.glTexImage2D(target, 0, GL.GL_RGB, img.getWidth(), img.getHeight(), 0,
                        GL.GL_RGB, GL.GL_UNSIGNED_BYTE, dest);
      }
    }

    private void initExtension(GL gl, String glExtensionName) {
      if (!gl.isExtensionAvailable(glExtensionName)) {
        throw new RuntimeException("OpenGL extension \"" + glExtensionName + "\" not available");
      }
    }

    // initalize texture combiners to compute:
    // refraction*(1-fresnel) + reflection*fresnel
    private void initCombiners(GL gl) {
      gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 1);
      
      // combiner 0
      // a*b+c*d
      gl.glCombinerInputNV(GL.GL_COMBINER0_NV, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
      gl.glCombinerInputNV(GL.GL_COMBINER0_NV, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_PRIMARY_COLOR_NV, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
      gl.glCombinerInputNV(GL.GL_COMBINER0_NV, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE1_ARB, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
      gl.glCombinerInputNV(GL.GL_COMBINER0_NV, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_PRIMARY_COLOR_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);

      // output:
      // (stage, portion, abOutput, cdOutput, sumOutput, scale, bias, abDotProduct, cdDotProduct, muxSum)
      gl.glCombinerOutputNV(GL.GL_COMBINER0_NV, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);
      
      // final combiner
      // output: Frgb = A*B + (1-A)*C + D
      // (variable, input, mapping, componentUsage);
      gl.glFinalCombinerInputNV(GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
      gl.glFinalCombinerInputNV(GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
      gl.glFinalCombinerInputNV(GL.GL_VARIABLE_C_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
      gl.glFinalCombinerInputNV(GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    }

    private void drawSkyBox(GL gl, GLU glu) {
      // Compensates for ExaminerViewer's modification of modelview matrix
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();

      gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
      gl.glDisable(GL.GL_TEXTURE_CUBE_MAP_ARB);
  
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, cubemap);
      gl.glEnable(GL.GL_TEXTURE_CUBE_MAP_ARB);

      gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP_EXT);
      gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP_EXT);
      gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_NORMAL_MAP_EXT);

      gl.glEnable(GL.GL_TEXTURE_GEN_S);
      gl.glEnable(GL.GL_TEXTURE_GEN_T);
      gl.glEnable(GL.GL_TEXTURE_GEN_R);

      gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

      gl.glMatrixMode(GL.GL_TEXTURE);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      gl.glScalef(1.0f, -1.0f, 1.0f);
      viewer.updateInverseRotation(gl);
    
      glut.glutSolidSphere(glu, 5.0, 40, 20);

      gl.glPopMatrix();
      gl.glMatrixMode(GL.GL_MODELVIEW);

      gl.glDisable(GL.GL_TEXTURE_GEN_S);
      gl.glDisable(GL.GL_TEXTURE_GEN_T);
      gl.glDisable(GL.GL_TEXTURE_GEN_R);
    }

    private void drawObj(GL gl, GLU glu, int obj) {
      switch(obj) {
      case 0:
        gl.glCallList(bunnydl);
        break;
    
      case 1:
        glut.glutSolidSphere(glu, 0.5, 64, 64);
        break;

      case 2:
	glut.glutSolidTorus(gl, 0.25, 0.5, 64, 64);
	break;

      case 3:
        drawPlane(gl, 1.0f, 1.0f, 50, 50);
	break;
      }
    }

    private void setRefraction(GL gl, float index) {
      gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, 58, index, index*index, 0.0f, 0.0f);
    }

    // draw square subdivided into quad strips
    private void drawPlane(GL gl, float w, float h, int rows, int cols) {
      int x, y;
      float vx, vy, s, t;
      float ts, tt, tw, th;

      ts = 1.0f / cols;
      tt = 1.0f / rows;

      tw = w / cols;
      th = h / rows;

      gl.glNormal3f(0.0f, 0.0f, 1.0f);

      for(y=0; y<rows; y++) {
	gl.glBegin(GL.GL_QUAD_STRIP);
	for(x=0; x<=cols; x++) {
	  vx = tw * x -(w/2.0f);
	  vy = th * y -(h/2.0f);
	  s = ts * x;
	  t = tt * y;

	  gl.glTexCoord2f(s, t);
	  gl.glColor3f(s, t, 0.0f);
	  gl.glVertex3f(vx, vy, 0.0f);

	  gl.glColor3f(s, t + tt, 0.0f);
	  gl.glTexCoord2f(s, t + tt);
	  gl.glVertex3f(vx, vy + th, 0.0f);
	}
	gl.glEnd();
      }
    }
  }

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
