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

import javabullet.collision.narrowphase.ConvexCast.CastResult;
import javabullet.collision.shapes.ConvexShape;
import javabullet.collision.shapes.TriangleCallback;
import javabullet.collision.shapes.TriangleShape;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public abstract class TriangleConvexcastCallback implements TriangleCallback {

	public ConvexShape convexShape;
	public final Transform convexShapeFrom = new Transform();
	public final Transform convexShapeTo = new Transform();
	public final Transform triangleToWorld = new Transform();
	public float hitFraction;

	public TriangleConvexcastCallback(ConvexShape convexShape, Transform convexShapeFrom, Transform convexShapeTo, Transform triangleToWorld) {
		this.convexShape = convexShape;
		this.convexShapeFrom.set(convexShapeFrom);
		this.convexShapeTo.set(convexShapeTo);
		this.triangleToWorld.set(triangleToWorld);
		this.hitFraction = 1f;
	}
	
	public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
		TriangleShape triangleShape = new TriangleShape(triangle[0], triangle[1], triangle[2]);

		VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();

		//#define  USE_SUBSIMPLEX_CONVEX_CAST 1
		//#ifdef USE_SUBSIMPLEX_CONVEX_CAST
		// TODO: implement ContinuousConvexCollision
		SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(convexShape, triangleShape, simplexSolver);
		//#else
		// //btGjkConvexCast	convexCaster(m_convexShape,&triangleShape,&simplexSolver);
		//btContinuousConvexCollision convexCaster(m_convexShape,&triangleShape,&simplexSolver,NULL);
		//#endif //#USE_SUBSIMPLEX_CONVEX_CAST

		CastResult castResult = new CastResult();
		castResult.fraction = 1f;
		if (convexCaster.calcTimeOfImpact(convexShapeFrom, convexShapeTo, triangleToWorld, triangleToWorld, castResult)) {
			// add hit
			if (castResult.normal.lengthSquared() > 0.0001f) {
				if (castResult.fraction < hitFraction) {

					//#ifdef USE_SUBSIMPLEX_CONVEX_CAST
					// rotate normal into worldspace
					convexShapeFrom.basis.transform(castResult.normal);
					//#endif //USE_SUBSIMPLEX_CONVEX_CAST
					castResult.normal.normalize();

					reportHit(castResult.normal,
							castResult.hitPoint,
							castResult.fraction,
							partId,
							triangleIndex);
				}
			}
		}
	}

	public abstract float reportHit(Vector3f hitNormalLocal, Vector3f hitPointLocal, float hitFraction, int partId, int triangleIndex);
	
}
