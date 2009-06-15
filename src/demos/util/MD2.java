/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package demos.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/** Reader for MD2 models, used by Quake II. */

public class MD2 {
  public static Model loadMD2(String filename) throws IOException {
    List/*<IFrame>*/ ifr = new ArrayList/*<IFrame>*/();
    loadFrames(filename, ifr);
    return computeModel(ifr);
  }

  public static Model loadMD2(InputStream in) throws IOException {
    List/*<IFrame>*/ ifr = new ArrayList/*<IFrame>*/();
    loadFrames(in, ifr);
    return computeModel(ifr);
  }

  public static class FileHeader {
    public int ident;
    public int version;
    public int skinwidth;
    public int skinheight;
    public int framesize;     // byte size of each frame
    public int num_skins;
    public int num_xyz;
    public int num_st;        // greater than num_xyz for seams
    public int num_tris;
    public int num_glcmds;    // dwords in strip/fan command list
    public int num_frames;
    public int ofs_skins;     // each skin is a MAX_SKINNAME string
    public int ofs_st;        // byte offset from start for stverts
    public int ofs_tris;      // offset for dtriangles
    public int ofs_frames;    // offset for first frame
    public int ofs_glcmds;
    public int ofs_end;       // end of file
  };

  public static class FileCompressedVertex {
    public byte[] v = new byte[3]; // scaled byte to fit in frame mins/maxs
    public byte lightnormalindex;
  }
  
  public static class FileFrame {
    public float[] scale     = new float[3];           // multiply byte verts by this
    public float[] translate = new float[3];           // then add this
    public String name;                                // frame name from grabbing
    public FileCompressedVertex[] verts;               // variable sized
  }
  
  public static class FileModel {
    public int[] glcmds;
    public FileFrame[] frames;
  }

  public static class PositionNormal implements Cloneable {
    public float x, y, z;
    public float nx, ny, nz;
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class TexCoord {
    public float s,t;
  }

  public static class Vertex {
    public int pn_index;
    public TexCoord tc = new TexCoord();
  }

  public static class Triangle {
    public Vertex[] v = new Vertex[3];
    public boolean kill;
  }

  public static class WingedEdge {
    public int[] e = new int[2];  // vertex index
    public int[] w = new int[2];  // triangle index: for "open" models, w[1] == -1 on open edges
  }

  public static class Plane implements Cloneable {
    public float a,b,c,d;
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class Frame implements Cloneable {
    public PositionNormal[] pn;  // [pn_index]
    public Plane[] triplane;     // [tri_num]

    public Object clone() {
      Frame res = new Frame();
      res.pn = new PositionNormal[pn.length];
      for (int i = 0; i < pn.length; i++) {
        res.pn[i] = (PositionNormal) pn[i].clone();
      }
      res.triplane = new Plane[triplane.length];
      for (int i = 0; i < triplane.length; i++) {
        res.triplane[i] = (Plane) triplane[i].clone();
      }
      return res;
    }
  }

  public static class Model {
    public Frame[] f;
    public Triangle[] tri;                   // [tri_num]
    public WingedEdge[] edge;                // [edge_num]
  }

