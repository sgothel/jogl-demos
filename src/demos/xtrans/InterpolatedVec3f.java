package demos.xtrans;

import gleem.linalg.*;

/** A vector of three-dimensional floating-point values which
    interpolates between specified start and end values. */

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
