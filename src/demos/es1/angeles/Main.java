package demos.es1.angeles;

import java.nio.*;
import javax.media.opengl.*;
import com.sun.javafx.newt.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Angeles Main");
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

            Angeles angel = new Angeles();
            angel.init(gl);
            angel.reshape(gl, 0, 0, window.getWidth(), window.getHeight());

            long startTime = System.currentTimeMillis();
            long curTime = 0;

            do {
                angel.display(gl);
                drawable.swapBuffers();
                window.pumpMessages();

                //                Thread.yield();

                //                try{
                //                    Thread.sleep(10);
                //                } catch(InterruptedException ie) {}
                curTime = System.currentTimeMillis();
            } while ((curTime - startTime) < 115000);

            // Shut things down cooperatively
            context.release();
            context.destroy();
            drawable.setRealized(false);
            factory.shutdown();
            System.out.println("Angeles shut down cleanly.");
        } catch (GLException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
