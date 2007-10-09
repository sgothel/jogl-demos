package demos.nurbs.curveapp;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.*;

import com.sun.opengl.util.GLUT;

/**
 * Listener raacting to OpenGL canvas events
 * Listener reagující na události na OpenGL plátně
 * @author Tomáš Hráský
 *
 */
public class GLListener implements GLEventListener {

  /**
   * OpenGL object
   * objekt realizující základní OpenGL funkce
   */
  private GL gl;

  /**
   * GLU
   * Objekt realizující funkce nadstavbové knihovny GLU
   */
  private GLU glu;

  /**
   * GLUT object
   * Objekt realizující funkce nadstavbové knihovny GLUT
   */
  private GLUT glut;
	
  /**
   * NURBS curve object
   * OpenGL Objekt NURBS křivky
   */
  private GLUnurbs nurbs;

	
  /* (non-Javadoc)
   * @see javax.media.opengl.GLEventListener#init(javax.media.opengl.GLAutoDrawable)
   */
  public void init(GLAutoDrawable drawable) {
    this.gl = drawable.getGL();
    this.glu = new GLU();
    this.glut=new GLUT();

    this.nurbs = glu.gluNewNurbsRenderer();
    gl.glClearColor(1, 1, 1, 1);
  }

  /* (non-Javadoc)
   * @see javax.media.opengl.GLEventListener#display(javax.media.opengl.GLAutoDrawable)
   */
  public void display(GLAutoDrawable drawable) {

    gl.glClear(GL.GL_COLOR_BUFFER_BIT);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();

    float[] knots = Curve.getInstance().getKnots();
    float[] ctrlpoints = Curve.getInstance().getCtrlPoints();
		
    gl.glEnable(GL.GL_LINE_SMOOTH);
    gl.glEnable(GL.GL_BLEND);
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);
	    
    gl.glLineWidth(3);
		
    if(Curve.getInstance().isCurveFinished()){
      glu.gluBeginCurve(nurbs);
      glu.gluNurbsCurve(nurbs, knots.length, knots, 4, ctrlpoints, Curve.getInstance().getOrder(), GL.GL_MAP1_VERTEX_4);
      glu.gluEndCurve(nurbs);
    }
		
    gl.glColor3f(0,0,0);
    gl.glPointSize(5);
    gl.glBegin(GL.GL_POINTS);
    for (int i = 0; i < ctrlpoints.length / 4; i++) {
      //			if(i!=Curve.getInstance().getBodIndex())
      gl.glVertex3d(ctrlpoints[i * 4]/ctrlpoints[i * 4 + 3], ctrlpoints[i * 4 + 1]/ctrlpoints[i * 4 + 3],
                    ctrlpoints[i * 4 + 2]/ctrlpoints[i * 4 + 3]);
    }
    gl.glEnd();
		
		
    for (int i = 0; i < ctrlpoints.length / 4; i++) {
      //			if(i!=Curve.getInstance().getBodIndex())
      //			gl.glPushMatrix();
      gl.glRasterPos2f(ctrlpoints[i * 4]/ctrlpoints[i * 4 + 3], ctrlpoints[i * 4 + 1]/ctrlpoints[i * 4 + 3]-5);
      glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,String.valueOf(i+1));
      //			gl.glPopMatrix();
    }
		
		
    gl.glLineWidth(1);
    gl.glBegin(GL.GL_LINE_STRIP);
    for (int i = 0; i < ctrlpoints.length / 4; i++) {
      //			if(i!=Curve.getInstance().getBodIndex())
      gl.glVertex3d(ctrlpoints[i * 4]/ctrlpoints[i * 4 + 3], ctrlpoints[i * 4 + 1]/ctrlpoints[i * 4 + 3],
                    ctrlpoints[i * 4 + 2]/ctrlpoints[i * 4 + 3]);
    }
    gl.glEnd();
    gl.glColor3f(0,0,1);
    if(Curve.getInstance().getBodIndex()>=0){
      gl.glPointSize(8);
      gl.glBegin(GL.GL_POINTS);
      int i=Curve.getInstance().getBodIndex();
      gl.glVertex3d(ctrlpoints[i * 4]/ctrlpoints[i * 4 + 3], ctrlpoints[i * 4 + 1]/ctrlpoints[i * 4 + 3],
                    ctrlpoints[i * 4 + 2]/ctrlpoints[i * 4 + 3]);
      gl.glEnd();
    }
			
		

  }

  /* (non-Javadoc)
   * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable, int, int, int, int)
   */
  public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                      int height) {
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glOrtho(0, drawable.getWidth(), 0, drawable.getHeight(), -1, 1);
    gl.glScalef(1, -1, 1);
    gl.glTranslatef(0, -drawable.getHeight(), 0);

  }

  /* (non-Javadoc)
   * @see javax.media.opengl.GLEventListener#displayChanged(javax.media.opengl.GLAutoDrawable, boolean, boolean)
   */
  public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
  }
}
