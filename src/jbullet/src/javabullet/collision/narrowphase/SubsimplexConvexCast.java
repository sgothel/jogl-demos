/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http://continuousphysics.com/Bullet/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package javabullet.collision.narrowphase;

import javabullet.BulletGlobals;
import javabullet.BulletStack;
import javabullet.collision.shapes.ConvexShape;
import javabullet.collision.shapes.MinkowskiSumShape;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 * SubsimplexConvexCast implements Gino van den Bergens' paper
 * "Ray Casting against bteral Convex Objects with Application to Continuous Collision Detection"
 * GJK based Ray Cast, optimized version
 * Objects should not start in overlap, otherwise results are not defined.
 * 
 * @author jezek2
 */
public class SubsimplexConvexCast implements ConvexCast {

	protected final BulletStack stack = BulletStack.get();
	
	// Typically the conservative advancement reaches solution in a few iterations, clip it to 32 for degenerate cases.
	// See discussion about this here http://continuousphysics.com/Bullet/phpBB2/viewtopic.php?t=565
	//#ifdef BT_USE_DOUBLE_PRECISION
	//#define MAX_ITERATIONS 64
	//#else
	//#define MAX_ITERATIONS 32
	//#endif
	
	private static final int MAX_ITERATIONS = 32;
	
	private SimplexSolverInterface simplexSolver;
	private ConvexShape convexA;
	private ConvexShape convexB;

	public SubsimplexConvexCast(ConvexShape shapeA, ConvexShape shapeB, SimplexSolverInterface simplexSolver) {
		this.convexA = shapeA;
		this.convexB = shapeB;
		this.simplexSolver = simplexSolver;
	}
	
	public boolean calcTimeOfImpact(Transform fromA, Transform toA, Transform fromB, Transform toB, CastResult result) {
		stack.pushCommonMath();
		try {
			MinkowskiSumShape combi = new MinkowskiSumShape(convexA, convexB);
			MinkowskiSumShape convex = combi;

			Transform rayFromLocalA = stack.transforms.get();
			Transform rayToLocalA = stack.transforms.get();

			rayFromLocalA.inverse(fromA);
			rayFromLocalA.mul(fromB);

			rayToLocalA.inverse(toA);
			rayToLocalA.mul(toB);

			simplexSolver.reset();

			convex.setTransformB(stack.transforms.get(rayFromLocalA.basis));

			//btScalar radius = btScalar(0.01);

			float lambda = 0f;
			// todo: need to verify this:
			// because of minkowski difference, we need the inverse direction

			Vector3f s = stack.vectors.get();
			s.negate(rayFromLocalA.origin);

			//Vector3f r = -(rayToLocalA.getOrigin()-rayFromLocalA.getOrigin());
			Vector3f r = stack.vectors.get();
			r.sub(rayToLocalA.origin, rayFromLocalA.origin);
			r.negate();

			Vector3f x = stack.vectors.get(s);
			Vector3f v = stack.vectors.get();
			Vector3f arbitraryPoint = stack.vectors.get(convex.localGetSupportingVertex(r));

			v.sub(x, arbitraryPoint);

			int maxIter = MAX_ITERATIONS;

			Vector3f n = stack.vectors.get(0f, 0f, 0f);
			boolean hasResult = false;
			Vector3f c = stack.vectors.get();

			float lastLambda = lambda;

			float dist2 = v.lengthSquared();
			//#ifdef BT_USE_DOUBLE_PRECISION
			//	btScalar epsilon = btScalar(0.0001);
			//#else
			float epsilon = 0.0001f;
			//#endif
			Vector3f w = stack.vectors.get(), p = stack.vectors.get();
			float VdotR;

			while ((dist2 > epsilon) && (maxIter--) != 0) {
				p.set(convex.localGetSupportingVertex(v));
				w.sub(x, p);

				float VdotW = v.dot(w);

				if (VdotW > 0f) {
					VdotR = v.dot(r);

					if (VdotR >= -(BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
						return false;
					}
					else {
						lambda = lambda - VdotW / VdotR;
						x.scaleAdd(lambda, r, s);
						simplexSolver.reset();
						// check next line
						w.sub(x, p);
						lastLambda = lambda;
						n.set(v);
						hasResult = true;
					}
				}
				simplexSolver.addVertex(w, x, p);
				if (simplexSolver.closest(v)) {
					dist2 = v.lengthSquared();
					hasResult = true;
					//printf("V=%f , %f, %f\n",v[0],v[1],v[2]);
					//printf("DIST2=%f\n",dist2);
					//printf("numverts = %i\n",m_simplexSolver->numVertices());
				}
				else {
					dist2 = 0f;
				}
			}

			//int numiter = MAX_ITERATIONS - maxIter;
			//	printf("number of iterations: %d", numiter);
			result.fraction = lambda;
			result.normal.set(n);

			return true;
		}
		finally {
			stack.popCommonMath();
		}
	}

}
