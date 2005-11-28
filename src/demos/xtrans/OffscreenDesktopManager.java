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

// Internal JOGL API references
import com.sun.opengl.impl.Debug;
// FIXME: debugging only
import com.sun.opengl.impl.Java2D;

// FIXME: we need a way to lock a portion of the off-screen back
// buffer to be persistent for a while during component removals. It
// turns out that the removal process of JInternalFrames "mostly"
// works OK with temporarily preserving a region of the back buffer
// but in the case of redraws of the target component occurring after
// it has been removed (such as in the case of a GLJPanel being
// animated) we need to preserve that region.

/** A DesktopManager implementation supporting off-screen rendering
 * and management of components' images for later compositing.
 *
 * @author Kenneth Russell
 */

public class OffscreenDesktopManager implements DesktopManager {
  protected final static String HAS_BEEN_ICONIFIED_PROPERTY = "wasIconOnce";

  protected VolatileImage offscreenBackBuffer;

  // Verious dirty states.
  //
  // STATE_CLEAN indicates that the OpenGL texture is in sync with the
  // VolatileImage back buffer and that no (relatively expensive) copy
  // back operations need be performed.
  //
  // STATE_COPY_BACK indicates that the VolatileImage back buffer must
  // be copied back e.g. into an OpenGL texture, but that the back
  // buffer's contents are clean.
  //
  // STATE_REDRAW indicates that all components need to be repainted
  // on the back buffer. STATE_REDRAW implies STATE_COPY_BACK.
  //
  // STATE_RELAYOUT is the most expensive state, in which all
  // components are re-laid out on the back buffer and repainted
  // completely. This implies STATE_REDRAW, which in turn implies
  // STATE_COPY_BACK.
  public static final int STATE_CLEAN     = 0;
  public static final int STATE_COPY_BACK = 1;
  public static final int STATE_REDRAW    = 2;
  public static final int STATE_RELAYOUT  = 3;

  // We start off assuming that we need to lay out everything
  protected int dirtyState = STATE_RELAYOUT;

  protected Map/*<Component, Rectangle>*/ componentPositionsOnBackBuffer = new WeakHashMap();

  // For debugging
  private static final boolean DEBUG = Debug.debug("OffscreenDesktopManager");
  private static final boolean VERBOSE = Debug.verbose();
  private JFrame debuggingFrame;
  private JPanel debuggingPanel;

  public OffscreenDesktopManager() {
    if (DEBUG) {
      debuggingFrame = new JFrame("Debugging frame");
      debuggingPanel = new JPanel() {
          public void paintComponent(Graphics g) {
            if (offscreenBackBuffer != null) {
              g.drawImage(offscreenBackBuffer, 0, 0, null);
            }
          }
        };
      debuggingPanel.setDoubleBuffered(false);
      debuggingFrame.getContentPane().add(debuggingPanel);
      debuggingFrame.pack();
      debuggingFrame.setLocation(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() / 2, 0);
      debuggingFrame.setSize(256, 256);
      debuggingFrame.setVisible(true);
    }
  }

  /** Sets the state bit in the desktop manager indicating that the
      offscreen texture has changed and may need to be copied back
      e.g. to an OpenGL texture. */
  public void setNeedsCopyBack() {
    dirtyState = Math.max(dirtyState, STATE_COPY_BACK);
  }

  /** Sets the state bit in the desktop manager indicating that the
      child components need to be redrawn to the off-screen buffer. */
  public void setNeedsRedraw() {
    dirtyState = Math.max(dirtyState, STATE_REDRAW);
  }

  /** Sets the state bit in the desktop manager indicating that the
      components need to be re-laid out on the off-screen back buffer.
      This implies that all of these components need to be repainted
      and also implies that the off-screen back buffer may need to be
      copied back (needsCopyBack()). */
  public void setNeedsReLayout() {
    dirtyState = Math.max(dirtyState, STATE_RELAYOUT);
  }

  /** Returns the state bit in the desktop manager indicating that the
      offscreen texture has changed and may need to be copied back
      e.g. to an OpenGL texture. */
  public boolean needsCopyBack() {
    return (dirtyState >= STATE_COPY_BACK);
  }

