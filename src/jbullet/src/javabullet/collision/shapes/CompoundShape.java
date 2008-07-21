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

import java.util.ArrayList;
import java.util.List;
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.linearmath.MatrixUtil;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

/**
 * CompoundShape allows to store multiple other CollisionShapes.
 * This allows for concave collision objects. This is more general then the Static Concave TriangleMeshShape.
 * 
 * @author jezek2
 */
public class CompoundShape extends CollisionShape {

	private final List<CompoundShapeChild> children = new ArrayList<CompoundShapeChild>();
	private final Vector3f localAabbMin = new Vector3f(1e30f, 1e30f, 1e30f);
	private final Vector3f localAabbMax = new Vector3f(-1e30f, -1e30f, -1e30f);

	private OptimizedBvh aabbTree = null;

	private float collisionMargin = 0f;
	protected final Vector3f localScaling = new Vector3f(1f, 1f, 1f);

	public void addChildShape(Transform localTransform, CollisionShape shape) {
		stack.vectors.push();
		try {
			//m_childTransforms.push_back(localTransform);
			//m_childShapes.push_back(shape);
			CompoundShapeChild child = new CompoundShapeChild();
			child.transform.set(localTransform);
			child.childShape = shape;
			child.childShapeType = shape.getShapeType();
			child.childMargin = shape.getMargin();

			children.add(child);

			// extend the local aabbMin/aabbMax
			Vector3f _localAabbMin = stack.vectors.get(), _localAabbMax = stack.vectors.get();
			shape.getAabb(localTransform, _localAabbMin, _localAabbMax);

			// JAVA NOTE: rewritten
	//		for (int i=0;i<3;i++)
	//		{
	//			if (this.localAabbMin[i] > _localAabbMin[i])
	//			{
	//				this.localAabbMin[i] = _localAabbMin[i];
	//			}
	//			if (this.localAabbMax[i] < _localAabbMax[i])
	//			{
	//				this.localAabbMax[i] = _localAabbMax[i];
	//			}
	//		}
			VectorUtil.setMin(this.localAabbMin, _localAabbMin);
			VectorUtil.setMax(this.localAabbMax, _localAabbMax);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public int getNumChildShapes() {
		return children.size();
	}

	public CollisionShape getChildShape(int index) {
		return children.get(index).childShape;
	}

	public Transform getChildTransform(int index) {
		return children.get(index).transform;
	}

	public List<CompoundShapeChild> getChildList() {
		return children;
	}

	/**
	 * getAabb's default implementation is brute force, expected derived classes to implement a fast dedicated version.
	 */
	@Override
	public void getAabb(Transform trans, Vector3f aabbMin, Vector3f aabbMax) {
		stack.pushCommonMath();
		try {
			Vector3f localHalfExtents = stack.vectors.get();
			localHalfExtents.sub(localAabbMax, localAabbMin);
			localHalfExtents.scale(0.5f);

			Vector3f localCenter = stack.vectors.get();
			localCenter.add(localAabbMax, localAabbMin);
			localCenter.scale(0.5f);

			Matrix3f abs_b = stack.matrices.get(trans.basis);
			MatrixUtil.absolute(abs_b);

			Vector3f center = stack.vectors.get(localCenter);
			trans.transform(center);

			Vector3f tmp = stack.vectors.get();

			Vector3f extent = stack.vectors.get();
			abs_b.getRow(0, tmp);
			extent.x = tmp.dot(localHalfExtents);
			abs_b.getRow(1, tmp);
			extent.y = tmp.dot(localHalfExtents);
			abs_b.getRow(2, tmp);
			extent.z = tmp.dot(localHalfExtents);

			extent.x += getMargin();
			extent.y += getMargin();
			extent.z += getMargin();

			aabbMin.sub(center, extent);
			aabbMax.add(center, extent);
		}
		finally {
			stack.popCommonMath();
		}
	}

	@Override
	public void setLocalScaling(Vector3f scaling) {
		localScaling.set(scaling);
	}

	@Override
	public Vector3f getLocalScaling() {
		return localScaling;
	}

	@Override
	public void calculateLocalInertia(float mass, Vector3f inertia) {
		stack.pushCommonMath();
		try {
			// approximation: take the inertia from the aabb for now
			Transform ident = stack.transforms.get();
			ident.setIdentity();
			Vector3f aabbMin = stack.vectors.get(), aabbMax = stack.vectors.get();
			getAabb(ident, aabbMin, aabbMax);

			Vector3f halfExtents = stack.vectors.get();
			halfExtents.sub(aabbMax, aabbMin);
			halfExtents.scale(0.5f);

			float lx = 2f * halfExtents.x;
			float ly = 2f * halfExtents.y;
			float lz = 2f * halfExtents.z;

			inertia.x = (mass / 12f) * (ly * ly + lz * lz);
			inertia.y = (mass / 12f) * (lx * lx + lz * lz);
			inertia.z = (mass / 12f) * (lx * lx + ly * ly);
		}
		finally {
			stack.popCommonMath();
		}
	}
	
	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE;
	}

	@Override
	public void setMargin(float margin) {
		collisionMargin = margin;
	}

	@Override
	public float getMargin() {
		return collisionMargin;
	}

	@Override
	public String getName() {
		return "Compound";
	}

	// this is optional, but should make collision queries faster, by culling non-overlapping nodes
	// void	createAabbTreeFromChildren();
	
	public OptimizedBvh getAabbTree() {
		return aabbTree;
	}
	
}
