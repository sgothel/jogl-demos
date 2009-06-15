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

package javabullet.collision.dispatch;

import javabullet.collision.broadphase.CollisionAlgorithm;
import javabullet.collision.broadphase.CollisionAlgorithmConstructionInfo;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.narrowphase.ConvexCast.CastResult;
import javabullet.collision.narrowphase.SubsimplexConvexCast;
import javabullet.collision.narrowphase.VoronoiSimplexSolver;
import javabullet.collision.shapes.ConcaveShape;
import javabullet.collision.shapes.SphereShape;
import javabullet.collision.shapes.TriangleCallback;
import javabullet.collision.shapes.TriangleShape;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 * ConvexConcaveCollisionAlgorithm supports collision between convex shapes
 * and (concave) trianges meshes.
 * 
 * @author jezek2
 */
public class ConvexConcaveCollisionAlgorithm extends CollisionAlgorithm {

	private boolean isSwapped;
	private ConvexTriangleCallback btConvexTriangleCallback;
	
	public ConvexConcaveCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1, boolean isSwapped) {
		super(ci);
		this.isSwapped = isSwapped;
		this.btConvexTriangleCallback = new ConvexTriangleCallback(dispatcher, body0, body1, isSwapped);
	}
	
	@Override
	public void destroy() {
		btConvexTriangleCallback.destroy();
	}

	@Override
	public void processCollision(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		CollisionObject convexBody = isSwapped ? body1 : body0;
		CollisionObject triBody = isSwapped ? body0 : body1;

		if (triBody.getCollisionShape().isConcave()) {
			CollisionObject triOb = triBody;
			ConcaveShape concaveShape = (ConcaveShape)triOb.getCollisionShape();

			if (convexBody.getCollisionShape().isConvex()) {
				float collisionMarginTriangle = concaveShape.getMargin();

				resultOut.setPersistentManifold(btConvexTriangleCallback.manifoldPtr);
				btConvexTriangleCallback.setTimeStepAndCounters(collisionMarginTriangle, dispatchInfo, resultOut);

				// Disable persistency. previously, some older algorithm calculated all contacts in one go, so you can clear it here.
				//m_dispatcher->clearManifold(m_btConvexTriangleCallback.m_manifoldPtr);

				btConvexTriangleCallback.manifoldPtr.setBodies(convexBody, triBody);

				concaveShape.processAllTriangles(btConvexTriangleCallback, btConvexTriangleCallback.getAabbMin(), btConvexTriangleCallback.getAabbMax());

				resultOut.refreshContactPoints();
			}
		}
	}

	@Override
	public float calculateTimeOfImpact(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		stack.pushCommonMath();
		try {
			Vector3f tmp = stack.vectors.get();

			CollisionObject convexbody = isSwapped ? body1 : body0;
			CollisionObject triBody = isSwapped ? body0 : body1;

			// quick approximation using raycast, todo: hook up to the continuous collision detection (one of the btConvexCast)

			// only perform CCD above a certain threshold, this prevents blocking on the long run
			// because object in a blocked ccd state (hitfraction<1) get their linear velocity halved each frame...
			tmp.sub(convexbody.getInterpolationWorldTransform().origin, convexbody.getWorldTransform().origin);
			float squareMot0 = tmp.lengthSquared();
			if (squareMot0 < convexbody.getCcdSquareMotionThreshold()) {
				return 1f;
			}

			//const btVector3& from = convexbody->m_worldTransform.getOrigin();
			//btVector3 to = convexbody->m_interpolationWorldTransform.getOrigin();
			//todo: only do if the motion exceeds the 'radius'

			Transform triInv = stack.transforms.get(triBody.getWorldTransform());
			triInv.inverse();

			Transform convexFromLocal = stack.transforms.get();
			convexFromLocal.mul(triInv, convexbody.getWorldTransform());

			Transform convexToLocal = stack.transforms.get();
			convexToLocal.mul(triInv, convexbody.getInterpolationWorldTransform());

			if (triBody.getCollisionShape().isConcave()) {
				Vector3f rayAabbMin = stack.vectors.get(convexFromLocal.origin);
				VectorUtil.setMin(rayAabbMin, convexToLocal.origin);

				Vector3f rayAabbMax = stack.vectors.get(convexFromLocal.origin);
				VectorUtil.setMax(rayAabbMax, convexToLocal.origin);

				float ccdRadius0 = convexbody.getCcdSweptSphereRadius();

				tmp.set(ccdRadius0, ccdRadius0, ccdRadius0);
				rayAabbMin.sub(tmp);
				rayAabbMax.add(tmp);

				float curHitFraction = 1f; // is this available?
				LocalTriangleSphereCastCallback raycastCallback = new LocalTriangleSphereCastCallback(convexFromLocal, convexToLocal, convexbody.getCcdSweptSphereRadius(), curHitFraction);

				raycastCallback.hitFraction = convexbody.getHitFraction();

				CollisionObject concavebody = triBody;

				ConcaveShape triangleMesh = (ConcaveShape)concavebody.getCollisionShape();

				if (triangleMesh != null) {
					triangleMesh.processAllTriangles(raycastCallback, rayAabbMin, rayAabbMax);
				}

				if (raycastCallback.hitFraction < convexbody.getHitFraction()) {
					convexbody.setHitFraction(raycastCallback.hitFraction);
					return raycastCallback.hitFraction;
				}
			}

			return 1f;
		}
		finally {
			stack.popCommonMath();
		}
	}
	
	public void clearCache() {
		btConvexTriangleCallback.clearCache();
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private static class LocalTriangleSphereCastCallback implements TriangleCallback {
		public final Transform ccdSphereFromTrans = new Transform();
		public final Transform ccdSphereToTrans = new Transform();
		public final Transform meshTransform = new Transform();

		public float ccdSphereRadius;
		public float hitFraction;
		
		private final Transform ident = new Transform();
		
		public LocalTriangleSphereCastCallback(Transform from, Transform to, float ccdSphereRadius, float hitFraction) {
			this.ccdSphereFromTrans.set(from);
			this.ccdSphereToTrans.set(to);
			this.ccdSphereRadius = ccdSphereRadius;
			this.hitFraction = hitFraction;

			// JAVA NOTE: moved here from processTriangle
			ident.setIdentity();
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			// do a swept sphere for now
			
			//btTransform ident;
			//ident.setIdentity();
			
			CastResult castResult = new CastResult();
			castResult.fraction = hitFraction;
			SphereShape pointShape = new SphereShape(ccdSphereRadius);
			TriangleShape triShape = new TriangleShape(triangle[0], triangle[1], triangle[2]);
			VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
			SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(pointShape, triShape, simplexSolver);
			//GjkConvexCast	convexCaster(&pointShape,convexShape,&simplexSolver);
			//ContinuousConvexCollision convexCaster(&pointShape,convexShape,&simplexSolver,0);
			//local space?

			if (convexCaster.calcTimeOfImpact(ccdSphereFromTrans, ccdSphereToTrans, ident, ident, castResult)) {
				if (hitFraction > castResult.fraction) {
					hitFraction = castResult.fraction;
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////

	public static class CreateFunc extends CollisionAlgorithmCreateFunc {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			return new ConvexConcaveCollisionAlgorithm(ci, body0, body1, false);
		}
	}
	
	public static class SwappedCreateFunc extends CollisionAlgorithmCreateFunc {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			return new ConvexConcaveCollisionAlgorithm(ci, body0, body1, true);
		}
	}
	
}
