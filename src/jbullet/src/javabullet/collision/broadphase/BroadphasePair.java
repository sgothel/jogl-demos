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

/**
 *
 * @author jezek2
 */
public class BroadphasePair {

	public BroadphaseProxy pProxy0;
	public BroadphaseProxy pProxy1;
	public CollisionAlgorithm algorithm;
	public Object userInfo;

	public BroadphasePair() {
	}

	public void set(BroadphaseProxy pProxy0, BroadphaseProxy pProxy1) {
		this.pProxy0 = pProxy0;
		this.pProxy1 = pProxy1;
		this.algorithm = null;
		this.userInfo = null;
	}
	
	public void set(BroadphasePair p) {
		pProxy0 = p.pProxy0;
		pProxy1 = p.pProxy1;
		algorithm = p.algorithm;
		userInfo = p.userInfo;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BroadphasePair)) return false;
		BroadphasePair k = (BroadphasePair)obj;
		return (pProxy0 == k.pProxy0 && pProxy1 == k.pProxy1) || (pProxy0 == k.pProxy1 && pProxy1 == k.pProxy0);
	}

	@Override
	public int hashCode() {
		return pProxy0.hashCode() ^ pProxy1.hashCode();
	}
	
}
