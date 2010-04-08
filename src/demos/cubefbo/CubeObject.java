/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package demos.cubefbo;

import com.jogamp.common.nio.Buffers;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2;

class CubeObject {

    CubeObject (boolean useTexCoords) {
        // Initialize data Buffers
        this.cubeVertices = Buffers.newDirectShortBuffer(s_cubeVertices.length);
        cubeVertices.put(s_cubeVertices);
        cubeVertices.rewind();

        this.cubeColors = Buffers.newDirectByteBuffer(s_cubeColors.length);
        cubeColors.put(s_cubeColors);
        cubeColors.rewind();

        this.cubeNormals = Buffers.newDirectByteBuffer(s_cubeNormals.length);
        cubeNormals.put(s_cubeNormals);
        cubeNormals.rewind();

        this.cubeIndices = Buffers.newDirectByteBuffer(s_cubeIndices.length);
        cubeIndices.put(s_cubeIndices);
        cubeIndices.rewind();

        if (useTexCoords) {
            this.cubeTexCoords = Buffers.newDirectShortBuffer(s_cubeTexCoords.length);
            cubeTexCoords.put(s_cubeTexCoords);
            cubeTexCoords.rewind();
        }
    }

    private void perspective(GL2 gl, float fovy, float aspect, float zNear, float zFar) {
        float xmin;
        float xmax;
        float ymin;
        float ymax;

        ymax = zNear * (float)Math.tan((fovy * Math.PI) / 360.0);
        ymin = -ymax;
        xmin = ymin * aspect;
        xmax = ymax * aspect;

        gl.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
    }

    static final float[] light_position = { -50.f, 50.f, 50.f, 0.f };
    static final float[] light_ambient = { 0.125f, 0.125f, 0.125f, 1.f };
    static final float[] light_diffuse = { 1.0f, 1.0f, 1.0f, 1.f };
    static final float[] material_spec = { 1.0f, 1.0f, 1.0f, 0.f };
    static final float[] zero_vec4 = { 0.0f, 0.0f, 0.0f, 0.f };

    public void dispose(GL2 gl) {
        gl.glDisableClientState(GL2ES1.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2ES1.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL2ES1.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);
        this.cubeVertices.clear();
        this.cubeVertices=null;
        this.cubeColors.clear();
        this.cubeColors=null;
        this.cubeNormals.clear();
        this.cubeNormals=null;
        this.cubeIndices.clear();
        this.cubeIndices=null;
        if(null!=this.cubeTexCoords) {
            this.cubeTexCoords.clear();
            this.cubeTexCoords=null;
        }
    }

    public void reshape(GL2 gl, int x, int y, int width, int height) {
        float aspect = (height != 0) ? ((float)width / (float)height) : 1.0f;

        gl.glViewport(0, 0, width, height);
        gl.glScissor(0, 0, width, height);

        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_POSITION, light_position, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_AMBIENT, light_ambient, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_DIFFUSE, light_diffuse, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_SPECULAR, zero_vec4, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2ES1.GL_SPECULAR, material_spec, 0);

        gl.glEnable(GL2ES1.GL_NORMALIZE);
        gl.glEnable(GL2ES1.GL_LIGHTING);
        gl.glEnable(GL2ES1.GL_LIGHT0);
        gl.glEnable(GL2ES1.GL_COLOR_MATERIAL);
        gl.glEnable(GL.GL_CULL_FACE);

        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_FASTEST);

        gl.glShadeModel(GL2ES1.GL_SMOOTH);
        gl.glDisable(GL.GL_DITHER);

        gl.glClearColor(0.0f, 0.1f, 0.0f, 1.0f);

        gl.glEnableClientState(GL2ES1.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2ES1.GL_NORMAL_ARRAY);
        gl.glEnableClientState(GL2ES1.GL_COLOR_ARRAY);
        if (cubeTexCoords != null) {
            gl.glEnableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);
        } else {
            gl.glDisableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);
        }

        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();

        perspective(gl, 55.f, aspect, 0.1f, 100.f);
        gl.glCullFace(GL.GL_BACK);
    }

    public void display(GL2 gl, float xRot, float yRot) {
        //        System.out.println("CubeObject .. p1: "+this);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Draw a green square using MIDP
        //g.setColor(0, 255, 0);
        //g.fillRect(20, 20, width - 40, height - 40);

        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glTranslatef(0.f, 0.f, -30.f);
        //        gl.glTranslatef(0.f, 0.f, -30.f);
        //        gl.glRotatef((float)(time * 29.77f), 1.0f, 2.0f, 0.0f);
        //        gl.glRotatef((float)(time * 22.311f), -0.1f, 0.0f, -5.0f);
        gl.glRotatef(yRot, 0, 1, 0);
        gl.glRotatef(xRot, 1, 0, 0);

        gl.glVertexPointer(3, GL.GL_SHORT, 0, cubeVertices);
        gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cubeColors);
        gl.glNormalPointer(GL.GL_BYTE, 0, cubeNormals);
        if (cubeTexCoords != null) {
            gl.glTexCoordPointer(2, GL.GL_SHORT, 0, cubeTexCoords);
            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
        }

        //        System.out.println("CubeObject .. p8: "+this);
        gl.glDrawElements(GL.GL_TRIANGLES, 6 * 6, GL.GL_UNSIGNED_BYTE, cubeIndices);
        //        System.out.println("CubeObject .. p9: "+this);

        //        time += 0.01f;
    }

    boolean initialized = false;
    //    float time = 0.0f;

    ShortBuffer cubeVertices;
    ShortBuffer cubeTexCoords;
    //    FloatBuffer cubeTexCoords;
    ByteBuffer cubeColors;
    ByteBuffer cubeNormals;
    ByteBuffer cubeIndices;

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

    private static final byte[] s_cubeColors =
        {
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,

            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,

            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,

            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,

            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,

            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255,
            (byte) 0, (byte) 128, (byte) 0, (byte) 255, (byte) 0, (byte) 128, (byte) 0, (byte) 255
        };

    /*
    private static final byte[] s_cubeColors =
        {
            (byte)40, (byte)80, (byte)160, (byte)255, (byte)40, (byte)80, (byte)160, (byte)255,
            (byte)40, (byte)80, (byte)160, (byte)255, (byte)40, (byte)80, (byte)160, (byte)255,
            
            (byte)40, (byte)80, (byte)160, (byte)255, (byte)40, (byte)80, (byte)160, (byte)255,
            (byte)40, (byte)80, (byte)160, (byte)255, (byte)40, (byte)80, (byte)160, (byte)255,
            
            (byte)128, (byte)128, (byte)128, (byte)255, (byte)128, (byte)128, (byte)128, (byte)255,
            (byte)128, (byte)128, (byte)128, (byte)255, (byte)128, (byte)128, (byte)128, (byte)255,
            
            (byte)128, (byte)128, (byte)128, (byte)255, (byte)128, (byte)128, (byte)128, (byte)255,
            (byte)128, (byte)128, (byte)128, (byte)255, (byte)128, (byte)128, (byte)128, (byte)255,
            
            (byte)255, (byte)110, (byte)10, (byte)255, (byte)255, (byte)110, (byte)10, (byte)255,
            (byte)255, (byte)110, (byte)10, (byte)255, (byte)255, (byte)110, (byte)10, (byte)255,
            
            (byte)255, (byte)70, (byte)60, (byte)255, (byte)255, (byte)70, (byte)60, (byte)255,
            (byte)255, (byte)70, (byte)60, (byte)255, (byte)255, (byte)70, (byte)60, (byte)255
        };
    */

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
}

