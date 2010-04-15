package demos.readbuffer;

import java.lang.reflect.*;
import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class Main implements WindowListener, MouseListener, SurfaceUpdatedListener {

    public boolean quit = false;
    public GLWindow window = null;

    public void surfaceUpdated(Object updater, NativeWindow window, long when) {
        if(null!=window) {
            this.window.display();
        }
    }

    public void windowResized(WindowEvent e) { }
    public void windowMoved(WindowEvent e) { }
    public void windowGainedFocus(WindowEvent e) { }
    public void windowLostFocus(WindowEvent e) { }
    public void windowDestroyNotify(WindowEvent e) {
        System.err.println("********** quit **************");
        quit = true;
    }

    public void mouseClicked(MouseEvent e) {
        System.out.println("mouseevent: "+e);
        switch(e.getClickCount()) {
            case 1:
                if(null!=window) {
                    window.setFullscreen(!window.isFullscreen());
                }
                break;
            default: 
                quit=true;
                break;
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

    public GLWindow createOffscreen(GLCapabilities caps, int w, int h, boolean pbuffer) {
        GLCapabilities capsOffscreen = (GLCapabilities) caps.clone();
        capsOffscreen.setOnscreen(false);
        capsOffscreen.setPBuffer(pbuffer);
        capsOffscreen.setDoubleBuffered(false);

            Display nDisplay = NewtFactory.createDisplay(null); // local display
            Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
            Window nWindow = NewtFactory.createWindow(nScreen, capsOffscreen, false /* undecorated */);

        GLWindow windowOffscreen = GLWindow.create(nWindow);
        windowOffscreen.enablePerfLog(true);
        windowOffscreen.setSize(w, h);
        windowOffscreen.setVisible(true);
        return windowOffscreen;
    }

    private void run(String glProfileStr, int typeNewt, boolean fullscreen, int typeTest, GLEventListener demo, boolean pbuffer) {
        GLProfile glp = GLProfile.get(glProfileStr);
        int width = 800;
        int height = 480;
        System.out.println("readbuffer.Main.run() Test: "+typeTest);
        try {
            GLCapabilities caps = new GLCapabilities(glp);

            // Full init pbuffer window ..
            GLWindow windowOffscreen = createOffscreen(caps, width, height, pbuffer);
            // setField(demo, "glDebug", new Boolean(true));
            // setField(demo, "glTrace", new Boolean(true));
            if(!setField(demo, "window", windowOffscreen)) {
                setField(demo, "glWindow", windowOffscreen);
            }
            windowOffscreen.addGLEventListener(demo);

            if ( TEST_SURFACE2FILE < typeTest ) {
                System.out.println("readbuffer.Main.run() Using a target onscreen window with read drawable attachment");
                // Setup init onscreen window ..
                if(0!=(typeNewt&USE_AWT)) {
                    Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                    Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                    Window nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                    window = GLWindow.create(nWindow);
                } else {
                    Display nDisplay = NewtFactory.createDisplay(null); // local display
                    Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
                    Window nWindow = NewtFactory.createWindow(nScreen, caps, false /* undecorated */);
                    window = GLWindow.create(nWindow);
                }

                window.addWindowListener(this);
                window.addMouseListener(this);

                window.setSize(width, height);
                window.setFullscreen(fullscreen);
                window.setVisible(true);
            }

            GLDrawable readDrawable = windowOffscreen.getContext().getGLDrawable() ;

            if ( TEST_SURFACE2FILE == typeTest ) {
                Surface2File s2f = new Surface2File();
                windowOffscreen.addSurfaceUpdatedListener(s2f);
            } else if ( TEST_READBUFFER2FILE == typeTest ) {
                ReadBuffer2File readDemo = new ReadBuffer2File( readDrawable ) ;
                window.addGLEventListener(readDemo);
            } else if ( TEST_READBUFFER2SCREEN == typeTest ) {
                ReadBuffer2Screen readDemo = new ReadBuffer2Screen( readDrawable ) ;
                window.addGLEventListener(readDemo);
            }

            if(null!=window) {
                windowOffscreen.addSurfaceUpdatedListener(this);
            }

            System.out.println("+++++++++++++++++++++++++++");
            System.out.println(windowOffscreen);
            System.out.println("+++++++++++++++++++++++++++");

            while ( !quit ) {
                // System.out.println("...............................");
                windowOffscreen.display();
                if ( TEST_READBUFFER2SCREEN == typeTest ) {
                    if ( windowOffscreen.getDuration() >= 10000) {
                        break;
                    }
                } else {
                    if ( windowOffscreen.getTotalFrames() >= 10) {
                        break;
                    }
                }
            }

            // Shut things down cooperatively
            windowOffscreen.destroy();
            if(null!=window) {
                window.destroy();
            }
            try {
                Thread.sleep(2000);
            } catch (Exception e) {}
            windowOffscreen.getFactory().shutdown();
            System.out.println("readbuffer.Main shut down cleanly.");
        } catch (GLException e) {
            e.printStackTrace();
        }
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static int TEST_SURFACE2FILE      = 0;
    public static int TEST_READBUFFER2FILE   = 1;
    public static int TEST_READBUFFER2SCREEN = 2;

    public static void main(String[] args) {
        String glProfileStr = null;
        boolean fullscreen = false;
        boolean pbuffer = true;
        int typeNewt = USE_NEWT ;
        int typeTest = TEST_SURFACE2FILE;
        int i=0;
        while(i<args.length-1) {
            if(args[i].equals("-awt")) {
                typeNewt |= USE_AWT; 
            } else if(args[i].equals("-fs")) {
                fullscreen = true;
            } else if(args[i].equals("-nopbuffer")) {
                pbuffer = false;
            } else if(args[i].equals("-test")) {
                i++;
                typeTest = str2int(args[i], typeTest);
            } else if(args[i].startsWith("-GL")) {
                glProfileStr = args[i].substring(1);
            }
            i++;
        }
        String demoClassName = args[i];
        Object demoObject = null;

        try {
            Class demoClazz = Class.forName(demoClassName);
            demoObject = demoClazz.newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error while instantiating demo: "+demoClassName);
        }
        if( !(demoObject instanceof GLEventListener) ) {
            throw new RuntimeException("Not a GLEventListener: "+demoClassName);
        }
        GLEventListener demo = (GLEventListener) demoObject;

        new Main().run(glProfileStr, typeNewt, fullscreen, typeTest, demo, pbuffer);
        System.exit(0);
    }

    public static int str2int(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static boolean setField(Object instance, String fieldName, Object value) {
        try {
            Field f = instance.getClass().getField(fieldName);
            if(value instanceof Boolean || f.getType().isInstance(value)) {
                f.set(instance, value);
                return true;
            } else {
                System.out.println(instance.getClass()+" '"+fieldName+"' field not assignable with "+value.getClass()+", it's a: "+f.getType());
            }
        } catch (NoSuchFieldException nsfe) {
            System.out.println(instance.getClass()+" has no '"+fieldName+"' field");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

}
