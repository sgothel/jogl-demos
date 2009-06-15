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

package javabullet.dynamics.constraintsolver;

import java.util.List;
import javabullet.BulletStack;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.dispatch.CollisionObject;
import javabullet.collision.narrowphase.PersistentManifold;
import javabullet.linearmath.IDebugDraw;

/**
 *
 * @author jezek2
 */
public abstract class ConstraintSolver {
	
	protected final BulletStack stack = BulletStack.get();

	public void prepareSolve (int numBodies, int numManifolds) {}

	/**
	 * Solve a group of constraints.
	 */
	public abstract float solveGroup(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifold, int manifold_offset, int numManifolds, List<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo info, IDebugDraw debugDrawer/*, btStackAlloc* stackAlloc*/, Dispatcher dispatcher);

	public void allSolved(ContactSolverInfo info, IDebugDraw debugDrawer/*, btStackAlloc* stackAlloc*/) {}

	/**
	 * Clear internal cached data and reset random seed.
	 */
	public abstract void reset();
	
}
