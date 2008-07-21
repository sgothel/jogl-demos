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

import javabullet.BulletGlobals;
import javabullet.dynamics.RigidBody;
import javabullet.linearmath.MatrixUtil;
import javabullet.linearmath.Transform;


/// 
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
/*!

*/
/**
 * Generic6DofConstraint between two rigidbodies each with a pivotpoint that descibes the axis location in local space.<p>
 * Generic6DofConstraint can leave any of the 6 degree of freedom 'free' or 'locked'.
 * Currently this limit supports rotational motors.<br>
 * <ul>
 * <li>For Linear limits, use Generic6DofConstraint.setLinearUpperLimit, Generic6DofConstraint.setLinearLowerLimit. You can set the parameters with the TranslationalLimitMotor structure accsesible through the Generic6DofConstraint.getTranslationalLimitMotor method.
 * At this moment translational motors are not supported. May be in the future.</li>
 * 
 * <li>For Angular limits, use the RotationalLimitMotor structure for configuring the limit.
 * This is accessible through Generic6DofConstraint.getLimitMotor method,
 * This brings support for limit parameters and motors.</li>
 * 
 * <li>Angulars limits have these possible ranges:
 * <table border=1 >
 * <tr>
 * 	<td><b>AXIS</b></td>
 * 	<td><b>MIN ANGLE</b></td>
 * 	<td><b>MAX ANGLE</b></td>
 * </tr><tr>
 * 	<td>X</td>
 * 		<td>-PI</td>
 * 		<td>PI</td>
 * </tr><tr>
 * 	<td>Y</td>
 * 		<td>-PI/2</td>
 * 		<td>PI/2</td>
 * </tr><tr>
 * 	<td>Z</td>
 * 		<td>-PI/2</td>
 * 		<td>PI/2</td>
 * </tr>
 * </table>
 * </li>
 * </ul>
 * 
 * @author jezek2
 */
public class Generic6DofConstraint extends TypedConstraint {

	protected final Transform frameInA = new Transform(); //!< the constraint space w.r.t body A
    protected final Transform frameInB = new Transform(); //!< the constraint space w.r.t body B