  /** Returns the state bit in the desktop manager indicating that the
      child components need to be redrawn to the off-screen buffer. */
  public boolean needsRedraw() {
    return (dirtyState >= STATE_REDRAW);
  }

  /** Returns the state bit in the desktop manager indicating that the
      components need to be re-laid out on the off-screen back buffer.
      This implies that all of these components need to be repainted
      and also implies that the off-screen back buffer may need to be
      copied back (needsCopyBack()). */
  public boolean needsReLayout() {
    return (dirtyState >= STATE_RELAYOUT);
  }

  /** Returns the default graphics configuration of the default screen
      device for the local graphics environment. */
  protected GraphicsConfiguration getDefaultConfiguration() {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
  }

  /** Fetches the Graphics object corresponding to the off-screen back
      buffer. */
  public Graphics getOffscreenGraphics() {
    return offscreenBackBuffer.getGraphics();
  }

  /** Fetches the Image being used as the back buffer. */
  public Image getOffscreenBackBuffer() {
    return offscreenBackBuffer;
  }

  /** Returns the width of the off-screen back buffer at this point in
      time, or -1 if it has not been created yet. */
  public int getOffscreenBackBufferWidth() {
    return offscreenBackBuffer.getWidth();
  }

  /** Returns the height of the off-screen back buffer at this point
      in time, or -1 if it has not been created yet. */
  public int getOffscreenBackBufferHeight() {
    return offscreenBackBuffer.getHeight();
  }

  /** Fetches the Rectangle corresponding to the bounds of the given
      child component on the off-screen back buffer. This portion of
      the back buffer can be drawn manually by the end user to display
      the given component. */
  public Rectangle getBoundsOnBackBuffer(Component c) {
    return (Rectangle) componentPositionsOnBackBuffer.get(c);
  }

  /** Updates the layouts of the components on the off-screen back
      buffer without repainting the back buffer. This should always be
      called after adding, removing or resizing child components. It
      is called automatically by {@link #updateOffscreenBuffer
      updateOffscreenBuffer}. */
  public void layoutOffscreenBuffer(OffscreenDesktopPane parent) {
    if (needsReLayout()) {
      // Must do the following:
      // 1. Lay out the desktop pane's children on the off-screen back
      //    buffer, keeping track of where they went
      // 2. Draw the children to the off-screen buffer
      // (Not done here: 3. Use JOGL to copy the off-screen buffer to a
      //   texture for later consumption by the XTDesktopPane)

      //////////////////////////////////////////////////////////////////
      //                                                              //
      // FIXME: must use a more efficient packing algorithm than this //
      //                                                              //
      //////////////////////////////////////////////////////////////////

      // NOTE: this is the rectangle packing problem, which is NP-hard.
      // We could go to arbitrary lengths in order to improve the
      // efficiency of this packing. Ideally we would like to minimize
      // wasted space and (probably) shoot for a somewhat-square layout.
      // Because currently we're just trying to get things working,
      // we're going to do the simplest layout possible: just line
      // things up left-to-right.
      int maxHeight = -1;
      int widthSum  = 0;

      // Two-pass algorithm, one getting maximum height and summing
      // widths, and one laying things out
      for (int i = 0; i < parent.getComponentCount(); i++) {
        Component c = ((OffscreenComponentWrapper) parent.getComponent(i)).getChild();
        int w = c.getWidth();
        int h = c.getHeight();
        maxHeight = Math.max(maxHeight, h);
        widthSum += w;
      }
      int curX = 0;
      for (int i = 0; i < parent.getComponentCount(); i++) {
        Component c = ((OffscreenComponentWrapper) parent.getComponent(i)).getChild();
        int w = c.getWidth();
        int h = c.getHeight();
        Rectangle r = new Rectangle(curX, 0, w, h);
        componentPositionsOnBackBuffer.put(c, r);
        curX += w;
      }

      // Re-create off-screen buffer if necessary
      int offscreenWidth  = nextPowerOf2(widthSum);
      int offscreenHeight = nextPowerOf2(maxHeight);
      if ((offscreenBackBuffer == null) ||
          (offscreenWidth != offscreenBackBuffer.getWidth()) ||
          (offscreenHeight != offscreenBackBuffer.getHeight())) {
        if (offscreenBackBuffer != null) {
          offscreenBackBuffer.flush();
        }
        offscreenBackBuffer =
          getDefaultConfiguration().createCompatibleVolatileImage(offscreenWidth,
                                                                  offscreenHeight);
      }

      if (DEBUG) {
        debuggingPanel.setPreferredSize(new Dimension(offscreenWidth, offscreenHeight));
        debuggingFrame.setSize(offscreenWidth + 10, offscreenHeight + 30);
      }

      dirtyState = STATE_REDRAW;
    }
  }

