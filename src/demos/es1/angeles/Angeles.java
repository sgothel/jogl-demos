/* San Angeles Observation OpenGL ES version example
 * Copyright 2004-2005 Jetro Lauha
 * All rights reserved.
 * Web: http://iki.fi/jetro/
 *
 * This source is free software; you can redistribute it and/or
 * modify it under the terms of EITHER:
 *   (1) The GNU Lesser General Public License as published by the Free
 *       Software Foundation; either version 2.1 of the License, or (at
 *       your option) any later version. The text of the GNU Lesser
 *       General Public License is included with this source in the
 *       file LICENSE-LGPL.txt.
 *   (2) The BSD-style license that is included with this source in
 *       the file LICENSE-BSD.txt.
 *
 * This source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files
 * LICENSE-LGPL.txt and LICENSE-BSD.txt for more details.
 *
 * $Id$
 * $Revision$
 */

package demos.es1.angeles;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import java.nio.*;

public class Angeles /* implements GLEventListener */ {

    public Angeles() {
        quadVertices = BufferUtil.newIntBuffer(12);
        quadVertices.put(new int[]{
            -0x10000, -0x10000,
             0x10000, -0x10000,
            -0x10000,  0x10000,
             0x10000, -0x10000,
             0x10000,  0x10000,
            -0x10000,  0x10000
        });
        quadVertices.rewind();

        light0Position=BufferUtil.newIntBuffer(4);
        light0Diffuse=BufferUtil.newIntBuffer(4);
        light1Position=BufferUtil.newIntBuffer(4);
        light1Diffuse=BufferUtil.newIntBuffer(4);
        light2Position=BufferUtil.newIntBuffer(4);
        light2Diffuse=BufferUtil.newIntBuffer(4);
        materialSpecular=BufferUtil.newIntBuffer(4);

        light0Position.put(new int[] { -0x40000, 0x10000, 0x10000, 0 });
        light0Diffuse.put(new int[] { 0x10000, 0x6666, 0, 0x10000 });
        light1Position.put(new int[] { 0x10000, -0x20000, -0x10000, 0 });
        light1Diffuse.put(new int[] { 0x11eb, 0x23d7, 0x5999, 0x10000 });
        light2Position.put(new int[] { -0x10000, 0, -0x40000, 0 });
        light2Diffuse.put(new int[] { 0x11eb, 0x2b85, 0x23d7, 0x10000 });
        materialSpecular.put(new int[] { 0x10000, 0x10000, 0x10000, 0x10000 });

        light0Position.rewind();
        light0Diffuse.rewind();
        light1Position.rewind();
        light1Diffuse.rewind();
        light2Position.rewind();
        light2Diffuse.rewind();
        materialSpecular.rewind();

        seedRandom(15);

        width=0;
        height=0;
        x=0;
        y=0;
    }

    public void init(GL gl) {
        // FIXME: gl.setSwapInterval(1);

        this.gl = gl;
        gl.glEnable(gl.GL_NORMALIZE);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_CULL_FACE);
        gl.glShadeModel(gl.GL_FLAT);

        gl.glEnable(gl.GL_LIGHTING);
        gl.glEnable(gl.GL_LIGHT0);
        gl.glEnable(gl.GL_LIGHT1);
        gl.glEnable(gl.GL_LIGHT2);

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

        for (int a = 0; a < SuperShape.COUNT; ++a)
        {
            sSuperShapeObjects[a] = createSuperShape(SuperShape.sParams[a]);
        }
        sGroundPlane = createGroundPlane();

        gAppAlive = 1;

