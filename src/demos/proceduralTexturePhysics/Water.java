/*
 * Portions Copyright (C) 2003 Sun Microsystems, Inc.
 * All rights reserved.
 */

/*
 *
 * COPYRIGHT NVIDIA CORPORATION 2003. ALL RIGHTS RESERVED.
 * BY ACCESSING OR USING THIS SOFTWARE, YOU AGREE TO:
 *
 *  1) ACKNOWLEDGE NVIDIA'S EXCLUSIVE OWNERSHIP OF ALL RIGHTS
 *     IN AND TO THE SOFTWARE;
 *
 *  2) NOT MAKE OR DISTRIBUTE COPIES OF THE SOFTWARE WITHOUT
 *     INCLUDING THIS NOTICE AND AGREEMENT;
 *
 *  3) ACKNOWLEDGE THAT TO THE MAXIMUM EXTENT PERMITTED BY
 *     APPLICABLE LAW, THIS SOFTWARE IS PROVIDED *AS IS* AND
 *     THAT NVIDIA AND ITS SUPPLIERS DISCLAIM ALL WARRANTIES,
 *     EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED
 *     TO, IMPLIED WARRANTIES OF MERCHANTABILITY  AND FITNESS
 *     FOR A PARTICULAR PURPOSE.
 *
 * IN NO EVENT SHALL NVIDIA OR ITS SUPPLIERS BE LIABLE FOR ANY
 * SPECIAL, INCIDENTAL, INDIRECT, OR CONSEQUENTIAL DAMAGES
 * WHATSOEVER (INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS
 * OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS
 * INFORMATION, OR ANY OTHER PECUNIARY LOSS), INCLUDING ATTORNEYS'
 * FEES, RELATING TO THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package demos.proceduralTexturePhysics;

import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;

import gleem.linalg.*;
import net.java.games.jogl.*;
import demos.util.*;

/**
 * Auxiliary Water simulation class used by ProceduralTexturePhysics
 * main loop. Demonstration by NVidia Corporation.
 *
 * <P>
 *
 * Ported to Java by Kenneth Russell
 */

public class Water {
  // Note: this class is organized differently than most of the demos
  // due to the fact that it is used for two purposes: when the
  // pbuffer's context is current it is used to update the cellular
  // automata, and when the parent drawable's context is current it is
  // used to render the water geometry (with the parent drawable's GL
  // object).

  // Rendering modes
  public static final int CA_FULLSCREEN_REFLECT   = 0;
  public static final int CA_FULLSCREEN_FORCE     = 1;
  public static final int CA_FULLSCREEN_HEIGHT    = 2;
  public static final int CA_FULLSCREEN_NORMALMAP = 3;
  public static final int CA_TILED_THREE_WINDOWS  = 4;
  public static final int CA_DO_NOT_RENDER        = 5;

  private int[] initialMapDimensions = new int[2];
  private TGAImage initialMap;

  private String tmpSpinFilename;
  private String tmpDropletFilename;
  private String tmpCubeMapFilenamePattern;

  private GLDrawable pbuffer;
  private Rotf cameraOrientation = new Rotf();

  // Static texture names
  private static final int CA_TEXTURE_INITIAL_MAP = 0;
  private static final int CA_TEXTURE_SPIN        = 1;
  private static final int CA_TEXTURE_DROPLET     = 2;
  private static final int CA_TEXTURE_CUBEMAP     = 3;
  private static final int CA_NUM_STATIC_TEXTURES = 4;

  // Dynamic texture names
  private static final int CA_TEXTURE_FORCE_INTERMEDIATE = 0;
  private static final int CA_TEXTURE_FORCE_TARGET       = 1;
  private static final int CA_TEXTURE_VELOCITY_SOURCE    = 2;
  private static final int CA_TEXTURE_VELOCITY_TARGET    = 3;
  private static final int CA_TEXTURE_HEIGHT_SOURCE      = 4;
  private static final int CA_TEXTURE_HEIGHT_TARGET      = 5;
  private static final int CA_TEXTURE_NORMAL_MAP         = 6;
  private static final int CA_NUM_DYNAMIC_TEXTURES       = 7;
    
  // List names
  private static final int CA_REGCOMBINER_EQ_WEIGHT_COMBINE     = 0;
  private static final int CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_1 = 1;
  private static final int CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_2 = 2;
  private static final int CA_REGCOMBINER_APPLY_FORCE           = 3;
  private static final int CA_REGCOMBINER_APPLY_VELOCITY        = 4;
  private static final int CA_REGCOMBINER_CREATE_NORMAL_MAP     = 5;
  private static final int CA_TEXTURE_SHADER_REFLECT            = 6;
  private static final int CA_DRAW_SCREEN_QUAD                  = 7;
  private static final int CA_NUM_LISTS                         = 8;

  private int[]  staticTextureIDs = new int[CA_NUM_STATIC_TEXTURES];     
  private int[]  dynamicTextureIDs = new int[CA_NUM_DYNAMIC_TEXTURES];
    
  private int       texHeightInput;                 // current input height texture ID.
  private int       texHeightOutput;                // current output height texture ID.
  private int       texVelocityInput;               // current input velocity texture ID.
  private int       texVelocityOutput;              // current output velocity texture ID.
  private int       texForceStepOne;                // intermediate force computation result texture ID.
  private int       texForceOutput;                 // current output force texture ID.

  private int[]     displayListIDs = new int[CA_NUM_LISTS];
    
  private int       vertexProgramID;                // one vertex shader is used to choose the texcoord offset

  private int       flipState;                      // used to flip target texture configurations.

  private boolean   wrap;                           // CA can either wrap its borders, or clamp (clamp by default)  
  private boolean   reset = true;                   // are we resetting this frame? (user hit reset).
  private boolean   singleStep;                     // animation step on keypress.
  private boolean   animate = true;                 // continuous animation.
  private boolean   slow = true;                    // run slow.
  private boolean   wireframe;                      // render in wireframe mode
  private boolean   applyInteriorBoundaries = true; // enable / disable "boundary" image drawing.
  private boolean   spinLogo = true;                // draw spinning logo.
  private boolean   createNormalMap = true;         // enable / disable normal map creation.

  private float     perTexelWidth;                  // width of a texel (percentage of texture)
  private float     perTexelHeight;                 // height of a texel

  private float     blurDist = 0.5f;                // distance over which to blur.
  private boolean   mustUpdateBlurOffsets;          // flag indicating blurDist was set last tick

  private float     normalSTScale = 0.8f;           // scale of normals in normal map.
  private float     bumpScale = 0.25f;              // scale of bumps in water.

  private float     dropletFrequency = 0.175f;      // frequency at which droplets are drawn in water...

  private int       slowDelay = 15;                 // amount (milliseconds) to delay when running slow.
  private int       skipInterval;                   // frames to skip simulation.
  private int       skipCount;                      // frame count for skipping rendering

  private int       angle;                          // angle in degrees for spinning logo

  private List/*<Droplet>*/ droplets = new ArrayList/*<Droplet>*/();             // array of droplets

  private int       renderMode; 

  // Constant memory locations
  private static final int CV_WORLDVIEWPROJ_0  =  0;
  private static final int CV_WORLDVIEWPROJ_1  =  1;
  private static final int CV_WORLDVIEWPROJ_2  =  2;
  private static final int CV_WORLDVIEWPROJ_3  =  3;

  private static final int CV_UV_OFFSET_TO_USE =  4;

  private static final int CV_UV_T0_NO_OFFSET  =  8;
  private static final int CV_UV_T0_TYPE1      =  9;
  private static final int CV_UV_T0_TYPE2      = 10;
  private static final int CV_UV_T0_TYPE3      = 11;
  private static final int CV_UV_T0_TYPE4      = 12;

  private static final int CV_UV_T1_NO_OFFSET  = 13;
  private static final int CV_UV_T1_TYPE1      = 14;
  private static final int CV_UV_T1_TYPE2      = 15;
  private static final int CV_UV_T1_TYPE3      = 16;
  private static final int CV_UV_T1_TYPE4      = 17;

