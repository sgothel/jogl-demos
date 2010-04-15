package demos.misc;

import com.jogamp.opengl.util.FileUtil;
import com.jogamp.opengl.util.TGAWriter;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.gl2.TileRenderer;
import demos.gears.Gears;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;

/** Demonstrates the TileRenderer class by rendering a large version
    of the Gears demo to the specified file. */

public class TiledRendering {

  public static void main(String[] args) throws IOException {
      
    if (args.length != 1) {
      System.out.println("Usage: java TiledRendering [output file name]");
      System.out.println("Writes output (a large version of the Gears demo) to");
      System.out.println("the specified file, using either ImageIO or the fast TGA writer");
      System.out.println("depending on the file extension.");
      System.exit(1);
    }

    String filename = args[0];
    File file = new File(filename);

    GLCapabilities caps = new GLCapabilities(null);
    caps.setDoubleBuffered(false);

    if (!GLDrawableFactory.getFactory(caps.getGLProfile()).canCreateGLPbuffer(null)) {
      System.out.println("Demo requires pbuffer support");
      System.exit(1);
    }

    // Use a pbuffer for rendering
    GLPbuffer pbuffer = GLDrawableFactory.getFactory(caps.getGLProfile()).createGLPbuffer(caps, null,
                                                                       256, 256,
                                                                       null);
    
    // Fix the image size for now
    int tileWidth = 256;
    int tileHeight = 256;
    int imageWidth = tileWidth * 16;
    int imageHeight = tileHeight * 12;
    
    // Figure out the file format
    TGAWriter tga = null;
    BufferedImage img = null;
    Buffer buf = null;
    
    if (filename.endsWith(".tga")) {
      tga = new TGAWriter();
      tga.open(file,
               imageWidth,
               imageHeight,
               false);
      buf = tga.getImageData();
    } else {
      img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
      buf = ByteBuffer.wrap(((DataBufferByte) img.getRaster().getDataBuffer()).getData());
    }

    // Initialize the tile rendering library
    TileRenderer renderer = new TileRenderer();
    renderer.setTileSize(tileWidth, tileHeight, 0);
    renderer.setImageSize(imageWidth, imageHeight);
    renderer.setImageBuffer(GL2.GL_BGR, GL.GL_UNSIGNED_BYTE, buf);
    renderer.trPerspective(20.0f, (float) imageWidth / (float) imageHeight, 5.0f, 60.0f);

    GLContext context = pbuffer.getContext();
    if (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
      System.out.println("Error making pbuffer's context current");
      System.exit(1);
    }
    
    GL2 gl = pbuffer.getGL().getGL2();
    gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);
    // Tile renderer will set up projection matrix    

    do {
      renderer.beginTile(gl);
      drawGears(gl);
    } while (renderer.endTile(gl));

    context.release();

    // Close things up and/or write image using ImageIO
    if (tga != null) {
      tga.close();
    } else {
      ImageUtil.flipImageVertically(img);
      if (!ImageIO.write(img, FileUtil.getFileSuffix(file), file)) {
        System.err.println("Error writing file using ImageIO (unsupported file format?)");
      }
    }
  }

  private static void drawGears(GL2 gl) {
    float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
    float angle = 0.0f;
    float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
    float red[] = { 0.8f, 0.1f, 0.0f, 1.0f };
    float green[] = { 0.0f, 0.8f, 0.2f, 1.0f };
    float blue[] = { 0.2f, 0.2f, 1.0f, 1.0f };

    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_POSITION, pos, 0);
    gl.glEnable(GL.GL_CULL_FACE);
    gl.glEnable(GL2ES1.GL_LIGHTING);
    gl.glEnable(GL2ES1.GL_LIGHT0);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glEnable(GL2ES1.GL_NORMALIZE);

    gl.glPushMatrix();
    gl.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
    gl.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
    gl.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
            
    gl.glPushMatrix();
    gl.glTranslatef(-3.0f, -2.0f, 0.0f);
    gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, red, 0);
    Gears.gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
    gl.glPopMatrix();
            
    gl.glPushMatrix();
    gl.glTranslatef(3.1f, -2.0f, 0.0f);
    gl.glRotatef(-2.0f * angle - 9.0f, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, green, 0);
    Gears.gear(gl, 0.5f, 2.0f, 2.0f, 10, 0.7f);
    gl.glPopMatrix();
            
    gl.glPushMatrix();
    gl.glTranslatef(-3.1f, 4.2f, 0.0f);
    gl.glRotatef(-2.0f * angle - 25.0f, 0.0f, 0.0f, 1.0f);
    gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_AMBIENT_AND_DIFFUSE, blue, 0);
    Gears.gear(gl, 1.3f, 2.0f, 0.5f, 10, 0.7f);
    gl.glPopMatrix();
            
    gl.glPopMatrix();
  }
}
