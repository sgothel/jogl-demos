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

// Internal JOGL API references
import com.sun.opengl.impl.Debug;

/** A subclass of JDesktopPane which performs all of the rendering of
 * its child components into an off-screen buffer. Provides access to
 * this back buffer so subclasses can determine how to draw the
 * contents of the child windows to the screen.
 *
 * @author Kenneth Russell
 */

public class OffscreenDesktopPane extends JDesktopPane {
  //
  // Debugging functionality only
  //
  private static final boolean DEBUG = Debug.debug("OffscreenDesktopPane");
  private static final Color[] colors = {
    Color.LIGHT_GRAY,
    Color.CYAN,
    Color.PINK,
    Color.GRAY,
    Color.MAGENTA,
    Color.BLUE,
    Color.ORANGE,
    Color.DARK_GRAY,
    Color.RED,
    Color.YELLOW
  };
  private int colorIdx;

  private Color getNextColor() {
    Color c = colors[colorIdx];
    colorIdx = (colorIdx + 1) % colors.length;
    return c;
  }

  private Map/*<Component, Color>*/ componentColorMap;

  /** Constructs a new OffscreenDesktopPane. */
  public OffscreenDesktopPane() {
    super();
    if (DEBUG) {
      componentColorMap = new WeakHashMap();
    }
    // Only use an OffscreenDesktopManager instance directly if we
    // have not been subclassed
    if (getClass() == OffscreenDesktopPane.class) {
      setDesktopManager(new OffscreenDesktopManager());
    }
  }

  /** Overrides superclass's addImpl to insert an
      OffscreenComponentWrapper between the added component and this
      one. The wrapper component produces Graphics objects used when
      children repaint themselves directly. */
  protected void addImpl(Component c, Object constraints, int index) {
    if (c instanceof OffscreenComponentWrapper) {
      throw new RuntimeException("Should not add OffscreenComponentWrappers directly");
    }
    OffscreenComponentWrapper wrapper = new OffscreenComponentWrapper(c);
    // Note: this is essential in order to keep mouse events
    // propagating correctly down the hierarchy
    wrapper.setBounds(getBounds());
    OffscreenDesktopManager.switchDoubleBuffering(wrapper, false);
    super.addImpl(wrapper, constraints, index);
    if (DEBUG) {
      componentColorMap.put(c, getNextColor());
    }
    getOffscreenDesktopManager().setNeedsReLayout();
    repaint();
  }

  // In order to hide the presence of the OffscreenComponentWrapper a
  // little more, we override remove to make it look like we can
  // simply pass in the JInternalFrames inside the
  // OffscreenComponentWrappers. There are some situations where we
  // can't hide the presence of the OffscreenComponentWrapper (such as
  // when calling getParent() of the JInternalFrame) so to avoid
  // incorrect behavior of the rest of the toolkit we don't override
  // getComponent() to skip the OffscreenComponentWrappers.

  /** Removes the component at the given index. */
  public void remove(int index) {
    Component c = getComponent(index);
    super.remove(index);
    OffscreenDesktopManager.switchDoubleBuffering(c, true);
  }

  /** Removes the specified component from this
      OffscreenDesktopPane. This method accepts either the components
      added by the application (which are not direct children of this
      one) or the OffscreenComponentWrappers added implicitly by the
      add() method. */
  public void remove(Component comp) {
    comp = getWrapper(comp);
    if (comp == null) {
      // This component must not be one of our children
      return;
    }
    super.remove(comp);
    OffscreenDesktopManager.switchDoubleBuffering(comp, true);
  }

  public void reshape(int x, int y, int w, int h) {
    super.reshape(x, y, w, h);
    Rectangle rect = new Rectangle(x, y, w, h);
    Component[] cs = getComponents();
    for (int i = 0; i < cs.length; i++) {
      // Note: this is essential in order to keep mouse events
      // propagating correctly down the hierarchy
      ((OffscreenComponentWrapper) cs[i]).setBounds(rect);
    }
  }

  /** Overridden from JLayeredPane for convenience when manipulating
      child components. Accepts either the component added by the
      application or the OffscreenComponentWrapper added implicitly by
      the add() method. */
  public void setPosition(Component c, int position) {
    super.setPosition(getWrapper(c), position);
  }

  /** Paints all children of this OffscreenDesktopPane to the internal
      off-screen buffer. Does no painting to the passed Graphics
      object; this is the responsibility of subclasses. */
  protected void paintChildren(Graphics g) {
    // Update desktop manager's offscreen buffer if necessary
    getOffscreenDesktopManager().updateOffscreenBuffer(this);

    if (DEBUG) {
      // Subclasses will need to override this behavior anyway, so
      // only enable an on-screen representation if debugging is
      // enabled. For now, simply paint colored rectangles indicating
      // the on-screen locations of the windows.
      final Component[] components = getRealChildComponents();
      int compCount = components.length;
      for (int i = compCount - 1; i >= 0; i--) {
        Component c = components[i];
        Rectangle r = c.getBounds();
        Color col = (Color) componentColorMap.get(c);
        g.setColor(col);
        g.fillRect(r.x, r.y, r.width, r.height);
      }
    }
  }

  /** Fetches the real child components of this OffscreenDesktopPane,
      skipping all OffscreenComponentWrappers implicitly added. */
  protected Component[] getRealChildComponents() {
    Component[] cs = getComponents();
    for (int i = 0; i < cs.length; i++) {
      cs[i] = ((OffscreenComponentWrapper) cs[i]).getChild();
    }
    return cs;
  }

  /** Returns the OffscreenDesktopManager associated with this
      pane. */
  public OffscreenDesktopManager getOffscreenDesktopManager() {
    return (OffscreenDesktopManager) getDesktopManager();
  }

  /** Returns the real child component of this OffscreenDesktopPane,
      skipping the OffscreenComponentWrapper implicitly added. */
  protected static Component getRealComponent(Component c) {
    if (c instanceof OffscreenComponentWrapper) {
      return ((OffscreenComponentWrapper) c).getChild();
    }    
    return c;
  }

  /** Returns the OffscreenComponentWrapper corresponding to the given
      child component, or the passed component if it is already the
      wrapper. */
  protected static Component getWrapper(Component c) {
    if (c instanceof OffscreenComponentWrapper) {
      return c;
    }
    Component parent = c.getParent();
    if (parent instanceof OffscreenComponentWrapper) {
      return parent;
    }
    return null;
  }
}
