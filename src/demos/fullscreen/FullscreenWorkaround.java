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

package demos.fullscreen;

import java.awt.*;
import javax.swing.*;

import javax.media.opengl.*;

/** Class which implements workaround for full-screen bugs on Windows
    when <code>-Dsun.java2d.noddraw=true</code> is specified as well
    as a similar bug on Mac OS X. This code currently expects that the
    GLAutoDrawable will be placed in a containing Frame. */

public class FullscreenWorkaround implements GLEventListener {
  private int width;
  private int height;

  /** Creates a full-screen workaround with the specified width and
      height to set the full-screen window to later. */
  public FullscreenWorkaround(int width, int height) {
    this.width  = width;
    this.height = height;
  }

  public void init(GLAutoDrawable drawable) {
    // Find parent frame if any
    final Frame frame = getParentFrame((Component) drawable);
    if (frame != null) {
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            frame.setVisible(false);
            frame.setBounds(0, 0, width, height);
            frame.setVisible(true);
            frame.toFront();
          }
        });
    }
  }

  public void display(GLAutoDrawable drawable) {}
  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {}
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static Frame getParentFrame(Component c) {
    while (c != null &&
           (!(c instanceof Frame))) {
      c = c.getParent();
    }
    return (Frame) c;
  }
}
