package demos.applets;

import java.applet.*;
import java.awt.*;
import demos.gears.Gears;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.GLAnimatorControl;

/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class GearsApplet extends Applet {
  private GLAnimatorControl animator;

  public void init() {
    System.err.println("GearsApplet: init() - begin");
    setLayout(new BorderLayout());
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Gears());
    canvas.setSize(getSize());
    add(canvas, BorderLayout.CENTER);
    animator = new FPSAnimator(canvas, 60);
    System.err.println("GearsApplet: init() - end");
  }

  public void start() {
    System.err.println("GearsApplet: start() - begin");
    animator.start();
    System.err.println("GearsApplet: start() - end");
  }

  public void stop() {
    // FIXME: do I need to do anything else here?
    System.err.println("GearsApplet: stop() - begin");
    animator.stop();
    System.err.println("GearsApplet: stop() - end");
  }

  public void destroy() {
    System.err.println("GearsApplet: destroy() - X");
  }
}
