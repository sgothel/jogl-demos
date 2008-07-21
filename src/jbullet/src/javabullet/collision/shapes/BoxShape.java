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

import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.linearmath.MatrixUtil;
import javabullet.linearmath.ScalarUtil;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * BoxShape implements both a feature based (vertex/edge/plane) and implicit (getSupportingVertex) Box.
 * 
 * @author jezek2
 */
public class BoxShape extends PolyhedralConvexShape {

	public BoxShape(Vector3f boxHalfExtents) {
		Vector3f margin = new Vector3f(getMargin(), getMargin(), getMargin());
		VectorUtil.mul(implicitShapeDimensions, boxHalfExtents, localScaling);
		implicitShapeDimensions.sub(margin);
	}

	public Vector3f getHalfExtentsWithMargin() {
		stack.vectors.push();
		try {
			Vector3f halfExtents = stack.vectors.get(getHalfExtentsWithoutMargin());
			Vector3f margin = stack.vectors.get(getMargin(), getMargin(), getMargin());
			halfExtents.add(margin);
			return stack.vectors.returning(halfExtents);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public Vector3f getHalfExtentsWithoutMargin() {
		return implicitShapeDimensions; // changed in Bullet 2.63: assume the scaling and margin are included
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.BOX_SHAPE_PROXYTYPE;
	}

	@Override
	public Vector3f localGetSupportingVertex(Vector3f vec) {
		stack.vectors.push();
		try {
			Vector3f halfExtents = stack.vectors.get(getHalfExtentsWithoutMargin());
			Vector3f margin = stack.vectors.get(getMargin(), getMargin(), getMargin());
			halfExtents.add(margin);

			return stack.vectors.returning(stack.vectors.get(
					ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
					ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
					ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z)));
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public Vector3f localGetSupportingVertexWithoutMargin(Vector3f vec) {
		stack.vectors.push();
		try {
			Vector3f halfExtents = stack.vectors.get(getHalfExtentsWithoutMargin());

			return stack.vectors.returning(stack.vectors.get(
					ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
					ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
					ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z)));
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(Vector3f[] vectors, Vector3f[] supportVerticesOut, int numVectors) {
		stack.vectors.push();
		try {
			Vector3f halfExtents = stack.vectors.get(getHalfExtentsWithoutMargin());

			for (int i = 0; i < numVectors; i++) {
				Vector3f vec = vectors[i];
				supportVerticesOut[i].set(ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
						ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
						ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z));
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void setMargin(float margin) {
		stack.vectors.push();
		try {
			// correct the implicitShapeDimensions for the margin
			Vector3f oldMargin = stack.vectors.get(getMargin(), getMargin(), getMargin());
			Vector3f implicitShapeDimensionsWithMargin = stack.vectors.get();
			implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);

			super.setMargin(margin);
			Vector3f newMargin = stack.vectors.get(getMargin(), getMargin(), getMargin());
			implicitShapeDimensions.sub(implicitShapeDimensionsWithMargin, newMargin);
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void setLocalScaling(Vector3f scaling) {
		stack.vectors.push();
		try {
			Vector3f oldMargin = stack.vectors.get(getMargin(), getMargin(), getMargin());
			Vector3f implicitShapeDimensionsWithMargin = stack.vectors.get();
			implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);
			Vector3f unScaledImplicitShapeDimensionsWithMargin = stack.vectors.get();
			VectorUtil.div(unScaledImplicitShapeDimensionsWithMargin, implicitShapeDimensionsWithMargin, localScaling);

			super.setLocalScaling(scaling);

			VectorUtil.mul(implicitShapeDimensions, unScaledImplicitShapeDimensionsWithMargin, localScaling);
			implicitShapeDimensions.sub(oldMargin);
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void getAabb(Transform t, Vector3f aabbMin, Vector3f aabbMax) {
		stack.pushCommonMath();
		try {
			Vector3f halfExtents = getHalfExtentsWithoutMargin();

			Matrix3f abs_b = stack.matrices.get(t.basis);
			MatrixUtil.absolute(abs_b);

			Vector3f tmp = stack.vectors.get();

			Vector3f center = stack.vectors.get(t.origin);
			Vector3f extent = stack.vectors.get();
			abs_b.getRow(0, tmp);
			extent.x = tmp.dot(halfExtents);
			abs_b.getRow(1, tmp);
			extent.y = tmp.dot(halfExtents);
			abs_b.getRow(2, tmp);
			extent.z = tmp.dot(halfExtents);

			extent.add(stack.vectors.get(getMargin(), getMargin(), getMargin()));

			aabbMin.sub(center, extent);
			aabbMax.add(center, extent);
		}
		finally {
			stack.popCommonMath();
		}
	}

	@Override
	public void calculateLocalInertia(float mass, Vector3f inertia) {
		stack.vectors.push();
		try {
			//btScalar margin = btScalar(0.);
			Vector3f halfExtents = stack.vectors.get(getHalfExtentsWithMargin());

			float lx = 2f * halfExtents.x;
			float ly = 2f * halfExtents.y;
			float lz = 2f * halfExtents.z;

			inertia.set(mass / 12f * (ly * ly + lz * lz),
					mass / 12f * (lx * lx + lz * lz),
					mass / 12f * (lx * lx + ly * ly));
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void getPlane(Vector3f planeNormal, Vector3f planeSupport, int i) {
		stack.vectors.push();
		stack.vectors4.push();
		try {
			// this plane might not be aligned...
			Vector4f plane = stack.vectors4.get();
			getPlaneEquation(plane, i);
			planeNormal.set(plane.x, plane.y, plane.z);
			Vector3f tmp = stack.vectors.get();
			tmp.negate(planeNormal);
			planeSupport.set(localGetSupportingVertex(tmp));
		}
		finally {
			stack.vectors.pop();
			stack.vectors4.pop();
		}
	}

	@Override
	public int getNumPlanes() {
		return 6;
	}

	@Override
	public int getNumVertices() {
		return 8;
	}

	@Override
	public int getNumEdges() {
		return 12;
	}

	@Override
	public void getVertex(int i, Vector3f vtx) {
		// JAVA NOTE: against stack usage, but safe with code below
		Vector3f halfExtents = getHalfExtentsWithoutMargin();

		vtx.set(halfExtents.x * (1 - (i & 1)) - halfExtents.x * (i & 1),
				halfExtents.y * (1 - ((i & 2) >> 1)) - halfExtents.y * ((i & 2) >> 1),
				halfExtents.z * (1 - ((i & 4) >> 2)) - halfExtents.z * ((i & 4) >> 2));
	}
	
	public void getPlaneEquation(Vector4f plane, int i) {
		// JAVA NOTE: against stack usage, but safe with code below
		Vector3f halfExtents = getHalfExtentsWithoutMargin();

		switch (i) {
			case 0:
				plane.set(1f, 0f, 0f, -halfExtents.x);
				break;
			case 1:
				plane.set(-1f, 0f, 0f, -halfExtents.x);
				break;
			case 2:
				plane.set(0f, 1f, 0f, -halfExtents.y);
				break;
			case 3:
				plane.set(0f, -1f, 0f, -halfExtents.y);
				break;
			case 4:
				plane.set(0f, 0f, 1f, -halfExtents.z);
				break;
			case 5:
				plane.set(0f, 0f, -1f, -halfExtents.z);
				break;
			default:
				assert (false);
		}
	}

	@Override
	public void getEdge(int i, Vector3f pa, Vector3f pb) {
		int edgeVert0 = 0;
		int edgeVert1 = 0;

		switch (i) {
			case 0:
				edgeVert0 = 0;
				edgeVert1 = 1;
				break;
			case 1:
				edgeVert0 = 0;
				edgeVert1 = 2;
				break;
			case 2:
				edgeVert0 = 1;
				edgeVert1 = 3;

				break;
			case 3:
				edgeVert0 = 2;
				edgeVert1 = 3;
				break;
			case 4:
				edgeVert0 = 0;
				edgeVert1 = 4;
				break;
			case 5:
				edgeVert0 = 1;
				edgeVert1 = 5;

				break;
			case 6:
				edgeVert0 = 2;
				edgeVert1 = 6;
				break;
			case 7:
				edgeVert0 = 3;
				edgeVert1 = 7;
				break;
			case 8:
				edgeVert0 = 4;
				edgeVert1 = 5;
				break;
			case 9:
				edgeVert0 = 4;
				edgeVert1 = 6;
				break;
			case 10:
				edgeVert0 = 5;
				edgeVert1 = 7;
				break;
			case 11:
				edgeVert0 = 6;
				edgeVert1 = 7;
				break;
			default:
				assert (false);
		}

		getVertex(edgeVert0, pa);
		getVertex(edgeVert1, pb);
	}

	@Override
	public boolean isInside(Vector3f pt, float tolerance) {
		// JAVA NOTE: against stack usage, but safe with code below
		Vector3f halfExtents = getHalfExtentsWithoutMargin();

		//btScalar minDist = 2*tolerance;

		boolean result =
				(pt.x <= (halfExtents.x + tolerance)) &&
				(pt.x >= (-halfExtents.x - tolerance)) &&
				(pt.y <= (halfExtents.y + tolerance)) &&
				(pt.y >= (-halfExtents.y - tolerance)) &&
				(pt.z <= (halfExtents.z + tolerance)) &&
				(pt.z >= (-halfExtents.z - tolerance));

		return result;
	}

	@Override
	public String getName() {
		return "Box";
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 6;
	}

	@Override
	public void getPreferredPenetrationDirection(int index, Vector3f penetrationVector) {
		switch (index) {
			case 0:
				penetrationVector.set(1f, 0f, 0f);
				break;
			case 1:
				penetrationVector.set(-1f, 0f, 0f);
				break;
			case 2:
				penetrationVector.set(0f, 1f, 0f);
				break;
			case 3:
				penetrationVector.set(0f, -1f, 0f);
				break;
			case 4:
				penetrationVector.set(0f, 0f, 1f);
				break;
			case 5:
				penetrationVector.set(0f, 0f, -1f);
				break;
			default:
				assert (false);
		}
	}

}
