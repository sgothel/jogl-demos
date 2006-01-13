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
import java.util.*;

import com.sun.opengl.util.*;

/** Simple parser for Wavefront .OBJ files. Does not support all file
    options -- currently requires vertices and normals (only) to be
    present. */

public class ObjReader {
  private int verticesPerFace = -1;
  private FloatBuffer vertices;
  private FloatBuffer normals;
  private float[] aabbMin = new float[3];
  private float[] aabbMax = new float[3];
  private float[] center = new float[3];
  private float radius;
  // If we wanted this to be really general we'd have an array of
  // FloatLists for the various kinds of vertices as well
  private FloatList tmpVertices;
  private FloatList tmpVertexNormals;
  private IntList   faceIndices;
  private IntList[] tmpFaceIndices;

  public ObjReader(String filename) throws IOException {
    this(new File(filename));
  }

  public ObjReader(InputStream in) throws IOException {
    this(new InputStreamReader(in));
  }

  public ObjReader(File file) throws IOException {
    this (new FileReader(file));
  }

  public ObjReader(Reader r) throws IOException {
    BufferedReader reader = new BufferedReader(r);
    String line = null;
    int lineNo = 0;
    float[] floatTmp = new float[3];

    while ((line = reader.readLine()) != null) {
      ++lineNo;
      if (line.length() > 0) {
        char c = line.charAt(0);
        // FIXME: support continuation of lines with trailing '\'
        switch (c) {
          case '#':
            break;

          case 'v':
            if (Character.isWhitespace(line.charAt(1))) {
              addVertex(parseFloats(line, 3, floatTmp, lineNo));
            } else if (line.startsWith("vn")) {
              addVertexNormal(parseFloats(line, 3, floatTmp, lineNo));
            } else {
              throw new IOException("Unsupported vertex command on line " + lineNo);
            }
            break;
            
          case 'f':
            parseIndices(line, lineNo);

          default:
            // For now we ignore all other lines
        }
      }
    }

    // Now have all vertex information.
    // Make it possible to use same indices for both vertices and normals
    condenseIndices();

    // Compute axis-aligned bounding box and radius
    computeBoundingBox();
  }

  public void rescale(float amount) {
    for (int i = 0; i < vertices.capacity(); i++) {
      vertices.put(i, vertices.get(i) * amount);
    }
  }

  public FloatBuffer getVertices() {
    return vertices;
  }

  public FloatBuffer getVertexNormals() {
    return normals;
  }

  public int[] getFaceIndices() {
    return faceIndices.getData();
  }
  
  public int getVerticesPerFace() {
    return verticesPerFace;
  }

  public float[] getAABBMin() {
    return aabbMin;
  }

  public float[] getAABBMax() {
    return aabbMax;
  }

  public float[] getCenter() {
    return center;
  }

