package demos.es2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.glsl.ShaderUtil;

public class RedSquare extends Thread implements WindowListener, KeyListener, MouseListener, GLEventListener {

    public Window nWindow = null;
    public GLWindow window;
    private GLProfile glp;
    private boolean quit = false;
    private final String glprofile;
    private final int type;

    public RedSquare() {
        this(null, USE_NEWT);
    }

    public RedSquare(final String glprofile, final int type) {
        super();
        this.glprofile=glprofile;
        this.type=type;
    }

    @Override
    public void windowRepaint(final WindowUpdateEvent e) { }
    @Override
    public void windowResized(final WindowEvent e) { }
    @Override
    public void windowMoved(final WindowEvent e) { }
    @Override
    public void windowGainedFocus(final WindowEvent e) { }
    @Override
    public void windowLostFocus(final WindowEvent e) { }
    @Override
    public void windowDestroyNotify(final WindowEvent e) {
        System.out.println("WINDOW-DESTROY NOTIFY "+Thread.currentThread()+" QUIT "+e);
        quit = true;
    }
    @Override
    public void windowDestroyed(final WindowEvent e) {
        System.out.println("WINDOW-DESTROYED "+Thread.currentThread());
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        System.out.println("KEY-PRESSED "+Thread.currentThread()+" UNHANDLED "+e);
    }
    @Override
    public void keyReleased(final KeyEvent e) {
        System.out.println("KEY-RELEASED "+Thread.currentThread()+" UNHANDLED "+e);
        if( !e.isPrintableKey() || e.isAutoRepeat() ) {
            return;
        }
        if(e.getKeyChar()=='f') {
            System.out.println("KEY-TYPED "+Thread.currentThread()+" FULLSCREEN "+e);
            window.setFullscreen(!window.isFullscreen());
        } else if(e.getKeyChar()=='q') {
            System.out.println("KEY-TYPED "+Thread.currentThread()+" QUIT "+e);
            quit = true;
        } else {
            System.out.println("KEY-TYPED "+Thread.currentThread()+" UNHANDLED "+e);
        }
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        System.out.println("MOUSE-CLICKED "+Thread.currentThread()+" UNHANDLED "+e);
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

    public boolean shouldQuit() { return quit; }

    @Override
    public void run() {
        final int width = 800;
        final int height = 480;
        glp = GLProfile.get(glprofile);
        System.out.println("RUN "+Thread.currentThread()+" "+glp);
        try {
            final GLCapabilities caps = new GLCapabilities(glp);

            if(0!=(type&USE_AWT)) {
                final Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                final Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(nScreen, caps);
                window = GLWindow.create(nWindow);
            } else {
                window = GLWindow.create(caps);
            }

            window.addWindowListener(this);
            window.addMouseListener(this);
            window.addKeyListener(this);
            window.addGLEventListener(this);

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            // window.setFullscreen(true);
            window.setVisible(true);
            window.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);

            if(!oneThread) {
                do {
                    display();
                } while (!quit && window.getTotalFPSDuration() < 20000) ;

                shutdown();
            }

        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    public void display() {
        try {
            window.display();
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            // Shut things down cooperatively
            window.destroy();
            window = null;
            if(null!=nWindow) {
                nWindow.destroy();
                nWindow=null;
            }
            System.out.println("SHUTDOWN "+Thread.currentThread()+" cleanly");
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }


    ShaderState st;
    PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLArrayDataServer vertices ;
    private GLArrayDataServer colors ;

    private void initShader(final GL2ES2 gl) {
        // Create & Compile the shader objects
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RedSquare.class,
                                            "shader", "shader/bin", "redsquare", false);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RedSquare.class,
                                            "shader", "shader/bin", "redsquare", false);

        // Create & Link the shader program
        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, false);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(swapInterval>=0) {
            gl.setSwapInterval(swapInterval);
        }

        System.err.println(Thread.currentThread()+" Entering initialization");
        System.err.println(Thread.currentThread()+" GL Profile: "+gl.getGLProfile());
        System.err.println(Thread.currentThread()+" GL:" + gl);
        System.err.println(Thread.currentThread()+" GL_VERSION=" + gl.glGetString(GL.GL_VERSION));
        System.err.println(Thread.currentThread()+" GL_EXTENSIONS:");
        System.err.println(Thread.currentThread()+"   " + gl.glGetString(GL.GL_EXTENSIONS));
        System.err.println(Thread.currentThread()+" swapInterval: " + swapInterval + " (GL: "+gl.getSwapInterval()+")");
        System.err.println(Thread.currentThread()+" isShaderCompilerAvailable: " + ShaderUtil.isShaderCompilerAvailable(gl));

        if(debuggl) {
            try {
                // Debug ..
                gl = (GL2ES2) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES2.class, gl, null) );

                // Trace ..
                gl = (GL2ES2) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES2.class, gl, new Object[] { System.err } ) );
            } catch (final Exception e) {e.printStackTrace();}
        }

        pmvMatrix = new PMVMatrix();

        initShader(gl);

        // Push the 1st uniform down the path
        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.getSyncPMvMat()); // P, Mv
        st.ownUniform(pmvMatrixUniform);
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }

        // Allocate Vertex Array
        vertices = GLArrayDataServer.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices.putf(-2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf( 2); vertices.putf( 0);
        vertices.putf(-2); vertices.putf(-2); vertices.putf( 0);
        vertices.putf( 2); vertices.putf(-2); vertices.putf( 0);
        vertices.seal(gl, true);
        st.ownAttribute(vertices, true);
        vertices.enableBuffer(gl, false);

        // Allocate Color Array
        colors= GLArrayDataServer.createGLSL("mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(0); colors.putf(0); colors.putf(1); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
        colors.seal(gl, true);
        st.ownAttribute(colors, true);
        colors.enableBuffer(gl, false);

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(Thread.currentThread()+" "+st);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        if(null==st) return;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);

        // Set location in front of camera
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);

        st.uniform(gl, pmvMatrixUniform);

        st.useProgram(gl, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if(null==st) return;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.out.println(Thread.currentThread()+" RedSquare.dispose: "+gl.getContext());

        st.destroy(gl);
        st=null;
        pmvMatrix=null;
        System.out.println(Thread.currentThread()+" RedSquare.dispose: FIN");
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        if(null==st) return;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        final float ang = (window.getTotalFPSDuration() * 360.0f) / 4000.0f;
        pmvMatrix.glRotatef(ang, 0, 0, 1);
        pmvMatrix.glRotatef(ang, 0, 1, 0);
        st.uniform(gl, pmvMatrixUniform);

        // Draw a square
        vertices.enableBuffer(gl, true);
        colors.enableBuffer(gl, true);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        vertices.enableBuffer(gl, false);
        colors.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static boolean oneThread = false;
    public static int swapInterval = -1;
    public static boolean debuggl = false;

    public static void main(final String[] args) {
        NewtFactory.setUseEDT(true); // should be the default
        int type = USE_NEWT ;
        final List threads = new ArrayList();
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-swapi")) {
                i++;
                try {
                    swapInterval = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-debug")) {
                debuggl=true;
            } else if(args[i].equals("-1thread")) {
                oneThread=true;
            } else if(args[i].equals("-awt")) {
                type |= USE_AWT;
            } else if(args[i].startsWith("-GL")) {
                threads.add(new RedSquare(args[i].substring(1), type));
            }
        }
        if(threads.size()==0) {
            threads.add(new RedSquare(GLProfile.GL2ES2, type));
        }

        if(!oneThread) {
            for(final Iterator i = threads.iterator(); i.hasNext(); ) {
                ((Thread)i.next()).start();
            }

            boolean done = false;

            while(!done) {
                int aliveCount = 0;
                for(final Iterator i = threads.iterator(); i.hasNext(); ) {
                    if ( ((Thread)i.next()).isAlive() ) {
                        try {
                            Thread.sleep(100);
                        } catch (final InterruptedException ie) {}
                        aliveCount++;
                    }
                }
                done = 0==aliveCount ;
            }
        } else {
            // init all ..
            for(final Iterator i = threads.iterator(); i.hasNext(); ) {
                ((Thread)i.next()).run();
            }
            while (threads.size()>0) {
                for(final Iterator i = threads.iterator(); i.hasNext(); ) {
                    final RedSquare app = (RedSquare) i.next();
                    if(app.shouldQuit()) {
                        app.shutdown();
                        i.remove();
                    } else {
                        app.display();
                    }
                }
            }
        }
    }
}
