package demos.applets;

import java.applet.*;
import java.awt.Container;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.*;

/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class JOGLNewtApplet1Run extends Applet {
    JOGLNewtAppletBase base;
    Window nWindow = null;

    public void init() {
        if(!(this instanceof Container)) {
            throw new RuntimeException("This Applet is not a AWT Container");
        }
        Container container = (Container) this; // have to think about that, we may use a Container

        String glEventListenerClazzName=null;
        String glProfileName=null;
        int glSwapInterval=0;
        boolean glDebug=false;
        boolean glTrace=false;
        String tmp;
        try {
            glEventListenerClazzName = getParameter("gl_event_listener_class");
            glProfileName = getParameter("gl_profile");
            glSwapInterval = JOGLNewtAppletBase.str2Int(getParameter("gl_swap_interval"), glSwapInterval);
            glDebug = JOGLNewtAppletBase.str2Bool(getParameter("gl_debug"), glDebug);
            glTrace = JOGLNewtAppletBase.str2Bool(getParameter("gl_trace"), glTrace);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null==glEventListenerClazzName) {
            throw new RuntimeException("No applet parameter 'gl_event_listener_class'");
        }
        base = new JOGLNewtAppletBase(glEventListenerClazzName, 
                                      glSwapInterval,
                                      false /* pumpMessages == handleWindowEvents */,
                                      glDebug,
                                      glTrace);

        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.get(glProfileName));
            Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
            Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
            nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, new Object[] { container }, 
                                               nScreen, caps, true /* undecorated */);
            // nWindow.setPosition(x, y);
            // nWindow.setSize(container.getWidth(), container.getHeight());
            if(null!=nWindow) {
                base.init(nWindow);
            }
            if(base.isValid()) {
                GLEventListener glEventListener = base.getGLEventListener();

                if(glEventListener instanceof MouseListener) {
                    addMouseListener((MouseListener)glEventListener);
                }
                if(glEventListener instanceof MouseMotionListener) {
                    addMouseMotionListener((MouseMotionListener)glEventListener);
                }
                if(glEventListener instanceof KeyListener) {
                    addKeyListener((KeyListener)glEventListener);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void start() {
        base.start();
    }

    public void stop() {
        base.stop();
    }

    public void destroy() {
        base.destroy(false); // no dispose events
        base=null;
        if(null!=nWindow) {
            nWindow.destroy();
            nWindow=null;
        }
    }
}

