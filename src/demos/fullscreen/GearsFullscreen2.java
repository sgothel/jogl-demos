package demos.fullscreen;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import demos.gears.Gears;
import demos.util.*;

/**
 * GearsFullscreen2.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Goethel) <P>
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21 <P>
 *
 * Illustrates more complex usage of GLCanvas in full-screen mode. On
 * Windows this demo should be run with the system property
 * -Dsun.java2d.noddraw=true specified to prevent Java2D from using
 * DirectDraw, which is incompatible with OpenGL at the driver level.
 */

public class GearsFullscreen2 {
  private GraphicsDevice dev;
  private DisplayMode origMode;
  private boolean fullScreen;
  private JFrame frame;
  private Animator animator;
  private int initWidth = 300;
  private int initHeight = 300;

  public static void main(String[] args) {
    new GearsFullscreen2().run(args);
  }

  public void run(String[] args) {
    dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    origMode = dev.getDisplayMode();
    DisplayMode newMode = null;

    if (dev.isFullScreenSupported()) {
      newMode = ScreenResSelector.showSelectionDialog();
      if (newMode != null) {
        initWidth = newMode.getWidth();
        initHeight = newMode.getHeight();
      }
    } else {
      System.err.println("NOTE: full-screen mode not supported; running in window instead");
    }

    frame = new JFrame("Gear Demo");
    if (newMode != null) {
      frame.setUndecorated(true);
    }
    GLCanvas canvas = new GLCanvas();

    canvas.addGLEventListener(new Gears());
    canvas.addGLEventListener(new FullscreenWorkaround(initWidth, initHeight));
    frame.getContentPane().setLayout(new BorderLayout());

    ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

    JButton button = new JButton("West");
    button.setToolTipText("West ToolTip");
    frame.getContentPane().add(button, BorderLayout.WEST);

    button = new JButton("East");
    button.setToolTipText("East ToolTip");
    frame.getContentPane().add(button, BorderLayout.EAST);

    button = new JButton("North");
    button.setToolTipText("North ToolTip");
    frame.getContentPane().add(button, BorderLayout.NORTH);

    button = new JButton("South");
    button.setToolTipText("South ToolTip");
    frame.getContentPane().add(button, BorderLayout.SOUTH);

    frame.getContentPane().add(canvas, BorderLayout.CENTER);
    frame.setSize(initWidth, initHeight);
    animator = new Animator(canvas);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          runExit();
        }
      });
    frame.setVisible(true);

    if (dev.isFullScreenSupported() && (newMode != null)) {
      dev.setFullScreenWindow(frame);
      if (dev.isDisplayChangeSupported()) {
        dev.setDisplayMode(newMode);
        fullScreen = true;
      } else {
        // Not much point in having a full-screen window in this case
        dev.setFullScreenWindow(null);
        final Frame f2 = frame;
        try {
          EventQueue.invokeAndWait(new Runnable() {
              public void run() {
                f2.setVisible(false);
                f2.setUndecorated(false);
                f2.setVisible(true);
                f2.setSize(initWidth, initHeight);
              }
            });
        } catch (Exception e) {
          e.printStackTrace();
        }
        System.err.println("NOTE: was not able to change display mode; full-screen disabled");
      }
    }

    animator.start();
  }

  public void runExit() {
    // Run this on another thread than the AWT event queue to
    // make sure the call to Animator.stop() completes before
    // exiting
    new Thread(new Runnable() {
        public void run() {
          animator.stop();
          try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                  if (fullScreen) {
                    try {
                      dev.setDisplayMode(origMode);
                    } catch (Exception e1) {
                    }
                    try {
                      dev.setFullScreenWindow(null);
                    } catch (Exception e2) {
                    }
                    fullScreen = false;
                  }
                }
              });
          } catch (Exception e) {
            e.printStackTrace();
          }
          System.exit(0);
        }
      }).start();
  }
}
