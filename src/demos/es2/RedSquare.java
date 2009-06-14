package demos.es2;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;

import com.sun.opengl.util.*;
import com.sun.opengl.util.glsl.*;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.opengl.*;

public class RedSquare extends Thread implements WindowListener, MouseListener, GLEventListener {

    private GLWindow window;
    private GLProfile glp;
    private GLU glu;
    private boolean quit = false;
    private long startTime;
    private long curTime;
    private String glprofile;
    private int type;
    
    public RedSquare(String glprofile, int type) {
        super();
        this.glprofile=glprofile;
        this.type=type;
    }

    public void windowResized(WindowEvent e) { }

    public void windowMoved(WindowEvent e) { }

    public void windowGainedFocus(WindowEvent e) { }

    public void windowLostFocus(WindowEvent e) { }

    public void windowDestroyNotify(WindowEvent e) {
        quit = true;
    }

    public void mouseClicked(MouseEvent e) {
        System.out.println(glp+" mouseevent: "+e);
        switch(e.getClickCount()) {
            case 1:
                window.setFullscreen(!window.isFullscreen());
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

    public void run() {
        System.err.println(glp+" RedSquare.run()");
        int width = 800;
        int height = 480;
        glp = GLProfile.GetProfile(glprofile);
        try {
            GLCapabilities caps = new GLCapabilities(glp);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
            }
            window = GLWindow.create(nWindow, caps);

            window.addWindowListener(this);
            window.addMouseListener(this);
            window.addGLEventListener(this);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            // window.setFullscreen(true);
            window.setVisible(true);
            window.enablePerfLog(true);

            startTime = System.currentTimeMillis();

            while (!quit && ((curTime = System.currentTimeMillis()) - startTime) < 20000) {
                window.display();
            }

            // Shut things down cooperatively
            window.destroy();
            window.getFactory().shutdown();
            System.out.println(glp+" RedSquare shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    ShaderState st;
    PMVMatrix pmvMatrix;

    private void initShader(GL2ES2 gl) {
        int tmpI;

        // Create & Compile the shader objects
        ShaderCode rsVp = ShaderCode.create(gl, gl.GL_VERTEX_SHADER, 1, RedSquare.class,
                                            "shader", "shader/bin", "redsquare");
        ShaderCode rsFp = ShaderCode.create(gl, gl.GL_FRAGMENT_SHADER, 1, RedSquare.class,
                                            "shader", "shader/bin", "redsquare");

        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp);
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        glu = GLU.createGLU();
        System.err.println(glp+" Entering initialization");
        System.err.println(glp+" GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
        System.err.println(glp+" GL_EXTENSIONS:");
        System.err.println(glp+"   " + gl.glGetString(gl.GL_EXTENSIONS));

        pmvMatrix = new PMVMatrix();

        initShader(gl);

        // Push the 1st uniform down the path 
        st.glUseProgram(gl, true);

        pmvMatrix.glMatrixMode(pmvMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(pmvMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        // Allocate vertex arrays
        GLArrayDataClient vertices = GLArrayDataClient.createGLSL(gl, "mgl_Vertex", 3, gl.GL_FLOAT, false, 4);
        {
            // Fill them up
            FloatBuffer verticeb = (FloatBuffer)vertices.getBuffer();
            verticeb.put(-2);  verticeb.put(  2);  verticeb.put( 0);
            verticeb.put( 2);  verticeb.put(  2);  verticeb.put( 0);
            verticeb.put(-2);  verticeb.put( -2);  verticeb.put( 0);
            verticeb.put( 2);  verticeb.put( -2);  verticeb.put( 0);
        }
        vertices.seal(gl, true);

        GLArrayDataClient colors = GLArrayDataClient.createGLSL(gl, "mgl_Color",  4, gl.GL_FLOAT, false, 4);
        {
            // Fill them up
            FloatBuffer colorb = (FloatBuffer)colors.getBuffer();
            colorb.put( 1);    colorb.put( 0);     colorb.put( 0);    colorb.put( 1);
            colorb.put( 0);    colorb.put( 0);     colorb.put( 1);    colorb.put( 1);
            colorb.put( 1);    colorb.put( 0);     colorb.put( 0);    colorb.put( 1);
            colorb.put( 1);    colorb.put( 0);     colorb.put( 0);    colorb.put( 1);
        }
        colors.seal(gl, true);
        
        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        st.glUseProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(glp+" "+st);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.glUseProgram(gl, true);

        // Set location in front of camera
        pmvMatrix.glMatrixMode(pmvMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);

        GLUniformData ud = st.getUniform("mgl_PMVMatrix");
        if(null!=ud) {
            // same data object
            st.glUniform(gl, ud);
        } 

        st.glUseProgram(gl, false);
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.out.println(glp+" RedSquare.dispose: "+gl.getContext());

        st.destroy(gl);
        st=null;
        pmvMatrix.destroy();
        pmvMatrix=null;
        System.out.println(glp+" RedSquare.dispose: fin");
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.glUseProgram(gl, true);

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        pmvMatrix.glMatrixMode(pmvMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        float ang = ((float) (curTime - startTime) * 360.0f) / 4000.0f;
        pmvMatrix.glRotatef(ang, 0, 0, 1);
        pmvMatrix.glRotatef(ang, 0, 1, 0);

        GLUniformData ud = st.getUniform("mgl_PMVMatrix");
        if(null!=ud) {
            // same data object
            st.glUniform(gl, ud);
        } 

        // Draw a square
        gl.glDrawArrays(gl.GL_TRIANGLE_STRIP, 0, 4);

        st.glUseProgram(gl, false);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        List threads = new ArrayList();
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
            if(args[i].startsWith("-GL")) {
                threads.add(new RedSquare(args[i].substring(1), type));
            }
        }
        if(threads.size()==0) {
            threads.add(new RedSquare(null, type));
        }
        Thread firstT = (Thread) threads.remove(0);

        for(Iterator i = threads.iterator(); i.hasNext(); ) {
            ((Thread)i.next()).start();
        }

        // always run the first on main ..
        firstT.run();

        boolean done = false;

        while(!done) {
            int aliveCount = 0;
            for(Iterator i = threads.iterator(); i.hasNext(); ) {
                if ( ((Thread)i.next()).isAlive() ) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {}
                    aliveCount++;
                }
            }
            done = 0==aliveCount ;
        }
    }
}