        sStartTick = System.currentTimeMillis();
        frames=0;
    }

    public void reshape(GL gl, int x, int y, int width, int height) {
        this.width = width;
        this.height=height;
        this.x = x;
        this.y = y;

        this.gl = gl;

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glClearColorx((int)(0.1f * 65536),
                      (int)(0.2f * 65536),
                      (int)(0.3f * 65536), 0x10000);

        gl.glCullFace(GL.GL_FRONT);

        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_FASTEST);

        //gl.glShadeModel(GL.GL_SMOOTH);
        gl.glShadeModel(GL.GL_FLAT);
        gl.glDisable(GL.GL_DITHER);

        //gl.glMatrixMode(gl.GL_PROJECTION);
        //gl.glLoadIdentity();
        //gluPerspective(45.0f, (float)width / (float)height, 0.5f, 150.0f);

        System.out.println("reshape ..");
    }

    public void display(GL gl) {
        long tick = System.currentTimeMillis();

        if (gAppAlive==0)
            return;

        this.gl = gl;

        // Actual tick value is "blurred" a little bit.
        sTick = (sTick + tick - sStartTick) >> 1;

        // Terminate application after running through the demonstration once.
        if (sTick >= RUN_LENGTH)
        {
            gAppAlive = 0;
            return;
        }

        gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gluPerspective(45.0f, (float)width / (float)height, 0.5f, 150.0f);

        // Update the camera position and set the lookat.
        camTrack();

        // Configure environment.
        configureLightAndMaterial();

        // Draw the reflection by drawing models with negated Z-axis.
        gl.glPushMatrix();
        drawModels(-1);
        gl.glPopMatrix();

        // Blend the ground plane to the window.
        drawGroundPlane();

        // Draw all the models normally.
        drawModels(1);

        // Draw fade quad over whole window (when changing cameras).
        drawFadeQuad();

        frames++;
        tick = System.currentTimeMillis();
        long dT = tick - sStartTick;
        //        System.out.println(frames+"f, "+dT+"ms "+ (frames*1000)/dT +"fps");
    }

    public void displayChanged(GL gl, boolean modeChanged, boolean deviceChanged) {
    }
    
 private GL  gl;

 // Total run length is 20 * camera track base unit length (see cams.h).
 private int RUN_LENGTH  = (20 * CamTrack.CAMTRACK_LEN) ;
 private int RANDOM_UINT_MAX = 65535 ;

 private long sRandomSeed = 0;

void seedRandom(long seed)
{
    sRandomSeed = seed;
}

int randomUInt()
{
    sRandomSeed = sRandomSeed * 0x343fd + 0x269ec3;
    return Math.abs((int) (sRandomSeed >> 16));
}


// Capped conversion from float to fixed.
int FIXED(float value)
{
    if (value < -32768) value = -32768;
    if (value > 32767) value = 32767;
    return (int)(value * 65536);
}

// Definition of one GL object in this demo.
public class GLObject {
    /* Vertex array and color array are enabled for all objects, so their
     * pointers must always be valid and non-null. Normal array is not
     * used by the ground plane, so when its pointer is null then normal
     * array usage is disabled.
     *
     * Vertex array is supposed to use gl.GL_FIXED datatype and stride 0
     * (i.e. tightly packed array). Color array is supposed to have 4
     * components per color with gl.GL_UNSIGNED_BYTE datatype and stride 0.
     * Normal array is supposed to use gl.GL_FIXED datatype and stride 0.
     */
    IntBuffer vertexArray;
    ByteBuffer colorArray;
    IntBuffer normalArray;
    int vertexComponents;
    int count;
    int vbo[];
    
    public GLObject(int vertices, int vertexComponents,
                    boolean useNormalArray) {
        this.count = vertices;
        this.vertexComponents = vertexComponents;
        this.vertexArray = BufferUtil.newIntBuffer( vertices * vertexComponents );
        this.colorArray =  BufferUtil.newByteBuffer (vertices * 4 );
        if (useNormalArray)
        {
            this.normalArray = BufferUtil.newIntBuffer (vertices * 3 );
        } else {
            this.normalArray = null;
        }
    }

    void seal()
    {
        rewind();
        vbo = new int[3];
        gl.glGenBuffers(3, vbo, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexArray.capacity() * BufferUtil.SIZEOF_INT, vertexArray, GL.GL_STATIC_DRAW);
        gl.glVertexPointer(vertexComponents, gl.GL_FLOAT, 0, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, colorArray.capacity() * BufferUtil.SIZEOF_BYTE, colorArray, GL.GL_STATIC_DRAW);
        gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, 0);

