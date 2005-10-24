package demos.xtrans;

import gleem.linalg.*;

/** A quadrilateral of three-dimensional floating-point values which
    interpolates between specified start and end values. */

public class InterpolatedQuad3f {
  private Quad3f start;
  private Quad3f end;

  /** Constructs a new InterpolatedQuad3f. By default both the start
      and end quadrilaterals have all of their points set to the
      origin. */
  public InterpolatedQuad3f() {
    start = new Quad3f();
    end = new Quad3f();
  }

  /** Returns the starting value for the interpolation. */
  public Quad3f getStart() {
    return start;
  }

  /** Sets the starting value for the interpolation. */
  public void setStart(Quad3f quad) {
    start.set(quad);
  }

  /** Returns the ending value for the interpolation. */
  public Quad3f getEnd() {
    return end;
  }

  /** Sets the ending value for the interpolation. */
  public void setEnd(Quad3f quad) {
    end.set(quad);
  }

  /** Gets the current interpolated value at the specified fraction of
      interpolation (0.0 - 1.0). */
  public Quad3f getCurrent(float fraction) {
    return start.times(1.0f - fraction).plus(end.times(fraction));
  }
}
