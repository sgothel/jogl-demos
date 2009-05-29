package demos;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.opengl.*;

public class GLInfo implements GLEventListener {

    private GLWindow window;

    private void run(int type) {
        int width = 256;
        int height = 256;
        System.err.println("GLInfo.run()");
        if(null==glprofile) {
            GLProfile.setProfileGLAny();
        } else {
            GLProfile.setProfile(glprofile);
        }
        try {
            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            System.err.println("GLCapabilities PRE : "+caps);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                System.err.println(nWindow);
                //nWindow.setVisible(true);
            }
            window = GLWindow.create(nWindow, caps);

            window.addGLEventListener(this);

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            // window.setFullscreen(true);
            window.setVisible(true);

            window.display();

            // Shut things down cooperatively
            window.destroy();
            window.getFactory().shutdown();
            System.out.println("GLInfo shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        System.err.println("GLCapabilities POST: "+drawable.getChosenGLCapabilities());
        System.err.println("GL Profile: "+GLProfile.getProfile());
        System.err.println("GL:" + gl);
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL_EXTENSIONS: ");
        System.err.println("  " + gl.glGetString(GL.GL_EXTENSIONS));
        System.err.println("Platform EXTENSIONS: ");
        System.err.println("  " + gl.getContext().getPlatformExtensionsString());
        System.err.println("Availability Tests: ");
        System.err.println("  Fixed: glBegin: "+gl.isFunctionAvailable("glBegin"));
        System.err.println("  ES1  : glClearColorx: "+gl.isFunctionAvailable("glClearColorx"));
        System.err.println("  GLSL : glUseProgram: "+gl.isFunctionAvailable("glUseProgram"));
        System.err.println("  GL_ARB_vertex_array_object: "+gl.isExtensionAvailable("GL_ARB_vertex_array_object"));
        System.err.println("  GL_ARB_vertex_array_object: glBindVertexArray: "+gl.isFunctionAvailable("glBindVertexArray"));
        System.err.println("  GL_EXT_gpu_shader4: "+gl.isExtensionAvailable("GL_EXT_gpu_shader4"));
        System.err.println("  GL_EXT_gpu_shader4: glBindFragDataLocation"+gl.isFunctionAvailable("glBindFragDataLocation"));
        System.err.println("  GL_VERSION_3_0: "+gl.isExtensionAvailable("GL_VERSION_3_0"));
        System.err.println("  GL_VERSION_3_0: glBeginConditionalRender: "+gl.isFunctionAvailable("glBeginConditionalRender"));
        System.err.println("  GL_ARB_texture_buffer_object: "+gl.isExtensionAvailable("GL_ARB_texture_buffer_object"));
        System.err.println("  GL_ARB_texture_buffer_object: glTexBuffer: "+gl.isFunctionAvailable("glTexBuffer"));
        System.err.println("  GL_VERSION_3_1: "+gl.isExtensionAvailable("GL_VERSION_3_1"));
        System.err.println("  EGL  : eglCreateContext: "+gl.isFunctionAvailable("eglCreateContext"));
        System.err.println("  EGLEx: eglCreateImage: "+gl.isFunctionAvailable("eglCreateImage"));
        System.err.println("  GLX  : glXCreateWindow: "+gl.isFunctionAvailable("glXCreateWindow"));
        System.err.println("  GLX_ARB_create_context: "+gl.isExtensionAvailable("GLX_ARB_create_context"));
        System.err.println("  WGL  : wglCreateContext: "+gl.isFunctionAvailable("wglCreateContext"));
        System.err.println("  CGL  : CGLCreateContext: "+gl.isFunctionAvailable("CGLCreateContext"));
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void dispose(GLAutoDrawable drawable) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;
    public static String glprofile  = null;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
            if(args[i].startsWith("-GL")) {
                glprofile=args[i].substring(1);
            }
        }
        new GLInfo().run(type);
        System.exit(0);
    }
}
