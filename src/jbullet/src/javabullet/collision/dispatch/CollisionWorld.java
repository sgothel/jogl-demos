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
import javabullet.BulletGlobals;
import javabullet.BulletStack;
import javabullet.collision.broadphase.BroadphaseInterface;
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.collision.broadphase.BroadphaseProxy;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.broadphase.OverlappingPairCache;
import javabullet.collision.narrowphase.ConvexCast.CastResult;
import javabullet.collision.narrowphase.SubsimplexConvexCast;
import javabullet.collision.narrowphase.TriangleRaycastCallback;
import javabullet.collision.narrowphase.VoronoiSimplexSolver;
import javabullet.collision.shapes.BvhTriangleMeshShape;
import javabullet.collision.shapes.CollisionShape;
import javabullet.collision.shapes.CompoundShape;
import javabullet.collision.shapes.ConcaveShape;
import javabullet.collision.shapes.ConvexShape;
import javabullet.collision.shapes.SphereShape;
import javabullet.linearmath.AabbUtil2;
import javabullet.linearmath.IDebugDraw;
import javabullet.linearmath.Transform;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 * CollisionWorld is interface and container for the collision detection.
 * 
 * @author jezek2
 */
public class CollisionWorld {

	protected final BulletStack stack = BulletStack.get();
	
	protected List<CollisionObject> collisionObjects = new ArrayList<CollisionObject>();
	protected Dispatcher dispatcher1;
	protected DispatcherInfo dispatchInfo = new DispatcherInfo();
	//protected btStackAlloc*	m_stackAlloc;
	protected BroadphaseInterface broadphasePairCache;
	protected IDebugDraw debugDrawer;
	
	/**
	 * This constructor doesn't own the dispatcher and paircache/broadphase.
	 */
	public CollisionWorld(Dispatcher dispatcher,BroadphaseInterface broadphasePairCache, CollisionConfiguration collisionConfiguration) {
		this.dispatcher1 = dispatcher;
		this.broadphasePairCache = broadphasePairCache;
	}
	