        if (null!=normalArray)
        {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, normalArray.capacity() * BufferUtil.SIZEOF_INT, normalArray, GL.GL_STATIC_DRAW);
            gl.glNormalPointer(gl.GL_FLOAT, 0, 0);
            gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
        } else {
            gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        }
    }

    void draw()
    {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexPointer(vertexComponents, gl.GL_FIXED, 0, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[1]);
        gl.glColorPointer(4, gl.GL_UNSIGNED_BYTE, 0, 0);

        if (null!=normalArray)
        {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[2]);
            gl.glNormalPointer(gl.GL_FIXED, 0, 0);
            gl.glEnableClientState(gl.GL_NORMAL_ARRAY);
        }
        else
            gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        gl.glDrawArrays(gl.GL_TRIANGLES, 0, count);
    }

    void rewind() {
        vertexArray.rewind();
        colorArray.rewind();
        if (normalArray != null) {
            normalArray.rewind();
        }
    }
}

long sStartTick = 0;
long sTick = 0;

int sCurrentCamTrack = 0;
long sCurrentCamTrackStartTick = 0;
long sNextCamTrackStartTick = 0x7fffffff;

GLObject sSuperShapeObjects[] = new GLObject[SuperShape.COUNT];
GLObject sGroundPlane;


public class VECTOR3 {
    float x, y, z;

    public VECTOR3() {
        x=0f; y=0f; z=0f;
    }
    public VECTOR3(float x, float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }
}



static void vector3Sub(VECTOR3 dest, VECTOR3 v1, VECTOR3 v2)
{
    dest.x = v1.x - v2.x;
    dest.y = v1.y - v2.y;
    dest.z = v1.z - v2.z;
}


static void superShapeMap(VECTOR3 point, float r1, float r2, float t, float p)
{
    // sphere-mapping of supershape parameters
    point.x = (float)(Math.cos(t) * Math.cos(p) / r1 / r2);
    point.y = (float)(Math.sin(t) * Math.cos(p) / r1 / r2);
    point.z = (float)(Math.sin(p) / r2);
}


float ssFunc(final float t, final float p[])
{
    return ssFunc(t, p, 0);
}

float ssFunc(final float t, final float p[], int pOff)
{
    return (float)(Math.pow(Math.pow(Math.abs(Math.cos(p[0+pOff] * t / 4)) / p[1+pOff], p[4+pOff]) +
                            Math.pow(Math.abs(Math.sin(p[0+pOff] * t / 4)) / p[2+pOff], p[5+pOff]), 1 / p[3+pOff]));
}


