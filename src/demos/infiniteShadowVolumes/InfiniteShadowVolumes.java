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

package demos.infiniteShadowVolumes;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import demos.common.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/**
  Infinite shadow volumes are described in the paper 
  "Practical and Robust Stenciled Shadow Volumes for
   Hardware-Accelerated Rendering" which can be found
   online at: <P>
   
 <a href = "http://developer.nvidia.com/view.asp?IO=robust_shadow_volumes">http://developer.nvidia.com/view.asp?IO=robust_shadow_volumes</a><P>

  This code is intended to illustrate the technique.  It
  is not optimized for performance. <P>

  Cass Everitt <BR>
  04-04-2002 <P>

  Ported to Java by Kenneth Russell
*/

public class InfiniteShadowVolumes extends Demo {
  public static void main(String[] args) {
    GLCapabilities caps = new GLCapabilities();
    caps.setStencilBits(16);
    final GLCanvas canvas = new GLCanvas(caps);
    InfiniteShadowVolumes demo = new InfiniteShadowVolumes();
    canvas.addGLEventListener(demo);

    demo.setDemoListener(new DemoListener() {
        public void shutdownDemo() {
          runExit();
        }
        public void repaint() {
          canvas.repaint();
        }
      });
    
    Frame frame = new Frame("Infinite Stenciled Shadow Volumes");
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

  static class Model {
    Model() {
      frame_num = 0;
      frame_incr = 0.25f;
      draw = true;
      ambient =  new Vec4f(0.1f, 0.1f, 0.1f, 1);
      diffuse =  new Vec4f(0.8f, 0,    0,    1);
      specular = new Vec4f(0.6f, 0.6f, 0.6f, 1);
      shininess = 64;
    }

    MD2.Model mod;
    MD2.Frame interp_frame;
    float frame_num;
    float frame_incr;

    Vec4f ambient;
    Vec4f diffuse;
    Vec4f specular;
    float shininess;
    boolean draw;
  };

  // You can load multiple models and 
  // position them independently.  If they're
  // quake2 models you can animate them as well.  

  private static final int MAX_MODELS = 4;
  private Model[] m = new Model[MAX_MODELS];
  private int curr_model = 0;
  private int num_models = 0;

  // selector for the current view mode
  private static final int CAMERA_VIEW = 0;
  private static final int SCENE_VIEW  = 1;
  private static final int CLIP_VIEW   = 2;
  private int curr_view  = CAMERA_VIEW;

  private GLU  glu  = new GLU();
  private GLUT glut = new GLUT();

  private GLAutoDrawable drawable;
  private ExaminerViewer viewer;
  private HandleBoxManip objectManip;
  private HandleBoxManip lightManip;
  private Mat4f objectManipXform;
  private Mat4f lightManipXform;
  int faceDisplayList;
  int wallTexObject;

  private boolean[] b = new boolean[256];

  Vec4f light_position = new Vec4f(0,0,0,1);
  float light_object_scale = 1;
  float volume_alpha = .1f;
  float room_ambient = .3f;

  boolean doViewAll = true;

  private boolean enableDepthClampNV;
  private boolean toggleDepthClampNV;
  private boolean animateContinually;
  private boolean animateForward;
  private boolean animateBackward;
  private boolean hideCurrentModel;
  private boolean toggleWireframe;

  public void init(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();

    gl.glClearStencil(128);
    //glEnable(GL.GL_DEPTH_CLAMP_NV);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDepthFunc(GL.GL_LESS);
    gl.glEnable(GL.GL_NORMALIZE);
    gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
    float[] ambient = new float[] {0.3f, 0.3f, 0.3f, 1};
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, ambient, 0);
    faceDisplayList = gl.glGenLists(1);
    gl.glNewList(faceDisplayList, GL.GL_COMPILE);
    drawMesh(gl, 20, 40);
    gl.glEndList();

    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    wallTexObject = tmp[0];
    gl.glBindTexture(GL.GL_TEXTURE_2D, wallTexObject);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

    float[] tex = new float[32*32];
    for(int i=0; i < 32; i++) {
      for(int j=0; j < 32; j++) {
        if ((i>>4 ^ j>>4) != 0)
          tex[i+j*32] = 1;
        else
          tex[i+j*32] = .9f;
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, 32, 32, 0, GL.GL_LUMINANCE, GL.GL_FLOAT, FloatBuffer.wrap(tex));
      
    initModel();
    
    b['S'] = true; // no silhouette outlines
    b['v'] = true; // no volume drawing
    b['I'] = true; // use infinite far plane
    b['L'] = true; // use local light for shadowing

    doViewAll = true;

    drawable.addKeyListener(new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          dispatchKey(e.getKeyChar());
          demoListener.repaint();
        }
      });

    // Register the window with the ManipManager
    ManipManager manager = ManipManager.getManipManager();
    manager.registerWindow(drawable);
    this.drawable = drawable;

    objectManip = new HandleBoxManip();
    manager.showManipInWindow(objectManip, drawable);
    objectManip.setTranslation(new Vec3f(0, 0, -2));
    objectManip.setRotation(new Rotf(new Vec3f(1, 0, 0), (float) Math.toRadians(-90)));

