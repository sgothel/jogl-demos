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

import javabullet.BulletGlobals;
import javabullet.BulletStack;
import javabullet.collision.broadphase.CollisionAlgorithm;
import javabullet.collision.broadphase.CollisionAlgorithmConstructionInfo;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.narrowphase.PersistentManifold;
import javabullet.collision.shapes.SphereShape;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class SphereSphereCollisionAlgorithm extends CollisionAlgorithm {
	
	private boolean ownManifold;
	private PersistentManifold manifoldPtr;

	public SphereSphereCollisionAlgorithm(PersistentManifold mf, CollisionAlgorithmConstructionInfo ci, CollisionObject col0, CollisionObject col1) {
		super(ci);
		manifoldPtr = mf;

		if (manifoldPtr == null) {
			manifoldPtr = dispatcher.getNewManifold(col0, col1);
			ownManifold = true;
		}
	}

	public SphereSphereCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci) {
		super(ci);
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
	public void processCollision(CollisionObject col0, CollisionObject col1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		if (manifoldPtr == null) {
			return;
		}

		stack.vectors.push();
		try {
			resultOut.setPersistentManifold(manifoldPtr);

			SphereShape sphere0 = (SphereShape) col0.getCollisionShape();
			SphereShape sphere1 = (SphereShape) col1.getCollisionShape();

			Vector3f diff = stack.vectors.get();
			diff.sub(col0.getWorldTransform().origin, col1.getWorldTransform().origin);

			float len = diff.length();
			float radius0 = sphere0.getRadius();
			float radius1 = sphere1.getRadius();

			manifoldPtr.clearManifold();

			// if distance positive, don't generate a new contact
			if (len > (radius0 + radius1)) {
				return;
			}
			// distance (negative means penetration)
			float dist = len - (radius0 + radius1);

			Vector3f normalOnSurfaceB = stack.vectors.get(1f, 0f, 0f);
			if (len > BulletGlobals.FLT_EPSILON) {
				normalOnSurfaceB.scale(1f / len, diff);
			}

			Vector3f tmp = stack.vectors.get();

			// point on A (worldspace)
			Vector3f pos0 = stack.vectors.get();
			tmp.scale(radius0, normalOnSurfaceB);
			pos0.sub(col0.getWorldTransform().origin, tmp);

			// point on B (worldspace)
			Vector3f pos1 = stack.vectors.get();
			tmp.scale(radius1, normalOnSurfaceB);
			pos1.add(col1.getWorldTransform().origin, tmp);

			// report a contact. internally this will be kept persistent, and contact reduction is done
			resultOut.addContactPoint(normalOnSurfaceB, pos1, dist);

			//no resultOut->refreshContactPoints(); needed, because of clearManifold (all points are new)
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public float calculateTimeOfImpact(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		return 1f;
	}

	////////////////////////////////////////////////////////////////////////////
	
	public static final CollisionAlgorithmCreateFunc createFunc = new CollisionAlgorithmCreateFunc() {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			return new SphereSphereCollisionAlgorithm(null, ci, body0, body1);
		}
	};
	
}
