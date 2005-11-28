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

import gleem.linalg.*;

/** A vector of three-dimensional floating-point values which
 * interpolates between specified start and end values.
 *
 * @author Kenneth Russell
 */

public class InterpolatedVec3f {
  private Vec3f start;
  private Vec3f end;

  /** Constructs a new InterpolatedQuad2f. By default both the start
      and end quadrilaterals have all of their points set to the
      origin. */
  public InterpolatedVec3f() {
    start = new Vec3f();
    end   = new Vec3f();
  }

  /** Returns the starting value for the interpolation. */
  public Vec3f getStart() { return start; }

  /** Sets the starting value for the interpolation. */
  public void setStart(Vec3f vec) { start.set(vec); }

  /** Returns the ending value for the interpolation. */
  public Vec3f getEnd() { return end; }

  /** Sets the ending value for the interpolation. */
  public void setEnd(Vec3f vec)   { end.set(vec); }

  /** Gets the current interpolated value at the specified fraction of
      interpolation (0.0 - 1.0). */
  public Vec3f getCurrent(float fraction) {
    return start.times(1.0f - fraction).plus(end.times(fraction));
  }
}
