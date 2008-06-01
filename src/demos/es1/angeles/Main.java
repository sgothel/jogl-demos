package demos.es1.angeles;

import java.nio.*;
import javax.media.opengl.*;
import com.sun.javafx.newt.*;

public class Main implements MouseListener {

    public boolean quit = false;

    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount()>1) {
            quit=true;
        }
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseMoved(MouseEvent e) {
    }
    public void mouseDragged(MouseEvent e) {
    }

    public static void main(String[] args) {
        System.out.println("Angeles Main");
        try {
            Display display = NewtFactory.createDisplay(null); // local display
            Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
            Window window = NewtFactory.createWindow(screen, 0); // dummy VisualID

            Main ml = new Main();
            window.addMouseListener(ml);

            // Size OpenGL to Video Surface
            int width = 800;
            int height = 480;
            if (!window.setFullscreen(true)) {
                window.setSize(width, height);
            }

            // Hook this into EGL
            GLDrawableFactory factory = GLDrawableFactory.getFactory(GLDrawableFactory.PROFILE_GLES1, window);
            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            GLDrawable drawable = factory.createGLDrawable(window, caps, null);
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
            } while (!ml.quit && (curTime - startTime) < 215000);

            // Shut things down cooperatively
            context.release();
            context.destroy();
            drawable.destroy();
            factory.shutdown();
            System.out.println("Angeles shut down cleanly.");
        } catch (GLException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
