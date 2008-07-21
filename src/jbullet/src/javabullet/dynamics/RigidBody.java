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

import java.util.ArrayList;
import java.util.List;
import javabullet.BulletGlobals;
import javabullet.collision.broadphase.BroadphaseProxy;
import javabullet.collision.dispatch.CollisionFlags;
import javabullet.collision.dispatch.CollisionObject;
import javabullet.collision.shapes.CollisionShape;
import javabullet.dynamics.constraintsolver.TypedConstraint;
import javabullet.linearmath.MatrixUtil;
import javabullet.linearmath.MiscUtil;
import javabullet.linearmath.MotionState;
import javabullet.linearmath.Transform;
import javabullet.linearmath.TransformUtil;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * RigidBody is the main class for rigid body objects. It is derived from btCollisionObject, so it keeps a pointer to a btCollisionShape.
 * It is recommended for performance and memory use to share btCollisionShape objects whenever possible.<p>
 * 
 * There are 3 types of rigid bodies:<br>
 * - A) Dynamic rigid bodies, with positive mass. Motion is controlled by rigid body dynamics.<br>
 * - B) Fixed objects with zero mass. They are not moving (basically collision objects)<br>
 * - C) Kinematic objects, which are objects without mass, but the user can move them. There is on-way interaction, and Bullet calculates a velocity based on the timestep and previous and current world transform.<p>
 * 
 * Bullet automatically deactivates dynamic rigid bodies, when the velocity is below a threshold for a given time.<p>
 * Deactivated (sleeping) rigid bodies don't take any processing time, except a minor broadphase collision detection impact (to allow active objects to activate/wake up sleeping objects).
 * 
 * @author jezek2
 */
public class RigidBody extends CollisionObject {

	private static final float MAX_ANGVEL = BulletGlobals.SIMD_HALF_PI;
	
	private final Matrix3f invInertiaTensorWorld = new Matrix3f();
	private final Vector3f linearVelocity = new Vector3f();
	private final Vector3f angularVelocity = new Vector3f();
	private float inverseMass;
	private float angularFactor;

	private final Vector3f gravity = new Vector3f();
	private final Vector3f invInertiaLocal = new Vector3f();
	private final Vector3f totalForce = new Vector3f();
	private final Vector3f totalTorque = new Vector3f();
	
	private float linearDamping;
	private float angularDamping;

	private boolean additionalDamping;
	private float additionalDampingFactor;
	private float additionalLinearDampingThresholdSqr;
	private float additionalAngularDampingThresholdSqr;
	private float additionalAngularDampingFactor;

	private float linearSleepingThreshold;
	private float angularSleepingThreshold;

	// optionalMotionState allows to automatic synchronize the world transform for active objects
	private MotionState optionalMotionState;

	// keep track of typed constraints referencing this rigid body
	private final List<TypedConstraint> constraintRefs = new ArrayList<TypedConstraint>();

	// for experimental overriding of friction/contact solver func
	public int contactSolverType;
	public int frictionSolverType;
	
	private static int uniqueId = 0;
	public int debugBodyId;
	
	public RigidBody(RigidBodyConstructionInfo constructionInfo) {
		setupRigidBody(constructionInfo);
	}

	public RigidBody(float mass, MotionState motionState, CollisionShape collisionShape) {
		this(mass, motionState, collisionShape, BulletGlobals.ZERO_VECTOR3);
	}
	
