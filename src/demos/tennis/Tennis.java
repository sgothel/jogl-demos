package demos.tennis;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;

import javax.media.opengl.GL;
//import javax.media.opengl.glu.GLU;
import javax.swing.JOptionPane;

//import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
//import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.jogamp.newt.Window;
//import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.PrintStream;

/**
 * Tennis.java <BR>
 * author: Fofonov Alexey <P>
 */
 
public class Tennis implements GLEventListener {
  static {
	GLProfile.initSingleton();
  }
  private float view_rotx = 0.0f, view_roty = 0.0f, view_rotz = 0.0f; 	//View angles
  private float sx = 0.0f, sy = 0.0f;									//X, Y coords of Mydesk
  private float spx,spy,spz;											//Speed of the ball
  private float BallCx = 0.0f, BallCy = 0.0f, BallCz = 0.0f;			//Coords of the ball
  private float EnDeskCx = 0.0f, EnDeskCy = 0.0f;						//X, Y coords of Endesk
  private int   cube=0, mydesk=0, endesk=0, ball=0, box=0;				//Flags of the existence 
  private int   swapInterval;
  private static Texture[] texture;
  private float Bax=0, Bay=0;											//Acceleration summands
  private float Vec=3;													//Balls direction
  private boolean CanF=false;											//Ready for play
  
  private int WindowW=0, WindowH=0;	
  
  private float LPositionDX=0, NPositionDX=0;							//Mouse positions 
  private float LPositionDY=0, NPositionDY=0;							//
  private float DspeedX=0, DspeedY=0;									//Speed of Mydesk

  private boolean mouseButtonDown = false, control = true;
  private int prevMouseX, prevMouseY;

