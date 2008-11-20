package demos.hdr;

import demos.util.FileUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;


public class ARBFPPipeline implements Pipeline {

  private int textureFormat;

  public ARBFPPipeline(int textureFormat) {
    this.textureFormat = textureFormat;
  }

  public void init() {
  }

  public void initFloatingPointTexture(GL2 gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE, textureObject);
    gl.glCopyTexImage2D(GL2.GL_TEXTURE_RECTANGLE, 0, textureFormat, 0, 0, w, h, 0);
  }

  public void initTexture(GL2 gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE, textureObject);
    gl.glCopyTexImage2D(GL2.GL_TEXTURE_RECTANGLE, 0, GL2.GL_RGBA, 0, 0, w, h, 0);
  }

  public void copyToTexture(GL2 gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE, textureObject);
    gl.glCopyTexSubImage2D(GL2.GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, w, h);
  }

  public void bindTexture(GL2 gl, int textureObject) {
    gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE, textureObject);
  }

  private List programs = new ArrayList();
  public int loadVertexProgram(GL2 gl, String filename) throws IOException {
    return loadProgram(gl, filename, GL2.GL_VERTEX_PROGRAM);
  }

  public int loadFragmentProgram(GL2 gl, String filename) throws IOException {
    return loadProgram(gl, filename, GL2.GL_FRAGMENT_PROGRAM);
  }

  private int loadProgram(GL2 gl, String fileName, int profile) throws IOException {
    String programBuffer = FileUtils.loadStreamIntoString(getClass().getClassLoader().getResourceAsStream(fileName));
    int[] tmpInt = new int[1];
    gl.glGenPrograms(1, tmpInt, 0);
    int res = tmpInt[0];
    gl.glBindProgram(profile, res);
    gl.glProgramString(profile, GL2.GL_PROGRAM_FORMAT_ASCII, programBuffer.length(), programBuffer);
    int[] errPos = new int[1];
    gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION, errPos, 0);
    if (errPos[0] >= 0) {
      String kind = "Program";
      if (profile == GL2.GL_VERTEX_PROGRAM) {
        kind = "Vertex program";
      } else if (profile == GL2.GL_FRAGMENT_PROGRAM) {
        kind = "Fragment program";
      }
      System.out.println(kind + " failed to load:");
      String errMsg = gl.glGetString(GL2.GL_PROGRAM_ERROR_STRING);
      if (errMsg == null) {
        System.out.println("[No error message available]");
      } else {
        System.out.println("Error message: \"" + errMsg + "\"");
      }
      System.out.println("Error occurred at position " + errPos[0] + " in program:");
      int endPos = errPos[0];
      while (endPos < programBuffer.length() && programBuffer.charAt(endPos) != '\n') {
        ++endPos;
      }
      System.out.println(programBuffer.substring(errPos[0], endPos));
      throw new GLException("Error loading " + kind);
    } else {
      if (profile == GL2.GL_FRAGMENT_PROGRAM) {
        int[] isNative = new int[1];
        gl.glGetProgramiv(GL2.GL_FRAGMENT_PROGRAM,
                             GL2.GL_PROGRAM_UNDER_NATIVE_LIMITS,
                             isNative, 0);
        if (isNative[0] != 1) {
          System.out.println("WARNING: fragment program is over native resource limits");
          Thread.dumpStack();
        }
      }
    }
    return res;
  }

  public void enableVertexProgram(GL2 gl, int program) {
    gl.glBindProgram(GL2.GL_VERTEX_PROGRAM, program);
    gl.glEnable(GL2.GL_VERTEX_PROGRAM);
  }

  public void enableFragmentProgram(GL2 gl, int program) {
    gl.glBindProgram(GL2.GL_FRAGMENT_PROGRAM, program);
    gl.glEnable(GL2.GL_FRAGMENT_PROGRAM);
  }

  public void disableVertexProgram(GL2 gl) {
    gl.glDisable(GL2.GL_VERTEX_PROGRAM);
  }

  public void disableFragmentProgram(GL2 gl) {
    gl.glDisable(GL2.GL_FRAGMENT_PROGRAM);
  }

  public int getNamedParameter(int program, String name) {
    throw new RuntimeException("Not supported");
  }

  public void setVertexProgramParameter1f(GL2 gl, int param, float val) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_VERTEX_PROGRAM, param, val, 0, 0, 0);
  }

  public void setVertexProgramParameter3f(GL2 gl, int param, float x, float y, float z) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_VERTEX_PROGRAM, param, x, y, z, 0);
  }

  public void setVertexProgramParameter4f(GL2 gl, int param, float x, float y, float z, float w) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_VERTEX_PROGRAM, param, x, y, z, w);
  }

  public void setFragmentProgramParameter1f(GL2 gl, int param, float val) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_FRAGMENT_PROGRAM, param, val, 0, 0, 0);
  }

  public void setFragmentProgramParameter3f(GL2 gl, int param, float x, float y, float z) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_FRAGMENT_PROGRAM, param, x, y, z, 0);
  }

  public void setFragmentProgramParameter4f(GL2 gl, int param, float x, float y, float z, float w) {
    if (param < 0) return;
    gl.glProgramLocalParameter4f(GL2.GL_FRAGMENT_PROGRAM, param, x, y, z, w);
  }

  public void trackModelViewProjectionMatrix(GL2 gl, int param) {
    float[] modelView  = new float[16];
    float[] projection = new float[16];
    float[] mvp        = new float[16];

    // Get matrices
    gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection, 0);
    gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelView, 0);
    // Multiply together
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        float sum = 0;
        for (int k = 0; k < 4; k++) {
          sum += modelView[4 * i + k] * projection[4 * k + j];
        }
        mvp[4 * i + j] = sum;
      }
    }

    setMatrixParameterfc(gl, param, mvp);
  }
  
  public void setMatrixParameterfc(GL2 gl, int param, float[] matrix) {
    // Correct for row-major vs. column-major differences
    for (int i = 0; i < 4; i++) {
      gl.glProgramLocalParameter4f(GL2.GL_VERTEX_PROGRAM, param + i, matrix[i],  matrix[4+i],  matrix[8+i],  matrix[12+i]);
    }
  }
}
