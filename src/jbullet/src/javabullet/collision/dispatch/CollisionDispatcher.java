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
import java.util.Collections;
import java.util.List;
import javabullet.BulletPool;
import javabullet.ObjectPool;
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.collision.broadphase.BroadphasePair;
import javabullet.collision.broadphase.CollisionAlgorithm;
import javabullet.collision.broadphase.CollisionAlgorithmConstructionInfo;
import javabullet.collision.broadphase.DispatchFunc;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.broadphase.DispatcherInfo;
import javabullet.collision.broadphase.OverlapCallback;
import javabullet.collision.broadphase.OverlappingPairCache;
import javabullet.collision.narrowphase.PersistentManifold;

/**
 * CollisionDispatcher supports algorithms that handle ConvexConvex and ConvexConcave collision pairs.
 * Time of Impact, Closest Points and Penetration Depth.
 * 
 * @author jezek2
 */
public class CollisionDispatcher extends Dispatcher {
	
	protected final ObjectPool<PersistentManifold> manifoldsPool = BulletPool.get(PersistentManifold.class);

	private static final int MAX_BROADPHASE_COLLISION_TYPES = BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal();
	private int count = 0;
	private final List<PersistentManifold> manifoldsPtr = new ArrayList<PersistentManifold>();
	private boolean useIslands = true;
	private boolean staticWarningReported = false;
	private ManifoldResult defaultManifoldResult;
	private NearCallback nearCallback;
	//private PoolAllocator*	m_collisionAlgorithmPoolAllocator;
	//private PoolAllocator*	m_persistentManifoldPoolAllocator;
	private final CollisionAlgorithmCreateFunc[][] doubleDispatch = new CollisionAlgorithmCreateFunc[MAX_BROADPHASE_COLLISION_TYPES][MAX_BROADPHASE_COLLISION_TYPES];
	private CollisionConfiguration collisionConfiguration;
	private static int gNumManifold = 0;

	public CollisionDispatcher(CollisionConfiguration collisionConfiguration) {
		this.collisionConfiguration = collisionConfiguration;

		setNearCallback(new DefaultNearCallback());

		//m_collisionAlgorithmPoolAllocator = collisionConfiguration->getCollisionAlgorithmPool();
		//m_persistentManifoldPoolAllocator = collisionConfiguration->getPersistentManifoldPool();

		for (int i = 0; i < MAX_BROADPHASE_COLLISION_TYPES; i++) {
			for (int j = 0; j < MAX_BROADPHASE_COLLISION_TYPES; j++) {
				doubleDispatch[i][j] = collisionConfiguration.getCollisionAlgorithmCreateFunc(
					BroadphaseNativeType.forValue(i),
					BroadphaseNativeType.forValue(j)
				);
				assert (doubleDispatch[i][j] != null);
			}
		}
	}

	public void registerCollisionCreateFunc(int proxyType0, int proxyType1, CollisionAlgorithmCreateFunc createFunc) {
		doubleDispatch[proxyType0][proxyType1] = createFunc;
	}

	public NearCallback getNearCallback() {
		return nearCallback;
	}

	public void setNearCallback(NearCallback nearCallback) {
		this.nearCallback = nearCallback;
	}

	public CollisionConfiguration getCollisionConfiguration() {
		return collisionConfiguration;
	}

	public void setCollisionConfiguration(CollisionConfiguration collisionConfiguration) {
		this.collisionConfiguration = collisionConfiguration;
	}

	@Override
	public CollisionAlgorithm findAlgorithm(CollisionObject body0, CollisionObject body1, PersistentManifold sharedManifold) {
		CollisionAlgorithmConstructionInfo ci = new CollisionAlgorithmConstructionInfo();

		ci.dispatcher1 = this;
		ci.manifold = sharedManifold;
		CollisionAlgorithm algo = doubleDispatch[body0.getCollisionShape().getShapeType().ordinal()][body1.getCollisionShape().getShapeType().ordinal()].createCollisionAlgorithm(ci, body0, body1);

		return algo;
	}

	@Override
	public PersistentManifold getNewManifold(Object b0, Object b1) {
		gNumManifold++;

		//btAssert(gNumManifold < 65535);

		CollisionObject body0 = (CollisionObject)b0;
		CollisionObject body1 = (CollisionObject)b1;

		/*
		void* mem = 0;

		if (m_persistentManifoldPoolAllocator->getFreeCount())
		{
			mem = m_persistentManifoldPoolAllocator->allocate(sizeof(btPersistentManifold));
		} else
		{
			mem = btAlignedAlloc(sizeof(btPersistentManifold),16);

		}
		btPersistentManifold* manifold = new(mem) btPersistentManifold (body0,body1,0);
		manifold->m_index1a = m_manifoldsPtr.size();
		m_manifoldsPtr.push_back(manifold);
		*/
		
		PersistentManifold manifold = manifoldsPool.get();
		manifold.init(body0,body1,0);
		
		manifold.index1a = manifoldsPtr.size();
		manifoldsPtr.add(manifold);

		return manifold;
	}

