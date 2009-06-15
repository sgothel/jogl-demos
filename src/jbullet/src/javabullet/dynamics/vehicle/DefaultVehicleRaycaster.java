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

package javabullet.dynamics.vehicle;

import javabullet.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import javabullet.dynamics.DynamicsWorld;
import javabullet.dynamics.RigidBody;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class DefaultVehicleRaycaster implements VehicleRaycaster {

	protected DynamicsWorld dynamicsWorld;

	public DefaultVehicleRaycaster(DynamicsWorld world) {
		this.dynamicsWorld = world;
	}

	public Object castRay(Vector3f from, Vector3f to, VehicleRaycasterResult result) {
		//RayResultCallback& resultCallback;

		ClosestRayResultCallback rayCallback = new ClosestRayResultCallback(from, to);

		dynamicsWorld.rayTest(from, to, rayCallback);

		if (rayCallback.hasHit()) {
			RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
			if (body != null) {
				result.hitPointInWorld.set(rayCallback.hitPointWorld);
				result.hitNormalInWorld.set(rayCallback.hitNormalWorld);
				result.hitNormalInWorld.normalize();
				result.distFraction = rayCallback.closestHitFraction;
				return body;
			}
		}
		return null;
	}

}