  public float getRadius() {
    return radius;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void addVertex(float[] tmp) {
    if (tmpVertices == null) {
      tmpVertices = new FloatList();
    }
    for (int i = 0; i < 3; i++) {
      tmpVertices.add(tmp[i]);
    }
  }

  private void addVertexNormal(float[] tmp) {
    if (tmpVertexNormals == null) {
      tmpVertexNormals = new FloatList();
    }
    for (int i = 0; i < 3; i++) {
      tmpVertexNormals.add(tmp[i]);
    }
  }

  private float[] parseFloats(String line, int num, float[] tmp, int lineNo) throws IOException {
    StringTokenizer tok = new StringTokenizer(line);
    tok.nextToken(); // skip command
    int idx = 0;
    while (tok.hasMoreTokens()) {
      if (idx >= tmp.length) {
        throw new IOException("Too many floating-point values on line " + lineNo);
      }
      tmp[idx++] = Float.parseFloat(tok.nextToken());
    }
    return tmp;
  }

  private void parseIndices(String line, int lineNo) throws IOException {
    StringTokenizer tok = new StringTokenizer(line);
    tok.nextToken(); // skip command
    List tokens = new ArrayList();
    while (tok.hasMoreTokens()) {
      tokens.add(tok.nextToken());
    }
    // This is the number of vertices in this face.
    // If we seem to have already found this, it had better match the
    // previously read value (for now - don't want to add the
    // complexity of supporting some faces with a certain number of
    // vertices and some with a different number)
    if (verticesPerFace < 0) {
      verticesPerFace = tokens.size();
    } else {
      if (verticesPerFace != tokens.size()) {
        throw new IOException("Face on line " + lineNo + " had " + tokens.size() +
                              " vertices, but had already previously set the number of vertices per face to " +
                              verticesPerFace);
      }
    }
    // Now read the individual indices out of each token
    for (Iterator iter = tokens.iterator(); iter.hasNext(); ) {
      String indices = (String) iter.next();
      if (tmpFaceIndices == null) {
        StringTokenizer tmpTok = new StringTokenizer(indices, "/");
        int numIndicesPerVertex = 0;
        while (tmpTok.hasMoreTokens()) {
          tmpTok.nextToken();
          ++numIndicesPerVertex;
        }
        tmpFaceIndices = new IntList[numIndicesPerVertex];
        for (int i = 0; i < numIndicesPerVertex; i++) {
          tmpFaceIndices[i] = new IntList();
        }
      }

      StringTokenizer tok2 = new StringTokenizer(indices, "/");
      int which = 0;
      while (tok2.hasMoreTokens()) {
        if (which >= tmpFaceIndices.length) {
          throw new IOException("Expected all vertices to have " + tmpFaceIndices.length +
                                " indices based on earlier input, but saw vertex with more on line " + lineNo);
        }
        String token = tok2.nextToken();
        int index = Integer.parseInt(token);
        tmpFaceIndices[which].add(index);
        ++which;
      }
    }
  }

  // Don't know the hashing rules for arrays off the top of my head
  static class Indices {
    int[] data;
    Indices(int[] data) {
      this.data = data;
    }

    public boolean equals(Object obj) {
      if ((obj == null) || (!(obj instanceof Indices))) {
        return false;
      }

      Indices other = (Indices) obj;

      if (data.length != other.data.length) {
        return false;
      }

      for (int i = 0; i < data.length; i++) {
        if (data[i] != other.data[i]) {
          return false;
        }
      }
      
      return true;
    }

    public int hashCode() {
      int hash = 0;
      for (int i = 0; i < data.length; i++) {
        hash ^= data[i];
      }
      return hash;
    }
  }

  private void condenseIndices() {
    FloatList newVertices = new FloatList();
    FloatList newVertexNormals = new FloatList();
    IntList   newIndices = new IntList();
    int nextIndex = 0;
    HashMap condensingMap = new HashMap();
    for (int i = 0; i < tmpFaceIndices[0].size(); i++) {
      Indices indices = getIndices(i);
      Integer newIndex = (Integer) condensingMap.get(indices);
      if (newIndex == null) {
        // Fabricate new vertex and normal index for this one
        // FIXME: generalize this by putting vertices and vertex
        // normals in FloatList[] as well
        condensingMap.put(indices, new Integer(nextIndex));
        int vtxIdx    = 3 * (indices.data[0] - 1);
        int vtxNrmIdx = 3 * (indices.data[1] - 1);
        newVertices.add(tmpVertices.get(vtxIdx + 0));
        newVertices.add(tmpVertices.get(vtxIdx + 1));
        newVertices.add(tmpVertices.get(vtxIdx + 2));
        newVertexNormals.add(tmpVertexNormals.get(vtxNrmIdx + 0));
        newVertexNormals.add(tmpVertexNormals.get(vtxNrmIdx + 1));
        newVertexNormals.add(tmpVertexNormals.get(vtxNrmIdx + 2));
        newIndices.add(nextIndex);
        ++nextIndex;
      } else {
        newIndices.add(newIndex.intValue());
      }
    }
    newVertices.trim();
    newVertexNormals.trim();
    newIndices.trim();
    vertices = BufferUtil.newFloatBuffer(newVertices.size());
    vertices.put(newVertices.getData());
    vertices.rewind();
    normals = BufferUtil.newFloatBuffer(newVertexNormals.size());
    normals.put(newVertexNormals.getData());
    normals.rewind();
    faceIndices = newIndices;
    tmpVertices = null;
    tmpVertexNormals = null;
  }

  private void computeBoundingBox() {
    for (int i = 0; i < vertices.capacity(); i += 3) {
      if (i == 0) {
        aabbMin[0] = vertices.get(i + 0);
        aabbMin[1] = vertices.get(i + 1);
        aabbMin[2] = vertices.get(i + 2);
        aabbMax[0] = vertices.get(i + 0);
        aabbMax[1] = vertices.get(i + 1);
        aabbMax[2] = vertices.get(i + 2);
      } else {
        aabbMin[0] = Math.min(aabbMin[0], vertices.get(i + 0));
        aabbMin[1] = Math.min(aabbMin[1], vertices.get(i + 1));
        aabbMin[2] = Math.min(aabbMin[2], vertices.get(i + 2));
        aabbMax[0] = Math.max(aabbMax[0], vertices.get(i + 0));
        aabbMax[1] = Math.max(aabbMax[1], vertices.get(i + 1));
        aabbMax[2] = Math.max(aabbMax[2], vertices.get(i + 2));
      }
    }
    center[0] = 0.5f * (aabbMin[0] + aabbMax[0]);
    center[1] = 0.5f * (aabbMin[1] + aabbMax[1]);
    center[2] = 0.5f * (aabbMin[2] + aabbMax[2]);
    radius = (float) Math.sqrt((aabbMax[0] - center[0]) * (aabbMax[0] - center[0]) +
                               (aabbMax[1] - center[1]) * (aabbMax[1] - center[1]) +
                               (aabbMax[2] - center[2]) * (aabbMax[2] - center[2]));
  }

  private Indices getIndices(int index) {
    int[] indices = new int[tmpFaceIndices.length];
    for (int i = 0; i < tmpFaceIndices.length; i++) {
      indices[i] = tmpFaceIndices[i].get(index);
    }
    return new Indices(indices);
  }
}
