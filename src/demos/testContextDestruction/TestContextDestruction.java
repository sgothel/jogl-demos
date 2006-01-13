/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package demos.testContextDestruction;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;

/** A simple demonstration exercising context creation and destruction
    as a GLCanvas is added to and removed from its parent container. */

public class TestContextDestruction {
  private int gearDisplayList;
  private Frame frame1, frame2;
  private Component frame1ContainedComponent;
  private Component frame1RemovedComponent;
  private Component frame2ContainedComponent;
  private Component frame2RemovedComponent;
  private GLCanvas canvas;
  private Canvas emptyCanvas;
  private boolean frame1IsTarget = true;
  private float angle = 0.0f;
  private static final int BORDER_WIDTH = 6;

  public static void main(String[] args) {
    new TestContextDestruction().run(args);
  }

  public void run(String[] args) {
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Listener());
    canvas.setSize(256, 256);

    frame1 = new Frame("Frame 1");
    frame1.setLayout(new BorderLayout());
    frame1.add(canvas, BorderLayout.CENTER);

    emptyCanvas = new Canvas();
    emptyCanvas.setBackground(Color.GRAY);
    emptyCanvas.setSize(256, 256);

    frame2 = new Frame("Frame 2");
    frame2.setLayout(new BorderLayout());
    frame2.add(emptyCanvas, BorderLayout.CENTER);

    frame1ContainedComponent = canvas;
    frame2ContainedComponent = emptyCanvas;

    frame1.pack();
    frame1.show();
    frame2.pack();
    frame2.show();
    frame2.setLocation(256 + BORDER_WIDTH, 0);
    
