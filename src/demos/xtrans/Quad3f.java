package demos.xtrans;

import gleem.linalg.*;

/** A quadrilateral in which the vertices are three-dimensional
    floating-point values. */

public class Quad3f {
  private Vec3f[] vecs;

  public static final int UPPER_LEFT  = 0;
  public static final int LOWER_LEFT  = 1;
  public static final int LOWER_RIGHT = 2;
  public static final int UPPER_RIGHT = 3;

  private static final int NUM_VECS = 4;
  
  /** Constructs a Quad3f in which all the vertices are set to the
      origin. */
  public Quad3f() {
    vecs = new Vec3f[NUM_VECS];
    for (int i = 0; i < NUM_VECS; i++) {
      vecs[i] = new Vec3f();
    }
  }

  /** Constructs a Quad3f in which the vertices are set to the
      specified values. */
  public Quad3f(Vec3f upperLeft,
                Vec3f lowerLeft,
                Vec3f lowerRight,
                Vec3f upperRight) {
    this();
    setVec(0, upperLeft);
    setVec(1, lowerLeft);
    setVec(2, lowerRight);
    setVec(3, upperRight);
  }

  /** Sets the specified vertex to the specified value. */
  public void setVec(int which, Vec3f val) {
    vecs[which].set(val);
  }

  /** Returns the specified vertex. */
  public Vec3f getVec(int which) {
    return vecs[which];
  }

  /** Sets all four points of this quadrilateral. */
  public void set(Quad3f quad) {
    for (int i = 0; i < NUM_VECS; i++) {
      setVec(i, quad.getVec(i));
    }
  }

  /** Returns a newly-constructed Quad2f in which all vertices have
      been multiplied in scalar fashion by the passed value.  */
  public Quad3f times(float val) {
    return new Quad3f(getVec(0).times(val),
                      getVec(1).times(val),
                      getVec(2).times(val),
                      getVec(3).times(val));
  }

  /** Returns a newly-constructed Quad2f in which the vertices are the
      component-wise sums of this quad and the passed quad. */
  public Quad3f plus(Quad3f val) {
    return new Quad3f(getVec(0).plus(val.getVec(0)),
                      getVec(1).plus(val.getVec(1)),
                      getVec(2).plus(val.getVec(2)),
                      getVec(3).plus(val.getVec(3)));
  }
}
