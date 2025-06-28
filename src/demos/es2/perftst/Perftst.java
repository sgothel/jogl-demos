package demos.es2.perftst;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class Perftst implements MouseListener, GLEventListener {

    private GLWindow window;
    private boolean quit = false;

    private PerfModule pmod;
    private ShaderState st;
    private PMVMatrix pmvMatrix;


    @Override
    public void mouseClicked(final MouseEvent e) {
        quit=true;
    }
    @Override
    public void mouseEntered(final MouseEvent e) {
    }
    @Override
    public void mouseExited(final MouseEvent e) {
    }
    @Override
    public void mousePressed(final MouseEvent e) {
    }
    @Override
    public void mouseReleased(final MouseEvent e) {
    }
    @Override
    public void mouseMoved(final MouseEvent e) {
    }
    @Override
    public void mouseDragged(final MouseEvent e) {
    }
    @Override
    public void mouseWheelMoved(final MouseEvent e) {
    }

    private void run(final int type, final PerfModule pm) {
        final int width = 800;
        final int height = 480;
        pmod = pm;
        System.err.println("Perftst.run()");
        try {
            final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                final Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                final Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(nScreen, caps);
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
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        drawable.setAutoSwapBufferMode(false);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println("Entering initialization");
        System.err.println("GL_VERSION=" + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL_EXTENSIONS:");
        System.err.println("  " + gl.glGetString(GL.GL_EXTENSIONS));

        pmvMatrix = new PMVMatrix();

        st = pmod.initShaderState(gl);

        // Push the 1st uniform down the path
        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        if(!st.uniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMv()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(st);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);

        // Set location in front of camera
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, 1.0f, 0.0f, 1.0f, 1.0f, 100.0f);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);

        final GLUniformData ud = st.getUniform("mgl_PMVMatrix");
        if(null!=ud) {
            // same data object
            st.uniform(gl, ud);
        }

        st.useProgram(gl, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.destroy(gl);
        st=null;
        pmvMatrix=null;
        quit=true;
    }


    @Override
    public void display(final GLAutoDrawable drawable) {
        pmod.run(drawable, 10);
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(final String[] args) {
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
            final PerfModule pmod = (PerfModule) Class.forName(tstName).newInstance();
            new Perftst().run(type, pmod);
            System.exit(0);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
