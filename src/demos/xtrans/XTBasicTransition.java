/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

package demos.xtrans;

import javax.media.opengl.*;
import gleem.linalg.*;

/** A basic transition supporting animated translation, rotation about
 * a pivot point, scrolling and fading effects.
 *
 * @author Kenneth Russell
 */

public class XTBasicTransition implements XTTransition {
  protected Vec3f              pivot       = new Vec3f();
  protected Vec3f              axis        = new Vec3f();
  protected InterpolatedFloat  angle;
  protected InterpolatedVec3f  translation;
  protected InterpolatedQuad3f vertices;
  protected InterpolatedQuad2f texcoords;
  protected InterpolatedFloat  alpha;
  protected float              percentage;

  /** Constructs a new transition object with all of its initial state
      set to zero. */
  public XTBasicTransition() {
  }

  public void update(float percentage) {
    this.percentage = percentage;
  }

  public void draw(GL gl) {
    float  percent = percentage;
    Quad3f vts     = vertices.getCurrent(percent);
    Quad2f tex     = texcoords.getCurrent(percent);

    if (translation != null) {
      Vec3f  trans   = translation.getCurrent(percent);
      gl.glTranslatef(trans.x(), trans.y(), trans.z());
    }
    // Rotate about pivot point
    gl.glTranslatef(pivot.x(), pivot.y(), pivot.z());
    if (angle != null) {
      gl.glRotatef(angle.getCurrent(percent), axis.x(), axis.y(), axis.z());
    }
    gl.glTranslatef(-pivot.x(), -pivot.y(), -pivot.z());

    gl.glBegin(GL.GL_TRIANGLES);
    float a = 1.0f;
    if (alpha != null) {
      a = alpha.getCurrent(percent);
    }
    gl.glColor4f(1, 1, 1, a);
    // Triangle 1
    gl.glTexCoord2f(tex.getVec(0).x(), tex.getVec(0).y());
    gl.glVertex3f  (vts.getVec(0).x(), vts.getVec(0).y(), vts.getVec(0).z());
    gl.glTexCoord2f(tex.getVec(1).x(), tex.getVec(1).y());
    gl.glVertex3f  (vts.getVec(1).x(), vts.getVec(1).y(), vts.getVec(1).z());
    gl.glTexCoord2f(tex.getVec(3).x(), tex.getVec(3).y());
    gl.glVertex3f  (vts.getVec(3).x(), vts.getVec(3).y(), vts.getVec(3).z());
    // Triangle 2
    gl.glTexCoord2f(tex.getVec(3).x(), tex.getVec(3).y());
    gl.glVertex3f  (vts.getVec(3).x(), vts.getVec(3).y(), vts.getVec(3).z());
    gl.glTexCoord2f(tex.getVec(1).x(), tex.getVec(1).y());
    gl.glVertex3f  (vts.getVec(1).x(), vts.getVec(1).y(), vts.getVec(1).z());
    gl.glTexCoord2f(tex.getVec(2).x(), tex.getVec(2).y());
    gl.glVertex3f  (vts.getVec(2).x(), vts.getVec(2).y(), vts.getVec(2).z());
    gl.glEnd();
  }

  /** Returns the rotation axis for this transition. By default this
      axis is not set, so if an interpolator is specified for the
      rotation angle the axis of rotation must be specified as
      well. */
  public Vec3f             getRotationAxis() { return axis; }

  /** Sets the rotation axis for this transition. By default this axis
      is not set, so if an interpolator is specified for the rotation
      angle the axis of rotation must be specified as well. */
  public void              setRotationAxis(Vec3f axis) { this.axis.set(axis); }

  /** Returns the interpolator for the rotation angle for this
      transition. By default this interpolator is null, meaning that
      the transition does not perform any rotation. */
  public InterpolatedFloat getRotationAngle() { return angle; }

  /** Sets the interpolator for the rotation angle for this
      transition. By default this interpolator is null, meaning that
      the transition does not perform any rotation. */
  public void              setRotationAngle(InterpolatedFloat angle) { this.angle = angle; }

  /** Returns the pivot point for this transition's rotation. By
      default this is set to the origin, which corresponds to the
      upper-left corner of the component being animated. */
  public Vec3f getPivotPoint() { return pivot; }

  /** Sets the pivot point for this transition's rotation. By default
      this is set to the origin, which corresponds to the upper-left
      corner of the component being animated. */
  public void  setPivotPoint(Vec3f point) { pivot.set(point); }

  /** Returns the interpolator for this transition's translation. By
      default the translation is a fixed vector from the origin to the
      upper left corner of the component being animated; the vertices
      are specified relative to that point in the +x and -y
      directions. */
  public InterpolatedVec3f getTranslation() { return translation; }

  /** Sets the interpolator for this transition's translation. By
      default the translation is a fixed vector from the origin to the
      upper left corner of the component being animated; the vertices
      are specified relative to that point in the +x and -y
      directions. */
  public void              setTranslation(InterpolatedVec3f interp) { translation = interp; }

  /** Returns the interpolator for the vertices of this transition's
      component when drawn on screen. By default the vertices are
      specified with the upper-left corner of the component at the
      origin and the component extending in the +x (right) and -y
      (down) directions; the units of the vertices correspond to the
      on-screen size of the component in pixels. The component's
      location is specified using the translation interpolator. */
  public InterpolatedQuad3f getVertices() { return vertices; }

  /** Sets the interpolator for the vertices of this transition's
      component when drawn on screen. By default the vertices are
      specified with the upper-left corner of the component at the
      origin and the component extending in the +x (right) and -y
      (down) directions; the units of the vertices correspond to the
      on-screen size of the component in pixels. The component's
      location is specified using the translation interpolator. */
  public void               setVertices(InterpolatedQuad3f interp) { vertices = interp; }

  /** Returns the interpolator for the texture coordinates of this
      transition's component when drawn on screen. By default these
      are set so that at either the start or end point (depending on
      whether this is an "in" or "out" transition) the texture
      coordinates specify rendering of the component's entire
      contents. */
  public InterpolatedQuad2f getTexCoords() { return texcoords; }

  /** Sets the interpolator for the texture coordinates of this
      transition's component when drawn on screen. By default these
      are set so that at either the start or end point (depending on
      whether this is an "in" or "out" transition) the texture
      coordinates specify rendering of the component's entire
      contents. */
  public void               setTexCoords(InterpolatedQuad2f interp) { texcoords = interp; }

  /** Returns the interpolator for this component's alpha value. */
  public InterpolatedFloat  getAlpha() { return alpha; }

  /** Sets the interpolator for this component's alpha value. */
  public void               setAlpha(InterpolatedFloat interp) { alpha = interp; }
}
