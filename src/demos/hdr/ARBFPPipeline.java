package demos.hdr;

import java.io.*;
import java.util.*;

import javax.media.opengl.*;
import demos.util.*;

public class ARBFPPipeline implements Pipeline {
  private int textureFormat;
  public ARBFPPipeline(int textureFormat) {
    this.textureFormat = textureFormat;
  }

  public void init() {
  }

  public void initFloatingPointTexture(GL gl, int textureObject, int w, int h) {
    gl.glBindTexture(GL.GL_TEXTURE_RECTANGLE_NV, textureObject);
    gl.glCopyTexImage2D(GL.GL_TEXTURE_RECTANGLE_NV, 0, textureFormat, 0, 0, w, h, 0);
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
    return loadProgram(gl, filename, GL.GL_VERTEX_PROGRAM_ARB);
  }

  public int loadFragmentProgram(GL gl, String filename) throws IOException {
    return loadProgram(gl, filename, GL.GL_FRAGMENT_PROGRAM_ARB);
  }

  private int loadProgram(GL gl, String fileName, int profile) throws IOException {
    String programBuffer = FileUtils.loadStreamIntoString(getClass().getClassLoader().getResourceAsStream(fileName));
    int[] tmpInt = new int[1];
    gl.glGenProgramsARB(1, tmpInt, 0);
    int res = tmpInt[0];
    gl.glBindProgramARB(profile, res);
    gl.glProgramStringARB(profile, GL.GL_PROGRAM_FORMAT_ASCII_ARB, programBuffer.length(), programBuffer);
    int[] errPos = new int[1];
    gl.glGetIntegerv(GL.GL_PROGRAM_ERROR_POSITION_ARB, errPos, 0);
    if (errPos[0] >= 0) {
      String kind = "Program";
      if (profile == GL.GL_VERTEX_PROGRAM_ARB) {
        kind = "Vertex program";
      } else if (profile == GL.GL_FRAGMENT_PROGRAM_ARB) {
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
      while (endPos < programBuffer.length() && programBuffer.charAt(endPos) != '\n') {
        ++endPos;
      }
      System.out.println(programBuffer.substring(errPos[0], endPos));
      throw new GLException("Error loading " + kind);
    } else {
      if (profile == GL.GL_FRAGMENT_PROGRAM_ARB) {
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
    return res;
  }

  public void enableVertexProgram(GL gl, int program) {
    gl.glBindProgramARB(GL.GL_VERTEX_PROGRAM_ARB, program);
    gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
  }

  public void enableFragmentProgram(GL gl, int program) {
    gl.glBindProgramARB(GL.GL_FRAGMENT_PROGRAM_ARB, program);
    gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);
  }

  public void disableVertexProgram(GL gl) {
    gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
  }

  public void disableFragmentProgram(GL gl) {
    gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
  }

  public int getNamedParameter(int program, String name) {
    throw new RuntimeException("Not supported");
  }

  public void setVertexProgramParameter1f(GL gl, int param, float val) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, param, val, 0, 0, 0);
  }

  public void setVertexProgramParameter3f(GL gl, int param, float x, float y, float z) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, param, x, y, z, 0);
  }

  public void setVertexProgramParameter4f(GL gl, int param, float x, float y, float z, float w) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, param, x, y, z, w);
  }

  public void setFragmentProgramParameter1f(GL gl, int param, float val) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_FRAGMENT_PROGRAM_ARB, param, val, 0, 0, 0);
  }

  public void setFragmentProgramParameter3f(GL gl, int param, float x, float y, float z) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_FRAGMENT_PROGRAM_ARB, param, x, y, z, 0);
  }

  public void setFragmentProgramParameter4f(GL gl, int param, float x, float y, float z, float w) {
    if (param < 0) return;
    gl.glProgramLocalParameter4fARB(GL.GL_FRAGMENT_PROGRAM_ARB, param, x, y, z, w);
  }

  public void trackModelViewProjectionMatrix(GL gl, int param) {
    float[] modelView  = new float[16];
    float[] projection = new float[16];
    float[] mvp        = new float[16];

    // Get matrices
    gl.glGetFloatv(GL.GL_PROJECTION_MATRIX, projection, 0);
    gl.glGetFloatv(GL.GL_MODELVIEW_MATRIX, modelView, 0);
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
  
  public void setMatrixParameterfc(GL gl, int param, float[] matrix) {
    // Correct for row-major vs. column-major differences
    for (int i = 0; i < 4; i++) {
      gl.glProgramLocalParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, param + i, matrix[i],  matrix[4+i],  matrix[8+i],  matrix[12+i]);
    }
  }
}
