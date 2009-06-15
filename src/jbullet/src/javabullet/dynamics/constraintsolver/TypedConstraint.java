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

import javabullet.BulletGlobals;
import javabullet.BulletStack;
import javabullet.dynamics.RigidBody;

/**
 * TypedConstraint is the baseclass for Bullet constraints and vehicles.
 * 
 * @author jezek2
 */
public abstract class TypedConstraint {
	
	protected final BulletStack stack = BulletStack.get();
	
	private static final RigidBody s_fixed = new RigidBody(0, null, null);

	private int userConstraintType = -1;
	private int userConstraintId = -1;

	private TypedConstraintType constraintType;
	
	protected RigidBody rbA;
	protected RigidBody rbB;
	protected float appliedImpulse = 0f;

	public TypedConstraint(TypedConstraintType type) {
		this(type, s_fixed, s_fixed);
	}
	
	public TypedConstraint(TypedConstraintType type, RigidBody rbA) {
		this(type, rbA, s_fixed);
	}
	
	public TypedConstraint(TypedConstraintType type, RigidBody rbA, RigidBody rbB) {
		this.constraintType = type;
		this.rbA = rbA;
		this.rbB = rbB;
		s_fixed.setMassProps(0f, BulletGlobals.ZERO_VECTOR3);
	}
	
	public abstract void buildJacobian();

	public abstract void solveConstraint(float timeStep);
	
	public RigidBody getRigidBodyA() {
		return rbA;
	}

	public RigidBody getRigidBodyB() {
		return rbB;
	}

	public int getUserConstraintType() {
		return userConstraintType;
	}

	public void setUserConstraintType(int userConstraintType) {
		this.userConstraintType = userConstraintType;
	}

	public int getUserConstraintId() {
		return userConstraintId;
	}

	public void setUserConstraintId(int userConstraintId) {
		this.userConstraintId = userConstraintId;
	}

	public float getAppliedImpulse() {
		return appliedImpulse;
	}

	public TypedConstraintType getConstraintType() {
		return constraintType;
	}
	
}
