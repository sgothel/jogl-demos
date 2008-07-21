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
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 * Implements cylinder shape interface.
 * 
 * @author jezek2
 */
public class CylinderShape extends BoxShape {

	protected int upAxis;

	public CylinderShape(Vector3f halfExtents) {
		super(halfExtents);
		upAxis = 1;
		recalcLocalAabb();
	}

	protected CylinderShape(Vector3f halfExtents, boolean unused) {
		super(halfExtents);
	}

	@Override
	public void getAabb(Transform t, Vector3f aabbMin, Vector3f aabbMax) {
		_PolyhedralConvexShape_getAabb(t, aabbMin, aabbMax);
	}

	protected Vector3f cylinderLocalSupportX(Vector3f halfExtents, Vector3f v) {
		return cylinderLocalSupport(halfExtents, v, 0, 1, 0, 2);
	}

	protected Vector3f cylinderLocalSupportY(Vector3f halfExtents, Vector3f v) {
		return cylinderLocalSupport(halfExtents, v, 1, 0, 1, 2);
	}

	protected Vector3f cylinderLocalSupportZ(Vector3f halfExtents, Vector3f v) {
		return cylinderLocalSupport(halfExtents, v, 2, 0, 2, 1);
	}
	
	private Vector3f cylinderLocalSupport(Vector3f halfExtents, Vector3f v, int cylinderUpAxis, int XX, int YY, int ZZ) {
		stack.vectors.push();
		try {
			//mapping depends on how cylinder local orientation is
			// extents of the cylinder is: X,Y is for radius, and Z for height

			float radius = VectorUtil.getCoord(halfExtents, XX);
			float halfHeight = VectorUtil.getCoord(halfExtents, cylinderUpAxis);

			Vector3f tmp = stack.vectors.get();
			float d;

			float s = (float) Math.sqrt(VectorUtil.getCoord(v, XX) * VectorUtil.getCoord(v, XX) + VectorUtil.getCoord(v, ZZ) * VectorUtil.getCoord(v, ZZ));
			if (s != 0f) {
				d = radius / s;
				VectorUtil.setCoord(tmp, XX, VectorUtil.getCoord(v, XX) * d);
				VectorUtil.setCoord(tmp, YY, VectorUtil.getCoord(v, YY) < 0f ? -halfHeight : halfHeight);
				VectorUtil.setCoord(tmp, ZZ, VectorUtil.getCoord(v, ZZ) * d);
				return stack.vectors.returning(tmp);
			}
			else {
				VectorUtil.setCoord(tmp, XX, radius);
				VectorUtil.setCoord(tmp, YY, VectorUtil.getCoord(v, YY) < 0f ? -halfHeight : halfHeight);
				VectorUtil.setCoord(tmp, ZZ, 0f);
				return stack.vectors.returning(tmp);
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public Vector3f localGetSupportingVertexWithoutMargin(Vector3f vec) {
		return cylinderLocalSupportY(getHalfExtentsWithoutMargin(), vec);
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(Vector3f[] vectors, Vector3f[] supportVerticesOut, int numVectors) {
		for (int i = 0; i < numVectors; i++) {
			supportVerticesOut[i].set(cylinderLocalSupportY(getHalfExtentsWithoutMargin(), vectors[i]));
		}
	}

	@Override
	public Vector3f localGetSupportingVertex(Vector3f vec) {
		stack.vectors.push();
		try {
			Vector3f supVertex = stack.vectors.get();
			supVertex.set(localGetSupportingVertexWithoutMargin(vec));

			if (getMargin() != 0f) {
				Vector3f vecnorm = stack.vectors.get(vec);
				if (vecnorm.lengthSquared() < (BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON)) {
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

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.CYLINDER_SHAPE_PROXYTYPE;
	}

	public int getUpAxis() {
		return upAxis;
	}
	
	public float getRadius() {
		return getHalfExtentsWithMargin().x;
	}

	@Override
	public String getName() {
		return "CylinderY";
	}
	
}
