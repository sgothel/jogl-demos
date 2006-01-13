/*
 * Portions Copyright (C) 2003 Sun Microsystems, Inc.
 * All rights reserved.
 */

package demos.printext;

import java.awt.*;
import javax.media.opengl.*;

public class PrintExt {
  public static void main(String[] args) {
    Frame frame = new Frame();
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Listener());
    frame.setUndecorated(true);
    frame.add(canvas);
    frame.setSize(1, 1);
    frame.setVisible(true);
  }

  static class Listener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      System.out.println("GL vendor: " + gl.glGetString(GL.GL_VENDOR));
      System.out.println("GL version: " + gl.glGetString(GL.GL_VERSION));
      System.out.println("GL renderer: " + gl.glGetString(GL.GL_RENDERER));
      System.out.println("GL extensions:");
      String[] extensions = gl.glGetString(GL.GL_EXTENSIONS).split(" ");
      int i = 0;
      while (i < extensions.length) {
        System.out.print("  ");
        String ext = extensions[i++];
        System.out.print(ext);
        if (i < extensions.length) {
          for (int j = 0; j < (40 - ext.length()); j++) {
            System.out.print(" ");
          }
          System.out.println(extensions[i++]);
        } else {
          System.out.println();
        }
      }
      runExit();
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
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