// Creates and returns a supershape object.
// Based on Paul Bourke's POV-Ray implementation.
// http://astronomy.swin.edu.au/~pbourke/povray/supershape/
GLObject createSuperShape(final float params[])
{
    final int resol1 = (int)params[SuperShape.PARAMS - 3];
    final int resol2 = (int)params[SuperShape.PARAMS - 2];
    // latitude 0 to pi/2 for no mirrored bottom
    // (latitudeBegin==0 for -pi/2 to pi/2 originally)
    final int latitudeBegin = resol2 / 4;
    final int latitudeEnd = resol2 / 2;    // non-inclusive
    final int longitudeCount = resol1;
    final int latitudeCount = latitudeEnd - latitudeBegin;
    final int triangleCount = longitudeCount * latitudeCount * 2;
    final int vertices = triangleCount * 3;
    GLObject result;
    float baseColor[] = new float[3];
    int a, longitude, latitude;
    int currentVertex, currentQuad;

    result = new GLObject(vertices, 3, true);
    if (result == null)
        return null;

    for (a = 0; a < 3; ++a)
        baseColor[a] = ((randomUInt() % 155) + 100) / 255.f;

    currentQuad = 0;
    currentVertex = 0;

    // longitude -pi to pi
    for (longitude = 0; longitude < longitudeCount; ++longitude)
    {

        // latitude 0 to pi/2
        for (latitude = latitudeBegin; latitude < latitudeEnd; ++latitude)
        {
            float t1 = (float) ( -Math.PI + longitude * 2 * Math.PI / resol1 );
            float t2 = (float) ( -Math.PI + (longitude + 1) * 2 * Math.PI / resol1 );
            float p1 = (float) ( -Math.PI / 2 + latitude * 2 * Math.PI / resol2 );
            float p2 = (float) ( -Math.PI / 2 + (latitude + 1) * 2 * Math.PI / resol2 );
            float r0, r1, r2, r3;

            r0 = ssFunc(t1, params);
            r1 = ssFunc(p1, params, 6);
            r2 = ssFunc(t2, params);
            r3 = ssFunc(p2, params, 6);

            if (r0 != 0 && r1 != 0 && r2 != 0 && r3 != 0)
            {
                VECTOR3 pa=new VECTOR3(), pb=new VECTOR3(), pc=new VECTOR3(), pd=new VECTOR3();
                VECTOR3 v1=new VECTOR3(), v2=new VECTOR3(), n=new VECTOR3();
                float ca;
                int i;
                //float lenSq, invLenSq;

                superShapeMap(pa, r0, r1, t1, p1);
                superShapeMap(pb, r2, r1, t2, p1);
                superShapeMap(pc, r2, r3, t2, p2);
                superShapeMap(pd, r0, r3, t1, p2);

                // kludge to set lower edge of the object to fixed level
                if (latitude == latitudeBegin + 1)
                    pa.z = pb.z = 0;

                vector3Sub(v1, pb, pa);
                vector3Sub(v2, pd, pa);

                // Calculate normal with cross product.
                /*   i    j    k      i    j
                 * v1.x v1.y v1.z | v1.x v1.y
                 * v2.x v2.y v2.z | v2.x v2.y
                 */

                n.x = v1.y * v2.z - v1.z * v2.y;
                n.y = v1.z * v2.x - v1.x * v2.z;
                n.z = v1.x * v2.y - v1.y * v2.x;

                /* Pre-normalization of the normals is disabled here because
                 * they will be normalized anyway later due to automatic
                 * normalization (gl.GL_NORMALIZE). It is enabled because the
                 * objects are scaled with glScale.
                 */
                /*
                lenSq = n.x * n.x + n.y * n.y + n.z * n.z;
                invLenSq = (float)(1 / sqrt(lenSq));
                n.x *= invLenSq;
                n.y *= invLenSq;
                n.z *= invLenSq;
                */

                ca = pa.z + 0.5f;

                for (i = currentVertex * 3;
                     i < (currentVertex + 6) * 3;
                     i += 3)
                {
                    result.normalArray.put(i    , FIXED(n.x));
                    result.normalArray.put(i + 1, FIXED(n.y));
                    result.normalArray.put(i + 2, FIXED(n.z));
                }
                for (i = currentVertex * 4;
                     i < (currentVertex + 6) * 4;
                     i += 4)
                {
                    int j, color[] = new int[3];
                    for (j = 0; j < 3; ++j)
                    {
                        color[j] = (int)(ca * baseColor[j] * 255);
                        if (color[j] > 255) color[j] = 255;
                    }
                    result.colorArray.put(i    , (byte)color[0]);
                    result.colorArray.put(i + 1, (byte)color[1]);
                    result.colorArray.put(i + 2, (byte)color[2]);
                    result.colorArray.put(i + 3, (byte)0);
                }
                result.vertexArray.put(currentVertex * 3, FIXED(pa.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pa.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pa.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, FIXED(pb.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pb.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pb.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, FIXED(pd.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pd.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pd.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, FIXED(pb.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pb.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pb.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, FIXED(pc.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pc.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pc.z));
                ++currentVertex;
                result.vertexArray.put(currentVertex * 3, FIXED(pd.x));
                result.vertexArray.put(currentVertex * 3 + 1, FIXED(pd.y));
                result.vertexArray.put(currentVertex * 3 + 2, FIXED(pd.z));
                ++currentVertex;
            } // r0 && r1 && r2 && r3
            ++currentQuad;
        } // latitude
    } // longitude

    // Set number of vertices in object to the actual amount created.
    result.count = currentVertex;
    result.seal();
    return result;
}


GLObject createGroundPlane()
{
    final  int scale = 4;
    final  int yBegin = -15, yEnd = 15;    // ends are non-inclusive
    final  int xBegin = -15, xEnd = 15;
    final  int triangleCount = (yEnd - yBegin) * (xEnd - xBegin) * 2;
    final  int vertices = triangleCount * 3;
    GLObject result;
    int x, y;
    int currentVertex, currentQuad;

    result = new GLObject(vertices, 2, false);
    if (result == null)
        return null;

    currentQuad = 0;
    currentVertex = 0;

    for (y = yBegin; y < yEnd; ++y)
    {
        for (x = xBegin; x < xEnd; ++x)
        {
            byte color;
            int i, a;
            color = (byte)((randomUInt() & 0x5f) + 81);  // 101 1111
            for (i = currentVertex * 4; i < (currentVertex + 6) * 4; i += 4)
            {
                result.colorArray.put(i, color);
                result.colorArray.put(i + 1, color);
                result.colorArray.put(i + 2, color);
                result.colorArray.put(i + 3, (byte)0);
            }

            // Axis bits for quad triangles:
            // x: 011100 (0x1c), y: 110001 (0x31)  (clockwise)
            // x: 001110 (0x0e), y: 100011 (0x23)  (counter-clockwise)
            for (a = 0; a < 6; ++a)
            {
                final int xm = x + ((0x1c >> a) & 1);
                final int ym = y + ((0x31 >> a) & 1);
                final float m = (float)(Math.cos(xm * 2) * Math.sin(ym * 4) * 0.75f);
                result.vertexArray.put(currentVertex * 2, FIXED(xm * scale + m));
                result.vertexArray.put(currentVertex * 2 + 1, FIXED(ym * scale + m));
                ++currentVertex;
            }
            ++currentQuad;
        }
    }
    result.seal();
    return result;
}


void drawGroundPlane()
{
    gl.glDisable(gl.GL_CULL_FACE);
    gl.glDisable(gl.GL_DEPTH_TEST);
    gl.glEnable(gl.GL_BLEND);
    gl.glBlendFunc(gl.GL_ZERO, gl.GL_SRC_COLOR);
    gl.glDisable(gl.GL_LIGHTING);

    sGroundPlane.draw();

    gl.glEnable(gl.GL_LIGHTING);
    gl.glDisable(gl.GL_BLEND);
    gl.glEnable(gl.GL_DEPTH_TEST);
}

void drawFadeQuad()
{
    final int beginFade = (int) (sTick - sCurrentCamTrackStartTick);
    final int endFade = (int) (sNextCamTrackStartTick - sTick);
    final int minFade = beginFade < endFade ? beginFade : endFade;

    if (minFade < 1024)
    {
        final int fadeColor = minFade << 7;
        gl.glColor4x(fadeColor, fadeColor, fadeColor, 0);

        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_ZERO, gl.GL_SRC_COLOR);
        gl.glDisable(gl.GL_LIGHTING);

        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        gl.glVertexPointer(2, gl.GL_FIXED, 0, quadVertices);
        gl.glDrawArrays(gl.GL_TRIANGLES, 0, 6);

        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

        gl.glMatrixMode(gl.GL_MODELVIEW);

        gl.glEnable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_BLEND);
        gl.glEnable(gl.GL_DEPTH_TEST);
    }
}

IntBuffer quadVertices;
IntBuffer light0Position;
IntBuffer light0Diffuse;
IntBuffer light1Position;
IntBuffer light1Diffuse;
IntBuffer light2Position;
IntBuffer light2Diffuse;
IntBuffer materialSpecular;

void configureLightAndMaterial()
{
    gl.glLightxv(gl.GL_LIGHT0, gl.GL_POSITION, light0Position);
    gl.glLightxv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light0Diffuse);
    gl.glLightxv(gl.GL_LIGHT1, gl.GL_POSITION, light1Position);
    gl.glLightxv(gl.GL_LIGHT1, gl.GL_DIFFUSE, light1Diffuse);
    gl.glLightxv(gl.GL_LIGHT2, gl.GL_POSITION, light2Position);
    gl.glLightxv(gl.GL_LIGHT2, gl.GL_DIFFUSE, light2Diffuse);
    gl.glMaterialxv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, materialSpecular);

    gl.glMaterialx(gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, 60 << 16);
    gl.glEnable(gl.GL_COLOR_MATERIAL);
}


