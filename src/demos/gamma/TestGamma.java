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

package demos.gamma;

import demos.gears.Gears;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.Gamma;



public class TestGamma implements GLEventListener {
  static {
    GLProfile.initSingleton();
  }

  private static void usage() {
    System.out.println("Usage: java TestGamma [gamma value] [brightness value] [contrast value]");
    System.exit(1);
  }

  public void init(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();
    if (!Gamma.setDisplayGamma(gl, gamma, brightness, contrast)) {
      System.err.println("Unable to change display gamma, brightness, and contrast");
    }
    System.err.println("init: Gamma.setDisplayGamma");
  }

  public void dispose(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();
    Gamma.resetDisplayGamma(gl);
    System.err.println("dispose: Gamma.resetDisplayGamma");
  }

  public void display(GLAutoDrawable drawable) {
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
  }

  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
  }

  public static void main(String[] args) {
    if (args.length != 3) {
      usage();
    }

    try {
      gamma = Float.parseFloat(args[0]);
      brightness = Float.parseFloat(args[1]);
      contrast = Float.parseFloat(args[2]);
    } catch (NumberFormatException e) {
      usage();
    }

    Frame frame = new Frame("Gear and Gamma Demo");
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Gears());
    canvas.addGLEventListener(new TestGamma());
    frame.add(canvas);
    frame.setSize(300, 300);
    final Animator animator = new Animator(canvas);
    frame.addWindowListener(new WindowAdapter() {
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
      });
    frame.setVisible(true);
    animator.start();
    
  }

    static float gamma = 1.0f;
    static float brightness = 0.0f;
    static float contrast = 0.0f;
}
