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

/*
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Le�n
email: projectileman@yahoo.com
http://gimpact.sf.net
*/

package javabullet.dynamics.constraintsolver;

import javabullet.BulletStack;
import javabullet.dynamics.RigidBody;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class TranslationalLimitMotor {
	
	protected final BulletStack stack = BulletStack.get();
	
	public final Vector3f lowerLimit = new Vector3f(); //!< the constraint lower limits
	public final Vector3f upperLimit = new Vector3f(); //!< the constraint upper limits
	public final Vector3f accumulatedImpulse = new Vector3f();
	
	public float limitSoftness; //!< Softness for linear limit
	public float damping; //!< Damping for linear limit
	public float restitution; //! Bounce parameter for linear limit

	public TranslationalLimitMotor() {
		lowerLimit.set(0f, 0f, 0f);
		upperLimit.set(0f, 0f, 0f);
		accumulatedImpulse.set(0f, 0f, 0f);

		limitSoftness = 0.7f;
		damping = 1.0f;
		restitution = 0.5f;
	}

	public TranslationalLimitMotor(TranslationalLimitMotor other) {
		lowerLimit.set(other.lowerLimit);
		upperLimit.set(other.upperLimit);
		accumulatedImpulse.set(other.accumulatedImpulse);

		limitSoftness = other.limitSoftness;
		damping = other.damping;
		restitution = other.restitution;
	}

	/**
	 * Test limit.<p>
	 * - free means upper &lt; lower,<br>
	 * - locked means upper == lower<br>
	 * - limited means upper &gt; lower<br>
	 * - limitIndex: first 3 are linear, next 3 are angular
	 */
	public boolean isLimited(int limitIndex) {
		return (VectorUtil.getCoord(upperLimit, limitIndex) >= VectorUtil.getCoord(lowerLimit, limitIndex));
	}

	public float solveLinearAxis(float timeStep, float jacDiagABInv, RigidBody body1, Vector3f pointInA, RigidBody body2, Vector3f pointInB, int limit_index, Vector3f axis_normal_on_a) {
		stack.vectors.push();
		try {
			Vector3f tmp = stack.vectors.get();

			// find relative velocity
			Vector3f rel_pos1 = stack.vectors.get();
			rel_pos1.sub(pointInA, body1.getCenterOfMassPosition());

			Vector3f rel_pos2 = stack.vectors.get();
			rel_pos2.sub(pointInB, body2.getCenterOfMassPosition());

			Vector3f vel1 = stack.vectors.get(body1.getVelocityInLocalPoint(rel_pos1));
			Vector3f vel2 = stack.vectors.get(body2.getVelocityInLocalPoint(rel_pos2));
			Vector3f vel = stack.vectors.get();
			vel.sub(vel1, vel2);

			float rel_vel = axis_normal_on_a.dot(vel);

			// apply displacement correction

			// positional error (zeroth order error)
			tmp.sub(pointInA, pointInB);
			float depth = -(tmp).dot(axis_normal_on_a);
			float lo = -1e30f;
			float hi = 1e30f;

			float minLimit = VectorUtil.getCoord(lowerLimit, limit_index);
			float maxLimit = VectorUtil.getCoord(upperLimit, limit_index);

			// handle the limits
			if (minLimit < maxLimit) {
				{
					if (depth > maxLimit) {
						depth -= maxLimit;
						lo = 0f;

					}
					else {
						if (depth < minLimit) {
							depth -= minLimit;
							hi = 0f;
						}
						else {
							return 0.0f;
						}
					}
				}
			}

			float normalImpulse = limitSoftness * (restitution * depth / timeStep - damping * rel_vel) * jacDiagABInv;

			float oldNormalImpulse = VectorUtil.getCoord(accumulatedImpulse, limit_index);
			float sum = oldNormalImpulse + normalImpulse;
			VectorUtil.setCoord(accumulatedImpulse, limit_index, sum > hi ? 0f : sum < lo ? 0f : sum);
			normalImpulse = VectorUtil.getCoord(accumulatedImpulse, limit_index) - oldNormalImpulse;

			Vector3f impulse_vector = stack.vectors.get();
			impulse_vector.scale(normalImpulse, axis_normal_on_a);
			body1.applyImpulse(impulse_vector, rel_pos1);

			tmp.negate(impulse_vector);
			body2.applyImpulse(tmp, rel_pos2);
			return normalImpulse;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
}