    lightManip = new HandleBoxManip();
    manager.showManipInWindow(lightManip, drawable);
    lightManip.setTranslation(new Vec3f(0.5f, 0.5f, -1));
    lightManip.setGeometryScale(new Vec3f(0.1f, 0.1f, 0.1f));

    viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
    viewer.attach(drawable, new BSphereProvider() {
        public BSphere getBoundingSphere() {
          return new BSphere(objectManip.getTranslation(), 1.0f);
        }
      });
    viewer.setZNear(1.0f);
    viewer.setZFar(100.0f);
    viewer.setOrientation(new Rotf(new Vec3f(0, 1, 0), (float) Math.toRadians(15)));

    // FIXME
    //      glutAddMenuEntry("mouse controls view [1]", '1');
    //      glutAddMenuEntry("mouse controls model  [2]", '2');
    //      glutAddMenuEntry("mouse controls light  [3]", '3');
    //      glutAddMenuEntry("mouse controls room   [4]", '4');
    //      glutAddMenuEntry("enable depth clamp [!]", '!');
    //      glutAddMenuEntry("disable depth clamp [~]", '~');
    //      glutAddMenuEntry("start animation [ ]", ' ');
    //      glutAddMenuEntry("step animation forward [a]", 'a');
    //      glutAddMenuEntry("step animation backward [b]", 'b');
    //      glutAddMenuEntry("toggle drawing silhouette [S]", 'S');
    //      glutAddMenuEntry("toggle drawing shadow  [s]", 's');
    //      glutAddMenuEntry("toggle drawing visible shadow volume [v]", 'v');
    //      glutAddMenuEntry("toggle drawing model geometry[m]", 'm');

    //      glutAddMenuEntry("increase shadow volume alpha [;]", ';');
    //      glutAddMenuEntry("decrease shadow volume alpha [:]", ':');

    //      glutAddMenuEntry("next model [,]", ',');
    //      glutAddMenuEntry("hide current model [.]", '.');

    //      glutAddMenuEntry("toggle view frustum clip planes [X]", 'X');

    //      glutAddMenuEntry("camera view [5]", '5');
    //      glutAddMenuEntry("scene view [6]", '6');
    //      glutAddMenuEntry("clipspace view [7]", '7');

    //      glutAddMenuEntry("enable depth clamp [!]", '!');
    //      glutAddMenuEntry("disable depth clamp [~]", '~');

    //      glutAddMenuEntry("increase light size [n]", 'n');
    //      glutAddMenuEntry("decrease light size [N]", 'N');

    //      glutAddMenuEntry("move near plane in [[]", '[');
    //      glutAddMenuEntry("move near plane out []]", ']');
    //      glutAddMenuEntry("move far plane in [{]", '[');
    //      glutAddMenuEntry("move far plane out [}]", ']');

    //      glutAddMenuEntry("toggle local/infinite light [L]", 'L');

    //      glutAddMenuEntry("hide room [R]", 'R');

    //      glutAddMenuEntry("view all with camera [c]", 'c');

