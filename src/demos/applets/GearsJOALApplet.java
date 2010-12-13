package demos.applets;

import demos.gears.Gears;
import java.applet.Applet;
import java.awt.EventQueue;
import java.awt.GridLayout;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import javax.swing.JPanel;
import com.jogamp.openal.util.ALut;
import demos.joal.SingleStaticSource;
import javax.media.opengl.GLAnimatorControl;

/** Shows how to deploy an applet using both JOGL and JOAL. This demo
    must be referenced from a web page via an &lt;applet&gt; tag. */

public class GearsJOALApplet extends Applet {
  private GLAnimatorControl animator;

  public void init() {
    GLProfile.initSingleton(false);
    setLayout(new GridLayout(1, 2));
    GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Gears());
    canvas.setSize(getSize());
    add(canvas);
    JPanel joalDemoParent = new JPanel();
    SingleStaticSource joalDemo = new SingleStaticSource(true, joalDemoParent, false);
    add(joalDemoParent);
    animator = new FPSAnimator(canvas, 60);
  }

  public void start() {
    animator.start();
  }

  public void stop() {
    // FIXME: do I need to do anything else here?
    animator.stop();
    // Note that the SingleStaticSource demo does an alutInit()
    // internally (on the Event Dispatch Thread), so we should do an
    // alutExit() ourselves
    try {
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            ALut.alutExit();
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
