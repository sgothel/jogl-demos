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

package javabullet.collision.shapes;

import javabullet.BulletStack;
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 * CollisionShape provides interface for collision shapes that can be shared among CollisionObjects.
 * 
 * @author jezek2
 */
public abstract class CollisionShape {

	protected final BulletStack stack = BulletStack.get();
	
	///getAabb returns the axis aligned bounding box in the coordinate frame of the given transform t.
	public abstract void getAabb(Transform t, Vector3f aabbMin, Vector3f aabbMax);

	public void getBoundingSphere(Vector3f center, float[] radius) {
		stack.pushCommonMath();
		try {
			Vector3f tmp = stack.vectors.get();

			Transform tr = stack.transforms.get();
			tr.setIdentity();
			Vector3f aabbMin = stack.vectors.get(), aabbMax = stack.vectors.get();

			getAabb(tr, aabbMin, aabbMax);

			tmp.sub(aabbMax, aabbMin);
			radius[0] = tmp.length() * 0.5f;

			tmp.add(aabbMin, aabbMax);
			center.scale(0.5f, tmp);
		}
		finally {
			stack.popCommonMath();
		}
	}

	///getAngularMotionDisc returns the maximus radius needed for Conservative Advancement to handle time-of-impact with rotations.
	public float getAngularMotionDisc() {
		stack.vectors.push();
		try {
			Vector3f center = stack.vectors.get();
			float[] disc = new float[1]; // TODO: stack
			getBoundingSphere(center, disc);
			disc[0] += center.length();
			return disc[0];
		}
		finally {
			stack.vectors.pop();
		}
	}

	///calculateTemporalAabb calculates the enclosing aabb for the moving object over interval [0..timeStep)
	///result is conservative
	public void calculateTemporalAabb(Transform curTrans, Vector3f linvel, Vector3f angvel, float timeStep, Vector3f temporalAabbMin, Vector3f temporalAabbMax) {
		stack.vectors.push();
		try {
			//start with static aabb
			getAabb(curTrans, temporalAabbMin, temporalAabbMax);

			float temporalAabbMaxx = temporalAabbMax.x;
			float temporalAabbMaxy = temporalAabbMax.y;
			float temporalAabbMaxz = temporalAabbMax.z;
			float temporalAabbMinx = temporalAabbMin.x;
			float temporalAabbMiny = temporalAabbMin.y;
			float temporalAabbMinz = temporalAabbMin.z;

			// add linear motion
			Vector3f linMotion = stack.vectors.get(linvel);
			linMotion.scale(timeStep);

			//todo: simd would have a vector max/min operation, instead of per-element access
			if (linMotion.x > 0f) {
				temporalAabbMaxx += linMotion.x;
			}
			else {
				temporalAabbMinx += linMotion.x;
			}
			if (linMotion.y > 0f) {
				temporalAabbMaxy += linMotion.y;
			}
			else {
				temporalAabbMiny += linMotion.y;
			}
			if (linMotion.z > 0f) {
				temporalAabbMaxz += linMotion.z;
			}
			else {
				temporalAabbMinz += linMotion.z;
			}

			//add conservative angular motion
			float angularMotion = angvel.length() * getAngularMotionDisc() * timeStep;
			Vector3f angularMotion3d = stack.vectors.get(angularMotion, angularMotion, angularMotion);
			temporalAabbMin.set(temporalAabbMinx, temporalAabbMiny, temporalAabbMinz);
			temporalAabbMax.set(temporalAabbMaxx, temporalAabbMaxy, temporalAabbMaxz);

			temporalAabbMin.sub(angularMotion3d);
			temporalAabbMax.add(angularMotion3d);
		}
		finally {
			stack.vectors.pop();
		}
	}

//#ifndef __SPU__
	public boolean isPolyhedral() {
		return getShapeType().isPolyhedral();
	}

	public boolean isConvex() {
		return getShapeType().isConvex();
	}

	public boolean isConcave() {
		return getShapeType().isConcave();
	}

	public boolean isCompound() {
		return getShapeType().isCompound();
	}

	///isInfinite is used to catch simulation error (aabb check)
	public boolean isInfinite() {
		return getShapeType().isInfinite();
	}

	public abstract BroadphaseNativeType getShapeType();

	public abstract void setLocalScaling(Vector3f scaling);
	
	// TODO: returns const
	public abstract Vector3f getLocalScaling();

	public abstract void calculateLocalInertia(float mass, Vector3f inertia);


//debugging support
	public abstract String getName();
//#endif //__SPU__
	public abstract void setMargin(float margin);

	public abstract float getMargin();
}
