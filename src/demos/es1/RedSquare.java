package demos.es1;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.egl.*;
import com.sun.opengl.util.*;

public class RedSquare {
    public static void main(String[] args) {
        System.out.println("Basic.main()");
        try {
            System.out.println("GLDrawableFactory.getFactory()");
            EGLDrawableFactory factory = (EGLDrawableFactory) GLDrawableFactory.getFactory();

            /*

            System.out.println("Testing getDirectBufferAddress");
            factory.testGetDirectBufferAddress();

            */

            System.out.println("EGLDrawableFactory.initialize()");
            factory.initialize();
            System.out.println("factory.createExternalGLContext()");
            GLContext context = factory.createExternalGLContext();
            // OpenGL context is current at this point

            // The following is a no-op that is only needed to get the
            // Java-level GLContext object set up in thread-local storage
            System.out.println("context.makeCurrent()");
            context.makeCurrent();
            context.setGL(new DebugGL(context.getGL()));

            GL gl = context.getGL();
            GLU glu = new GLU();

            //----------------------------------------------------------------------
            // Code for GLEventListener.init()
            //

            System.out.println("Entering initialization");

            // Size OpenGL to Video Surface
            int width = 800;
            int height = 480;
            System.out.println("calling glViewport()");
            //            gl.glViewport(0, 0, width, height);
            System.out.println("calling glMatrixMode()");
            gl.glMatrixMode(GL.GL_PROJECTION);
            System.out.println("calling glLoadIdentity()");
            gl.glLoadIdentity();
            System.out.println("calling gluPerspective()");
            glu.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
            System.out.println("calling glMatrixMode()");
            gl.glMatrixMode(GL.GL_MODELVIEW);

            System.out.println("Intialized matrices");

            // Allocate vertex arrays
            //            FloatBuffer colors   = BufferUtil.newFloatBuffer(12);
            //            FloatBuffer vertices = BufferUtil.newFloatBuffer(12);
            //            FloatBuffer normals  = BufferUtil.newFloatBuffer(12);
            //            FloatBuffer colors   = FloatBuffer.wrap(new float[12]);
            //            FloatBuffer vertices = FloatBuffer.wrap(new float[12]);
            //            FloatBuffer normals  = FloatBuffer.wrap(new float[12]);
            // Fill them up
            //            colors.put(0, 1);     colors.put( 1, 0);     colors.put( 2, 0);
            //            colors.put(3, 1);     colors.put( 4, 0);     colors.put( 5, 0);
            //            colors.put(6, 1);     colors.put( 7, 0);     colors.put( 8, 0);
            //            colors.put(9, 1);     colors.put(10, 0);     colors.put(11, 0);
            //            vertices.put(0, -2);  vertices.put( 1,  2);  vertices.put( 2,  0);
            //            vertices.put(3,  2);  vertices.put( 4,  2);  vertices.put( 5,  0);
            //            vertices.put(6, -2);  vertices.put( 7, -2);  vertices.put( 8,  0);
            //            vertices.put(9,  2);  vertices.put(10, -2);  vertices.put(11,  0);
            //            normals.put(0, 0);  normals.put( 1, 0);  normals.put( 2, 1);
            //            normals.put(3, 0);  normals.put( 4, 0);  normals.put( 5, 1);
            //            normals.put(6, 0);  normals.put( 7, 0);  normals.put( 8, 1);
            //            normals.put(9, 0);  normals.put(10, 0);  normals.put(11, 1);

            //            FloatBuffer data = FloatBuffer.wrap(new float[24]);
            FloatBuffer data = BufferUtil.newFloatBuffer(12 + 16);
            // Vertices first
            data.put(0, -2);  data.put( 1,  2);  data.put( 2,  0);
            data.put(3,  2);  data.put( 4,  2);  data.put( 5,  0);
            data.put(6, -2);  data.put( 7, -2);  data.put( 8,  0);
            data.put(9,  2);  data.put(10, -2);  data.put(11,  0);
            // Now colors
            data.put(12, 1);     data.put(13, 0);     data.put(14, 0);     data.put(15, 0);
            data.put(16, 1);     data.put(17, 0);     data.put(18, 0);     data.put(19, 0);
            data.put(20, 1);     data.put(21, 0);     data.put(22, 0);     data.put(23, 0);
            data.put(24, 1);     data.put(25, 0);     data.put(26, 0);     data.put(27, 0);

            System.out.println("Set up buffers");

            int[] vbo = new int[1];
            gl.glGenBuffers(1, vbo, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, (12 + 16) * BufferUtil.SIZEOF_FLOAT, data, GL.GL_STATIC_DRAW);
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
            //            gl.glColorPointer(3, GL.GL_FLOAT, 0, 12 * BufferUtil.SIZEOF_FLOAT);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            //            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            //            System.out.println("Vertices' address: 0x" + Integer.toHexString(factory.getDirectBufferAddress(vertices)));
            //            System.out.println("Expected: 0x" + Integer.toHexString(vertices.arrayOffset));
            //            System.out.println("Colors' address: 0x" + Integer.toHexString(factory.getDirectBufferAddress(colors)));
            //            System.out.println("Expected: 0x" + Integer.toHexString(colors.arrayOffset));
            //            gl.glNormalPointer(GL.GL_FLOAT, 0, normals);
            //            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
            //            System.out.println("Normals' address: 0x" + Integer.toHexString(factory.getDirectBufferAddress(normals)));
            //            System.out.println("Expected: 0x" + Integer.toHexString(normals.arrayOffset));
            //            System.out.println("Set up vertex, color and normal pointers");

            System.out.println("Set up vertex and color pointers");

            // OpenGL Render Settings
            gl.glClearColor(0, 0, 0, 1);
            gl.glClearDepthf(1.0f);
            gl.glEnable(GL.GL_DEPTH_TEST);

            System.out.println("Set up clear color and clear depth");

            //----------------------------------------------------------------------
            // Code for GLEventListener.display()
            //
            
            long startTime = System.currentTimeMillis();
            long curTime;
            while ((System.currentTimeMillis() - startTime) < 10000) {
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                System.out.println("Cleared buffers");

                // Set location in front of camera
                gl.glLoadIdentity();
                gl.glTranslatef(0, 0, -10);

                System.out.println("Translated");

                // Draw a square
                gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

                System.out.println("Drew arrays");

                // FIXME -- need an external GLDrawable
                factory.swapBuffers();

                System.out.println("Swapped buffers");

                // Process events
                factory.processEvents();

                // Sleep a bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        } catch (GLException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
