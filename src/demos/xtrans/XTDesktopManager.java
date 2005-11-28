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
import java.awt.image.*;
import java.beans.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.*;

/** A desktop manager implementation supporting accelerated
 * transitions of the components on the desktop via OpenGL. This class
 * does not need to be instantiated by end users; it is installed
 * automatically when an XTDesktopPane is constructed.
 *
 * @author Kenneth Russell
 */
public class XTDesktopManager extends OffscreenDesktopManager {
  private GLContext j2dContext;
  private Object    j2dContextSurfaceIdentifier;
  private int       oglTextureId;
  private int       prevBackBufferWidth;
  private int       prevBackBufferHeight;

  private int textureTarget = GL.GL_TEXTURE_2D;

  /** Returns the OpenGL texture object ID associated with the
      off-screen back buffer for all of the components on the
      desktop. */
  public int getOpenGLTextureObject() {
    return oglTextureId;
  }

  /** Returns a rectangle specifying the OpenGL texture coordinates of
      the passed component in the texture object. The x and y
      coordinates of the returned rectangle specify the lower left
      corner of the component's image. */
  public Rectangle2D getOpenGLTextureCoords(Component c) {
    Rectangle rect = getBoundsOnBackBuffer(c);
    if (rect == null) {
      throw new RuntimeException("Unknown component " + c);
    }
    double offscreenWidth  = getOffscreenBackBufferWidth();
    double offscreenHeight = getOffscreenBackBufferHeight();
    return new Rectangle2D.Double(rect.x / offscreenWidth,
                                  (offscreenHeight - rect.y - rect.height) / offscreenHeight,
                                  rect.width / offscreenWidth,
                                  rect.height / offscreenHeight);
  }

  /** Updates the off-screen buffer of this desktop manager and makes
      the rendering results available to OpenGL in the form of a
      texture object. */
  public void updateOffscreenBuffer(OffscreenDesktopPane parent) {
    boolean needsCopy = needsCopyBack();
    boolean hadPrevBackBuffer = false;
    super.updateOffscreenBuffer(parent);
    Image img = getOffscreenBackBuffer();
    final boolean mustResizeOGLTexture = ((oglTextureId == 0) ||
                                          (img == null) ||
                                          (prevBackBufferWidth  != img.getWidth(null)) ||
                                          (prevBackBufferHeight != img.getHeight(null)));
    if (needsCopy) {
      final Graphics g = getOffscreenGraphics();
      // Capture off-screen buffer contents into OpenGL texture
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

              if (oglTextureId == 0) {
                // Set up and initialize texture
                int[] tmp = new int[1];

                gl.glGenTextures(1, tmp, 0);
                oglTextureId = tmp[0];
                if (oglTextureId == 0) {
                  throw new RuntimeException("Error generating OpenGL back buffer texture");
                }
                assert mustResizeOGLTexture : "Must know we need to resize";
              }

              gl.glBindTexture(textureTarget, oglTextureId);

              int offscreenWidth  = getOffscreenBackBufferWidth();
              int offscreenHeight = getOffscreenBackBufferHeight();

              if (mustResizeOGLTexture) {
                prevBackBufferWidth  = offscreenWidth;
                prevBackBufferHeight = offscreenHeight;

                gl.glTexImage2D(textureTarget,
                                0,
                                GL.GL_RGBA8,
                                offscreenWidth,
                                offscreenHeight,
                                0,
                                GL.GL_RGBA,
                                GL.GL_UNSIGNED_BYTE,
                                null);
              }

              // Copy texture from offscreen buffer
              // NOTE: assumes read buffer is set up
              // FIXME: could be more efficient by copying only bounding rectangle

              gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE);
              gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, GL.GL_FALSE);
              gl.glPixelStorei(GL.GL_UNPACK_LSB_FIRST, GL.GL_FALSE);
              gl.glPixelStorei(GL.GL_PACK_LSB_FIRST, GL.GL_FALSE);
              gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
              gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, 0);
              gl.glPixelStorei(GL.GL_UNPACK_SKIP_ROWS, 0);
              gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, 0);
              gl.glPixelStorei(GL.GL_UNPACK_SKIP_PIXELS, 0);
              gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, 0);
              gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
              gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
              gl.glPixelTransferf(GL.GL_RED_SCALE, 1);
              gl.glPixelTransferf(GL.GL_GREEN_SCALE, 1);
              gl.glPixelTransferf(GL.GL_BLUE_SCALE, 1);
              gl.glPixelTransferf(GL.GL_ALPHA_SCALE, 1);
              gl.glPixelTransferf(GL.GL_RED_BIAS, 0);
              gl.glPixelTransferf(GL.GL_GREEN_BIAS, 0);
              gl.glPixelTransferf(GL.GL_BLUE_BIAS, 0);
              gl.glPixelTransferf(GL.GL_ALPHA_BIAS, 0);

              // long start = System.currentTimeMillis();
              gl.glCopyTexSubImage2D(textureTarget,
                                     0,
                                     0,
                                     0,
                                     0,
                                     0,
                                     offscreenWidth,
                                     offscreenHeight);
              // long end = System.currentTimeMillis();
              // System.err.println("glCopyTexSubImage2D " + offscreenWidth + "x" + offscreenHeight + " took " + (end - start) + " ms");

            } finally {
              j2dContext.release();
            }
          }
        });
    }
  }

  // Ideally we would force a repaint only of the 2D bounds of the 3D
  // component projected onto the desktop. However for expedience
  // we'll currently just repaint the entire desktop to get correct
  // results.
  protected void repaintPortionOfDesktop(JDesktopPane desktop, Component comp) {
    desktop.repaint();
  }
}