  /** Updates the image on the off-screen back buffer. This should
      always be called before attempting to draw the child components'
      contents on the screen. If the child components' states are
      clean, this method does nothing. Note that this changes the
      state bits back to clean, so subclasses should capture the
      current state before calling the superclass implementation. */
  public void updateOffscreenBuffer(OffscreenDesktopPane parent) {
    if (!needsCopyBack()) {
      // Cleanest possible state
      return;
    }

    layoutOffscreenBuffer(parent);

    boolean validated = false;
    boolean done = false;
    while (!done) {
      if (needsRedraw()) {
        boolean redrawn = false;

        do {
          // Validate it
          int res = offscreenBackBuffer.validate(getDefaultConfiguration());
          if (!((res == VolatileImage.IMAGE_OK) ||
                (res == VolatileImage.IMAGE_RESTORED))) {
            // FIXME: fail more gracefully
            throw new RuntimeException("Unable to validate VolatileImage");
          }
          validated = true;

          // Lay out and render components
          final Graphics g = offscreenBackBuffer.getGraphics();
          int curX = 0;
          for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = ((OffscreenComponentWrapper) parent.getComponent(i)).getChild();

            if (c.isVisible()) {
              // Ensure this component and all children have double
              // buffering disabled to prevent incorrect rendering results.
              // Should try to make this more efficient, but doesn't look
              // like there's any way to listen for setDoubleBuffered
              // changes; could however listen for hierarchy change events,
              // which are more likely to break things, but these are
              // expensive to deliver as well
              switchDoubleBuffering(c, false);

              // NOTE: should probably be smarter about this and only
              // paint components which really need it (consult
              // RepaintManager?). However, experimentation has shown
              // that at this point the RepaintManager is already in
              // the process of painting the child components and its
              // notion of the dirty regions has already been cleared.

              Rectangle r = (Rectangle) componentPositionsOnBackBuffer.get(c);
              if (r == null) {
                // May be bug or race condition; for now just skip this one
                continue;
              }
              Graphics g2 = g.create();
              if (DEBUG && VERBOSE) {
                System.err.println("Translating Graphics to (" + r.x + "," + r.y + ")");
                System.err.println("  Surface identifier = " + Java2D.getOGLSurfaceIdentifier(g2));
              }
              g2.translate(r.x, r.y);
              c.paint(g2);
              g2.dispose();
            }
          }
          g.dispose();
          if (!offscreenBackBuffer.contentsLost()) {
            redrawn = true;
            done = true;
          }
        } while (!redrawn);
      }

      // If we didn't need to re-layout and draw the components, we
      // might still need to copy back their results to an OpenGL
      // texture. Subclasses should override this method to do
      // additional work afterward.

      if (!validated) {
        int res = offscreenBackBuffer.validate(getDefaultConfiguration());
        if (!((res == VolatileImage.IMAGE_OK) ||
              (res == VolatileImage.IMAGE_RESTORED))) {
          // FIXME: fail more gracefully
          throw new RuntimeException("Unable to validate VolatileImage");
        }
        if (res == VolatileImage.IMAGE_RESTORED) {
          // The contents were blown away since the time of the last
          // render, so force a re-render
          setNeedsRedraw();
        } else {
          done = true;
        }
      }
    }

    dirtyState = STATE_CLEAN;

    // Subclasses would do the copy back here

