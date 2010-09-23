/*
 *
 * Copyright (c) 2007, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package demos.es2.openmax;

import com.jogamp.common.nio.Buffers;
import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.glsl.fixedfunc.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class Cube implements GLEventListener {
    boolean quit = false;

    public Cube () {
        this(false, false);
    }

    public Cube (boolean useTexCoords, boolean innerCube) {
        this.innerCube = innerCube;

        // Initialize data Buffers
        this.cubeVertices = Buffers.newDirectShortBuffer(s_cubeVertices.length);
        cubeVertices.put(s_cubeVertices);
        cubeVertices.flip();

        this.cubeColors = Buffers.newDirectFloatBuffer(s_cubeColors.length);
        cubeColors.put(s_cubeColors);
        cubeColors.flip();

        this.cubeNormals = Buffers.newDirectByteBuffer(s_cubeNormals.length);
        cubeNormals.put(s_cubeNormals);
        cubeNormals.flip();

        this.cubeIndices = Buffers.newDirectByteBuffer(s_cubeIndices.length);
        cubeIndices.put(s_cubeIndices);
        cubeIndices.flip();
        
        if (useTexCoords) {
            float aspect = 16.0f/9.0f;
            float ss=1f, ts=1f; // scale tex-coord

            ss =     1f/aspect; // b > h, crop width
            for(int i=0; i<s_cubeTexCoords.length; i++) {
                if(s_cubeTexCoords[i]>0) {
                    if ( (i+1) % 2 == 0 ) {
                        // y
                        s_cubeTexCoords[i] *= ts;
                    } else {
                        // x
                        s_cubeTexCoords[i] *= ss;
                    }
                }
            }

            this.cubeTexCoords = Buffers.newDirectFloatBuffer(s_cubeTexCoords.length);
            cubeTexCoords.put(s_cubeTexCoords);
            cubeTexCoords.flip();
        }
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES1 gl = FixedFuncUtil.getFixedFuncImpl(drawable.getGL());

        glu = GLU.createGLU();

        gl.glGenBuffers(4, vboNames, 0);

        if(!innerCube) {
            System.err.println("Entering initialization");
            System.err.println("GL Profile: "+gl.getGLProfile());
            System.err.println("GL:" + gl);
            System.err.println("GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
            System.err.println("GL_EXTENSIONS:");
            System.err.println("  " + gl.glGetString(gl.GL_EXTENSIONS));
            System.err.println("GLF:" + gl);
        }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        float aspect = (height != 0) ? ((float)width / (float)height) : 1.0f;

        GL2ES1 gl = drawable.getGL().getGL2ES1();

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        // JAU gl.glScissor(0, 0, width, height);
        if(innerCube) {
            // Clear background to white
            gl.glClearColor(1.0f, 1.0f, 1.0f, 0.4f);
        } else {
            // Clear background to blue
            gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        }

        if(!innerCube) {
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, light_position, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, light_ambient, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light_diffuse, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, zero_vec4, 0);
            gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, material_spec, 0);

            gl.glEnable(gl.GL_LIGHTING);
            gl.glEnable(gl.GL_LIGHT0);
            gl.glEnable(gl.GL_COLOR_MATERIAL);
        } else {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glDisable(gl.GL_LIGHT0);
        }
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glEnable(gl.GL_NORMALIZE);

        gl.glShadeModel(gl.GL_SMOOTH);
        gl.glDisable(GL.GL_DITHER);

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboNames[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, cubeVertices.limit() * Buffers.SIZEOF_SHORT, cubeVertices, GL.GL_STATIC_DRAW);
        gl.glVertexPointer(3, gl.GL_SHORT, 0, 0);

        gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboNames[1]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, cubeNormals.limit() * Buffers.SIZEOF_BYTE, cubeNormals, GL.GL_STATIC_DRAW);
        gl.glNormalPointer(gl.GL_BYTE, 0, 0);

        gl.glEnableClientState(gl.GL_COLOR_ARRAY);
        if (cubeColors != null) {
            gl.glEnableClientState(gl.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboNames[2]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, cubeColors.limit() * Buffers.SIZEOF_FLOAT, cubeColors, GL.GL_STATIC_DRAW);
            gl.glColorPointer(4, gl.GL_FLOAT, 0, 0);
        }

        if (cubeTexCoords != null) {
            gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboNames[3]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, cubeTexCoords.limit() * Buffers.SIZEOF_SHORT, cubeTexCoords, GL.GL_STATIC_DRAW);
            gl.glTexCoordPointer(2, gl.GL_SHORT, 0, 0);
            gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_INCR);
        } else {
            gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, gl.GL_FASTEST);

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();

        if(!innerCube) {
            glu.gluPerspective(90.0f, aspect, 1.0f, 100.0f);
        } else {
            gl.glOrthof(-20.0f, 20.0f, -20.0f, 20.0f, 1.0f, 40.0f);
        }
        // weird effect ..: gl.glCullFace(gl.GL_FRONT);
    }

    public void dispose(GLAutoDrawable drawable) {
        quit=true;
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES1 gl = drawable.getGL().getGL2ES1();

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glTranslatef(0.f, 0.f, -30.f);
        gl.glRotatef((float)(time * 29.77f), 1.0f, 2.0f, 0.0f);
        gl.glRotatef((float)(time * 22.311f), -0.1f, 0.0f, -5.0f);

        gl.glDrawElements(gl.GL_TRIANGLES, 6 * 6, gl.GL_UNSIGNED_BYTE, cubeIndices);

        time += 0.01f;
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    static final float[] light_position = { -50.f, 50.f, 50.f, 0.f };
    static final float[] light_ambient = { 0.125f, 0.125f, 0.125f, 1.f };
    static final float[] light_diffuse = { 1.0f, 1.0f, 1.0f, 1.f };
    static final float[] material_spec = { 1.0f, 1.0f, 1.0f, 0.f };
    static final float[] zero_vec4 = { 0.0f, 0.0f, 0.0f, 0.f };

    int[] vboNames = new int[4];
    boolean innerCube;
    boolean initialized = false;
    float time = 0.0f;
    ShortBuffer cubeVertices;
    FloatBuffer cubeTexCoords;
    FloatBuffer cubeColors;
    ByteBuffer cubeNormals;
    ByteBuffer cubeIndices;
    private GLU glu;

    private static final short[] s_cubeVertices =
        {
            -10, 10, 10, 10, -10, 10, 10, 10, 10, -10, -10, 10,
            
            -10, 10, -10, 10, -10, -10, 10, 10, -10, -10, -10, -10,
            
            -10, -10, 10, 10, -10, -10, 10, -10, 10, -10, -10, -10,
            
            -10, 10, 10, 10, 10, -10, 10, 10, 10, -10, 10, -10,
            
            10, -10, 10, 10, 10, -10, 10, 10, 10, 10, -10, -10,
            
            -10, -10, 10, -10, 10, -10, -10, 10, 10, -10, -10, -10
        };
    private static final float[] s_cubeTexCoords =
        {
            0, 1f, 1f, 0, 1f, 1f, 0, 0,

            0, 1f, 1f, 0, 1f, 1f, 0, 0,

            0, 1f, 1f, 0, 1f, 1f, 0, 0,

            0, 1f, 1f, 0, 1f, 1f, 0, 0,

            0, 1f, 1f, 0, 1f, 1f, 0, 0,

            0, 1f, 1f, 0, 1f, 1f, 0, 0,
        };

    private static final float[] s_cubeColors =
        {
            40f/255f, 80f/255f, 160f/255f, 255f/255f, 40f/255f, 80f/255f, 160f/255f, 255f/255f,
            40f/255f, 80f/255f, 160f/255f, 255f/255f, 40f/255f, 80f/255f, 160f/255f, 255f/255f,
            
            40f/255f, 80f/255f, 160f/255f, 255f/255f, 40f/255f, 80f/255f, 160f/255f, 255f/255f,
            40f/255f, 80f/255f, 160f/255f, 255f/255f, 40f/255f, 80f/255f, 160f/255f, 255f/255f,
            
            128f/255f, 128f/255f, 128f/255f, 255f/255f, 128f/255f, 128f/255f, 128f/255f, 255f/255f,
            128f/255f, 128f/255f, 128f/255f, 255f/255f, 128f/255f, 128f/255f, 128f/255f, 255f/255f,
            
            128f/255f, 128f/255f, 128f/255f, 255f/255f, 128f/255f, 128f/255f, 128f/255f, 255f/255f,
            128f/255f, 128f/255f, 128f/255f, 255f/255f, 128f/255f, 128f/255f, 128f/255f, 255f/255f,
            
            255f/255f, 110f/255f, 10f/255f, 255f/255f, 255f/255f, 110f/255f, 10f/255f, 255f/255f,
            255f/255f, 110f/255f, 10f/255f, 255f/255f, 255f/255f, 110f/255f, 10f/255f, 255f/255f,
            
            255f/255f, 70f/255f, 60f/255f, 255f/255f, 255f/255f, 70f/255f, 60f/255f, 255f/255f,
            255f/255f, 70f/255f, 60f/255f, 255f/255f, 255f/255f, 70f/255f, 60f/255f, 255
        };
    private static final byte[] s_cubeIndices =
        {
            0, 3, 1, 2, 0, 1, /* front  */
            6, 5, 4, 5, 7, 4, /* back   */
            8, 11, 9, 10, 8, 9, /* top    */
            15, 12, 13, 12, 14, 13, /* bottom */
            16, 19, 17, 18, 16, 17, /* right  */
            23, 20, 21, 20, 22, 21 /* left   */
        };
    private static final byte[] s_cubeNormals =
        {
            0, 0, 127, 0, 0, 127, 0, 0, 127, 0, 0, 127,
            
            0, 0, -128, 0, 0, -128, 0, 0, -128, 0, 0, -128,
            
            0, -128, 0, 0, -128, 0, 0, -128, 0, 0, -128, 0,
            
            0, 127, 0, 0, 127, 0, 0, 127, 0, 0, 127, 0,
            
            127, 0, 0, 127, 0, 0, 127, 0, 0, 127, 0, 0,
            
            -128, 0, 0, -128, 0, 0, -128, 0, 0, -128, 0, 0
        };

    private void run(int type) {
        int width = 800;
        int height = 480;
        System.err.println("Cube.run()");
        try {
            GLCapabilities caps = new GLCapabilities(null);
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            GLWindow window;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps);
                window = GLWindow.create(nWindow);
            } else {
                window = GLWindow.create(caps);
            }

            window.addGLEventListener(this);

            window.enablePerfLog(true);
            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);

            while (!quit && window.getDuration() < 31000) {
                window.display();
            }

            // Shut things down cooperatively
            window.destroy();
            System.out.println("Cube shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
        new Cube().run(type);
        System.exit(0);
    }
}

