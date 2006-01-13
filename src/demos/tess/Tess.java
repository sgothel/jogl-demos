/*
 * Portions Copyright (C) 2003 Sun Microsystems, Inc.
 * All rights reserved.
 */

/*
 * Copyright (c) 1993-1999, Silicon Graphics, Inc.
 * ALL RIGHTS RESERVED
 * Permission to use, copy, modify, and distribute this software for
 * any purpose and without fee is hereby granted, provided that the above
 * copyright notice appear in all copies and that both the copyright notice
 * and this permission notice appear in supporting documentation, and that
 * the name of Silicon Graphics, Inc. not be used in advertising
 * or publicity pertaining to distribution of the software without specific,
 * written prior permission.
 *
 * THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU "AS-IS"
 * AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR OTHERWISE,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL SILICON
 * GRAPHICS, INC.  BE LIABLE TO YOU OR ANYONE ELSE FOR ANY DIRECT,
 * SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY
 * KIND, OR ANY DAMAGES WHATSOEVER, INCLUDING WITHOUT LIMITATION,
 * LOSS OF PROFIT, LOSS OF USE, SAVINGS OR REVENUE, OR THE CLAIMS OF
 * THIRD PARTIES, WHETHER OR NOT SILICON GRAPHICS, INC.  HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS, HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE
 * POSSESSION, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * US Government Users Restricted Rights
 * Use, duplication, or disclosure by the Government is subject to
 * restrictions set forth in FAR 52.227.19(c)(2) or subparagraph
 * (c)(1)(ii) of the Rights in Technical Data and Computer Software
 * clause at DFARS 252.227-7013 and/or in similar or successor
 * clauses in the FAR or the DOD or NASA FAR Supplement.
 * Unpublished-- rights reserved under the copyright laws of the
 * United States.  Contractor/manufacturer is Silicon Graphics,
 * Inc., 2011 N.  Shoreline Blvd., Mountain View, CA 94039-7311.
 *
 * OpenGL(R) is a registered trademark of Silicon Graphics, Inc.
 */

package demos.tess;

