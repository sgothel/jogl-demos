package demos.es1;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;

import com.sun.javafx.newt.*;

public class RedSquare {
    public static void main(String[] args) {
        System.out.println("RedSquare.main()");
        try {
            Window window = Window.create();

            // Size OpenGL to Video Surface
            int width = 800;
            int height = 480;
            if (!window.setFullscreen(true)) {
                window.setSize(width, height);
            }

            // Hook this into EGL
            GLDrawableFactory.initialize(GLDrawableFactory.PROFILE_GLES1);
            GLDrawableFactory factory = GLDrawableFactory.getFactory();
            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(5);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            GLDrawable drawable = factory.getGLDrawable(new Long(window.getWindowHandle()), caps, null);
            drawable.setRealized(true);
            GLContext context = drawable.createContext(null);
            context.makeCurrent();

            GL gl = context.getGL();
            GLU glu = new GLU();

            //----------------------------------------------------------------------
            // Code for GLEventListener.init()
            //

            System.out.println("Entering initialization");

            // Allocate vertex arrays
            FloatBuffer colors   = BufferUtil.newFloatBuffer(16);
            FloatBuffer vertices = BufferUtil.newFloatBuffer(12);
            // Fill them up
            colors.put( 0, 1);    colors.put( 1, 0);     colors.put( 2, 0);    colors.put( 3, 1);
            colors.put( 4, 1);    colors.put( 5, 0);     colors.put( 6, 0);    colors.put( 7, 1);
            colors.put( 8, 1);    colors.put( 9, 0);     colors.put(10, 0);    colors.put(11, 1);
            colors.put(12, 1);    colors.put(13, 0);     colors.put(14, 0);    colors.put(15, 1);
            vertices.put(0, -2);  vertices.put( 1,  2);  vertices.put( 2,  0);
            vertices.put(3,  2);  vertices.put( 4,  2);  vertices.put( 5,  0);
            vertices.put(6, -2);  vertices.put( 7, -2);  vertices.put( 8,  0);
            vertices.put(9,  2);  vertices.put(10, -2);  vertices.put(11,  0);

            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertices);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            gl.glColorPointer(4, GL.GL_FLOAT, 0, colors);

            // OpenGL Render Settings
            gl.glClearColor(0, 0, 0, 1);
            gl.glEnable(GL.GL_DEPTH_TEST);

            //----------------------------------------------------------------------
            // Code for GLEventListener.display()
            //
            
            long startTime = System.currentTimeMillis();
            long curTime;
            while ((System.currentTimeMillis() - startTime) < 10000) {
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                // Set location in front of camera
                width = window.getWidth();
                height = window.getHeight();
                gl.glViewport(0, 0, width, height);
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity();
                glu.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glLoadIdentity();
                gl.glTranslatef(0, 0, -10);

                // Draw a square
                gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

                drawable.swapBuffers();

                window.pumpMessages();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