    JFrame uiFrame = new JFrame("Controls");
    uiFrame.getContentPane().setLayout(new GridLayout(3, 1));
    JButton button = new JButton("Toggle Frame 1's component");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (frame1ContainedComponent == null) {
            frame1ContainedComponent = frame1RemovedComponent;
            frame1RemovedComponent = null;
            frame1.add(frame1ContainedComponent);
          } else {
            frame1RemovedComponent = frame1ContainedComponent;
            frame1ContainedComponent = null;
            frame1.remove(frame1RemovedComponent);
          }
        }
      });
    uiFrame.getContentPane().add(button);
    button = new JButton("Swap Frame 1's and Frame 2's components");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("Swapping Frame 1's and Frame 2's components");
          Component t1 = null, t2 = null;
          t1 = frame1ContainedComponent;
          t2 = frame2ContainedComponent;
          if (t1 != null) {
            frame1.remove(t1);
          }
          if (t2 != null) {
            frame2.remove(t2);
          }
          if (t1 != null) {
            frame2.add(t1);
          }
          if (t2 != null) {
            frame1.add(t2);
          }
          frame1ContainedComponent = t2;
          frame2ContainedComponent = t1;
          t1 = frame1RemovedComponent;
          frame1RemovedComponent = frame2RemovedComponent;
          frame2RemovedComponent = t1;
        }
      });
    uiFrame.getContentPane().add(button);
    button = new JButton("Toggle Frame 2's component");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (frame2ContainedComponent == null) {
            frame2ContainedComponent = frame2RemovedComponent;
            frame2RemovedComponent = null;
            frame2.add(frame2ContainedComponent);
          } else {
            frame2RemovedComponent = frame2ContainedComponent;
            frame2ContainedComponent = null;
            frame2.remove(frame2RemovedComponent);
          }
        }
      });
    uiFrame.getContentPane().add(button);
    uiFrame.pack();
    uiFrame.show();
    uiFrame.setLocation(512 + BORDER_WIDTH + BORDER_WIDTH, 0);

    final Animator animator = new Animator(canvas);
    WindowListener windowListener = new WindowAdapter() {
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
      };
    frame1.addWindowListener(windowListener);
    frame2.addWindowListener(windowListener);
    uiFrame.addWindowListener(windowListener);
    animator.start();
  }

  class Listener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      System.out.println("Listener.init()");
      drawable.setGL(new DebugGL(drawable.getGL()));

      GL gl = drawable.getGL();

      float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
      gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, pos, 0);
      gl.glEnable(GL.GL_CULL_FACE);
      gl.glEnable(GL.GL_LIGHTING);
      gl.glEnable(GL.GL_LIGHT0);
      gl.glEnable(GL.GL_DEPTH_TEST);

      initializeDisplayList(gl);

      gl.glEnable(GL.GL_NORMALIZE);

      reshape(drawable, 0, 0, drawable.getWidth(), drawable.getHeight());
    }

    public void display(GLAutoDrawable drawable) {
      angle += 2.0f;

      GL gl = drawable.getGL();

      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      gl.glPushMatrix();
      gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
      gl.glCallList(gearDisplayList);
      gl.glPopMatrix();
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.println("Listener.reshape()");
      GL gl = drawable.getGL();

      float h = (float)height / (float)width;
            
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glLoadIdentity();
      gl.glTranslatef(0.0f, 0.0f, -40.0f);
    }

    public void destroy(GLAutoDrawable drawable) {
      System.out.println("Listener.destroy()");
      GL gl = drawable.getGL();
      gl.glDeleteLists(gearDisplayList, 1);
      gearDisplayList = 0;
    }

    // Unused routines
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  private synchronized void initializeDisplayList(GL gl) {
    gearDisplayList = gl.glGenLists(1);
    gl.glNewList(gearDisplayList, GL.GL_COMPILE);
    float red[] = { 0.8f, 0.1f, 0.0f, 1.0f };
    gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, red, 0);
    gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
    gl.glEndList();
  }

  private void gear(GL gl,
                    float inner_radius,
                    float outer_radius,
                    float width,
                    int teeth,
                    float tooth_depth)
  {
    int i;
    float r0, r1, r2;
    float angle, da;
    float u, v, len;

    r0 = inner_radius;
    r1 = outer_radius - tooth_depth / 2.0f;
    r2 = outer_radius + tooth_depth / 2.0f;
            
    da = 2.0f * (float) Math.PI / teeth / 4.0f;
            
    gl.glShadeModel(GL.GL_FLAT);

    gl.glNormal3f(0.0f, 0.0f, 1.0f);

    /* draw front face */
    gl.glBegin(GL.GL_QUAD_STRIP);
    for (i = 0; i <= teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
        if(i < teeth)
          {
            gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
            gl.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), width * 0.5f);
          }
      }
    gl.glEnd();

    /* draw front sides of teeth */
    gl.glBegin(GL.GL_QUADS);
    for (i = 0; i < teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + 2.0f * da), r2 * (float)Math.sin(angle + 2.0f * da), width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle + 3.0f * da), r1 * (float)Math.sin(angle + 3.0f * da), width * 0.5f);
      }
    gl.glEnd();
    
    /* draw back face */
    gl.glBegin(GL.GL_QUAD_STRIP);
    for (i = 0; i <= teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
      }
    gl.glEnd();
    
    /* draw back sides of teeth */
    gl.glBegin(GL.GL_QUADS);
    for (i = 0; i < teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
      }
    gl.glEnd();
    
    /* draw outward faces of teeth */
    gl.glBegin(GL.GL_QUAD_STRIP);
    for (i = 0; i < teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle), r1 * (float)Math.sin(angle), -width * 0.5f);
        u = r2 * (float)Math.cos(angle + da) - r1 * (float)Math.cos(angle);
        v = r2 * (float)Math.sin(angle + da) - r1 * (float)Math.sin(angle);
        len = (float)Math.sqrt(u * u + v * v);
        u /= len;
        v /= len;
        gl.glNormal3f(v, -u, 0.0f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + da), r2 * (float)Math.sin(angle + da), -width * 0.5f);
        gl.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), width * 0.5f);
        gl.glVertex3f(r2 * (float)Math.cos(angle + 2 * da), r2 * (float)Math.sin(angle + 2 * da), -width * 0.5f);
        u = r1 * (float)Math.cos(angle + 3 * da) - r2 * (float)Math.cos(angle + 2 * da);
        v = r1 * (float)Math.sin(angle + 3 * da) - r2 * (float)Math.sin(angle + 2 * da);
        gl.glNormal3f(v, -u, 0.0f);
        gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), width * 0.5f);
        gl.glVertex3f(r1 * (float)Math.cos(angle + 3 * da), r1 * (float)Math.sin(angle + 3 * da), -width * 0.5f);
        gl.glNormal3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f);
      }
    gl.glVertex3f(r1 * (float)Math.cos(0), r1 * (float)Math.sin(0), width * 0.5f);
    gl.glVertex3f(r1 * (float)Math.cos(0), r1 * (float)Math.sin(0), -width * 0.5f);
    gl.glEnd();
    
    gl.glShadeModel(GL.GL_SMOOTH);
    
    /* draw inside radius cylinder */
    gl.glBegin(GL.GL_QUAD_STRIP);
    for (i = 0; i <= teeth; i++)
      {
        angle = i * 2.0f * (float) Math.PI / teeth;
        gl.glNormal3f(-(float)Math.cos(angle), -(float)Math.sin(angle), 0.0f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), -width * 0.5f);
        gl.glVertex3f(r0 * (float)Math.cos(angle), r0 * (float)Math.sin(angle), width * 0.5f);
      }
    gl.glEnd();
  }
}