	@Override
	public void releaseManifold(PersistentManifold manifold) {
		gNumManifold--;

		//printf("releaseManifold: gNumManifold %d\n",gNumManifold);
		clearManifold(manifold);

		// TODO: optimize
		int findIndex = manifold.index1a;
		assert (findIndex < manifoldsPtr.size());
		Collections.swap(manifoldsPtr, findIndex, manifoldsPtr.size()-1);
		manifoldsPtr.get(findIndex).index1a = findIndex;
		manifoldsPtr.remove(manifoldsPtr.size()-1);

		manifoldsPool.release(manifold);
		/*
		manifold->~btPersistentManifold();
		if (m_persistentManifoldPoolAllocator->validPtr(manifold))
		{
			m_persistentManifoldPoolAllocator->freeMemory(manifold);
		} else
		{
			btAlignedFree(manifold);
		}
		*/
	}

	@Override
	public void clearManifold(PersistentManifold manifold) {
		manifold.clearManifold();
	}

	@Override
	public boolean needsCollision(CollisionObject body0, CollisionObject body1) {
		assert (body0 != null);
		assert (body1 != null);

		boolean needsCollision = true;

		//#ifdef BT_DEBUG
		if (!staticWarningReported) {
			// broadphase filtering already deals with this
			if ((body0.isStaticObject() || body0.isKinematicObject()) &&
					(body1.isStaticObject() || body1.isKinematicObject())) {
				staticWarningReported = true;
				System.err.println("warning CollisionDispatcher.needsCollision: static-static collision!");
			}
		}
		//#endif //BT_DEBUG

		if ((!body0.isActive()) && (!body1.isActive())) {
			needsCollision = false;
		}
		else if (!body0.checkCollideWith(body1)) {
			needsCollision = false;
		}

		return needsCollision;
	}

	@Override
	public boolean needsResponse(CollisionObject body0, CollisionObject body1) {
		//here you can do filtering
		boolean hasResponse = (body0.hasContactResponse() && body1.hasContactResponse());
		//no response between two static/kinematic bodies:
		hasResponse = hasResponse && ((!body0.isStaticOrKinematicObject()) || (!body1.isStaticOrKinematicObject()));
		return hasResponse;
	}

	private static class CollisionPairCallback implements OverlapCallback {
		private DispatcherInfo dispatchInfo;
		private CollisionDispatcher dispatcher;

		public void init(DispatcherInfo dispatchInfo, CollisionDispatcher dispatcher) {
			this.dispatchInfo = dispatchInfo;
			this.dispatcher = dispatcher;
		}
		
		public boolean processOverlap(BroadphasePair pair) {
			dispatcher.getNearCallback().invoke(pair, dispatcher, dispatchInfo);
			return false;
		}
	}
	
	private CollisionPairCallback collisionPairCallback = new CollisionPairCallback();
	
	@Override
	public void dispatchAllCollisionPairs(OverlappingPairCache pairCache, DispatcherInfo dispatchInfo, Dispatcher dispatcher) {
		//m_blockedForChanges = true;
		collisionPairCallback.init(dispatchInfo, this);
		pairCache.processAllOverlappingPairs(collisionPairCallback, dispatcher);
		//m_blockedForChanges = false;
	}

	@Override
	public int getNumManifolds() {
		return manifoldsPtr.size();
	}

	@Override
	public PersistentManifold getManifoldByIndexInternal(int index) {
		return manifoldsPtr.get(index);
	}

	@Override
	public List<PersistentManifold> getInternalManifoldPointer() {
		return manifoldsPtr;
	}

	////////////////////////////////////////////////////////////////////////////
	
	private static class DefaultNearCallback implements NearCallback {
		private final ManifoldResult contactPointResult = new ManifoldResult();
		
		public void invoke(BroadphasePair collisionPair, CollisionDispatcher dispatcher, DispatcherInfo dispatchInfo) {
			CollisionObject colObj0 = (CollisionObject) collisionPair.pProxy0.clientObject;
			CollisionObject colObj1 = (CollisionObject) collisionPair.pProxy1.clientObject;

			if (dispatcher.needsCollision(colObj0, colObj1)) {
				// dispatcher will keep algorithms persistent in the collision pair
				if (collisionPair.algorithm == null) {
					collisionPair.algorithm = dispatcher.findAlgorithm(colObj0, colObj1);
				}

				if (collisionPair.algorithm != null) {
					//ManifoldResult contactPointResult = new ManifoldResult(colObj0, colObj1);
					contactPointResult.init(colObj0, colObj1);

					if (dispatchInfo.dispatchFunc == DispatchFunc.DISPATCH_DISCRETE) {
						// discrete collision detection query
						collisionPair.algorithm.processCollision(colObj0, colObj1, dispatchInfo, contactPointResult);
					}
					else {
						// continuous collision detection query, time of impact (toi)
						float toi = collisionPair.algorithm.calculateTimeOfImpact(colObj0, colObj1, dispatchInfo, contactPointResult);
						if (dispatchInfo.timeOfImpact > toi) {
							dispatchInfo.timeOfImpact = toi;
						}
					}
				}
			}
		}
	}
}
