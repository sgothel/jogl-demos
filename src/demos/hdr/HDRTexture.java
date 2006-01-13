package demos.hdr;

import java.io.*;
import java.nio.*;

import javax.media.opengl.*;

public class HDRTexture {
  private RGBE.Header header;
  private byte[] m_data;
  private float[] m_floatdata;
  private int m_width, m_height;
  private float m_max_r, m_max_g, m_max_b;
  private float m_min_r, m_min_g, m_min_b;
  private float m_max;
  private int m_target;

  public HDRTexture(String filename) throws IOException {
    this(new FileInputStream(filename));
  }

  public HDRTexture(InputStream in) throws IOException {
    DataInputStream datain = new DataInputStream(new BufferedInputStream(in));
    header = RGBE.readHeader(datain);
    m_width = header.getWidth();
    m_height = header.getHeight();
    m_data = new byte[m_width * m_height * 4];
    RGBE.readPixelsRawRLE(datain, m_data, 0, m_width, m_height);
    System.err.println("Loaded HDR image " + m_width + " x " + m_height);
  }

  public byte[] getData() { return m_data; }
  public int    getPixelIndex(int x, int y) {
    return ((m_width * (m_height - 1 - y)) + x) * 4;
  }
  public float[] getFloatData() { return m_floatdata; }
  public int getPixelFloatIndex(int x, int y) {
    return ((m_width * (m_height - 1 - y)) + x) * 3;
  }

  public void analyze() {
    m_max_r = m_max_g = m_max_b = 0.0f;
    m_min_r = m_min_g = m_min_b = 1e10f;
    int mine = 255;
    int maxe = 0;

    int ptr = 0;
    float[] rgb = new float[3];
    for(int i=0; i<m_width*m_height; i++) {
      int e = m_data[ptr+3] & 0xFF;
      if (e < mine) mine = e;
      if (e > maxe) maxe = e;

      RGBE.rgbe2float(rgb, m_data, ptr);
      float r = rgb[0];
      float g = rgb[1];
      float b = rgb[2];
      if (r > m_max_r) m_max_r = r;
      if (g > m_max_g) m_max_g = g;
      if (b > m_max_b) m_max_b = b;
      if (r < m_min_r) m_min_r = r;
      if (g < m_min_g) m_min_g = g;
      if (b < m_min_b) m_min_b = b;
      ptr += 4;
    }
    System.err.println("max intensity: " + m_max_r + " " + m_max_g + " " + m_max_b);
    System.err.println("min intensity: " + m_min_r + " " + m_min_g + " " + m_min_b);
    System.err.println("max e: " + maxe + " = " + RGBE.ldexp(1.0, maxe-128));
    System.err.println("min e: " + mine + " = " + RGBE.ldexp(1.0, mine-128));

    m_max = m_max_r;
    if (m_max_g > m_max) m_max = m_max_g;
    if (m_max_b > m_max) m_max = m_max_b;
    System.err.println("max: " + m_max);
  }

  /** Converts from RGBE to floating-point RGB data. */
  public void convert() {
    m_floatdata = new float [m_width*m_height*3];

    int src = 0;
    int dest = 0;
    float[] rgb = new float[3];
    for(int i=0; i<m_width*m_height; i++) {
      RGBE.rgbe2float(rgb, m_data, src);

      m_floatdata[dest++] = remap(rgb[0], m_max);
      m_floatdata[dest++] = remap(rgb[1], m_max);
      m_floatdata[dest++] = remap(rgb[2], m_max);

      src += 4;
    }
  }

  public int create2DTextureRGBE(GL gl, int targetTextureType) {
    m_target = targetTextureType;
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[1];

    gl.glBindTexture(m_target, texid);

    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(m_target, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);
    gl.glTexImage2D(m_target, 0, GL.GL_RGBA, m_width, m_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(m_data));

    return texid;
  }

  public int create2DTextureHILO(GL gl, int targetTextureType, boolean rg) {
    m_target = targetTextureType;
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];

    gl.glBindTexture(m_target, texid);

    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(m_target, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);

