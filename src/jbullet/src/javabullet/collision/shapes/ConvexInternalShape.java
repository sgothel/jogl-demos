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

import javabullet.BulletGlobals;
import javabullet.linearmath.MatrixUtil;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public abstract class ConvexInternalShape extends ConvexShape {

	//local scaling. collisionMargin is not scaled !
	protected final Vector3f localScaling = new Vector3f(1f, 1f, 1f);
	protected final Vector3f implicitShapeDimensions = new Vector3f();
	protected float collisionMargin = BulletGlobals.CONVEX_DISTANCE_MARGIN;

	/**
	 * getAabb's default implementation is brute force, expected derived classes to implement a fast dedicated version.
	 */
	@Override
	public void getAabb(Transform t, Vector3f aabbMin, Vector3f aabbMax) {
		getAabbSlow(t, aabbMin, aabbMax);
	}
	
	@Override
	public void getAabbSlow(Transform trans, Vector3f minAabb, Vector3f maxAabb) {
		stack.vectors.push();
		try {
			float margin = getMargin();
			for (int i=0;i<3;i++)
			{
				Vector3f vec = stack.vectors.get(0f, 0f, 0f);
				VectorUtil.setCoord(vec, i, 1f);

				Vector3f tmp1 = stack.vectors.get();
				MatrixUtil.transposeTransform(tmp1, vec, trans.basis);
				Vector3f sv = stack.vectors.get(localGetSupportingVertex(tmp1));

				Vector3f tmp2 = stack.vectors.get(sv);
				trans.transform(tmp2);

				VectorUtil.setCoord(maxAabb, i, VectorUtil.getCoord(tmp2, i) + margin);

				VectorUtil.setCoord(vec, i, -1f);
				
				MatrixUtil.transposeTransform(tmp1, vec, trans.basis);
				tmp2.set(localGetSupportingVertex(tmp1));
				trans.transform(tmp2);

				VectorUtil.setCoord(minAabb, i, VectorUtil.getCoord(tmp2, i) - margin);
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public Vector3f localGetSupportingVertex(Vector3f vec) {
		stack.vectors.push();
		try {
			Vector3f supVertex = stack.vectors.get(localGetSupportingVertexWithoutMargin(vec));

			if (getMargin() != 0f) {
				Vector3f vecnorm = stack.vectors.get(vec);
				if (vecnorm.lengthSquared() < (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
					vecnorm.set(-1f, -1f, -1f);
				}
				vecnorm.normalize();
				supVertex.scaleAdd(getMargin(), vecnorm, supVertex);
			}
			return stack.vectors.returning(supVertex);
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	public void setLocalScaling(Vector3f scaling) {
		this.localScaling.set(scaling);
	}
	
	public Vector3f getLocalScaling() {
		return localScaling;
	}

	public float getMargin() {
		return collisionMargin;
	}

	public void setMargin(float margin) {
		this.collisionMargin = margin;
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 0;
	}

	@Override
	public void getPreferredPenetrationDirection(int index, Vector3f penetrationVector) {
		throw new InternalError();
	}
	
}
