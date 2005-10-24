package demos.xtrans;

import gleem.linalg.*;

/** A quadrilateral of two-dimensional floating-point values which
    interpolates between specified start and end values. */

public class InterpolatedQuad2f {
  private Quad2f start;
  private Quad2f end;

  /** Constructs a new InterpolatedQuad2f. By default both the start
      and end quadrilaterals have all of their points set to the
      origin. */
  public InterpolatedQuad2f() {
    start = new Quad2f();
    end = new Quad2f();
  }

  /** Returns the starting value for the interpolation. */
  public Quad2f getStart() {
    return start;
  }

  /** Sets the starting value for the interpolation. */
  public void setStart(Quad2f quad) {
    start.set(quad);
  }

  /** Returns the ending value for the interpolation. */
  public Quad2f getEnd() {
    return end;
  }

  /** Sets the ending value for the interpolation. */
  public void setEnd(Quad2f quad) {
    end.set(quad);
  }

  /** Gets the current interpolated value at the specified fraction of
      interpolation (0.0 - 1.0). */
  public Quad2f getCurrent(float fraction) {
    return start.times(1.0f - fraction).plus(end.times(fraction));
  }
}
