package demos.hdr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import demos.common.*;
import demos.util.*;
import gleem.*;
import gleem.linalg.*;

/** HDR demo by NVidia Corporation - Simon Green, sgreen@nvidia.com <P>

    Ported to Java by Kenneth Russell
*/

public class HDR extends Demo {
  private static String[] defaultArgs = {
    "demos/data/images/stpeters_cross.hdr",
    "512",
    "384",
    "2",
    "7",
    "3",
    "demos/data/models/teapot.obj"
  };
  private GLAutoDrawable drawable;
  private boolean  useCg;
  private boolean  initComplete;
  private HDRTexture hdr;
  private String modelFilename;
  private ObjReader model;
  private Pipeline pipeline;

  private GLUT glut = new GLUT();

  private boolean[] b = new boolean[256];
  
  private ExaminerViewer viewer;
  private boolean doViewAll = true;

  private DurationTimer timer = new DurationTimer();
  private boolean  firstRender = true;
  private int      frameCount;

  private Time  time = new SystemTime();
  private float animRate = (float) Math.toRadians(-12.0f); // Radians / sec

  private String hdrFilename;
  private int win_w;
  private int win_h;
  private float win_scale;
  private int pbuffer_w;
  private int pbuffer_h;
  private int blurWidth;
  private int blur_scale;
  private int blur_w;
  private int blur_h;
  private float blurAmount = 0.5f;

  private int modelno = 4;
  private int numModels = 5;

  private boolean hilo = false;
  private int hdr_tex;
  private int hdr_tex2;
  private int gamma_tex;
  private int vignette_tex;

  private int textureTarget; // Either GL_TEXTURE_RECTANGLE_NV or GL_TEXTURE_RECTANGLE_EXT/ARB

  private GLPbuffer pbuffer;
  private GLPbuffer blur_pbuffer;
  private GLPbuffer blur2_pbuffer;
  private GLPbuffer tonemap_pbuffer;
  // Texture objects for these pbuffers
  private int       pbuffer_tex;
  private int       blur_pbuffer_tex;
  private int       blur2_pbuffer_tex;
  private int       tonemap_pbuffer_tex;

  // Render passes for blur2_pbuffer
  private static final int BLUR2_SHRINK_PASS = 0;
  private static final int BLUR2_VERT_BLUR_PASS = 1;
  private int blur2Pass;

  private int blurh_fprog, blurv_fprog;
  private int skybox_fprog, object_fprog, object_vprog;
  private int tonemap_fprog, shrink_fprog;
  private int blurAmount_param, windowSize_param, exposure_param;
  private int modelViewProj_param, model_param, eyePos_param;


  private float exposure = 32.0f;

  private float[] identityMatrix = { 1.0f, 0.0f, 0.0f, 0.0f,
                                     0.0f, 1.0f, 0.0f, 0.0f,
                                     0.0f, 0.0f, 1.0f, 0.0f,
                                     0.0f, 0.0f, 0.0f, 1.0f };

