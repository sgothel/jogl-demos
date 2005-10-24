package demos.xtrans;

import javax.media.opengl.*;

/** Specifies the interface by which a transition is updated and drawn
    by the XTDesktopPane. */

public interface XTTransition {
  /** Updates this transition's state to the given fraction in its
      animation cycle (0.0 - 1.0). */
  public void update(float fraction);

  /** Draws this transition using the passed OpenGL object. */
  public void draw(GL gl);
}
