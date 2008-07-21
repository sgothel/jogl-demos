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

package javabullet.dynamics;

import javabullet.collision.broadphase.BroadphaseInterface;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.dispatch.CollisionConfiguration;
import javabullet.collision.dispatch.CollisionWorld;
import javabullet.dynamics.constraintsolver.ConstraintSolver;
import javabullet.dynamics.constraintsolver.TypedConstraint;
import javabullet.dynamics.vehicle.RaycastVehicle;
import javax.vecmath.Vector3f;

/**
 * DynamicsWorld is the baseclass for several dynamics implementation, basic, discrete, parallel, and continuous.
 * 
 * @author jezek2
 */
public abstract class DynamicsWorld extends CollisionWorld {

	public DynamicsWorld(Dispatcher dispatcher, BroadphaseInterface broadphasePairCache, CollisionConfiguration collisionConfiguration) {
		super(dispatcher, broadphasePairCache, collisionConfiguration);
	}

	public final int stepSimulation(float timeStep) {
		return stepSimulation(timeStep, 1, 1f / 60f);
	}

	public final int stepSimulation(float timeStep, int maxSubSteps) {
		return stepSimulation(timeStep, maxSubSteps, 1f / 60f);
	}

	/**
	 * stepSimulation proceeds the simulation over timeStep units.
	 * if maxSubSteps > 0, it will interpolate time steps.
	 */
	public abstract int stepSimulation(float timeStep, int maxSubSteps, float fixedTimeStep);

	public abstract void debugDrawWorld();

	public final void addConstraint(TypedConstraint constraint) {
		addConstraint(constraint, false);
	}
	
	public void addConstraint(TypedConstraint constraint, boolean disableCollisionsBetweenLinkedBodies) {
	}

	public void removeConstraint(TypedConstraint constraint) {
	}

	public void addVehicle(RaycastVehicle vehicle) {
	}

	public void removeVehicle(RaycastVehicle vehicle) {
	}

	/**
	 * Once a rigidbody is added to the dynamics world, it will get this gravity assigned.
	 * Existing rigidbodies in the world get gravity assigned too, during this method.
	 */
	public abstract void setGravity(Vector3f gravity);

	public abstract void addRigidBody(RigidBody body);

	public abstract void removeRigidBody(RigidBody body);

	public abstract void setConstraintSolver(ConstraintSolver solver);

	public abstract ConstraintSolver getConstraintSolver();

	public int getNumConstraints() {
		return 0;
	}

	public TypedConstraint getConstraint(int index) {
		return null;
	}

	public abstract DynamicsWorldType getWorldType();

	public abstract void clearForces();
	
}