	public RigidBody(float mass, MotionState motionState, CollisionShape collisionShape, Vector3f localInertia) {
		RigidBodyConstructionInfo cinfo = new RigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia);
		setupRigidBody(cinfo);
	}
	
	private void setupRigidBody(RigidBodyConstructionInfo constructionInfo) {
		linearVelocity.set(0f, 0f, 0f);
		angularVelocity.set(0f, 0f, 0f);
		angularFactor = 1f;
		gravity.set(0f, 0f, 0f);
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);
		linearDamping = 0f;
		angularDamping = 0.5f;
		linearSleepingThreshold = constructionInfo.linearSleepingThreshold;
		angularSleepingThreshold = constructionInfo.angularSleepingThreshold;
		optionalMotionState = constructionInfo.motionState;
		contactSolverType = 0;
		frictionSolverType = 0;
		additionalDamping = constructionInfo.additionalDamping;

		additionalLinearDampingThresholdSqr = constructionInfo.additionalLinearDampingThresholdSqr;
		additionalAngularDampingThresholdSqr = constructionInfo.additionalAngularDampingThresholdSqr;
		additionalAngularDampingFactor = constructionInfo.additionalAngularDampingFactor;

		if (optionalMotionState != null)
		{
			optionalMotionState.getWorldTransform(worldTransform);
		} else
		{
			worldTransform.set(constructionInfo.startWorldTransform);
		}

		interpolationWorldTransform.set(worldTransform);
		interpolationLinearVelocity.set(0f, 0f, 0f);
		interpolationAngularVelocity.set(0f, 0f, 0f);

		// moved to CollisionObject
		friction = constructionInfo.friction;
		restitution = constructionInfo.restitution;

		collisionShape = constructionInfo.collisionShape;
		debugBodyId = uniqueId++;

		// internalOwner is to allow upcasting from collision object to rigid body
		internalOwner = this;

		setMassProps(constructionInfo.mass, constructionInfo.localInertia);
		setDamping(constructionInfo.linearDamping, constructionInfo.angularDamping);
		updateInertiaTensor();
	}
	
	public void destroy() {
		// No constraints should point to this rigidbody
		// Remove constraints from the dynamics world before you delete the related rigidbodies. 
		assert (constraintRefs.size() == 0);
	}

	public void proceedToTransform(Transform newTrans) {
		setCenterOfMassTransform(newTrans);
	}
	
	/**
	 * To keep collision detection and dynamics separate we don't store a rigidbody pointer,
	 * but a rigidbody is derived from CollisionObject, so we can safely perform an upcast.
	 */
	public static RigidBody upcast(CollisionObject colObj) {
		return (RigidBody) colObj.getInternalOwner();
	}

	/**
	 * Continuous collision detection needs prediction.
	 */
	public void predictIntegratedTransform(float timeStep, Transform predictedTransform) {
		TransformUtil.integrateTransform(worldTransform, linearVelocity, angularVelocity, timeStep, predictedTransform);
	}
	
	public void saveKinematicState(float timeStep) {
		//todo: clamp to some (user definable) safe minimum timestep, to limit maximum angular/linear velocities
		if (timeStep != 0f) {
			//if we use motionstate to synchronize world transforms, get the new kinematic/animated world transform
			if (getMotionState() != null) {
				getMotionState().getWorldTransform(worldTransform);
			}
			//Vector3f linVel = new Vector3f(), angVel = new Vector3f();

			TransformUtil.calculateVelocity(interpolationWorldTransform, worldTransform, timeStep, linearVelocity, angularVelocity);
			interpolationLinearVelocity.set(linearVelocity);
			interpolationAngularVelocity.set(angularVelocity);
			interpolationWorldTransform.set(worldTransform);
		//printf("angular = %f %f %f\n",m_angularVelocity.getX(),m_angularVelocity.getY(),m_angularVelocity.getZ());
		}
	}
	
	public void applyGravity() {
		if (isStaticOrKinematicObject())
			return;

		applyCentralForce(gravity);
	}
	
	public void setGravity(Vector3f acceleration) {
		if (inverseMass != 0f) {
			gravity.scale(1f / inverseMass, acceleration);
		}
	}

	public Vector3f getGravity() {
		return gravity;
	}

	public void setDamping(float lin_damping, float ang_damping) {
		linearDamping = MiscUtil.GEN_clamped(lin_damping, 0f, 1f);
		angularDamping = MiscUtil.GEN_clamped(ang_damping, 0f, 1f);
	}

	/**
	 * Damps the velocity, using the given linearDamping and angularDamping.
	 */
	public void applyDamping(float timeStep) {
		stack.vectors.push();
		try {
			linearVelocity.scale(MiscUtil.GEN_clamped((1f - timeStep * linearDamping), 0f, 1f));
			angularVelocity.scale(MiscUtil.GEN_clamped((1f - timeStep * angularDamping), 0f, 1f));

			if (additionalDamping) {
				// Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
				// Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics system has improved, this should become obsolete
				if ((angularVelocity.lengthSquared() < additionalAngularDampingThresholdSqr) &&
						(linearVelocity.lengthSquared() < additionalLinearDampingThresholdSqr)) {
					angularVelocity.scale(additionalDampingFactor);
					linearVelocity.scale(additionalDampingFactor);
				}

				float speed = linearVelocity.length();
				if (speed < linearDamping) {
					float dampVel = 0.005f;
					if (speed > dampVel) {
						Vector3f dir = stack.vectors.get(linearVelocity);
						dir.normalize();
						dir.scale(dampVel);
						linearVelocity.sub(dir);
					}
					else {
						linearVelocity.set(0f, 0f, 0f);
					}
				}

				float angSpeed = angularVelocity.length();
				if (angSpeed < angularDamping) {
					float angDampVel = 0.005f;
					if (angSpeed > angDampVel) {
						Vector3f dir = stack.vectors.get(angularVelocity);
						dir.normalize();
						dir.scale(angDampVel);
						angularVelocity.sub(dir);
					}
					else {
						angularVelocity.set(0f, 0f, 0f);
					}
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void setMassProps(float mass, Vector3f inertia) {
		if (mass == 0f) {
			collisionFlags |= CollisionFlags.STATIC_OBJECT;
			inverseMass = 0f;
		}
		else {
			collisionFlags &= (~CollisionFlags.STATIC_OBJECT);
			inverseMass = 1f / mass;
		}

		invInertiaLocal.set(inertia.x != 0f ? 1f / inertia.x : 0f,
				inertia.y != 0f ? 1f / inertia.y : 0f,
				inertia.z != 0f ? 1f / inertia.z : 0f);
	}

	public float getInvMass() {
		return inverseMass;
	}

	public Matrix3f getInvInertiaTensorWorld() {
		return invInertiaTensorWorld;
	}
	
	public void integrateVelocities(float step) {
		if (isStaticOrKinematicObject()) {
			return;
		}

		stack.vectors.push();
		try {
			linearVelocity.scaleAdd(inverseMass * step, totalForce, linearVelocity);
			Vector3f tmp = stack.vectors.get(totalTorque);
			invInertiaTensorWorld.transform(tmp);
			angularVelocity.scaleAdd(step, tmp, angularVelocity);

			// clamp angular velocity. collision calculations will fail on higher angular velocities	
			float angvel = angularVelocity.length();
			if (angvel * step > MAX_ANGVEL) {
				angularVelocity.scale((MAX_ANGVEL / step) / angvel);
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void setCenterOfMassTransform(Transform xform) {
		if (isStaticOrKinematicObject()) {
			interpolationWorldTransform.set(worldTransform);
		}
		else {
			interpolationWorldTransform.set(xform);
		}
		interpolationLinearVelocity.set(getLinearVelocity());
		interpolationAngularVelocity.set(getAngularVelocity());
		worldTransform.set(xform);
		updateInertiaTensor();
	}

	public void applyCentralForce(Vector3f force) {
		totalForce.add(force);
	}
	
	public Vector3f getInvInertiaDiagLocal() {
		return invInertiaLocal;
	}

	public void setInvInertiaDiagLocal(Vector3f diagInvInertia) {
		invInertiaLocal.set(diagInvInertia);
	}

	public void setSleepingThresholds(float linear, float angular) {
		linearSleepingThreshold = linear;
		angularSleepingThreshold = angular;
	}

	public void applyTorque(Vector3f torque) {
		totalTorque.add(torque);
	}

	public void applyForce(Vector3f force, Vector3f rel_pos) {
		stack.vectors.push();
		try {
			applyCentralForce(force);
			Vector3f tmp = stack.vectors.get();
			tmp.cross(rel_pos, force);
			applyTorque(tmp);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void applyCentralImpulse(Vector3f impulse) {
		linearVelocity.scaleAdd(inverseMass, impulse, linearVelocity);
	}
	
	public void applyTorqueImpulse(Vector3f torque) {
		stack.vectors.push();
		try {
			Vector3f tmp = stack.vectors.get(torque);
			invInertiaTensorWorld.transform(tmp);
			angularVelocity.add(tmp);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void applyImpulse(Vector3f impulse, Vector3f rel_pos) {
		stack.vectors.push();
		try {
			if (inverseMass != 0f) {
				applyCentralImpulse(impulse);
				if (angularFactor != 0f) {
					Vector3f tmp = stack.vectors.get();
					tmp.cross(rel_pos, impulse);
					tmp.scale(angularFactor);
					applyTorqueImpulse(tmp);
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	/**
	 * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
	 */
	public void internalApplyImpulse(Vector3f linearComponent, Vector3f angularComponent, float impulseMagnitude) {
		if (inverseMass != 0f) {
			linearVelocity.scaleAdd(impulseMagnitude, linearComponent, linearVelocity);
			if (angularFactor != 0f) {
				angularVelocity.scaleAdd(impulseMagnitude * angularFactor, angularComponent, angularVelocity);
			}
		}
	}

	public void clearForces() {
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);
	}
	
	public void updateInertiaTensor() {
		stack.matrices.push();
		try {
			Matrix3f mat1 = stack.matrices.get();
			MatrixUtil.scale(mat1, worldTransform.basis, invInertiaLocal);

			Matrix3f mat2 = stack.matrices.get(worldTransform.basis);
			mat2.transpose();

			invInertiaTensorWorld.mul(mat1, mat2);
		}
		finally {
			stack.matrices.pop();
		}
	}
	
	public Vector3f getCenterOfMassPosition() {
		return worldTransform.origin;
	}

	public Quat4f getOrientation() {
		stack.quats.push();
		try {
			Quat4f orn = stack.quats.get();
			MatrixUtil.getRotation(worldTransform.basis, orn);
			return stack.quats.returning(orn);
		}
		finally {
			stack.quats.pop();
		}
	}
	
	public Transform getCenterOfMassTransform() {
		return worldTransform;
	}

	public Vector3f getLinearVelocity() {
		return linearVelocity;
	}

	public Vector3f getAngularVelocity() {
		return angularVelocity;
	}

	public void setLinearVelocity(Vector3f lin_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		linearVelocity.set(lin_vel);
	}

	public void setAngularVelocity(Vector3f ang_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		angularVelocity.set(ang_vel);
	}

	public Vector3f getVelocityInLocalPoint(Vector3f rel_pos) {
		stack.vectors.push();
		try {
			// we also calculate lin/ang velocity for kinematic objects
			Vector3f vec = stack.vectors.get();
			vec.cross(angularVelocity, rel_pos);
			vec.add(linearVelocity);
			return stack.vectors.returning(vec);

			//for kinematic objects, we could also use use:
			//		return 	(m_worldTransform(rel_pos) - m_interpolationWorldTransform(rel_pos)) / m_kinematicTimeStep;
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void translate(Vector3f v) {
		worldTransform.origin.add(v);
	}
	
	public void getAabb(Vector3f aabbMin, Vector3f aabbMax) {
		getCollisionShape().getAabb(worldTransform, aabbMin, aabbMax);
	}

	public float computeImpulseDenominator(Vector3f pos, Vector3f normal) {
		stack.vectors.push();
		try {
			Vector3f r0 = stack.vectors.get();
			r0.sub(pos, getCenterOfMassPosition());

			Vector3f c0 = stack.vectors.get();
			c0.cross(r0, normal);

			Vector3f tmp = stack.vectors.get();
			MatrixUtil.transposeTransform(tmp, c0, getInvInertiaTensorWorld());

			Vector3f vec = stack.vectors.get();
			vec.cross(tmp, r0);

			return inverseMass + normal.dot(vec);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public float computeAngularImpulseDenominator(Vector3f axis) {
		stack.vectors.push();
		try {
			Vector3f vec = stack.vectors.get();
			MatrixUtil.transposeTransform(vec, axis, getInvInertiaTensorWorld());
			return axis.dot(vec);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void updateDeactivation(float timeStep) {
		if ((getActivationState() == ISLAND_SLEEPING) || (getActivationState() == DISABLE_DEACTIVATION)) {
			return;
		}

		if ((getLinearVelocity().lengthSquared() < linearSleepingThreshold * linearSleepingThreshold) &&
				(getAngularVelocity().lengthSquared() < angularSleepingThreshold * angularSleepingThreshold)) {
			deactivationTime += timeStep;
		}
		else {
			deactivationTime = 0f;
			setActivationState(0);
		}
	}

	public boolean wantsSleeping() {
		if (getActivationState() == DISABLE_DEACTIVATION) {
			return false;
		}

		// disable deactivation
		if (BulletGlobals.gDisableDeactivation || (BulletGlobals.gDeactivationTime == 0f)) {
			return false;
		}

		if ((getActivationState() == ISLAND_SLEEPING) || (getActivationState() == WANTS_DEACTIVATION)) {
			return true;
		}

		if (deactivationTime > BulletGlobals.gDeactivationTime) {
			return true;
		}
		return false;
	}
	
	public BroadphaseProxy getBroadphaseProxy() {
		return broadphaseHandle;
	}

	public void setNewBroadphaseProxy(BroadphaseProxy broadphaseProxy) {
		this.broadphaseHandle = broadphaseProxy;
	}

	public MotionState getMotionState() {
		return optionalMotionState;
	}

	public void setMotionState(MotionState motionState) {
		this.optionalMotionState = motionState;
		if (optionalMotionState != null) {
			motionState.getWorldTransform(worldTransform);
		}
	}

	public void setAngularFactor(float angFac) {
		angularFactor = angFac;
	}

	public float getAngularFactor() {
		return angularFactor;
	}

	/**
	 * Is this rigidbody added to a CollisionWorld/DynamicsWorld/Broadphase?
	 */
	public boolean isInWorld() {
		return (getBroadphaseProxy() != null);
	}

	@Override
	public boolean checkCollideWithOverride(CollisionObject co) {
		// TODO: change to cast
		RigidBody otherRb = RigidBody.upcast(co);
		if (otherRb == null) {
			return true;
		}

		for (int i = 0; i < constraintRefs.size(); ++i) {
			TypedConstraint c = constraintRefs.get(i);
			if (c.getRigidBodyA() == otherRb || c.getRigidBodyB() == otherRb) {
				return false;
			}
		}

		return true;
	}

	public void addConstraintRef(TypedConstraint c) {
		int index = constraintRefs.indexOf(c);
		if (index == -1) {
			constraintRefs.add(c);
		}

		checkCollideWith = true;
	}
	
	public void removeConstraintRef(TypedConstraint c) {
		constraintRefs.remove(c);
		checkCollideWith = (constraintRefs.size() > 0);
	}

	public TypedConstraint getConstraintRef(int index) {
		return constraintRefs.get(index);
	}

	public int getNumConstraintRefs() {
		return constraintRefs.size();
	}
	
}
