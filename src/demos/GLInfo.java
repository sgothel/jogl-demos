package demos;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.opengl.*;

public class GLInfo extends Thread implements GLEventListener {

    private GLWindow window;
    private GLProfile glp;

    public GLInfo() {
        super();
    }

    private void init(String glprofile, int type) {
        int width = 640;
        int height = 480;
        glp = GLProfile.get(glprofile);
        System.err.println(glp+" GLInfo.start()");
        try {
            GLCapabilities caps = new GLCapabilities(glp);
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            System.err.println(glp+" GLCapabilities PRE : "+caps);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                System.err.println(glp+" "+nWindow);
            }
            window = GLWindow.create(nWindow, caps);

            System.err.println(glp+" GLWindow : "+window);

            window.addGLEventListener(this);

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            // window.setFullscreen(true);

            window.setVisible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void runInThread(String glprofile, int type) {
        init(glprofile, type);
        run();
    }

    private void start(String glprofile, int type) {
        init(glprofile, type);
        start();
    }

    public void run() {
        try {
            System.err.println(glp+" GLInfo.run() 1");

            System.err.println(glp+" GLInfo.run() 2");

            window.display();

            System.err.println(glp+" GLInfo.run() 3");

            try {
                Thread.sleep(500);
            } catch (Exception e) {}

            window.display();

            // Shut things down cooperatively
            window.destroy();
            window.getFactory().shutdown();
            System.out.println(glp+" GLInfo shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        GLProfile glp = gl.getGLProfile();

        System.err.println(glp+" GLCapabilities POST: "+drawable.getChosenGLCapabilities());
        System.err.println(glp+" GL Profile: "+drawable.getGLProfile());
        System.err.println(glp+" GL:" + gl);
        System.err.println(glp+" GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println(glp+" GL_EXTENSIONS: ");
        System.err.println(glp+"   " + gl.glGetString(GL.GL_EXTENSIONS));
        System.err.println(glp+" Platform EXTENSIONS: ");
        System.err.println(glp+"   " + gl.getContext().getPlatformExtensionsString());
        System.err.println(glp+" Availability Tests: ");
        System.err.println(glp+"   Fixed: glBegin: "+gl.isFunctionAvailable("glBegin"));
        System.err.println(glp+"   ES1  : glClearColorx: "+gl.isFunctionAvailable("glClearColorx"));
        System.err.println(glp+"   GLSL : glUseProgram: "+gl.isFunctionAvailable("glUseProgram"));
        System.err.println(glp+"   GL_ARB_vertex_array_object: "+gl.isExtensionAvailable("GL_ARB_vertex_array_object"));
        System.err.println(glp+"   GL_ARB_vertex_array_object: glBindVertexArray: "+gl.isFunctionAvailable("glBindVertexArray"));
        System.err.println(glp+"   GL_EXT_gpu_shader4: "+gl.isExtensionAvailable("GL_EXT_gpu_shader4"));
        System.err.println(glp+"   GL_EXT_gpu_shader4: glBindFragDataLocation"+gl.isFunctionAvailable("glBindFragDataLocation"));
        System.err.println(glp+"   GL_VERSION_3_0: "+gl.isExtensionAvailable("GL_VERSION_3_0"));
        System.err.println(glp+"   GL_VERSION_3_0: glBeginConditionalRender: "+gl.isFunctionAvailable("glBeginConditionalRender"));
        System.err.println(glp+"   GL_ARB_texture_buffer_object: "+gl.isExtensionAvailable("GL_ARB_texture_buffer_object"));
        System.err.println(glp+"   GL_ARB_texture_buffer_object: glTexBuffer: "+gl.isFunctionAvailable("glTexBuffer"));
        System.err.println(glp+"   GL_VERSION_3_1: "+gl.isExtensionAvailable("GL_VERSION_3_1"));
        System.err.println(glp+"   EGL  : eglCreateContext: "+gl.isFunctionAvailable("eglCreateContext"));
        System.err.println(glp+"   EGLEx: eglCreateImage: "+gl.isFunctionAvailable("eglCreateImage"));
        System.err.println(glp+"   GLX  : glXCreateWindow: "+gl.isFunctionAvailable("glXCreateWindow"));
        System.err.println(glp+"   GLX_ARB_create_context: "+gl.isExtensionAvailable("GLX_ARB_create_context"));
        System.err.println(glp+"   WGL  : wglCreateContext: "+gl.isFunctionAvailable("wglCreateContext"));
        System.err.println(glp+"   CGL  : CGLCreateContext: "+gl.isFunctionAvailable("CGLCreateContext"));
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void dispose(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        GLProfile glp = gl.getGLProfile();

        System.err.println(glp+" dispose");
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        GLProfile glp = gl.getGLProfile();

        System.err.println(glp+" display: "+displayed);
        displayed++;
    }
    int displayed = 0;

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String[] args) {
        String glprofile  = null;
        int type = USE_NEWT ;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
            if(args[i].startsWith("-GL")) {
                if(null!=glprofile) {
                    new GLInfo().start(glprofile, type);
                }
                glprofile=args[i].substring(1);
            }
        }
        new GLInfo().runInThread(glprofile, type);
    }
}
