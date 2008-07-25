package demos.es2;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.util.*;
import javax.media.opengl.glu.*;

import com.sun.javafx.newt.*;

public class RedSquare implements MouseListener, GLEventListener {

    private GLWindow window;
    private GLU glu;
    private boolean quit = false;
    private long startTime;
    private long curTime;

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
        GLProfile.setProfileGL2ES2();
        try {
            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NewtFactory.AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NewtFactory.AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NewtFactory.AWT, nScreen, 0); // dummy VisualID
            }

            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            window = GLWindow.create(nWindow, caps);

            window.addMouseListener(this);
            window.addGLEventListener(this);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);

            startTime = System.currentTimeMillis();
            while (!quit && ((curTime = System.currentTimeMillis()) - startTime) < 20000) {
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

    public static final int VERTEX_ARRAY = 0;
    public static final int COLOR_ARRAY = 1;

    boolean shaderOk = false;
    IntBuffer fragShader = BufferUtil.newIntBuffer(1);
    IntBuffer vertShader = BufferUtil.newIntBuffer(1);
    int shaderProgram=-1;
    int shaderPMVMatrix=-1;
    PMVMatrix pmvMatrix;

    public static final String[][] vertShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "uniform MEDIUMP mat4    mgl_PMVMatrix[2];\n"+
        "attribute HIGHP vec4    mgl_Vertex;\n"+
        "attribute HIGHP vec4    mgl_Color;\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "void main(void)\n"+
        "{\n"+
        "  frontColor=mgl_Color;\n"+
        "  gl_Position = mgl_PMVMatrix[0] * mgl_PMVMatrix[1] * mgl_Vertex;\n"+
        "}\n" } } ;

    public static final String[][] fragShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "void main (void)\n"+
        "{\n"+
        "    gl_FragColor = frontColor;\n"+
        "}\n" } } ;


    private void initShader(GL2ES2 gl) {
        int tmpI;

        // Create & Compile the vertex shader object
        tmpI = gl.glCreateShader(gl.GL_VERTEX_SHADER);
        vertShader.put(tmpI);
        vertShader.flip();

        gl.glShaderBinaryOrSource(vertShader, 0, null, vertShaderSource);
        gl.glCompileShader(vertShader.get(0));
        if ( ! gl.glIsShaderStatusValid(vertShader.get(0), gl.GL_COMPILE_STATUS) ) {
                System.err.println("Failed to compile vertex shader: id "+vertShader.get(0)+
                                   "\n\t"+gl.glGetShaderInfoLog(vertShader.get(0)));
            return;
        }

        // Create & Compile the fragment shader object
        tmpI = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
        fragShader.put(tmpI);
        fragShader.flip();

        gl.glShaderBinaryOrSource(fragShader, 0, null, fragShaderSource);

        gl.glCompileShader(fragShader.get(0));

        if ( ! gl.glIsShaderStatusValid(fragShader.get(0), gl.GL_COMPILE_STATUS) ) {
                System.err.println("Failed to compile fragment shader: id "+fragShader.get(0)+
                                   "\n\t"+gl.glGetShaderInfoLog(fragShader.get(0)));
            return;
        }
        
        // Create the shader program
        shaderProgram = gl.glCreateProgram();

        // Attach the fragment and vertex shaders to it
        gl.glAttachShader(shaderProgram, fragShader.get(0));
        gl.glAttachShader(shaderProgram, vertShader.get(0));

        gl.glBindAttribLocation(shaderProgram, VERTEX_ARRAY, "mgl_Vertex");
        gl.glBindAttribLocation(shaderProgram, COLOR_ARRAY, "mgl_Color");

        // Link the program
        gl.glLinkProgram(shaderProgram);

        if ( ! gl.glIsProgramValid(shaderProgram, System.err) )  {
            return;
        }

        gl.glUseProgram(shaderProgram);

        pmvMatrix.glMatrixMode(gl.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(gl.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        shaderPMVMatrix = gl.glGetUniformLocation(shaderProgram, "mgl_PMVMatrix");
        if(0<=shaderPMVMatrix) {
            gl.glUniformMatrix4fv(shaderPMVMatrix, 2, false, pmvMatrix.glGetPMVMatrixf());
        } else {
            System.err.println("could not get uniform mgl_PMVMatrix: "+shaderPMVMatrix);
            return;
        }

        shaderOk = true;

    }

    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        glu = GLU.createGLU();
        System.err.println("Entering initialization");
        System.err.println("GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
        System.err.println("GL_EXTENSIONS:");
        System.err.println("  " + gl.glGetString(gl.GL_EXTENSIONS));

        if(gl.isGLES2()) {
            pmvMatrix = gl.getGLES2().getPMVMatrix();
        } else {
            pmvMatrix = new PMVMatrix();
        }

        initShader(gl);

        // Allocate vertex arrays
        colors   = BufferUtil.newFloatBuffer(16);
        vertices = BufferUtil.newFloatBuffer(12);
        // Fill them up
        colors.put( 1);    colors.put( 0);     colors.put( 0);    colors.put( 1);
        colors.put( 0);    colors.put( 0);     colors.put( 1);    colors.put( 1);
        colors.put( 1);    colors.put( 0);     colors.put( 0);    colors.put( 1);
        colors.put( 1);    colors.put( 0);     colors.put( 0);    colors.put( 1);
        colors.flip();

        vertices.put(-2);  vertices.put(  2);  vertices.put( 0);
        vertices.put( 2);  vertices.put(  2);  vertices.put( 0);
        vertices.put(-2);  vertices.put( -2);  vertices.put( 0);
        vertices.put( 2);  vertices.put( -2);  vertices.put( 0);
        vertices.flip();
    
        gl.glEnableVertexAttribArray(VERTEX_ARRAY);
        gl.glVertexAttribPointer(VERTEX_ARRAY, 3, gl.GL_FLOAT, false, 0, vertices);

        gl.glEnableVertexAttribArray(COLOR_ARRAY);
        gl.glVertexAttribPointer(COLOR_ARRAY, 4, gl.GL_FLOAT, false, 0, colors);

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        gl.glUseProgram(0);

    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        // Set location in front of camera
        pmvMatrix.glMatrixMode(GL2ES2.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);

        if(0<=shaderPMVMatrix) {
            gl.glUniformMatrix4fv(shaderPMVMatrix, 2, false, pmvMatrix.glGetPMVMatrixf());
        }
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glUseProgram(shaderProgram);

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        pmvMatrix.glMatrixMode(gl.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        float ang = ((float) (curTime - startTime) * 360.0f) / 4000.0f;
        pmvMatrix.glRotatef(ang, 0, 0, 1);
        pmvMatrix.glRotatef(ang, 0, 1, 0);

        if(0<=shaderPMVMatrix) {
            gl.glUniformMatrix4fv(shaderPMVMatrix, 2, false, pmvMatrix.glGetPMVMatrixf());
        }

        // Draw a square
        gl.glDrawArrays(gl.GL_TRIANGLE_STRIP, 0, 4);

        gl.glUseProgram(0);

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
