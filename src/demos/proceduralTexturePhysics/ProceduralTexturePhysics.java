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

package demos.proceduralTexturePhysics;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import demos.common.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/**
 * Water demonstration by NVidia Corporation - <a href =
 * "http://developer.nvidia.com/view.asp?IO=ogl_dynamic_bumpreflection">http://developer.nvidia.com/view.asp?IO=ogl_dynamic_bumpreflection</a>
 *
 * <P>
 *
 * Demonstrates pbuffers, vertex programs, fragment programs
 *
 * <P>
 *
 * Ported to Java and ARB_fragment_program by Kenneth Russell
 *
 */

public class ProceduralTexturePhysics extends Demo {
  public static void main(String[] args) {
    GLCanvas canvas = new GLCanvas();
    ProceduralTexturePhysics demo = new ProceduralTexturePhysics();
    canvas.addGLEventListener(demo);

    final Animator animator = new Animator(canvas);
    demo.setDemoListener(new DemoListener() {
        public void shutdownDemo() {
          runExit(animator);
        }
        public void repaint() {}
      });

    Frame frame = new Frame("Procedural Texture Waves");
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

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public void shutdownDemo() {
    viewer.detach();
    ManipManager.getManipManager().unregisterWindow(drawable);
    drawable.removeGLEventListener(this);
    super.shutdownDemo();
  }

  private volatile boolean drawing;
  private volatile int     mousePosX;
  private volatile int     mousePosY;

  private GLAutoDrawable drawable;
  private Water    water = new Water();
  private volatile ExaminerViewer viewer;
  private boolean[] b = new boolean[256];
  private boolean  doViewAll = true;
  private float    zNear = 0.1f;
  private float    zFar  = 10.0f;

  private DurationTimer timer = new DurationTimer();
  private boolean  firstRender = true;
  private int      frameCount;

  private float blurIncrement         = 0.01f;
  private float bumpIncrement         = 0.01f;
  private float frequencyIncrement    = 0.1f;

  public void init(GLAutoDrawable drawable) {
    water.destroy();
    water.initialize("demos/data/images/nvfixed.tga", 
                     "demos/data/images/nvspin.tga", 
                     "demos/data/images/droplet.tga", 
                     "demos/data/cubemaps/CloudyHills_",
                     "tga",
                     drawable);

    GL gl = drawable.getGL();
    gl.setSwapInterval(1);

    try {
      checkExtension(gl, "GL_VERSION_1_3"); // For multitexture
      checkExtension(gl, "GL_ARB_vertex_program");
      checkExtension(gl, "GL_ARB_fragment_program");
      checkExtension(gl, "GL_ARB_pbuffer");
      checkExtension(gl, "GL_ARB_pixel_format");
    } catch (GLException e) {
      e.printStackTrace();
      throw(e);
    }
      
    gl.glClearColor(0, 0.2f, 0.5f, 0);
    gl.glDisable(GL.GL_LIGHTING);
    gl.glDisable(GL.GL_DEPTH_TEST);
    gl.glDisable(GL.GL_CULL_FACE);

    doViewAll = true;

    if (firstRender) {
      firstRender = false;

      // Register the window with the ManipManager
      ManipManager manager = ManipManager.getManipManager();
      manager.registerWindow(drawable);
      this.drawable = drawable;

      viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
      viewer.setAutoRedrawMode(false);
      viewer.attach(drawable, new BSphereProvider() {
          public BSphere getBoundingSphere() {
            return new BSphere(new Vec3f(0, 0, 0), 1.2f);
          }
        });
      viewer.setVertFOV((float) (15.0f * Math.PI / 32.0f));
      viewer.setZNear(zNear);
      viewer.setZFar(zFar);

      drawable.addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            dispatchKey(e.getKeyChar());
          }
        });

