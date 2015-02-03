/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http://continuousphysics.com/Bullet/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package javabullet.demos.opengl;

import java.net.URL;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
// import javabullet.demos.opengl.FontRender.GLFont;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.BufferUtil;

/**
 *
 * @author jezek2
 */
public class GLSRT {

    public static final boolean VBO_CACHE = true; // JAU
    // public static final boolean VBO_CACHE = false;

    /*static {
        ImmModeSink.setVBOUsage(false);
    }*/

	private GLU    glu;
	// private GLFont font;

	public GLSRT(GLU glu, GL gl) {
        System.out.println("VBO_CACHE: "+VBO_CACHE);
        this.glu = glu;
        /*
		try {
			font = new GLFont(gl, DemoApplication.class.getResourceAsStream("DejaVu_Sans_11.fnt"));
            URL fontURL = DemoApplication.class.getResource("DejaVu_Sans_11.fnt");
            if(fontURL!=null) {
                font = new GLFont(gl, fontURL.openStream());
            }
		}
		catch (IOException e) {
			e.printStackTrace();
		} */
	}
	
    ImmModeSink vboCube = null;

	public void drawCube(GL gl, float extent) {
		extent = extent * 0.5f;
		
        if(vboCube==null) {
            vboCube = ImmModeSink.createFixed(GL.GL_STATIC_DRAW, 24,
                                  3, GL.GL_FLOAT,  // vertex
                                  0, GL.GL_FLOAT,  // color
                                  3, GL.GL_FLOAT,  // normal
                                  0, GL.GL_FLOAT); // texture

            vboCube.glBegin(GL.GL_TRIANGLES);
            vboCube.glNormal3f( 0f, 0f, 1f); 
            vboCube.glNormal3f( 0f, 0f, 1f); 
            vboCube.glNormal3f( 0f, 0f, 1f); 
            vboCube.glNormal3f( 0f, 0f, 1f); 
            vboCube.glVertex3f(-extent,+extent,+extent); 
            vboCube.glVertex3f(+extent,-extent,+extent); 
            vboCube.glVertex3f(+extent,+extent,+extent); 
            vboCube.glVertex3f(-extent,-extent,+extent);
            vboCube.glNormal3f( 0f, 0f,-1f); 
            vboCube.glNormal3f( 0f, 0f,-1f); 
            vboCube.glNormal3f( 0f, 0f,-1f); 
            vboCube.glNormal3f( 0f, 0f,-1f); 
            vboCube.glVertex3f(-extent,+extent,-extent); 
            vboCube.glVertex3f(+extent,-extent,-extent); 
            vboCube.glVertex3f(+extent,+extent,-extent); 
            vboCube.glVertex3f(+extent,+extent,+extent);
            vboCube.glNormal3f( 0f, -1f, 0f); 
            vboCube.glNormal3f( 0f, -1f, 0f); 
            vboCube.glNormal3f( 0f, -1f, 0f); 
            vboCube.glNormal3f( 0f, -1f, 0f); 
            vboCube.glVertex3f(-extent,-extent,+extent); 
            vboCube.glVertex3f(+extent,-extent,-extent); 
            vboCube.glVertex3f(+extent,-extent,+extent); 
            vboCube.glVertex3f(-extent,-extent,-extent);
            vboCube.glNormal3f( 0f,1f, 0f); 
            vboCube.glNormal3f( 0f,1f, 0f); 
            vboCube.glNormal3f( 0f,1f, 0f); 
            vboCube.glNormal3f( 0f,1f, 0f); 
            vboCube.glVertex3f(-extent,+extent,+extent); 
            vboCube.glVertex3f(+extent,+extent,-extent); 
            vboCube.glVertex3f(+extent,+extent,+extent); 
            vboCube.glVertex3f(-extent,+extent,-extent);
            vboCube.glNormal3f( 1f,0f, 0f); 
            vboCube.glNormal3f( 1f,0f, 0f); 
            vboCube.glNormal3f( 1f,0f, 0f); 
            vboCube.glNormal3f( 1f,0f, 0f); 
            vboCube.glVertex3f(+extent,-extent,+extent); 
            vboCube.glVertex3f(+extent,+extent,-extent); 
            vboCube.glVertex3f(+extent,+extent,+extent); 
            vboCube.glVertex3f(+extent,-extent,-extent);
            vboCube.glNormal3f( -1f, 0f, 0f); 
            vboCube.glNormal3f( -1f, 0f, 0f); 
            vboCube.glNormal3f( -1f, 0f, 0f); 
            vboCube.glNormal3f( -1f, 0f, 0f); 
            vboCube.glVertex3f(-extent,-extent,+extent); 
            vboCube.glVertex3f(-extent,+extent,-extent); 
            vboCube.glVertex3f(-extent,+extent,+extent); 
            vboCube.glVertex3f(-extent,-extent,-extent);
            vboCube.glEnd(gl, false);
        }
        vboCube.draw(gl, cubeIndices, true);
	}
    private static final byte[] s_cubeIndices =
        {
            0, 3, 1, 2, 0, 1, /* front  */
            6, 5, 4, 5, 7, 4, /* back   */
            8, 11, 9, 10, 8, 9, /* top    */
            15, 12, 13, 12, 14, 13, /* bottom */
            16, 19, 17, 18, 16, 17, /* right  */
            23, 20, 21, 20, 22, 21 /* left   */
    };
    private ByteBuffer cubeIndices=GLBuffers.newDirectByteBuffer(s_cubeIndices);
	