    //      glutAddMenuEntry("quit [<esc>]", 27);
  }

  public void display(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();

    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();

    if (doViewAll) {
      viewer.viewAll(gl);
      doViewAll = false;
    }

    objectManipXform = objectManip.getTransform();
    lightManipXform  = lightManip.getTransform();

    if (toggleDepthClampNV) {
      if (enableDepthClampNV) {
        gl.glEnable(GL.GL_DEPTH_CLAMP_NV);
      } else {
        gl.glDisable(GL.GL_DEPTH_CLAMP_NV);
      }
      toggleDepthClampNV = false;
    }

    if (b[' ']) {
      animateForward = true;
    }

    if (animateForward) {
      Model mm = m[curr_model];
      mm.frame_num += mm.frame_incr;
      if (mm.frame_num >= mm.mod.f.length)
        mm.frame_num = 0;
      interpolate_frame();
      animateForward = false;
    }

    if (animateBackward) {
      Model mm = m[curr_model];
      mm.frame_num -= mm.frame_incr;
      if (mm.frame_num < 0)
        mm.frame_num += mm.mod.f.length;
      interpolate_frame();
      animateBackward = false;
    }

    if (hideCurrentModel) {
      gl.glNewList(faceDisplayList, GL.GL_COMPILE);
      drawMesh(gl, 20, 40);
      gl.glEndList();
      hideCurrentModel = false;
    }

    if (toggleWireframe) {
      if(b['w'])
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
      else
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
    }

    if(b['I']) {
      // push far plane to infinity
      switch (curr_view) {
      case CAMERA_VIEW:
        viewer.update(gl);
        // Undo perspective effects of ExaminerViewer
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        applyInfinitePerspective(gl, viewer);
        break;

      case SCENE_VIEW:
        applyInfinitePerspective(gl, viewer);
        // FIXME: do we need more primitives in the ExaminerViewer class?
        //            scenecam.apply_inverse_transform();
        break;

      case CLIP_VIEW:
        applyInfinitePerspective(gl, viewer);
        // FIXME
        //            clipcam.apply_inverse_transform();
        gl.glScalef(10,10,-10);
        applyInfinitePerspective(gl, viewer);
        break;

      default:
        break;
      }
    } else {
      switch (curr_view) {
      case CAMERA_VIEW:
        viewer.update(gl);
        break;

      case SCENE_VIEW:
        applyInfinitePerspective(gl, viewer);
        // FIXME
        //            scenecam.apply_inverse_transform();
        break;

      case CLIP_VIEW:
        applyInfinitePerspective(gl, viewer);
        // FIXME
        //            clipcam.apply_inverse_transform();
        gl.glScalef(10,10,-10);
        // FIXME
        //            reshaper.apply_projection();
        break;

      default:
        break;
      }
    }

    gl.glMatrixMode(GL.GL_MODELVIEW);

    // FIXME
    if (b['X']) {
      gl.glLoadIdentity();
      if(b['I']) {
        // FIXME
        applyInfinitePerspectiveInverse(gl, viewer);
      } else {
        // FIXME
        //          reshaper.apply_projection_inverse();
      }
      double[] pos_x = new double[] {-1, 0, 0, 1};
      double[] neg_x = new double[] { 1, 0, 0, 1};
      double[] pos_y = new double[] { 0,-1, 0, 1};
      double[] neg_y = new double[] { 0, 1, 0, 1};
      double[] pos_z = new double[] { 0, 0,-1, 1};
      double[] neg_z = new double[] { 0, 0, 1, 1};
      gl.glClipPlane(GL.GL_CLIP_PLANE0, pos_x, 0);
      gl.glClipPlane(GL.GL_CLIP_PLANE1, neg_x, 0);
      gl.glClipPlane(GL.GL_CLIP_PLANE2, pos_y, 0);
      gl.glClipPlane(GL.GL_CLIP_PLANE3, neg_y, 0);
      gl.glClipPlane(GL.GL_CLIP_PLANE4, pos_z, 0);
      gl.glClipPlane(GL.GL_CLIP_PLANE5, neg_z, 0);
      gl.glEnable(GL.GL_CLIP_PLANE0);
      gl.glEnable(GL.GL_CLIP_PLANE1);
      gl.glEnable(GL.GL_CLIP_PLANE2);
      gl.glEnable(GL.GL_CLIP_PLANE3);
      gl.glEnable(GL.GL_CLIP_PLANE4);
      gl.glEnable(GL.GL_CLIP_PLANE5);
      gl.glLoadIdentity();
    }

    gl.glPushMatrix();
    // FIXME
    //      camera.apply_inverse_transform();
    //      light.apply_transform();
    gl.glMultMatrixf(getData(lightManipXform), 0);
    gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, getData(light_position), 0);
    gl.glPopMatrix();
    gl.glEnable(GL.GL_LIGHT0);

    // FIXME
    gl.glPushMatrix();
    //      gl.glLoadIdentity();
    //      camera.apply_inverse_transform();

    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

    ManipManager.getManipManager().updateCameraParameters(drawable, viewer.getCameraParameters());
    ManipManager.getManipManager().render(drawable, gl);

    if (!b['R']) {
      drawRoom(gl, false);
    }

    if (!b['m']) {
      for (int i = 0; i < num_models; i++)
        if (m[i].draw)
          drawModel(gl, i, false);
    }

    if (b['X']) {
      gl.glDisable(GL.GL_CLIP_PLANE0);
      gl.glDisable(GL.GL_CLIP_PLANE1);
      gl.glDisable(GL.GL_CLIP_PLANE2);
      gl.glDisable(GL.GL_CLIP_PLANE3);
      gl.glDisable(GL.GL_CLIP_PLANE4);
      gl.glDisable(GL.GL_CLIP_PLANE5);
    }

    if (!b['s']) {
      for (int i = 0; i < num_models; i++)
        if (m[i].draw)
          drawShadowVolumeToStencil(gl, i);
    }

    // Be aware that this can cause some multipass artifacts
    // due to invariance issues.
    if (b['X']) {
      gl.glEnable(GL.GL_CLIP_PLANE0);
      gl.glEnable(GL.GL_CLIP_PLANE1);
      gl.glEnable(GL.GL_CLIP_PLANE2);
      gl.glEnable(GL.GL_CLIP_PLANE3);
      gl.glEnable(GL.GL_CLIP_PLANE4);
      gl.glEnable(GL.GL_CLIP_PLANE5);
    }
    if (!b['d']) {
      if (!b['R'])
        drawRoom(gl, true);
      if (!b['m'])
        for (int i = 0; i < num_models; i++)
          if (m[i].draw)
            drawModel(gl, i, true);
    }

    if(!b['S']) {
      for (int i = 0; i < num_models; i++)
        if (m[i].draw)
          drawPossibleSilhouette(gl, i);
    }

    if (!b['v']) {
      for (int i = 0; i < num_models; i++)
        if (m[i].draw)
          drawShadowVolumeToColor(gl, i);
    }

    // Be aware that this can cause some multipass artifacts
    // due to invariance issues.
    if (b['X']) {
      gl.glDisable(GL.GL_CLIP_PLANE0);
      gl.glDisable(GL.GL_CLIP_PLANE1);
      gl.glDisable(GL.GL_CLIP_PLANE2);
      gl.glDisable(GL.GL_CLIP_PLANE3);
      gl.glDisable(GL.GL_CLIP_PLANE4);
      gl.glDisable(GL.GL_CLIP_PLANE5);
    }

    drawLight(gl);

    gl.glPopMatrix();

    // In an "external" viewing mode, show the camera's view volume
    // as a yellow wireframe cube or frustum.
    if (curr_view != CAMERA_VIEW) {
      gl.glPushMatrix();
      if (b['I']) {
        // FIXME
        applyInfinitePerspectiveInverse(gl, viewer);
      } else {
        // FIXME
        //          reshaper.apply_projection_inverse();
      }
      gl.glColor3f(.75f,.75f,0);
      gl.glLineWidth(3);
      glut.glutWireCube(2);
      gl.glLineWidth(1);
      gl.glPopMatrix();
    }

    if (b[' ']) {
      // Animating continually. Schedule another repaint soon.
      demoListener.repaint();
    }
  }

  // Unused routines
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  private void dispatchKey(char k) {
    b[k] = ! b[k];
    if (k==27 || k=='q') {
      shutdownDemo();
      return;
    }

    if(';' == k) {
      volume_alpha *= 1.1f;
    }
    if(':' == k) {
      volume_alpha /= 1.1f;
    }

    if('\'' == k) {
      room_ambient += .025f;
    }
    if('"' == k) {
      room_ambient -= .025f;
    }

    if(',' == k) {
      curr_model++;
      curr_model %= num_models;
      // FIXME
      //        key('2',0,0);
    }
    if('.' == k) {
      m[curr_model].draw = ! m[curr_model].draw;
    }
    if('w' == k) {
      toggleWireframe = true;
    }
    if('1' == k) {
      // FIXME
      /*
        curr_manip = 1;
        camera.disable();
        clipcam.disable();
        scenecam.disable();
        if(curr_view == 0)
        camera.enable();
        else if(curr_view == 1)
        scenecam.enable();
        else
        clipcam.enable();
        for(int i=0; i < num_models; i++)
        object[i].disable();
        light.disable();
        room.disable();
      */
    }
    if('2' == k) {
      // FIXME
      /*
        curr_manip = 2;
        camera.disable();
        clipcam.disable();
        scenecam.disable();
        light.disable();
        for(int i=0; i < num_models; i++)
        object[i].disable();
        object[curr_model].enable();
        room.disable();
      */
    }
    if('3' == k) {
      // FIXME
      /*
        curr_manip = 3;
        camera.disable();
        clipcam.disable();
        scenecam.disable();
        light.enable();
        for(int i=0; i < num_models; i++)
        object[i].disable();
        room.disable();
      */
    }
    if('4' == k) {
      // FIXME
      /*
        curr_manip = 4;
        camera.disable();
        clipcam.disable();
        scenecam.disable();
        light.disable();
        for(int i=0; i < num_models; i++)
        object[i].disable();
        room.enable();
      */
    }

    if('5' == k) {
      // FIXME
      /*
        curr_view = 0;
        if(curr_manip == 1)
        key('1',0,0);
      */
    }

    if('6' == k) {
      // FIXME
      /*
        curr_view = 1;
        if(curr_manip == 1)
        key('1',0,0);
      */
    }

    if('7' == k) {
      // FIXME
      /*
        curr_view = 2;
        if(curr_manip == 1)
        key('1',0,0);
      */
    }

    if('[' == k) {
      // FIXME: correct?
      viewer.setZNear(viewer.getZNear() / 2);
      //        reshaper.zNear /= 2;
    }
    if(']' == k) {
      // FIXME: correct?
      viewer.setZNear(viewer.getZNear() * 2);
      //        reshaper.zNear *= 2;
    }

    if('{' == k) {
      // FIXME: correct?
      viewer.setZFar(viewer.getZFar() / 2);
      //        reshaper.zFar /= 2;
    }
    if('}' == k) {
      // FIXME: correct?
      viewer.setZFar(viewer.getZFar() * 2);
      //        reshaper.zFar *= 2;
    }

    if('!' == k) {
      enableDepthClampNV = true;
      toggleDepthClampNV = true;
    }
    if('~' == k) {
      enableDepthClampNV = false;
      toggleDepthClampNV = true;
    }

    if('a' == k) {
      animateForward = true;
    }

    if('b' == k) {
      animateBackward = true;
    }

    if('.' == k) {
      hideCurrentModel = true;
    }

    if('n' == k) {
      light_object_scale *= 1.1f;
    }
    if('N' == k) {
      light_object_scale /= 1.1f;
    }

    if('L' == k) {
      if(b[k])
        light_position.set(0,0,0,1);
      else
        light_position.set(0.25f, 0.25f, 1, 0);
    }

    if ('c' == k) {
      doViewAll = true;
    }
  }

  private void initModel() {
    int i = 0;

    try {
      MD2.Model mod = MD2.loadMD2(getClass().getClassLoader().getResourceAsStream("demos/data/models/knight.md2"));
      m[i] = new Model();
      m[i].mod = mod;
      m[i].interp_frame = (MD2.Frame) m[i].mod.f[0].clone();
      m[i].ambient.componentMul(m[i].diffuse);
      i++;
    } catch (IOException e) {
      e.printStackTrace();
    }

    num_models = i;
  }

  // interpolate between keyframes
  private void interpolate_frame() {
    float frac =  m[curr_model].frame_num - (float) Math.floor(m[curr_model].frame_num);
    int f0_index = (int) Math.floor(m[curr_model].frame_num);
    int f1_index = ((int) Math.ceil(m[curr_model].frame_num)) % m[curr_model].mod.f.length;
    MD2.Frame f0 = m[curr_model].mod.f[f0_index];
    MD2.Frame f1 = m[curr_model].mod.f[f1_index];

    for (int i = 0; i < f0.pn.length; i++) {
      MD2.PositionNormal pn  = m[curr_model].interp_frame.pn[i];
      MD2.PositionNormal pn0 = f0.pn[i];
      MD2.PositionNormal pn1 = f1.pn[i];

      pn.x = (1-frac) * pn0.x + frac * pn1.x;
      pn.y = (1-frac) * pn0.y + frac * pn1.y;
      pn.z = (1-frac) * pn0.z + frac * pn1.z;
      pn.nx = (1-frac) * pn0.nx + frac * pn1.nx;
      pn.ny = (1-frac) * pn0.ny + frac * pn1.ny;
      pn.nz = (1-frac) * pn0.nz + frac * pn1.nz;
    }
    
    for (int i = 0; i < f0.triplane.length; i++) {
      MD2.Plane p = m[curr_model].interp_frame.triplane[i];

      MD2.computePlane(m[curr_model].interp_frame.pn[m[curr_model].mod.tri[i].v[0].pn_index], 
                       m[curr_model].interp_frame.pn[m[curr_model].mod.tri[i].v[1].pn_index],
                       m[curr_model].interp_frame.pn[m[curr_model].mod.tri[i].v[2].pn_index],
                       p); 
    }
  }

  // This routine draws the end caps (both local and infinite) for an
  // occluder.  These caps are required for the zfail approach to work.
  private void drawShadowVolumeEndCaps(GL gl, int mindex) {
    Vec4f olight = new Vec4f();

    Mat4f ml = new Mat4f(objectManipXform);
    ml.invertRigid();
    ml = ml.mul(lightManipXform);
    ml.xformVec(light_position, olight);

    MD2.PositionNormal[] vpn = m[mindex].interp_frame.pn;

    gl.glPushMatrix();
    gl.glMultMatrixf(getData(objectManipXform), 0);
    gl.glBegin(GL.GL_TRIANGLES);
    for (int i = 0; i < m[mindex].mod.tri.length; i++) {
      if (m[mindex].mod.tri[i].kill)
        continue;
      MD2.Plane p = m[mindex].interp_frame.triplane[i];

      boolean facing_light  = (( p.a * olight.get(0) + 
                                 p.b * olight.get(1) +
                                 p.c * olight.get(2) +
                                 p.d * olight.get(3) ) >= 0 );

      for (int j = 0; j < 3; j++) {
        MD2.PositionNormal pn = vpn[m[mindex].mod.tri[i].v[j].pn_index];
        if (facing_light)  // draw locally
          gl.glVertex4f(pn.x, pn.y, pn.z, 1);
        else              // draw at infinity
          gl.glVertex4f(pn.x*olight.get(3) - olight.get(0),
                        pn.y*olight.get(3) - olight.get(1),
                        pn.z*olight.get(3) - olight.get(2),
                        0);
      }
    }
    gl.glEnd();
    gl.glPopMatrix();
  }

  private void drawModel(GL gl, int mindex, boolean do_diffuse) {
    MD2.PositionNormal[] vpn = m[mindex].interp_frame.pn;

    float[] zero = new float[] {  0,  0,  0,  0};
    float[] dim  = new float[] {.2f,.2f,.2f,.2f};
    float[] diffuse = new float[4];
    float[] specular = new float[4];
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, getData(m[mindex].ambient), 0);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, getData(m[mindex].diffuse), 0);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, getData(m[mindex].specular), 0);
    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, m[mindex].shininess);
    if (!do_diffuse) {
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuse, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, dim, 0);
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, zero, 0);
    } else {
      gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
      gl.glEnable(GL.GL_BLEND);
      gl.glStencilFunc(GL.GL_EQUAL, 128, ~0);
      gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
      gl.glEnable(GL.GL_STENCIL_TEST);
      gl.glDepthFunc(GL.GL_EQUAL);
    }
    gl.glPushMatrix();
    gl.glMultMatrixf(getData(objectManipXform), 0);
    gl.glEnable(GL.GL_LIGHTING);

    gl.glPolygonOffset(0,-2);
    gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

    gl.glBegin(GL.GL_TRIANGLES);
    {
      for (int i = 0; i < m[mindex].mod.tri.length; i++) {
        for(int j=0; j < 3; j++) {
          MD2.PositionNormal pn = vpn[m[mindex].mod.tri[i].v[j].pn_index];
          gl.glNormal3f(pn.nx, pn.ny, pn.nz);
          gl.glVertex4f(pn.x, pn.y, pn.z, 1);
        }
      }
    }
    gl.glEnd();

    gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);

    gl.glDisable(GL.GL_LIGHTING);
    gl.glPopMatrix();
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE,  new float[] { 0.8f, 0.8f, 0.8f, 1}, 0);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, new float[] { 0.3f, 0.3f, 0.3f, 1}, 0);

    if (!do_diffuse) {
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuse, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
    } else {
      gl.glDisable(GL.GL_BLEND);
      //glDisable(GL.GL_STENCIL_TEST);
      gl.glStencilFunc(GL.GL_ALWAYS, 128, ~0);
      gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

      gl.glDepthFunc(GL.GL_LESS);
    }
  }

  // This is for drawing the walls of the room.
  private void drawMesh(GL gl, float size, int tess) {
    float hsize = size/2;
    float delta = size/(tess-1);

    gl.glPushMatrix();
    gl.glTranslatef(-hsize, -hsize, hsize);
    
    gl.glNormal3f(0,0,-1);

    float x = 0;
    for(int i=0; i < tess-1; i++) {
      float y = 0;
      gl.glBegin(GL.GL_QUAD_STRIP);
      for(int j=0; j < tess; j++) {
        gl.glTexCoord2f(      x, y);
        gl.glVertex2f  (      x, y);
        gl.glTexCoord2f(x+delta, y);
        gl.glVertex2f  (x+delta, y);
        y += delta;
      }
      gl.glEnd();
      x += delta;
    }
    gl.glPopMatrix();
  }

  private void drawCube(GL gl) {
    gl.glBindTexture(GL.GL_TEXTURE_2D, wallTexObject);
    gl.glEnable(GL.GL_TEXTURE_2D);
    gl.glPushMatrix();
    // FIXME
    //      room.apply_transform();
    gl.glCallList(faceDisplayList);
    gl.glRotatef(90, 1, 0, 0);
    gl.glCallList(faceDisplayList);
    gl.glRotatef(90, 1, 0, 0);
    gl.glCallList(faceDisplayList);
    gl.glRotatef(90, 1, 0, 0);
    gl.glCallList(faceDisplayList);
    gl.glRotatef(90, 1, 0, 0);
    gl.glRotatef(90, 0, 1, 0);
    gl.glCallList(faceDisplayList);
    gl.glRotatef(180, 0, 1, 0);
    gl.glCallList(faceDisplayList);
    gl.glPopMatrix();
    gl.glDisable(GL.GL_TEXTURE_2D);
  }

  private void drawRoom(GL gl, boolean do_diffuse) {
    float[] zero = new float[] {0,0,0,0};
    float[] a = new float[4];
    a[0] = room_ambient;
    a[1] = room_ambient;
    a[2] = room_ambient;
    a[3] = 1;

    float[] d1 = new float[] {.1f,.1f,.1f,.1f};
    float[] d2 = new float[] {.7f,.7f,.7f,.7f};
    float[] s  = new float[] {.7f,.7f,.7f,.7f};
    float[] emission = new float[4];
    float[] ambient  = new float[4];
    float[] diffuse  = new float[4];
    float[] specular = new float[4];

    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, a, 0);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE,  new float[] {0.8f, 0.8f, 0.8f, 1}, 0);
    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, new float[] {0.4f, 0.4f, 0.4f, 1}, 0);
    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 64.0f);

    if (!do_diffuse) {
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuse, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, d1, 0);
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, zero, 0);
      gl.glStencilFunc(GL.GL_ALWAYS, 128, ~0);
    } else {
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_EMISSION, emission, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_EMISSION, zero, 0);
      gl.glGetLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, ambient, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, zero, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, d2, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, s, 0);

      gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
      gl.glEnable(GL.GL_BLEND);
      gl.glStencilFunc(GL.GL_EQUAL, 128, ~0);
      gl.glDepthFunc(GL.GL_EQUAL);
    }
    gl.glPushMatrix();
    gl.glTranslatef(0,9,0);
    gl.glEnable(GL.GL_LIGHTING);
    gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
    gl.glEnable(GL.GL_STENCIL_TEST);

    drawCube(gl);

    gl.glStencilFunc(GL.GL_ALWAYS, 128, ~0);
    gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

    gl.glDisable(GL.GL_LIGHTING);
    gl.glPopMatrix();
    
    if (!do_diffuse) {
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuse, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specular, 0);
    } else {
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_EMISSION, emission, 0);
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, ambient, 0);

      gl.glDisable(GL.GL_BLEND);
      gl.glDepthFunc(GL.GL_LESS);
    }
  }
    
  // This routine draws the extruded "possible silhouette" edge.  The
  // edge is extruded to infinity.

  // The paper describes identifying silhouette edge loops.  The approach
  // in this demo is to visit each edge, determine if it's a "possible silhouette"
  // or not, and if it is, draw the extruded edge.   This approach is not
  // as efficient, but it has the benefit of being extremely simple.

  // This routine also doubles as the routine for drawing the local and ininite
  // silhouette edges (when prim == GL_LINES).
  private void drawShadowVolumeEdges(GL gl,
                                     int mindex,
                                     int prim,
                                     boolean local,
                                     boolean infinity) {
    Vec4f olight = new Vec4f();

    Mat4f ml = new Mat4f(objectManipXform);
    ml.invertRigid();
    ml = ml.mul(lightManipXform);
    ml.xformVec(light_position, olight);

    gl.glPushMatrix();
    gl.glMultMatrixf(getData(objectManipXform), 0);

    MD2.Frame f = m[mindex].interp_frame;

    gl.glBegin(prim);
    for (int i = 0; i < m[mindex].mod.edge.length; i++) {
      MD2.WingedEdge we = m[mindex].mod.edge[i];
      if (we.w[0] == -1 || m[mindex].mod.tri[we.w[0]].kill ||
          we.w[1] == -1 || m[mindex].mod.tri[we.w[1]].kill )
        continue;

      MD2.Plane p0 = f.triplane[we.w[0]];
      float f0 = ( p0.a * olight.get(0) + 
                   p0.b * olight.get(1) +
                   p0.c * olight.get(2) +
                   p0.d * olight.get(3) );
            
      float f1 = -f0;
      if(we.w[1] != -1) {
        MD2.Plane p1 = f.triplane[we.w[1]];

        f1 = ( p1.a * olight.get(0) + 
               p1.b * olight.get(1) +
               p1.c * olight.get(2) +
               p1.d * olight.get(3) );
      }

      int[] edge = new int[2];

      if(f0 >= 0 && f1 < 0) {
        edge[0] = we.e[1];
        edge[1] = we.e[0];
      } else if(f1 >= 0 && f0 < 0) {
        edge[0] = we.e[0];
        edge[1] = we.e[1];
      } else {
        continue;
      }
        
      MD2.PositionNormal pn0 = f.pn[edge[0]];
      MD2.PositionNormal pn1 = f.pn[edge[1]];

      if(prim == GL.GL_QUADS || local) {
        // local segment
        gl.glVertex4f(pn0.x, pn0.y, pn0.z, 1);
        gl.glVertex4f(pn1.x, pn1.y, pn1.z, 1);
      }
      if(prim == GL.GL_QUADS || infinity) {
        // segment projected to infinity
        gl.glVertex4f(pn1.x*olight.get(3) - olight.get(0),
                      pn1.y*olight.get(3) - olight.get(1),
                      pn1.z*olight.get(3) - olight.get(2),
                      0);
        gl.glVertex4f(pn0.x*olight.get(3) - olight.get(0),
                      pn0.y*olight.get(3) - olight.get(1),
                      pn0.z*olight.get(3) - olight.get(2),
                      0);
      }
    }
    gl.glEnd();
    gl.glPopMatrix();
  }

  private void drawShadowVolumeExtrudedEdges(GL gl, int mindex) {
    drawShadowVolumeEdges(gl, mindex, GL.GL_QUADS, true, true);
  }

  private void drawPossibleSilhouette(GL gl, int mindex) {
    gl.glLineWidth(3);
    gl.glColor3f(1,1,1);
    drawShadowVolumeEdges(gl, mindex, GL.GL_LINES, true, !b['-']);
    gl.glLineWidth(1);
  }

  // Draw the shadow volume into the stencil buffer.
  private void drawShadowVolumeToStencil(GL gl, int mindex) {
    gl.glDepthFunc(GL.GL_LESS);
    gl.glDepthMask(false);

    gl.glStencilFunc(GL.GL_ALWAYS, 128, ~0);
    gl.glEnable(GL.GL_STENCIL_TEST);

    gl.glEnable(GL.GL_CULL_FACE);
    gl.glCullFace(GL.GL_FRONT);
    gl.glStencilOp(GL.GL_KEEP, GL.GL_INCR, GL.GL_KEEP);
    gl.glColorMask(false, false, false, false);

    drawShadowVolumeExtrudedEdges(gl, mindex);
    drawShadowVolumeEndCaps(gl, mindex);

    gl.glCullFace(GL.GL_BACK);
    gl.glStencilOp(GL.GL_KEEP, GL.GL_DECR, GL.GL_KEEP);

    drawShadowVolumeExtrudedEdges(gl, mindex);
    drawShadowVolumeEndCaps(gl, mindex);

    gl.glColorMask(true, true, true, true);
    gl.glDisable(GL.GL_CULL_FACE);

    gl.glStencilFunc(GL.GL_ALWAYS, 128, ~0);
    gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

    gl.glDepthMask(true);
    gl.glDepthFunc(GL.GL_LESS);
  }

  // Draw the shadow volume into the color buffer.
  private void drawShadowVolumeToColor(GL gl, int mindex) {
    gl.glDepthFunc(GL.GL_LESS);
    gl.glDepthMask(false);

    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

    gl.glColor4f(1,1,1,.7f * volume_alpha);
    drawShadowVolumeEndCaps(gl, mindex);
    gl.glColor4f(1,1,.7f,.15f * volume_alpha);
    drawShadowVolumeExtrudedEdges(gl, mindex);

    gl.glDepthMask(true);
    gl.glDepthFunc(GL.GL_LESS);
    gl.glDisable(GL.GL_BLEND);
  }

  // Draw an icon to show where the local light is
  // or in what direction the infinite light is pointing. 
  private void drawLight(GL gl) {
    gl.glColor3f(1,1,0);
    gl.glPushMatrix();
    gl.glMultMatrixf(getData(lightManipXform), 0);
    gl.glScalef(light_object_scale, light_object_scale, light_object_scale);
    if (b['L']) {
      glut.glutSolidSphere(.01f, 20, 10);
    } else {
      Vec3f ldir = new Vec3f(light_position.get(0),
                             light_position.get(1),
                             light_position.get(2));
      Rotf r = new Rotf(new Vec3f(0,0,1), ldir);
      Mat4f m = new Mat4f();
      m.makeIdent();
      m.setRotation(r);
      m = m.mul(perspectiveInverse(30, 1, 0.001f, 0.04f));
      gl.glRotatef(180, 1, 0, 0);
      gl.glTranslatef(0,0,-0.02f);
      gl.glMultMatrixf(getData(m), 0);
      glut.glutSolidCube(2);
    }
    gl.glPopMatrix();
  }

  // The infinite frustum set-up code.
  private Mat4f infiniteFrustum(float left, float right,
                                float bottom, float top,
                                float zNear) {
    Mat4f m = new Mat4f();
    m.makeIdent();
	
    m.set(0,0, (2*zNear) / (right - left));
    m.set(0,2, (right + left) / (right - left));
	
    m.set(1,1, (2*zNear) / (top - bottom));
    m.set(1,2, (top + bottom) / (top - bottom));
	
    // nudge infinity in just slightly for lsb slop
    float nudge = 1 - 1.0f / (1<<23);

    m.set(2,2, -1  * nudge);
    m.set(2,3, -2*zNear * nudge);
	
    m.set(3,2, -1);
    m.set(3,3, 0);
	
    m.transpose();
      
    return m;
  }

  private Mat4f infiniteFrustumInverse(float left, float right,
                                       float bottom, float top,
                                       float zNear) {
    Mat4f m = new Mat4f();
    m.makeIdent();
	
    m.set(0,0, (right - left) / (2 * zNear));
    m.set(0,3, (right + left) / (2 * zNear));
	
    m.set(1,1, (top - bottom) / (2 * zNear));
    m.set(1,3, (top + bottom) / (2 * zNear));
	
    m.set(2,2, 0);
    m.set(2,3, -1);
	
    m.set(3,2, -1 / (2 * zNear));
    m.set(3,3, 1 / (2 * zNear));
	
    return m;
  }

  private Mat4f infinitePerspective(float fovy, float aspect, float zNear) {
    float tangent = (float) Math.tan(fovy / 2.0);
    float y = tangent * zNear;
    float x = aspect * y;
    return infiniteFrustum(-x, x, -y, y, zNear);
  }

  private Mat4f infinitePerspectiveInverse(float fovy, float aspect, float zNear) {
    float tangent = (float) Math.tan(fovy / 2.0);
    float y = tangent * zNear;
    float x = aspect * y;
    return infiniteFrustumInverse(-x, x, -y, y, zNear);
  }

  private void applyInfinitePerspective(GL gl, ExaminerViewer v) {
    CameraParameters parms = v.getCameraParameters();
    float aspect = parms.getImagePlaneAspectRatio();
    gl.glMultMatrixf(getData(infinitePerspective(parms.getVertFOV(), aspect, v.getZNear())), 0);
  }

  private void applyInfinitePerspectiveInverse(GL gl, ExaminerViewer v) {
    CameraParameters parms = v.getCameraParameters();
    float aspect = parms.getImagePlaneAspectRatio();
    gl.glMultMatrixf(getData(infinitePerspectiveInverse(parms.getVertFOV(), aspect, v.getZNear())), 0);
  }

  private Mat4f perspectiveInverse(float fovy, float aspect, float zNear, float zFar) {
    float tangent = (float) Math.tan(Math.toRadians(fovy / 2.0));
    float y = tangent * zNear;
    float x = aspect * y;
    return frustumInverse(-x, x, -y, y, zNear, zFar);
  }

  private Mat4f frustumInverse(float left, float right,
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

  private float[] getData(Vec4f v) {
    return new float[] { v.x(), v.y(), v.z(), v.w() };
  }

  private float[] getData(Mat4f m) {
    float[] res = new float[16];
    m.getColumnMajorData(res);
    return res;
  }

  private static void runExit() {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Instead run
    // the exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          System.exit(0);
        }
      }).start();
  }
}
