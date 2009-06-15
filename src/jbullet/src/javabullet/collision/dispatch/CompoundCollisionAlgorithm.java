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

import java.util.ArrayList;
import java.util.List;
import javabullet.collision.broadphase.CollisionAlgorithm;
import javabullet.collision.broadphase.CollisionAlgorithmConstructionInfo;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.shapes.CollisionShape;
import javabullet.collision.shapes.CompoundShape;
import javabullet.linearmath.Transform;

/**
 * CompoundCollisionAlgorithm  supports collision between CompoundCollisionShapes and other collision shapes.
 * Place holder, not fully implemented yet.
 * 
 * @author jezek2
 */
public class CompoundCollisionAlgorithm extends CollisionAlgorithm {

	private final List<CollisionAlgorithm> childCollisionAlgorithms = new ArrayList<CollisionAlgorithm>();
	private boolean isSwapped;
	
	public CompoundCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1, boolean isSwapped) {
		super(ci);
		this.isSwapped = isSwapped;

		CollisionObject colObj = isSwapped ? body1 : body0;
		CollisionObject otherObj = isSwapped ? body0 : body1;
		assert (colObj.getCollisionShape().isCompound());

		CompoundShape compoundShape = (CompoundShape) colObj.getCollisionShape();
		int numChildren = compoundShape.getNumChildShapes();
		int i;

		//childCollisionAlgorithms.resize(numChildren);
		for (i = 0; i < numChildren; i++) {
			CollisionShape childShape = compoundShape.getChildShape(i);
			CollisionShape orgShape = colObj.getCollisionShape();
			colObj.setCollisionShape(childShape);
			childCollisionAlgorithms.add(ci.dispatcher1.findAlgorithm(colObj, otherObj));
			colObj.setCollisionShape(orgShape);
		}
	}

	@Override
	public void destroy() {
		int numChildren = childCollisionAlgorithms.size();
		int i;
		for (i = 0; i < numChildren; i++) {
			childCollisionAlgorithms.get(i).destroy();
			//m_dispatcher->freeCollisionAlgorithm(m_childCollisionAlgorithms[i]);
		}
	}
	
	@Override
	public void processCollision(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		stack.transforms.push();
		try {
			CollisionObject colObj = isSwapped ? body1 : body0;
			CollisionObject otherObj = isSwapped ? body0 : body1;

			assert (colObj.getCollisionShape().isCompound());
			CompoundShape compoundShape = (CompoundShape) colObj.getCollisionShape();

			// We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
			// If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
			// given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
			// determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
			// then use each overlapping node AABB against Tree0
			// and vise versa.

			Transform tmpTrans = stack.transforms.get();
			Transform orgTrans = stack.transforms.get();

			int numChildren = childCollisionAlgorithms.size();
			int i;
			for (i = 0; i < numChildren; i++) {
				// temporarily exchange parent btCollisionShape with childShape, and recurse
				CollisionShape childShape = compoundShape.getChildShape(i);

				// backup
				orgTrans.set(colObj.getWorldTransform());
				CollisionShape orgShape = colObj.getCollisionShape();

				Transform childTrans = compoundShape.getChildTransform(i);
				//btTransform	newChildWorldTrans = orgTrans*childTrans ;
				tmpTrans.set(orgTrans);
				tmpTrans.mul(childTrans);
				colObj.setWorldTransform(tmpTrans);
				// the contactpoint is still projected back using the original inverted worldtrans
				colObj.setCollisionShape(childShape);
				childCollisionAlgorithms.get(i).processCollision(colObj, otherObj, dispatchInfo, resultOut);
				// revert back
				colObj.setCollisionShape(orgShape);
				colObj.setWorldTransform(orgTrans);
			}
		}
		finally {
			stack.transforms.pop();
		}
	}

	@Override
	public float calculateTimeOfImpact(CollisionObject body0, CollisionObject body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		stack.transforms.push();
		try {
			CollisionObject colObj = isSwapped ? body1 : body0;
			CollisionObject otherObj = isSwapped ? body0 : body1;

			assert (colObj.getCollisionShape().isCompound());

			CompoundShape compoundShape = (CompoundShape) colObj.getCollisionShape();

			// We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
			// If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
			// given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
			// determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
			// then use each overlapping node AABB against Tree0
			// and vise versa.

			Transform tmpTrans = stack.transforms.get();
			Transform orgTrans = stack.transforms.get();
			float hitFraction = 1f;

			int numChildren = childCollisionAlgorithms.size();
			int i;
			for (i = 0; i < numChildren; i++) {
				// temporarily exchange parent btCollisionShape with childShape, and recurse
				CollisionShape childShape = compoundShape.getChildShape(i);

				// backup
				orgTrans.set(colObj.getWorldTransform());
				CollisionShape orgShape = colObj.getCollisionShape();

				Transform childTrans = compoundShape.getChildTransform(i);
				//btTransform	newChildWorldTrans = orgTrans*childTrans ;
				tmpTrans.set(orgTrans);
				tmpTrans.mul(childTrans);
				colObj.setWorldTransform(tmpTrans);

				colObj.setCollisionShape(childShape);
				float frac = childCollisionAlgorithms.get(i).calculateTimeOfImpact(colObj, otherObj, dispatchInfo, resultOut);
				if (frac < hitFraction) {
					hitFraction = frac;
				}
				// revert back
				colObj.setCollisionShape(orgShape);
				colObj.setWorldTransform(orgTrans);
			}
			return hitFraction;
		}
		finally {
			stack.transforms.pop();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	public static final CollisionAlgorithmCreateFunc createFunc = new CollisionAlgorithmCreateFunc() {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			return new CompoundCollisionAlgorithm(ci, body0, body1, false);
		}
	};
	
	public static final CollisionAlgorithmCreateFunc swappedCreateFunc = new CollisionAlgorithmCreateFunc() {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, CollisionObject body0, CollisionObject body1) {
			return new CompoundCollisionAlgorithm(ci, body0, body1, true);
		}
	};

}
