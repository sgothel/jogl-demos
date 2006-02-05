/*
 * gleem -- OpenGL Extremely Easy-To-Use Manipulators.
 * Copyright (C) 1998-2003 Kenneth B. Russell (kbrussel@alum.mit.edu)
 *
 * Copying, distribution and use of this software in source and binary
 * forms, with or without modification, is permitted provided that the
 * following conditions are met:
 *
 * Distributions of source code must reproduce the copyright notice,
 * this list of conditions and the following disclaimer in the source
 * code header files; and Distributions of binary code must reproduce
 * the copyright notice, this list of conditions and the following
 * disclaimer in the documentation, Read me file, license file and/or
 * other materials provided with the software distribution.
 *
 * The names of Sun Microsystems, Inc. ("Sun") and/or the copyright
 * holder may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT A WARRANTY OF ANY
 * KIND. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INTERFERENCE, ACCURACY OF
 * INFORMATIONAL CONTENT OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. THE
 * COPYRIGHT HOLDER, SUN AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL THE
 * COPYRIGHT HOLDER, SUN OR SUN'S LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES. YOU ACKNOWLEDGE THAT THIS SOFTWARE IS NOT
 * DESIGNED, LICENSED OR INTENDED FOR USE IN THE DESIGN, CONSTRUCTION,
 * OPERATION OR MAINTENANCE OF ANY NUCLEAR FACILITY. THE COPYRIGHT
 * HOLDER, SUN AND SUN'S LICENSORS DISCLAIM ANY EXPRESS OR IMPLIED
 * WARRANTY OF FITNESS FOR SUCH USES.
 */

package gleem;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

import gleem.linalg.*;
import javax.media.opengl.*;

/** <P> This is an application-level class, not part of the
    manipulator hierarchy. It is an example of how you might integrate
    gleem with another application which uses the mouse. </P>

    <P> For the given GLAutoDrawable, the ExaminerViewer takes over the
    setting of the view position. It passes along mouse events it is
    not interested in to the ManipManager's mouse routines. </P>

    <P> The ExaminerViewer's controls are similar to those of Open
    Inventor's Examiner Viewer. Alt + Left mouse button causes
    rotation about the focal point. Alt + Right mouse button causes
    translation parallel to the image plane. Alt + both mouse buttons,
    combined with up/down mouse motion, causes zooming out and in
    along the view vector. (On platforms with a "Meta" key, that key
    can be substituted in place of the Alt key.) The method
    <code>setNoAltKeyMode</code> can be used to cause the
    ExaminerViewer to take control of all mouse interactions in the
    window, avoiding the need to hold down the Alt key. </P>

    <P>NOTE: the current ExaminerViewer implementation assumes a
    minimum of two mouse buttons. For the Mac OS, the code needs to be
    adjusted to use e.g., the Control key as the "right" mouse
    button. </P> */

public class ExaminerViewer {
  private GLAutoDrawable window;
  /** Simple state machine for figuring out whether we are grabbing
      events */
  private boolean interactionUnderway;
  private boolean iOwnInteraction;

  private boolean noAltKeyMode;
  private boolean autoRedrawMode = true;

  /** Simple state machine for computing distance dragged */
  private boolean button1Down;
  private boolean button2Down;
  private int numMouseButtons;
  private int oldNumMouseButtons;
  private int lastX;
  private int lastY;

  /** Camera parameters */
  private float minFocalDist = 1.0f;
  private Vec3f dolly        = new Vec3f(0, 0, 10); // Amount we have "backed up" from focal point
  private Vec3f center       = new Vec3f(0, 0,  0); // Position of focal point in world coordinates
  private Rotf  orientation  = new Rotf();
  private float rotateSpeed       = 1.0f;
  private float minRotateSpeed    = 0.0001f;
  private float dollySpeed        = 2.0f;
  private float minDollySpeed     = 0.0001f;
  private float zNear             = 1.0f;
  private float zFar              = 100.0f;
  private float vertFOVScale      = 1.0f;
  private CameraParameters params = new CameraParameters();

  /** Our bounding sphere provider (for viewAll()) */
  private BSphereProvider provider;

