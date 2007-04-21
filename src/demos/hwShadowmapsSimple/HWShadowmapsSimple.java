/*
 * Portions Copyright (C) 2003-2005 Sun Microsystems, Inc.
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

package demos.hwShadowmapsSimple;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.texture.*;
import demos.common.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/** This demo is a simple illustration of ARB_shadow and ARB_depth_texture. <P>
    Cass Everitt <BR>
    12-11-00 <P>

    hw_shadowmaps_simple (c) 2001 NVIDIA Corporation <P>

    Ported to Java by Kenneth Russell
*/

public class HWShadowmapsSimple extends Demo {
  public static void main(String[] args) {
    final GLCanvas canvas = new GLCanvas();
    HWShadowmapsSimple demo = new HWShadowmapsSimple();
    canvas.addGLEventListener(demo);

    demo.setDemoListener(new DemoListener() {
        public void shutdownDemo() {
          runExit();
        }
        public void repaint() {
          canvas.repaint();
        }
      });

    Frame frame = new Frame("ARB_shadow Shadows");
    frame.setLayout(new BorderLayout());
    canvas.setSize(512, 512);
    frame.add(canvas, BorderLayout.CENTER);
    frame.pack();
    frame.show();
    canvas.requestFocus();

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          runExit();
        }
      });
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public void shutdownDemo() {
    ManipManager.getManipManager().unregisterWindow(drawable);
    drawable.removeGLEventListener(this);
    super.shutdownDemo();
  }

  private GLPbuffer pbuffer;

  private GLU  glu;
  private GLUT glut;

  private float[] light_ambient   = { 0, 0, 0, 0 };
  private float[] light_intensity = { 1, 1, 1, 1 };
  private float[] light_pos       = { 0, 0, 0, 1 };

  static class Tweak {
    String name;
    float val;
    float incr;

    Tweak(String name, float val, float incr) {
      this.name = name;
      this.val = val;
      this.incr = incr;
    }
  };
  private java.util.List/*<Tweak>*/ tweaks = new ArrayList();
  private static final int R_COORDINATE_SCALE   = 0;
  private static final int R_COORDINATE_BIAS    = 1;
  private static final int POLYGON_OFFSET_SCALE = 2;
  private static final int POLYGON_OFFSET_BIAS  = 3;
  private int curr_tweak;

  // Texture objects
  private static final int TEX_SIZE = 1024;
  private Texture decal;
  private Texture light_image;
  private int light_view_depth;

  // Depth buffer format
  private int depth_format;

  private boolean fullyInitialized;

  // Display mode
  private static final int RENDER_SCENE_FROM_CAMERA_VIEW = 0;
  private static final int RENDER_SCENE_FROM_CAMERA_VIEW_SHADOWED = 1;
  private static final int RENDER_SCENE_FROM_LIGHT_VIEW = 2;
  private static final int NUM_DISPLAY_MODES = 3;
  private int displayMode = 1;

  // Display lists
  private int quad;
  private int wirecube;
  private int geometry;

  // Shadowing light
  private float lightshaper_fovy  = 60.0f;
  private float lightshaper_zNear = 0.5f;
  private float lightshaper_zFar  = 5.0f;

  // Manipulators
  private GLAutoDrawable drawable;
  private ExaminerViewer viewer;
  private boolean  doViewAll = true;
  //  private float    zNear = 0.5f;
  //  private float    zFar  = 5.0f;
  private float    zNear = 0.5f;
  private float    zFar  = 50.0f;
  private HandleBoxManip object;
  private HandleBoxManip spotlight;
  private Mat4f cameraPerspective = new Mat4f();
  private Mat4f cameraTransform = new Mat4f();
  private Mat4f cameraInverseTransform = new Mat4f();
  private Mat4f spotlightTransform = new Mat4f();
  private Mat4f spotlightInverseTransform = new Mat4f();
  private Mat4f objectTransform = new Mat4f();
  private int viewportX;
  private int viewportY;

  public void init(GLAutoDrawable drawable) {
    // Use debug pipeline
    // drawable.setGL(new DebugGL(drawable.getGL()));

    GL gl = drawable.getGL();
    glu = new GLU();
    glut = new GLUT();

    try {
      checkExtension(gl, "GL_VERSION_1_3"); // For multitexture
      checkExtension(gl, "GL_ARB_depth_texture");
      checkExtension(gl, "GL_ARB_shadow");
      checkExtension(gl, "GL_ARB_pbuffer");
      checkExtension(gl, "GL_ARB_pixel_format");
    } catch (GLException e) {
      e.printStackTrace();
      throw(e);
    }
      
    gl.glClearColor(.5f, .5f, .5f, .5f);

    try {
      decal = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/decal_image.png"),
                                   true,
                                   TextureIO.PNG);
      decal.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
      decal.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
      light_image = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/nvlogo_spot.png"),
                                         true,
                                         TextureIO.PNG);
    } catch (IOException e) {
      throw new GLException(e);
    }

    quad = gl.glGenLists(1);
    gl.glNewList(quad, GL.GL_COMPILE);
    gl.glPushMatrix();
    gl.glRotatef(-90, 1, 0, 0);
    gl.glScalef(4,4,4);
    gl.glBegin(GL.GL_QUADS);
    gl.glNormal3f(0, 0, 1);
    gl.glVertex2f(-1, -1);
    gl.glVertex2f(-1,  1);
    gl.glVertex2f( 1,  1);
    gl.glVertex2f( 1, -1);
    gl.glEnd();
    gl.glPopMatrix();
    gl.glEndList();

    wirecube = gl.glGenLists(1);
    gl.glNewList(wirecube, GL.GL_COMPILE);
    glut.glutWireCube(2);
    gl.glEndList();

    geometry = gl.glGenLists(1);
    gl.glNewList(geometry, GL.GL_COMPILE);
    gl.glPushMatrix();
    glut.glutSolidTeapot(0.8f);
    gl.glPopMatrix();
    gl.glEndList();

    gl.glEnable(GL.GL_LIGHT0);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, light_ambient, 0);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light_intensity, 0);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, light_intensity, 0);

    gl.glEnable(GL.GL_DEPTH_TEST);

    // init pbuffer
    GLCapabilities caps = new GLCapabilities();
    caps.setDoubleBuffered(false);
      
    if (!GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
      unavailableExtension("Can not create pbuffer");
    }
    if (pbuffer != null) {
      pbuffer.destroy();
      pbuffer = null;
    }
    pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, TEX_SIZE, TEX_SIZE, drawable.getContext());
    pbuffer.addGLEventListener(new PbufferListener());

    doViewAll = true;

    // Register the window with the ManipManager
    ManipManager manager = ManipManager.getManipManager();
    manager.registerWindow(drawable);
    this.drawable = drawable;

    object = new HandleBoxManip();
    object.setTranslation(new Vec3f(0, 0.7f, 1.8f));
    object.setGeometryScale(new Vec3f(0.7f, 0.7f, 0.7f));
    manager.showManipInWindow(object, drawable);

    spotlight = new HandleBoxManip();
    spotlight.setScale(new Vec3f(0.5f, 0.5f, 0.5f));
    spotlight.setTranslation(new Vec3f(-0.25f, 2.35f, 5.0f));
    spotlight.setRotation(new Rotf(Vec3f.X_AXIS, (float) Math.toRadians(-30.0f)));
    manager.showManipInWindow(spotlight, drawable);

    viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
    viewer.attach(drawable, new BSphereProvider() {
        public BSphere getBoundingSphere() {
          return new BSphere(object.getTranslation(), 2.0f);
        }
      });
    viewer.setOrientation(new Rotf(Vec3f.Y_AXIS, (float) Math.toRadians(45.0f)).times
                          (new Rotf(Vec3f.X_AXIS, (float) Math.toRadians(-15.0f))));
    viewer.setVertFOV((float) Math.toRadians(lightshaper_fovy / 2.0f));
    viewer.setZNear(zNear);
    viewer.setZFar(zFar);

    float bias = 1/((float) Math.pow(2.0,16.0)-1);

    tweaks.add(new Tweak("r coordinate scale",    0.5f, bias));
    tweaks.add(new Tweak("r coordinate bias",     0.5f, bias));
    tweaks.add(new Tweak("polygon offset scale",  2.5f, 0.5f));
    tweaks.add(new Tweak("polygon offset bias",  10.0f, 1.0f));

    drawable.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          dispatchKey(e.getKeyChar());
          demoListener.repaint();
        }
      });
  }

  public void display(GLAutoDrawable drawable) {
    viewer.update();

    // Grab these values once per render to avoid multithreading
    // issues with their values being changed by manipulation from
    // the AWT thread during the render
    CameraParameters params = viewer.getCameraParameters();

    cameraPerspective.set(params.getProjectionMatrix());
    cameraInverseTransform.set(params.getModelviewMatrix());
    cameraTransform.set(cameraInverseTransform);
    cameraTransform.invertRigid();
    spotlightTransform.set(spotlight.getTransform());
    spotlightInverseTransform.set(spotlightTransform);
    spotlightInverseTransform.invertRigid();
    objectTransform.set(object.getTransform());

    if (displayMode == RENDER_SCENE_FROM_CAMERA_VIEW_SHADOWED || !fullyInitialized) {
      if (pbuffer != null) {
        pbuffer.display();
      }
    }

    if (!fullyInitialized) {
      // Repaint again later once everything is set up
      demoListener.repaint();
      return;
    }

    GL gl = drawable.getGL();

    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    if (doViewAll) {
      viewer.viewAll(gl);
      doViewAll = false;
      // Immediately zap effects
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();
      // Schedule repaint to clean up first bogus frame
      demoListener.repaint();
    }

    switch (displayMode) {
    case RENDER_SCENE_FROM_CAMERA_VIEW:          render_scene_from_camera_view(gl, drawable, params); break;
    case RENDER_SCENE_FROM_CAMERA_VIEW_SHADOWED: render_scene_from_camera_view_shadowed(gl, drawable, params); break;
    case RENDER_SCENE_FROM_LIGHT_VIEW:           render_scene_from_light_view(gl, drawable, viewportX, viewportY); break;
    default: throw new RuntimeException("Illegal display mode " + displayMode);
    }
  }

  // Unused routines
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    viewportX = x;
    viewportY = y;
  }
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  private void checkExtension(GL gl, String extensionName) {
    if (!gl.isExtensionAvailable(extensionName)) {
      String message = "Unable to initialize " + extensionName + " OpenGL extension";
      unavailableExtension(message);
    }
  }

  private void unavailableExtension(String message) {
    JOptionPane.showMessageDialog(null, message, "Unavailable extension", JOptionPane.ERROR_MESSAGE);
    throw new GLException(message);
  }

  private void dispatchKey(char k) {
    switch (k) {
    case 27:
    case 'q':
      shutdownDemo();
      break;

    case 'v':
      doViewAll = true;
      System.err.println("Forcing viewAll()");
      break;

    case ' ':
      displayMode = (displayMode + 1) % NUM_DISPLAY_MODES;
      System.err.println("Switching to display mode " + displayMode);
      break;

      // FIXME: add more key behaviors from original demo

    default:
      break;
    }
  }

  class PbufferListener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      // Use debug pipeline
      // drawable.setGL(new DebugGL(drawable.getGL()));

      GL gl = drawable.getGL();

      gl.glEnable(GL.GL_DEPTH_TEST);

      int[] depth_bits = new int[1];
      gl.glGetIntegerv(GL.GL_DEPTH_BITS, depth_bits, 0);
        
      if (depth_bits[0] == 16)  depth_format = GL.GL_DEPTH_COMPONENT16_ARB;
      else                      depth_format = GL.GL_DEPTH_COMPONENT24_ARB;

      light_view_depth = genTexture(gl);
      gl.glBindTexture(GL.GL_TEXTURE_2D, light_view_depth);
      gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, depth_format, TEX_SIZE, TEX_SIZE, 0, 
                      GL.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT, null);
      set_light_view_texture_parameters(gl);

      fullyInitialized = true;
    }

    public void display(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();

      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      gl.glPolygonOffset(((Tweak) tweaks.get(POLYGON_OFFSET_SCALE)).val,
                         ((Tweak) tweaks.get(POLYGON_OFFSET_BIAS)).val);
      gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

      render_scene_from_light_view(gl, drawable, 0, 0);

      gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
    
      gl.glBindTexture(GL.GL_TEXTURE_2D, light_view_depth);

      // trying different ways of getting the depth info over
      gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0,   0, 0,   0, 0,  TEX_SIZE, TEX_SIZE);
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  private void set_light_view_texture_parameters(GL gl) {
    gl.glBindTexture(GL.GL_TEXTURE_2D, light_view_depth);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_COMPARE_MODE_ARB, GL.GL_COMPARE_R_TO_TEXTURE_ARB);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_COMPARE_FUNC_ARB, GL.GL_LEQUAL);
  }

  private int genTexture(GL gl) {
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    return tmp[0];
  }

  private void eye_linear_texgen(GL gl) {
    Mat4f m = new Mat4f();
    m.makeIdent();

    set_texgen_planes(gl, GL.GL_EYE_PLANE, m);
    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
    gl.glTexGeni(GL.GL_Q, GL.GL_TEXTURE_GEN_MODE, GL.GL_EYE_LINEAR);
  }

  private void obj_linear_texgen(GL gl) {
    Mat4f m = new Mat4f();
    m.makeIdent();

    set_texgen_planes(gl, GL.GL_OBJECT_PLANE, m);
    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    gl.glTexGeni(GL.GL_Q, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
  }

  private void set_texgen_planes(GL gl, int plane_type, Mat4f m) {
    int[] coord = {GL.GL_S, GL.GL_T, GL.GL_R, GL.GL_Q};
    float[] row = new float[4];
    for(int i = 0; i < 4; i++) {
      getRow(m, i, row);
      gl.glTexGenfv(coord[i], plane_type, row, 0);
    }
  }

  private void texgen(GL gl, boolean enable) {
    if(enable) {
      gl.glEnable(GL.GL_TEXTURE_GEN_S);
      gl.glEnable(GL.GL_TEXTURE_GEN_T);
      gl.glEnable(GL.GL_TEXTURE_GEN_R);
      gl.glEnable(GL.GL_TEXTURE_GEN_Q);
    } else {
      gl.glDisable(GL.GL_TEXTURE_GEN_S);
      gl.glDisable(GL.GL_TEXTURE_GEN_T);
      gl.glDisable(GL.GL_TEXTURE_GEN_R);
      gl.glDisable(GL.GL_TEXTURE_GEN_Q);
    }
  }

  private void render_light_frustum(GL gl) {
    gl.glPushMatrix();
    applyTransform(gl, cameraInverseTransform);
    applyTransform(gl, spotlightTransform);
    applyTransform(gl, perspectiveInverse(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar));
    gl.glDisable(GL.GL_LIGHTING);
    gl.glColor3f(1,1,0);
    gl.glCallList(wirecube);
    gl.glColor3f(1,1,1);
    gl.glEnable(GL.GL_LIGHTING);
    gl.glPopMatrix();
  }

  private void render_quad(GL gl) {
    gl.glActiveTexture(GL.GL_TEXTURE0);
    obj_linear_texgen(gl);
    texgen(gl, true);
    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glScalef(4,4,1);
    gl.glMatrixMode(GL.GL_MODELVIEW);

    gl.glDisable(GL.GL_LIGHTING);
    decal.bind();
    decal.enable();
    gl.glCallList(quad);
    decal.disable();
    gl.glEnable(GL.GL_LIGHTING);

    texgen(gl, false);
    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glMatrixMode(GL.GL_MODELVIEW);
  }

  private void render_scene(GL gl, Mat4f view, GLAutoDrawable drawable, CameraParameters params) {
    gl.glColor3f(1,1,1);
    gl.glPushMatrix();
    Mat4f inverseView = new Mat4f(view);
    inverseView.invertRigid();
    applyTransform(gl, inverseView);

    gl.glPushMatrix();
    render_quad(gl);

    applyTransform(gl, objectTransform);

    gl.glEnable(GL.GL_LIGHTING);
    gl.glCallList(geometry);
    gl.glDisable(GL.GL_LIGHTING);

    gl.glPopMatrix();

    gl.glPopMatrix();
  }

  private void render_manipulators(GL gl, Mat4f view, GLAutoDrawable drawable, CameraParameters params) {
    gl.glColor3f(1,1,1);
    gl.glPushMatrix();
    Mat4f inverseView = new Mat4f(view);
    inverseView.invertRigid();
    applyTransform(gl, inverseView);

    if (params != null) {
      ManipManager.getManipManager().updateCameraParameters(drawable, params);
      ManipManager.getManipManager().render(drawable, gl);
    }

    gl.glPopMatrix();
  }

  private void render_scene_from_camera_view(GL gl, GLAutoDrawable drawable, CameraParameters params) {
    // place light
    gl.glPushMatrix();
    gl.glLoadIdentity();
    applyTransform(gl, cameraInverseTransform);
    applyTransform(gl, spotlightTransform);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light_pos, 0);
    gl.glPopMatrix();

    // spot image
    gl.glActiveTexture(GL.GL_TEXTURE1);

    gl.glPushMatrix();
    applyTransform(gl, cameraInverseTransform);
    eye_linear_texgen(gl);    
    texgen(gl, true);
    gl.glPopMatrix();

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glTranslatef(.5f, .5f, .5f);
    gl.glScalef(.5f, -.5f, .5f);
    glu.gluPerspective(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar);
    applyTransform(gl, spotlightInverseTransform);
    gl.glMatrixMode(GL.GL_MODELVIEW);

    light_image.bind();
    light_image.enable();
    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glViewport(viewportX, viewportY, drawable.getWidth(), drawable.getHeight());
    applyTransform(gl, cameraPerspective);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    render_scene(gl, cameraTransform, drawable, params);

    gl.glActiveTexture(GL.GL_TEXTURE1);
    light_image.disable();
    gl.glActiveTexture(GL.GL_TEXTURE0);

    render_manipulators(gl, cameraTransform, drawable, params);

    render_light_frustum(gl);
  }

  private void render_scene_from_camera_view_shadowed(GL gl, GLAutoDrawable drawable, CameraParameters params) {
    // place light
    gl.glPushMatrix();
    gl.glLoadIdentity();
    applyTransform(gl, cameraInverseTransform);
    applyTransform(gl, spotlightTransform);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light_pos, 0);
    gl.glPopMatrix();

    // spot image
    gl.glActiveTexture(GL.GL_TEXTURE1);

    gl.glPushMatrix();
    applyTransform(gl, cameraInverseTransform);
    eye_linear_texgen(gl);    
    texgen(gl, true);
    gl.glPopMatrix();

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glTranslatef(.5f, .5f, .5f);
    gl.glScalef(.5f, -.5f, .5f);
    glu.gluPerspective(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar);
    applyTransform(gl, spotlightInverseTransform);
    gl.glMatrixMode(GL.GL_MODELVIEW);

    light_image.bind();
    light_image.enable();
    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

    // depth compare
    gl.glActiveTexture(GL.GL_TEXTURE2);

    gl.glPushMatrix();
    applyTransform(gl, cameraInverseTransform);
    eye_linear_texgen(gl);    
    texgen(gl, true);
    gl.glPopMatrix();

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glTranslatef(.5f, .5f, ((Tweak) tweaks.get(R_COORDINATE_SCALE)).val);
    gl.glScalef(.5f, .5f, ((Tweak) tweaks.get(R_COORDINATE_BIAS)).val);
    glu.gluPerspective(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar);
    applyTransform(gl, spotlightInverseTransform);
    gl.glMatrixMode(GL.GL_MODELVIEW);

    gl.glBindTexture(GL.GL_TEXTURE_2D, light_view_depth);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
    
    gl.glActiveTexture(GL.GL_TEXTURE0);

    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glViewport(viewportX, viewportY, drawable.getWidth(), drawable.getHeight());
    applyTransform(gl, cameraPerspective);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    render_scene(gl, cameraTransform, drawable, params);

    gl.glActiveTexture(GL.GL_TEXTURE1);
    light_image.disable();
    gl.glActiveTexture(GL.GL_TEXTURE2);
    gl.glDisable(GL.GL_TEXTURE_2D);
    gl.glActiveTexture(GL.GL_TEXTURE0);

    render_manipulators(gl, cameraTransform, drawable, params);

    render_light_frustum(gl);
  }

  private void largest_square_power_of_two_viewport(GL gl, GLAutoDrawable drawable, int viewportX, int viewportY) {
    float min = Math.min(drawable.getWidth(), drawable.getHeight());
    float log2min = (float) Math.log(min) / (float) Math.log(2.0);
    float pow2 = (float) Math.floor(log2min);
    int size = 1 << (int) pow2;
    gl.glViewport(viewportX, viewportY, size, size);
  }

  private void render_scene_from_light_view(GL gl, GLAutoDrawable drawable, int viewportX, int viewportY) {
    // place light
    gl.glPushMatrix();
    gl.glLoadIdentity();
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light_pos, 0);
    gl.glPopMatrix();

    // spot image
    gl.glActiveTexture(GL.GL_TEXTURE1);

    gl.glPushMatrix();
    eye_linear_texgen(gl);    
    texgen(gl, true);
    gl.glPopMatrix();

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glTranslatef(.5f, .5f, .5f);
    gl.glScalef(.5f, .5f, .5f);
    glu.gluPerspective(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar);
    gl.glMatrixMode(GL.GL_MODELVIEW);

    light_image.bind();
    light_image.enable();
    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

    gl.glActiveTexture(GL.GL_TEXTURE0);

    gl.glViewport(0, 0, TEX_SIZE, TEX_SIZE);
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(lightshaper_fovy, 1, lightshaper_zNear, lightshaper_zFar);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    if (displayMode == RENDER_SCENE_FROM_LIGHT_VIEW)
      largest_square_power_of_two_viewport(gl, drawable, viewportX, viewportY);
    render_scene(gl, spotlightTransform, null, null);

    gl.glActiveTexture(GL.GL_TEXTURE1);
    light_image.disable();
    gl.glActiveTexture(GL.GL_TEXTURE0);
  }

  private static void getRow(Mat4f m, int row, float[] out) {
    out[0] = m.get(row, 0);
    out[1] = m.get(row, 1);
    out[2] = m.get(row, 2);
    out[3] = m.get(row, 3);
  }

  private static void applyTransform(GL gl, Mat4f xform) {
    float[] data = new float[16];
    xform.getColumnMajorData(data);
    gl.glMultMatrixf(data, 0);
  }

  private static Mat4f perspectiveInverse(float fovy, float aspect, float zNear, float zFar) {
    float tangent = (float) Math.tan(Math.toRadians(fovy / 2.0));
    float y = tangent * zNear;
    float x = aspect * y;
    return frustumInverse(-x, x, -y, y, zNear, zFar);
  }

  private static Mat4f frustumInverse(float left, float right,
                               float bottom, float top,
                               float zNear, float zFar) {
    Mat4f m = new Mat4f();
    m.makeIdent();

    m.set(0, 0, (right - left) / (2 * zNear));
    m.set(0, 3, (right + left) / (2 * zNear));
	
    m.set(1, 1, (top - bottom) / (2 * zNear));
    m.set(1, 3, (top + bottom) / (2 * zNear));

    m.set(2, 2,  0);
    m.set(2, 3, -1);
	
    m.set(3, 2, -(zFar - zNear) / (2 * zFar * zNear));
    m.set(3, 3,  (zFar + zNear) / (2 * zFar * zNear));

    return m;
  }

  private static void runExit() {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Run the
    // exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          System.exit(0);
        }
      }).start();
  }
}