/**
 *  tess.java
 *  This program demonstrates polygon tessellation.
 *  Two tesselated objects are drawn.  The first is a
 *  rectangle with a triangular hole.  The second is a
 *  smooth shaded, self-intersecting star.
 *
 *  Note the exterior rectangle is drawn with its vertices
 *  in counter-clockwise order, but its interior clockwise.
 *  Note the combineCallback is needed for the self-intersecting
 *  star.  Also note that removing the TessProperty for the
 *  star will make the interior unshaded (WINDING_ODD).
 * 
 * @author Ported by Nathan Parker Burg, July 2003
 */

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Tess {
    public static void main(String[] args) {
        try {
            Frame frame = new Frame("Tess Demo");
            frame.setSize(500, 500);

            GLCanvas canvas = new GLCanvas();
            frame.add(canvas);
            canvas.addGLEventListener(new TessRenderer());

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                  // Run this on another thread than the AWT event queue to
                  // avoid deadlocks on shutdown on some platforms
                  new Thread(new Runnable() {
                      public void run() {
                        System.exit(0);
                      }
                    }).start();
                }
            });
            frame.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class TessRenderer implements GLEventListener {
        private GL gl;
        private GLU glu = new GLU();
        private int startList;

        public void init(GLAutoDrawable drawable) {
            drawable.setGL(new DebugGL(drawable.getGL()));

            gl = drawable.getGL();

            double[][] rect = new double[][]{{50.0, 50.0, 0.0},
                                             {200.0, 50.0, 0.0},
                                             {200.0, 200.0, 0.0},
                                             {50.0, 200.0, 0.0}};
            double[][] tri = new double[][]{{75.0, 75.0, 0.0},
                                            {125.0, 175.0, 0.0},
                                            {175.0, 75.0, 0.0}};
            double[][] star = new double[][]{{250.0, 50.0, 0.0, 1.0, 0.0, 1.0},
                                             {325.0, 200.0, 0.0, 1.0, 1.0, 0.0},
                                             {400.0, 50.0, 0.0, 0.0, 1.0, 1.0},
                                             {250.0, 150.0, 0.0, 1.0, 0.0, 0.0},
                                             {400.0, 150.0, 0.0, 0.0, 1.0, 0.0}};

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            startList = gl.glGenLists(2);
            GLUtessellator tobj = glu.gluNewTess();

            TessCallback tessCallback = new TessCallback(gl, glu);

            glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);

            gl.glNewList(startList, GL.GL_COMPILE);
            gl.glShadeModel(GL.GL_FLAT);
            glu.gluTessBeginPolygon(tobj, null);
            glu.gluTessBeginContour(tobj);
            glu.gluTessVertex(tobj, rect[0], 0, rect[0]);
            glu.gluTessVertex(tobj, rect[1], 0, rect[1]);
            glu.gluTessVertex(tobj, rect[2], 0, rect[2]);
            glu.gluTessVertex(tobj, rect[3], 0, rect[3]);
            glu.gluTessEndContour(tobj);
            glu.gluTessBeginContour(tobj);
            glu.gluTessVertex(tobj, tri[0], 0, tri[0]);
            glu.gluTessVertex(tobj, tri[1], 0, tri[1]);
            glu.gluTessVertex(tobj, tri[2], 0, tri[2]);
            glu.gluTessEndContour(tobj);
            glu.gluTessEndPolygon(tobj);
            gl.glEndList();

            glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);
            glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);

            gl.glNewList(startList + 1, GL.GL_COMPILE);
            gl.glShadeModel(GL.GL_SMOOTH);
            glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE);
            glu.gluTessBeginPolygon(tobj, null);
            glu.gluTessBeginContour(tobj);
            glu.gluTessVertex(tobj, star[0], 0, star[0]);
            glu.gluTessVertex(tobj, star[1], 0, star[1]);
            glu.gluTessVertex(tobj, star[2], 0, star[2]);
            glu.gluTessVertex(tobj, star[3], 0, star[3]);
            glu.gluTessVertex(tobj, star[4], 0, star[4]);
            glu.gluTessEndContour(tobj);
            glu.gluTessEndPolygon(tobj);
            gl.glEndList();
            glu.gluDeleteTess(tobj);
        }//end init


        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho( 0, 450, 0, 250, -1, 1 );
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        }

        public void display(GLAutoDrawable drawable) {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            gl.glColor3d(1.0, 1.0, 1.0);
            gl.glCallList(startList);
            gl.glCallList(startList + 1);

            gl.glFlush();
        }
    }//end TessRenderer


    public static class TessCallback extends javax.media.opengl.glu.GLUtessellatorCallbackAdapter {
        GL gl;
        GLU glu;

        public TessCallback(GL gl, GLU glu) {
            this.gl = gl;
            this.glu = glu;
        };
        public void begin(int type) {
            gl.glBegin(type);
        }

        public void end() {
            gl.glEnd();
        }

        public void vertex(Object data) {
            if (data instanceof double[]) {
                double[] d = (double[]) data;
                if (d.length == 6) {
                    gl.glColor3dv(d, 3);
                }
                gl.glVertex3dv(d, 0);
            }
        }

        public void error(int errnum) {
            String estring;
            estring = glu.gluErrorString(errnum);
            System.out.println("Tessellation Error: " + estring);
            //System.exit(0);
            throw new RuntimeException();
        }

        public void combine(double[] coords, Object[] data,
                            float[] weight, Object[] outData) {
            double[] vertex = new double[6];

            int i;
            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
            for (i = 3; i < 6; i++)
                vertex[i] = weight[0] * ((double[]) data[0])[i] +
                        weight[1] * ((double[]) data[1])[i] +
                        weight[2] * ((double[]) data[2])[i] +
                        weight[3] * ((double[]) data[3])[i];
            outData[0] = vertex;
        }
    }//End TessCallback
}//End Tess
