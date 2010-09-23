package demos.es1.angeles;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class Main implements WindowListener, MouseListener {

    public boolean quit = false;
    public GLWindow window = null;

    public void windowRepaint(WindowUpdateEvent e) { }
    public void windowResized(WindowEvent e) { }
    public void windowMoved(WindowEvent e) { }
    public void windowGainedFocus(WindowEvent e) { }
    public void windowLostFocus(WindowEvent e) { }
    public void windowDestroyNotify(WindowEvent e) {
        quit = true;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
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
    public void mouseWheelMoved(MouseEvent e) {
    }

    private void run(int type) {
        int width = 800;
        int height = 480;
        System.out.println("angeles.Main.run()");
        //GLProfile.setProfileGL2ES1();
        try {
            // Hook this into EGL
            GLCapabilities caps = new GLCapabilities(null);
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            /*
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(8);
            */
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                nWindow.setUndecorated(false);
                window = GLWindow.create(nWindow);
            } else {
                window = GLWindow.create(caps);
            }

            window.addWindowListener(this);
            window.addMouseListener(this);

            window.enablePerfLog(true);
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);

            GL gl = window.getGL();
            if(gl.isGLES1() && 0==(type&USE_ANGELESF)) {
                System.out.println("Using: AngelesES1 .. ");
                AngelesES1 angel = new AngelesES1( 0 == (type&USE_NOBLEND) );
                window.addGLEventListener(angel);
            } else {
                if(0!=(type&USE_INTERLEAVE)) {
                    System.out.println("Using: AngelesGLil .. ");
                    AngelesGLil angel = new AngelesGLil( 0 == (type&USE_NOBLEND) );
                    window.addGLEventListener(angel);
                } else {
                    System.out.println("Using: AngelesGL .. ");
                    AngelesGL angel = new AngelesGL( 0 == (type&USE_NOBLEND) );
                    window.addGLEventListener(angel);
                }
            } 

            while (!quit && window.getDuration() < 215000) {
                window.display();
            }

            // Shut things down cooperatively
            window.destroy();
            System.out.println("angeles.Main shut down cleanly.");
        } catch (GLException e) {
            e.printStackTrace();
        }
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;
    public static int USE_ANGELESF  = 1 << 1;
    public static int USE_NOBLEND   = 1 << 2;
    public static int USE_INTERLEAVE= 1 << 3;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            } else if(args[i].equals("-angelesf")) {
                type |= USE_ANGELESF; 
            } else if(args[i].equals("-noblend")) {
                type |= USE_NOBLEND; 
            } else if(args[i].equals("-interleave")) {
                type |= USE_INTERLEAVE; 
            }
        }
        new Main().run(type);
        System.exit(0);
    }

}
