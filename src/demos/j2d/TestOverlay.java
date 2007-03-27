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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.j2d.*;

import demos.gears.Gears;
import demos.util.*;
import gleem.linalg.*;

/** A simple test of the Overlay utility class. Draws gears underneath
    with moving Java 2D-rendered text on top. */

public class TestOverlay implements GLEventListener {
  public static void main(String[] args) {
    Frame frame = new Frame("Java 2D Overlay Test");
    GLCapabilities caps = new GLCapabilities();
    caps.setAlphaBits(8);
    GLCanvas canvas = new GLCanvas(caps);
    canvas.addGLEventListener(new Gears());
    canvas.addGLEventListener(new TestOverlay());
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

  private Overlay overlay;
  private Time time;
  private Font font;
  private Color TRANSPARENT_BLACK = new Color(0.0f, 0.0f, 0.0f, 0.0f);
  private FontRenderContext frc;
  private GlyphVector gv;
  private Vec2f velocity = new Vec2f(100.0f, 150.0f);
  private Vec2f position;
  private Rectangle textBounds;
  private Rectangle lastTextBounds;
  private String TEST_STRING = "Java 2D Text";
  private long startTime;
  private int frameCount;
  private DecimalFormat format = new DecimalFormat("####.00");

  private Rectangle fpsBounds;

  public void init(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();
    gl.setSwapInterval(0);

    overlay = new Overlay(drawable);
    time = new SystemTime();
    ((SystemTime) time).rebase();

    // Start the text half way up the left side
    position = new Vec2f(0.0f, drawable.getHeight() / 2);

    // Create the font, render context, and glyph vector
    font = new Font("SansSerif", Font.BOLD, 36);
  }

  public void display(GLAutoDrawable drawable) {
    if (startTime == 0) {
      startTime = System.currentTimeMillis();
    }

    Graphics2D g2d = overlay.createGraphics();

    if (++frameCount == 30) {
      long endTime = System.currentTimeMillis();
      float fps = 30.0f / (float) (endTime - startTime) * 1000;
      frameCount = 0;
      startTime = System.currentTimeMillis();

      FontRenderContext frc = g2d.getFontRenderContext();
      String fpsString = "FPS: " + format.format(fps);
      GlyphVector gv = font.createGlyphVector(frc, TEST_STRING);
      fpsBounds = gv.getPixelBounds(frc, 0, 0);
      int x = drawable.getWidth() - fpsBounds.width - 20;
      int y = drawable.getHeight() - 20;
      g2d.setFont(font);
      g2d.setComposite(AlphaComposite.Src);
      g2d.setColor(TRANSPARENT_BLACK);
      g2d.fillRect(x + fpsBounds.x, y + fpsBounds.y, fpsBounds.width, fpsBounds.height);
      g2d.setColor(Color.WHITE);
      g2d.drawString(fpsString, x, y);
      overlay.markDirty(x + fpsBounds.x, y + fpsBounds.y, fpsBounds.width, fpsBounds.height);
    }

    time.update();

    g2d.setFont(font);
    g2d.setComposite(AlphaComposite.Src);
    if (overlay.contentsLost()) {
      frc = g2d.getFontRenderContext();
      gv = font.createGlyphVector(frc, TEST_STRING);
    }

    // Compute the next position of the text
    position = position.plus(velocity.times((float) time.deltaT()));
    // Figure out whether we have to switch directions
    textBounds = gv.getPixelBounds(frc, position.x(), position.y());
    if (textBounds.getMinX() < 0) {
      velocity.setX(Math.abs(velocity.x()));
    } else if (textBounds.getMaxX() > drawable.getWidth()) {
      velocity.setX(-1.0f * Math.abs(velocity.x()));
    }
    if (textBounds.getMinY() < 0) {
      velocity.setY(Math.abs(velocity.y()));
    } else if (textBounds.getMaxY() > drawable.getHeight()) {
      velocity.setY(-1.0f * Math.abs(velocity.y()));
    }

    // Clear the last text (if any) and draw the current
    if (lastTextBounds != null) {
      g2d.setColor(TRANSPARENT_BLACK);
      g2d.fillRect((int) lastTextBounds.getMinX(), (int) lastTextBounds.getMinY(),
                   (int) (lastTextBounds.getWidth() + 1), (int) (lastTextBounds.getHeight() + 1));
    } else if (overlay.contentsLost()) {
      g2d.setColor(TRANSPARENT_BLACK);
      g2d.fillRect(0, 0, drawable.getWidth(), drawable.getHeight());
    }
    g2d.setColor(Color.WHITE);
    g2d.drawString(TEST_STRING, position.x(), position.y());

    // Compute the union of these rectangles to push an update to
    // the overlay
    Rectangle union = new Rectangle(textBounds);
    if (lastTextBounds != null) {
      union.add(lastTextBounds);
    }
    // Put a little slop around this text due to apparent rounding errors
    overlay.markDirty(union.x, union.y, union.width + 10, union.height + 10);

    // Move down the text bounds
    lastTextBounds = textBounds;

    // Draw the overlay
    overlay.drawAll();
    g2d.dispose();
  }

  // Unused methods
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
}
