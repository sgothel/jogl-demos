package demos.hdr;

import java.io.*;
import java.util.*;

import javax.media.opengl.*;
import com.sun.opengl.cg.*;
import demos.util.*;

public interface Pipeline {
  public void init();
  public void initFloatingPointTexture      (GL gl, int textureObject, int w, int h);
  public void initTexture                   (GL gl, int textureObject, int w, int h);
  public void copyToTexture                 (GL gl, int textureObject, int w, int h);
  public void bindTexture                   (GL gl, int textureObject);
  public int  loadVertexProgram             (GL gl, String filename) throws IOException;
  public int  loadFragmentProgram           (GL gl, String filename) throws IOException;
  public void enableVertexProgram           (GL gl, int program);
  public void enableFragmentProgram         (GL gl, int program);
  public void disableVertexProgram          (GL gl);
  public void disableFragmentProgram        (GL gl);
  public int  getNamedParameter             (int program, String name);
  public void setVertexProgramParameter1f   (GL gl, int param, float val);
  public void setVertexProgramParameter3f   (GL gl, int param, float x, float y, float z);
  public void setVertexProgramParameter4f   (GL gl, int param, float x, float y, float z, float w);
  public void setFragmentProgramParameter1f (GL gl, int param, float val);
  public void setFragmentProgramParameter3f (GL gl, int param, float x, float y, float z);
  public void setFragmentProgramParameter4f (GL gl, int param, float x, float y, float z, float w);
  public void trackModelViewProjectionMatrix(GL gl, int param);
  public void setMatrixParameterfc          (GL gl, int param, float[] matrix);
}
