/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package demos.xtrans;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.*;

/** A JDesktopPane subclass supporting Accelerated Transitions (XT) of
 * the components contained within.
 *
 * @author Kenneth Russell
 */

public class XTDesktopPane extends OffscreenDesktopPane {
  private GLContext j2dContext;
  private Object    j2dContextSurfaceIdentifier;

  private Rectangle oglViewport;

  private XTTransitionManager transitionManager = new XTBasicTransitionManager();

  private boolean reallyRemove;

  private boolean alwaysRedraw;

  static class TransitionInfo {
    boolean isIn;
    Component target;
    long startTime;
    XTTransition trans;

    TransitionInfo(boolean isIn,
                   Component target,
                   long startTime,
                   XTTransition trans) {
      this.isIn = isIn;
      this.target = target;
      this.startTime = startTime;
      this.trans = trans;
    }
  }

  private java.util.List/*<TransitionInfo>*/ transitions    = new ArrayList();

  private float TRANSITION_DURATION = 300.0f;

  private int textureTarget = GL.GL_TEXTURE_2D;
  private GLU glu = new GLU();

  /** Creates a new accelerated transition desktop pane. */
  public XTDesktopPane() {
    super();
    if (!Java2D.isOGLPipelineActive()) {
      throw new RuntimeException("XTDesktopPane requires new Java2D/JOGL support in Java SE 6 and -Dsun.java2d.opengl=true");
    }
    setDesktopManager(new XTDesktopManager());
  }

  /** Overridden to use a transition to display the given
      component. */
  protected void addImpl(Component c, Object constraints, int index) {
    super.addImpl(c, constraints, index);
    getOffscreenDesktopManager().layoutOffscreenBuffer(this);

    // When animating the component's transition, center the
    // perspective projection around the center of the newly-added
    // component so that the perspective effects appear symmetric.
    // This amounts to moving the viewport so the component is in the
    // center.
    addTransition(true, c,
                  transitionManager.createTransitionForComponent(c,
                                                                 true,
                                                                 getOGLViewport(),
                                                                 computeViewportOffsetToCenterComponent(c, getOGLViewport()),
                                                                 getXTDesktopManager().getOpenGLTextureCoords(c)));
  }

  /** Overridden to use an animated transition to remove the passed
      component. */
  public void remove(int index) {
    if (reallyRemove) {
      super.remove(index);
    } else {
      addRemoveTransition(getRealComponent(getComponent(index)));
    }
  }

  /** Overridden to use an animated transition to remove the passed
      component. */
  public void remove(Component c) {
    if (reallyRemove) {
      super.remove(c);
    } else {
      addRemoveTransition(getRealComponent(c));
    }
  }

  /** Causes the given component to really be removed from this
      desktop pane. Called when the removal transition is complete. */
  protected void removeImpl(Component c) {
    reallyRemove = true;
    try {
      remove(c);
    } finally {
      reallyRemove = false;
    }
  }

