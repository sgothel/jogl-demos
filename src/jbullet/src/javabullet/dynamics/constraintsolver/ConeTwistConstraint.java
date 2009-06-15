/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * btConeTwistConstraint is Copyright (c) 2007 Starbreeze Studios
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
 * 
 * Written by: Marcus Hennix
 */

package javabullet.dynamics.constraintsolver;

import javabullet.BulletGlobals;
import javabullet.dynamics.RigidBody;
import javabullet.linearmath.QuaternionUtil;
import javabullet.linearmath.ScalarUtil;
import javabullet.linearmath.Transform;
import javabullet.linearmath.TransformUtil;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * ConeTwistConstraint can be used to simulate ragdoll joints (upper arm, leg etc).
 * 
 * @author jezek2
 */
public class ConeTwistConstraint extends TypedConstraint {

	private JacobianEntry[] jac/*[3]*/ = new JacobianEntry[] { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; //3 orthogonal linear constraints

	private final Transform rbAFrame = new Transform();
	private final Transform rbBFrame = new Transform();

	private float limitSoftness;
	private float biasFactor;
	private float relaxationFactor;

	private float swingSpan1;
	private float swingSpan2;
	private float twistSpan;

	private final Vector3f swingAxis = new Vector3f();
	private final Vector3f twistAxis = new Vector3f();

	private float kSwing;
	private float kTwist;

	private float twistLimitSign;
	private float swingCorrection;
	private float twistCorrection;

	private float accSwingLimitImpulse;
	private float accTwistLimitImpulse;

	private boolean angularOnly = false;
	private boolean solveTwistLimit;
	private boolean solveSwingLimit;
	
	public ConeTwistConstraint() {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE);
	}

	public ConeTwistConstraint(RigidBody rbA, RigidBody rbB, Transform rbAFrame, Transform rbBFrame) {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE, rbA, rbB);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(rbBFrame);

		// flip axis for correct angles
		this.rbBFrame.basis.m10 *= -1f;
		this.rbBFrame.basis.m11 *= -1f;
		this.rbBFrame.basis.m12 *= -1f;

		swingSpan1 = 1e30f;
		swingSpan2 = 1e30f;
		twistSpan = 1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;

