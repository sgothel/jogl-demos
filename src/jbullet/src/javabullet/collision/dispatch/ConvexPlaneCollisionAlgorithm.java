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
import javabullet.collision.narrowphase.PersistentManifold;
import javabullet.collision.shapes.ConvexShape;
import javabullet.collision.shapes.StaticPlaneShape;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class ConvexPlaneCollisionAlgorithm extends CollisionAlgorithm {

	private boolean ownManifold = false;
	private PersistentManifold manifoldPtr;
	private boolean isSwapped;
	
	public ConvexPlaneCollisionAlgorithm(PersistentManifold mf, CollisionAlgorithmConstructionInfo ci, CollisionObject col0, CollisionObject col1, boolean isSwapped) {
		super(ci);
		this.manifoldPtr = mf;
		this.isSwapped = isSwapped;

		CollisionObject convexObj = isSwapped ? col1 : col0;
		CollisionObject planeObj = isSwapped ? col0 : col1;

		if (manifoldPtr == null && dispatcher.needsCollision(convexObj, planeObj)) {
			manifoldPtr = dispatcher.getNewManifold(convexObj, planeObj);
			ownManifold = true;
		}
	}
	
	@Override
	public void destroy() {
		if (ownManifold) {
			if (manifoldPtr != null) {
				dispatcher.releaseManifold(manifoldPtr);
			}
		}
	}
	
	
	@Override
	public void processCollision(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		if (manifoldPtr == null) {
			return;
		}

		stack.pushCommonMath();
		try {
			CollisionObject convexObj = isSwapped ? body1 : body0;
			CollisionObject planeObj = isSwapped ? body0 : body1;

			ConvexShape convexShape = (ConvexShape) convexObj.getCollisionShape();
			StaticPlaneShape planeShape = (StaticPlaneShape) planeObj.getCollisionShape();

			boolean hasCollision = false;
			Vector3f planeNormal = planeShape.getPlaneNormal();
			float planeConstant = planeShape.getPlaneConstant();

			Transform planeInConvex = stack.transforms.get();
			planeInConvex.inverse(convexObj.getWorldTransform());
			planeInConvex.mul(planeObj.getWorldTransform());

			Transform convexInPlaneTrans = stack.transforms.get();
			convexInPlaneTrans.inverse(planeObj.getWorldTransform());
			convexInPlaneTrans.mul(convexObj.getWorldTransform());

			Vector3f tmp = stack.vectors.get();
			tmp.negate(planeNormal);
			planeInConvex.basis.transform(tmp);

			Vector3f vtx = stack.vectors.get(convexShape.localGetSupportingVertexWithoutMargin(tmp));
			Vector3f vtxInPlane = stack.vectors.get(vtx);
			convexInPlaneTrans.transform(vtxInPlane);

			float distance = (planeNormal.dot(vtxInPlane) - planeConstant) - convexShape.getMargin();

			Vector3f vtxInPlaneProjected = stack.vectors.get();
			tmp.scale(distance, planeNormal);
			vtxInPlaneProjected.sub(vtxInPlane, tmp);

			Vector3f vtxInPlaneWorld = stack.vectors.get(vtxInPlaneProjected);
			planeObj.getWorldTransform().transform(vtxInPlaneWorld);

			hasCollision = distance < manifoldPtr.getContactBreakingThreshold();
			resultOut.setPersistentManifold(manifoldPtr);
			if (hasCollision) {
				// report a contact. internally this will be kept persistent, and contact reduction is done
				Vector3f normalOnSurfaceB = stack.vectors.get(planeNormal);
				planeObj.getWorldTransform().basis.transform(normalOnSurfaceB);

				Vector3f pOnB = stack.vectors.get(vtxInPlaneWorld);
				resultOut.addContactPoint(normalOnSurfaceB, pOnB, distance);
			}
			if (ownManifold) {
				if (manifoldPtr.getNumContacts() != 0) {
					resultOut.refreshContactPoints();
				}
			}
		}
		finally {
			stack.popCommonMath();
		}
	}

	@Override
	public float calculateTimeOfImpact(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		// not yet
		return 1f;
	}

	////////////////////////////////////////////////////////////////////////////
	
	public static class CreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			if (!swapped) {
				return new ConvexPlaneCollisionAlgorithm(null, ci, body0, body1, false);
			}
			else {
				return new ConvexPlaneCollisionAlgorithm(null, ci, body0, body1, true);
			}
		}
	}
	
}
