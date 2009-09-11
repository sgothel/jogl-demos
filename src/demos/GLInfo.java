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
        System.err.println(glp+"");
        System.err.println(glp+" GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println(glp+" GL_EXTENSIONS: ");
        System.err.println(glp+"   " + gl.glGetString(GL.GL_EXTENSIONS));
        System.err.println(glp+"");
        System.err.println(glp+" Platform EXTENSIONS: ");
        System.err.println(glp+"   " + gl.getContext().getPlatformExtensionsString());
        System.err.println(glp+"");
        System.err.println(glp+" Availability Tests: ");
        System.err.println(glp+"   glConvolutionFilter2D: "+gl.isFunctionAvailable("glConvolutionFilter2D"));
        System.err.println(glp+"   Fixed: glBegin: "+gl.isFunctionAvailable("glBegin"));
        System.err.println(glp+"   ES1  : glClearColorx: "+gl.isFunctionAvailable("glClearColorx"));
        System.err.println(glp+"   GLSL : glUseProgram: "+gl.isFunctionAvailable("glUseProgram"));
        System.err.println(glp+"   GL_ARB_vertex_array_object: "+gl.isExtensionAvailable("GL_ARB_vertex_array_object"));
        System.err.println(glp+"   GL_ARB_vertex_array_object: glBindVertexArray: "+gl.isFunctionAvailable("glBindVertexArray"));
        System.err.println(glp+"   GL_EXT_gpu_shader4: "+gl.isExtensionAvailable("GL_EXT_gpu_shader4"));
        System.err.println(glp+"   GL_EXT_gpu_shader4: glBindFragDataLocation"+gl.isFunctionAvailable("glBindFragDataLocation"));
        System.err.println(glp+"");
        boolean complete30 = gl.isExtensionAvailable("GL_VERSION_3_0") &&
                        gl.isExtensionAvailable("GL_ARB_framebuffer_object") &&
                        gl.isExtensionAvailable("GL_ARB_map_buffer_range") &&
                        gl.isExtensionAvailable("GL_ARB_vertex_array_object") ;
        System.err.println(glp+"   GL_VERSION_3_0: "+gl.isExtensionAvailable("GL_VERSION_3_0")+", complete: "+complete30);
        System.err.println(glp+"       glBeginConditionalRender: "+gl.isFunctionAvailable("glBeginConditionalRender"));
        System.err.println(glp+"       GL_ARB_framebuffer_object: "+gl.isExtensionAvailable("GL_ARB_framebuffer_object"));
        System.err.println(glp+"           glIsRenderbuffer: "+gl.isFunctionAvailable("glIsRenderbuffer"));
        System.err.println(glp+"       GL_ARB_map_buffer_range: "+gl.isExtensionAvailable("GL_ARB_map_buffer_range"));
        System.err.println(glp+"           glMapBufferRange: "+gl.isFunctionAvailable("glMapBufferRange"));
        System.err.println(glp+"       GL_ARB_vertex_array_object: "+gl.isExtensionAvailable("GL_ARB_vertex_array_object"));
        System.err.println(glp+"           glBindVertexArray: "+gl.isFunctionAvailable("glBindVertexArray"));
        System.err.println(glp+"");
        boolean complete31 = complete30 &&
                   gl.isExtensionAvailable("GL_VERSION_3_1") &&
                   gl.isExtensionAvailable("GL_ARB_uniform_buffer_object") &&
                   gl.isExtensionAvailable("GL_ARB_copy_buffer") ;
        System.err.println(glp+"   GL_VERSION_3_1: "+gl.isExtensionAvailable("GL_VERSION_3_1")+", complete: "+complete31);
        System.err.println(glp+"       glDrawArraysInstanced: "+gl.isFunctionAvailable("glDrawArraysInstanced"));
        System.err.println(glp+"       GL_ARB_uniform_buffer_object: "+gl.isExtensionAvailable("GL_ARB_uniform_buffer_object"));
        System.err.println(glp+"           glGetUniformIndices: "+gl.isFunctionAvailable("glGetUniformIndices"));
        System.err.println(glp+"       GL_ARB_copy_buffer: "+gl.isExtensionAvailable("GL_ARB_copy_buffer"));
        System.err.println(glp+"           glCopyBufferSubData: "+gl.isFunctionAvailable("glCopyBufferSubData"));
        System.err.println(glp+"");
        boolean complete32 = complete31 &&
                   gl.isExtensionAvailable("GL_VERSION_3_2") &&
                   gl.isExtensionAvailable("GL_ARB_vertex_array_bgra") &&
                   gl.isExtensionAvailable("GL_ARB_draw_elements_base_vertex") &&
                   gl.isExtensionAvailable("GL_ARB_fragment_coord_conventions") &&
                   gl.isExtensionAvailable("GL_ARB_provoking_vertex") &&
                   gl.isExtensionAvailable("GL_ARB_seamless_cube_map") &&
                   gl.isExtensionAvailable("GL_ARB_texture_multisample") &&
                   gl.isExtensionAvailable("GL_ARB_depth_clamp") &&
                   gl.isExtensionAvailable("GL_ARB_geometry_shader4") &&
                   gl.isExtensionAvailable("GL_ARB_sync") ;
        System.err.println(glp+"   GL_VERSION_3_2: "+gl.isExtensionAvailable("GL_VERSION_3_2")+", complete: "+complete32);
        System.err.println(glp+"       GL_ARB_vertex_array_bgra: "+gl.isExtensionAvailable("GL_ARB_vertex_array_bgra"));
        System.err.println(glp+"       GL_ARB_draw_elements_base_vertex: "+gl.isExtensionAvailable("GL_ARB_draw_elements_base_vertex"));
        System.err.println(glp+"           glDrawElementsBaseVertex: "+gl.isFunctionAvailable("glDrawElementsBaseVertex"));
        System.err.println(glp+"       GL_ARB_fragment_coord_conventions: "+gl.isExtensionAvailable("GL_ARB_fragment_coord_conventions"));
        System.err.println(glp+"       GL_ARB_provoking_vertex: "+gl.isExtensionAvailable("GL_ARB_provoking_vertex"));
        System.err.println(glp+"           glProvokingVertex: "+gl.isFunctionAvailable("glProvokingVertex"));
        System.err.println(glp+"       GL_ARB_seamless_cube_map: "+gl.isExtensionAvailable("GL_ARB_seamless_cube_map"));
        System.err.println(glp+"       GL_ARB_texture_multisample: "+gl.isExtensionAvailable("GL_ARB_texture_multisample"));
        System.err.println(glp+"           glTexImage2DMultisample: "+gl.isFunctionAvailable("glTexImage2DMultisample"));
        System.err.println(glp+"       GL_ARB_depth_clamp: "+gl.isExtensionAvailable("GL_ARB_depth_clamp"));
        System.err.println(glp+"       GL_ARB_geometry_shader4: "+gl.isExtensionAvailable("GL_ARB_geometry_shader4"));
        System.err.println(glp+"           glProgramParameteri: "+gl.isFunctionAvailable("glProgramParameteri"));
        System.err.println(glp+"       GL_ARB_sync: "+gl.isExtensionAvailable("GL_ARB_sync"));
        System.err.println(glp+"           glFenceSync: "+gl.isFunctionAvailable("glFenceSync"));
        System.err.println(glp+"");
        System.err.println(glp+"   GL_AMD_vertex_shader_tessellator: "+gl.isExtensionAvailable("GL_AMD_vertex_shader_tessellator"));
        System.err.println(glp+"       glTessellationFactorAMD: "+gl.isFunctionAvailable("glTessellationFactorAMD"));
        System.err.println(glp+"");
        System.err.println(glp+"   EGL  : eglCreateContext: "+gl.isFunctionAvailable("eglCreateContext"));
        System.err.println(glp+"   EGLEx: eglCreateImage: "+gl.isFunctionAvailable("eglCreateImage"));
        System.err.println(glp+"");
        System.err.println(glp+"   GLX  : glXCreateWindow: "+gl.isFunctionAvailable("glXCreateWindow"));
        System.err.println(glp+"   GLX_ARB_create_context: "+gl.isExtensionAvailable("GLX_ARB_create_context"));
        System.err.println(glp+"");
        System.err.println(glp+"   WGL  : wglCreateContext: "+gl.isFunctionAvailable("wglCreateContext"));
        System.err.println(glp+"");
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
