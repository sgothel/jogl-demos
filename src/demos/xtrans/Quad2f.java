package demos.xtrans;

import gleem.linalg.*;

/** A quadrilateral in which the vertices are two-dimensional
    floating-point values. */

public class Quad2f {
  private Vec2f[] vecs;

  public static final int UPPER_LEFT  = 0;
  public static final int LOWER_LEFT  = 1;
  public static final int LOWER_RIGHT = 2;
  public static final int UPPER_RIGHT = 2;

  private static final int NUM_VECS = 4;
  
  /** Constructs a Quad2f in which all the vertices are set to the
      origin. */
  public Quad2f() {
    vecs = new Vec2f[NUM_VECS];
    for (int i = 0; i < NUM_VECS; i++) {
      vecs[i] = new Vec2f();
    }
  }

  /** Constructs a Quad2f in which the vertices are set to the
      specified values. */
  public Quad2f(Vec2f upperLeft,
                Vec2f lowerLeft,
                Vec2f lowerRight,
                Vec2f upperRight) {
    this();
    setVec(0, upperLeft);
    setVec(1, lowerLeft);
    setVec(2, lowerRight);
    setVec(3, upperRight);
  }

  /** Sets the specified vertex to the specified value. */
  public void setVec(int which, Vec2f val) {
    vecs[which].set(val);
  }

  /** Returns the specified vertex. */
  public Vec2f getVec(int which) {
    return vecs[which];
  }

  /** Sets all four points of this quadrilateral. */
  public void set(Quad2f quad) {
    for (int i = 0; i < NUM_VECS; i++) {
      setVec(i, quad.getVec(i));
    }
  }

  /** Returns a newly-constructed Quad2f in which all vertices have
      been multiplied in scalar fashion by the passed value.  */
  public Quad2f times(float val) {
    return new Quad2f(getVec(0).times(val),
                      getVec(1).times(val),
                      getVec(2).times(val),
                      getVec(3).times(val));
  }

  /** Returns a newly-constructed Quad2f in which the vertices are the
      component-wise sums of this quad and the passed quad. */
  public Quad2f plus(Quad2f val) {
    return new Quad2f(getVec(0).plus(val.getVec(0)),
                      getVec(1).plus(val.getVec(1)),
                      getVec(2).plus(val.getVec(2)),
                      getVec(3).plus(val.getVec(3)));
  }
}