void drawModels(float zScale)
{
    final int translationScale = 9;
    int x, y;

    seedRandom(9);

    gl.glScalex(1 << 16, 1 << 16, (int)(zScale * 65536));

    for (y = -5; y <= 5; ++y)
    {
        for (x = -5; x <= 5; ++x)
        {
            float buildingScale;
            int fixedScale;

            int curShape = randomUInt() % SuperShape.COUNT;
            buildingScale = SuperShape.sParams[curShape][SuperShape.PARAMS - 1];
            fixedScale = (int)(buildingScale * 65536);

            gl.glPushMatrix();
            gl.glTranslatex((x * translationScale) * 65536,
                         (y * translationScale) * 65536,
                         0);
            gl.glRotatex((int)((randomUInt() % 360) << 16), 0, 0, 1 << 16);
            gl.glScalex(fixedScale, fixedScale, fixedScale);

            sSuperShapeObjects[curShape].draw();
            gl.glPopMatrix();
        }
    }

    for (x = -2; x <= 2; ++x)
    {
        final int shipScale100 = translationScale * 500;
        final int offs100 = x * shipScale100 + (int)(sTick % shipScale100);
        float offs = offs100 * 0.01f;
        int fixedOffs = (int)(offs * 65536);
        gl.glPushMatrix();
        gl.glTranslatex(fixedOffs, -4 * 65536, 2 << 16);
        sSuperShapeObjects[SuperShape.COUNT - 1].draw();
        gl.glPopMatrix();
        gl.glPushMatrix();
        gl.glTranslatex(-4 * 65536, fixedOffs, 4 << 16);
        gl.glRotatex(90 << 16, 0, 0, 1 << 16);
        sSuperShapeObjects[SuperShape.COUNT - 1].draw();
        gl.glPopMatrix();
    }
}