  private static final int CV_UV_T2_NO_OFFSET  = 18;
  private static final int CV_UV_T2_TYPE1      = 19;
  private static final int CV_UV_T2_TYPE2      = 20;
  private static final int CV_UV_T2_TYPE3      = 21;
  private static final int CV_UV_T2_TYPE4      = 22;

  private static final int CV_UV_T3_NO_OFFSET  = 23;
  private static final int CV_UV_T3_TYPE1      = 24;
  private static final int CV_UV_T3_TYPE2      = 25;
  private static final int CV_UV_T3_TYPE3      = 26;
  private static final int CV_UV_T3_TYPE4      = 27;

  private static final int CV_CONSTS_1         = 28;

  public void initialize(String initialMapFilename,
                         String spinFilename,
                         String dropletFilename,
                         String cubeMapFilenamePattern,
                         GLDrawable parentWindow) {
    loadInitialTexture(initialMapFilename);
    tmpSpinFilename           = spinFilename;
    tmpDropletFilename        = dropletFilename;
    tmpCubeMapFilenamePattern = cubeMapFilenamePattern;
    
    // create the pbuffer.  Will use this as an offscreen rendering buffer.
    // it allows rendering a texture larger than our window.
    if (!parentWindow.canCreateOffscreenDrawable()) {
      throw new GLException("Parent window doesn't support creation of pbuffers");
    }
    GLCapabilities caps = new GLCapabilities();
    caps.setDoubleBuffered(false);
    pbuffer = parentWindow.createOffscreenDrawable(caps, 
                                                   initialMapDimensions[0],
                                                   initialMapDimensions[1]);
    pbuffer.addGLEventListener(new Listener());
  }

  public void tick() { 
    pbuffer.display();
  }

  public void draw(GL gl, Rotf cameraOrientation) {
    this.cameraOrientation.set(cameraOrientation);

    if (skipCount >= skipInterval && renderMode != CA_DO_NOT_RENDER) {
      skipCount = 0;
      // Display the results of the rendering to texture
      if (wireframe) {
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
           
        // chances are the texture will be all dark, so lets not use a texture
        gl.glDisable(GL.GL_TEXTURE_2D);
      } else {
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
            			
        gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
        gl.glEnable(GL.GL_TEXTURE_2D);
      }

      switch (renderMode) {
        case CA_FULLSCREEN_REFLECT: {
          // include bump scale...
          Mat4f bscale = new Mat4f();
          bscale.makeIdent();
          bscale.set(0, 0, bumpScale);
          bscale.set(1, 1, bumpScale);
          Mat4f rot = new Mat4f();
          rot.makeIdent();
          rot.setRotation(cameraOrientation);
          Mat4f matRot = rot.mul(bscale);

          gl.glCallList(displayListIDs[CA_TEXTURE_SHADER_REFLECT]);

          // Draw quad over full display
          gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_NORMAL_MAP]);
          gl.glDisable(GL.GL_TEXTURE_2D);
          gl.glActiveTextureARB(GL.GL_TEXTURE3_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, staticTextureIDs[CA_TEXTURE_CUBEMAP]);
          gl.glEnable(GL.GL_TEXTURE_2D);

          gl.glColor4f(1, 1, 1, 1);
          gl.glBegin(GL.GL_QUADS);
                
          gl.glMultiTexCoord2fARB(GL.GL_TEXTURE0_ARB, 0,0);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE1_ARB, matRot.get(0,0), matRot.get(0,1), matRot.get(0,2),  1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE2_ARB, matRot.get(1,0), matRot.get(1,1), matRot.get(1,2),  1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE3_ARB, matRot.get(2,0), matRot.get(2,1), matRot.get(2,2),  1);
          gl.glVertex2f(-1,-1);
                
          gl.glMultiTexCoord2fARB(GL.GL_TEXTURE0_ARB, 1,0);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE1_ARB, matRot.get(0,0), matRot.get(0,1), matRot.get(0,2), -1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE2_ARB, matRot.get(1,0), matRot.get(1,1), matRot.get(1,2),  1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE3_ARB, matRot.get(2,0), matRot.get(2,1), matRot.get(2,2),  1);
          gl.glVertex2f( 1,-1);
                
          gl.glMultiTexCoord2fARB(GL.GL_TEXTURE0_ARB, 1,1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE1_ARB, matRot.get(0,0), matRot.get(0,1), matRot.get(0,2), -1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE2_ARB, matRot.get(1,0), matRot.get(1,1), matRot.get(1,2), -1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE3_ARB, matRot.get(2,0), matRot.get(2,1), matRot.get(2,2),  1);
          gl.glVertex2f( 1, 1);
                
          gl.glMultiTexCoord2fARB(GL.GL_TEXTURE0_ARB, 0,1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE1_ARB, matRot.get(0,0), matRot.get(0,1), matRot.get(0,2),  1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE2_ARB, matRot.get(1,0), matRot.get(1,1), matRot.get(1,2), -1);
          gl.glMultiTexCoord4fARB(GL.GL_TEXTURE3_ARB, matRot.get(2,0), matRot.get(2,1), matRot.get(2,2),  1);
          gl.glVertex2f(-1, 1);
                
          gl.glEnd();
    
          gl.glDisable(GL.GL_TEXTURE_SHADER_NV);
          gl.glDisable(GL.GL_REGISTER_COMBINERS_NV);
                
