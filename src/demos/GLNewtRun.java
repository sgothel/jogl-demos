package demos;

import java.lang.reflect.*;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class GLNewtRun extends WindowAdapter implements KeyListener, MouseListener {

    static GLWindow window;
    static volatile boolean quit = false;

    public void windowDestroyNotify(WindowEvent e) {
        quit = true;
    }

    static int dx=0;
    static int dy=0;
    static int dw=0;
    static int dh=0;

    public void keyPressed(KeyEvent e) { 
        System.out.println(e);
        if(e.getKeyChar()=='f') {
            window.setFullscreen(!window.isFullscreen());
        } else if(e.getKeyChar()=='q') {
            quit = true;
        } else if(e.getKeyChar()=='p') {
            int x = window.getX() + dx;
            int y = window.getY() + dy;
            System.out.println("Reset Pos "+x+"/"+y);
            window.setPosition(x, y);
        } else if(e.getKeyChar()=='s') {
            int w = window.getWidth() + dw;
            int h = window.getHeight() + dh;
            System.out.println("Reset Size "+w+"x"+h);
            window.setSize(w, h);
        }
    }
    public void keyReleased(KeyEvent e) { 
        System.out.println(e);
    }
    public void keyTyped(KeyEvent e) { 
        System.out.println(e);
    }

    public void mouseClicked(MouseEvent e) {
        System.out.println(" mouseevent: "+e);
        switch(e.getClickCount()) {
            case 1:
                if(e.getButton()>MouseEvent.BUTTON1) {
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

    public boolean shouldQuit() { return quit; }

    public static int str2int(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static boolean setField(Object instance, String fieldName, Object value) {
        try {
            Field f = instance.getClass().getField(fieldName);
            if(f.getType().isInstance(value)) {
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

    public static void main(String[] args) {
        boolean parented = false;
        boolean useAWTTestFrame = false;
        boolean useAWT = false;
        boolean undecorated = false;
        boolean fullscreen = false;
        int x_p = 0;
        int y_p = 0;
        int x = 0;
        int y = 0;
        int width = 800;
        int height = 480;
        String glProfileStr = null;

        if(0==args.length) {
            throw new RuntimeException("Usage: "+GLNewtRun.class+" <demo class name (GLEventListener)>");
        }

        GLNewtRun listener = new GLNewtRun();

        int i=0;
        while(i<args.length-1) {
            if(args[i].equals("-awt")) {
                useAWT = true;
            } else if(args[i].equals("-awttestframe")) {
                useAWT = true;
                useAWTTestFrame = true;
            } else if(args[i].equals("-undecorated")) {
                undecorated = true;
            } else if(args[i].equals("-parented")) {
                parented = true;
            } else if(args[i].equals("-fs")) {
                fullscreen = true;
            } else if(args[i].equals("-xp")) {
                i++;
                x_p = str2int(args[i], x_p);
            } else if(args[i].equals("-yp")) {
                i++;
                y_p = str2int(args[i], y_p);
            } else if(args[i].equals("-x")) {
                i++;
                x = str2int(args[i], x);
            } else if(args[i].equals("-y")) {
                i++;
                y = str2int(args[i], y);
            } else if(args[i].equals("-width")) {
                i++;
                width = str2int(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = str2int(args[i], height);
            } else if(args[i].startsWith("-GL")) {
                glProfileStr = args[i].substring(1);
            } else if(args[i].equals("-dx")) {
                i++;
                dx = str2int(args[i], dx);
            } else if(args[i].equals("-dy")) {
                i++;
                dy = str2int(args[i], dy);
            } else if(args[i].equals("-dw")) {
                i++;
                dw = str2int(args[i], dw);
            } else if(args[i].equals("-dh")) {
                i++;
                dh = str2int(args[i], dh);
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

        GLProfile glp = GLProfile.get(glProfileStr);
        try {
            GLCapabilities caps = new GLCapabilities(glp);

            NewtFactory.setUseEDT(true);
            Window nWindow = null;
            if(useAWT) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                if(useAWTTestFrame) {
                    java.awt.MenuBar menuTest = new java.awt.MenuBar();
                    menuTest.add(new java.awt.Menu("External Frame Test - Menu"));
                    java.awt.Frame frame = new java.awt.Frame("External Frame Test");
                    frame.setMenuBar(menuTest);
                    nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, new Object[] { frame }, nScreen, caps);
                } else {
                    nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                }
            } else {
                Display nDisplay = NewtFactory.createDisplay(null); // local display
                Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
                if(parented) {
                    Window parent = NewtFactory.createWindow(nScreen, caps);
                    parent.setPosition(x_p, y_p);
                    parent.setSize(width+width/10, height+height/10);
                    parent.setVisible(true);
                    nWindow = NewtFactory.createWindow(parent, caps);
                } else {
                    nWindow = NewtFactory.createWindow(nScreen, caps);
                }
            }
            nWindow.setUndecorated(undecorated);
            nWindow.getScreen().setDestroyWhenUnused(true);
            window = GLWindow.create(nWindow);

            if(!setField(demo, "window", window)) {
                setField(demo, "glWindow", window);
            }

            window.addWindowListener(listener);
            window.addMouseListener(listener);
            window.addKeyListener(listener);
            window.addGLEventListener(demo);

            window.setPosition(x, y);
            window.setSize(width, height);
            window.setFullscreen(fullscreen);
            // Size OpenGL to Video Surface
            window.setVisible(true);
            window.enablePerfLog(true);

            do {
                window.display();
            } while (!quit && window.getDuration() < 20000) ;

            window.destroy();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