  /** Overridden to draw the child components, including any animated
      transitions, using OpenGL. */
  protected void paintChildren(final Graphics g) {
    // FIXME: this is a hack to get repainting behavior to work
    // properly when we specify that optimized drawing is disabled (so
    // that childrens' repaint requests will trickle up to us via the
    // Animator) but need to descend to repaint our children --
    // currently don't know how to distinguish between repaint events
    // propagated up to us and those initiated by the children (which
    // typically go through the OffscreenComponentWrapper's
    // getGraphics() method and implicitly cause a redraw of all child
    // components as well as the desktop)
    if (alwaysRedraw) {
      getOffscreenDesktopManager().setNeedsRedraw();
    }

    // Update desktop manager's offscreen buffer if necessary
    getOffscreenDesktopManager().updateOffscreenBuffer(this);

    // Draw textured quads using JOGL over current contents of back
    // buffer
    final Component[] components = getRealChildComponents();
    final ArrayList   expiredTransitions = new ArrayList();
    Java2D.invokeWithOGLContextCurrent(g, new Runnable() {
        public void run() {
          // Get valid Java2D context
          if (j2dContext == null ||
              j2dContextSurfaceIdentifier != Java2D.getOGLSurfaceIdentifier(g)) {
            j2dContext = GLDrawableFactory.getFactory().createExternalGLContext();
            j2dContext.setGL(new DebugGL(j2dContext.getGL()));
            j2dContextSurfaceIdentifier = Java2D.getOGLSurfaceIdentifier(g);
          }

          j2dContext.makeCurrent(); // No-op
          try {
            GL gl = j2dContext.getGL();

            // Figure out where JDesktopPane is on the Swing back buffer
            Rectangle oglRect = Java2D.getOGLViewport(g, getWidth(), getHeight());
            // Cache this value for adding transitions later
            oglViewport = new Rectangle(oglRect);

            // Set up perspective projection so we can do some subtle
            // 3D effects. We set up the view volume so that at z=0
            // the lower-left coordinates of the desktop are (0, 0)
            // and the upper right coordinates are
            // (oglRect.getWidth(), oglRect.getHeight()). The key here
            // is to decide on the field of view and then figure out
            // how far back we have to put the eye point in order for
            // this to occur.
            double fovy = 30.0; // degrees
            double w = oglRect.getWidth();
            double h = oglRect.getHeight();
            // d is the distance from the eye point to the image plane
            // (z=0)
            double d = (h / 2) / Math.tan(Math.toRadians(fovy) / 2);
            double near = d - (h / 2);
            double far  = d + (h / 2);
            gl.glViewport(oglRect.x, oglRect.y, oglRect.width, oglRect.height);
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            glu.gluPerspective(fovy, (w / h), near, far);
            gl.glMatrixMode(GL.GL_TEXTURE);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            double eyeX = w / 2;
            double eyeY = h / 2;
            // Object x and y are the same as eye x and y since we're
            // looking in the -z direction
            glu.gluLookAt(eyeX, eyeY, d,
                          eyeX, eyeY, 0,
                          0, 1, 0);

            // Set up a scissor box so we don't blow away other
            // components if we shift around the viewport to get the
            // animated transitions' perspective effects to be
            // centered
            gl.glEnable(GL.GL_SCISSOR_TEST);
            Rectangle r = Java2D.getOGLScissorBox(g);
            if (r != null) {
              gl.glScissor(r.x, r.y, r.width, r.height);
            }
                          
            /*

            // Orthographic projection for debugging
            gl.glViewport(oglRect.x, oglRect.y, oglRect.width, oglRect.height);
            // Set up coordinate system for easy access
            gl.glMatrixMode(GL.GL_PROJECTION);
            //          System.err.println("oglRect x = " + oglRect.getX());
            //          System.err.println("oglRect y = " + oglRect.getY());
            //          System.err.println("oglRect w = " + oglRect.getWidth());
            //          System.err.println("oglRect h = " + oglRect.getHeight());
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glOrtho(oglRect.getX(), oglRect.getX() + oglRect.getWidth(),
                       oglRect.getY(), oglRect.getY() + oglRect.getHeight(),
                       -1,
                       1);
            gl.glMatrixMode(GL.GL_TEXTURE);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            */

            // Enable and bind texture corresponding to internal frames' back buffer
            gl.glBindTexture(textureTarget, getXTDesktopManager().getOpenGLTextureObject());

            gl.glEnable(textureTarget);
            gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
            gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

            gl.glEnable(GL.GL_BLEND);
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

            // Iterate down children in z order bottom-to-top
            int compCount = components.length;
            long curTime = currentTimeMillis();
            for (int i = compCount - 1; i >= 0; i--) {
              Component c = components[i];

              // Find transition for this component
              TransitionInfo info = transitionForComponent(c);

              if (info != null) {
                gl.glPushMatrix();
                // When animating the component's transition, center the
                // perspective projection around the center of the newly-added
                // component so that the perspective effects appear symmetric.
                // This amounts to moving the viewport so the component is in the
                // center.
                Point viewportOffset = computeViewportOffsetToCenterComponent(c, getOGLViewport());
                gl.glViewport(oglRect.x + viewportOffset.x,
                              oglRect.y + viewportOffset.y,
                              oglRect.width,
                              oglRect.height);

                // Update it
                float percent = clamp((curTime - info.startTime) / TRANSITION_DURATION, 0.0f, 1.0f);
                XTTransition trans = info.trans;
                trans.update(percent);
                trans.draw(gl);
                // See whether the transition has expired
                if (percent == 1.0f) {
                  transitions.remove(info);
                  expiredTransitions.add(info);
                }
                gl.glPopMatrix();
                // Put the viewport back where it was
                gl.glViewport(oglRect.x, oglRect.y, oglRect.width, oglRect.height);
              } else {
                // For each one, get the OpenGL texture coordinates on the offscreen OpenGL texture
                Rectangle2D oglTexCoords = getXTDesktopManager().getOpenGLTextureCoords(c);
                Rectangle   bounds       = c.getBounds();

                int   cx = bounds.x;
                int   cy = bounds.y;
                int   cw = bounds.width;
                int   ch = bounds.height;
                float tx = (float) oglTexCoords.getX();
                float ty = (float) oglTexCoords.getY();
                float tw = (float) oglTexCoords.getWidth();
                float th = (float) oglTexCoords.getHeight();
                float vx = oglRect.x;
                float vy = oglRect.y;
                float vw = oglRect.width;
                float vh = oglRect.height;

                // Draw a quad per component
                gl.glBegin(GL.GL_TRIANGLES);
                gl.glColor4f(1, 1, 1, 1);

                // Triangle 1
                gl.glTexCoord2f(tx,        ty + th);
                gl.glVertex3f  (cx,        vh - cy,      0);
                gl.glTexCoord2f(tx,        ty);
                gl.glVertex3f  (cx,        vh - cy - ch, 0);
                gl.glTexCoord2f(tx + tw,   ty + th);
                gl.glVertex3f  (cx + cw,   vh - cy,      0);
                // Triangle 2
                gl.glTexCoord2f(tx + tw,   ty + th);
                gl.glVertex3f  (cx + cw,   vh - cy,      0);
                gl.glTexCoord2f(tx,        ty);
                gl.glVertex3f  (cx,        vh - cy - ch, 0);
                gl.glTexCoord2f(tx + tw,   ty);
                gl.glVertex3f  (cx + cw,   vh - cy - ch, 0);
                
                gl.glEnd();
              }
            }
            gl.glFlush();
            gl.glDisable(textureTarget);
            gl.glDisable(GL.GL_BLEND);

            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL.GL_TEXTURE);
            gl.glPopMatrix();
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPopMatrix();
            gl.glFinish();
          } finally {
            j2dContext.release();
          }
        }
      });

