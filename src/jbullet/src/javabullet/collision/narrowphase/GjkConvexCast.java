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
import javabullet.BulletPool;
import javabullet.BulletStack;
import javabullet.ObjectPool;
import javabullet.collision.narrowphase.DiscreteCollisionDetectorInterface.ClosestPointInput;
import javabullet.collision.shapes.ConvexShape;
import javabullet.collision.shapes.MinkowskiSumShape;
import javabullet.collision.shapes.SphereShape;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 * GjkConvexCast performs a raycast on a convex object using support mapping.
 * 
 * @author jezek2
 */
public class GjkConvexCast implements ConvexCast {

	protected final BulletStack stack = BulletStack.get();
	protected final ObjectPool<ClosestPointInput> pointInputsPool = BulletPool.get(ClosestPointInput.class);
	
	private SimplexSolverInterface simplexSolver;
	private ConvexShape convexA;
	private ConvexShape convexB;

	public GjkConvexCast(ConvexShape convexA, ConvexShape convexB, SimplexSolverInterface simplexSolver) {
		this.simplexSolver = simplexSolver;
		this.convexA = convexA;
		this.convexB = convexB;
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

			Transform trA = stack.transforms.get(), trB = stack.transforms.get();
			trA.set(fromA);
			trB.set(fromB);
			trA.origin.set(0f, 0f, 0f);
			trB.origin.set(0f, 0f, 0f);

			convex.setTransformA(trA);
			convex.setTransformB(trB);

			float radius = 0.01f;

			float lambda = 0f;
			Vector3f s = stack.vectors.get(rayFromLocalA.origin);
			Vector3f r = stack.vectors.get();
			r.sub(rayToLocalA.origin, rayFromLocalA.origin);
			Vector3f x = stack.vectors.get(s);
			Vector3f n = stack.vectors.get();
			n.set(0f, 0f, 0f);
			boolean hasResult = false;
			Vector3f c = stack.vectors.get();

			float lastLambda = lambda;

			// first solution, using GJK

			// no penetration support for now, perhaps pass a pointer when we really want it
			ConvexPenetrationDepthSolver penSolverPtr = null;

			Transform identityTrans = stack.transforms.get();
			identityTrans.setIdentity();

			SphereShape raySphere = new SphereShape(0f);
			raySphere.setMargin(0f);

			Transform sphereTr = stack.transforms.get();
			sphereTr.setIdentity();
			sphereTr.origin.set(rayFromLocalA.origin);

			result.drawCoordSystem(sphereTr);
			{
				PointCollector pointCollector1 = new PointCollector();
				GjkPairDetector gjk = new GjkPairDetector(raySphere, convex, simplexSolver, penSolverPtr);

				ClosestPointInput input = pointInputsPool.get();
				input.init(gl);
				
				input.transformA.set(sphereTr);
				input.transformB.set(identityTrans);
				gjk.getClosestPoints(input, pointCollector1, null);
				
				pointInputsPool.release(input);

				hasResult = pointCollector1.hasResult;
				c.set(pointCollector1.pointInWorld);
				n.set(pointCollector1.normalOnBInWorld);
			}

			if (hasResult) {
				Vector3f tmp = stack.vectors.get();

				float dist;
				tmp.sub(c, x);
				dist = tmp.length();
				if (dist < radius) {
					// penetration
					lastLambda = 1f;
				}

				// not close enough
				while (dist > radius) {

					n.sub(x, c);
					float nDotr = n.dot(r);

					if (nDotr >= -(BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
						return false;
					}

					lambda = lambda - n.dot(n) / nDotr;
					if (lambda <= lastLambda) {
						break;
					}

					lastLambda = lambda;

					x.scaleAdd(lambda, r, s);

					sphereTr.origin.set(x);
					result.drawCoordSystem(sphereTr);
					PointCollector pointCollector = new PointCollector();
					GjkPairDetector gjk = new GjkPairDetector(raySphere, convex, simplexSolver, penSolverPtr);
					
					ClosestPointInput input = pointInputsPool.get();
					input.init(gl);
					
					input.transformA.set(sphereTr);
					input.transformB.set(identityTrans);
					gjk.getClosestPoints(input, pointCollector, null);
					
					pointInputsPool.release(input);
					
					if (pointCollector.hasResult) {
						if (pointCollector.distance < 0f) {
							// degeneracy, report a hit
							result.fraction = lastLambda;
							result.normal.set(n);
							result.hitPoint.set(pointCollector.pointInWorld);
							return true;
						}
						c.set(pointCollector.pointInWorld);
						tmp.sub(c, x);
						dist = tmp.length();
					}
					else {
						// ??
						return false;
					}

				}

				if (lastLambda < 1f) {
					result.fraction = lastLambda;
					result.normal.set(n);
					result.hitPoint.set(c);
					return true;
				}
			}

			return false;
		}
		finally {
			stack.popCommonMath();
		}
	}

}
