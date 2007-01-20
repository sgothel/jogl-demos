/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package demos.j2d;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.j2d.*;

import demos.gears.Gears;
import demos.util.*;
import gleem.linalg.*;

/** A simple test of the TextRenderer class. Draws gears underneath
    with moving Java 2D-rendered text on top. */

public class TestTextRenderer implements GLEventListener {
  public static void main(String[] args) {
    Frame frame = new Frame("Text Renderer Test");
    GLCapabilities caps = new GLCapabilities();
    caps.setAlphaBits(8);
    GLCanvas canvas = new GLCanvas(caps);
    canvas.addGLEventListener(new Gears());
    canvas.addGLEventListener(new TestTextRenderer());
    frame.add(canvas);
    frame.setSize(512, 512);
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

    frame.show();
    animator.start();
  }

  private TextRenderer renderer;
  private Time time;
  private Font font;
  private Vec2f velocity = new Vec2f(100.0f, 150.0f);
  private Vec2f position;
  private String TEST_STRING = "Java 2D Text";
  private int textWidth;
  private int textHeight;
  private FPSCounter fps;

  public void init(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();

    // Don't artificially slow us down, at least on platforms where we
    // have control over this (note: on X11 platforms this may not
    // have the effect of overriding the setSwapInterval(1) in the
    // Gears demo)
    gl.setSwapInterval(0);

    renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
    time = new SystemTime();
    ((SystemTime) time).rebase();

    // Start the text half way up the left side
    position = new Vec2f(0.0f, drawable.getHeight() / 2);
    Rectangle2D textBounds = renderer.getBounds(TEST_STRING);
    textWidth = (int) textBounds.getWidth();
    textHeight = (int) textBounds.getHeight();

    fps = new FPSCounter(drawable, 36);
  }

  public void display(GLAutoDrawable drawable) {
    time.update();

    // Compute the next position of the text
    position = position.plus(velocity.times((float) time.deltaT()));
    // Figure out whether we have to switch directions
    if (position.x() < 0) {
      velocity.setX(Math.abs(velocity.x()));
    } else if (position.x() + textWidth > drawable.getWidth()) {
      velocity.setX(-1.0f * Math.abs(velocity.x()));
    }      
    if (position.y() < 0) {
      velocity.setY(Math.abs(velocity.y()));
    } else if (position.y() + textHeight > drawable.getHeight()) {
      velocity.setY(-1.0f * Math.abs(velocity.y()));
    }      

    GL gl = drawable.getGL();

    // Prepare to draw text
    renderer.beginRendering(drawable.getWidth(), drawable.getHeight());

    // Draw text
    renderer.draw(TEST_STRING, (int) position.x(), (int) position.y());

    // Draw FPS
    fps.draw();

    // Clean up rendering
    renderer.endRendering();
  }

  // Unused methods
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
}