  public static void computePlane(PositionNormal a, PositionNormal b, PositionNormal c, Plane p) {
    float[] v0 = new float[3];
    v0[0] = b.x - a.x;
    v0[1] = b.y - a.y;
    v0[2] = b.z - a.z;
    float[] v1 = new float[3];
    v1[0] = c.x - a.x;
    v1[1] = c.y - a.y;
    v1[2] = c.z - a.z;
    float[] cr = new float[3];
    cr[0] = v0[1] * v1[2] - v0[2] * v1[1];
    cr[1] = v0[2] * v1[0] - v0[0] * v1[2];
    cr[2] = v0[0] * v1[1] - v0[1] * v1[0];
    float l = (float) Math.sqrt(cr[0] * cr[0] + cr[1] * cr[1] + cr[2] * cr[2]);
    if (l == 0) {
      // degenerate triangle
      p.a = p.b = p.c = p.d = 0;
      return;
    }
    p.a = cr[0] / l;
    p.b = cr[1] / l;
    p.c = cr[2] / l;

    p.d = -(p.a * a.x + p.b * a.y + p.c * a.z);  // signed distance of a point on the plane from the origin
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static Model computeModel(List/*<IFrame>*/ ifr) throws IOException {
    if (!compareFrames(ifr)) {
      throw new IOException("unsuitable model -- frames aren't same");
    }
    Model m = new Model();
    m.tri = ((IFrame) ifr.get(0)).tri;
    m.f = new Frame[ifr.size()];
    for (int i = 0; i < ifr.size(); i++) {
      Frame f = new Frame();
      m.f[i] = f;
      IFrame it = (IFrame) ifr.get(i);
      f.pn = it.pn;
      computeFramePlanes(m.tri, f);
    }
    computeWingedEdges(m);
    return m;
  }

  private static class IFrame {
    PositionNormal[] pn;
    Triangle[] tri;
  }

  // normal table lifted from Mark Kilgard's md2bump demo
  private static final float[] normalTable = new float[] {
    -0.525731f, 0.000000f, 0.850651f, 
    -0.442863f, 0.238856f, 0.864188f, 
    -0.295242f, 0.000000f, 0.955423f, 
    -0.309017f, 0.500000f, 0.809017f, 
    -0.162460f, 0.262866f, 0.951056f, 
    0.000000f, 0.000000f, 1.000000f, 
    0.000000f, 0.850651f, 0.525731f, 
    -0.147621f, 0.716567f, 0.681718f, 
    0.147621f, 0.716567f, 0.681718f, 
    0.000000f, 0.525731f, 0.850651f, 
    0.309017f, 0.500000f, 0.809017f, 
    0.525731f, 0.000000f, 0.850651f, 
    0.295242f, 0.000000f, 0.955423f, 
    0.442863f, 0.238856f, 0.864188f, 
    0.162460f, 0.262866f, 0.951056f, 
    -0.681718f, 0.147621f, 0.716567f, 
    -0.809017f, 0.309017f, 0.500000f, 
    -0.587785f, 0.425325f, 0.688191f, 
    -0.850651f, 0.525731f, 0.000000f, 
    -0.864188f, 0.442863f, 0.238856f, 
    -0.716567f, 0.681718f, 0.147621f, 
    -0.688191f, 0.587785f, 0.425325f, 
    -0.500000f, 0.809017f, 0.309017f, 
    -0.238856f, 0.864188f, 0.442863f, 
    -0.425325f, 0.688191f, 0.587785f, 
    -0.716567f, 0.681718f, -0.147621f, 
    -0.500000f, 0.809017f, -0.309017f, 
    -0.525731f, 0.850651f, 0.000000f, 
    0.000000f, 0.850651f, -0.525731f, 
    -0.238856f, 0.864188f, -0.442863f, 
    0.000000f, 0.955423f, -0.295242f, 
    -0.262866f, 0.951056f, -0.162460f, 
    0.000000f, 1.000000f, 0.000000f, 
    0.000000f, 0.955423f, 0.295242f, 
    -0.262866f, 0.951056f, 0.162460f, 
    0.238856f, 0.864188f, 0.442863f, 
    0.262866f, 0.951056f, 0.162460f, 
    0.500000f, 0.809017f, 0.309017f, 
    0.238856f, 0.864188f, -0.442863f, 
    0.262866f, 0.951056f, -0.162460f, 
    0.500000f, 0.809017f, -0.309017f, 
    0.850651f, 0.525731f, 0.000000f, 
    0.716567f, 0.681718f, 0.147621f, 
    0.716567f, 0.681718f, -0.147621f, 
    0.525731f, 0.850651f, 0.000000f, 
    0.425325f, 0.688191f, 0.587785f, 
    0.864188f, 0.442863f, 0.238856f, 
    0.688191f, 0.587785f, 0.425325f, 
    0.809017f, 0.309017f, 0.500000f, 
    0.681718f, 0.147621f, 0.716567f, 
    0.587785f, 0.425325f, 0.688191f, 
    0.955423f, 0.295242f, 0.000000f, 
    1.000000f, 0.000000f, 0.000000f, 
    0.951056f, 0.162460f, 0.262866f, 
    0.850651f, -0.525731f, 0.000000f, 
    0.955423f, -0.295242f, 0.000000f, 
    0.864188f, -0.442863f, 0.238856f, 
    0.951056f, -0.162460f, 0.262866f, 
    0.809017f, -0.309017f, 0.500000f, 
    0.681718f, -0.147621f, 0.716567f, 
    0.850651f, 0.000000f, 0.525731f, 
    0.864188f, 0.442863f, -0.238856f, 
    0.809017f, 0.309017f, -0.500000f, 
    0.951056f, 0.162460f, -0.262866f, 
    0.525731f, 0.000000f, -0.850651f, 
    0.681718f, 0.147621f, -0.716567f, 
    0.681718f, -0.147621f, -0.716567f, 
    0.850651f, 0.000000f, -0.525731f, 
    0.809017f, -0.309017f, -0.500000f, 
    0.864188f, -0.442863f, -0.238856f, 
    0.951056f, -0.162460f, -0.262866f, 
    0.147621f, 0.716567f, -0.681718f, 
    0.309017f, 0.500000f, -0.809017f, 
    0.425325f, 0.688191f, -0.587785f, 
    0.442863f, 0.238856f, -0.864188f, 
    0.587785f, 0.425325f, -0.688191f, 
    0.688191f, 0.587785f, -0.425325f, 
    -0.147621f, 0.716567f, -0.681718f, 
    -0.309017f, 0.500000f, -0.809017f, 
    0.000000f, 0.525731f, -0.850651f, 
    -0.525731f, 0.000000f, -0.850651f, 
    -0.442863f, 0.238856f, -0.864188f, 
    -0.295242f, 0.000000f, -0.955423f, 
    -0.162460f, 0.262866f, -0.951056f, 
    0.000000f, 0.000000f, -1.000000f, 
    0.295242f, 0.000000f, -0.955423f, 
    0.162460f, 0.262866f, -0.951056f, 
    -0.442863f, -0.238856f, -0.864188f, 
    -0.309017f, -0.500000f, -0.809017f, 
    -0.162460f, -0.262866f, -0.951056f, 
    0.000000f, -0.850651f, -0.525731f, 
    -0.147621f, -0.716567f, -0.681718f, 
    0.147621f, -0.716567f, -0.681718f, 
    0.000000f, -0.525731f, -0.850651f, 
    0.309017f, -0.500000f, -0.809017f, 
    0.442863f, -0.238856f, -0.864188f, 
    0.162460f, -0.262866f, -0.951056f, 
    0.238856f, -0.864188f, -0.442863f, 
    0.500000f, -0.809017f, -0.309017f, 
    0.425325f, -0.688191f, -0.587785f, 
    0.716567f, -0.681718f, -0.147621f, 
    0.688191f, -0.587785f, -0.425325f, 
    0.587785f, -0.425325f, -0.688191f, 
    0.000000f, -0.955423f, -0.295242f, 
    0.000000f, -1.000000f, 0.000000f, 
    0.262866f, -0.951056f, -0.162460f, 
    0.000000f, -0.850651f, 0.525731f, 
    0.000000f, -0.955423f, 0.295242f, 
    0.238856f, -0.864188f, 0.442863f, 
    0.262866f, -0.951056f, 0.162460f, 
    0.500000f, -0.809017f, 0.309017f, 
    0.716567f, -0.681718f, 0.147621f, 
    0.525731f, -0.850651f, 0.000000f, 
    -0.238856f, -0.864188f, -0.442863f, 
    -0.500000f, -0.809017f, -0.309017f, 
    -0.262866f, -0.951056f, -0.162460f, 
    -0.850651f, -0.525731f, 0.000000f, 
    -0.716567f, -0.681718f, -0.147621f, 
    -0.716567f, -0.681718f, 0.147621f, 
    -0.525731f, -0.850651f, 0.000000f, 
    -0.500000f, -0.809017f, 0.309017f, 
    -0.238856f, -0.864188f, 0.442863f, 
    -0.262866f, -0.951056f, 0.162460f, 
    -0.864188f, -0.442863f, 0.238856f, 
    -0.809017f, -0.309017f, 0.500000f, 
    -0.688191f, -0.587785f, 0.425325f, 
    -0.681718f, -0.147621f, 0.716567f, 
    -0.442863f, -0.238856f, 0.864188f, 
    -0.587785f, -0.425325f, 0.688191f, 
    -0.309017f, -0.500000f, 0.809017f, 
    -0.147621f, -0.716567f, 0.681718f, 
    -0.425325f, -0.688191f, 0.587785f, 
    -0.162460f, -0.262866f, 0.951056f, 
    0.442863f, -0.238856f, 0.864188f, 
    0.162460f, -0.262866f, 0.951056f, 
    0.309017f, -0.500000f, 0.809017f, 
    0.147621f, -0.716567f, 0.681718f, 
    0.000000f, -0.525731f, 0.850651f, 
    0.425325f, -0.688191f, 0.587785f, 
    0.587785f, -0.425325f, 0.688191f, 
    0.688191f, -0.587785f, 0.425325f, 
    -0.955423f, 0.295242f, 0.000000f, 
    -0.951056f, 0.162460f, 0.262866f, 
    -1.000000f, 0.000000f, 0.000000f, 
    -0.850651f, 0.000000f, 0.525731f, 
    -0.955423f, -0.295242f, 0.000000f, 
    -0.951056f, -0.162460f, 0.262866f, 
    -0.864188f, 0.442863f, -0.238856f, 
    -0.951056f, 0.162460f, -0.262866f, 
    -0.809017f, 0.309017f, -0.500000f, 
    -0.864188f, -0.442863f, -0.238856f, 
    -0.951056f, -0.162460f, -0.262866f, 
    -0.809017f, -0.309017f, -0.500000f, 
    -0.681718f, 0.147621f, -0.716567f, 
    -0.681718f, -0.147621f, -0.716567f, 
    -0.850651f, 0.000000f, -0.525731f, 
    -0.688191f, 0.587785f, -0.425325f, 
    -0.587785f, 0.425325f, -0.688191f, 
    -0.425325f, 0.688191f, -0.587785f, 
    -0.425325f, -0.688191f, -0.587785f, 
    -0.587785f, -0.425325f, -0.688191f, 
    -0.688191f, -0.587785f, -0.425325f
  };
  
  private static void loadFrames(String filename, List/*<IFrame>*/ md2p) throws IOException {
    FileModel mf = loadMD2File(filename);
    computeFrames(mf, md2p);
  }

  private static void loadFrames(InputStream in, List/*<IFrame>*/ md2p) throws IOException {
    FileModel mf = loadMD2File(in);
    computeFrames(mf, md2p);
  }

  private static void computeFrames(FileModel mf, List/*<IFrame>*/ md2p) throws IOException {
    for (int i = 0; i < mf.frames.length; i++) {
      IFrame f = new IFrame();
      md2p.add(f);
      FileFrame curframe = mf.frames[i];
      f.pn = new PositionNormal[curframe.verts.length];
      for (int j = 0; j < curframe.verts.length; j++) {
        PositionNormal pn = new PositionNormal();
        pn.x = (((curframe.verts[j].v[0] & 0xFF) * curframe.scale[0]) + curframe.translate[0]) * .025f;
        pn.y = (((curframe.verts[j].v[1] & 0xFF) * curframe.scale[1]) + curframe.translate[1]) * .025f;
        pn.z = (((curframe.verts[j].v[2] & 0xFF) * curframe.scale[2]) + curframe.translate[2]) * .025f;
        int normal_index = curframe.verts[j].lightnormalindex & 0xFF;
        pn.nx = normalTable[3 * normal_index + 0];
        pn.ny = normalTable[3 * normal_index + 1];
        pn.nz = normalTable[3 * normal_index + 2];
        f.pn[j] = pn;
      }

      List/*<Triangle>*/ tris = new ArrayList();
      int[] idx = new int[1];
      while (mf.glcmds[idx[0]] != 0) {
        int vertnum;
        boolean is_strip;
        if (mf.glcmds[idx[0]] > 0) {
          vertnum =  mf.glcmds[idx[0]++]; is_strip = true;  // triangle strip
        } else {
          vertnum = -mf.glcmds[idx[0]++]; is_strip = false; // triangle fan
        }

        if (is_strip) {
          Vertex[] prev = new Vertex[2];
          prev[0] = extractVertex(mf.glcmds, idx);
          prev[1] = extractVertex(mf.glcmds, idx);
          for (int j = 2; j < vertnum; j++) {
            Triangle tri = new Triangle();
            if ((j % 2) == 0) {
              tri.v[0] = prev[0];
              tri.v[1] = prev[1];
              tri.v[2] = extractVertex(mf.glcmds, idx);
              prev[0] = tri.v[2];
            } else {
              tri.v[0] = prev[1];
              tri.v[1] = extractVertex(mf.glcmds, idx);
              tri.v[2] = prev[0];
              prev[1] = tri.v[1];
            }
            // swap v[1] and v[2] to fix triangle winding
            Vertex hold = tri.v[1];
            tri.v[1] = tri.v[2];
            tri.v[2] = hold;
            tris.add(tri);
          }
        } else {
          // is fan
          Vertex ctr = extractVertex(mf.glcmds, idx);
          Vertex prev = extractVertex(mf.glcmds, idx);
          for (int j = 2; j < vertnum; j++) {
            Triangle tri = new Triangle();
            tri.v[0] = ctr;
            tri.v[1] = prev;
            tri.v[2] = extractVertex(mf.glcmds, idx);
            prev = tri.v[2];
            // swap v[1] and v[2] to fix triangle winding
            Vertex hold = tri.v[1];
            tri.v[1] = tri.v[2];
            tri.v[2] = hold;
            tris.add(tri);
          }
        }
      }
      f.tri = (Triangle[]) tris.toArray(new Triangle[0]);
    }
  }

  private static FileModel loadMD2File(ByteBuffer buf) throws IOException {
    buf.order(ByteOrder.LITTLE_ENDIAN);
    FileModel md2p = new FileModel();
    FileHeader header = readHeader(buf);
    buf.position(header.ofs_frames);
    readFrames(buf, header, md2p);
    buf.position(header.ofs_glcmds);
    readGLCommands(buf, header, md2p);
    return md2p;
  }

  private static FileModel loadMD2File(InputStream in) throws IOException {
    in = new BufferedInputStream(in);
    int avail = in.available();
    byte[] data = new byte[avail];
    int numRead = 0;
    int pos = 0;
    do {
      if (pos + avail > data.length) {
        byte[] newData = new byte[pos + avail];
        System.arraycopy(data, 0, newData, 0, pos);
        data = newData;
      }
      numRead = in.read(data, pos, avail);
      if (numRead >= 0) {
        pos += numRead;
      }
      avail = in.available();
    } while (avail > 0 && numRead >= 0);
    ByteBuffer buf = ByteBuffer.allocateDirect(pos);
    buf.put(data, 0, pos);
    buf.rewind();
    return loadMD2File(buf);
  }

  private static FileModel loadMD2File(String filename) throws IOException {
    FileInputStream fis = new FileInputStream(filename);
    FileChannel chan = fis.getChannel();
    ByteBuffer buf = chan.map(FileChannel.MapMode.READ_ONLY, 0, fis.available());
    FileModel md2p = loadMD2File(buf);
    chan.close();
    fis.close();
    return md2p;
  }

  private static FileHeader readHeader(ByteBuffer buf) {
    FileHeader header = new FileHeader();
    header.ident      = buf.getInt();
    header.version    = buf.getInt();
    header.skinwidth  = buf.getInt();
    header.skinheight = buf.getInt();
    header.framesize  = buf.getInt();
    header.num_skins  = buf.getInt();
    header.num_xyz    = buf.getInt();
    header.num_st     = buf.getInt();
    header.num_tris   = buf.getInt();
    header.num_glcmds = buf.getInt();
    header.num_frames = buf.getInt();
    header.ofs_skins  = buf.getInt();
    header.ofs_st     = buf.getInt();
    header.ofs_tris   = buf.getInt();
    header.ofs_frames = buf.getInt();
    header.ofs_glcmds = buf.getInt();
    header.ofs_end    = buf.getInt();
    return header;
  }

  private static int numVerts(int framesize) {
    return (framesize >> 2) - 10;
  }

  private static void readFrames(ByteBuffer buf, FileHeader header, FileModel md2p) throws IOException {
    int numframes = header.num_frames;
    int framesize = header.framesize;
    int numVerts = numVerts(framesize);
    FileFrame[] frames = new FileFrame[numframes];
    byte[] name = new byte[16];
    for (int i = 0; i < numframes; i++) {
      FileFrame frame = new FileFrame();
      frame.scale[0] = buf.getFloat();
      frame.scale[1] = buf.getFloat();
      frame.scale[2] = buf.getFloat();
      frame.translate[0] = buf.getFloat();
      frame.translate[1] = buf.getFloat();
      frame.translate[2] = buf.getFloat();
      buf.get(name);
      try {
        frame.name = new String(name, "US-ASCII");
      } catch (UnsupportedEncodingException e) {
        throw new IOException(e.toString());
      }
      frame.verts = new FileCompressedVertex[numVerts];
      for (int j = 0; j < numVerts; j++) {
        FileCompressedVertex vert = new FileCompressedVertex();
        buf.get(vert.v);
        vert.lightnormalindex = buf.get();
        frame.verts[j] = vert;
      }
      frames[i] = frame;
    }
    md2p.frames = frames;
  }

  private static void readGLCommands(ByteBuffer buf, FileHeader header, FileModel md2p) {
    int num_glcmds = header.num_glcmds;
    int[] glcmds = new int[num_glcmds];
    for (int i = 0; i < num_glcmds; i++) {
      glcmds[i] = buf.getInt();
    }
    md2p.glcmds = glcmds;
  }

  private static Vertex extractVertex(int[] glcmds, int[] idx) {
    Vertex v = new Vertex();
    v.tc.s = Float.intBitsToFloat(glcmds[idx[0]++]);
    v.tc.t = Float.intBitsToFloat(glcmds[idx[0]++]);
    v.pn_index = glcmds[idx[0]++];
    return v;
  }

  private static boolean compareFrames(List/*<IFrame>*/ m) {
    IFrame f0 = (IFrame) m.get(0);
    boolean same_topology  = true;
    boolean same_texcoords = true;
		
    for (int i = 1; i < m.size(); i++) {
      IFrame f = (IFrame) m.get(i);
      if (f.pn.length != f0.pn.length) {
        System.err.println("pn size different for iframe " + i + " :  " + f0.pn.length + " != " + f.pn.length);
        same_topology = false;
      }
      if (f.tri.length != f0.tri.length) {
        System.err.println("tri size different for iframe " + i + " :  " + f0.tri.length + " != " + f.tri.length);
        same_topology = false;
      }
      if (same_topology) {
        for (int j = 0; j < f.tri.length; j++) {
          Triangle t0 = f0.tri[j];
          Triangle t  = f.tri[j];
          for (int k = 0; k < 3; k++) {
            if (t0.v[k].pn_index != t.v[k].pn_index) {
              System.err.println("tri " + j + " triangle pn_index " + k + " different!");
              same_topology = false;
            }
            if (t0.v[k].tc.s != t.v[k].tc.s || t0.v[k].tc.t != t.v[k].tc.t) {
              System.err.println("tri " + j + " triangle tc " + k + " different!");
              same_texcoords = false;
            }
          }
        }
      }
    }

    return same_topology && same_texcoords;
  }

  /**
     Computes the plane equations for each polygon of a frame.
  */
  private static void computeFramePlanes(Triangle[] tri, Frame f) {
    f.triplane = new Plane[tri.length];
    for (int i = 0; i < tri.length; i++) {
      Triangle t = tri[i];
      int ia = t.v[0].pn_index;
      int ib = t.v[1].pn_index;
      int ic = t.v[2].pn_index;
      Plane p = new Plane();
      computePlane(f.pn[ia], f.pn[ib], f.pn[ic], p);
      f.triplane[i] = p;
    }
  }

  private static int computeWingedEdges(Model m) {
    Triangle[] tri = m.tri;
    List/*<WingedEdge>*/ edge = new ArrayList/*<WingedEdge>*/();

    // for each triangle, try to add each edge to the winged_edge vector,
    // but check first to see if it's already there
    int tsize = tri.length;
    for (int i = 0; i < tsize; i++) {
      Triangle t = tri[i];
      for (int j = 0; j < 3; j++) {
        WingedEdge we = new WingedEdge();
        we.e[0] = t.v[   j   ].pn_index;
        we.e[1] = t.v[(j+1)%3].pn_index;
        we.w[0] = i;
        we.w[1] = -1;  // subsequent attempt to add this edge will replace w[1] 
        addEdge(edge, we);
      }
    }
    int open_edge = 0;
    for (int i = 0; i < edge.size(); i++) {
      if (((WingedEdge) edge.get(i)).w[1] == -1)
        open_edge++;
    }
    //fprintf(stderr, "out of % edges, there were %d open edges\n", edge.size(), open_edge);
    m.edge = (WingedEdge[]) edge.toArray(new WingedEdge[0]);
    return open_edge;
  }

  /**
     add_edge will look to see if the current edge is already in the list.
     If it is not, it will add it. If it is, it will replace the w[1] in
     the existing table with w[0] from the edge being added.
  */
  private static void addEdge(List/*<WingedEdge>*/ edge, WingedEdge we) {
    int esize = edge.size();
    for (int i=0; i < esize; i++) {
      WingedEdge we0 = (WingedEdge) edge.get(i);
      if (we0.e[0] == we.e[0] && we0.e[1] == we.e[1]) {
        System.err.println("facingness different between polys on edge!");
      }
      if(we0.e[0] == we.e[1]  && we0.e[1] == we.e[0]) {
        if(we0.w[1] != -1) {
          System.err.println("triple edge! bad...");
        }
        we0.w[1] = we.w[0]; // pair the edge and return
        return;
      }
    }
    edge.add(we);  // otherwise, add the new edge
  }

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        MD2.Model model = loadMD2(args[i]);
        System.err.println("Successfully parsed " + args[i]);
      } catch (IOException e) {
        System.err.println("Error parsing " + args[i] + ":");
        e.printStackTrace();
      }
    }
  }
}