  private MouseMotionAdapter mouseMotionListener = new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) {
        motionMethod(e, e.getX(), e.getY());
      }

      public void mouseMoved(MouseEvent e) {
        passiveMotionMethod(e);
      }
    };

  private MouseAdapter mouseListener = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        mouseMethod(e, e.getModifiers(), true, e.getX(), e.getY());
      }

      public void mouseReleased(MouseEvent e) {
        mouseMethod(e, e.getModifiers(), false, e.getX(), e.getY());
      }
    };

  private GLEventListener glListener = new GLEventListener() {
      public void init(GLAutoDrawable drawable) {}
      public void display(GLAutoDrawable drawable) {}
      public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        reshapeMethod(width, height);
      }
      public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
    };

  /** The constructor takes the number of mouse buttons on this system
      (couldn't figure out how to determine this internally) */
  public ExaminerViewer(int numMouseButtons) {
    this.numMouseButtons = numMouseButtons;
    oldNumMouseButtons = numMouseButtons;
  }

  /** <P> Attaches this ExaminerViewer to the given GLAutoDrawable. This
      causes the ManipManager's mouse routines to be removed from the
      window (using ManipManager.removeMouseListeners) and the
      ExaminerViewer's to be installed. The GLAutoDrawable should be
      registered with the ManipManager before the ExaminerViewer is
      attached to it. </P>

      <P> In order for the viewer to do anything useful, you need to
      provide a BSphereProvider to it to allow "view all"
      functionality. </P> */
  public void attach(GLAutoDrawable window, BSphereProvider provider) {
    this.window = window;
    this.provider = provider;
    init();
    setupListeners();
  }

  /** Detaches from the given window. This causes the ManipManager's
      mouse listeners to be reinstalled on the GLAutoDrawable and the
      ExaminerViewer's to be removed. */
  public void detach() {
    removeListeners();
    this.window = null;
    this.provider = null;
  }
  
  /** Call this at the end of your display() method to cause the
      Modelview matrix to be recomputed for the next frame. */
  public void update(GL gl) {
    recalc(gl);
  }

  /** Call this to apply the inverse rotation matrix of the camera to
      the current matrix. This is useful for drawing a skybox. Does
      not update which OpenGL matrix is currently being modified or
      the ExaminerViewer's camera parameters. */
  public void updateInverseRotation(GL gl) {
    recalcInverseRotation(gl);
  }

  /** Call this to force the ExaminerViewer to update its
      CameraParameters without touching the OpenGL state. */
  public void update() {
    recalc();
  }

  /** Call this from within your display() method to cause the
      ExaminerViewer to recompute its position based on the visible
      geometry. A BSphereProvider must have already been set or this
      method has no effect. */
  public void viewAll(GL gl) {
    if (provider == null) {
      return;
    }
    // Figure out how far to move
    float vertFOV, horizFOV, minFOV;
    float adjustedVertFOV = params.getVertFOV() * vertFOVScale;
    vertFOV = 2.0f * adjustedVertFOV;
    horizFOV = 2.0f * (float) Math.atan(params.getImagePlaneAspectRatio() *
                                        Math.tan(adjustedVertFOV));
    if (vertFOV < horizFOV)
      minFOV = vertFOV;
    else
      minFOV = horizFOV;
    if (minFOV == 0.0f) {
      throw new RuntimeException("Minimum field of view was zero");
    }
    BSphere bsph = provider.getBoundingSphere();
    float dist = bsph.getRadius() / (float) Math.sin(minFOV / 2.0f);
    dolly.setZ(dist);
    center.set(bsph.getCenter());
    recalc(gl);
  }

  /** Get the camera parameters out of this Examiner Viewer (for
      example, to pass to ManipManager.updateCameraParameters()). Note
      that mutating the returned object is not recommended but
      regardless will have no effect on the ExaminerViewer. */
  public CameraParameters getCameraParameters() {
    return params;
  }

  /** These routines can be hooked into a GUI by calling them from
      ActionEvent listeners for buttons elsewhere in the application. */
  public void rotateFaster() {
    rotateSpeed *= 2.0f;
  }

  public void rotateSlower() {
    if (rotateSpeed < minRotateSpeed)
      return;
    else
      rotateSpeed /= 2.0f;
  }

  public void dollyFaster() {
    dollySpeed *= 2.0f;
  }

  public void dollySlower() {
    if (dollySpeed < minDollySpeed)
      return;
    else
      dollySpeed /= 2.0f;
  }

  public float getZNear() {
    return zNear;
  }

  public void setZNear(float zNear) {
    this.zNear = zNear;
  }

  public float getZFar() {
    return zFar;
  }

  public void setZFar(float zFar) {
    this.zFar = zFar;
  }

  /** Takes HALF of the vertical angular span of the frustum,
      specified in radians. For example, if your <b>fovy</b> argument
      to gluPerspective() is 90, then this would be Math.PI / 4. Note
      that the ExaminerViewer's algorithms break down if the vertical
      field of view approaches or exceeds 180 degrees, or Math.PI /
      2. */
  public void setVertFOV(float vertFOV) {
    vertFOVScale = (float) (vertFOV / (Math.PI / 4));
  }

  /** Sets the position of this ExaminerViewer. */
  public void setPosition(Vec3f position) {
    Vec3f tmp = orientation.rotateVector(Vec3f.NEG_Z_AXIS);
    tmp.scale(dolly.z());
    center.add(position, tmp);
  }

  /** Sets the orientation of this ExaminerViewer. */
  public void setOrientation(Rotf orientation) {
    this.orientation.set(orientation);
  }

  public void setNoAltKeyMode(boolean noAltKeyMode) {
    this.noAltKeyMode = noAltKeyMode;
    if (noAltKeyMode) {
      // FIXME: this is a hack to work around Windows' apparently
      // conflating the alt/meta key with one of the mouse buttons
      oldNumMouseButtons = numMouseButtons;
      numMouseButtons = 3;
    } else {
      numMouseButtons = oldNumMouseButtons;
    }
  }

  public boolean getNoAltKeyMode() {
    return noAltKeyMode;
  }

  /** Enables or disables the automatic redrawing of the
      GLAutoDrawable to which this ExaminerViewer is attached. If the
      GLAutoDrawable is already being animated, disabling auto redraw
      mode may provide better performance. Defaults to on. */
  public void setAutoRedrawMode(boolean onOrOff) {
    autoRedrawMode = onOrOff;
  }

  /** Returns whether this ExaminerViewer automatically redraws the
      GLAutoDrawable to which it is attached upon updates. */
  public boolean getAutoRedrawMode() {
    return autoRedrawMode;
  }

  /** Rotates this ExaminerViewer about the focal point by the
      specified incremental rotation; performs postmultiplication,
      i.e. the incremental rotation is applied after the current
      orientation. */
  public void rotateAboutFocalPoint(Rotf rot) {
    orientation = rot.times(orientation);
    orientation.normalize();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static final float EPSILON = 0.0001f;

  private void setupListeners() {
    ManipManager.getManipManager().removeMouseListeners(window);
    window.addMouseMotionListener(mouseMotionListener);
    window.addMouseListener(mouseListener);
    window.addGLEventListener(glListener);
  }

  private void removeListeners() {
    if (window != null) {
      window.removeMouseMotionListener(mouseMotionListener);
      window.removeMouseListener(mouseListener);
      window.removeGLEventListener(glListener);
      ManipManager.getManipManager().setupMouseListeners(window);
    }
  }

  private void passiveMotionMethod(MouseEvent e) {
    ManipManager.getManipManager().mouseMoved(e);
  }

  private boolean modifiersMatch(MouseEvent e, int mods) {
    if (noAltKeyMode) {
      if ((mods & MouseEvent.BUTTON1_MASK) != 0 &&
          (mods & MouseEvent.BUTTON2_MASK) == 0 &&
          (mods & MouseEvent.BUTTON3_MASK) == 0) {
        return (!e.isAltDown() && !e.isMetaDown() && !e.isControlDown() && !e.isShiftDown());
      } else {
        // At least on Windows, meta seems to be declared to be down on right button presses
        return !e.isControlDown() && !e.isShiftDown();
      }
    } else {
      return ((e.isAltDown() || e.isMetaDown()) &&
              (!e.isControlDown() && !e.isShiftDown()));
    }
  }

  private void init() {
    interactionUnderway = false;
    iOwnInteraction = false;
    button1Down = false;
    button2Down = false;

    int xSize = window.getWidth();
    int ySize = window.getHeight();
    params.setOrientation(orientation);
    params.setPosition(computePosition(new Vec3f()));
    params.setForwardDirection(Vec3f.NEG_Z_AXIS);
    params.setUpDirection(Vec3f.Y_AXIS);
    params.setVertFOV((float) Math.PI / 8.0f);
    params.setImagePlaneAspectRatio((float) xSize / (float) ySize);
    params.setXSize(xSize);
    params.setYSize(ySize);
  }

  private void motionMethod(MouseEvent e, int x, int y) {
    if (interactionUnderway && !iOwnInteraction) {
      ManipManager.getManipManager().mouseDragged(e);
    } else {
      int dx = x - lastX;
      int dy = y - lastY;
  
      lastX = x;
      lastY = y;

      if ((button1Down && (!button2Down))) {

        // Rotation functionality
        float xRads = (float) Math.PI * -1.0f * dy * rotateSpeed / 1000.0f;
        float yRads = (float) Math.PI * -1.0f * dx * rotateSpeed / 1000.0f;
        Rotf xRot = new Rotf(Vec3f.X_AXIS, xRads);
        Rotf yRot = new Rotf(Vec3f.Y_AXIS, yRads);
        Rotf newRot = yRot.times(xRot);
        orientation = orientation.times(newRot);

      } else if (button2Down && (!button1Down)) {

        // Translate functionality
        // Compute the local coordinate system's difference vector
        Vec3f localDiff = new Vec3f(dollySpeed * -1.0f * dx / 100.0f,
                                    dollySpeed * dy / 100.0f,
                                    0.0f);
        // Rotate this by camera's orientation
        Vec3f worldDiff = orientation.rotateVector(localDiff);
        // Add on to center
        center.add(worldDiff);

      } else if (button1Down && button2Down) {

        float diff = dollySpeed * -1.0f * dy / 100.0f;
        float newDolly = dolly.z() + diff;
        if (newDolly < minFocalDist) {
          newDolly = minFocalDist;
        }
        dolly.setZ(newDolly);

      }

      if (autoRedrawMode) {
        // Force redraw
        window.repaint();
      }
    }
  }

  private void mouseMethod(MouseEvent e, int mods, boolean press,
                           int x, int y) {
    if ((interactionUnderway && !iOwnInteraction) ||
        (!modifiersMatch(e, mods))) {
      // Update state and pass this event along to the ManipManager
      if (press) {
        interactionUnderway = true;
        iOwnInteraction = false;
        ManipManager.getManipManager().mousePressed(e);
      } else {
        interactionUnderway = false;
        iOwnInteraction = false;
        ManipManager.getManipManager().mouseReleased(e);
      }
    } else {
      if ((mods & MouseEvent.BUTTON1_MASK) != 0) {
        if (press) {
          button1Down = true;
        } else {
          button1Down = false;
        }
      } else {
        if (numMouseButtons != 3) {
          if ((mods & MouseEvent.BUTTON2_MASK) != 0) {
            if (press) {
              button2Down = true;
            } else {
              button2Down = false;
            }
          }
        } else {
          // FIXME: must test this on 3-button system
          if ((mods & MouseEvent.BUTTON3_MASK) != 0) {
            if (press) {
              button2Down = true;
            } else {
              button2Down = false;
            }
          }
        }
      }

      lastX = x;
      lastY = y;

      if (button1Down || button2Down) {
        interactionUnderway = true;
        iOwnInteraction = true;
      } else {
        interactionUnderway = false;
        iOwnInteraction = false;
      }

      if (autoRedrawMode) {
        // Force redraw
        window.repaint();
      }
    }
  }

  private void reshapeMethod(int w, int h) {
    float aspect, theta;
    aspect = (float) w / (float) h;
    if (w >= h)
      theta = 45;
    else
      theta = (float) Math.toDegrees(Math.atan(1 / aspect));
    theta *= vertFOVScale;
    params.setVertFOV((float) (Math.toRadians(theta) / 2.0));
    params.setImagePlaneAspectRatio(aspect);
    params.setXSize(w);
    params.setYSize(h);
  }

  private void recalc() {
    // Recompute position, forward and up vectors
    Vec3f tmp = new Vec3f();
    params.setPosition(computePosition(tmp));
    orientation.rotateVector(Vec3f.NEG_Z_AXIS, tmp);
    params.setForwardDirection(tmp);
    orientation.rotateVector(Vec3f.Y_AXIS, tmp);
    params.setUpDirection(tmp);
    params.setOrientation(orientation);

    // Compute modelview matrix based on camera parameters, position and
    // orientation
    Mat4f tmpMat = new Mat4f();
    tmpMat.makeIdent();
    tmpMat.setRotation(orientation);
    tmpMat.setTranslation(params.getPosition());
    tmpMat.invertRigid();
    params.setModelviewMatrix(tmpMat);

    // Compute perspective matrix given camera parameters
    float deltaZ = zFar - zNear;
    float aspect = params.getImagePlaneAspectRatio();
    float radians = params.getVertFOV();
    float sine = (float) Math.sin(radians);
    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      tmpMat.makeIdent();
      params.setProjectionMatrix(tmpMat);
      return;
    }
    float cotangent = (float) Math.cos(radians) / sine;
    tmpMat.makeIdent();
    tmpMat.set(0, 0, cotangent / aspect);
    tmpMat.set(1, 1, cotangent);
    tmpMat.set(2, 2, -(zFar + zNear) / deltaZ);
    tmpMat.set(3, 2, -1);
    tmpMat.set(2, 3, -2 * zNear * zFar / deltaZ);
    tmpMat.set(3, 3, 0);
    params.setProjectionMatrix(tmpMat);


    /********************

    // Recompute position, forward and up vectors
    params.setPosition(position);
    Vec3f tmp = new Vec3f();
    orientation.rotateVector(Vec3f.NEG_Z_AXIS, tmp);
    params.setForwardDirection(tmp);
    orientation.rotateVector(Vec3f.Y_AXIS, tmp);
    params.setUpDirection(tmp);
    params.setOrientation(orientation);

    // Compute modelview matrix based on camera parameters, position and
    // orientation
    Mat4f tmpMat = new Mat4f();
    tmpMat.makeIdent();
    tmpMat.setRotation(orientation);
    tmpMat.setTranslation(position);
    tmpMat.invertRigid();
    params.setModelviewMatrix(tmpMat);

    // Compute perspective matrix given camera parameters
    float deltaZ = zFar - zNear;
    float aspect = params.getImagePlaneAspectRatio();
    float radians = params.getVertFOV();
    float sine = (float) Math.sin(radians);
    if ((deltaZ == 0) || (sine == 0) || (aspect == 0)) {
      tmpMat.makeIdent();
      params.setProjectionMatrix(tmpMat);
      return;
    }
    float cotangent = (float) Math.cos(radians) / sine;
    tmpMat.makeIdent();
    tmpMat.set(0, 0, cotangent / aspect);
    tmpMat.set(1, 1, cotangent);
    tmpMat.set(2, 2, -(zFar + zNear) / deltaZ);
    tmpMat.set(3, 2, -1);
    tmpMat.set(2, 3, -2 * zNear * zFar / deltaZ);
    tmpMat.set(3, 3, 0);
    params.setProjectionMatrix(tmpMat);

    **********************/
  }

  private void recalc(GL gl) {
    recalc();

    gl.glMatrixMode(GL.GL_MODELVIEW);
    float[] data = new float[16];
    params.getModelviewMatrix().getColumnMajorData(data);
    gl.glLoadMatrixf(data, 0);

    gl.glMatrixMode(GL.GL_PROJECTION);
    params.getProjectionMatrix().getColumnMajorData(data);
    gl.glLoadMatrixf(data, 0);
  }

  private void recalcInverseRotation(GL gl) {
    Rotf oriInv = orientation.inverse();
    Vec3f tmp = new Vec3f();
    float ang = orientation.get(tmp);
    if (tmp.lengthSquared() > EPSILON)
      gl.glRotatef((float) Math.toDegrees(ang), tmp.x(), tmp.y(), tmp.z());
  }

  private Vec3f computePosition(Vec3f tmp) {
    orientation.rotateVector(dolly, tmp);
    tmp.add(center);
    return tmp;
  }
}
