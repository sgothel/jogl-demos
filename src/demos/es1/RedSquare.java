package demos.es1;

import java.nio.*;
import javax.media.nwi.*;
import javax.media.opengl.*;
import javax.media.opengl.sub.fixed.*;
import javax.media.opengl.util.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.glsl.fixed.*;
import com.sun.opengl.impl.fixed.GLFixedFuncImpl;

import com.sun.javafx.newt.*;

public class RedSquare implements KeyListener, MouseListener, GLEventListener {

    private GLWindow window;
    private GLU glu;
    private boolean quit = false;

    public void keyPressed(KeyEvent e) { 
        System.out.println(e);
    }
    public void keyReleased(KeyEvent e) { 
        System.out.println(e);
    }
    public void keyTyped(KeyEvent e) { 
        System.out.println(e);
    }

    public void mouseClicked(MouseEvent e) {
        System.out.println("mouseevent: "+e);
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

    private void run(int type) {
        int width = 800;
        int height = 480;
        System.err.println("RedSquare.run()");
        GLProfile.setProfileGLAny();
        try {
            NWCapabilities caps = new NWCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NewtFactory.AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NewtFactory.AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NewtFactory.AWT, nScreen, caps);
            }
            window = GLWindow.create(nWindow, caps);

            window.addMouseListener(this);
            window.addKeyListener(this);
            window.addGLEventListener(this);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            window.enablePerfLog(true);
            // Size OpenGL to Video Surface
            window.setSize(width, height);
            // window.setFullscreen(true);
            window.setVisible(true);

            while (!quit && window.getDuration() < 20000) {
                window.display();
            }

            // Shut things down cooperatively
            window.close();
            window.getFactory().shutdown();
            System.out.println("RedSquare shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // FIXME: we must add storage of the pointers in the GL state to
    // the GLImpl classes. The need for this can be seen by making
    // these variables method local instead of instance members. The
    // square will disappear after a second or so due to garbage
    // collection. On desktop OpenGL this implies a stack of
    // references due to the existence of glPush/PopClientAttrib. On
    // OpenGL ES 1/2 it can simply be one set of references.
    private FloatBuffer colors;
    private FloatBuffer vertices;

    public void init(GLAutoDrawable drawable) {
        GLFixedFuncIf gl;
        {
            GL _gl = drawable.getGL();
            if(!GLFixedFuncUtil.isGLFixedFuncIf(_gl)) {
                if(_gl.isGLES2()) {
                    gl = new GLFixedFuncImpl(_gl, new FixedFuncHook(_gl.getGL2ES2()));
                } else {
                    gl = new GLFixedFuncImpl(_gl, _gl.getGL2ES1());
                }
                _gl.getContext().setGL(gl);
            } else {
                gl = GLFixedFuncUtil.getGLFixedFuncIf(_gl);
            }
        }

        System.err.println("Entering initialization");
        System.err.println("GL Profile: "+GLProfile.getProfile());
        System.err.println("GL:" + gl);
        System.err.println("GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
        System.err.println("GL_EXTENSIONS:");
        System.err.println("  " + gl.glGetString(gl.GL_EXTENSIONS));

        glu = GLU.createGLU();

        // Allocate vertex arrays
        colors   = BufferUtil.newFloatBuffer(16);
        vertices = BufferUtil.newFloatBuffer(12);
        // Fill them up
        colors.put( 0, 1);    colors.put( 1, 0);     colors.put( 2, 0);    colors.put( 3, 1);
        colors.put( 4, 0);    colors.put( 5, 0);     colors.put( 6, 1);    colors.put( 7, 1);
        colors.put( 8, 1);    colors.put( 9, 0);     colors.put(10, 0);    colors.put(11, 1);
        colors.put(12, 1);    colors.put(13, 0);     colors.put(14, 0);    colors.put(15, 1);
        vertices.put(0, -2);  vertices.put( 1,  2);  vertices.put( 2,  0);
        vertices.put(3,  2);  vertices.put( 4,  2);  vertices.put( 5,  0);
        vertices.put(6, -2);  vertices.put( 7, -2);  vertices.put( 8,  0);
        vertices.put(9,  2);  vertices.put(10, -2);  vertices.put(11,  0);

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertices);
        gl.glColorPointer(4, GL.GL_FLOAT, 0, colors);

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GLFixedFuncIf gl = GLFixedFuncUtil.getGLFixedFuncIf(drawable.getGL());
        // Set location in front of camera
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //gl.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        //glu.gluLookAt(0, 0, -20, 0, 0, 0, 0, 1, 0);
    }

    public void display(GLAutoDrawable drawable) {
        GLFixedFuncIf gl = GLFixedFuncUtil.getGLFixedFuncIf(drawable.getGL());
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -10);
        float ang = ((float) window.getDuration() * 360.0f) / 4000.0f;
        gl.glRotatef(ang, 0, 0, 1);
        gl.glRotatef(ang, 0, 1, 0);


        // Draw a square
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String[] args) {
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
        }
        new RedSquare().run(type);
        System.exit(0);
    }
}
