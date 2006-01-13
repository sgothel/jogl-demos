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

import javax.media.opengl.*;

/** Renders a bunny.

   <P> This file was produced by 3D Exploration Plugin: CPP Export filter.

   <P> 3D Exploration

   <P> Copyright (c) 1999-2000 X Dimension Software

   <P>WWW         http://www.xdsoft.com/explorer/ <BR>
   eMail       info@xdsoft.com
*/
public class Bunny {

  /** Generates and returns a display list for the bunny model. */
  public static int gen3DObjectList(GL gl) throws IOException {
    StreamTokenizer tok = new StreamTokenizer(new BufferedReader(new InputStreamReader(
      Bunny.class.getClassLoader().getResourceAsStream("demos/data/models/bunny.txt"))));
    // Reset tokenizer's syntax so numbers are not parsed
    tok.resetSyntax();
    tok.wordChars('a', 'z');
    tok.wordChars('A', 'Z');
    tok.wordChars('0', '9');
    tok.wordChars('-', '-');
    tok.wordChars('.', '.');
    tok.wordChars(128 + 32, 255);
    tok.whitespaceChars(0, ' ');
    tok.whitespaceChars(',', ',');
    tok.whitespaceChars('{', '{');
    tok.whitespaceChars('}', '}');
    tok.commentChar('/');
    tok.quoteChar('"');
    tok.quoteChar('\'');
    tok.slashSlashComments(true);
    tok.slashStarComments(true);

    // Read in file
    int numFaceIndices = nextInt(tok, "number of face indices");
    short[] faceIndices = new short[numFaceIndices * 6];
    for (int i = 0; i < numFaceIndices * 6; i++) {
      faceIndices[i] = (short) nextInt(tok, "face index");
    }
    int numVertices = nextInt(tok, "number of vertices");
    float[] vertices = new float[numVertices * 3];
    for (int i = 0; i < numVertices * 3; i++) {
      vertices[i] = nextFloat(tok, "vertex");
    }
    int numNormals = nextInt(tok, "number of normals");
    float[] normals = new float[numNormals * 3];
    for (int i = 0; i < numNormals * 3; i++) {
      normals[i] = nextFloat(tok, "normal");
    }

    int lid = gl.glGenLists(1);
    gl.glNewList(lid, GL.GL_COMPILE);

    gl.glBegin(GL.GL_TRIANGLES);
    for (int i = 0; i < faceIndices.length; i += 6) {
      for (int j = 0; j < 3; j++) {
        int vi = faceIndices[i + j];
        int ni = faceIndices[i + j + 3];
        gl.glNormal3f(normals[3 * ni],
                      normals[3 * ni + 1],
                      normals[3 * ni + 2]);
        gl.glVertex3f(vertices[3 * vi],
                      vertices[3 * vi + 1],
                      vertices[3 * vi + 2]);
      }
    }
    gl.glEnd();

    gl.glEndList();
    return lid;
  }

  private static int nextInt(StreamTokenizer tok, String error) throws IOException {
    if (tok.nextToken() != StreamTokenizer.TT_WORD) {
      throw new IOException("Parse error reading " + error + " at line " + tok.lineno());
    }
    try {
      return Integer.parseInt(tok.sval);
    } catch (NumberFormatException e) {
      throw new IOException("Parse error reading " + error + " at line " + tok.lineno());
    }
  }

  private static float nextFloat(StreamTokenizer tok, String error) throws IOException {
    if (tok.nextToken() != StreamTokenizer.TT_WORD) {
      throw new IOException("Parse error reading " + error + " at line " + tok.lineno());
    }
    try {
      return Float.parseFloat(tok.sval);
    } catch (NumberFormatException e) {
      throw new IOException("Parse error reading " + error + " at line " + tok.lineno());
    }
  }
}