	protected final JacobianEntry[] jacLinear/*[3]*/ = new JacobianEntry[] { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; //!< 3 orthogonal linear constraints
    protected final JacobianEntry[] jacAng/*[3]*/ = new JacobianEntry[] { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; //!< 3 orthogonal angular constraints

	protected final TranslationalLimitMotor linearLimits = new TranslationalLimitMotor();

	protected final RotationalLimitMotor[] angularLimits/*[3]*/ = new RotationalLimitMotor[] { new RotationalLimitMotor(), new RotationalLimitMotor(), new RotationalLimitMotor() };

	protected float timeStep;
    protected final Transform calculatedTransformA = new Transform();
    protected final Transform calculatedTransformB = new Transform();
    protected final Vector3f calculatedAxisAngleDiff = new Vector3f();
    protected final Vector3f[] calculatedAxis/*[3]*/ = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };
    
    protected boolean useLinearReferenceFrameA;

	public Generic6DofConstraint() {
		super(TypedConstraintType.D6_CONSTRAINT_TYPE);
		useLinearReferenceFrameA = true;
	}

	public Generic6DofConstraint(RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		super(TypedConstraintType.D6_CONSTRAINT_TYPE, rbA, rbB);
		this.frameInA.set(frameInA);
		this.frameInB.set(frameInB);
		this.useLinearReferenceFrameA = useLinearReferenceFrameA;
	}

	private static float getMatrixElem(Matrix3f mat, int index) {
		int i = index % 3;
		int j = index / 3;
		return mat.getElement(i, j);
	}
	
	/**
	 * MatrixToEulerXYZ from http://www.geometrictools.com/LibFoundation/Mathematics/Wm4Matrix3.inl.html
	 */
	private static boolean matrixToEulerXYZ(Matrix3f mat, Vector3f xyz) {
		//	// rot =  cy*cz          -cy*sz           sy
		//	//        cz*sx*sy+cx*sz  cx*cz-sx*sy*sz -cy*sx
		//	//       -cx*cz*sy+sx*sz  cz*sx+cx*sy*sz  cx*cy
		//

		if (getMatrixElem(mat, 2) < 1.0f) {
			if (getMatrixElem(mat, 2) > -1.0f) {
				xyz.x = (float) Math.atan2(-getMatrixElem(mat, 5), getMatrixElem(mat, 8));
				xyz.y = (float) Math.asin(getMatrixElem(mat, 2));
				xyz.z = (float) Math.atan2(-getMatrixElem(mat, 1), getMatrixElem(mat, 0));
				return true;
			}
			else {
				// WARNING.  Not unique.  XA - ZA = -atan2(r10,r11)
				xyz.x = -(float) Math.atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4));
				xyz.y = -BulletGlobals.SIMD_HALF_PI;
				xyz.z = 0.0f;
				return false;
			}
		}
		else {
			// WARNING.  Not unique.  XAngle + ZAngle = atan2(r10,r11)
			xyz.x = (float) Math.atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4));
			xyz.y = BulletGlobals.SIMD_HALF_PI;
			xyz.z = 0.0f;
		}

		return false;
	}

	/**
	 * Calcs the euler angles between the two bodies.
	 */
	protected void calculateAngleInfo() {
		stack.pushCommonMath();
		try {
			Matrix3f mat = stack.matrices.get();

			Matrix3f relative_frame = stack.matrices.get();
			mat.set(calculatedTransformA.basis);
			MatrixUtil.invert(mat);
			relative_frame.mul(mat, calculatedTransformB.basis);

			matrixToEulerXYZ(relative_frame, calculatedAxisAngleDiff);

			// in euler angle mode we do not actually constrain the angular velocity
			// along the axes axis[0] and axis[2] (although we do use axis[1]) :
			//
			//    to get			constrain w2-w1 along		...not
			//    ------			---------------------		------
			//    d(angle[0])/dt = 0	ax[1] x ax[2]			ax[0]
			//    d(angle[1])/dt = 0	ax[1]
			//    d(angle[2])/dt = 0	ax[0] x ax[1]			ax[2]
			//
			// constraining w2-w1 along an axis 'a' means that a'*(w2-w1)=0.
			// to prove the result for angle[0], write the expression for angle[0] from
			// GetInfo1 then take the derivative. to prove this for angle[2] it is
			// easier to take the euler rate expression for d(angle[2])/dt with respect
			// to the components of w and set that to 0.

			Vector3f axis0 = stack.vectors.get();
			calculatedTransformB.basis.getColumn(0, axis0);

			Vector3f axis2 = stack.vectors.get();
			calculatedTransformA.basis.getColumn(2, axis2);

			calculatedAxis[1].cross(axis2, axis0);
			calculatedAxis[0].cross(calculatedAxis[1], axis2);
			calculatedAxis[2].cross(axis0, calculatedAxis[1]);

			//    if(m_debugDrawer)
			//    {
			//
			//    	char buff[300];
			//		sprintf(buff,"\n X: %.2f ; Y: %.2f ; Z: %.2f ",
			//		m_calculatedAxisAngleDiff[0],
			//		m_calculatedAxisAngleDiff[1],
			//		m_calculatedAxisAngleDiff[2]);
			//    	m_debugDrawer->reportErrorWarning(buff);
			//    }
		}
		finally {
			stack.popCommonMath();
		}
	}

	/**
	 * Calcs global transform of the offsets.<p>
	 * Calcs the global transform for the joint offset for body A an B, and also calcs the agle differences between the bodies.
	 * 
	 * See also: Generic6DofConstraint.getCalculatedTransformA, Generic6DofConstraint.getCalculatedTransformB, Generic6DofConstraint.calculateAngleInfo
	 */
	public void calculateTransforms() {
		calculatedTransformA.set(rbA.getCenterOfMassTransform());
		calculatedTransformA.mul(frameInA);

		calculatedTransformB.set(rbB.getCenterOfMassTransform());
		calculatedTransformB.mul(frameInB);

		calculateAngleInfo();
	}
	
	protected void buildLinearJacobian(/*JacobianEntry jacLinear*/int jacLinear_index, Vector3f normalWorld, Vector3f pivotAInW, Vector3f pivotBInW) {
		stack.pushCommonMath();
		try {
			Matrix3f mat1 = stack.matrices.get(rbA.getCenterOfMassTransform().basis);
			mat1.transpose();

			Matrix3f mat2 = stack.matrices.get(rbB.getCenterOfMassTransform().basis);
			mat2.transpose();

			Vector3f tmp1 = stack.vectors.get();
			tmp1.sub(pivotAInW, rbA.getCenterOfMassPosition());

			Vector3f tmp2 = stack.vectors.get();
			tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition());

			jacLinear[jacLinear_index].init(
					mat1,
					mat2,
					tmp1,
					tmp2,
					normalWorld,
					rbA.getInvInertiaDiagLocal(),
					rbA.getInvMass(),
					rbB.getInvInertiaDiagLocal(),
					rbB.getInvMass());
		}
		finally {
			stack.popCommonMath();
		}
	}

	protected void buildAngularJacobian(/*JacobianEntry jacAngular*/int jacAngular_index, Vector3f jointAxisW) {
		stack.matrices.push();
		try {
			Matrix3f mat1 = stack.matrices.get(rbA.getCenterOfMassTransform().basis);
			mat1.transpose();

			Matrix3f mat2 = stack.matrices.get(rbB.getCenterOfMassTransform().basis);
			mat2.transpose();

			jacAng[jacAngular_index].init(jointAxisW,
					mat1,
					mat2,
					rbA.getInvInertiaDiagLocal(),
					rbB.getInvInertiaDiagLocal());
		}
		finally {
			stack.matrices.pop();
		}
	}

	/**
	 * Test angular limit.<p>
	 * Calculates angular correction and returns true if limit needs to be corrected.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
	public boolean testAngularLimitMotor(int axis_index) {
		float angle = VectorUtil.getCoord(calculatedAxisAngleDiff, axis_index);

		// test limits
		angularLimits[axis_index].testLimitValue(angle);
		return angularLimits[axis_index].needApplyTorques();
	}
	
	@Override
	public void buildJacobian() {
		stack.vectors.push();
		try {
			// calculates transform
			calculateTransforms();

			Vector3f pivotAInW = calculatedTransformA.origin;
			Vector3f pivotBInW = calculatedTransformB.origin;

			Vector3f rel_pos1 = stack.vectors.get();
			rel_pos1.sub(pivotAInW, rbA.getCenterOfMassPosition());

			Vector3f rel_pos2 = stack.vectors.get();
			rel_pos2.sub(pivotBInW, rbB.getCenterOfMassPosition());

			Vector3f normalWorld = stack.vectors.get();
			int i;
			// linear part
			for (i = 0; i < 3; i++) {
				if (linearLimits.isLimited(i)) {
					if (useLinearReferenceFrameA) {
						calculatedTransformA.basis.getColumn(i, normalWorld);
					}
					else {
						calculatedTransformB.basis.getColumn(i, normalWorld);
					}

					buildLinearJacobian(
							/*jacLinear[i]*/i, normalWorld,
							pivotAInW, pivotBInW);

				}
			}

			// angular part
			for (i = 0; i < 3; i++) {
				// calculates error angle
				if (testAngularLimitMotor(i)) {
					normalWorld.set(this.getAxis(i));
					// Create angular atom
					buildAngularJacobian(/*jacAng[i]*/i, normalWorld);
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	@Override
	public void solveConstraint(float timeStep) {
		stack.vectors.push();
		try {
			this.timeStep = timeStep;

			//calculateTransforms();

			int i;

			// linear

			Vector3f pointInA = stack.vectors.get(calculatedTransformA.origin);
			Vector3f pointInB = stack.vectors.get(calculatedTransformB.origin);

			float jacDiagABInv;
			Vector3f linear_axis = stack.vectors.get();
			for (i = 0; i < 3; i++) {
				if (linearLimits.isLimited(i)) {
					jacDiagABInv = 1f / jacLinear[i].getDiagonal();

					if (useLinearReferenceFrameA) {
						calculatedTransformA.basis.getColumn(i, linear_axis);
					}
					else {
						calculatedTransformB.basis.getColumn(i, linear_axis);
					}

					linearLimits.solveLinearAxis(
							this.timeStep,
							jacDiagABInv,
							rbA, pointInA,
							rbB, pointInB,
							i, linear_axis);

				}
			}

			// angular
			Vector3f angular_axis = stack.vectors.get();
			float angularJacDiagABInv;
			for (i = 0; i < 3; i++) {
				if (angularLimits[i].needApplyTorques()) {
					// get axis
					angular_axis.set(getAxis(i));

					angularJacDiagABInv = 1f / jacAng[i].getDiagonal();

					angularLimits[i].solveAngularLimits(this.timeStep, angular_axis, angularJacDiagABInv, rbA, rbB);
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}
	

    public void updateRHS(float timeStep) {
	}

	/**
	 * Get the rotation axis in global coordinates.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
	public Vector3f getAxis(int axis_index) {
		return calculatedAxis[axis_index];
	}

	/**
	 * Get the relative Euler angle.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
	public float getAngle(int axis_index) {
		return VectorUtil.getCoord(calculatedAxisAngleDiff, axis_index);
	}

	/**
	 * Gets the global transform of the offset for body A.<p>
	 * See also: Generic6DofConstraint.getFrameOffsetA, Generic6DofConstraint.getFrameOffsetB, Generic6DofConstraint.calculateAngleInfo.
	 */
	public Transform getCalculatedTransformA() {
		return calculatedTransformA;
	}

	/**
	 * Gets the global transform of the offset for body B.<p>
	 * See also: Generic6DofConstraint.getFrameOffsetA, Generic6DofConstraint.getFrameOffsetB, Generic6DofConstraint.calculateAngleInfo.
	 */
	public Transform getCalculatedTransformB() {
		return calculatedTransformB;
	}

	public Transform getFrameOffsetA() {
		return frameInA;
	}

	public Transform getFrameOffsetB() {
		return frameInB;
	}
	
	public void setLinearLowerLimit(Vector3f linearLower) {
		linearLimits.lowerLimit.set(linearLower);
	}

	public void setLinearUpperLimit(Vector3f linearUpper) {
		linearLimits.upperLimit.set(linearUpper);
	}

	public void setAngularLowerLimit(Vector3f angularLower) {
		angularLimits[0].loLimit = angularLower.x;
		angularLimits[1].loLimit = angularLower.y;
		angularLimits[2].loLimit = angularLower.z;
	}

	public void setAngularUpperLimit(Vector3f angularUpper) {
		angularLimits[0].hiLimit = angularUpper.x;
		angularLimits[1].hiLimit = angularUpper.y;
		angularLimits[2].hiLimit = angularUpper.z;
	}

	/**
	 * Retrieves the angular limit informacion.
	 */
	public RotationalLimitMotor getRotationalLimitMotor(int index) {
		return angularLimits[index];
	}

	/**
	 * Retrieves the limit informacion.
	 */
	public TranslationalLimitMotor getTranslationalLimitMotor() {
		return linearLimits;
	}

	/**
	 * first 3 are linear, next 3 are angular
	 */
	public void setLimit(int axis, float lo, float hi) {
		if (axis < 3) {
			VectorUtil.setCoord(linearLimits.lowerLimit, axis, lo);
			VectorUtil.setCoord(linearLimits.upperLimit, axis, hi);
		}
		else {
			angularLimits[axis - 3].loLimit = lo;
			angularLimits[axis - 3].hiLimit = hi;
		}
	}
	
	/**
	 * Test limit.<p>
	 * - free means upper &lt; lower,<br>
	 * - locked means upper == lower<br>
	 * - limited means upper &gt; lower<br>
	 * - limitIndex: first 3 are linear, next 3 are angular
	 */
	public boolean isLimited(int limitIndex) {
		if (limitIndex < 3) {
			return linearLimits.isLimited(limitIndex);

		}
		return angularLimits[limitIndex - 3].isLimited();
	}
	
}
