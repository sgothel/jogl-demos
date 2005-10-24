package demos.xtrans;

import gleem.linalg.*;

/** A floating-point value which interpolates between specified start
    and end values. */

public class InterpolatedFloat {
  private float start;
  private float end;

  /** Returns the starting value for the interpolation. */
  public float getStart() { return start; }

  /** Sets the starting value for the interpolation. */
  public void setStart(float val) { start = val; }

  /** Returns the ending value for the interpolation. */
  public float getEnd() { return end; }

  /** Sets the ending value for the interpolation. */
  public void setEnd(float val) { end = val; }

  /** Gets the current interpolated value at the specified fraction of
      interpolation (0.0 - 1.0). */
  public float getCurrent(float fraction) { 
    return (start * (1.0f - fraction)) + (end * fraction);
  }
}
