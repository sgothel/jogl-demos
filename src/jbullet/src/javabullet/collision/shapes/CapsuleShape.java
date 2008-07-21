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
 * CapsuleShape represents a capsule around the Y axis.
 * A more general solution that can represent capsules is the MultiSphereShape.
 * 
 * @author jezek2
 */
public class CapsuleShape extends ConvexInternalShape {
	
	public CapsuleShape(float radius, float height) {
		implicitShapeDimensions.set(radius, 0.5f * height, radius);
	}

	@Override
	public Vector3f localGetSupportingVertexWithoutMargin(Vector3f vec0) {
		stack.vectors.push();
		try {
			Vector3f supVec = stack.vectors.get(0f, 0f, 0f);

			float maxDot = -1e30f;

			Vector3f vec = stack.vectors.get(vec0);
			float lenSqr = vec.lengthSquared();
			if (lenSqr < 0.0001f) {
				vec.set(1f, 0f, 0f);
			}
			else {
				float rlen = 1f / (float) Math.sqrt(lenSqr);
				vec.scale(rlen);
			}

			Vector3f vtx = stack.vectors.get();
			float newDot;

			float radius = getRadius();

			Vector3f tmp1 = stack.vectors.get();
			Vector3f tmp2 = stack.vectors.get();

			{
				Vector3f pos = stack.vectors.get(0f, getHalfHeight(), 0f);
				VectorUtil.mul(tmp1, vec, localScaling);
				tmp1.scale(radius);
				tmp2.scale(getMargin(), vec);
				vtx.add(pos, tmp1);
				vtx.sub(tmp2);
				newDot = vec.dot(vtx);
				if (newDot > maxDot) {
					maxDot = newDot;
					supVec.set(vtx);
				}
			}
			{
				Vector3f pos = stack.vectors.get(0f, -getHalfHeight(), 0f);
				VectorUtil.mul(tmp1, vec, localScaling);
				tmp1.scale(radius);
				tmp2.scale(getMargin(), vec);
				vtx.add(pos, tmp1);
				vtx.sub(tmp2);
				newDot = vec.dot(vtx);
				if (newDot > maxDot) {
					maxDot = newDot;
					supVec.set(vtx);
				}
			}

			return stack.vectors.returning(supVec);
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(Vector3f[] vectors, Vector3f[] supportVerticesOut, int numVectors) {
		// TODO: implement
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void calculateLocalInertia(float mass, Vector3f inertia) {
		stack.pushCommonMath();
		try {
			// as an approximation, take the inertia of the box that bounds the spheres

			Transform ident = stack.transforms.get();
			ident.setIdentity();

			float radius = getRadius();

			Vector3f halfExtents = stack.vectors.get(radius, radius + getHalfHeight(), radius);

			float margin = BulletGlobals.CONVEX_DISTANCE_MARGIN;

			float lx = 2f * (halfExtents.x + margin);
			float ly = 2f * (halfExtents.y + margin);
			float lz = 2f * (halfExtents.z + margin);
			float x2 = lx * lx;
			float y2 = ly * ly;
			float z2 = lz * lz;
			float scaledmass = mass * 0.08333333f;

			inertia.x = scaledmass * (y2 + z2);
			inertia.y = scaledmass * (x2 + z2);
			inertia.z = scaledmass * (x2 + y2);
		}
		finally {
			stack.popCommonMath();
		}
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.CAPSULE_SHAPE_PROXYTYPE;
	}

	@Override
	public String getName() {
		return "CapsuleShape";
	}
	
	public float getRadius() {
		return implicitShapeDimensions.x;
	}

	public float getHalfHeight() {
		return implicitShapeDimensions.y;
	}

}