      drawable.addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 &&
                !e.isAltDown() && !e.isMetaDown()) {
              drawing = true;
            }
          }

          public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
              drawing = false;
            }
          }
        });

      drawable.addMouseMotionListener(new MouseMotionAdapter() {
          public void mouseDragged(MouseEvent e) {
            mousePosX = e.getX();
            mousePosY = e.getY();
          }            
        });

      timer.start();
    }
  }

  public void display(GLAutoDrawable drawable) {
    if (++frameCount == 30) {
      timer.stop();
      System.err.println("Frames per second: " + (30.0f / timer.getDurationAsSeconds()));
      timer.reset();
      timer.start();
      frameCount = 0;
    }

    GL gl = drawable.getGL();

    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    if (doViewAll) {
      viewer.viewAll(gl);
      doViewAll = false;
    }

    viewer.update(gl);
    ManipManager.getManipManager().updateCameraParameters(drawable, viewer.getCameraParameters());
    ManipManager.getManipManager().render(drawable, gl);

    if (drawing) {
      int w = drawable.getWidth();
      int h = drawable.getHeight();
      water.addDroplet(new Water.Droplet( 2 * (mousePosX / (float) w  - 0.5f),
                                          -2 * (mousePosY / (float) h - 0.5f),
                                          0.08f));
    }
    water.tick();

    CameraParameters params = viewer.getCameraParameters();
    water.draw(gl, params.getOrientation().inverse());
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}

  // Unused routines
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  private void setFlag(char key, boolean val) {
    b[((int) key) & 0xFF] = val;
  }

  private boolean getFlag(char key) {
    return b[((int) key) & 0xFF];
  }

  private void checkExtension(GL gl, String extensionName) {
    if (!gl.isExtensionAvailable(extensionName)) {
      String message = "Unable to initialize " + extensionName + " OpenGL extension";
      JOptionPane.showMessageDialog(null, message, "Unavailable extension", JOptionPane.ERROR_MESSAGE);
      shutdownDemo();
    }
  }

  private void dispatchKey(char k) {
    setFlag(k, !getFlag(k));

    switch (k) {
    case 27:
    case 'q':
      shutdownDemo();
      break;
    case 'w':
      water.enableWireframe(getFlag('w'));
      break;
    case 'd':
      // FIXME
      /*

      if (getKey('d')) {
      glutMouseFunc(glh::glut_mouse_function);
      glutMotionFunc(glh::glut_motion_function);
      }
      else
      {
      glutMouseFunc(Mouse);
      glutMotionFunc(Motion);
      }
      */
      break;
    case ' ':
      water.enableAnimation(getFlag(' '));
      break;
    case 'b':
      water.enableBorderWrapping(getFlag('b'));
      break;
    case 'n':
      water.singleStep();
      break;
    case 's':
      water.enableSlowAnimation(getFlag('s'));
      break;
    case '1':
      water.setRenderMode(Water.CA_FULLSCREEN_REFLECT);
      break;
    case '2':
      water.setRenderMode(Water.CA_FULLSCREEN_HEIGHT);
      break;
    case '3':
      water.setRenderMode(Water.CA_FULLSCREEN_FORCE);
      break;
    case '4':
      water.setRenderMode(Water.CA_FULLSCREEN_NORMALMAP);
      break;
    case '5':
      water.setRenderMode(Water.CA_TILED_THREE_WINDOWS);
      break;
    case 'r':
      water.reset();
      break;    
    case 'i':
      // FIXME: make sure this is what this does
      doViewAll = true;
      //          gluPerspective(90, 1, .01, 10);
      break;
    case 'c': {
      float dist = water.getBlurDistance();
      if (dist > blurIncrement)
        water.setBlurDistance(dist - blurIncrement);
      break;
    }
    case 'v': {
      float dist = water.getBlurDistance();
      if (dist < 1)
        water.setBlurDistance(dist + blurIncrement);
      break;
    }
    case '-': {
      float scale = water.getBumpScale();
      if (scale > -1)
        water.setBumpScale(scale - bumpIncrement);
      break;
    }
    case '=': {
      float scale = water.getBumpScale();
      if (scale < 1)
        water.setBumpScale(scale + bumpIncrement);
      break;
    }
    case 'l':
      water.enableBoundaryApplication(getFlag('l'));
      break;
    case 'o':
      water.enableSpinningLogo(getFlag('o'));
      break;
    case '.': {
      float frequency = water.getBumpScale();
      if (frequency < 1)
        water.setDropFrequency(frequency + frequencyIncrement);
      break;
    }
    case ',': {
      float frequency = water.getBumpScale();
      if (frequency > 0)
        water.setDropFrequency(frequency - frequencyIncrement);
      break;
    }
    default:
      break;
    }
  }

  private static void runExit(final Animator animator) {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Run the
    // exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          animator.stop();
          System.exit(0);
        }
      }).start();
  }
}
