package com.io7m.examples.jp4da;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class Example implements GLEventListener
{
  @Override public void display(
    final GLAutoDrawable drawable)
  {
    final GL2ES2 gl = drawable.getGL().getGL2ES2();

    gl.glClearColor(0f, 0f, 1.0f, 1f);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  }

  @Override public void dispose(
    @SuppressWarnings("unused") final GLAutoDrawable drawable)
  {
    // Nothing.
  }

  @Override public void init(
    @SuppressWarnings("unused") final GLAutoDrawable drawable)
  {
    // Nothing.
  }

  @Override public void reshape(
    @SuppressWarnings("unused") final GLAutoDrawable drawable,
    @SuppressWarnings("unused") final int x,
    @SuppressWarnings("unused") final int y,
    @SuppressWarnings("unused") final int w,
    @SuppressWarnings("unused") final int h)
  {
    // Nothing.
  }
}