  public static void main(String[] args) {
    GLCanvas canvas = new GLCanvas();
    HDR demo = new HDR();
    canvas.addGLEventListener(demo);

    final Animator animator = new Animator(canvas);
    demo.setDemoListener(new DemoListener() {
        public void shutdownDemo() {
          runExit(animator);
        }
        public void repaint() {}
      });
    demo.setup(args);

    Frame frame = new Frame("High Dynamic Range Rendering Demo");
    frame.setLayout(new BorderLayout());
    canvas.setSize(demo.getPreferredWidth(), demo.getPreferredHeight());
    
    frame.add(canvas, BorderLayout.CENTER);
    frame.pack();
    frame.show();
    canvas.requestFocus();

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          runExit(animator);
        }
      });

    animator.start();
  }

  public void setup(String[] args) {
    if ((args == null) || (args.length == 0)) {
      args = defaultArgs;
    }

    if (args.length < 6 || args.length > 8) {
      usage();
    }

    try {
      int argNo = 0;
      if (args[argNo].equals("-cg")) {
        useCg = true;
        ++argNo;
      }
      hdrFilename    = args[argNo++];
      pbuffer_w      = Integer.parseInt(args[argNo++]);
      pbuffer_h      = Integer.parseInt(args[argNo++]);
      win_scale      = Float.parseFloat(args[argNo++]);
      blurWidth      = Integer.parseInt(args[argNo++]);
      blur_scale     = Integer.parseInt(args[argNo++]);
      if (argNo < args.length) {
        modelFilename = args[argNo++];
      }

      blur_w = pbuffer_w / blur_scale;
      blur_h = pbuffer_h / blur_scale;
      win_w  = (int) (pbuffer_w * win_scale);
      win_h  = (int) (pbuffer_h * win_scale);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      usage();
    }

    if (modelFilename != null) {
      try {
        InputStream in = getClass().getClassLoader().getResourceAsStream(modelFilename);
        if (in == null) {
          throw new IOException("Unable to open model file " + modelFilename);
        }
        model = new ObjReader(in);
        if (model.getVerticesPerFace() != 3) {
          throw new IOException("Sorry, only triangle-based WaveFront OBJ files supported");
        }
        model.rescale(1.2f / model.getRadius());
        ++numModels;
        modelno = 5;
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    b['f'] = true; // fragment programs
    b['g'] = true; // glare
    b['l'] = true;
    b[' '] = true; // animation
    b['n'] = true; // upsampling smoothing

    try {
      InputStream in = getClass().getClassLoader().getResourceAsStream(hdrFilename);
      if (in == null) {
        throw new IOException("Unable to open HDR file " + hdrFilename);
      }
      hdr = new HDRTexture(in);
      hdr.analyze();
      hdr.convert();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(0);
    }

  }

  public int getPreferredWidth() {
    return win_w;
  }

  public int getPreferredHeight() {
    return win_h;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public void shutdownDemo() {
    ManipManager.getManipManager().unregisterWindow(drawable);
    drawable.removeGLEventListener(this);
    super.shutdownDemo();
  }

  //----------------------------------------------------------------------
  // Listener for main window
  //

  private float zNear = 0.1f;
  private float zFar  = 10.0f;
  private boolean wire = false;
  private boolean toggleWire = false;
  private GLU glu = new GLU();

  public void init(GLAutoDrawable drawable) {
    initComplete = false;
    //      printThreadName("init for Listener");

    GL gl = drawable.getGL();

    checkExtension(gl, "GL_VERSION_1_3"); // For multitexture
    checkExtension(gl, "GL_ARB_pbuffer");
    checkExtension(gl, "GL_ARB_vertex_program");
    checkExtension(gl, "GL_ARB_fragment_program");
    if (!gl.isExtensionAvailable("GL_NV_texture_rectangle") &&
        !gl.isExtensionAvailable("GL_EXT_texture_rectangle") &&
        !gl.isExtensionAvailable("GL_ARB_texture_rectangle")) {
      // NOTE: it turns out the constants associated with these extensions are all identical
      unavailableExtension("Texture rectangle extension not available (need one of GL_NV_texture_rectangle, GL_EXT_texture_rectangle or GL_ARB_texture_rectangle");
    }

    if (!gl.isExtensionAvailable("GL_NV_float_buffer") &&
        !gl.isExtensionAvailable("GL_ATI_texture_float") &&
        !gl.isExtensionAvailable("GL_APPLE_float_pixels")) {
      unavailableExtension("Floating-point textures not available (need one of GL_NV_float_buffer, GL_ATI_texture_float, or GL_APPLE_float_pixels");
    }

    setOrthoProjection(gl, 0, 0, win_w, win_h);

    gamma_tex = createGammaTexture(gl, 1024, 1.0f / 2.2f);
    vignette_tex = createVignetteTexture(gl, pbuffer_w, pbuffer_h, 0.25f*pbuffer_w, 0.7f*pbuffer_w);

    int floatBits = 16;
    int floatAlphaBits = 0;
    // int floatDepthBits = 16;
    // Workaround for apparent bug when not using render-to-texture-rectangle
    int floatDepthBits = 1;

    GLCapabilities caps = new GLCapabilities();
    caps.setDoubleBuffered(false);
    caps.setPbufferFloatingPointBuffers(true);
    caps.setRedBits(floatBits);
    caps.setGreenBits(floatBits);
    caps.setBlueBits(floatBits);
    caps.setAlphaBits(floatAlphaBits);
    caps.setDepthBits(floatDepthBits);
    int[] tmp = new int[1];
    if (!GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
      unavailableExtension("Can not create pbuffer");
    }
    if (pbuffer != null) {
      pbuffer.destroy();
      pbuffer = null;
    }
    if (blur_pbuffer != null) {
      blur_pbuffer.destroy();
      blur_pbuffer = null;
    }
    if (blur2_pbuffer != null) {
      blur2_pbuffer.destroy();
      blur2_pbuffer = null;
    }
    if (tonemap_pbuffer != null) {
      tonemap_pbuffer.destroy();
      tonemap_pbuffer = null;
    }

    GLContext parentContext = drawable.getContext();
    pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, pbuffer_w, pbuffer_h, parentContext);
    pbuffer.addGLEventListener(new PbufferListener());
    gl.glGenTextures(1, tmp, 0);
    pbuffer_tex = tmp[0];
    blur_pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, blur_w, blur_h, parentContext);
    blur_pbuffer.addGLEventListener(new BlurPbufferListener());
    gl.glGenTextures(1, tmp, 0);
    blur_pbuffer_tex = tmp[0];
    blur2_pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, blur_w, blur_h, parentContext);
    blur2_pbuffer.addGLEventListener(new Blur2PbufferListener());
    gl.glGenTextures(1, tmp, 0);
    blur2_pbuffer_tex = tmp[0];
    caps.setPbufferFloatingPointBuffers(false);
    caps.setRedBits(8);
    caps.setGreenBits(8);
    caps.setBlueBits(8);
    caps.setDepthBits(24);
    tonemap_pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, pbuffer_w, pbuffer_h, parentContext);
    tonemap_pbuffer.addGLEventListener(new TonemapPbufferListener());
    gl.glGenTextures(1, tmp, 0);
    tonemap_pbuffer_tex = tmp[0];
      
    drawable.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          dispatchKey(e.getKeyCode(), e.getKeyChar());
        }
      });

    doViewAll = true;

    // Register the window with the ManipManager
    ManipManager manager = ManipManager.getManipManager();
    manager.registerWindow(drawable);
    this.drawable = drawable;

    viewer = new ExaminerViewer(MouseButtonHelper.numMouseButtons());
    viewer.setAutoRedrawMode(false);
    viewer.setNoAltKeyMode(true);
    viewer.attach(drawable, new BSphereProvider() {
        public BSphere getBoundingSphere() {
          return new BSphere(new Vec3f(0, 0, 0), 1.0f);
        }
      });
    viewer.setZNear(zNear);
    viewer.setZFar(zFar);
    initComplete = true;
  }

  public void display(GLAutoDrawable drawable) {
    //      printThreadName("display for Listener");

    if (!initComplete) {
      return;
    }

    if (!firstRender) {
      if (++frameCount == 30) {
        timer.stop();
        System.err.println("Frames per second: " + (30.0f / timer.getDurationAsSeconds()));
        timer.reset();
        timer.start();
        frameCount = 0;
      }
    } else {
      firstRender = false;
      timer.start();
    }

    time.update();

    GL gl = drawable.getGL();

    // OK, ready to go
    if (b[' ']) {
      viewer.rotateAboutFocalPoint(new Rotf(Vec3f.Y_AXIS, (float) (time.deltaT() * animRate)));
    }

    pbuffer.display();

    // FIXME: because of changes in lazy pbuffer instantiation
    // behavior the pbuffer might not have been run just now
    if (pipeline == null) {
      return;
    }

    // blur pass
    if (b['g']) {
      // shrink image
      blur2Pass = BLUR2_SHRINK_PASS;
      blur2_pbuffer.display();
    }

    // horizontal blur
    blur_pbuffer.display();

    // vertical blur
    blur2Pass = BLUR2_VERT_BLUR_PASS;
    blur2_pbuffer.display();

    // tone mapping pass
    tonemap_pbuffer.display();

    // display in window
    gl.glEnable(GL.GL_TEXTURE_RECTANGLE_NV);
    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, tonemap_pbuffer_tex);
    if (b['n']) {
      gl.glTexParameteri( GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    } else {
      gl.glTexParameteri( GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    }
    drawQuadRect4(gl, win_w, win_h, pbuffer_w, pbuffer_h);
    gl.glDisable(GL.GL_TEXTURE_RECTANGLE_NV);

    // Try to avoid swamping the CPU on Linux
    Thread.yield();
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    setOrthoProjection(drawable.getGL(), x, y, width, height);
    win_w = width;
    win_h = height;
  }

  // Unused routines
  public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

  private void checkExtension(GL gl, String glExtensionName) {
    if (!gl.isExtensionAvailable(glExtensionName)) {
      unavailableExtension("Unable to initialize " + glExtensionName + " OpenGL extension");
    }
  }

  private void unavailableExtension(String message) {
    JOptionPane.showMessageDialog(null, message, "Unavailable extension", JOptionPane.ERROR_MESSAGE);
    shutdownDemo();
    throw new GLException(message);
  }

  private void dispatchKey(int keyCode, char k) {
    if (k < 256)
      b[k] = !b[k];

    switch (keyCode) {
    case KeyEvent.VK_ESCAPE:
    case KeyEvent.VK_Q:
      shutdownDemo();
      break;

    case KeyEvent.VK_EQUALS:
      exposure *= 2;
      break;

    case KeyEvent.VK_MINUS:
      exposure *= 0.5f;
      break;

    case KeyEvent.VK_PLUS:
      exposure += 1.0f;
      break;

    case KeyEvent.VK_UNDERSCORE:
      exposure -= 1.0f;
      break;

    case KeyEvent.VK_PERIOD:
      blurAmount += 0.1f;
      break;

    case KeyEvent.VK_COMMA:
      blurAmount -= 0.1f;
      break;

    case KeyEvent.VK_G:
      if (b['g'])
        blurAmount = 0.5f;
      else
        blurAmount = 0.0f;
      break;

    case KeyEvent.VK_O:
      modelno = (modelno + 1) % numModels;
      break;
          
    case KeyEvent.VK_V:
      doViewAll = true;
      break;
    }
  }

  // create gamma lookup table texture
  private int createGammaTexture(GL gl, int size, float gamma) {
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];

    int target = GL.GL_TEXTURE_1D;
    gl.glBindTexture(target, texid);
    gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

    float[] img = new float [size];

    for(int i=0; i<size; i++) {
      float x = i / (float) size;
      img[i] = (float) Math.pow(x, gamma);
    }

    gl.glTexImage1D(target, 0, GL.GL_LUMINANCE, size, 0, GL.GL_LUMINANCE, GL.GL_FLOAT, FloatBuffer.wrap(img));

    return texid;
  }

  // create vignette texture
  // based on Debevec's pflare.c
  int createVignetteTexture(GL gl, int xsiz, int ysiz, float r0, float r1) {
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];
      
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, texid);
    gl.glTexParameteri(GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(GL.GL_TEXTURE_RECTANGLE_NV, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

    float[] img = new float [xsiz*ysiz];

    for (int y = 0; y < ysiz; y++) {
      for (int x = 0; x < xsiz; x++) {
        float radius = (float) Math.sqrt((x-xsiz/2)*(x-xsiz/2) + (y-ysiz/2)*(y-ysiz/2));
        if (radius > r0) {
          if (radius < r1) {
            float t = 1.0f - (radius-r0)/(r1-r0);
            float a = t * 2 - 1;
            float reduce = (float) ((0.25 * Math.PI + 0.5 * Math.asin(a) + 0.5 * a * Math.sqrt( 1 - a*a ))/(0.5 * Math.PI));
            img[y*xsiz + x] = reduce;
          } else {
            img[y*xsiz + x] = 0.0f;
          }
        } else {
          img[y*xsiz + x] = 1.0f;
        }
      }
    }

    gl.glTexImage2D(GL.GL_TEXTURE_RECTANGLE_NV, 0, GL.GL_LUMINANCE, xsiz, ysiz, 0, GL.GL_LUMINANCE, GL.GL_FLOAT, FloatBuffer.wrap(img));

    return texid;
  }

  //----------------------------------------------------------------------
  // Listeners for pbuffers
  //

  class PbufferListener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      //      printThreadName("init for PbufferListener");

      //      drawable.setGL(new DebugGL(drawable.getGL()));

      GL gl = drawable.getGL();
      gl.glEnable(GL.GL_DEPTH_TEST);

      // FIXME: what about the ExaminerViewer?
      setPerspectiveProjection(gl, pbuffer_w, pbuffer_h);

      GLPbuffer pbuffer = (GLPbuffer) drawable;
      int fpmode = pbuffer.getFloatingPointMode();
      int texmode = 0;
      switch (fpmode) {
        case GLPbuffer.NV_FLOAT:
          System.err.println("Creating HILO cubemap");
          hdr_tex  = hdr.createCubemapHILO(gl, true);
          hdr_tex2 = hdr.createCubemapHILO(gl, false);
          texmode = GL.GL_FLOAT_RGBA16_NV;
          hilo = true;
          break;
        case GLPbuffer.APPLE_FLOAT:
          System.err.println("Creating FLOAT16_APPLE cubemap");
          hdr_tex = hdr.createCubemap(gl, GL.GL_RGB_FLOAT16_APPLE);
          texmode = GL.GL_RGBA_FLOAT16_APPLE;
          break;
        case GLPbuffer.ATI_FLOAT:
          System.err.println("Creating FLOAT16_ATI cubemap");
          hdr_tex = hdr.createCubemap(gl, GL.GL_RGB_FLOAT16_ATI);
          texmode = GL.GL_RGBA_FLOAT16_ATI;
          break;
        default:
          throw new RuntimeException("Unexpected floating-point mode " + fpmode);
      }

      if (useCg) {
        initCg(gl);
      } else {
        initARBFP(gl, texmode);
      }
      initBlurCode(gl, blurWidth);

      pipeline.initFloatingPointTexture(gl, pbuffer_tex, pbuffer_w, pbuffer_h);
    }

    public void display(GLAutoDrawable drawable) {
      //      printThreadName("display for PbufferListener");

      GL gl = drawable.getGL();

      renderScene(gl);

      // Copy results back to texture
      pipeline.copyToTexture(gl, pbuffer_tex, pbuffer_w, pbuffer_h);
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    // render scene to float pbuffer
    private void renderScene(GL gl) {
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

      if (doViewAll) {
        viewer.viewAll(gl);
      }

      if (b['w'])
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
      else
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

      if (b['m']) {
        gl.glEnable(GL.GL_MULTISAMPLE);
        gl.glHint(GL.GL_MULTISAMPLE_FILTER_HINT_NV, GL.GL_NICEST);
      } else {
        gl.glDisable(GL.GL_MULTISAMPLE);
      }
  
      if (!b['e']) {
        // draw background
        pipeline.enableFragmentProgram(gl, skybox_fprog);
        gl.glDisable(GL.GL_DEPTH_TEST);
        drawSkyBox(gl);
        gl.glEnable(GL.GL_DEPTH_TEST);
      }

      // draw object
      pipeline.enableVertexProgram(gl, object_vprog);
      pipeline.enableFragmentProgram(gl, object_fprog);

      gl.glMatrixMode(GL.GL_TEXTURE);
      gl.glLoadIdentity();
      viewer.update();
      viewer.updateInverseRotation(gl);

      gl.glMatrixMode( GL.GL_MODELVIEW );
      gl.glLoadIdentity();
      CameraParameters params = viewer.getCameraParameters();
      Mat4f view = params.getModelviewMatrix();
      applyTransform(gl, view);

      pipeline.trackModelViewProjectionMatrix(gl, modelViewProj_param);

      // FIXME: add interation for object separately from camera?
      //      cgGLSetMatrixParameterfc(model_param, object.get_transform().get_value());
      pipeline.setMatrixParameterfc(gl, model_param, identityMatrix);

      // calculate eye position in cubemap space
      Vec3f eyePos_eye = new Vec3f();
      Vec3f eyePos_model = new Vec3f();
      view.invertRigid();
      view.xformPt(eyePos_eye, eyePos_model);
      pipeline.setVertexProgramParameter3f(gl, eyePos_param, eyePos_model.x(), eyePos_model.y(), eyePos_model.z());

      gl.glActiveTexture(GL.GL_TEXTURE0);
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, hdr_tex);
      gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);

      boolean linear = b['l'];
      if (linear) {
        gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
      } else {
        //    glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      }

      if (hilo) {
        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, hdr_tex2);
        gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);

        if (linear) {
          gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
          gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        } else {
          //    glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST_MIPMAP_NEAREST);
          gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
          gl.glTexParameteri( GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        }
      }

      gl.glEnable(GL.GL_CULL_FACE);

      switch(modelno) {
        case 0:
          glut.glutSolidTorus( 0.25, 0.5, 40, 40);
          break;
        case 1:
          glut.glutSolidSphere(0.75f, 40, 40);
          break;
        case 2:
          glut.glutSolidTetrahedron();
          break;
        case 3:
          glut.glutSolidCube(1.0f);
          break;
        case 4:
          // Something about the teapot's geometry causes bad artifacts
          //          glut.glutSolidTeapot(gl, 1.0f);
          break;
        case 5:
          gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
          gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
          gl.glVertexPointer(3, GL.GL_FLOAT, 0, model.getVertices());
          gl.glNormalPointer(GL.GL_FLOAT, 0, model.getVertexNormals());
          int[] indices = model.getFaceIndices();
          gl.glDrawElements(GL.GL_TRIANGLES, indices.length, GL.GL_UNSIGNED_INT, IntBuffer.wrap(indices));
          gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
          gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
          break;
      }

      gl.glDisable(GL.GL_CULL_FACE);
      pipeline.disableVertexProgram(gl);
      pipeline.disableFragmentProgram(gl);
      gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
    }
  }

  class BlurPbufferListener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      //      printThreadName("init for BlurPbufferListener");

      //      drawable.setGL(new DebugGL(drawable.getGL()));

      GL gl = drawable.getGL();

      // FIXME: what about the ExaminerViewer?
      setOrthoProjection(gl, 0, 0, blur_w, blur_h);

      pipeline.initFloatingPointTexture(gl, blur_pbuffer_tex, blur_w, blur_h);
    }

    public void display(GLAutoDrawable drawable) {
      //      printThreadName("display for BlurPbufferListener");

      GL gl = drawable.getGL();

      // horizontal blur
      gl.glBindProgramARB(GL.GL_FRAGMENT_PROGRAM_ARB, blurh_fprog);
      gl.glActiveTexture(GL.GL_TEXTURE0);
      pipeline.bindTexture(gl, blur2_pbuffer_tex);
      glowPass(gl);

      pipeline.copyToTexture(gl, blur_pbuffer_tex, blur_w, blur_h);
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  class Blur2PbufferListener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      //      printThreadName("init for Blur2PbufferListener");

      //      drawable.setGL(new DebugGL(drawable.getGL()));

      GL gl = drawable.getGL();
      // FIXME: what about the ExaminerViewer?
      setOrthoProjection(gl, 0, 0, blur_w, blur_h);

      pipeline.initFloatingPointTexture(gl, blur2_pbuffer_tex, blur_w, blur_h);
    }

    public void display(GLAutoDrawable drawable) {
      //      printThreadName("display for Blur2PbufferListener");

      GL gl = drawable.getGL();

      if (blur2Pass == BLUR2_SHRINK_PASS) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        pipeline.enableFragmentProgram(gl, shrink_fprog);
        setOrthoProjection(gl, 0, 0, blur_w, blur_h);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, pbuffer_tex);
        drawQuadRect2(gl, blur_w, blur_h, pbuffer_w, pbuffer_h);
        pipeline.disableFragmentProgram(gl);

      } else if (blur2Pass == BLUR2_VERT_BLUR_PASS) {

        // vertical blur
        gl.glBindProgramARB(GL.GL_FRAGMENT_PROGRAM_ARB, blurv_fprog);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        pipeline.bindTexture(gl, blur_pbuffer_tex);
        glowPass(gl);
        
      } else {
        throw new RuntimeException("Illegal value of blur2Pass: " + blur2Pass);
      }

      pipeline.copyToTexture(gl, blur2_pbuffer_tex, blur_w, blur_h);
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  class TonemapPbufferListener implements GLEventListener {
    public void init(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();

      setOrthoProjection(gl, 0, 0, pbuffer_w, pbuffer_h);

      pipeline.initTexture(gl, tonemap_pbuffer_tex, pbuffer_w, pbuffer_h);
    }

    public void display(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();

      toneMappingPass(gl);

      pipeline.copyToTexture(gl, tonemap_pbuffer_tex, pbuffer_w, pbuffer_h);
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  //----------------------------------------------------------------------
  // Rendering routines
  //

  private void setOrthoProjection(GL gl, int x, int y, int w, int h) {
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glOrtho(0, w, 0, h, -1.0, 1.0);
    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glLoadIdentity();
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glViewport(x, y, w, h);
  }
    
  private void setPerspectiveProjection(GL gl, int w, int h) {
    // FIXME: what about ExaminerViewer?
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(60.0, (float) w / (float) h, 0.1, 10.0);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glViewport(0, 0, w, h);
  }

  // blur floating point image
  private void glowPass(GL gl) {
    gl.glDisable(GL.GL_DEPTH_TEST);
    gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);

    setOrthoProjection(gl, 0, 0, blur_w, blur_h);
    drawQuadRect(gl, blur_w, blur_h);

    gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
  }

  private void drawQuadRect(GL gl, int w, int h) {
    gl.glBegin(GL.GL_QUADS);
    gl.glTexCoord2f(0, h); gl.glMultiTexCoord2f(GL.GL_TEXTURE1, 0, h / blur_scale); gl.glVertex3f(0, h, 0);
    gl.glTexCoord2f(w, h); gl.glMultiTexCoord2f(GL.GL_TEXTURE1, w / blur_scale, h / blur_scale); gl.glVertex3f(w, h, 0);
    gl.glTexCoord2f(w, 0); gl.glMultiTexCoord2f(GL.GL_TEXTURE1, w / blur_scale, 0); gl.glVertex3f(w, 0, 0);
    gl.glTexCoord2f(0, 0); gl.glMultiTexCoord2f(GL.GL_TEXTURE1, 0, 0); gl.glVertex3f(0, 0, 0);
    gl.glEnd();
  }

  private void drawQuadRect2(GL gl, int w, int h, int tw, int th) {
    gl.glBegin(GL.GL_QUADS);
    gl.glTexCoord2f(0, th); gl.glVertex3f(0, h, 0);
    gl.glTexCoord2f(tw, th); gl.glVertex3f(w, h, 0);
    gl.glTexCoord2f(tw, 0); gl.glVertex3f(w, 0, 0);
    gl.glTexCoord2f(0, 0); gl.glVertex3f(0, 0, 0);
    gl.glEnd();
  }

  private void drawQuadRect4(GL gl, int w, int h, int tw, int th) {
    float offset = 0.5f;
    gl.glBegin(GL.GL_QUADS);
    gl.glTexCoord2f(offset, th - offset); gl.glVertex3f(0, h, 0);
    gl.glTexCoord2f(tw - offset, th - offset); gl.glVertex3f(w, h, 0);
    gl.glTexCoord2f(tw - offset, offset); gl.glVertex3f(w, 0, 0);
    gl.glTexCoord2f(offset, offset); gl.glVertex3f(0, 0, 0);
    gl.glEnd();
  }

  private void disableTexGen(GL gl) {
    gl.glDisable(GL.GL_TEXTURE_GEN_S);
    gl.glDisable(GL.GL_TEXTURE_GEN_T);
    gl.glDisable(GL.GL_TEXTURE_GEN_R);
  }

  private void enableTexGen(GL gl) {
    gl.glEnable(GL.GL_TEXTURE_GEN_S);
    gl.glEnable(GL.GL_TEXTURE_GEN_T);
    gl.glEnable(GL.GL_TEXTURE_GEN_R);
  }

  // draw cubemap background
  private void drawSkyBox(GL gl) {
    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, hdr_tex);
    gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);

    if (hilo) {
      gl.glActiveTexture(GL.GL_TEXTURE1);
      gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, hdr_tex2);
      gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);
    }

    // initialize object linear texgen
    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    float[] s_plane = { 1.0f, 0.0f, 0.0f, 0.0f };
    float[] t_plane = { 0.0f, 1.0f, 0.0f, 0.0f };
    float[] r_plane = { 0.0f, 0.0f, 1.0f, 0.0f };
    gl.glTexGenfv(GL.GL_S, GL.GL_OBJECT_PLANE, s_plane, 0);
    gl.glTexGenfv(GL.GL_T, GL.GL_OBJECT_PLANE, t_plane, 0);
    gl.glTexGenfv(GL.GL_R, GL.GL_OBJECT_PLANE, r_plane, 0);
    gl.glPopMatrix();
    gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    gl.glTexGeni(GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
    enableTexGen(gl);

    gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    viewer.updateInverseRotation(gl);

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glPushMatrix();
    gl.glLoadIdentity();
    gl.glScalef(10.0f, 10.0f, 10.0f);
    glut.glutSolidCube(1.0f);
    gl.glPopMatrix();

    gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);

    gl.glMatrixMode(GL.GL_TEXTURE);
    gl.glPopMatrix();
    gl.glMatrixMode(GL.GL_MODELVIEW);

    disableTexGen(gl);
  }

  // read from float texture, apply tone mapping, render to regular 8/8/8 display
  private void toneMappingPass(GL gl) {
    gl.glFinish();

    gl.glActiveTexture(GL.GL_TEXTURE0);
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, pbuffer_tex);

    gl.glActiveTexture(GL.GL_TEXTURE1);
    if (blur2_pbuffer != null) {
      gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, blur2_pbuffer_tex);
    }

    gl.glActiveTexture(GL.GL_TEXTURE2);
    gl.glBindTexture(GL.GL_TEXTURE_1D, gamma_tex);

    gl.glActiveTexture(GL.GL_TEXTURE3);
    pipeline.bindTexture(gl, vignette_tex);

    pipeline.enableFragmentProgram(gl, tonemap_fprog);

    pipeline.setFragmentProgramParameter1f(gl, blurAmount_param, blurAmount);
    pipeline.setFragmentProgramParameter4f(gl, windowSize_param, 2.0f/win_w, 2.0f/win_h, -1.0f, -1.0f);
    pipeline.setFragmentProgramParameter1f(gl, exposure_param, exposure);

    drawQuadRect(gl, win_w, win_h);

    pipeline.disableFragmentProgram(gl);
  }

  //----------------------------------------------------------------------
  // Cg and blur code initialization
  //

  private String shaderRoot = "demos/hdr/shaders/";
  private void initCg(GL gl) {
    // NOTE: need to instantiate CgPipeline reflectively to avoid
    // compile-time dependence (since Cg support might not be present)
    try {
      Class cgPipelineClass = Class.forName("demos.hdr.CgPipeline");
      pipeline = (Pipeline) cgPipelineClass.newInstance();
    } catch (Exception e) {
      throw new GLException(e);
    }
    pipeline.init();

    try {
      tonemap_fprog    = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/tonemap.cg");
      blurAmount_param = pipeline.getNamedParameter(tonemap_fprog, "blurAmount");
      windowSize_param = pipeline.getNamedParameter(tonemap_fprog, "windowSize");
      exposure_param   = pipeline.getNamedParameter(tonemap_fprog, "exposure");

      if (hilo) {
        skybox_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/skybox_hilo.cg");
        object_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/object_hilo.cg");
      } else {
        skybox_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/skybox.cg");
        object_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/object.cg");
      }

      shrink_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "cg/shrink.cg");

      object_vprog     = pipeline.loadVertexProgram(gl, shaderRoot + "cg/object_vp.cg");
      modelViewProj_param = pipeline.getNamedParameter(object_vprog, "modelViewProj");
      model_param         = pipeline.getNamedParameter(object_vprog, "model");
      eyePos_param        = pipeline.getNamedParameter(object_vprog, "eyePos");
    } catch (IOException e) {
      throw new RuntimeException("Error loading shaders", e);
    }
  }

  private void initARBFP(GL gl, int texmode) {
    pipeline = new ARBFPPipeline(texmode);
    pipeline.init();

    try {
      // NOTE that the program parameters are hard-coded; in the
      // future we can use GLSL but for this demo we desire good
      // backward compatibility
      tonemap_fprog    = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/tonemap.arbfp1");
      blurAmount_param = 1;
      windowSize_param = -1; // Not used
      exposure_param   = 2;

      if (hilo) {
        skybox_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/skybox_hilo.arbfp1");
        object_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/object_hilo.arbfp1");
      } else {
        skybox_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/skybox.arbfp1");
        object_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/object.arbfp1");
      }

      shrink_fprog     = pipeline.loadFragmentProgram(gl, shaderRoot + "arbfp1/shrink.arbfp1");

      object_vprog     = pipeline.loadVertexProgram(gl, shaderRoot + "arbfp1/object_vp.arbvp1");
      modelViewProj_param = 0;
      model_param         = 4;
      eyePos_param        = 8;
    } catch (IOException e) {
      throw new RuntimeException("Error loading shaders", e);
    }
  }

  private void initBlurCode(GL gl, int blurWidth) {
    // generate blur code
    String blurCode = generateBlurCodeFP2(blurWidth, false);
    blurh_fprog = loadProgram(gl, GL.GL_FRAGMENT_PROGRAM_ARB, blurCode);
    //  printf("%s\n", blurCode);

    blurCode = generateBlurCodeFP2(blurWidth, true);
    blurv_fprog = loadProgram(gl, GL.GL_FRAGMENT_PROGRAM_ARB, blurCode);
    //  printf("%s\n", blurCode);
  }

  private int loadProgram(GL gl, int target, String code) {
    int prog_id;
    int[] tmp = new int[1];
    gl.glGenProgramsARB(1, tmp, 0);
    prog_id = tmp[0];
    gl.glBindProgramARB(target, prog_id);
    int size = code.length();
    gl.glProgramStringARB(target, GL.GL_PROGRAM_FORMAT_ASCII_ARB, code.length(), code);
    int[] errPos = new int[1];
    gl.glGetIntegerv(GL.GL_PROGRAM_ERROR_POSITION_ARB, errPos, 0);
    if (errPos[0] >= 0) {
      String kind = "Program";
      if (target == GL.GL_VERTEX_PROGRAM_ARB) {
        kind = "Vertex program";
      } else if (target == GL.GL_FRAGMENT_PROGRAM_ARB) {
        kind = "Fragment program";
      }
      System.out.println(kind + " failed to load:");
      String errMsg = gl.glGetString(GL.GL_PROGRAM_ERROR_STRING_ARB);
      if (errMsg == null) {
        System.out.println("[No error message available]");
      } else {
        System.out.println("Error message: \"" + errMsg + "\"");
      }
      System.out.println("Error occurred at position " + errPos[0] + " in program:");
      int endPos = errPos[0];
      while (endPos < code.length() && code.charAt(endPos) != '\n') {
        ++endPos;
      }
      System.out.println(code.substring(errPos[0], endPos));
      throw new GLException("Error loading " + kind);
    } else {
      if (target == GL.GL_FRAGMENT_PROGRAM_ARB) {
        int[] isNative = new int[1];
        gl.glGetProgramivARB(GL.GL_FRAGMENT_PROGRAM_ARB,
                             GL.GL_PROGRAM_UNDER_NATIVE_LIMITS_ARB,
                             isNative, 0);
        if (isNative[0] != 1) {
          System.out.println("WARNING: fragment program is over native resource limits");
          Thread.dumpStack();
        }
      }
    }
    return prog_id;
  }

  // 1d Gaussian distribution
  private float gaussian(float x, float s) {
    return (float) (Math.exp(-x*x/(2*s*s)) / (s*Math.sqrt(2*Math.PI)));
  }

  private void dumpWeights(int n) {
    float s = n / 3.0f;
    float sum = 0.0f;
    System.err.println("gaussian weights, s = " + s + ", n = " + n);
    for(int x=-n; x<=n; x++) {
      float w = gaussian(x, s);
      sum += w;
      System.err.println("" + x + ": " + w);
    }
    System.err.println("sum = " + sum);
  }

  // optimized version
  // pairs texture lookups, uses half precision
  private String generateBlurCodeFP2(int n, boolean vertical) {
    StringBuffer buf = new StringBuffer();

    float sum = 0;
    for(int i=-n; i<=n; i++) {
      float weight = gaussian(3.0f*i / (float) n, 1.0f);
      sum += weight;
    }
    System.err.println("sum = " + sum);

    buf.append("!!ARBfp1.0\n");
    buf.append("TEMP H0, H1, H2;\n");
    for(int i=-n; i<=n; i+=2) {
      float weight = gaussian(3.0f*i / (float) n, 1.0f) / sum;
      float weight2 = gaussian(3.0f*(i+1) / (float) n, 1.0f) / sum;

      int x_offset, y_offset, x_offset2, y_offset2;
      if (vertical) {
        x_offset = 0; x_offset2 = 0;
        y_offset = i; y_offset2 = i+1;
      } else {
        x_offset = i; x_offset2 = i+1;
        y_offset = 0; y_offset2 = 0;
      }

      // calculate texcoords
      buf.append("ADD H0, fragment.texcoord[0], {" + x_offset + ", " + y_offset + "};\n");
      if (i+1 <= n) {
        buf.append("ADD H1, fragment.texcoord[0], {" + x_offset2 + ", " + y_offset2 + "};\n");
      }
      // do texture lookups
      buf.append("TEX  H0, H0, texture[0], RECT;\n");
      if (i+1 <= n) {
        buf.append("TEX  H1, H1, texture[0], RECT;\n");
      }

      // accumulate results
      if (i==-n) {
        // first sample
        buf.append("MUL H2, H0, {" + weight + "}.x;\n");
        buf.append("MAD H2, H1, {" + weight2 + "}.x, H2;\n");
      } else {
        buf.append("MAD H2, H0, {" + weight + "}.x, H2;\n");
        if (i+1 <= n) {
          buf.append("MAD H2, H1, {" + weight2 + "}.x, H2;\n");
        }
      }
    }

    buf.append(
      "MOV result.color, H2;\n" +
      "END\n"
    );

    return buf.toString();
  }

  private void applyTransform(GL gl, Mat4f mat) {
    float[] data = new float[16];
    mat.getColumnMajorData(data);
    gl.glMultMatrixf(data, 0);
  }

  private void usage() {
    System.err.println("usage: java demos.hdr.HDR [-cg] image.hdr pbuffer_w pbuffer_h window_scale blur_width blur_decimate [obj file]");
    shutdownDemo();
  }

  private void printThreadName(String where) {
    System.err.println("In " + where + ": current thread = " + Thread.currentThread().getName());
  }

  private static void runExit(final Animator animator) {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Run the
    // exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          animator.stop();
          System.exit(0);
        }
      }).start();
  }
}
