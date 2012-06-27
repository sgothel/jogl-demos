package com.io7m.example.jogl_mvn_test_2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Test;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class TestJOGL
{
  private static GLWindow makeWindow(
    final String name)
  {
    final GLProfile pro = GLProfile.getDefault();
    final GLCapabilities caps = new GLCapabilities(pro);
    final GLWindow window = GLWindow.create(caps);

    window.setSize(640, 480);
    window.setVisible(true);
    window.setTitle(name);
    window.addWindowListener(new WindowAdapter() {
      @Override public void windowDestroyNotify(
        final WindowEvent e)
      {
        // System.exit(0);
      }
    });
    window.addGLEventListener(new GLEventListener() {
      int quad_x = (int) (Math.random() * 640);
      int quad_y = (int) (Math.random() * 480);

      public void display(
        final GLAutoDrawable drawable)
      {
        System.out.println("thread "
          + Thread.currentThread().getId()
          + " display");

        this.quad_x = (this.quad_x + 1) % 640;
        this.quad_y = (this.quad_y + 1) % 480;

        final GL2 g2 = drawable.getGL().getGL2();
        g2.glClearColor(0.0f, 0.0f, 0.3f, 1.0f);
        g2.glClear(GL.GL_COLOR_BUFFER_BIT);

        g2.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        g2.glLoadIdentity();
        g2.glOrtho(0, 640, 0, 480, 1, 100);
        g2.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        g2.glLoadIdentity();
        g2.glTranslated(0, 0, -1);

        g2.glBegin(GL2.GL_QUADS);
        {
          g2.glVertex2d(this.quad_x, this.quad_y + 10);
          g2.glVertex2d(this.quad_x, this.quad_y);
          g2.glVertex2d(this.quad_x + 10, this.quad_y);
          g2.glVertex2d(this.quad_x + 10, this.quad_y + 10);
        }
        g2.glEnd();
      }

      public void dispose(
        final GLAutoDrawable arg0)
      {
        // TODO Auto-generated method stub
      }

      public void init(
        final GLAutoDrawable arg0)
      {
        // TODO Auto-generated method stub
      }

      public void reshape(
        final GLAutoDrawable arg0,
        final int arg1,
        final int arg2,
        final int arg3,
        final int arg4)
      {
        // TODO Auto-generated method stub
      }
    });

    final FPSAnimator animator = new FPSAnimator(window, 60);
    animator.start();

    return window;
  }

  @Test public void go()
    throws InterruptedException
  {
    final GLWindow window0 = TestJOGL.makeWindow("Window 0");
    Thread.sleep(1000);
  }
}