	////////////////////////////////////////////////////////////////////////////
	
	private static GLUquadric cylinder=null;
	private static GLUquadric sphere=null;
	
	private static class SphereKey {
		public float radius;

		public SphereKey() {
		}

		public SphereKey(SphereKey key) {
			radius = key.radius;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof SphereKey)) return false;
			SphereKey other = (SphereKey)obj;
			return radius == other.radius;
		}

		@Override
		public int hashCode() {
			return Float.floatToIntBits(radius);
		}
	}
	
	private static Map<SphereKey,ImmModeSink> sphereDisplayLists = new HashMap<SphereKey,ImmModeSink>();
	private static SphereKey sphereKey = new SphereKey();
	
	public void drawSphere(GL gl, float radius, int slices, int stacks) {
        if(sphere==null) {
            sphere = glu.gluNewQuadric();
            sphere.setImmMode((VBO_CACHE)?false:true);
        }
		sphereKey.radius = radius;
		ImmModeSink vbo = sphereDisplayLists.get(sphereKey);
		if (vbo == null) {
			glu.gluSphere(sphere, radius, 8, 8);
            if(VBO_CACHE) {
                vbo = sphere.replaceImmModeSink();
                sphereDisplayLists.put(new SphereKey(sphereKey), vbo);
            }
		}
		
        if(VBO_CACHE && null!=vbo) {
            vbo.draw(gl, true);
        }
	}
	
	////////////////////////////////////////////////////////////////////////////

	
	private static class CylinderKey {
		public float radius;
		public float halfHeight;

		public CylinderKey() {
		}

		public CylinderKey(CylinderKey key) {
			radius = key.radius;
			halfHeight = key.halfHeight;
		}

		public void set(float radius, float halfHeight) {
			this.radius = radius;
			this.halfHeight = halfHeight;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof CylinderKey)) return false;
			CylinderKey other = (CylinderKey) obj;
			if (radius != other.radius) return false;
			if (halfHeight != other.halfHeight) return false;
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 23 * hash + Float.floatToIntBits(radius);
			hash = 23 * hash + Float.floatToIntBits(halfHeight);
			return hash;
		}
	}
	
	private static Map<CylinderKey,ImmModeSink> cylinderDisplayLists = new HashMap<CylinderKey,ImmModeSink>();
	private static CylinderKey cylinderKey = new CylinderKey();
	
	public void drawCylinder(GL gl, float radius, float halfHeight, int upAxis) {
        if(cylinder==null) {
            cylinder = glu.gluNewQuadric();
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

		// The gluCylinder subroutine draws a cylinder that is oriented along the z axis. 
		// The base of the cylinder is placed at z = 0; the top of the cylinder is placed at z=height. 
		// Like a sphere, the cylinder is subdivided around the z axis into slices and along the z axis into stacks.

		cylinderKey.set(radius, halfHeight);
		ImmModeSink vbo = cylinderDisplayLists.get(cylinderKey);
		if (vbo == null) {
			glu.gluQuadricDrawStyle(cylinder, glu.GLU_FILL);
			glu.gluQuadricNormals(cylinder, glu.GLU_SMOOTH);
			glu.gluCylinder(cylinder, radius, radius, 2f * halfHeight, 15, 10);
            if(VBO_CACHE) {
                vbo = cylinder.replaceImmModeSink();
                cylinderDisplayLists.put(new CylinderKey(cylinderKey), vbo);
            }
		}
		
        if(VBO_CACHE && null!=vbo) {
            vbo.draw(gl, true);
        }

		gl.glPopMatrix();
	}
	
	////////////////////////////////////////////////////////////////////////////

	public void drawString(GL gl, CharSequence s, int x, int y, float red, float green, float blue) {
        /*
		if (font != null) {
			FontRender.drawString(gl, font, s, x, y, red, green, blue);
		} */
	}

}
