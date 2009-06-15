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

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector3f;

/**
 * SimpleBroadphase is a brute force aabb culling broadphase based on O(n^2) aabb checks.
 * SimpleBroadphase is just a unit-test implementation to verify and test other broadphases.
 * So please don't use this class, but use bt32BitAxisSweep3 or btAxisSweep3 instead!
 * 
 * @author jezek2
 */
public class SimpleBroadphase implements BroadphaseInterface {

	private final List<SimpleBroadphaseProxy> handles = new ArrayList<SimpleBroadphaseProxy>();
	private int maxHandles;						// max number of handles
	private OverlappingPairCache pairCache;
	private boolean ownsPairCache;

	public SimpleBroadphase() {
		this(16384, null);
	}

	public SimpleBroadphase(int maxProxies) {
		this(maxProxies, null);
	}
	
	public SimpleBroadphase(int maxProxies, OverlappingPairCache overlappingPairCache) {
		this.pairCache = overlappingPairCache;

		if (overlappingPairCache == null) {
			pairCache = new OverlappingPairCache();
			ownsPairCache = true;
		}
	}

	public BroadphaseProxy createProxy(Vector3f aabbMin, Vector3f aabbMax, BroadphaseNativeType shapeType, Object userPtr, short collisionFilterGroup, short collisionFilterMask, Dispatcher dispatcher) {
		assert (aabbMin.x <= aabbMax.x && aabbMin.y <= aabbMax.y && aabbMin.z <= aabbMax.z);

		SimpleBroadphaseProxy proxy = new SimpleBroadphaseProxy(aabbMin, aabbMax, shapeType, userPtr, collisionFilterGroup, collisionFilterMask);
		proxy.uniqueId = handles.size();
		handles.add(proxy);
		return proxy;
	}

	public void destroyProxy(BroadphaseProxy proxyOrg, Dispatcher dispatcher) {
		handles.remove(proxyOrg);

		pairCache.removeOverlappingPairsContainingProxy(proxyOrg, dispatcher);
	}

	public void setAabb(BroadphaseProxy proxy, Vector3f aabbMin, Vector3f aabbMax, Dispatcher dispatcher) {
		SimpleBroadphaseProxy sbp = (SimpleBroadphaseProxy)proxy;
		sbp.min.set(aabbMin);
		sbp.max.set(aabbMax);
	}

	private static boolean aabbOverlap(SimpleBroadphaseProxy proxy0, SimpleBroadphaseProxy proxy1) {
		return proxy0.min.x <= proxy1.max.x && proxy1.min.x <= proxy0.max.x &&
				proxy0.min.y <= proxy1.max.y && proxy1.min.y <= proxy0.max.y &&
				proxy0.min.z <= proxy1.max.z && proxy1.min.z <= proxy0.max.z;
	}

	public void calculateOverlappingPairs(Dispatcher dispatcher) {
		for (int i=0; i<handles.size(); i++) {
			SimpleBroadphaseProxy proxy0 = handles.get(i);
			for (int j=0; j<handles.size(); j++) {
				SimpleBroadphaseProxy proxy1 = handles.get(j);
				if (proxy0 == proxy1) continue;
				
				if (aabbOverlap(proxy0, proxy1)) {
					if (pairCache.findPair(proxy0, proxy1) == null) {
						pairCache.addOverlappingPair(proxy0, proxy1);
					}
				}
				else {
					if (pairCache.findPair(proxy0, proxy1) != null) {
						pairCache.removeOverlappingPair(proxy0, proxy1, dispatcher);
					}
				}
			}
		}
	}

	public OverlappingPairCache getOverlappingPairCache() {
		return pairCache;
	}
}
