package demos.xtrans;

import java.awt.*;
import javax.swing.*;

// Internal JOGL API references
import com.sun.opengl.impl.Debug;
// FIXME: debugging only
import com.sun.opengl.impl.Java2D;

/** Provides an interposition point where we can install a new
    Graphics object in the rendering pipeline. Because lightweight
    components always delegate up to their parents to fetch Graphics
    objects, we can use this point to swap in the off-screen
    VolatileImage we're using for rendering. Applications should not
    need to construct instances of this class directly, though they
    may encounter them when traversing the component hierarchy created
    by the OffscreenDesktopPane, since they are automatically inserted
    during add operations. */

public class OffscreenComponentWrapper extends JComponent {
  private static final boolean DEBUG = Debug.debug("OffscreenComponentWrapper");

  /** Instantiates an OffscreenComponentWrapper. This should not be
      called directly by applications. */
  public OffscreenComponentWrapper(Component arg) {
    if (arg == null) {
      throw new RuntimeException("Null argument");
    }
    add(arg);
    setOpaque(false);
  }

  protected void addImpl(Component c, Object constraints, int index) {
    if (getComponentCount() != 0) {
      throw new RuntimeException("May only add one child");
    }
    super.addImpl(c, constraints, index);
  }

  /** Returns the sole child component of this one. */
  public Component getChild() {
    if (getComponentCount() == 0) {
      throw new RuntimeException("No child found");
    }
    return getComponent(0);
  }

  public void remove(int i) {
    super.remove(i);
    throw new RuntimeException("Should not call this");
  }

  public void remove(Component c) {
    super.remove(c);
    throw new RuntimeException("Should not call this");
  }

  /** Overrides the superclass's getGraphics() in order to provide a
      correctly-translated Graphics object on the
      OffscreenDesktopPane's back buffer. */
  public Graphics getGraphics() {
    Component parent = getParent();
    if ((parent != null) && (parent instanceof OffscreenDesktopPane)) {
      OffscreenDesktopPane desktop = (OffscreenDesktopPane) parent;
      OffscreenDesktopManager manager = (OffscreenDesktopManager) desktop.getDesktopManager();
      // Find out where the component we're rendering lives on the back buffer
      Graphics g = manager.getOffscreenGraphics();
      Rectangle bounds = manager.getBoundsOnBackBuffer(getChild());
      if (bounds == null) {
        if (DEBUG) {
          System.err.println("No bounds for child");
        }

        // Not yet laid out on back buffer; however, avoid painting to
        // screen
        return g;
      }
      if (DEBUG) {
        System.err.println("Graphics.translate(" + bounds.x + "," + bounds.y + ")");
        System.err.println("  Surface identifier = " + Java2D.getOGLSurfaceIdentifier(g));
      }

      g.translate(bounds.x, bounds.y);
      // Also take into account the translation of the child
      Component c = getChild();
      g.translate(-c.getX(), -c.getY());
      // Make sure any changes the user made to the double-buffering
      // of child components don't mess up the rendering results
      OffscreenDesktopManager.switchDoubleBuffering(this, false);
      // A child component will be performing drawing and therefore
      // get the OpenGL copy of the back buffer out of sync; will need
      // to repair it
      // NOTE: in theory we should only need to set the copy back
      // state on the OffscreenDesktopManager. However it seems that
      // there are certain operations (like scrolling in a
      // JScrollPane) that have unanticipated consequences, such as
      // drawing into the Swing back buffer (which is heavily
      // undesirable, because it can cause on-screen visual artifacts
      // in our rendering paradigm) as well as potentially
      // inadvertently copying regions of the back buffer from one
      // place to another. Since we don't know the side effects of
      // such operations we currently need to treat the entire back
      // buffer as being dirty.
      manager.setNeedsRedraw();
      // NOTE: this is a bit of a hack but we need to indicate to the
      // parent desktop that it is dirty as well since one of its
      // children (being painted by alternate means) needs to be
      // redrawn
      desktop.repaint();
      return g;
    } else {
      return super.getGraphics();
    }
  }

  public void paintComponent(Graphics g) {
  }

  protected void paintChildren(Graphics g) {
  }
}
