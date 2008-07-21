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

import javabullet.BulletStack;
import javabullet.collision.broadphase.CollisionAlgorithm;
import javabullet.collision.broadphase.CollisionAlgorithmConstructionInfo;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.narrowphase.PersistentManifold;
import javabullet.collision.shapes.CollisionShape;
import javabullet.collision.shapes.TriangleCallback;
import javabullet.collision.shapes.TriangleShape;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;

/**
 * For each triangle in the concave mesh that overlaps with the AABB of a convex
 * (convexProxy field), processTriangle is called.
 * 
 * @author jezek2
 */
class ConvexTriangleCallback implements TriangleCallback {

	protected final BulletStack stack = BulletStack.get();
	
	private CollisionObject convexBody;
	private CollisionObject triBody;

	private final Vector3f aabbMin = new Vector3f();
	private final Vector3f aabbMax = new Vector3f();

	private ManifoldResult resultOut;

	private Dispatcher dispatcher;
	private DispatcherInfo dispatchInfoPtr;
	private float collisionMarginTriangle;
	
	public int triangleCount;
	public PersistentManifold manifoldPtr;
	
	public ConvexTriangleCallback(Dispatcher dispatcher, CollisionObject body0, CollisionObject body1, boolean isSwapped) {
		this.dispatcher = dispatcher;
		this.dispatchInfoPtr = null;

		convexBody = isSwapped ? body1 : body0;
		triBody = isSwapped ? body0 : body1;

		//
		// create the manifold from the dispatcher 'manifold pool'
		//
		manifoldPtr = dispatcher.getNewManifold(convexBody, triBody);

		clearCache();
	}
	
	public void destroy() {
		clearCache();
		dispatcher.releaseManifold(manifoldPtr);
	}

	public void setTimeStepAndCounters(float collisionMarginTriangle, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		stack.pushCommonMath();
		try {
			this.dispatchInfoPtr = dispatchInfo;
			this.collisionMarginTriangle = collisionMarginTriangle;
			this.resultOut = resultOut;

			// recalc aabbs
			Transform convexInTriangleSpace = stack.transforms.get();

			convexInTriangleSpace.inverse(triBody.getWorldTransform());
			convexInTriangleSpace.mul(convexBody.getWorldTransform());

			CollisionShape convexShape = (CollisionShape)convexBody.getCollisionShape();
			//CollisionShape* triangleShape = static_cast<btCollisionShape*>(triBody->m_collisionShape);
			convexShape.getAabb(convexInTriangleSpace, aabbMin, aabbMax);
			float extraMargin = collisionMarginTriangle;
			Vector3f extra = stack.vectors.get(extraMargin, extraMargin, extraMargin);

			aabbMax.add(extra);
			aabbMin.sub(extra);
		}
		finally {
			stack.popCommonMath();
		}
	}

	private CollisionAlgorithmConstructionInfo ci = new CollisionAlgorithmConstructionInfo();
	private TriangleShape tm = new TriangleShape();
	
	public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
		stack.vectors.push();
		try {
			// just for debugging purposes
			//printf("triangle %d",m_triangleCount++);

			// aabb filter is already applied!	

			ci.dispatcher1 = dispatcher;

			CollisionObject ob = (CollisionObject) triBody;

			// debug drawing of the overlapping triangles
			if (dispatchInfoPtr != null && dispatchInfoPtr.debugDraw != null && dispatchInfoPtr.debugDraw.getDebugMode() > 0) {
				Vector3f color = stack.vectors.get(255, 255, 0);
				Transform tr = ob.getWorldTransform();

				Vector3f tmp1 = stack.vectors.get();
				Vector3f tmp2 = stack.vectors.get();

				tmp1.set(triangle[0]); tr.transform(tmp1);
				tmp2.set(triangle[1]); tr.transform(tmp2);
				dispatchInfoPtr.debugDraw.drawLine(tmp1, tmp2, color);

				tmp1.set(triangle[1]); tr.transform(tmp1);
				tmp2.set(triangle[2]); tr.transform(tmp2);
				dispatchInfoPtr.debugDraw.drawLine(tmp1, tmp2, color);

				tmp1.set(triangle[2]); tr.transform(tmp1);
				tmp2.set(triangle[0]); tr.transform(tmp2);
				dispatchInfoPtr.debugDraw.drawLine(tmp1, tmp2, color);

				//btVector3 center = triangle[0] + triangle[1]+triangle[2];
				//center *= btScalar(0.333333);
				//m_dispatchInfoPtr->m_debugDraw->drawLine(tr(triangle[0]),tr(center),color);
				//m_dispatchInfoPtr->m_debugDraw->drawLine(tr(triangle[1]),tr(center),color);
				//m_dispatchInfoPtr->m_debugDraw->drawLine(tr(triangle[2]),tr(center),color);
			}

			//btCollisionObject* colObj = static_cast<btCollisionObject*>(m_convexProxy->m_clientObject);

			if (convexBody.getCollisionShape().isConvex()) {
				tm.init(triangle[0], triangle[1], triangle[2]);
				tm.setMargin(collisionMarginTriangle);

				CollisionShape tmpShape = ob.getCollisionShape();
				ob.setCollisionShape(tm);

				CollisionAlgorithm colAlgo = ci.dispatcher1.findAlgorithm(convexBody, triBody, manifoldPtr);
				// this should use the btDispatcher, so the actual registered algorithm is used
				//		btConvexConvexAlgorithm cvxcvxalgo(m_manifoldPtr,ci,m_convexBody,m_triBody);

				resultOut.setShapeIdentifiers(-1, -1, partId, triangleIndex);
				//cvxcvxalgo.setShapeIdentifiers(-1,-1,partId,triangleIndex);
				//cvxcvxalgo.processCollision(m_convexBody,m_triBody,*m_dispatchInfoPtr,m_resultOut);
				colAlgo.processCollision(convexBody, triBody, dispatchInfoPtr, resultOut);
				colAlgo.destroy();
				//ci.dispatcher1.freeCollisionAlgorithm(colAlgo);
				ob.setCollisionShape(tmpShape);
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void clearCache() {
		dispatcher.clearManifold(manifoldPtr);
	}

	public Vector3f getAabbMin() {
		return aabbMin;
	}

	public Vector3f getAabbMax() {
		return aabbMax;
	}
	
}
