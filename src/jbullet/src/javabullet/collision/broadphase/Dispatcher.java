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

import java.util.List;
import javabullet.collision.dispatch.CollisionObject;
import javabullet.collision.narrowphase.PersistentManifold;

/**
 * Dispatcher can be used in combination with broadphase to dispatch overlapping pairs.
 * For example for pairwise collision detection or user callbacks (game logic).
 * 
 * @author jezek2
 */
public abstract class Dispatcher {

	public final CollisionAlgorithm findAlgorithm(CollisionObject body0, CollisionObject body1) {
		return findAlgorithm(body0, body1, null);
	}

	public abstract CollisionAlgorithm findAlgorithm(CollisionObject body0, CollisionObject body1, PersistentManifold sharedManifold);

	public abstract PersistentManifold getNewManifold(Object body0, Object body1);

	public abstract void releaseManifold(PersistentManifold manifold);

	public abstract void clearManifold(PersistentManifold manifold);

	public abstract boolean needsCollision(CollisionObject body0, CollisionObject body1);

	public abstract boolean needsResponse(CollisionObject body0, CollisionObject body1);

	public abstract void dispatchAllCollisionPairs(OverlappingPairCache pairCache, DispatcherInfo dispatchInfo, Dispatcher dispatcher);

	public abstract int getNumManifolds();

	public abstract PersistentManifold getManifoldByIndexInternal(int index);

	public abstract List<PersistentManifold> getInternalManifoldPointer();

	//public abstract Object allocateCollisionAlgorithm(int size);

	//public abstract void freeCollisionAlgorithm(Object ptr);
	
}