    for (Iterator iter = expiredTransitions.iterator(); iter.hasNext(); ) {
      TransitionInfo info = (TransitionInfo) iter.next();
      if (!info.isIn) {
        removeImpl(info.target);
        repaint();
      }
    }
                                       
    if (!transitions.isEmpty()) {
      repaint();
    }
  }

  /** Overridden from parent to disable optimized drawing so that we
      get correct rendering results with embedded GLJPanels */
  public boolean isOptimizedDrawingEnabled() {
    return false;
  }

  /** Returns the XTDesktopManager for this desktop pane. */
  public XTDesktopManager getXTDesktopManager() {
    return (XTDesktopManager) getDesktopManager();
  }

  /** Returns the transition manager for this desktop pane. By default
      this is an XTBasicTransitionManager. */
  public XTTransitionManager getTransitionManager() {
    return transitionManager;
  }

  /** Sets the transition manager for this desktop pane. By default
      this is an XTBasicTransitionManager. */
  public void setTransitionManager(XTTransitionManager manager) {
    transitionManager = manager;
  }

  /** Workaround to get painting behavior to work properly in some
      situations. */
  public void setAlwaysRedraw(boolean onOrOff) {
    alwaysRedraw = onOrOff;
  }

  /** Workaround to get painting behavior to work properly in some
      situations. */
  public boolean getAlwaysRedraw() {
    return alwaysRedraw;
  }

  /** Returns the transition corresponding to the passed Component, or
      null if no transition is currently active for this component. */
  private TransitionInfo transitionForComponent(Component c) {
    for (Iterator iter = transitions.iterator(); iter.hasNext(); ) {
      TransitionInfo info = (TransitionInfo) iter.next();
      if (info.target == c) {
        return info;
      }
    }
    return null;
  }

  /** Adds a transition for the specified component. An "out"
      transition will automatically cause the component to be removed
      after it has completed running. */
  protected void addTransition(boolean isIn,
                               Component target,
                               XTTransition trans) {
    TransitionInfo info = new TransitionInfo(isIn,
                                             target,
                                             currentTimeMillis(),
                                             trans);
    transitions.add(info);
  }

  /** Adds a removal transition for the given component. */
  protected void addRemoveTransition(Component target) {
    addTransition(false,
                  target,
                  transitionManager.createTransitionForComponent(target,
                                                                 false,
                                                                 getOGLViewport(),
                                                                 computeViewportOffsetToCenterComponent(target, getOGLViewport()),
                                                                 getXTDesktopManager().getOpenGLTextureCoords(target)));
  }

  /** Computes the offset applied to the OpenGL viewport to center the
      given component in the viewport. This is used to make the
      perspective effects appear symmetric about the component. */
  protected Point computeViewportOffsetToCenterComponent(Component c,
                                                         Rectangle oglViewport) {
    Rectangle bounds = c.getBounds();
    return new Point(bounds.x + ((bounds.width - oglViewport.width) / 2),
                     -bounds.y + ((oglViewport.height - bounds.height) / 2));
  }

  /** Clamps the given value between the specified minimum and
      maximum. */
  protected static float clamp(float val, float min, float max) {
    return Math.min(max, Math.max(min, val));
  }

  /** Returns the current time in milliseconds. */
  protected static long currentTimeMillis() {
    // Avoid 1.5 compilation dependencies since no perceived
    // improvement by changing this
    //    return System.nanoTime() / 1000000;
    return System.currentTimeMillis();
  }

  /** Returns the OpenGL viewport corresponding to this desktop pane. */
  protected Rectangle getOGLViewport() {
    if (oglViewport != null) {
      return oglViewport;
    }

    Rectangle b = getBounds();
    return new Rectangle(0, 0, b.width, b.height);
  }
}