	public void destroy() {
		// clean up remaining objects
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject collisionObject = collisionObjects.get(i);

			BroadphaseProxy bp = collisionObject.getBroadphaseHandle();
			if (bp != null) {
				//
				// only clear the cached algorithms
				//
				getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(bp, dispatcher1);
				getBroadphase().destroyProxy(bp, dispatcher1);
			}
		}
	}
	
	public void addCollisionObject(CollisionObject collisionObject) {
		addCollisionObject(collisionObject, (short)1, (short)1);
	}

	public void addCollisionObject(CollisionObject collisionObject, short collisionFilterGroup, short collisionFilterMask) {
		stack.pushCommonMath();
		try {
			// check that the object isn't already added
			assert (!collisionObjects.contains(collisionObject));

			collisionObjects.add(collisionObject);

			// calculate new AABB
			// TODO: check if it's overwritten or not
			Transform trans = stack.transforms.get(collisionObject.getWorldTransform());

			Vector3f minAabb = stack.vectors.get();
			Vector3f maxAabb = stack.vectors.get();
			collisionObject.getCollisionShape().getAabb(trans, minAabb, maxAabb);

			BroadphaseNativeType type = collisionObject.getCollisionShape().getShapeType();
			collisionObject.setBroadphaseHandle(getBroadphase().createProxy(
					minAabb,
					maxAabb,
					type,
					collisionObject,
					collisionFilterGroup,
					collisionFilterMask,
					dispatcher1));
		}
		finally {
			stack.popCommonMath();
		}
	}

	public void performDiscreteCollisionDetection() {
		BulletGlobals.pushProfile("performDiscreteCollisionDetection");
		try {
			//DispatcherInfo dispatchInfo = getDispatchInfo();

			updateAabbs();

			broadphasePairCache.calculateOverlappingPairs(dispatcher1);

			Dispatcher dispatcher = getDispatcher();
			{
				BulletGlobals.pushProfile("dispatchAllCollisionPairs");
				try {
					if (dispatcher != null) {
						dispatcher.dispatchAllCollisionPairs(broadphasePairCache.getOverlappingPairCache(), dispatchInfo, dispatcher1);
					}
				}
				finally {
					BulletGlobals.popProfile();
				}
			}
		}
		finally {
			BulletGlobals.popProfile();
		}
	}
	
	public void removeCollisionObject(CollisionObject collisionObject) {
		//bool removeFromBroadphase = false;

		{
			BroadphaseProxy bp = collisionObject.getBroadphaseHandle();
			if (bp != null) {
				//
				// only clear the cached algorithms
				//
				getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(bp, dispatcher1);
				getBroadphase().destroyProxy(bp, dispatcher1);
				collisionObject.setBroadphaseHandle(null);
			}
		}

		//swapremove
		collisionObjects.remove(collisionObject);
	}

	public BroadphaseInterface getBroadphase() {
		return broadphasePairCache;
	}
	
	public OverlappingPairCache getPairCache() {
		return broadphasePairCache.getOverlappingPairCache();
	}

	public Dispatcher getDispatcher() {
		return dispatcher1;
	}

	public DispatcherInfo getDispatchInfo() {
		return dispatchInfo;
	}
	
	private static boolean updateAabbs_reportMe = true;
	
	public void updateAabbs() {
		BulletGlobals.pushProfile("updateAabbs");
		stack.pushCommonMath();
		try {
			Transform predictedTrans = stack.transforms.get();
			Vector3f minAabb = stack.vectors.get(), maxAabb = stack.vectors.get();
			Vector3f tmp = stack.vectors.get();

			for (int i = 0; i < collisionObjects.size(); i++) {
				CollisionObject colObj = collisionObjects.get(i);

				// only update aabb of active objects
				if (colObj.isActive()) {
					colObj.getCollisionShape().getAabb(colObj.getWorldTransform(), minAabb, maxAabb);
					BroadphaseInterface bp = broadphasePairCache;

					// moving objects should be moderately sized, probably something wrong if not
					tmp.sub(maxAabb, minAabb); // TODO: optimize
					if (colObj.isStaticObject() || (tmp.lengthSquared() < 1e12f)) {
						bp.setAabb(colObj.getBroadphaseHandle(), minAabb, maxAabb, dispatcher1);
					}
					else {
						// something went wrong, investigate
						// this assert is unwanted in 3D modelers (danger of loosing work)
						colObj.setActivationState(CollisionObject.DISABLE_SIMULATION);

						if (updateAabbs_reportMe && debugDrawer != null) {
							updateAabbs_reportMe = false;
							debugDrawer.reportErrorWarning("Overflow in AABB, object removed from simulation");
							debugDrawer.reportErrorWarning("If you can reproduce this, please email bugs@continuousphysics.com\n");
							debugDrawer.reportErrorWarning("Please include above information, your Platform, version of OS.\n");
							debugDrawer.reportErrorWarning("Thanks.\n");
						}
					}
				}
			}
		}
		finally {
			stack.popCommonMath();
			BulletGlobals.popProfile();
		}
	}

	public IDebugDraw getDebugDrawer() {
		return debugDrawer;
	}

	public void setDebugDrawer(IDebugDraw debugDrawer) {
		this.debugDrawer = debugDrawer;
	}
	
	public int getNumCollisionObjects() {
		return collisionObjects.size();
	}

	// TODO
	public /*static*/ void rayTestSingle(Transform rayFromTrans, Transform rayToTrans,
			CollisionObject collisionObject,
			CollisionShape collisionShape,
			Transform colObjWorldTransform,
			RayResultCallback resultCallback, short collisionFilterMask) {
		stack.pushCommonMath();
		try {
			SphereShape pointShape = new SphereShape(0f);
			pointShape.setMargin(0f);
			ConvexShape castShape = pointShape;

			if (collisionShape.isConvex()) {
				CastResult castResult = new CastResult();
				castResult.fraction = resultCallback.closestHitFraction;

				ConvexShape convexShape = (ConvexShape) collisionShape;
				VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();

				//#define USE_SUBSIMPLEX_CONVEX_CAST 1
				//#ifdef USE_SUBSIMPLEX_CONVEX_CAST
				SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(castShape, convexShape, simplexSolver);
				//#else
				//btGjkConvexCast	convexCaster(castShape,convexShape,&simplexSolver);
				//btContinuousConvexCollision convexCaster(castShape,convexShape,&simplexSolver,0);
				//#endif //#USE_SUBSIMPLEX_CONVEX_CAST

				if (convexCaster.calcTimeOfImpact(rayFromTrans, rayToTrans, colObjWorldTransform, colObjWorldTransform, castResult)) {
					//add hit
					if (castResult.normal.lengthSquared() > 0.0001f) {
						if (castResult.fraction < resultCallback.closestHitFraction) {
							//#ifdef USE_SUBSIMPLEX_CONVEX_CAST
							//rotate normal into worldspace
							rayFromTrans.basis.transform(castResult.normal);
							//#endif //USE_SUBSIMPLEX_CONVEX_CAST

							castResult.normal.normalize();
							LocalRayResult localRayResult = new LocalRayResult(
									collisionObject,
									null,
									castResult.normal,
									castResult.fraction);

							boolean normalInWorldSpace = true;
							resultCallback.addSingleResult(localRayResult, normalInWorldSpace);
						}
					}
				}
			}
			else {
				if (collisionShape.isConcave()) {
					if (collisionShape.getShapeType() == BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE) {
						// optimized version for BvhTriangleMeshShape
						BvhTriangleMeshShape triangleMesh = (BvhTriangleMeshShape)collisionShape;
						Transform worldTocollisionObject = stack.transforms.get();
						worldTocollisionObject.inverse(colObjWorldTransform);
						Vector3f rayFromLocal = stack.vectors.get(rayFromTrans.origin);
						worldTocollisionObject.transform(rayFromLocal);
						Vector3f rayToLocal = stack.vectors.get(rayToTrans.origin);
						worldTocollisionObject.transform(rayToLocal);

						BridgeTriangleRaycastCallback rcb = new BridgeTriangleRaycastCallback(rayFromLocal, rayToLocal, resultCallback, collisionObject, triangleMesh);
						rcb.hitFraction = resultCallback.closestHitFraction;
						triangleMesh.performRaycast(rcb, rayFromLocal, rayToLocal);
					}
					else {
						ConcaveShape triangleMesh = (ConcaveShape)collisionShape;

						Transform worldTocollisionObject = stack.transforms.get();
						worldTocollisionObject.inverse(colObjWorldTransform);

						Vector3f rayFromLocal = stack.vectors.get(rayFromTrans.origin);
						worldTocollisionObject.transform(rayFromLocal);
						Vector3f rayToLocal = stack.vectors.get(rayToTrans.origin);
						worldTocollisionObject.transform(rayToLocal);

						BridgeTriangleRaycastCallback rcb = new BridgeTriangleRaycastCallback(rayFromLocal, rayToLocal, resultCallback, collisionObject, triangleMesh);
						rcb.hitFraction = resultCallback.closestHitFraction;

						Vector3f rayAabbMinLocal = stack.vectors.get(rayFromLocal);
						VectorUtil.setMin(rayAabbMinLocal, rayToLocal);
						Vector3f rayAabbMaxLocal = stack.vectors.get(rayFromLocal);
						VectorUtil.setMax(rayAabbMaxLocal, rayToLocal);

						triangleMesh.processAllTriangles(rcb, rayAabbMinLocal, rayAabbMaxLocal);
					}
				}
				else {
					// todo: use AABB tree or other BVH acceleration structure!
					if (collisionShape.isCompound()) {
						CompoundShape compoundShape = (CompoundShape) collisionShape;
						int i = 0;
						for (i = 0; i < compoundShape.getNumChildShapes(); i++) {
							Transform childTrans = stack.transforms.get(compoundShape.getChildTransform(i));
							CollisionShape childCollisionShape = compoundShape.getChildShape(i);
							Transform childWorldTrans = stack.transforms.get(colObjWorldTransform);
							childWorldTrans.mul(childTrans);
							rayTestSingle(rayFromTrans, rayToTrans,
									collisionObject,
									childCollisionShape,
									childWorldTrans,
									resultCallback, collisionFilterMask);
						}
					}
				}
			}
		}
		finally {
			stack.popCommonMath();
		}
	}
	
	public void rayTest(Vector3f rayFromWorld, Vector3f rayToWorld, RayResultCallback resultCallback) {
		rayTest(rayFromWorld, rayToWorld, resultCallback, (short)-1);
	}
	
	/**
	 * rayTest performs a raycast on all objects in the CollisionWorld, and calls the resultCallback.
	 * This allows for several queries: first hit, all hits, any hit, dependent on the value returned by the callback.
	 */
	public void rayTest(Vector3f rayFromWorld, Vector3f rayToWorld, RayResultCallback resultCallback, short collisionFilterMask) {
		stack.pushCommonMath();
		try {
			Transform rayFromTrans = stack.transforms.get(), rayToTrans = stack.transforms.get();
			rayFromTrans.setIdentity();
			rayFromTrans.origin.set(rayFromWorld);
			rayToTrans.setIdentity();

			rayToTrans.origin.set(rayToWorld);

			// go over all objects, and if the ray intersects their aabb, do a ray-shape query using convexCaster (CCD)
			Vector3f collisionObjectAabbMin = stack.vectors.get(), collisionObjectAabbMax = stack.vectors.get();
			float[] hitLambda = new float[1];

			for (int i = 0; i < collisionObjects.size(); i++) {
				// terminate further ray tests, once the closestHitFraction reached zero
				if (resultCallback.closestHitFraction == 0f) {
					break;
				}

				CollisionObject collisionObject = collisionObjects.get(i);
				// only perform raycast if filterMask matches
				if ((collisionObject.getBroadphaseHandle().collisionFilterGroup & collisionFilterMask) != 0) {
					//RigidcollisionObject* collisionObject = ctrl->GetRigidcollisionObject();
					collisionObject.getCollisionShape().getAabb(collisionObject.getWorldTransform(), collisionObjectAabbMin, collisionObjectAabbMax);

					hitLambda[0] = resultCallback.closestHitFraction;
					Vector3f hitNormal = stack.vectors.get();
					if (AabbUtil2.rayAabb(rayFromWorld, rayToWorld, collisionObjectAabbMin, collisionObjectAabbMax, hitLambda, hitNormal)) {
						rayTestSingle(rayFromTrans, rayToTrans,
								collisionObject,
								collisionObject.getCollisionShape(),
								collisionObject.getWorldTransform(),
								resultCallback,
								(short) -1);
					}
				}

			}
		}
		finally {
			stack.popCommonMath();
		}
	}
	
	public List<CollisionObject> getCollisionObjectArray() {
		return collisionObjects;
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * LocalShapeInfo gives extra information for complex shapes.
	 * Currently, only btTriangleMeshShape is available, so it just contains triangleIndex and subpart.
	 */
	public static class LocalShapeInfo {
		public int shapePart;
		public int triangleIndex;
		//const btCollisionShape*	m_shapeTemp;
		//const btTransform*	m_shapeLocalTransform;
	}
	
	public static class LocalRayResult {
		public CollisionObject collisionObject;
		public LocalShapeInfo localShapeInfo;
		public final Vector3f hitNormalLocal = new Vector3f();
		public float hitFraction;

		public LocalRayResult(CollisionObject collisionObject, LocalShapeInfo localShapeInfo, Vector3f hitNormalLocal, float hitFraction) {
			this.collisionObject = collisionObject;
			this.localShapeInfo = localShapeInfo;
			this.hitNormalLocal.set(hitNormalLocal);
			this.hitFraction = hitFraction;
		}
	}
	
	/**
	 * RayResultCallback is used to report new raycast results.
	 */
	public static abstract class RayResultCallback {
		public float closestHitFraction = 1f;
		public CollisionObject collisionObject;
		
		public boolean hasHit() {
			return (collisionObject != null);
		}
		
		public abstract float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace);
	}
	
	public static class ClosestRayResultCallback extends RayResultCallback {
		public final Vector3f rayFromWorld = new Vector3f(); //used to calculate hitPointWorld from hitFraction
		public final Vector3f rayToWorld = new Vector3f();

		public final Vector3f hitNormalWorld = new Vector3f();
		public final Vector3f hitPointWorld = new Vector3f();
		
		public ClosestRayResultCallback(Vector3f rayFromWorld, Vector3f rayToWorld) {
			this.rayFromWorld.set(rayFromWorld);
			this.rayToWorld.set(rayToWorld);
		}
		
		@Override
		public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
			// caller already does the filter on the closestHitFraction
			assert (rayResult.hitFraction <= closestHitFraction);

			closestHitFraction = rayResult.hitFraction;
			collisionObject = rayResult.collisionObject;
			if (normalInWorldSpace) {
				hitNormalWorld.set(rayResult.hitNormalLocal);
			}
			else {
				// need to transform normal into worldspace
				hitNormalWorld.set(rayResult.hitNormalLocal);
				collisionObject.getWorldTransform().basis.transform(hitNormalWorld);
			}

			VectorUtil.setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, rayResult.hitFraction);
			return rayResult.hitFraction;
		}
	}
	
	public static class LocalConvexResult {
		public CollisionObject hitCollisionObject;
		public LocalShapeInfo localShapeInfo;
		public final Vector3f hitNormalLocal = new Vector3f();
		public final Vector3f hitPointLocal = new Vector3f();
		public float hitFraction;

		public LocalConvexResult(CollisionObject hitCollisionObject, LocalShapeInfo localShapeInfo, Vector3f hitNormalLocal, Vector3f hitPointLocal, float hitFraction) {
			this.hitCollisionObject = hitCollisionObject;
			this.localShapeInfo = localShapeInfo;
			this.hitNormalLocal.set(hitNormalLocal);
			this.hitPointLocal.set(hitPointLocal);
			this.hitFraction = hitFraction;
		}
	}
	
	public static abstract class ConvexResultCallback {
		public float closestHitFraction = 1f;
		
		public boolean hasHit() {
			return (closestHitFraction < 1f);
		}
		
		public abstract float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace);
	}
	
	public static class ClosestConvexResultCallback extends ConvexResultCallback {
		public final Vector3f convexFromWorld = new Vector3f(); // used to calculate hitPointWorld from hitFraction
		public final Vector3f convexToWorld = new Vector3f();
		public final Vector3f hitNormalWorld = new Vector3f();
		public final Vector3f hitPointWorld = new Vector3f();
		public CollisionObject hitCollisionObject;

		@Override
		public float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace) {
			// caller already does the filter on the m_closestHitFraction
			assert (convexResult.hitFraction <= closestHitFraction);

			closestHitFraction = convexResult.hitFraction;
			hitCollisionObject = convexResult.hitCollisionObject;
			if (normalInWorldSpace) {
				hitNormalWorld.set(convexResult.hitNormalLocal);
			}
			else {
				// need to transform normal into worldspace
				hitNormalWorld.set(convexResult.hitNormalLocal);
				hitCollisionObject.getWorldTransform().basis.transform(hitNormalWorld);
			}

			hitPointWorld.set(convexResult.hitPointLocal);
			return convexResult.hitFraction;
		}
	}
	
	private static class BridgeTriangleRaycastCallback extends TriangleRaycastCallback {
		public RayResultCallback resultCallback;
		public CollisionObject collisionObject;
		public ConcaveShape triangleMesh;

		public BridgeTriangleRaycastCallback(Vector3f from, Vector3f to, RayResultCallback resultCallback, CollisionObject collisionObject, ConcaveShape triangleMesh) {
			super(from, to);
			this.resultCallback = resultCallback;
			this.collisionObject = collisionObject;
			this.triangleMesh = triangleMesh;
		}
	
		public float reportHit(Vector3f hitNormalLocal, float hitFraction, int partId, int triangleIndex) {
			LocalShapeInfo shapeInfo = new LocalShapeInfo();
			shapeInfo.shapePart = partId;
			shapeInfo.triangleIndex = triangleIndex;

			LocalRayResult rayResult = new LocalRayResult(collisionObject, shapeInfo, hitNormalLocal, hitFraction);

			boolean normalInWorldSpace = false;
			return resultCallback.addSingleResult(rayResult, normalInWorldSpace);
		}
	}
	
}