    float[] img = new float [m_width * m_height * 2];
    int src = 0;
    int dest = 0;
    for (int j=0; j<m_height; j++) {
      for (int i=0; i<m_width; i++) {
        if (rg) {
          img[dest++] = m_floatdata[src + 0];
          img[dest++] = m_floatdata[src + 1];
        } else {
          img[dest++] = m_floatdata[src + 2];
          img[dest++] = 0;
        }
        src+=3;
      }
    }

    gl.glTexImage2D(m_target, 0, GL.GL_HILO16_NV, m_width, m_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(img));

    return texid;
  }

  // create a cubemap texture from a 2D image in cross format (thanks to Jonathon McGee)
  public int createCubemapRGBE(GL gl) {
    // cross is 3 faces wide, 4 faces high
    int face_width = m_width / 3;
    int face_height = m_height / 4;
    byte[] face = new byte[face_width * face_height * 4];

    m_target = GL.GL_TEXTURE_CUBE_MAP;
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];
    gl.glBindTexture(m_target, texid);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(m_target, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);

    // gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    // gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    // extract 6 faces

    // positive Y
    int ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(2 * face_width - (i + 1), 3 * face_height + j);
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));
  
    // positive X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(i, m_height - (face_height + j + 1));
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));

    // negative Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(face_width + i, m_height - (face_height + j + 1));
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));

    // negative X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(2 * face_width + i, m_height - (face_height + j + 1));
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));

    // negative Y
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(2 * face_width - (i + 1), face_height + j);
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));

    // positive Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelIndex(2 * face_width - (i + 1), j);
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
        face[ptr++] = m_data[src++];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, GL.GL_RGBA, face_width, face_height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(face));

    return texid;
  }

  public int createCubemapHILO(GL gl, boolean rg) {
    // cross is 3 faces wide, 4 faces high
    int face_width = m_width / 3;
    int face_height = m_height / 4;
    float[] face = new float [face_width * face_height * 2];

    m_target = GL.GL_TEXTURE_CUBE_MAP;
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];
    gl.glBindTexture(m_target, texid);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(m_target, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);

    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    // extract 6 faces

    // positive Y
    int ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), 3 * face_height + j);
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));
  
    // positive X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(i, m_height - (face_height + j + 1));
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(face_width + i, m_height - (face_height + j + 1));
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width + i, m_height - (face_height + j + 1));
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative Y
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), face_height + j);
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // positive Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), j);
        if (rg) {
          face[ptr++] = m_floatdata[src + 0];
          face[ptr++] = m_floatdata[src + 1];
        } else {
          face[ptr++] = m_floatdata[src + 2];
          face[ptr++] = 0;
        }
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, GL.GL_HILO16_NV, face_width, face_height, 0, GL.GL_HILO_NV, GL.GL_FLOAT, FloatBuffer.wrap(face));

    return texid;
  }

  public int createCubemap(GL gl, int format) {
    // cross is 3 faces wide, 4 faces high
    int face_width = m_width / 3;
    int face_height = m_height / 4;
    float[] face = new float [face_width * face_height * 3];

    m_target = GL.GL_TEXTURE_CUBE_MAP;
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    int texid = tmp[0];
    gl.glBindTexture(m_target, texid);

    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameteri(m_target, GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);

    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    gl.glTexParameteri(m_target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    // extract 6 faces

    // positive Y
    int ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), 3 * face_height + j);
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));
  
    // positive X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(i, m_height - (face_height + j + 1));
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(face_width + i, m_height - (face_height + j + 1));
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative X
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width + i, m_height - (face_height + j + 1));
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // negative Y
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), face_height + j);
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));

    // positive Z
    ptr = 0;
    for (int j=0; j<face_height; j++) {
      for (int i=0; i<face_width; i++) {
        int src = getPixelFloatIndex(2 * face_width - (i + 1), j);
        face[ptr++] = m_floatdata[src + 0];
        face[ptr++] = m_floatdata[src + 1];
        face[ptr++] = m_floatdata[src + 2];
      }
    }
    gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, format, face_width, face_height, 0, GL.GL_RGB, GL.GL_FLOAT, FloatBuffer.wrap(face));

    return texid;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static float remap(float x, float max) {
    if (x > max) x = max;
    return (float) Math.sqrt(x / max);
  }

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        HDRTexture tex = new HDRTexture(args[i]);
        tex.analyze();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
