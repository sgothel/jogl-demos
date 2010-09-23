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
package demos.es1.cube;

import com.jogamp.common.nio.Buffers;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.fixedfunc.*;
import java.nio.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class CubeImmModeSink implements GLEventListener {
    boolean quit = false;

    public CubeImmModeSink () {
        this(false, false);
    }

    private static boolean VBO_CACHE = true;

    ByteBuffer cubeIndices=null;
    ImmModeSink vboCubeF = null;
    public void drawCube(GL2ES1 gl, float extent) {
        if(cubeIndices==null) {
            cubeIndices = Buffers.newDirectByteBuffer(s_cubeIndices);
        }
        
        if(vboCubeF==null) {
            ImmModeSink vbo = ImmModeSink.createFixed(gl, GL.GL_STATIC_DRAW, 36,
                                  3, GL.GL_SHORT,  // vertex
                                  4, GL.GL_FLOAT,  // color
                                  3, GL.GL_BYTE,  // normal
                                  0, GL.GL_FLOAT); // texture

            vbo.glBegin(GL.GL_TRIANGLES);

            vbo.glVertexv(Buffers.newDirectShortBuffer(s_cubeVertices));
            vbo.glColorv(Buffers.newDirectFloatBuffer(s_cubeColors));
            vbo.glNormalv(Buffers.newDirectByteBuffer(s_cubeNormals));

            if(VBO_CACHE) {
                vbo.glEnd(gl, false);
            } else {
                vbo.glEnd(gl, cubeIndices);
            }
            System.err.println("VBO Cube");
            System.err.println(vbo);
            if(VBO_CACHE) {
                vboCubeF = vbo;
            }
        }
        if(null!=vboCubeF) {
            vboCubeF.draw(gl, cubeIndices, true);
        }
    }

    private GLUquadric sphere=null;
    private ImmModeSink vboSphere=null;
    public void drawSphere(GL2ES1 gl, float radius, int slices, int stacks) {
        if(sphere==null) {
            sphere = glu.gluNewQuadric();
            sphere.enableImmModeSink(true);
            sphere.setImmMode((VBO_CACHE)?false:true);
        }
        ImmModeSink vbo = vboSphere;
        if (vbo == null) {
            glu.gluSphere(sphere, radius, 8, 8);
            if(VBO_CACHE) {
                vboSphere = sphere.replaceImmModeSink();
                vbo = vboSphere;
            } 
            System.err.println("VBO Sphere");
            System.err.println(vbo);
        }

        if(VBO_CACHE && null!=vbo) {
            vbo.draw(gl, true);
        }
    }


    private GLUquadric cylinder=null;
    private ImmModeSink vboCylinder=null;
    public void drawCylinder(GL2ES1 gl, float radius, float halfHeight, int upAxis) {
        if(cylinder==null) {
            cylinder = glu.gluNewQuadric();
            cylinder.enableImmModeSink(true);
            cylinder.setImmMode((VBO_CACHE)?false:true);
        }

        gl.glPushMatrix();
        switch (upAxis) {
            case 0:
                gl.glRotatef(-90f, 0.0f, 1.0f, 0.0f);
                gl.glTranslatef(0.0f, 0.0f, -halfHeight);
                break;
            case 1:
                gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                gl.glTranslatef(0.0f, 0.0f, -halfHeight);
                break;
            case 2:
                gl.glTranslatef(0.0f, 0.0f, -halfHeight);
                break;
            default: {
                assert (false);
            }
        }

        ImmModeSink vbo = vboCylinder;
        if (vbo == null) {
            glu.gluQuadricDrawStyle(cylinder, glu.GLU_FILL);
            glu.gluQuadricNormals(cylinder, glu.GLU_SMOOTH);
            glu.gluCylinder(cylinder, radius, radius, 2f * halfHeight, 15, 10);
            if(VBO_CACHE) {
                vboCylinder = cylinder.replaceImmModeSink();
                vbo = vboCylinder;
            } 
            System.err.println("VBO Cylinder");
            System.err.println(vbo);
        }

        if(VBO_CACHE && null!=vbo) {
            vbo.draw(gl, true);
        }

        gl.glPopMatrix();
    }



    public CubeImmModeSink (boolean useTexCoords, boolean innerCube) {
        this.innerCube = innerCube;
        this.useTexCoords = useTexCoords;
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES1 gl = FixedFuncUtil.getFixedFuncImpl(drawable.getGL());

        glu = GLU.createGLU();

        if(!innerCube) {
            System.err.println("Entering initialization");
            System.err.println("GL Profile: "+gl.getGLProfile());
            System.err.println("GL:" + gl);
            System.err.println("GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
            System.err.println("GL_EXTENSIONS:");
            System.err.println("  " + gl.glGetString(gl.GL_EXTENSIONS));
            System.err.println("GLF:" + gl);
        }

        gl.glGetError(); // flush error ..

        // Debug ..
        // DebugGL2 gl2dbg = new DebugGL2(gl.getGL2());
        // gl.getContext().setGL(gl2dbg);

        // Trace ..
        // TraceGL2 gl2trace = new TraceGL2(gl.getGL2(), System.err);
        // gl.getContext().setGL(gl2trace);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        float aspect = (height != 0) ? ((float)width / (float)height) : 1.0f;

        GL2ES1 gl = drawable.getGL().getGL2ES1();

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glScissor(0, 0, width, height);
        if(innerCube) {
            // Clear background to white
            gl.glClearColor(1.0f, 1.0f, 1.0f, 0.6f);
        } else {
            // Clear background to blue
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, light_position, 0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, light_ambient, 0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light_diffuse, 0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, zero_vec4, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, gl.GL_SPECULAR, material_spec, 0);
        gl.glEnable(gl.GL_NORMALIZE);

        gl.glEnable(gl.GL_LIGHTING);
        gl.glEnable(gl.GL_LIGHT0);
        gl.glEnable(gl.GL_COLOR_MATERIAL);
        gl.glEnable(GL.GL_CULL_FACE);

        gl.glShadeModel(gl.GL_SMOOTH);
        gl.glDisable(gl.GL_DITHER);

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

        gl.glTranslatef(0.f, 0.0f, -30.f);
        gl.glRotatef((float)(time * 29.77f), 1.0f, 2.0f, 0.0f);
        gl.glRotatef((float)(time * 22.311f), -0.1f, 0.0f, -5.0f);

        gl.glColor4f(0f, 0f, 0f, 1f);

        if(true) {
            //gl.glColor4f(1f, 0f, 0f, 1f); // RED inside the color4f's
            drawCube(gl, 10.0f);
        }

        if(true) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor4f(0f, 1f, 0f, 1f);
            gl.glPushMatrix();
            gl.glTranslatef(15.0f, 0.0f, 0.0f);
            drawSphere(gl, 5.0f, 10, 10);
            gl.glPopMatrix();
            gl.glEnable(gl.GL_LIGHTING);
        }

        if(true) {
            gl.glColor4f(0f, 0f, 1f, 1f);
            gl.glPushMatrix();
            gl.glMultMatrixf(identity4x4f, 0);
            drawCylinder(gl, 4.0f, 10.0f, 1);
            gl.glPopMatrix();
        }

        frameNum++;
        time += 0.01f;
    }
    int frameNum=0;

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    static final float[] light_position = { -50.f, 50.f, 50.f, 0.f };
    static final float[] light_ambient = { 0.125f, 0.125f, 0.125f, 1.f };
    static final float[] light_diffuse = { 1.0f, 1.0f, 1.0f, 1.f };
    static final float[] material_spec = { 1.0f, 1.0f, 1.0f, 0.f };
    static final float[] zero_vec4 = { 0.0f, 0.0f, 0.0f, 0.f };
    static final float[] identity4x4f = { 1.0f, 0.0f, 0.0f, 0.0f,
                                          0.0f, 1.0f, 0.0f, 0.0f,
                                          0.0f, 0.0f, 1.0f, 0.0f,
                                          0.0f, 0.0f, 0.0f, 1.0f };
    private static final short[] s_cubeVertices =
        {
            -10, 10, 10, 10, -10, 10, 10, 10, 10, -10, -10, 10,
            
            -10, 10, -10, 10, -10, -10, 10, 10, -10, -10, -10, -10,
            
            -10, -10, 10, 10, -10, -10, 10, -10, 10, -10, -10, -10,
            
            -10, 10, 10, 10, 10, -10, 10, 10, 10, -10, 10, -10,
            
            10, -10, 10, 10, 10, -10, 10, 10, 10, 10, -10, -10,
            
            -10, -10, 10, -10, 10, -10, -10, 10, 10, -10, -10, -10
        };
    private static final short[] s_cubeTexCoords =
        {
            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,

            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,

            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,

            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,

            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,

            0, (short) 0xffff, (short) 0xffff, 0, (short) 0xffff, (short) 0xffff, 0, 0,
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


    boolean innerCube;
    boolean useTexCoords;
    boolean initialized = false;
    float time = 0.0f;
    private GLU glu;

    private void run(int type) {
        int width = 800;
        int height = 480;
        System.err.println("CubeImmModeSink.run()");
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
                nWindow.setUndecorated(false);
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

            long curTime;
            long startTime = System.currentTimeMillis();
            while (!quit && ((curTime = System.currentTimeMillis()) - startTime) < 31000) {
                window.display();
            }

            // Shut things down cooperatively
            window.destroy();
            System.out.println("CubeImmModeSink shut down cleanly.");
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
        new CubeImmModeSink().run(type);
        System.exit(0);
    }
}