		solveTwistLimit = false;
		solveSwingLimit = false;
	}

	public ConeTwistConstraint(RigidBody rbA, Transform rbAFrame) {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE, rbA);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(this.rbAFrame);

		// flip axis for correct angles
		this.rbBFrame.basis.m10 *= -1f;
		this.rbBFrame.basis.m11 *= -1f;
		this.rbBFrame.basis.m12 *= -1f;

		this.rbBFrame.basis.m20 *= -1f;
		this.rbBFrame.basis.m21 *= -1f;
		this.rbBFrame.basis.m22 *= -1f;

		swingSpan1 = 1e30f;
		swingSpan2 = 1e30f;
		twistSpan = 1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;

		solveTwistLimit = false;
		solveSwingLimit = false;
	}
	
	@Override
	public void buildJacobian() {
		stack.pushCommonMath();
		stack.quats.push();
		try {
			Vector3f tmp = stack.vectors.get();
			Vector3f tmp1 = stack.vectors.get();
			Vector3f tmp2 = stack.vectors.get();

			appliedImpulse = 0f;

			// set bias, sign, clear accumulator
			swingCorrection = 0f;
			twistLimitSign = 0f;
			solveTwistLimit = false;
			solveSwingLimit = false;
			accTwistLimitImpulse = 0f;
			accSwingLimitImpulse = 0f;

			if (!angularOnly) {
				Vector3f pivotAInW = stack.vectors.get(rbAFrame.origin);
				rbA.getCenterOfMassTransform().transform(pivotAInW);

				Vector3f pivotBInW = stack.vectors.get(rbBFrame.origin);
				rbB.getCenterOfMassTransform().transform(pivotBInW);

				Vector3f relPos = stack.vectors.get();
				relPos.sub(pivotBInW, pivotAInW);

				// TODO: stack
				Vector3f[] normal/*[3]*/ = new Vector3f[]{stack.vectors.get(), stack.vectors.get(), stack.vectors.get()};
				if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
					normal[0].normalize(relPos);
				}
				else {
					normal[0].set(1f, 0f, 0f);
				}

				TransformUtil.planeSpace1(normal[0], normal[1], normal[2]);

				for (int i = 0; i < 3; i++) {
					Matrix3f mat1 = stack.matrices.get(rbA.getCenterOfMassTransform().basis);
					mat1.transpose();

					Matrix3f mat2 = stack.matrices.get(rbB.getCenterOfMassTransform().basis);
					mat2.transpose();

					tmp1.sub(pivotAInW, rbA.getCenterOfMassPosition());
					tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition());

					jac[i].init(
							mat1,
							mat2,
							tmp1,
							tmp2,
							normal[i],
							rbA.getInvInertiaDiagLocal(),
							rbA.getInvMass(),
							rbB.getInvInertiaDiagLocal(),
							rbB.getInvMass());
				}
			}

			Vector3f b1Axis1 = stack.vectors.get(), b1Axis2 = stack.vectors.get(), b1Axis3 = stack.vectors.get();
			Vector3f b2Axis1 = stack.vectors.get(), b2Axis2 = stack.vectors.get();

			rbAFrame.basis.getColumn(0, b1Axis1);
			getRigidBodyA().getCenterOfMassTransform().basis.transform(b1Axis1);

			rbBFrame.basis.getColumn(0, b2Axis1);
			getRigidBodyB().getCenterOfMassTransform().basis.transform(b2Axis1);

			float swing1 = 0f, swing2 = 0f;

			// Get Frame into world space
			if (swingSpan1 >= 0.05f) {
				rbAFrame.basis.getColumn(1, b1Axis2);
				getRigidBodyA().getCenterOfMassTransform().basis.transform(b1Axis2);
				swing1 = ScalarUtil.atan2Fast(b2Axis1.dot(b1Axis2), b2Axis1.dot(b1Axis1));
			}

			if (swingSpan2 >= 0.05f) {
				rbAFrame.basis.getColumn(2, b1Axis3);
				getRigidBodyA().getCenterOfMassTransform().basis.transform(b1Axis3);
				swing2 = ScalarUtil.atan2Fast(b2Axis1.dot(b1Axis3), b2Axis1.dot(b1Axis1));
			}

			float RMaxAngle1Sq = 1.0f / (swingSpan1 * swingSpan1);
			float RMaxAngle2Sq = 1.0f / (swingSpan2 * swingSpan2);
			float EllipseAngle = Math.abs(swing1) * RMaxAngle1Sq + Math.abs(swing2) * RMaxAngle2Sq;

			if (EllipseAngle > 1.0f) {
				swingCorrection = EllipseAngle - 1.0f;
				solveSwingLimit = true;

				// Calculate necessary axis & factors
				tmp1.scale(b2Axis1.dot(b1Axis2), b1Axis2);
				tmp2.scale(b2Axis1.dot(b1Axis3), b1Axis3);
				tmp.add(tmp1, tmp2);
				swingAxis.cross(b2Axis1, tmp);
				swingAxis.normalize();

				float swingAxisSign = (b2Axis1.dot(b1Axis1) >= 0.0f) ? 1.0f : -1.0f;
				swingAxis.scale(swingAxisSign);

				kSwing = 1f / (getRigidBodyA().computeAngularImpulseDenominator(swingAxis) +
						getRigidBodyB().computeAngularImpulseDenominator(swingAxis));

			}

			// Twist limits
			if (twistSpan >= 0f) {
				//Vector3f b2Axis2 = stack.vectors.get();
				rbBFrame.basis.getColumn(1, b2Axis2);
				getRigidBodyB().getCenterOfMassTransform().basis.transform(b2Axis2);

				Quat4f rotationArc = stack.quats.get(QuaternionUtil.shortestArcQuat(b2Axis1, b1Axis1));
				Vector3f TwistRef = stack.vectors.get(QuaternionUtil.quatRotate(rotationArc, b2Axis2));
				float twist = ScalarUtil.atan2Fast(TwistRef.dot(b1Axis3), TwistRef.dot(b1Axis2));

				float lockedFreeFactor = (twistSpan > 0.05f) ? limitSoftness : 0f;
				if (twist <= -twistSpan * lockedFreeFactor) {
					twistCorrection = -(twist + twistSpan);
					solveTwistLimit = true;

					twistAxis.add(b2Axis1, b1Axis1);
					twistAxis.scale(0.5f);
					twistAxis.normalize();
					twistAxis.scale(-1.0f);

					kTwist = 1f / (getRigidBodyA().computeAngularImpulseDenominator(twistAxis) +
							getRigidBodyB().computeAngularImpulseDenominator(twistAxis));

				}
				else if (twist > twistSpan * lockedFreeFactor) {
					twistCorrection = (twist - twistSpan);
					solveTwistLimit = true;

					twistAxis.add(b2Axis1, b1Axis1);
					twistAxis.scale(0.5f);
					twistAxis.normalize();

					kTwist = 1f / (getRigidBodyA().computeAngularImpulseDenominator(twistAxis) +
							getRigidBodyB().computeAngularImpulseDenominator(twistAxis));
				}
			}
		}
		finally {
			stack.popCommonMath();
			stack.quats.pop();
		}
	}

	@Override
	public void solveConstraint(float timeStep) {
		stack.vectors.push();
		try {
			Vector3f tmp = stack.vectors.get();
			Vector3f tmp2 = stack.vectors.get();

			Vector3f pivotAInW = stack.vectors.get(rbAFrame.origin);
			rbA.getCenterOfMassTransform().transform(pivotAInW);

			Vector3f pivotBInW = stack.vectors.get(rbBFrame.origin);
			rbB.getCenterOfMassTransform().transform(pivotBInW);

			float tau = 0.3f;

			// linear part
			if (!angularOnly) {
				Vector3f rel_pos1 = stack.vectors.get();
				rel_pos1.sub(pivotAInW, rbA.getCenterOfMassPosition());

				Vector3f rel_pos2 = stack.vectors.get();
				rel_pos2.sub(pivotBInW, rbB.getCenterOfMassPosition());

				Vector3f vel1 = stack.vectors.get(rbA.getVelocityInLocalPoint(rel_pos1));
				Vector3f vel2 = stack.vectors.get(rbB.getVelocityInLocalPoint(rel_pos2));
				Vector3f vel = stack.vectors.get();
				vel.sub(vel1, vel2);

				for (int i = 0; i < 3; i++) {
					Vector3f normal = jac[i].linearJointAxis;
					float jacDiagABInv = 1f / jac[i].getDiagonal();

					float rel_vel;
					rel_vel = normal.dot(vel);
					// positional error (zeroth order error)
					tmp.sub(pivotAInW, pivotBInW);
					float depth = -(tmp).dot(normal); // this is the error projected on the normal
					float impulse = depth * tau / timeStep * jacDiagABInv - rel_vel * jacDiagABInv;
					appliedImpulse += impulse;
					Vector3f impulse_vector = stack.vectors.get();
					impulse_vector.scale(impulse, normal);

					tmp.sub(pivotAInW, rbA.getCenterOfMassPosition());
					rbA.applyImpulse(impulse_vector, tmp);

					tmp.negate(impulse_vector);
					tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition());
					rbB.applyImpulse(tmp, tmp2);
				}
			}

			{
				// solve angular part
				Vector3f angVelA = getRigidBodyA().getAngularVelocity();
				Vector3f angVelB = getRigidBodyB().getAngularVelocity();

				// solve swing limit
				if (solveSwingLimit) {
					tmp.sub(angVelB, angVelA);
					float amplitude = ((tmp).dot(swingAxis) * relaxationFactor * relaxationFactor + swingCorrection * (1f / timeStep) * biasFactor);
					float impulseMag = amplitude * kSwing;

					// Clamp the accumulated impulse
					float temp = accSwingLimitImpulse;
					accSwingLimitImpulse = Math.max(accSwingLimitImpulse + impulseMag, 0.0f);
					impulseMag = accSwingLimitImpulse - temp;

					Vector3f impulse = stack.vectors.get();
					impulse.scale(impulseMag, swingAxis);

					rbA.applyTorqueImpulse(impulse);

					tmp.negate(impulse);
					rbB.applyTorqueImpulse(tmp);
				}

				// solve twist limit
				if (solveTwistLimit) {
					tmp.sub(angVelB, angVelA);
					float amplitude = ((tmp).dot(twistAxis) * relaxationFactor * relaxationFactor + twistCorrection * (1f / timeStep) * biasFactor);
					float impulseMag = amplitude * kTwist;

					// Clamp the accumulated impulse
					float temp = accTwistLimitImpulse;
					accTwistLimitImpulse = Math.max(accTwistLimitImpulse + impulseMag, 0.0f);
					impulseMag = accTwistLimitImpulse - temp;

					Vector3f impulse = stack.vectors.get();
					impulse.scale(impulseMag, twistAxis);

					rbA.applyTorqueImpulse(impulse);

					tmp.negate(impulse);
					rbB.applyTorqueImpulse(tmp);
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void updateRHS(float timeStep) {
	}

	public void setAngularOnly(boolean angularOnly) {
		this.angularOnly = angularOnly;
	}

	public void setLimit(float _swingSpan1, float _swingSpan2, float _twistSpan) {
		setLimit(_swingSpan1, _swingSpan2, _twistSpan, 0.8f, 0.3f, 1.0f);
	}

	public void setLimit(float _swingSpan1, float _swingSpan2, float _twistSpan, float _softness, float _biasFactor, float _relaxationFactor) {
		swingSpan1 = _swingSpan1;
		swingSpan2 = _swingSpan2;
		twistSpan = _twistSpan;

		limitSoftness = _softness;
		biasFactor = _biasFactor;
		relaxationFactor = _relaxationFactor;
	}

	public Transform getAFrame() {
		return rbAFrame;
	}

	public Transform getBFrame() {
		return rbBFrame;
	}

	public boolean getSolveTwistLimit() {
		return solveTwistLimit;
	}

	public boolean getSolveSwingLimit() {
		return solveTwistLimit;
	}

	public float getTwistLimitSign() {
		return twistLimitSign;
	}
	
}