          break;
        }

        case CA_FULLSCREEN_NORMALMAP: {
          // Draw quad over full display
          gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_NORMAL_MAP]);
                
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          break;
        }

        case CA_FULLSCREEN_HEIGHT: {
          // Draw quad over full display
          gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightOutput);
                
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          break;
        }

        case CA_FULLSCREEN_FORCE: {
          // Draw quad over full display
          gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_FORCE_TARGET]);
			                 
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          break;
        }

        case CA_TILED_THREE_WINDOWS: {
          // Draw quad over full display
          // lower left
          gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
          gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_FORCE_TARGET]);
          gl.glMatrixMode(GL.GL_MODELVIEW);
          gl.glPushMatrix();
			                 
          gl.glTranslatef(-0.5f, -0.5f, 0);
          gl.glScalef(0.5f, 0.5f, 1);
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          gl.glPopMatrix();

          // lower right
          gl.glBindTexture(GL.GL_TEXTURE_2D, texVelocityOutput);
          gl.glPushMatrix();
			                 
          gl.glTranslatef(0.5f, -0.5f, 0);
          gl.glScalef(0.5f, 0.5f, 1);
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          gl.glPopMatrix();

          // upper left
          gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_NORMAL_MAP]);
          gl.glMatrixMode(GL.GL_MODELVIEW);
          gl.glPushMatrix();
			                 
          gl.glTranslatef(-0.5f, 0.5f, 0);
          gl.glScalef(0.5f, 0.5f, 1);
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          gl.glPopMatrix();

          // upper right
          gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightOutput);
          gl.glMatrixMode(GL.GL_MODELVIEW);
          gl.glPushMatrix();
			                 
          gl.glTranslatef(0.5f, 0.5f, 0);
          gl.glScalef(0.5f, 0.5f, 1);
          gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);
          gl.glPopMatrix();
			    
          break;
        }
      }
    } else {
      // skip rendering this frame
      skipCount++;
    }
  }

  public void singleStep()                               { singleStep  = true;                 }
  public void enableAnimation(boolean enable)            { animate     = enable;               }
  public void enableSlowAnimation(boolean enable)        { slow        = enable;               }
  public void reset()                                    { reset       = true;                 }
  public void setRenderMode(int mode)                    { renderMode  = mode;                 }
    
  public void enableWireframe(boolean enable)            { wireframe   = enable;               }
  public void enableBorderWrapping(boolean enable)       { wrap        = enable;               }
    
  public void enableBoundaryApplication(boolean enable)  { applyInteriorBoundaries = enable;   }
  public void enableSpinningLogo(boolean enable)         { spinLogo    = enable;               }

  public void  setBlurDistance(float distance)           { blurDist    = distance;
                                                           mustUpdateBlurOffsets = true;       }
  public float getBlurDistance()                         { return blurDist;                    }

  public void  setBumpScale(float scale)                 { bumpScale   = scale;                }
  public float getBumpScale()                            { return bumpScale;                   }

  public void  setDropFrequency(float frequency)         { dropletFrequency = frequency;       }
  public float getDropFrequency()                        { return dropletFrequency;            }

  public static class Droplet {
    private float rX;
    private float rY;
    private float rScale;

    Droplet(float rX, float rY, float rScale) {
      this.rX     = rX;
      this.rY     = rY;
      this.rScale = rScale;
    }
    
    float rX()     { return rX;     }
    float rY()     { return rY;     }
    float rScale() { return rScale; }
  }

  public synchronized void addDroplet(Droplet drop) {
    droplets.add(drop);    
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  class Listener implements GLEventListener {
    public void init(GLDrawable drawable) {
      GL gl = drawable.getGL();
      GLU glu = drawable.getGLU();

      initOpenGL(gl, glu);
    }

    public void display(GLDrawable drawable) {
      GL gl = drawable.getGL();
      if (mustUpdateBlurOffsets) {
        updateBlurVertOffset(gl);
        mustUpdateBlurOffsets = false;
      }
      
      // Take a single step in the cellular automaton

      // Disable culling
      gl.glDisable(GL.GL_CULL_FACE);

      if (reset) {
        reset = false;
        flipState = 0;
      }

      if (animate) {
        // Update the textures for one step of the simulation
        doSingleTimeStep(gl);
      } else if (singleStep) {
        doSingleTimeStep(gl);
        singleStep = false;
      }
	
      if (slow && (slowDelay > 0) ) {
        try {
          Thread.sleep(slowDelay);
        } catch (InterruptedException e) {
        }
      }
    }

    public void reshape(GLDrawable drawable, int x, int y, int width, int height) {}

    // Unused routines
    public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  private TGAImage loadImage(String resourceName) {
    try {
      return TGAImage.read(getClass().getClassLoader().getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new GLException(e);
    }
  }

  // We need to load the initial texture file early to get the width
  // and height for the pbuffer
  private void loadInitialTexture(String initialMapFilename) {
    try {
      initialMap = TGAImage.read(getClass().getClassLoader().getResourceAsStream(initialMapFilename));
    } catch (IOException e) {
      throw new GLException(e);
    }
    initialMapDimensions[0] = initialMap.getWidth();
    initialMapDimensions[1] = initialMap.getHeight();
  }

  private void initOpenGL(GL gl, GLU glu) {
    loadTextures(gl, tmpSpinFilename, tmpDropletFilename, tmpCubeMapFilenamePattern);
    tmpSpinFilename           = null;
    tmpDropletFilename        = null;
    tmpCubeMapFilenamePattern = null;

    gl.glMatrixMode(GL.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glMatrixMode(GL.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluOrtho2D(-1, 1, -1, 1);
    
    gl.glClearColor(0, 0, 0, 0);
    gl.glDisable(GL.GL_LIGHTING);
    gl.glDisable(GL.GL_DEPTH_TEST);
      
    createAndWriteUVOffsets(gl, initialMapDimensions[0], initialMapDimensions[1]);

    checkExtension(gl, "GL_NV_register_combiners");
    checkExtension(gl, "GL_NV_register_combiners2");
    checkExtension(gl, "GL_NV_texture_shader");
    checkExtension(gl, "GL_ARB_multitexture");

    ///////////////////////////////////////////////////////////////////////////
    // UV Offset Vertex Program
    ///////////////////////////////////////////////////////////////////////////
    // track the MVP matrix for the vertex program
    gl.glTrackMatrixNV(GL.GL_VERTEX_PROGRAM_NV, 0, GL.GL_MODELVIEW_PROJECTION_NV, GL.GL_IDENTITY_NV);
    float[] rCVConsts = new float[] { 0, 0.5f, 1.0f, 2.0f };
    gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_CONSTS_1, rCVConsts);

    int[] tmpInt = new int[1];
    gl.glGenProgramsNV(1, tmpInt);
    vertexProgramID = tmpInt[0];
    gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, vertexProgramID);

    String programBuffer =
      "!!VP1.0\n" +
      "# CV_WORLDVIEWPROJ_0  = 0,\n" +
      "# CV_WORLDVIEWPROJ_1  = 1,\n" +
      "# CV_WORLDVIEWPROJ_2  = 2,\n" +
      "# CV_WORLDVIEWPROJ_3  = 3,\n" +
      "#\n" +
      "# CV_UV_OFFSET_TO_USE = 4,\n" +
      "#\n" +
      "#\n" +
      "# CV_UV_T0_NO_OFFSET  = 8,\n" +
      "# CV_UV_T0_TYPE1      = 9,\n" +
      "# CV_UV_T0_TYPE2      = 10,\n" +
      "# CV_UV_T0_TYPE3      = 11,\n" +
      "# CV_UV_T0_TYPE4      = 12,\n" +
      "#\n" +
      "# CV_UV_T1_NO_OFFSET  = 13,\n" +
      "# CV_UV_T1_TYPE1      = 14,\n" +
      "# CV_UV_T1_TYPE2      = 15,\n" +
      "# CV_UV_T1_TYPE3      = 16,\n" +
      "# CV_UV_T1_TYPE4      = 17,\n" +
      "#\n" +
      "# CV_UV_T2_NO_OFFSET  = 18,\n" +
      "# CV_UV_T2_TYPE1      = 19,\n" +
      "# CV_UV_T2_TYPE2      = 20,\n" +
      "# CV_UV_T2_TYPE3      = 21,\n" +
      "# CV_UV_T2_TYPE4      = 22,\n" +
      "#\n" +
      "# CV_UV_T3_NO_OFFSET  = 23,\n" +
      "# CV_UV_T3_TYPE1      = 24,\n" +
      "# CV_UV_T3_TYPE2      = 25,\n" +
      "# CV_UV_T3_TYPE3      = 26,\n" +
      "# CV_UV_T3_TYPE4      = 27,\n" +
      "#\n" +
      "# CV_CONSTS_1         = 28\n" +
      "\n" +
      "# Transform vertex-position to clip-space\n" +
      "DP4 o[HPOS].x, v[OPOS], c[0];\n" +
      "DP4 o[HPOS].y, v[OPOS], c[1];\n" +
      "DP4 o[HPOS].z, v[OPOS], c[2];\n" +
      "DP4 o[HPOS].w, v[OPOS], c[3];\n" +
      "\n" +
      "# Read which set of offsets to use\n" +
      "ARL A0.x, c[4].x;\n" +
      "\n" +
      "#	c[CV_CONSTS_1] = c[28]\n" +
      "#	x = 0\n" +
      "#	y = 0.5\n" +
      "#	z = 1\n" +
      "#	w = 2.0f\n" +
      "\n" +
      "#  Put a scale factor into r0 so the sample points\n" +
      "#    can be moved farther from the texel being written\n" +
      "\n" +
      "#MOV R0, c[28].z;\n" +
      "\n" +
      "# Add the offsets to the input texture\n" +
      "# coordinate, creating 4 sets of independent\n" +
      "# texture coordinates.\n" +
      "\n" +
      "ADD o[TEX0], c[A0.x + 8],  v[TEX0];\n" +
      "ADD o[TEX1], c[A0.x + 13], v[TEX0];\n" +
      "ADD o[TEX2], c[A0.x + 18], v[TEX0];\n" +
      "ADD o[TEX3], c[A0.x + 23], v[TEX0];\n" +
      "\n" +
      "#MAD o[TEX0], R0, c[A0.x + 8],  v[TEX0];\n" +
      "#MAD o[TEX1], R0, c[A0.x + 13], v[TEX0]; \n" +
      "#MAD o[TEX2], R0, c[A0.x + 18], v[TEX0]; \n" +
      "#MAD o[TEX3], R0, c[A0.x + 23], v[TEX0];\n" +
      "\n" +
      "END     \n";

    gl.glLoadProgramNV(GL.GL_VERTEX_PROGRAM_NV, vertexProgramID, programBuffer.length(), programBuffer);
    if (gl.glGetError() != GL.GL_NO_ERROR) {
      throw new GLException("Error loading vertex program \"Texcoord_4_Offset.vp\"");
    }

    ///////////////////////////////////////////////////////////////////////////
    // register combiner setup for equal weight combination of texels
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_EQ_WEIGHT_COMBINE] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_EQ_WEIGHT_COMBINE], GL.GL_COMPILE);
    initEqWeightCombine_PostMult(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // register combiners setup for computing force from neighbors (step 1)
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_1] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_1], GL.GL_COMPILE);
    initNeighborForceCalcStep1(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // register combiners setup for computing force from neighbors (step 2)
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_2] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_2], GL.GL_COMPILE);
    initNeighborForceCalcStep2(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // register combiners setup to apply force
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_APPLY_FORCE] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_APPLY_FORCE], GL.GL_COMPILE);
    initApplyForce(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // register combiners setup to apply velocity
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_APPLY_VELOCITY] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_APPLY_VELOCITY], GL.GL_COMPILE);
    initApplyVelocity(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // register combiners setup to create a normal map
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_REGCOMBINER_CREATE_NORMAL_MAP] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_REGCOMBINER_CREATE_NORMAL_MAP], GL.GL_COMPILE);
    initCreateNormalMap(gl);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // texture shader setup for dot product reflection
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_TEXTURE_SHADER_REFLECT] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_TEXTURE_SHADER_REFLECT], GL.GL_COMPILE);
    initDotProductReflect(gl);
    gl.glDisable(GL.GL_BLEND);
    gl.glEnable(GL.GL_TEXTURE_SHADER_NV);
    gl.glEnable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glEndList();

    ///////////////////////////////////////////////////////////////////////////
    // display list to render a single screen space quad.
    ///////////////////////////////////////////////////////////////////////////
    displayListIDs[CA_DRAW_SCREEN_QUAD] = gl.glGenLists(1);
    gl.glNewList(displayListIDs[CA_DRAW_SCREEN_QUAD], GL.GL_COMPILE);
    gl.glColor4f(1, 1, 1, 1);
    gl.glBegin(GL.GL_TRIANGLE_STRIP);
    gl.glTexCoord2f(0, 1); gl.glVertex2f(-1,  1);
    gl.glTexCoord2f(0, 0); gl.glVertex2f(-1, -1);
    gl.glTexCoord2f(1, 1); gl.glVertex2f( 1,  1);
    gl.glTexCoord2f(1, 0); gl.glVertex2f( 1, -1);
    gl.glEnd();
    gl.glEndList();
  }

  private void checkExtension(GL gl, String extensionName) {
    if (!gl.isExtensionAvailable(extensionName)) {
      throw new GLException("Unable to initialize " + extensionName + " OpenGL extension");
    }
  }

  private void doSingleTimeStep(GL gl) {
    int temp;

    // Swap texture source & target indices & pointers
    //  0 = start from initial loaded texture
    //  1/2 = flip flop back and forth between targets & sources

    switch (flipState) {
    case 0:
      texHeightInput    = dynamicTextureIDs[CA_TEXTURE_HEIGHT_SOURCE];       // initial height map.
      texHeightOutput   = dynamicTextureIDs[CA_TEXTURE_HEIGHT_TARGET];    // next height map.

      texVelocityInput  = dynamicTextureIDs[CA_TEXTURE_VELOCITY_SOURCE];  // initial velocity.
      texVelocityOutput = dynamicTextureIDs[CA_TEXTURE_VELOCITY_TARGET];  // next velocity.

      // Clear initial velocity texture to 0x80 == gray
      gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT);

      // Now we need to copy the resulting pixels into the intermediate force field texture
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
      gl.glBindTexture(GL.GL_TEXTURE_2D, texVelocityInput);

      // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
      gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);

      break;  
        
    case 1:
      temp              = texHeightInput;
      texHeightInput    = texHeightOutput;
      texHeightOutput   = temp;

      temp              = texVelocityInput;
      texVelocityInput  = texVelocityOutput;
      texVelocityOutput = temp;

      break;

    case 2:
      temp              = texHeightInput;
      texHeightInput    = texHeightOutput;
      texHeightOutput   = temp;

      temp              = texVelocityInput;
      texVelocityInput  = texVelocityOutput;
      texVelocityOutput = temp;
      break;
    }
	
    // even if wireframe mode, render to texture as solid
    gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
	
    /////////////////////////////////////////////////////////////
    //  Render first 3 components of force from three neighbors
    //  Offsets selected are 1 center texel for center height
    //    and 3 of the 4 nearest neighbors.  Texture selected
    //    is same for all stages as we're turning height difference
    //    of nearest neightbor texels into a force value.

    gl.glCallList(displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_1]);

    // set current source texture for stage 0 texture
    for (int i = 0; i < 4; i++)
      {
        gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB + i);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightInput);
        gl.glEnable(GL.GL_TEXTURE_2D);
      }

    int wrapMode = wrap ? GL.GL_REPEAT : GL.GL_CLAMP_TO_EDGE;
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapMode);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapMode);

    // disable blending
    gl.glDisable(GL.GL_BLEND);

    // render using offset 1 (type 1 -- center + 3 of 4 nearest neighbors).
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 1, 0, 0, 0);

    // bind the vertex program to be used for this step and the next one.
    gl.glBindProgramNV(GL.GL_VERTEX_PROGRAM_NV, vertexProgramID);
    gl.glEnable(GL.GL_VERTEX_PROGRAM_NV);

    // render a screen quad. with texture coords doing difference of nearby texels for force calc.
    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    // Now we need to copy the resulting pixels into the intermediate force field texture
    gl.glActiveTextureARB(GL.GL_TEXTURE2_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_FORCE_INTERMEDIATE]);

    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);

    ////////////////////////////////////////////////////////////////
    // Now add in last component of force for the 4th neighbor
    //  that we didn't have enough texture lookups to do in the 
    //  first pass

    gl.glCallList(displayListIDs[CA_REGCOMBINER_NEIGHBOR_FORCE_CALC_2]);
    
    // Cannot use additive blending as the force contribution might
    //   be negative and would have to subtract from the dest.
    // We must instead use an additional texture as target and read
    //   the previous partial 3-neighbor result into the pixel shader
    //   for possible subtraction

    // Alphablend must be false

    //; t0 = center  (same as last phase)
    //; t1 = 2nd axis final point (same as last phase)
    //; t2 = previous partial result texture sampled at center (result of last phase copied to texture)
    //; t3 = not used (disable now)

    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapMode);
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapMode);

    gl.glActiveTextureARB(GL.GL_TEXTURE3_ARB);
    gl.glDisable(GL.GL_TEXTURE_2D);

    // vertex program already bound.
    // render using offset 2 (type 2 -- final nearest neighbor plus center of previous result).
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 2, 0, 0, 0);

    // render a screen quad
    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    // Now we need to copy the resulting pixels into the intermediate force field texture
    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_FORCE_TARGET]);

    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);

    /////////////////////////////////////////////////////////////////
    // Apply the force with a scale factor to reduce it's magnitude.
    // Add this to the current texture representing the water height.
    
    gl.glCallList(displayListIDs[CA_REGCOMBINER_APPLY_FORCE]);

    // use offsets of zero
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 0, 0, 0, 0);

    // bind the vertex program to be used for this step and the next one.

    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texVelocityInput);
    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_FORCE_TARGET]);
    gl.glActiveTextureARB(GL.GL_TEXTURE2_ARB);
    gl.glDisable(GL.GL_TEXTURE_2D);
    gl.glActiveTextureARB(GL.GL_TEXTURE3_ARB);
    gl.glDisable(GL.GL_TEXTURE_2D);

    // Draw the quad to add in force.
    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    ///////////////////////////////////////////////////////////////////
    // With velocity texture selected, render new excitation droplets
    //   at random freq.

    float randomFrequency = (float) Math.random();

    if (dropletFrequency > randomFrequency) {
      // a drop falls - decide where
      Droplet drop = new Droplet(2 * ((float)Math.random() - 0.5f),
                                 2 * ((float)Math.random() - 0.5f),
                                 0.02f +  0.1f * ((float)Math.random()));
      addDroplet(drop);
    }

    //  Now draw the droplets:
    if (!droplets.isEmpty()) {
      drawDroplets(gl);
      droplets.clear();
    }

    // Now we need to copy the resulting pixels into the velocity texture
    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texVelocityOutput);

    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);  

    //////////////////////////////////////////////////////////////////////
    // Apply velocity to position
    gl.glCallList(displayListIDs[CA_REGCOMBINER_APPLY_VELOCITY]);
    gl.glEnable(GL.GL_VERTEX_PROGRAM_NV);

    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightInput);
    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB); // velocity output already bound
    gl.glEnable(GL.GL_TEXTURE_2D);

    // use offsets of zero
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 0, 0, 0, 0);

    // Draw the quad to add in force.
    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    // Now we need to copy the resulting pixels into the input height texture
    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightInput);
    
    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]); 

    ///////////////////////////////////////////////////////////////////
    //  blur positions to smooth noise & generaly dampen things
    //  degree of blur is controlled by magnitude of 4 neighbor texel
    //   offsets with bilinear on
    
    for (int i = 1; i < 4; i++) {
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB + i);
      gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightInput);
      gl.glEnable(GL.GL_TEXTURE_2D);
    }

    // use offsets of 3
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 3, 0, 0, 0);

    gl.glCallList(displayListIDs[CA_REGCOMBINER_EQ_WEIGHT_COMBINE]);

    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    // Draw the logo in the water.
    if (applyInteriorBoundaries) {
      gl.glDisable(GL.GL_VERTEX_PROGRAM_NV);
      drawInteriorBoundaryObjects(gl);
    }

    // Now we need to copy the resulting pixels into the velocity texture
    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightOutput);

    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);
      
    ///////////////////////////////////////////////////////////////////
    // If selected, create a normal map from the height
      
    if (createNormalMap) {
      createNormalMap(gl);
    }
      
    ///////////////////////////////////////////////////////////
    // Flip the state variable for the next round of rendering
    switch (flipState) {
    case 0:
      flipState = 1;
      break;
    case 1:
      flipState = 2;
      break;
    case 2:
      flipState = 1;
      break;
    }
  }

  private void createNormalMap(GL gl) {
    // use the height output on all four texture stages
    for (int i = 0; i < 4; i++) {
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB + i);
      gl.glBindTexture(GL.GL_TEXTURE_2D, texHeightOutput);
      gl.glEnable(GL.GL_TEXTURE_2D);
    }

    // Set constants for red & green scale factors (also essential color masks)
    // Red mask first
    float[] pixMasks = new float[] { normalSTScale, 0.0f, 0.0f, 0.0f };

    gl.glCombinerStageParameterfvNV(GL.GL_COMBINER2_NV, GL.GL_CONSTANT_COLOR0_NV, pixMasks);

    // Now green mask & scale:
    pixMasks[0] = 0.0f;
    pixMasks[1] = normalSTScale;
    gl.glCombinerStageParameterfvNV(GL.GL_COMBINER2_NV, GL.GL_CONSTANT_COLOR1_NV, pixMasks);

    gl.glCallList(displayListIDs[CA_REGCOMBINER_CREATE_NORMAL_MAP]);

    // set vp offsets to nearest neighbors
    gl.glProgramParameter4fNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_OFFSET_TO_USE, 4, 0, 0, 0);
    gl.glEnable(GL.GL_VERTEX_PROGRAM_NV);
    
    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    // Now we need to copy the resulting pixels into the normal map
    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, dynamicTextureIDs[CA_TEXTURE_NORMAL_MAP]);
    
    // use CopyTexSubImage for speed (even though we copy all of it) since we pre-allocated the texture
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 0, 0, initialMapDimensions[0], initialMapDimensions[1]);
  }

  private void drawInteriorBoundaryObjects(GL gl) {
    gl.glDisable(GL.GL_REGISTER_COMBINERS_NV);
    
    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, staticTextureIDs[CA_TEXTURE_INITIAL_MAP]);
    gl.glEnable(GL.GL_TEXTURE_2D);

    gl.glEnable(GL.GL_ALPHA_TEST);

    // disable other texture units.
    for (int i = 1; i < 4; i++) {
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB + i);
      gl.glDisable(GL.GL_TEXTURE_2D);
    }
    
    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL.GL_BLEND);

    gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

    if (spinLogo) {
      gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
      gl.glBindTexture(GL.GL_TEXTURE_2D, staticTextureIDs[CA_TEXTURE_SPIN]);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glRotatef(angle, 0, 0, 1);
      angle += 1;

      gl.glCallList(displayListIDs[CA_DRAW_SCREEN_QUAD]);

      gl.glPopMatrix();
    }

    gl.glDisable(GL.GL_ALPHA_TEST);
    gl.glDisable(GL.GL_BLEND);
  }

  private void createTextureObject(GL gl, int id, TGAImage image, boolean test) {
    // Fetch image data out of image
    gl.glBindTexture(GL.GL_TEXTURE_2D, id);
    gl.glTexImage2D (GL.GL_TEXTURE_2D, 
                     0,
                     GL.GL_RGBA8,
                     image.getWidth(),
                     image.getHeight(),
                     0,
                     image.getGLFormat(),
                     GL.GL_UNSIGNED_BYTE,
                     image.getData());
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
  }

  private void loadCubeMap(GL gl, int id, String filenamePattern, boolean mipmap) {
    int[] faces = new int[] { GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X_ARB,
                              GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X_ARB,
                              GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y_ARB,
                              GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_ARB,
                              GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z_ARB,
                              GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_ARB };
    String[] faceNames = new String[] { "posx", "negx", "posy", "negy", "posz", "negz" };

    // create and bind a cubemap texture object
    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP_ARB, id);

    // enable automipmap generation if needed.
    gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_GENERATE_MIPMAP_SGIS, (mipmap ? 1 : 0));
    
    if (mipmap)
      gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    else
      gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP_ARB, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    // load 6 faces.
    MessageFormat fmt = new MessageFormat(filenamePattern);
    for (int i = 0; i < 6; i++) {
      String filename = MessageFormat.format(filenamePattern, new String[] { faceNames[i] });
      TGAImage image = loadImage(filename);
      gl.glTexImage2D(faces[i], 
                      0,
                      GL.GL_RGBA8,
                      image.getWidth(),
                      image.getHeight(),
                      0,
                      image.getGLFormat(),
                      GL.GL_UNSIGNED_BYTE,
                      image.getData());
    }
  }

  private void loadTextures(GL gl,
                            String spinFilename,
                            String dropletFilename,
                            String cubeMapFilenamePattern) {
    if (initialMap == null) {
      throw new GLException("Must call loadInitialTexture ahead of time");
    }

    TGAImage spin    = loadImage(spinFilename);
    TGAImage droplet = loadImage(dropletFilename);

    gl.glGenTextures(CA_NUM_STATIC_TEXTURES, staticTextureIDs); 
    gl.glGenTextures(CA_NUM_DYNAMIC_TEXTURES, dynamicTextureIDs); // also create intermediate texture object

    // upload the initial map texture
    createTextureObject(gl, staticTextureIDs[CA_TEXTURE_INITIAL_MAP], initialMap, true);

    createTextureObject(gl, staticTextureIDs[CA_TEXTURE_SPIN], spin, true);

    createTextureObject(gl, staticTextureIDs[CA_TEXTURE_DROPLET], droplet, false);

    // load the cubemap texture
    loadCubeMap(gl, staticTextureIDs[CA_TEXTURE_CUBEMAP], cubeMapFilenamePattern, true);

    for (int i = 0; i < CA_NUM_DYNAMIC_TEXTURES; i++) {
      // now create a dummy intermediate textures from the initial map texture
      createTextureObject(gl, dynamicTextureIDs[i], initialMap, false);
    }

    initialMap = null;

    texHeightInput    = staticTextureIDs [CA_TEXTURE_INITIAL_MAP];      // initial height map.
    texHeightOutput   = dynamicTextureIDs[CA_TEXTURE_HEIGHT_TARGET];    // next height map.
    
    texVelocityInput  = dynamicTextureIDs[CA_TEXTURE_VELOCITY_SOURCE];  // initial velocity.
    texVelocityOutput = dynamicTextureIDs[CA_TEXTURE_VELOCITY_TARGET];  // next velocity.
  }

  private void createAndWriteUVOffsets(GL gl, int width, int height) {
    // This sets vertex shader constants used to displace the
    //  source texture over several additive samples.  This is 
    //  used to accumulate neighboring texel information that we
    //  need to run the game - the 8 surrounding texels, and the 
    //  single source texel which will either spawn or die in the 
    //  next generation.
    // Label the texels as follows, for a source texel "e" that
    //  we want to compute for the next generation:
    //
    //          abc
    //          def
    //          ghi:

    // first the easy one: no offsets for sampling center
    //  occupied or unoccupied
    // Use index offset value 0.0 to access these in the 
    //  vertex shader.
    
    perTexelWidth  = 1.0f / width;
    perTexelHeight = 1.0f / height;

    // Offset set 0 : center texel sampling
    float[] noOffsetX = new float[] { 0, 0, 0, 0 };
    float[] noOffsetY = new float[] { 0, 0, 0, 0 };

    // Offset set 1:  For use with neighbor force pixel shader 1
    //  samples center with 0, +u, -u, and +v,
    //  ie the 'e','d', 'f', and 'h' texels
    float dist = 1.5f;
    float[] type1OffsetX = new float[] { 0.0f, -dist * perTexelWidth,  dist * perTexelWidth,   dist * perTexelWidth  };
    float[] type1OffsetY = new float[] { 0.0f,  dist * perTexelHeight, dist * perTexelHeight, -dist * perTexelHeight };

    // Offset set 2:  for use with neighbor force pixel shader 2
    //  samples center with 0, and -v texels 
    //  ie the 'e' and 'b' texels
    // This completes a pattern of sampling center texel and it's
    //   4 nearest neighbors to run the height-based water simulation
    // 3rd must be 0 0 to sample texel center from partial result
    //   texture.

    float[] type2OffsetX = new float[] { 0.0f, -dist * perTexelWidth,  0.0f, 0.0f   };
    float[] type2OffsetY = new float[] { 0.0f, -dist * perTexelHeight, 0.0f, 0.0f   };
        
    // type 3 offsets
    updateBlurVertOffset(gl);

    /////////////////////////////////////////////////////////////
    // Nearest neighbor offsets:

    float[] type4OffsetX = new float[] { -perTexelWidth,   perTexelWidth,   0.0f,              0.0f   };
    float[] type4OffsetY = new float[] { 0.0f,             0.0f,            -perTexelHeight,   perTexelHeight };

    // write all these offsets to constant memory
    for (int i = 0; i < 4; ++i) {
      float noOffset[]    = { noOffsetX[i],    noOffsetY[i],    0.0f, 0.0f };
      float type1Offset[] = { type1OffsetX[i], type1OffsetY[i], 0.0f, 0.0f };
      float type2Offset[] = { type2OffsetX[i], type2OffsetY[i], 0.0f, 0.0f };
      float type4Offset[] = { type4OffsetX[i], type4OffsetY[i], 0.0f, 0.0f };

      gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_T0_NO_OFFSET + 5 * i, noOffset);
      gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_T0_TYPE1     + 5 * i, type1Offset);
      gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_T0_TYPE2     + 5 * i, type2Offset);
      gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_T0_TYPE4     + 5 * i, type4Offset);
    }
  }

  private void updateBlurVertOffset(GL gl) {
    float[] type3OffsetX = new float[] { -perTexelWidth * 0.5f, 
                                         perTexelWidth, 
                                         perTexelWidth * 0.5f, 
                                         -perTexelWidth 
    };
    float[] type3OffsetY = new float[] { perTexelHeight,
                                         perTexelHeight * 0.5f,
                                         -perTexelHeight,
                                         -perTexelHeight * 0.5f 
    };
    float[] offsets = new float[] { 0, 0, 0, 0 };

    for (int i = 0; i < 4; ++i) {
      offsets[0] = blurDist * ( type3OffsetX[i]);
      offsets[1] = blurDist * ( type3OffsetY[i]);
      gl.glProgramParameter4fvNV(GL.GL_VERTEX_PROGRAM_NV, CV_UV_T0_TYPE3 + 5 * i, offsets);
    }
  }

  private synchronized void drawDroplets(GL gl) {
    gl.glDisable(GL.GL_REGISTER_COMBINERS_NV);
    gl.glDisable(GL.GL_VERTEX_PROGRAM_NV);

    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glBindTexture(GL.GL_TEXTURE_2D, staticTextureIDs[CA_TEXTURE_DROPLET]);
    gl.glEnable(GL.GL_TEXTURE_2D);

    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
    gl.glDisable(GL.GL_TEXTURE_2D);

    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
    gl.glEnable(GL.GL_BLEND);

    gl.glBegin(GL.GL_QUADS);
    gl.glColor4f(1, 1, 1, 1);
    for (Iterator iter = droplets.iterator(); iter.hasNext(); ) {
      Droplet droplet = (Droplet) iter.next();
      // coords in [-1,1] range

      // Draw a single quad to the texture render target
      // The quad is textured with the initial droplet texture, and
      //   covers some small portion of the render target
      // Draw the droplet
       
      gl.glTexCoord2f(0, 0); gl.glVertex2f(droplet.rX() - droplet.rScale(), droplet.rY() - droplet.rScale());
      gl.glTexCoord2f(1, 0); gl.glVertex2f(droplet.rX() + droplet.rScale(), droplet.rY() - droplet.rScale());
      gl.glTexCoord2f(1, 1); gl.glVertex2f(droplet.rX() + droplet.rScale(), droplet.rY() + droplet.rScale());
      gl.glTexCoord2f(0, 1); gl.glVertex2f(droplet.rX() - droplet.rScale(), droplet.rY() + droplet.rScale());          
    }
    gl.glEnd();

    gl.glDisable(GL.GL_BLEND);
  }

  //----------------------------------------------------------------------
  // Inlined register combiner and texture shader programs
  // (don't want to port nvparse as it's a dead-end; we'll focus on Cg instead)

  private void initEqWeightCombine_PostMult(GL gl) {
    float[] const0 = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
    float[] const1 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR0_NV, const0);
    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR1_NV, const1);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 4);
  
    int stage = 0;
    // Stage 0
    // rgb
    // {
    //   discard = half_bias(tex0);
    //   discard = half_bias(tex1);
    //   spare0 = sum();
    //   scale_by_one_half();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_HALF_BIAS_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE1_ARB, GL.GL_HALF_BIAS_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // rgb
    // {
    //   discard = half_bias(tex2);
    //   discard = half_bias(tex3);
    //   spare1 = sum();
    //   scale_by_one_half();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE2_ARB, GL.GL_HALF_BIAS_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE3_ARB, GL.GL_HALF_BIAS_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE1_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 2
    // rgb
    // {
    //   discard = spare0;
    //   discard = spare1;
    //   spare0 = sum();
    //   scale_by_one_half();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, false, false, false);

    // Stage 2
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 3
    // rgb
    // {
    //   discard = const0;
    //   discard = spare0;
    //   spare0 = sum();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 3
    // alpha
    discardAlpha(gl, stage);

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);

    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV);
  }

  private void initNeighborForceCalcStep1(GL gl) {
    // Step one in the nearest-neighbor force calculation for height-based water
    // simulation.  NeighborForceCalc2 is the second step.
    //
    // This step takes the center point and three neighboring points, and computes
    // the texel difference as the "force" acting to pull the center texel.
    // 
    // The amount to which the computed force is applied to the texel is controlled
    // in a separate shader.

    //  get colors from all 4 texture stages
    //  tex0 = center texel
    //  tex1 = 1st neighbor
    //  tex2 = 2nd neighbor - same axis as 1st neighbor point
    //       so force for that axis == t1 - t0 + t2 - t0
    //  tex3 = 3rd neighbor on other axis

    float[] const0 = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };

    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR0_NV, const0);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 8);

    int stage = 0;
    // Stage 0
    // rgb
    // {
    //   //s0 = t1 - t0;
    //   discard = -tex0;
    //   discard = tex1;
    //   spare0 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE1_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // rgb
    // {
    //   //s1 = t2 - t0;
    //   discard = -tex0;
    //   discard = tex2;
    //   spare1 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE2_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE1_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 2
    // // 'force' for 1st axis
    // rgb 
    // {
    //   //s0 = s0 + s1 = t1 - t0 + t2 - t0;
    //   discard = spare0;
    //   discard = spare1;
    //   spare0 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 2
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 3
    // // one more point for 2nd axis
    // rgb
    // {
    //   //s1 = t3 - t0;
    //   discard = -tex0;
    //   discard = tex3;
    //   spare1 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE3_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE1_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 3
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 4
    // rgb
    // {
    //   //s0 = s0 + s1 = t3 - t0 + t2 - t0 + t1 - t0;
    //   discard = spare0;
    //   discard = spare1;
    //   spare0 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 4
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 5
    // // Now add in a force to gently pull the center texel's 
    // //  value to 0.5.  The strength of this is controlled by
    // //  the PCN_EQ_REST_FAC  - restoration factor
    // // Without this, the simulation will fade to zero or fly
    // //  away to saturate at 1.0
    // rgb 
    // {
    //   //s1 = 0.5 - t0;  
    //   discard = -tex0;
    //   discard = const0;
    //   spare1 = sum();  
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE1_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);
  
    // Stage 5
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 6
    // {
    //   rgb
    //   {
    //     discard = spare1 * const0;
    //     discard = spare0;
    //     spare0 = sum();
    //   }
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 6
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 7
    // rgb
    // {
    //   discard = spare0;
    //   discard = const0;
    //   spare0 = sum();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 7
    // alpha
    discardAlpha(gl, stage);

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV);
  }

  private void initNeighborForceCalcStep2(GL gl) {
    // 2nd step of force calc for render-to-texture
    // water simulation.
    //
    // Adds the 4th & final neighbor point to the 
    // force calc..
    //
    // Bias and scale the values so 0 force is 0.5, 
    // full negative force is 0.0, and full pos is
    // 1.0
    //
    // tex0    Center texel
    // tex1    2nd axis neighbor point
    // tex2    previous partial force amount
    // Result from t1 - t0 is added to this t2
    //  partial result & output

    float[] const0 = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };

    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR0_NV, const0);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 2);

    int stage = 0;
    // Stage 0
    // last element of neighbor force
    // rgb
    // {
    //   discard = -tex0;
    //   discard = tex1;
    //   spare0 = sum();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE1_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // add with previous partial force amount
    // rgb
    // {
    //   discard = spare0;
    //   discard = tex2;
    //   spare0 = sum();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE2_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV);
  }

  private void initApplyForce(GL gl) {
    // This shader samples t1, biases its value to a signed number, and applies this
    // value multiplied by a scale factor to the t0 sample.
    //
    // This is used to apply a "force" texture value to a "velocity" state texture
    // for nearest-neighbor height-based water simulations.  The output pixel is
    // the new "velocity" value to replace the t0 sample in rendering to a new 
    // texture which will replace the texture selected into t0.
    //
    // A nearly identical shader using a different scaling constant is used to
    // apply the "velocity" value to a "height" texture at each texel.
    //
    // t1 comes in the range [0,1] but needs to hold signed values, so a value of
    // 0.5 in t1 represents zero force.  This is biased to a signed value in 
    // computing the new velocity.
    //
    // tex0 = previous velocity
    // tex1 = force
    //
    // Bias the force so that 0.5 input = no change in t0 value
    //  and 0.0 input means -0.5 * scale change in t0 value
    //
    // New velocity = force * scale + previous velocity

    float[] const0 = new float[] { 0.25f, 0.25f, 0.25f, 1.0f };
    float[] const1 = new float[] { 0.5f,  0.5f,  0.5f,  1.0f };

    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR0_NV, const0);
    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR1_NV, const1);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 4);
  
    int stage = 0;
    // Stage 0
    // rgb
    // {
    //   discard = expand(tex1) * const0;
    //   discard = expand(tex0);
    //   spare0 = sum();
    //   scale_by_one_half();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE1_ARB, GL.GL_EXPAND_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE0_ARB, GL.GL_EXPAND_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // rgb
    // {
    //   discard = spare0;
    //   discard = const1;
    //   spare0 = sum();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_CONSTANT_COLOR1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_UNSIGNED_IDENTITY_NV);
  }

  private void initApplyVelocity(GL gl) {
    // This shader samples t1, biases its value to a signed number, and applies this
    // value multiplied by a scale factor to the t0 sample.
    //
    // This is used to apply a "velocity" texture value to a "height" state texture
    // for nearest-neighbor height-based water simulations.  The output pixel is
    // the new "height" value to replace the t0 sample in rendering to a new 
    // texture which will replace the texture selected into t0.
    //
    // A nearly identical shader using a different scaling constant is used to
    // apply the "force" value to the "velocity" texture at each texel.
    //
    // t1 comes in the range [0,1] but needs to hold signed values, so a value of
    // 0.5 in t1 represents zero velocity.  This is biased to a signed value in 
    // computing the new position.                       
    //
    // tex0 = height field
    // tex1 = velocity          
    //
    // Bias the force/velocity to a signed value so we can subtract from
    //   the t0 position sample.
    //
    // New height = velocity * scale factor + old height

    float[] const0 = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };

    gl.glCombinerParameterfvNV(GL.GL_CONSTANT_COLOR0_NV, const0);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 2);
  
    int stage = 0;
    // Stage 0
    // rgb
    // {
    //   discard = expand(tex1) * const0;
    //   discard = expand(tex0);
    //   spare0 = sum();
    //   scale_by_one_half();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE1_ARB, GL.GL_EXPAND_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE0_ARB, GL.GL_EXPAND_NORMAL_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // rgb
    // {
    //   discard = spare0;
    //   discard = const0;
    //   spare0 = sum();
    // }
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_UNSIGNED_IDENTITY_NV);
  }

  private void initCreateNormalMap(GL gl) {
    // Neighbor-differencing for RGB normal map creation.  Scale factors for s and t
    // axis components are set in program code.
    // This does a crude 1-s^2-t^2 calculation for the blue component in order to
    // approximately normalize the RGB normal map vector.  For s^2+t^2 close to 1.0,
    // this is a close approximation to blue = sqrt(1 - s^2 - t^2) which would give a
    // normalized vector.
    // An additional pass with a dependent texture lookup (alpha-red or green-blue)
    // could be used to produce an exactly normalized normal.

    // colors from all 4 texture stages
    // tex0 = -s,  0
    // tex1 = +s,  0
    // tex2 =  0, +t
    // tex3 =  0, -t

    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 7);
  
    int stage = 0;
    // Stage 0
    // rgb
    // {
    //   // (t0 - t1)*4  : 4 for higher scale
    //   discard = -tex1;
    //   discard = tex0;
    //   spare0 = sum();
    //   scale_by_four();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE1_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE0_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_SCALE_BY_FOUR_NV, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 1
    // rgb
    // {
    //   // (t3 - t2)*4 : 4 for higher scale
    //   discard = -tex2;
    //   discard = tex3;
    //   spare1 = sum();
    //   scale_by_four();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE2_ARB, GL.GL_SIGNED_NEGATE_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_TEXTURE3_ARB, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE1_NV, GL.GL_SCALE_BY_FOUR_NV, GL.GL_NONE, false, false, false);

    // Stage 1
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 2
    // Define const0 in the third general combiner as RGBA = (scale, 0, 0, 0)
    //  Where scale [0,1] is applied to reduce the magnitude
    //  of the s axis component of the normal.
    // Define const1 in the third combiner similarly to affect the t axis component
    // define these by "ramboing" them in the C++ code that uses this combiner script.
    //
    // rgb
    // {
    //   // see comment about consts above!
    //   // t0 = s result in red only
    //   discard = spare0 * const0;
    //   discard = spare1 * const1;
    //   spare0 = sum();
    // }
    //
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_CONSTANT_COLOR1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 2
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 3
    // rgb
    // {
    //   tex1 = spare0 * spare0;
    //   scale_by_two();
    // }
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_TEXTURE1_ARB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SCALE_BY_TWO_NV, GL.GL_NONE, false, false, false);

    // Stage 3
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 4
    //
    // const0 = (1, 1, 0, 0);
    // rgb
    // {
    //   spare1 = unsigned_invert(tex1) . const0;
    //   scale_by_one_half();
    // }
    //
    float[] const0 = new float[] { 1.0f, 1.0f, 0.0f, 0.0f };

    gl.glCombinerStageParameterfvNV(GL.GL_COMBINER0_NV + stage, GL.GL_CONSTANT_COLOR0_NV, const0);

    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_TEXTURE1_ARB, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_SPARE1_NV, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SCALE_BY_ONE_HALF_NV, GL.GL_NONE, true, false, false);

    // Stage 4
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 5
    //
    // const0 = (0.5, 0.5, 0, 0);
    // rgb
    // {
    //   discard = spare0;
    //   discard = const0;
    //   spare0 = sum();
    // }
    //
    const0 = new float[] { 0.5f, 0.5f, 0.0f, 0.0f };

    gl.glCombinerStageParameterfvNV(GL.GL_COMBINER0_NV + stage, GL.GL_CONSTANT_COLOR0_NV, const0);

    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 5
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    // Stage 6
    //
    //
    // const0 = (0, 0, 1, 1);
    // rgb 
    // {
    //   discard = spare1 * const0;
    //   discard = spare0;
    //   spare0 = sum();
    // }
    //
    const0 = new float[] { 0.0f, 0.0f, 1.0f, 1.0f };

    gl.glCombinerStageParameterfvNV(GL.GL_COMBINER0_NV + stage, GL.GL_CONSTANT_COLOR0_NV, const0);

    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_SPARE1_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_CONSTANT_COLOR0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_SPARE0_NV, GL.GL_SIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_SPARE0_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 6
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    gl.glEnable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_SPARE0_NV, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_ZERO, GL.GL_UNSIGNED_INVERT_NV);
  }

  private void initDotProductReflect(GL gl) {
    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_SHADER_OPERATION_NV, GL.GL_TEXTURE_2D);

    // 1 of 3
    gl.glActiveTextureARB(GL.GL_TEXTURE1_ARB);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_RGBA_UNSIGNED_DOT_PRODUCT_MAPPING_NV, GL.GL_EXPAND_NORMAL_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_SHADER_OPERATION_NV, GL.GL_DOT_PRODUCT_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_PREVIOUS_TEXTURE_INPUT_NV, GL.GL_TEXTURE0_ARB);

    // 2 of 3
    gl.glActiveTextureARB(GL.GL_TEXTURE2_ARB);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_RGBA_UNSIGNED_DOT_PRODUCT_MAPPING_NV, GL.GL_EXPAND_NORMAL_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_SHADER_OPERATION_NV, GL.GL_DOT_PRODUCT_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_PREVIOUS_TEXTURE_INPUT_NV, GL.GL_TEXTURE0_ARB);

    // 3 of 3
    gl.glActiveTextureARB(GL.GL_TEXTURE3_ARB);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_RGBA_UNSIGNED_DOT_PRODUCT_MAPPING_NV, GL.GL_EXPAND_NORMAL_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_SHADER_OPERATION_NV, GL.GL_DOT_PRODUCT_REFLECT_CUBE_MAP_NV);
    gl.glTexEnvi(GL.GL_TEXTURE_SHADER_NV, GL.GL_PREVIOUS_TEXTURE_INPUT_NV, GL.GL_TEXTURE0_ARB);

    gl.glActiveTextureARB(GL.GL_TEXTURE0_ARB);
    gl.glCombinerParameteriNV(GL.GL_NUM_GENERAL_COMBINERS_NV, 1);
  
    int stage = 0;
    // Stage 0
    // rgb
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_A_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_C_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doIn(gl, stage, GL.GL_RGB, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_RGB);
    doOut(gl, stage, GL.GL_RGB, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);

    // Stage 0
    // alpha
    discardAlpha(gl, stage);

    ++stage;

    gl.glDisable(GL.GL_PER_STAGE_CONSTANTS_NV);
    gl.glCombinerParameteriNV(GL.GL_COLOR_SUM_CLAMP_NV, GL.GL_FALSE);
    doFinal(gl, GL.GL_TEXTURE3_ARB, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV);
  }

  private void discardAlpha(GL gl, int stage) {
    doIn(gl, stage, GL.GL_ALPHA, GL.GL_VARIABLE_A_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_BLUE);
    doIn(gl, stage, GL.GL_ALPHA, GL.GL_VARIABLE_B_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_BLUE);
    doIn(gl, stage, GL.GL_ALPHA, GL.GL_VARIABLE_C_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_BLUE);
    doIn(gl, stage, GL.GL_ALPHA, GL.GL_VARIABLE_D_NV, GL.GL_ZERO, GL.GL_UNSIGNED_IDENTITY_NV, GL.GL_BLUE);
    doOut(gl, stage, GL.GL_ALPHA, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_DISCARD_NV, GL.GL_NONE, GL.GL_NONE, false, false, false);
  }

  private void doIn(GL gl,
                    int stage,
                    int colorSpace,
                    int variable,
                    int reg,
                    int operation,
                    int colorSelector) {
    gl.glCombinerInputNV(GL.GL_COMBINER0_NV + stage,
                         colorSpace,
                         variable,
                         reg,
                         operation,
                         colorSelector);
  }

  private void doOut(GL gl,
                     int stage,
                     int colorSpace,
                     int in0,
                     int in1,
                     int out0,
                     int scale,
                     int bias,
                     boolean unknown0,
                     boolean unknown1,
                     boolean unknown2) {
    gl.glCombinerOutputNV(GL.GL_COMBINER0_NV + stage,
                          colorSpace,
                          in0,
                          in1,
                          out0,
                          scale,
                          bias,
                          unknown0,
                          unknown1,
                          unknown2);
  }

  private void doFinal(GL gl,
                       int variableDInput,
                       int variableDOperation,
                       int variableGInput,
                       int variableGOperation) {
    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_A_NV,
                              GL.GL_ZERO,
                              GL.GL_UNSIGNED_IDENTITY_NV,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_B_NV,
                              GL.GL_ZERO,
                              GL.GL_UNSIGNED_IDENTITY_NV,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_C_NV,
                              GL.GL_ZERO,
                              GL.GL_UNSIGNED_IDENTITY_NV,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_D_NV,
                              variableDInput,
                              variableDOperation,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_E_NV,
                              GL.GL_ZERO,
                              GL.GL_UNSIGNED_IDENTITY_NV,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_F_NV,
                              GL.GL_ZERO,
                              GL.GL_UNSIGNED_IDENTITY_NV,
                              GL.GL_RGB);

    gl.glFinalCombinerInputNV(GL.GL_VARIABLE_G_NV,
                              variableGInput,
                              variableGOperation,
                              GL.GL_ALPHA);
  }
}
