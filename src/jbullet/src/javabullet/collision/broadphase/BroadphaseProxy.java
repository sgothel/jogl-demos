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
public class BroadphaseProxy {

	// Usually the client CollisionObject or Rigidbody class
	public Object clientObject;
	
	// TODO: mask
	public short collisionFilterGroup;
	public short collisionFilterMask;
	
	public int uniqueId; // uniqueId is introduced for paircache. could get rid of this, by calculating the address offset etc.

	public BroadphaseProxy() {
	}
	
	public BroadphaseProxy(Object userPtr, short collisionFilterGroup, short collisionFilterMask) {
		this.clientObject = userPtr;
		this.collisionFilterGroup = collisionFilterGroup;
		this.collisionFilterMask = collisionFilterMask;
	}

	public int getUid() {
		return uniqueId;
	}
	
}
