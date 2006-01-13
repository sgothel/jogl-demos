package demos.hdr;

import java.io.*;
import java.util.*;

import javax.media.opengl.*;
import com.sun.opengl.cg.*;
import demos.util.*;

public class CgPipeline implements Pipeline {
  private CGcontext context;
  public void init() {
    context = CgGL.cgCreateContext();
  }

  public void initFloatingPointTexture(GL gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, textureObject);
    gl.glCopyTexImage2D(GL.GL_TEXTURE_RECTANGLE_NV, 0, GL.GL_FLOAT_RGBA16_NV, 0, 0, w, h, 0);
  }

  public void initTexture(GL gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, textureObject);
    gl.glCopyTexImage2D(GL.GL_TEXTURE_RECTANGLE_NV, 0, GL.GL_RGBA, 0, 0, w, h, 0);
  }

  public void copyToTexture(GL gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, textureObject);
    gl.glCopyTexSubImage2D(GL.GL_TEXTURE_RECTANGLE_NV, 0, 0, 0, 0, 0, w, h);
  }

  public void bindTexture(GL gl, int textureObject) {
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, textureObject);
  }

  private List programs = new ArrayList();
  public int loadVertexProgram(GL gl, String filename) throws IOException {
    return loadProgram(filename, CgGL.CG_PROFILE_ARBVP1);
  }

  public int loadFragmentProgram(GL gl, String filename) throws IOException {
    return loadProgram(filename, CgGL.CG_PROFILE_ARBFP1);
  }

  private int loadProgram(String fileName, int profile) throws IOException {
    CGprogram prog = CgGL.cgCreateProgramFromFile(context, CgGL.CG_SOURCE, fileName, profile, null, null);
    if (prog == null) {
      throw new RuntimeException("Error loading program");
    }
    CgGL.cgGLLoadProgram(prog);
    int res = programs.size();
    programs.add(prog);
    return res;
  }

  public void enableVertexProgram(GL gl, int program) {
    CgGL.cgGLBindProgram((CGprogram) programs.get(program));
    CgGL.cgGLEnableProfile(CgGL.CG_PROFILE_ARBVP1);
  }

  public void enableFragmentProgram(GL gl, int program) {
    CgGL.cgGLBindProgram((CGprogram) programs.get(program));
    CgGL.cgGLEnableProfile(CgGL.CG_PROFILE_ARBFP1);
  }

  public void disableVertexProgram(GL gl) {
    CgGL.cgGLDisableProfile(CgGL.CG_PROFILE_ARBVP1);
  }

  public void disableFragmentProgram(GL gl) {
    CgGL.cgGLDisableProfile(CgGL.CG_PROFILE_ARBFP1);
  }

  private List parameters = new ArrayList();
  public int getNamedParameter(int program, String name) {
    CGprogram prog = (CGprogram) programs.get(program);
    CGparameter param = CgGL.cgGetNamedParameter(prog, name);
    int res = parameters.size();
    parameters.add(param);
    return res;
  }

  public void setVertexProgramParameter1f(GL gl, int param, float val) {
    CgGL.cgGLSetParameter1f((CGparameter) parameters.get(param), val);
  }

  public void setVertexProgramParameter3f(GL gl, int param, float x, float y, float z) {
    CgGL.cgGLSetParameter3f((CGparameter) parameters.get(param), x, y, z);
  }

  public void setVertexProgramParameter4f(GL gl, int param, float x, float y, float z, float w) {
    CgGL.cgGLSetParameter4f((CGparameter) parameters.get(param), x, y, z, w);
  }

  public void setFragmentProgramParameter1f(GL gl, int param, float val) {
    CgGL.cgGLSetParameter1f((CGparameter) parameters.get(param), val);
  }

  public void setFragmentProgramParameter3f(GL gl, int param, float x, float y, float z) {
    CgGL.cgGLSetParameter3f((CGparameter) parameters.get(param), x, y, z);
  }

  public void setFragmentProgramParameter4f(GL gl, int param, float x, float y, float z, float w) {
    CgGL.cgGLSetParameter4f((CGparameter) parameters.get(param), x, y, z, w);
  }

  public void trackModelViewProjectionMatrix(GL gl, int param) {
    CgGL.cgGLSetStateMatrixParameter((CGparameter) parameters.get(param), CgGL.CG_GL_MODELVIEW_PROJECTION_MATRIX, CgGL.CG_GL_MATRIX_IDENTITY);
  }
  
  public void setMatrixParameterfc(GL gl, int param, float[] matrix) {
    CgGL.cgGLSetMatrixParameterfc((CGparameter) parameters.get(param), matrix, 0);
  }
}
