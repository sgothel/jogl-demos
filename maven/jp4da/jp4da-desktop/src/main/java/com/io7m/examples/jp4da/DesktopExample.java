package com.io7m.examples.jp4da;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class DesktopExample implements Runnable
{
  public static void main(
    final String args[])
  {
    final DesktopExample de = new DesktopExample(new Example());
    de.run();
  }

  private final GLWindow    window;
  private final FPSAnimator animator;

  private DesktopExample(
    final GLEventListener gle)
  {
    final GLProfile pro = GLProfile.get(GLProfile.GL2GL3);
    final GLCapabilities cap = new GLCapabilities(pro);

    this.window = GLWindow.create(cap);
    this.window.setSize(640, 480);
    this.window.setTitle("Test1");
    this.window.addGLEventListener(gle);
    this.window.setVisible(true);

    this.animator = new FPSAnimator(60);
    this.animator.setUpdateFPSFrames(60, System.err);
    this.animator.add(this.window);
    this.animator.start();
  }

  @Override public void run()
  {
    try {
      while (this.animator.isAnimating() && this.window.isVisible()) {
        Thread.sleep(100);
      }

      this.animator.stop();
      this.window.destroy();

      System.err.println("Exiting...");
    } catch (final InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