    if (DEBUG) {
      debuggingPanel.repaint();
    }
  }

  public void openFrame(JInternalFrame f) {
    if(getDesktopPaneParent(f.getDesktopIcon()) != null) {
      getDesktopPaneParent(f.getDesktopIcon()).add(f);
      removeIconFor(f);
    }

    setNeedsReLayout();
  }

  public void closeFrame(JInternalFrame f) {
    boolean findNext = f.isSelected();
    JDesktopPane c = getDesktopPaneParent(f);
    if (findNext)
      try { f.setSelected(false); } catch (PropertyVetoException e2) { }
    if(c != null) {
      c.remove(f.getParent());
      repaintPortionOfDesktop(c, f);
    }
    removeIconFor(f);
    if(f.getNormalBounds() != null)
      f.setNormalBounds(null);
    if(wasIcon(f))
      setWasIcon(f, null);
    if (findNext) activateNextFrame(c);

    setNeedsReLayout();
  }

  public void maximizeFrame(JInternalFrame f) {
    if (f.isIcon()) {
      try {
        // In turn calls deiconifyFrame in the desktop manager.
        // That method will handle the maximization of the frame.
        f.setIcon(false);
      } catch (PropertyVetoException e2) {
      }
    } else {
      f.setNormalBounds(f.getBounds());
      Rectangle desktopBounds = getDesktopPaneParent(f).getBounds();
      setBoundsForFrame(f, 0, 0,
                        desktopBounds.width, desktopBounds.height);
    }

    // Set the maximized frame as selected.
    try {
      f.setSelected(true);
    } catch (PropertyVetoException e2) {
    }

    setNeedsReLayout();
  }

  public void minimizeFrame(JInternalFrame f) {
    // If the frame was an icon restore it back to an icon.
    if (f.isIcon()) {
      iconifyFrame(f);
      return;
    }

    if ((f.getNormalBounds()) != null) {
      Rectangle r = f.getNormalBounds();
      f.setNormalBounds(null);
      try { f.setSelected(true); } catch (PropertyVetoException e2) { }
      setBoundsForFrame(f, r.x, r.y, r.width, r.height);
    }

    setNeedsReLayout();
  }

  public void iconifyFrame(JInternalFrame f) {
    JInternalFrame.JDesktopIcon desktopIcon;
    Container c = getDesktopPaneParent(f);
    JDesktopPane d = f.getDesktopPane();
    boolean findNext = f.isSelected();

    desktopIcon = f.getDesktopIcon();
    if(!wasIcon(f)) {
      Rectangle r = getBoundsForIconOf(f);
      desktopIcon.setBounds(r.x, r.y, r.width, r.height);
      setWasIcon(f, Boolean.TRUE);
    }

    if (c == null) {
      return;
    }

    if (c instanceof JLayeredPane) {
      JLayeredPane lp = (JLayeredPane)c;
      int layer = lp.getLayer(f);
      lp.putLayer(desktopIcon, layer);
    }

    // If we are maximized we already have the normal bounds recorded
    // don't try to re-record them, otherwise we incorrectly set the
    // normal bounds to maximized state.
    if (!f.isMaximum()) {
      f.setNormalBounds(f.getBounds());
    }
    c.remove(f);
    c.add(desktopIcon);
    try {
      f.setSelected(false);
    } catch (PropertyVetoException e2) {
    }

    // Get topmost of the remaining frames
    if (findNext) {
      activateNextFrame(c);
    }

    setNeedsReLayout();
  }

  protected void activateNextFrame(Container c) {
    int i;
    JInternalFrame nextFrame = null;
    if (c == null) return;
    for (i = 0; i < c.getComponentCount(); i++) {
      if (c.getComponent(i) instanceof JInternalFrame) {
        nextFrame = (JInternalFrame) c.getComponent(i);
        break;
      }
    }
    if (nextFrame != null) {
      try { nextFrame.setSelected(true); }
      catch (PropertyVetoException e2) { }
      moveToFront(nextFrame);
    }
    else {
      c.requestFocus();
    }

    // This operation will change the graphic contents of the
    // offscreen buffer but not the positions of any of the windows
    setNeedsCopyBack();
  }

  public void deiconifyFrame(JInternalFrame f) {
    JInternalFrame.JDesktopIcon desktopIcon = f.getDesktopIcon();
    Container c = getDesktopPaneParent(desktopIcon);
    if (c != null) {
      c.add(f);
      // If the frame is to be restored to a maximized state make
      // sure it still fills the whole desktop.
      if (f.isMaximum()) {
        Rectangle desktopBounds = c.getBounds();
        if (f.getWidth() != desktopBounds.width ||
            f.getHeight() != desktopBounds.height) {
          setBoundsForFrame(f, 0, 0,
                            desktopBounds.width, desktopBounds.height);
        }
      }
      removeIconFor(f);
      if (f.isSelected()) {
        moveToFront(f);
      } else {
        try {
          f.setSelected(true);
        } catch (PropertyVetoException e2) {
        }
      }
    }

    setNeedsReLayout();
  }

  public void activateFrame(JInternalFrame f) {
    Container p = getDesktopPaneParent(f);
    Component[] c;
    JDesktopPane d = f.getDesktopPane();
    JInternalFrame currentlyActiveFrame =
      (d == null) ? null : d.getSelectedFrame();
    // fix for bug: 4162443
    if(p == null) {
      // If the frame is not in parent, its icon maybe, check it
      p = getDesktopPaneParent(f.getDesktopIcon());
      if(p == null)
        return;
    }
    // we only need to keep track of the currentActive InternalFrame, if any
    if (currentlyActiveFrame == null){
      if (d != null) { d.setSelectedFrame(f);}
    } else if (currentlyActiveFrame != f) {  
      // if not the same frame as the current active
      // we deactivate the current 
      if (currentlyActiveFrame.isSelected()) { 
        try {
          currentlyActiveFrame.setSelected(false);
        }
        catch(PropertyVetoException e2) {}
      }
      if (d != null) { d.setSelectedFrame(f);}
    }
    moveToFront(f);

    // This operation will change the graphic contents of the
    // offscreen buffer but not the positions of any of the windows
    //    setNeedsCopyBack();
    setNeedsRedraw();

    repaintPortionOfDesktop(f);
  }

  public void deactivateFrame(JInternalFrame f) {
    JDesktopPane d = f.getDesktopPane();
    JInternalFrame currentlyActiveFrame =
      (d == null) ? null : d.getSelectedFrame();
    if (currentlyActiveFrame == f)
      d.setSelectedFrame(null);

    // This operation will change the graphic contents of the
    // offscreen buffer but not the positions of any of the windows
    setNeedsRedraw();

    repaintPortionOfDesktop(f);
  }

  public void beginDraggingFrame(JComponent f) {
    // Nothing to do any more because the DesktopPane handles this by
    // redrawing from the off-screen buffer
  }

  public void dragFrame(JComponent f, int newX, int newY) {
    f.setLocation(newX, newY);
    repaintPortionOfDesktop(f);
  }
  
  public void endDraggingFrame(JComponent f) {
    // NOTE: nothing to do any more because OffscreenDesktopPane
    // subclasses handle this
  }
  
  public void beginResizingFrame(JComponent f, int direction) {
  }

  public void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
    setBoundsForFrame(f, newX, newY, newWidth, newHeight);
    repaintPortionOfDesktop(f);
  }

  public void endResizingFrame(JComponent f) {
    repaintPortionOfDesktop(f);
  }

  public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
    boolean didResize = (f.getWidth() != newWidth || f.getHeight() != newHeight);
    f.setBounds(newX, newY, newWidth, newHeight);
    if(didResize) {
      f.validate();
    } 
    setNeedsReLayout();
  }

  protected void removeIconFor(JInternalFrame f) {
    JInternalFrame.JDesktopIcon di = f.getDesktopIcon();
    JDesktopPane c = getDesktopPaneParent(di);
    if(c != null) {
      c.remove(di);
      repaintPortionOfDesktop(c, di);
    }
  }

  protected Rectangle getBoundsForIconOf(JInternalFrame f) {
    //
    // Get the icon for this internal frame and its preferred size
    //

    JInternalFrame.JDesktopIcon icon = f.getDesktopIcon();
    Dimension prefSize = icon.getPreferredSize();
    //
    // Get the parent bounds and child components.
    //

    Container c = getDesktopPaneParent(f);
    if (c == null) {
      c = getDesktopPaneParent(f.getDesktopIcon());
    }

    if (c == null) {
      /* the frame has not yet been added to the parent; how about (0,0) ?*/
      return new Rectangle(0, 0, prefSize.width, prefSize.height);
    }
	
    Rectangle parentBounds = c.getBounds();
    Component [] components = c.getComponents();


    //
    // Iterate through valid default icon locations and return the
    // first one that does not intersect any other icons.
    //

    Rectangle availableRectangle = null;
    JInternalFrame.JDesktopIcon currentIcon = null;

    int x = 0;
    int y = parentBounds.height - prefSize.height;
    int w = prefSize.width;
    int h = prefSize.height;

    boolean found = false;

    while (!found) {

      availableRectangle = new Rectangle(x,y,w,h);

      found = true;

      for ( int i=0; i<components.length; i++ ) {

        //
        // Get the icon for this component
        //

        if ( components[i] instanceof JInternalFrame ) {
          currentIcon = ((JInternalFrame)components[i]).getDesktopIcon();
        }
        else if ( components[i] instanceof JInternalFrame.JDesktopIcon ){
          currentIcon = (JInternalFrame.JDesktopIcon)components[i];
        } else
          /* found a child that's neither an internal frame nor
             an icon. I don't believe this should happen, but at
             present it does and causes a null pointer exception.
             Even when that gets fixed, this code protects against
             the npe. hania */
          continue;
	  
        //
        // If this icon intersects the current location, get next location.
        //

        if ( !currentIcon.equals(icon) ) {
          if ( availableRectangle.intersects(currentIcon.getBounds()) ) {
            found = false;
            break;
          }
        }
      }

      if (currentIcon == null)
        /* didn't find any useful children above. This probably shouldn't
	   happen, but this check protects against an npe if it ever does
	   (and it's happening now) */
        return availableRectangle;

      x += currentIcon.getBounds().width;

      if ( x + w > parentBounds.width ) {
        x = 0;
        y -= h;
      }
    }
	    
    return(availableRectangle);
  }

  protected void setWasIcon(JInternalFrame f, Boolean value)  {
    if (value != null) {
      f.putClientProperty(HAS_BEEN_ICONIFIED_PROPERTY, value);
    }
  }

  protected boolean wasIcon(JInternalFrame f) {
    return (f.getClientProperty(HAS_BEEN_ICONIFIED_PROPERTY) == Boolean.TRUE);
  }

  protected JDesktopPane getDesktopPaneParent(Component f) {
    Container c = f.getParent();
    if (c == null) {
      return null;
    }
    if (!(c instanceof OffscreenComponentWrapper)) {
      throw new RuntimeException("Illegal component structure");
    }
    Container parent = c.getParent();
    if (parent == null) {
      return null;
    }
    if (!(parent instanceof JDesktopPane)) {
      throw new RuntimeException("Illegal component structure");
    }
    return (JDesktopPane) parent;
  }

  private void moveToFront(Component f) {
    Container c = getDesktopPaneParent(f);
    if (c instanceof JDesktopPane) {
      ((JDesktopPane) c).moveToFront(f.getParent());
    }
  }

  private static int nextPowerOf2(int number) {
    // Workaround for problems where 0 width or height are transiently
    // seen during layout
    if (number == 0) {
      return 2;
    }

    if (((number-1) & number) == 0) {
      //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
      return number;
    }
    int power = 0;
    while (number > 0) {
      number = number>>1;
      power++;
    }
    return (1<<power);
  }

  /** Repaints the portion of the desktop pane parent corresponding to
      the given component. */
  protected void repaintPortionOfDesktop(Component comp) {
    repaintPortionOfDesktop(getDesktopPaneParent(comp), comp);
  }

  /** Repaints the portion of the passed desktop pane corresponding to
      the given component. */
  protected void repaintPortionOfDesktop(JDesktopPane desktop, Component comp) {
    // Indicate to the desktop pane that a certain portion of the
    // on-screen representation is dirty. The desktop can map these
    // coordinates to the positions of the windows if they are
    // different.
    Rectangle r = comp.getBounds();
    desktop.repaint(r.x, r.y, r.width, r.height);
  }  

  /** Helper function to force the double-buffering of an entire
      component hierarchy to the on or off state. */
  public static void switchDoubleBuffering(Component root, boolean val) {
    if (root instanceof JComponent) {
      ((JComponent) root).setDoubleBuffered(val);
    }
    if (root instanceof Container) {
      Container container = (Container) root;
      for (int i = 0; i < container.getComponentCount(); i++) {
        switchDoubleBuffering(container.getComponent(i), val);
      }
    }
  }
}
