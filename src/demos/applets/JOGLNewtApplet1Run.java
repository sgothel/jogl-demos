package demos.applets;

import java.applet.*;
import java.awt.Container;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;

import javax.media.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import java.awt.BorderLayout;

/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class JOGLNewtApplet1Run extends Applet {
    GLWindow glWindow;
    NewtCanvasAWT newtCanvasAWT;
    JOGLNewtAppletBase base;

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
                                      glDebug,
                                      glTrace);

        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.get(glProfileName));
            glWindow = GLWindow.create(caps);
            newtCanvasAWT = new NewtCanvasAWT(glWindow);
            container.setLayout(new BorderLayout());
            container.add(newtCanvasAWT, BorderLayout.CENTER);
            base.init(glWindow);
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
        glWindow.setVisible(false); // hide 1st
        glWindow.reparentWindow(null); // get out of newtCanvasAWT
        this.remove(newtCanvasAWT); // remove newtCanvasAWT
        base.destroy(true); // destroy glWindow unrecoverable
        base=null;
    }
}

