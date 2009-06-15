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

package javabullet.collision.broadphase;

import java.util.Collection;
import java.util.Iterator;
import javabullet.BulletGlobals;
import javabullet.BulletPool;
import javabullet.ObjectPool;
import javabullet.util.HashUtil;
import javabullet.util.HashUtil.IMap;
import javabullet.util.HashUtil.IObjectProcedure;

/**
 *
 * @author jezek2
 */
public class OverlappingPairCache {

	private final ObjectPool<BroadphasePair> pairsPool = BulletPool.get(BroadphasePair.class);
	
	private final IMap<BroadphasePair,BroadphasePair> overlappingPairs = HashUtil.createMap();
	private OverlapFilterCallback overlapFilterCallback;

	public OverlappingPairCache() {
	}

	/**
	 * Add a pair and return the new pair. If the pair already exists,
	 * no new pair is created and the old one is returned.
	 */
	public BroadphasePair addOverlappingPair(BroadphaseProxy proxy0, BroadphaseProxy proxy1) {
		BulletGlobals.gAddedPairs++;

		if (!needsBroadphaseCollision(proxy0, proxy1)) {
			return null;
		}

		BroadphasePair pair = pairsPool.get();
		pair.set(proxy0, proxy1);
		
		BroadphasePair old = overlappingPairs.get(pair);
		if (old != null) {
			pairsPool.release(pair);
			return old;
		}
		overlappingPairs.put(pair, pair);
		return pair;
	}

	public Object removeOverlappingPair(BroadphaseProxy proxy0, BroadphaseProxy proxy1, Dispatcher dispatcher) {
		BulletGlobals.gRemovePairs++;

		BroadphasePair key = pairsPool.get();
		key.set(proxy0, proxy1);
		BroadphasePair pair = overlappingPairs.remove(key);
		pairsPool.release(key);
		
		if (pair == null) {
			return null;
		}

		cleanOverlappingPair(pair, dispatcher);
		pairsPool.release(pair);

		return pair.userInfo;
	}

	public boolean needsBroadphaseCollision(BroadphaseProxy proxy0, BroadphaseProxy proxy1) {
		if (overlapFilterCallback != null) {
			return overlapFilterCallback.needBroadphaseCollision(proxy0, proxy1);
		}

		boolean collides = (proxy0.collisionFilterGroup & proxy1.collisionFilterMask) != 0;
		collides = collides && (proxy1.collisionFilterGroup & proxy0.collisionFilterMask) != 0;

		return collides;
	}

	private class ProcessAllOverlappingPairsCallback implements IObjectProcedure<BroadphasePair> {
		public OverlapCallback callback;
		public Dispatcher dispatcher;
		
		public boolean execute(BroadphasePair pair) {
			if (callback.processOverlap(pair)) {
				//removeOverlappingPair(pair.pProxy0, pair.pProxy1, dispatcher);
				cleanOverlappingPair(pair, dispatcher);
				BulletGlobals.gRemovePairs++;
				BulletGlobals.gOverlappingPairs--;
				pairsPool.release(pair);
				return false;
			}
			return true;
		}
	}
	
	private ProcessAllOverlappingPairsCallback processAllOverlappingPairsCallback = new ProcessAllOverlappingPairsCallback();
	
	public void processAllOverlappingPairs(OverlapCallback callback, Dispatcher dispatcher) {
		processAllOverlappingPairsCallback.callback = callback;
		processAllOverlappingPairsCallback.dispatcher = dispatcher;
		overlappingPairs.retainEntries(processAllOverlappingPairsCallback);
	}

	public void removeOverlappingPairsContainingProxy(BroadphaseProxy proxy, Dispatcher dispatcher) {
		processAllOverlappingPairs(new RemovePairCallback(proxy), dispatcher);
	}

	public void cleanProxyFromPairs(BroadphaseProxy proxy, Dispatcher dispatcher) {
		processAllOverlappingPairs(new CleanPairCallback(proxy, this, dispatcher), dispatcher);
	}

	public IMap<BroadphasePair,BroadphasePair> getOverlappingPairArray() {
		return overlappingPairs;
	}

	public void cleanOverlappingPair(BroadphasePair pair, Dispatcher dispatcher) {
		if (pair.algorithm != null) {
			pair.algorithm.destroy();
			// TODO: dispatcher.freeCollisionAlgorithm(pair.m_algorithm);
			pair.algorithm = null;
		}
	}

	public BroadphasePair findPair(BroadphaseProxy proxy0, BroadphaseProxy proxy1) {
		BulletGlobals.gFindPairs++;

		BroadphasePair key = pairsPool.get();
		key.set(proxy0, proxy1);
		BroadphasePair value = overlappingPairs.get(key);
		pairsPool.release(key);
		return value;
	}

	public int getCount() {
		return overlappingPairs.size();
	}

//	btBroadphasePair* GetPairs() { return m_pairs; }
	public OverlapFilterCallback getOverlapFilterCallback() {
		return overlapFilterCallback;
	}

	public void setOverlapFilterCallback(OverlapFilterCallback overlapFilterCallback) {
		this.overlapFilterCallback = overlapFilterCallback;
	}

	public int getNumOverlappingPairs() {
		return overlappingPairs.size();
	}
	
	////////////////////////////////////////////////////////////////////////////

	private static class RemovePairCallback implements OverlapCallback {
		private BroadphaseProxy obsoleteProxy;

		public RemovePairCallback(BroadphaseProxy obsoleteProxy) {
			this.obsoleteProxy = obsoleteProxy;
		}

		public boolean processOverlap(BroadphasePair pair) {
			return ((pair.pProxy0 == obsoleteProxy) ||
					(pair.pProxy1 == obsoleteProxy));
		}
	}

	private static class CleanPairCallback implements OverlapCallback {
		private BroadphaseProxy cleanProxy;
		private OverlappingPairCache pairCache;
		private Dispatcher dispatcher;

		public CleanPairCallback(BroadphaseProxy cleanProxy, OverlappingPairCache pairCache, Dispatcher dispatcher) {
			this.cleanProxy = cleanProxy;
			this.pairCache = pairCache;
			this.dispatcher = dispatcher;
		}

		public boolean processOverlap(BroadphasePair pair) {
			if ((pair.pProxy0 == cleanProxy) ||
					(pair.pProxy1 == cleanProxy)) {
				pairCache.cleanOverlappingPair(pair, dispatcher);
			}
			return false;
		}
	}
	
}