  public static void main(String[] args) {
    // set argument 'NotFirstUIActionOnProcess' in the JNLP's application-desc tag for example
    // <application-desc main-class="demos.j2d.TextCube"/>
    //   <argument>NotFirstUIActionOnProcess</argument> 
    // </application-desc>
    // boolean firstUIActionOnProcess = 0==args.length || !args[0].equals("NotFirstUIActionOnProcess") ;

    java.awt.Frame frame = new java.awt.Frame("Tennis Demo");
    frame.setSize(640, 480);
    frame.setLayout(new java.awt.BorderLayout());

    final Animator animator = new Animator();
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          // Run this on another thread than the AWT event queue to
          // make sure the call to Animator.stop() completes before
          // exiting
          new Thread(new Runnable() {
              public void run() {
                animator.stop();
                System.exit(0);
              }
            }).start();
        }
      });

    GLCanvas canvas = new GLCanvas();
    animator.add(canvas);
    // GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
    // GLCanvas canvas = new GLCanvas(caps);

    final Tennis tennis = new Tennis();
    canvas.addGLEventListener(tennis);

    frame.add(canvas, java.awt.BorderLayout.CENTER);
    frame.validate();
    
    //Hide the mouse cursor
    Toolkit t = Toolkit.getDefaultToolkit();
    Image i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none");
    frame.setCursor(noCursor);

    frame.setVisible(true);
    animator.start();
  }
  
  public Tennis(int swapInterval) {
	this.swapInterval = swapInterval;
  }

  public Tennis() {
	this.swapInterval = 1;
  }

  public void init(GLAutoDrawable drawable) {
	System.err.println("Tennis: Init: "+drawable);
	// Use debug pipeline
	// drawable.setGL(new DebugGL(drawable.getGL()));

	GL2 gl = drawable.getGL().getGL2();

    System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
    System.err.println("INIT GL IS: " + gl.getClass().getName());
    System.err.println("GL_VENDOR: " + gl.glGetString(GL2.GL_VENDOR));
    System.err.println("GL_RENDERER: " + gl.glGetString(GL2.GL_RENDERER));
    System.err.println("GL_VERSION: " + gl.glGetString(GL2.GL_VERSION));

    float mat_specular[] =
        { 1.0f, 1.0f, 1.0f, 1.0f };
    float mat_shininess[] =
        { 25.0f };
    float light_position[] =
        { 1.0f, 1.0f, 1.0f, 0.0f };
        
    float red[] = { 0.8f, 0.1f, 0.0f, 0.7f };
    float yellow[] = { 0.8f, 0.75f, 0.0f, 0.7f };
    float blue[] = { 0.2f, 0.2f, 1.0f, 0.7f };
    float brown[] = { 0.8f, 0.4f, 0.1f, 0.7f };
    
    texture = new Texture[5];
    
    //Load textures
    try {
        System.err.println("Loading texture...");
        texture[0] = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/TennisTop.png"),
                false,
                TextureIO.PNG);
        texture[1] = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/TennisBottom.png"),
        		false,
                TextureIO.PNG);
        texture[2] = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/TennisMyDesk.png"),
        		false,
                TextureIO.PNG);
        texture[3] = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/TennisEnDesk.png"),
        		false,
                TextureIO.PNG);
        texture[4] = TextureIO.newTexture(getClass().getClassLoader().getResourceAsStream("demos/data/images/Stars.png"),
        		false,
                TextureIO.PNG);
        System.err.println("Texture0 estimated memory size = " + texture[0].getEstimatedMemorySize());
        System.err.println("Texture1 estimated memory size = " + texture[1].getEstimatedMemorySize());
        System.err.println("Texture2 estimated memory size = " + texture[2].getEstimatedMemorySize());
        System.err.println("Texture3 estimated memory size = " + texture[3].getEstimatedMemorySize());
        System.err.println("Stars estimated memory size = " + texture[4].getEstimatedMemorySize());
      } catch (IOException e) {
        e.printStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(bos));
        JOptionPane.showMessageDialog(null,
                                      bos.toString(),
                                      "Error loading texture",
                                      JOptionPane.ERROR_MESSAGE);
        throw new GLException(e);
        //return;
      }

    gl.glShadeModel(GL2.GL_SMOOTH);              	// Enable Smooth Shading
    gl.glClearDepth(1.0f);                      	// Depth Buffer Setup
    gl.glEnable(GL2.GL_DEPTH_TEST);             	// Enables Depth Testing
    gl.glDepthFunc(GL2.GL_LEQUAL);               	// The Type Of Depth Testing To Do
    
    // Really Nice Perspective Calculations
    gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);  
    gl.glEnable(GL2.GL_TEXTURE_2D);
    
    // Texture filter
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NONE);
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NONE);
    
    // Light and material
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, mat_specular, 0);  
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SHININESS, mat_shininess, 0);
    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light_position, 0);
    gl.glEnable(GL2.GL_LIGHTING);
    gl.glEnable(GL2.GL_LIGHT0);
 
	            
    /* make the objects */
    if(0>=cube) {
        cube = gl.glGenLists(1);
        gl.glNewList(cube, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, brown, 0);
        cube(gl);
        gl.glEndList();
        System.err.println("cube list created: "+cube);
    } else {
        System.err.println("cube list reused: "+cube);
    }
    
    if(0>=box) {
    	box = gl.glGenLists(1);
        gl.glNewList(box, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, brown, 0);
        box(gl);
        gl.glEndList();
        System.err.println("box list created: "+box);
    } else {
        System.err.println("box list reused: "+box);
    }
	            
    if(0>=mydesk) {
    	mydesk = gl.glGenLists(1);
        gl.glNewList(mydesk, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, red, 0);
        desk(gl, 2);
        gl.glEndList();
        System.err.println("mydesk list created: "+mydesk);
    } else {
        System.err.println("mydesk list reused: "+mydesk);
    }
	            
    if(0>=endesk) {
    	endesk = gl.glGenLists(1);
        gl.glNewList(endesk, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, blue, 0);
        desk(gl, 3);
        gl.glEndList();
        System.err.println("endesk list created: "+endesk);
    } else {
        System.err.println("endesk list reused: "+endesk);
    }
    
    if(0>=ball) {
    	ball = gl.glGenLists(1);
        gl.glNewList(ball, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, yellow, 0);
        ball(gl);
        gl.glEndList();
        System.err.println("ball list created: "+ball);
    } else {
        System.err.println("ball list reused: "+ball);
    }
	            
    gl.glEnable(GL2.GL_NORMALIZE);
	                
    MouseListener tennisMouse = new TennisMouseAdapter();    
    KeyListener tennisKeys = new TennisKeyAdapter();

    if (drawable instanceof Window) {
        Window window = (Window) drawable;
        window.addMouseListener(tennisMouse);
        window.addKeyListener(tennisKeys);
    } else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
        java.awt.Component comp = (java.awt.Component) drawable;
        new AWTMouseAdapter(tennisMouse).addTo(comp);
        new AWTKeyAdapter(tennisKeys).addTo(comp);
    }		
  }

  public void dispose(GLAutoDrawable drawable) {
	System.err.println("Tennis: Dispose");
  }

  public void display(GLAutoDrawable drawable) {

    // Get the GL corresponding to the drawable we are animating
    GL2 gl = drawable.getGL().getGL2();
    
    if (mouseButtonDown == false && control == true)
    MovMydesk();
    MoveSphere();
    MoveEnDesk();

    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

    // Special handling for the case where the GLJPanel is translucent
    // and wants to be composited with other Java 2D content
    if (GLProfile.isAWTAvailable() && 
        (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
        !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }
    
 // Place the box and call its display list
    gl.glDisable(GL2.GL_DEPTH_TEST);
    gl.glCallList(box);
    gl.glEnable(GL2.GL_DEPTH_TEST);
	            
    // Rotate the entire assembly of tennis based on how the user
    // dragged the mouse around
    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
            
    // Place the cube and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(0.0f, 0.0f, 0.0f);
    gl.glRotatef(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(cube);
    gl.glPopMatrix();
	            
    // Place the mydesk and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(sx, sy, 3.0f);
    gl.glRotatef(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(mydesk);
    gl.glPopMatrix();
            
    // Place the endesk and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(EnDeskCx, EnDeskCy, -3.0f);
    gl.glRotatef(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(endesk);
    gl.glPopMatrix();
    
    // Place the ball and call its display list
    gl.glPushMatrix();
    gl.glTranslatef(BallCx, BallCy, BallCz);
    gl.glRotatef(0.0f, 0.0f, 0.0f, 1.0f);
    gl.glCallList(ball);
    gl.glPopMatrix();
	            
    // Remember that every push needs a pop; this one is paired with
    // rotating the entire tennis assembly
    gl.glPopMatrix();
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) 
  {
	  
	System.err.println("Gears: Reshape "+x+"/"+y+" "+width+"x"+height);
	GL2 gl = drawable.getGL().getGL2();

	gl.setSwapInterval(swapInterval);

	float h = (float)height / (float)width;
	
	WindowW = width;
	WindowH = height;
	            
	gl.glMatrixMode(GL2.GL_PROJECTION);

	gl.glLoadIdentity();
	
	if (h<1)
		gl.glFrustum(-1.0f, 1.0f, -h, h, 1.0f, 60.0f);
	else
	{
		h = 1.0f/h;
		gl.glFrustum(-h, h, -1.0f, 1.0f, 1.0f, 60.0f);
	}
	
	gl.glMatrixMode(GL2.GL_MODELVIEW);
	gl.glLoadIdentity();
	gl.glTranslatef(0.0f, 0.0f, -6.0f);
	
  }
  
  public static void cube(GL2 gl)
  {

	gl.glShadeModel(GL2.GL_FLAT);

	/* draw left sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(-2.0f, -1.5f, -3.0f);
		gl.glVertex3f(-2.0f,  1.5f, -3.0f);
		gl.glVertex3f(-2.0f,  1.5f,  3.0f);
		gl.glVertex3f(-2.0f, -1.5f,  3.0f);
		
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glVertex3f(-2.05f, -1.55f, -3.0f);
		gl.glVertex3f(-2.05f,  1.55f, -3.0f);
		gl.glVertex3f(-2.05f,  1.55f,  3.0f);
		gl.glVertex3f(-2.05f, -1.55f,  3.0f);

	gl.glEnd();
	
	if (texture[0] != null) {
	      texture[0].enable(gl);
	      texture[0].bind(gl);
	      gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
	      TextureCoords coords = texture[0].getImageTexCoords();
	
	/* draw up sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(coords.left(), coords.top());
		gl.glVertex3f(-2.0f, 1.5f, -3.0f);
		gl.glTexCoord2f(coords.left(), coords.bottom());
		gl.glVertex3f(-2.0f, 1.5f,  3.0f);
		gl.glTexCoord2f(coords.right(), coords.bottom());
		gl.glVertex3f( 2.0f, 1.5f,  3.0f);
		gl.glTexCoord2f(coords.right(), coords.top());
		gl.glVertex3f( 2.0f, 1.5f, -3.0f);
		
	gl.glEnd();
	
		texture[0].disable(gl);
	}	
		
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(-2.05f, 1.55f, -3.0f);
		gl.glVertex3f(-2.05f, 1.55f,  3.0f);
		gl.glVertex3f( 2.05f, 1.55f,  3.0f);
		gl.glVertex3f( 2.05f, 1.55f, -3.0f);

	gl.glEnd();
	
	/* draw right sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glVertex3f(2.0f, -1.5f, -3.0f);
		gl.glVertex3f(2.0f,  1.5f, -3.0f);
		gl.glVertex3f(2.0f,  1.5f,  3.0f);
		gl.glVertex3f(2.0f, -1.5f,  3.0f);
		
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(2.05f, -1.55f, -3.0f);
		gl.glVertex3f(2.05f,  1.55f, -3.0f);
		gl.glVertex3f(2.05f,  1.55f,  3.0f);
		gl.glVertex3f(2.05f, -1.55f,  3.0f);

	gl.glEnd();
	
	if (texture[1] != null) {
	      texture[1].enable(gl);
	      texture[1].bind(gl);
	      gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
	      TextureCoords coords = texture[1].getImageTexCoords();
	
	/* draw down sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(coords.left(), coords.top());
		gl.glVertex3f(-2.0f, -1.5f, -3.0f);
		gl.glTexCoord2f(coords.left(), coords.bottom());
		gl.glVertex3f(-2.0f, -1.5f,  3.0f);
		gl.glTexCoord2f(coords.right(), coords.bottom());
		gl.glVertex3f( 2.0f, -1.5f,  3.0f);
		gl.glTexCoord2f(coords.right(), coords.top());
		gl.glVertex3f( 2.0f, -1.5f, -3.0f);
		
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glVertex3f(-2.05f, -1.55f, -3.0f);
		gl.glVertex3f(-2.05f, -1.55f,  3.0f);
		gl.glVertex3f( 2.05f, -1.55f,  3.0f);
		gl.glVertex3f( 2.05f, -1.55f, -3.0f);

	gl.glEnd();
	
		texture[1].disable(gl);
	}
	
	/* draw back sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		
		gl.glVertex3f(-2.05f, 1.55f, -3.0f);
		gl.glVertex3f( 2.05f, 1.55f, -3.0f);
		gl.glVertex3f( 2.0f, 1.5f, -3.0f);
		gl.glVertex3f(-2.0f, 1.5f, -3.0f);
		
		gl.glVertex3f(-2.05f, -1.55f, -3.0f);
		gl.glVertex3f( 2.05f, -1.55f, -3.0f);
		gl.glVertex3f( 2.0f, -1.5f, -3.0f);
		gl.glVertex3f(-2.0f, -1.5f, -3.0f);
		
		gl.glVertex3f(-2.05f, -1.55f, -3.0f);
		gl.glVertex3f(-2.05f, 1.55f, -3.0f);
		gl.glVertex3f(-2.0f, 1.5f, -3.0f);
		gl.glVertex3f(-2.0f, -1.5f, -3.0f);
		
		gl.glVertex3f(2.05f, -1.55f, -3.0f);
		gl.glVertex3f(2.05f, 1.55f, -3.0f);
		gl.glVertex3f(2.0f, 1.5f, -3.0f);
		gl.glVertex3f(2.0f, -1.5f, -3.0f);

	gl.glEnd();
	
	/* draw front sides */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		
		gl.glVertex3f(-2.05f, 1.55f, 3.0f);
		gl.glVertex3f( 2.05f, 1.55f, 3.0f);
		gl.glVertex3f( 2.0f, 1.5f, 3.0f);
		gl.glVertex3f(-2.0f, 1.5f, 3.0f);
		
		gl.glVertex3f(-2.05f, -1.55f, 3.0f);
		gl.glVertex3f( 2.05f, -1.55f, 3.0f);
		gl.glVertex3f( 2.0f, -1.5f, 3.0f);
		gl.glVertex3f(-2.0f, -1.5f, 3.0f);
		
		gl.glVertex3f(-2.05f, -1.55f, 3.0f);
		gl.glVertex3f(-2.05f, 1.55f, 3.0f);
		gl.glVertex3f(-2.0f, 1.5f, 3.0f);
		gl.glVertex3f(-2.0f, -1.5f, 3.0f);
		
		gl.glVertex3f(2.05f, -1.55f, 3.0f);
		gl.glVertex3f(2.05f, 1.55f, 3.0f);
		gl.glVertex3f(2.0f, 1.5f, 3.0f);
		gl.glVertex3f(2.0f, -1.5f, 3.0f);

	gl.glEnd();
	 
  }
  
  public static void box(GL2 gl)	//Usually "box" mean box, but there only one side is enough
  {
	  
	gl.glShadeModel(GL2.GL_FLAT);
	
	if (texture[4] != null) {
	      texture[4].enable(gl);
	      texture[4].bind(gl);
	      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
	      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
	      gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
	
	/* draw the side */
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(-8.0f, -8.0f, 0.0f);
		gl.glTexCoord2f(0, 8.0f);
		gl.glVertex3f(-8.0f, 8.0f,  0.0f);
		gl.glTexCoord2f(8.0f, 8.0f);
		gl.glVertex3f( 8.0f, 8.0f,  0.0f);
		gl.glTexCoord2f(8.0f, 0);
		gl.glVertex3f( 8.0f, -8.0f, 0.0f);

	gl.glEnd();
	
		texture[4].disable(gl);
	}
	 
  }
  
  public static void desk(GL2 gl, int two_or_three)
  {
	  
	int i;
	float temp1;

	gl.glShadeModel(GL2.GL_FLAT);

	if (texture[two_or_three] != null) {
	      texture[two_or_three].enable(gl);
	      texture[two_or_three].bind(gl);
	      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
	      gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
	      gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
	
	/* draw the front */
	gl.glBegin(GL2.GL_QUAD_STRIP);
	
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		for (i=0; i<25; i++)
		{
			temp1 = (float) Math.pow(Math.sin(i/24.0f*Math.PI), 0.4d);
			 
			gl.glTexCoord2f((i-12)/40.0f, temp1/4 + 0.75f);
			gl.glVertex3f((i-12)/40.0f, temp1/10 + 0.1f , 0.01f + temp1/25);	
			gl.glTexCoord2f((i-12)/40.0f, -temp1/4 + 0.25f);
			gl.glVertex3f((i-12)/40.0f, -temp1/10 - 0.1f, 0.01f + temp1/25);	
		}

	gl.glEnd();
	
	/* draw the back */
	gl.glBegin(GL2.GL_QUAD_STRIP);
	
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		for (i=0; i<25; i++)
		{
			temp1 = (float) Math.pow(Math.sin(i/24.0f*Math.PI), 0.4d);
			
			gl.glTexCoord2f((i-12)/40.0f, temp1/4 + 0.75f);
			gl.glVertex3f((i-12)/40.0f, temp1/10 + 0.1f , -0.01f - temp1/25);	
			gl.glTexCoord2f((i-12)/40.0f, -temp1/4 + 0.25f);
			gl.glVertex3f((i-12)/40.0f, -temp1/10 - 0.1f, -0.01f - temp1/25);	
			
		}

	gl.glEnd();
	
	texture[2].disable(gl);
	}
	
	/* draw the top side */
	gl.glBegin(GL2.GL_QUAD_STRIP);
	
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		for (i=0; i<25; i++)
		{
			temp1 = (float) Math.pow(Math.sin(i/24.0f*Math.PI), 0.4d);
			
			gl.glVertex3f((i-12)/40.0f, temp1/10 + 0.1f , -0.01f - temp1/25);					
			gl.glVertex3f((i-12)/40.0f, temp1/10 + 0.1f , 0.01f + temp1/25);	
		}
		
	gl.glEnd();
		
	/* draw the bottom side */
	gl.glBegin(GL2.GL_QUAD_STRIP);
			
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		for (i=0; i<25; i++)
		{	
			temp1 = (float) Math.pow(Math.sin(i/24.0f*Math.PI), 0.4d);
			
			gl.glVertex3f((i-12)/40.0f, -temp1/10 - 0.1f, 0.01f + temp1/25);			
			gl.glVertex3f((i-12)/40.0f, -temp1/10 - 0.1f, -0.01f - temp1/25);	
		}

	gl.glEnd();
	
	/* draw the left and right sides */
	gl.glBegin(GL2.GL_QUADS);
			
			gl.glNormal3f(-1.0f, 0.0f, 0.0f);
			gl.glVertex3f((-12)/40.0f, -0.1f, 0.01f);
			gl.glVertex3f((-12)/40.0f, +0.1f, 0.01f);			
			gl.glVertex3f((-12)/40.0f, +0.1f, -0.01f);
			gl.glVertex3f((-12)/40.0f, -0.1f, -0.01f);	
			
			gl.glNormal3f(1.0f, 0.0f, 0.0f);
			gl.glVertex3f((+12)/40.0f, -0.1f, 0.01f);
			gl.glVertex3f((+12)/40.0f, +0.1f, 0.01f);			
			gl.glVertex3f((+12)/40.0f, +0.1f, -0.01f);
			gl.glVertex3f((+12)/40.0f, -0.1f, -0.01f);			


	gl.glEnd();
	
  }
  
  public static void ball(GL2 gl)
  {
	  
	int i,j;
	float y1,y2,r1,r2,x,z;

	gl.glShadeModel(GL2.GL_FLAT);

	/* draw the ball */
	gl.glBegin(GL2.GL_QUAD_STRIP);
	
		for (i=0; i<20; i++)
		{
			y1 = (float) Math.cos((i)/20.0f*Math.PI)/10;
			y2 = (float) Math.cos((i+1)/20.0f*Math.PI)/10;
			r1 = (float) Math.sqrt(Math.abs(0.01f-y1*y1));
			r2 = (float) Math.sqrt(Math.abs(0.01f-y2*y2));
			
			for (j=0; j<21; j++)
			{
				x = (float) (r1*Math.cos((float)j/21*2.0f*Math.PI));
				z = (float) (r1*Math.sin((float)j/21*2.0f*Math.PI));
				gl.glNormal3f(10*x, 10*y1, 10*z);
				gl.glVertex3f(x, y1, z);
				
				x = (float) (r2*Math.cos((float)j/21*2.0f*Math.PI));
				z = (float) (r2*Math.sin((float)j/21*2.0f*Math.PI));
				gl.glNormal3f(10*x, 10*y2, 10*z);
				gl.glVertex3f(x, y2, z);
			}
		}

	gl.glEnd();
	
  }
  
  public void MoveSphere()
  {
	  
	// Ball out

		if ((BallCz>3)||(BallCz<-3))
		{
			
			Vec=BallCz;
			
			BallCx = 0.0f;
			BallCy = 0.0f;
			BallCz = 0.0f;

			spz=0;
			spx=0;
			spy=0;

			CanF=false;
			
			Bax=0;
			Bay=0;

		}

	// Ball rebound

		if ((spz<0)&&(BallCz+spz<-2.8)&&(BallCx+spx<EnDeskCx+0.3)&&(BallCx+spx>EnDeskCx-0.3)&&(BallCy+spy<EnDeskCy+0.2)&&(BallCy+spy>EnDeskCy-0.2))
		{
		
			spz=-spz+0.002f;
			spx=spx+(BallCx-EnDeskCx)/10;
			spy=spy+(BallCy-EnDeskCy)/10;	
		
		}

		if ((spz>0)&&(BallCz+spz>2.8)&&(BallCx+spx<sx+0.3)&&(BallCx+spx>sx-0.3)&&(BallCy+spy<sy+0.2)&&(BallCy+spy>sy-0.2))
		{
			
			spz=-spz-0.002f;
			spx=spx+(BallCx-sx)/10;
			spy=spy+(BallCy-sy)/10;
			
			Bax=DspeedX/100;
			Bay=DspeedY/100;

		}

		if ((BallCx+spx<-1.9)||(BallCx+spx>1.9))
		spx=-spx;

		if ((BallCy+spy<-1.4)||(BallCy+spy>1.4))
		spy=-spy;

	// Ball acceleration

		spx=spx+Bax;
		spy=spy+Bay;

	// Ball move

	    if (CanF==true)
	    	
	    BallCx += spx;
		BallCy += spy;
		BallCz += spz;
		
	//Less the acceleration

		Bax=Bax-Bax/100;
		Bay=Bay-Bay/100;
	  
  }
  
  public void MoveEnDesk()
  {

	  //Just follow for the ball
	  
  float sx,sy;
  double gip=Math.sqrt((BallCx-EnDeskCx)*(BallCx-EnDeskCx)+(BallCy-EnDeskCy)*(BallCy-EnDeskCy));

  if (gip<0.07)
  {
  sx=Math.abs((BallCx-EnDeskCx));
  sy=Math.abs((BallCy-EnDeskCy));
  }
  else
  {
  sx=Math.abs((BallCx-EnDeskCx))/((float) gip)*0.07f;
  sy=Math.abs((BallCy-EnDeskCy))/((float) gip)*0.07f;
  }

  if ((BallCx-EnDeskCx>0)&&(EnDeskCx+sx<=1.7)) 
	  EnDeskCx += sx;

  if ((BallCx-EnDeskCx<0)&&(EnDeskCx-sx>=-1.7))
	  EnDeskCx -= sx;

  if ((BallCy-EnDeskCy>0)&&(EnDeskCy+sy<=1.3))
	  EnDeskCy += sy;

  if ((BallCy-EnDeskCy<0)&&(EnDeskCy-sy>=-1.3))
	  EnDeskCy -= sy;

  }
  
  public void MovMydesk() {
 	  
	  LPositionDX = sx;
	  LPositionDY = sy;
	  
	  int x = MouseInfo.getPointerInfo().getLocation().x; 
      int y = MouseInfo.getPointerInfo().getLocation().y;

      sx = sx + (float)(x-500.0f)/300.0f;
      sy = sy + (float)(400.0f-y)/300.0f;  
      
      //Check cube borders
      
      if (sx<-1.7f || sx>1.7f)
    	{ 
    	  if (sx>0) sx = 1.7f;
    	  else     sx = -1.7f;
    	}
      
      if (sy<-1.3 || sy>1.3)
      	{ 
    	  if (sy>0) sy = 1.3f;
    	  else     sy = -1.3f;
    	}
    	
      //Return the mouse back from screen borders
    	try {
          Robot r = new Robot();
          r.mouseMove(500,400);
    		} catch(AWTException ex) {}

    	NPositionDX=sx;
    	NPositionDY=sy;

    	DspeedX=NPositionDX-LPositionDX;
    	DspeedY=NPositionDY-LPositionDY;      
      
    }
  
  class TennisKeyAdapter extends KeyAdapter {      
	    public void keyPressed(KeyEvent e) {
	        int kc = e.getKeyCode();
	        if(KeyEvent.VK_ESCAPE == kc) {
	            System.exit(0);
	        } 
	        if(KeyEvent.VK_CONTROL == kc) {
	            control = false;
	        } 
	        if(KeyEvent.VK_SPACE == kc) {		//Ready for play
	        	if (CanF==false)
	        	{	
	        		if (Vec<0)
	        			spz=-0.07f;			
	        		else
	        			spz=0.07f;
	        	}
	        	CanF=true;
	    	}
	    }
	    
	    public void keyReleased(KeyEvent e) {    //Give the mouse control to the user    
	    	int kc = e.getKeyCode();
	        if(KeyEvent.VK_CONTROL == kc) {
	            control = true;
	        } 	        
	    }
	    
	  }
	  
  class TennisMouseAdapter extends MouseAdapter {
      public void mousePressed(MouseEvent e) {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
        
        mouseButtonDown = true;
      }
	        
      public void mouseReleased(MouseEvent e) {
          mouseButtonDown = false;
      }
	        
      public void mouseDragged(MouseEvent e) {
    	  
        int x = e.getX();
        int y = e.getY();
        
        float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)WindowW);
        float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)WindowH);
	        
        prevMouseX = x;
        prevMouseY = y;       
        
        view_rotx += thetaX;
        view_roty += thetaY;
      }
  }
  
}
