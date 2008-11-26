package demos.es1;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.util.*;
import javax.media.opengl.glu.*;

import com.sun.javafx.newt.*;

public class Info implements GLEventListener {

    private GLWindow window;

    private void run(int type) {
        int width = 10;
        int height = 10;
        System.err.println("Info.run()");
        GLProfile.setProfileGL2ES1();
        try {
            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NewtFactory.AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NewtFactory.AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NewtFactory.AWT, nScreen, caps);
            }
            window = GLWindow.create(nWindow, caps);

            window.addGLEventListener(this);

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);

            window.display();

            // Shut things down cooperatively
            window.close();
            window.getFactory().shutdown();
            System.out.println("Info shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        System.err.println("GL Profile: "+GLProfile.getProfile());
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL_EXTENSIONS: ");
        System.err.println("  " + gl.glGetString(GL.GL_EXTENSIONS));
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
        }
        new Info().run(type);
        System.exit(0);
    }
}