void camTrack()
{
    float lerp[]= new float[5];
    float eX, eY, eZ, cX, cY, cZ;
    float trackPos;
    CamTrack cam;
    long currentCamTick;
    int a;

    if (sNextCamTrackStartTick <= sTick)
    {
        ++sCurrentCamTrack;
        sCurrentCamTrackStartTick = sNextCamTrackStartTick;
    }
    sNextCamTrackStartTick = sCurrentCamTrackStartTick +
                             CamTrack.sCamTracks[sCurrentCamTrack].len * CamTrack.CAMTRACK_LEN;

    cam = CamTrack.sCamTracks[sCurrentCamTrack];
    currentCamTick = sTick - sCurrentCamTrackStartTick;
    trackPos = (float)currentCamTick / (CamTrack.CAMTRACK_LEN * cam.len);

    for (a = 0; a < 5; ++a)
        lerp[a] = (cam.src[a] + cam.dest[a] * trackPos) * 0.01f;

    if (cam.dist>0)
    {
        float dist = cam.dist * 0.1f;
        cX = lerp[0];
        cY = lerp[1];
        cZ = lerp[2];
        eX = cX - (float)Math.cos(lerp[3]) * dist;
        eY = cY - (float)Math.sin(lerp[3]) * dist;
        eZ = cZ - lerp[4];
    }
    else
    {
        eX = lerp[0];
        eY = lerp[1];
        eZ = lerp[2];
        cX = eX + (float)Math.cos(lerp[3]);
        cY = eY + (float)Math.sin(lerp[3]);
        cZ = eZ + lerp[4];
    }
    gluLookAt(eX, eY, eZ, cX, cY, cZ, 0, 0, 1);
}

