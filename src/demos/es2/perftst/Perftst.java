package demos.es2.perftst;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class Perftst implements MouseListener, GLEventListener {

    private GLWindow window;
    private boolean quit = false;

    private PerfModule pmod;
    private ShaderState st;
    private PMVMatrix pmvMatrix;


    public void mouseClicked(MouseEvent e) {
        quit=true;
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

    private void run(int type, PerfModule pm) {
        int width = 800;
        int height = 480;
        pmod = pm;
        System.err.println("Perftst.run()");
        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                window = GLWindow.create(nWindow);
            } else {
                window = GLWindow.create(caps);
            }

            window.addMouseListener(this);
            window.addGLEventListener(this);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);

            window.display();

            // Shut things down cooperatively
            window.destroy();
            System.out.println("Perftst shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void init(GLAutoDrawable drawable) {
        drawable.setAutoSwapBufferMode(false);

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println("Entering initialization");
        System.err.println("GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
        System.err.println("GL_EXTENSIONS:");
        System.err.println("  " + gl.glGetString(gl.GL_EXTENSIONS));

        pmvMatrix = new PMVMatrix();

        pmod.initShaderState(gl);
        st = ShaderState.getCurrent();

        // Push the 1st uniform down the path 
        st.glUseProgram(gl, true);

        pmvMatrix.glMatrixMode(pmvMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(pmvMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        st.glUseProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(st);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.glUseProgram(gl, true);

        // Set location in front of camera
        pmvMatrix.glMatrixMode(pmvMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, 1.0f, 0.0f, 1.0f, 1.0f, 100.0f);

        pmvMatrix.glMatrixMode(pmvMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);

        GLUniformData ud = st.getUniform("mgl_PMVMatrix");
        if(null!=ud) {
            // same data object
            st.glUniform(gl, ud);
        } 

        st.glUseProgram(gl, false);
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.destroy(gl);
        st=null;
        pmvMatrix.destroy();
        pmvMatrix=null;
        quit=true;
    }


    public void display(GLAutoDrawable drawable) {
        pmod.run(drawable, 10);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        String tstName = "demos.es2.perftst.PerfVBOLoad"; // default

        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
            if(args[i].equals("-test") && i+1<args.length ) {
                tstName = args[i+1];
            }
        }

        try {
            PerfModule pmod = (PerfModule) Class.forName(tstName).newInstance();
            new Perftst().run(type, pmod);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
