package demos.hdr;

import java.io.IOException;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2;


public interface Pipeline {
  public void init();
  public void initFloatingPointTexture      (GL2 gl, int textureObject, int w, int h);
  public void initTexture                   (GL2 gl, int textureObject, int w, int h);
  public void copyToTexture                 (GL2 gl, int textureObject, int w, int h);
  public void bindTexture                   (GL2 gl, int textureObject);
  public int  loadVertexProgram             (GL2 gl, String filename) throws IOException;
  public int  loadFragmentProgram           (GL2 gl, String filename) throws IOException;
  public void enableVertexProgram           (GL2 gl, int program);
  public void enableFragmentProgram         (GL2 gl, int program);
  public void disableVertexProgram          (GL2 gl);
  public void disableFragmentProgram        (GL2 gl);
  public int  getNamedParameter             (int program, String name);
  public void setVertexProgramParameter1f   (GL2 gl, int param, float val);
  public void setVertexProgramParameter3f   (GL2 gl, int param, float x, float y, float z);
  public void setVertexProgramParameter4f   (GL2 gl, int param, float x, float y, float z, float w);
  public void setFragmentProgramParameter1f (GL2 gl, int param, float val);
  public void setFragmentProgramParameter3f (GL2 gl, int param, float x, float y, float z);
  public void setFragmentProgramParameter4f (GL2 gl, int param, float x, float y, float z, float w);
  public void trackModelViewProjectionMatrix(GL2 gl, int param);
  public void setMatrixParameterfc          (GL2 gl, int param, float[] matrix);
}