private int gAppAlive = 0;
private int width, height, x, y, frames;

/* Following gluLookAt implementation is adapted from the
 * Mesa 3D Graphics library. http://www.mesa3d.org
 */
void gluLookAt(float eyex, float eyey, float eyez,
	           float centerx, float centery, float centerz,
	           float upx, float upy, float upz)
{
    float m[] = new float[16];
    float x[] = new float[3], y[] = new float[3], z[] = new float[3];
    float mag;

    /* Make rotation matrix */

    /* Z vector */
    z[0] = eyex - centerx;
    z[1] = eyey - centery;
    z[2] = eyez - centerz;
    mag = (float)Math.sqrt(z[0] * z[0] + z[1] * z[1] + z[2] * z[2]);
    if (mag!=0.0) {			/* mpichler, 19950515 */
        z[0] /= mag;
        z[1] /= mag;
        z[2] /= mag;
    }

    /* Y vector */
    y[0] = upx;
    y[1] = upy;
    y[2] = upz;

    /* X vector = Y cross Z */
    x[0] = y[1] * z[2] - y[2] * z[1];
    x[1] = -y[0] * z[2] + y[2] * z[0];
    x[2] = y[0] * z[1] - y[1] * z[0];

    /* Recompute Y = Z cross X */
    y[0] = z[1] * x[2] - z[2] * x[1];
    y[1] = -z[0] * x[2] + z[2] * x[0];
    y[2] = z[0] * x[1] - z[1] * x[0];

    /* mpichler, 19950515 */
    /* cross product gives area of parallelogram, which is < 1.0 for
     * non-perpendicular unit-length vectors; so normalize x, y here
     */

    mag = (float)Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
    if (mag!=0.0) {
        x[0] /= mag;
        x[1] /= mag;
        x[2] /= mag;
    }

    mag = (float)Math.sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
    if (mag!=0.0) {
        y[0] /= mag;
        y[1] /= mag;
        y[2] /= mag;
    }

    m[0 + 0*4] = x[0];
    m[0 + 1*4] = x[1];
    m[0 + 2*4] = x[2];
    m[0 + 3*4] = 0.0f;
    m[1 + 0*4] = y[0];
    m[1 + 1*4] = y[1];
    m[1 + 2*4] = y[2];
    m[1 + 3*4] = 0.0f;
    m[2 + 0*4] = z[0];
    m[2 + 1*4] = z[1];
    m[2 + 2*4] = z[2];
    m[2 + 3*4] = 0.0f;
    m[3 + 0*4] = 0.0f;
    m[3 + 1*4] = 0.0f;
    m[3 + 2*4] = 0.0f;
    m[3 + 3*4] = 1.0f;
    {
        int a;
        int fixedM[] = new int[16];
        for (a = 0; a < 16; ++a)
            fixedM[a] = (int)(m[a] * 65536);

        IntBuffer nioM = BufferUtil.newIntBuffer(16);
        nioM.put(fixedM);
        nioM.rewind();
        gl.glMultMatrixx(nioM);
    }

    /* Translate Eye to Origin */
    gl.glTranslatex((int)(-eyex * 65536),
                 (int)(-eyey * 65536),
                 (int)(-eyez * 65536));
}

void gluPerspective(float fovy, float aspect,
                    float zNear, float zFar)
{
    float xmin, xmax, ymin, ymax;

    ymax = zNear * (float)Math.tan(fovy * Math.PI / 360.0);
    ymin = -ymax;
    xmin = ymin * aspect;
    xmax = ymax * aspect;

    gl.glFrustumx((int)(xmin * 65536), (int)(xmax * 65536),
                  (int)(ymin * 65536), (int)(ymax * 65536),
                  (int)(zNear * 65536), (int)(zFar * 65536));
}


}

