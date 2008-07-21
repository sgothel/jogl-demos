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

import java.util.ArrayList;
import java.util.List;
import javabullet.BulletGlobals;
import javabullet.BulletPool;
import javabullet.ContactDestroyedCallback;
import javabullet.ObjectPool;
import javabullet.collision.broadphase.Dispatcher;
import javabullet.collision.dispatch.CollisionObject;
import javabullet.collision.narrowphase.ManifoldPoint;
import javabullet.collision.narrowphase.PersistentManifold;
import javabullet.dynamics.RigidBody;
import javabullet.linearmath.IDebugDraw;
import javabullet.linearmath.MiscUtil;
import javabullet.linearmath.TransformUtil;
import javabullet.util.IntArrayList;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

/**
 * SequentialImpulseConstraintSolver uses a Propagation Method and Sequentially applies impulses.
 * The approach is the 3D version of Erin Catto's GDC 2006 tutorial. See http://www.gphysics.com<p>
 * 
 * Although Sequential Impulse is more intuitive, it is mathematically equivalent to Projected Successive Overrelaxation (iterative LCP).
 * Applies impulses for combined restitution and penetration recovery and to simulate friction.
 * 
 * @author jezek2
 */
public class SequentialImpulseConstraintSolver extends ConstraintSolver {
	
	private static final int MAX_CONTACT_SOLVER_TYPES = ContactConstraintEnum.MAX_CONTACT_SOLVER_TYPES.ordinal();

	private static final int SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS = 16384;
	private static OrderIndex[] gOrder = new OrderIndex[SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS];
	
	private static int totalCpd = 0;
	
	static {
		for (int i=0; i<gOrder.length; i++) {
			gOrder[i] = new OrderIndex();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private final ObjectPool<SolverBody> bodiesPool = BulletPool.get(SolverBody.class);
	private final ObjectPool<SolverConstraint> constraintsPool = BulletPool.get(SolverConstraint.class);
	private final ObjectPool<JacobianEntry> jacobiansPool = BulletPool.get(JacobianEntry.class);
	
	private final List<SolverBody> tmpSolverBodyPool = new ArrayList<SolverBody>();
	private final List<SolverConstraint> tmpSolverConstraintPool = new ArrayList<SolverConstraint>();
	private final List<SolverConstraint> tmpSolverFrictionConstraintPool = new ArrayList<SolverConstraint>();
	private final IntArrayList orderTmpConstraintPool = new IntArrayList();
	private final IntArrayList orderFrictionConstraintPool = new IntArrayList();
	
	protected final ContactSolverFunc[][] contactDispatch = new ContactSolverFunc[MAX_CONTACT_SOLVER_TYPES][MAX_CONTACT_SOLVER_TYPES];
	protected final ContactSolverFunc[][] frictionDispatch = new ContactSolverFunc[MAX_CONTACT_SOLVER_TYPES][MAX_CONTACT_SOLVER_TYPES];
	
	// choose between several modes, different friction model etc.
	protected int solverMode = SolverMode.SOLVER_RANDMIZE_ORDER | SolverMode.SOLVER_CACHE_FRIENDLY; // not using SOLVER_USE_WARMSTARTING,
	// btSeed2 is used for re-arranging the constraint rows. improves convergence/quality of friction
	protected long btSeed2 = 0L;

	public SequentialImpulseConstraintSolver() {
		BulletGlobals.gContactDestroyedCallback = new ContactDestroyedCallback() {
			public boolean invoke(Object userPersistentData) {
				assert (userPersistentData != null);
				ConstraintPersistentData cpd = (ConstraintPersistentData) userPersistentData;
				//btAlignedFree(cpd);
				totalCpd--;
				//printf("totalCpd = %i. DELETED Ptr %x\n",totalCpd,userPersistentData);
				return true;
			}
		};

		// initialize default friction/contact funcs
		int i, j;
		for (i = 0; i < MAX_CONTACT_SOLVER_TYPES; i++) {
			for (j = 0; j < MAX_CONTACT_SOLVER_TYPES; j++) {
				contactDispatch[i][j] = ContactConstraint.resolveSingleCollision;
				frictionDispatch[i][j] = ContactConstraint.resolveSingleFriction;
			}
		}
	}
	
	public long rand2() {
		btSeed2 = (1664525L * btSeed2 + 1013904223L) & 0xffffffff;
		return btSeed2;
	}
	
	// See ODE: adam's all-int straightforward(?) dRandInt (0..n-1)
	public int randInt2(int n) {
		// seems good; xor-fold and modulus
		long un = n;
		long r = rand2();

		// note: probably more aggressive than it needs to be -- might be
		//       able to get away without one or two of the innermost branches.
		if (un <= 0x00010000L) {
			r ^= (r >>> 16);
			if (un <= 0x00000100L) {
				r ^= (r >>> 8);
				if (un <= 0x00000010L) {
					r ^= (r >>> 4);
					if (un <= 0x00000004L) {
						r ^= (r >>> 2);
						if (un <= 0x00000002L) {
							r ^= (r >>> 1);
						}
					}
				}
			}
		}

		// TODO: check modulo C vs Java mismatch
		return (int) Math.abs(r % un);
	}
	
	private void initSolverBody(SolverBody solverBody, CollisionObject collisionObject) {
		RigidBody rb = RigidBody.upcast(collisionObject);
		if (rb != null) {
			solverBody.angularVelocity.set(rb.getAngularVelocity());
			solverBody.centerOfMassPosition.set(collisionObject.getWorldTransform().origin);
			solverBody.friction = collisionObject.getFriction();
			solverBody.invMass = rb.getInvMass();
			solverBody.linearVelocity.set(rb.getLinearVelocity());
			solverBody.originalBody = rb;
			solverBody.angularFactor = rb.getAngularFactor();
		}
		else {
			solverBody.angularVelocity.set(0f, 0f, 0f);
			solverBody.centerOfMassPosition.set(collisionObject.getWorldTransform().origin);
			solverBody.friction = collisionObject.getFriction();
			solverBody.invMass = 0f;
			solverBody.linearVelocity.set(0f, 0f, 0f);
			solverBody.originalBody = null;
			solverBody.angularFactor = 1f;
		}
	}
	
	private float restitutionCurve(float rel_vel, float restitution) {
		float rest = restitution * -rel_vel;
		return rest;
	}

	/**
	 * velocity + friction
	 * response  between two dynamic objects with friction
	 */
	private float resolveSingleCollisionCombinedCacheFriendly(
			SolverBody body1,
			SolverBody body2,
			SolverConstraint contactConstraint,
			ContactSolverInfo solverInfo) {
		stack.vectors.push();
		try {
			float normalImpulse;

			//  Optimized version of projected relative velocity, use precomputed cross products with normal
			//	body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
			//	body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
			//	btVector3 vel = vel1 - vel2;
			//	btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

			float rel_vel;
			float vel1Dotn = contactConstraint.contactNormal.dot(body1.linearVelocity) + contactConstraint.relpos1CrossNormal.dot(body1.angularVelocity);
			float vel2Dotn = contactConstraint.contactNormal.dot(body2.linearVelocity) + contactConstraint.relpos2CrossNormal.dot(body2.angularVelocity);

			rel_vel = vel1Dotn - vel2Dotn;


			float positionalError = contactConstraint.penetration;
			float velocityError = contactConstraint.restitution - rel_vel; // * damping;

			float penetrationImpulse = positionalError * contactConstraint.jacDiagABInv;
			float velocityImpulse = velocityError * contactConstraint.jacDiagABInv;
			normalImpulse = penetrationImpulse + velocityImpulse;

			// See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
			float oldNormalImpulse = contactConstraint.appliedImpulse;
			float sum = oldNormalImpulse + normalImpulse;
			contactConstraint.appliedImpulse = 0f > sum ? 0f : sum;

			float oldVelocityImpulse = contactConstraint.appliedVelocityImpulse;
			float velocitySum = oldVelocityImpulse + velocityImpulse;
			contactConstraint.appliedVelocityImpulse = 0f > velocitySum ? 0f : velocitySum;

			normalImpulse = contactConstraint.appliedImpulse - oldNormalImpulse;

			Vector3f tmp = stack.vectors.get();
			if (body1.invMass != 0f) {
				tmp.scale(body1.invMass, contactConstraint.contactNormal);
				body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, normalImpulse);
			}
			if (body2.invMass != 0f) {
				tmp.scale(body2.invMass, contactConstraint.contactNormal);
				body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse);
			}

			return normalImpulse;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	private float resolveSingleFrictionCacheFriendly(
			SolverBody body1,
			SolverBody body2,
			SolverConstraint contactConstraint,
			ContactSolverInfo solverInfo,
			float appliedNormalImpulse) {
		stack.vectors.push();
		try {
			float combinedFriction = contactConstraint.friction;

			float limit = appliedNormalImpulse * combinedFriction;

			if (appliedNormalImpulse > 0f) //friction
			{

				float j1;
				{

					float rel_vel;
					float vel1Dotn = contactConstraint.contactNormal.dot(body1.linearVelocity) + contactConstraint.relpos1CrossNormal.dot(body1.angularVelocity);
					float vel2Dotn = contactConstraint.contactNormal.dot(body2.linearVelocity) + contactConstraint.relpos2CrossNormal.dot(body2.angularVelocity);
					rel_vel = vel1Dotn - vel2Dotn;

					// calculate j that moves us to zero relative velocity
					j1 = -rel_vel * contactConstraint.jacDiagABInv;
					//#define CLAMP_ACCUMULATED_FRICTION_IMPULSE 1
					//#ifdef CLAMP_ACCUMULATED_FRICTION_IMPULSE
					float oldTangentImpulse = contactConstraint.appliedImpulse;
					contactConstraint.appliedImpulse = oldTangentImpulse + j1;

					if (limit < contactConstraint.appliedImpulse) {
						contactConstraint.appliedImpulse = limit;
					}
					else {
						if (contactConstraint.appliedImpulse < -limit) {
							contactConstraint.appliedImpulse = -limit;
						}
					}
					j1 = contactConstraint.appliedImpulse - oldTangentImpulse;
					//	#else
					//	if (limit < j1)
					//	{
					//		j1 = limit;
					//	} else
					//	{
					//		if (j1 < -limit)
					//			j1 = -limit;
					//	}
					//	#endif

					//GEN_set_min(contactConstraint.m_appliedImpulse, limit);
					//GEN_set_max(contactConstraint.m_appliedImpulse, -limit);
				}

				Vector3f tmp = stack.vectors.get();
				if (body1.invMass != 0f) {
					tmp.scale(body1.invMass, contactConstraint.contactNormal);
					body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, j1);
				}
				if (body2.invMass != 0f) {
					tmp.scale(body2.invMass, contactConstraint.contactNormal);
					body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -j1);
				}

			}
			return 0f;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	protected void addFrictionConstraint(Vector3f normalAxis, int solverBodyIdA, int solverBodyIdB, int frictionIndex, ManifoldPoint cp, Vector3f rel_pos1, Vector3f rel_pos2, CollisionObject colObj0, CollisionObject colObj1, float relaxation) {
		stack.vectors.push();
		try {
			RigidBody body0 = RigidBody.upcast(colObj0);
			RigidBody body1 = RigidBody.upcast(colObj1);

			SolverConstraint solverConstraint = constraintsPool.get();
			tmpSolverFrictionConstraintPool.add(solverConstraint);

			solverConstraint.contactNormal.set(normalAxis);

			solverConstraint.solverBodyIdA = solverBodyIdA;
			solverConstraint.solverBodyIdB = solverBodyIdB;
			solverConstraint.constraintType = SolverConstraintType.SOLVER_FRICTION_1D;
			solverConstraint.frictionIndex = frictionIndex;

			solverConstraint.friction = cp.combinedFriction;

			solverConstraint.appliedImpulse = 0f;
			solverConstraint.appliedVelocityImpulse = 0f;
			solverConstraint.penetration = 0f;
			{
				Vector3f ftorqueAxis1 = stack.vectors.get();
				ftorqueAxis1.cross(rel_pos1, solverConstraint.contactNormal);
				solverConstraint.relpos1CrossNormal.set(ftorqueAxis1);
				if (body0 != null) {
					solverConstraint.angularComponentA.set(ftorqueAxis1);
					body0.getInvInertiaTensorWorld().transform(solverConstraint.angularComponentA);
				}
				else {
					solverConstraint.angularComponentA.set(0f, 0f, 0f);
				}
			}
			{
				Vector3f ftorqueAxis1 = stack.vectors.get();
				ftorqueAxis1.cross(rel_pos2, solverConstraint.contactNormal);
				solverConstraint.relpos2CrossNormal.set(ftorqueAxis1);
				if (body1 != null) {
					solverConstraint.angularComponentB.set(ftorqueAxis1);
					body1.getInvInertiaTensorWorld().transform(solverConstraint.angularComponentB);
				}
				else {
					solverConstraint.angularComponentB.set(0f, 0f, 0f);
				}
			}

			//#ifdef COMPUTE_IMPULSE_DENOM
			//	btScalar denom0 = rb0->computeImpulseDenominator(pos1,solverConstraint.m_contactNormal);
			//	btScalar denom1 = rb1->computeImpulseDenominator(pos2,solverConstraint.m_contactNormal);
			//#else
			Vector3f vec = stack.vectors.get();
			float denom0 = 0f;
			float denom1 = 0f;
			if (body0 != null) {
				vec.cross(solverConstraint.angularComponentA, rel_pos1);
				denom0 = body0.getInvMass() + normalAxis.dot(vec);
			}
			if (body1 != null) {
				vec.cross(solverConstraint.angularComponentB, rel_pos2);
				denom1 = body1.getInvMass() + normalAxis.dot(vec);
			}
			//#endif //COMPUTE_IMPULSE_DENOM

			float denom = relaxation / (denom0 + denom1);
			solverConstraint.jacDiagABInv = denom;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	public float solveGroupCacheFriendlySetup(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, List<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal, IDebugDraw debugDrawer/*,btStackAlloc* stackAlloc*/) {
		BulletGlobals.pushProfile("solveGroupCacheFriendlySetup");
		stack.vectors.push();
		try {

			if ((numConstraints + numManifolds) == 0) {
				// printf("empty\n");
				return 0f;
			}
			PersistentManifold manifold = null;
			CollisionObject colObj0 = null, colObj1 = null;

			//btRigidBody* rb0=0,*rb1=0;

	//	//#ifdef FORCE_REFESH_CONTACT_MANIFOLDS
	//
	//		BEGIN_PROFILE("refreshManifolds");
	//
	//		int i;
	//
	//
	//
	//		for (i=0;i<numManifolds;i++)
	//		{
	//			manifold = manifoldPtr[i];
	//			rb1 = (btRigidBody*)manifold->getBody1();
	//			rb0 = (btRigidBody*)manifold->getBody0();
	//
	//			manifold->refreshContactPoints(rb0->getCenterOfMassTransform(),rb1->getCenterOfMassTransform());
	//
	//		}
	//
	//		END_PROFILE("refreshManifolds");
	//	//#endif //FORCE_REFESH_CONTACT_MANIFOLDS

			Vector3f color = stack.vectors.get(0f, 1f, 0f);

			//int sizeofSB = sizeof(btSolverBody);
			//int sizeofSC = sizeof(btSolverConstraint);

			//if (1)
			{
				//if m_stackAlloc, try to pack bodies/constraints to speed up solving
				//		btBlock*					sablock;
				//		sablock = stackAlloc->beginBlock();

				//	int memsize = 16;
				//		unsigned char* stackMemory = stackAlloc->allocate(memsize);


				// todo: use stack allocator for this temp memory
				int minReservation = numManifolds * 2;

				//m_tmpSolverBodyPool.reserve(minReservation);

				//don't convert all bodies, only the one we need so solver the constraints
				/*
				{
				for (int i=0;i<numBodies;i++)
				{
				btRigidBody* rb = btRigidBody::upcast(bodies[i]);
				if (rb && 	(rb->getIslandTag() >= 0))
				{
				btAssert(rb->getCompanionId() < 0);
				int solverBodyId = m_tmpSolverBodyPool.size();
				btSolverBody& solverBody = m_tmpSolverBodyPool.expand();
				initSolverBody(&solverBody,rb);
				rb->setCompanionId(solverBodyId);
				} 
				}
				}
				*/

				//m_tmpSolverConstraintPool.reserve(minReservation);
				//m_tmpSolverFrictionConstraintPool.reserve(minReservation);

				{
					int i;

					for (i = 0; i < numManifolds; i++) {
						manifold = manifoldPtr.get(manifold_offset+i);
						colObj0 = (CollisionObject) manifold.getBody0();
						colObj1 = (CollisionObject) manifold.getBody1();

						int solverBodyIdA = -1;
						int solverBodyIdB = -1;

						if (manifold.getNumContacts() != 0) {
							if (colObj0.getIslandTag() >= 0) {
								if (colObj0.getCompanionId() >= 0) {
									// body has already been converted
									solverBodyIdA = colObj0.getCompanionId();
								}
								else {
									solverBodyIdA = tmpSolverBodyPool.size();
									SolverBody solverBody = bodiesPool.get();
									tmpSolverBodyPool.add(solverBody);
									initSolverBody(solverBody, colObj0);
									colObj0.setCompanionId(solverBodyIdA);
								}
							}
							else {
								// create a static body
								solverBodyIdA = tmpSolverBodyPool.size();
								SolverBody solverBody = bodiesPool.get();
								tmpSolverBodyPool.add(solverBody);
								initSolverBody(solverBody, colObj0);
							}

							if (colObj1.getIslandTag() >= 0) {
								if (colObj1.getCompanionId() >= 0) {
									solverBodyIdB = colObj1.getCompanionId();
								}
								else {
									solverBodyIdB = tmpSolverBodyPool.size();
									SolverBody solverBody = bodiesPool.get();
									tmpSolverBodyPool.add(solverBody);
									initSolverBody(solverBody, colObj1);
									colObj1.setCompanionId(solverBodyIdB);
								}
							}
							else {
								// create a static body
								solverBodyIdB = tmpSolverBodyPool.size();
								SolverBody solverBody = bodiesPool.get();
								tmpSolverBodyPool.add(solverBody);
								initSolverBody(solverBody, colObj1);
							}
						}

						Vector3f rel_pos1 = stack.vectors.get();
						Vector3f rel_pos2 = stack.vectors.get();
						float relaxation;

						for (int j = 0; j < manifold.getNumContacts(); j++) {

							ManifoldPoint cp = manifold.getContactPoint(j);

							if (debugDrawer != null) {
								debugDrawer.drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.getDistance(), cp.getLifeTime(), color);
							}

							if (cp.getDistance() <= 0f) {
								Vector3f pos1 = cp.getPositionWorldOnA();
								Vector3f pos2 = cp.getPositionWorldOnB();

								rel_pos1.sub(pos1, colObj0.getWorldTransform().origin);
								rel_pos2.sub(pos2, colObj1.getWorldTransform().origin);

								relaxation = 1f;
								float rel_vel;
								Vector3f vel = stack.vectors.get();

								int frictionIndex = tmpSolverConstraintPool.size();

								{
									SolverConstraint solverConstraint = constraintsPool.get();
									tmpSolverConstraintPool.add(solverConstraint);
									RigidBody rb0 = RigidBody.upcast(colObj0);
									RigidBody rb1 = RigidBody.upcast(colObj1);

									solverConstraint.solverBodyIdA = solverBodyIdA;
									solverConstraint.solverBodyIdB = solverBodyIdB;
									solverConstraint.constraintType = SolverConstraintType.SOLVER_CONTACT_1D;

									Vector3f torqueAxis0 = stack.vectors.get();
									torqueAxis0.cross(rel_pos1, cp.normalWorldOnB);

									if (rb0 != null) {
										solverConstraint.angularComponentA.set(torqueAxis0);
										rb0.getInvInertiaTensorWorld().transform(solverConstraint.angularComponentA);
									}
									else {
										solverConstraint.angularComponentA.set(0f, 0f, 0f);
									}

									Vector3f torqueAxis1 = stack.vectors.get();
									torqueAxis1.cross(rel_pos2, cp.normalWorldOnB);

									if (rb1 != null) {
										solverConstraint.angularComponentB.set(torqueAxis1);
										rb1.getInvInertiaTensorWorld().transform(solverConstraint.angularComponentB);
									}
									else {
										solverConstraint.angularComponentB.set(0f, 0f, 0f);
									}

									{
										//#ifdef COMPUTE_IMPULSE_DENOM
										//btScalar denom0 = rb0->computeImpulseDenominator(pos1,cp.m_normalWorldOnB);
										//btScalar denom1 = rb1->computeImpulseDenominator(pos2,cp.m_normalWorldOnB);
										//#else							
										Vector3f vec = stack.vectors.get();
										float denom0 = 0f;
										float denom1 = 0f;
										if (rb0 != null) {
											vec.cross(solverConstraint.angularComponentA, rel_pos1);
											denom0 = rb0.getInvMass() + cp.normalWorldOnB.dot(vec);
										}
										if (rb1 != null) {
											vec.cross(solverConstraint.angularComponentB, rel_pos2);
											denom1 = rb1.getInvMass() + cp.normalWorldOnB.dot(vec);
										}
										//#endif //COMPUTE_IMPULSE_DENOM		

										float denom = relaxation / (denom0 + denom1);
										solverConstraint.jacDiagABInv = denom;
									}

									solverConstraint.contactNormal.set(cp.normalWorldOnB);
									solverConstraint.relpos1CrossNormal.cross(rel_pos1, cp.normalWorldOnB);
									solverConstraint.relpos2CrossNormal.cross(rel_pos2, cp.normalWorldOnB);

									Vector3f vel1 = rb0 != null ? stack.vectors.get(rb0.getVelocityInLocalPoint(rel_pos1)) : stack.vectors.get(0f, 0f, 0f);
									Vector3f vel2 = rb1 != null ? stack.vectors.get(rb1.getVelocityInLocalPoint(rel_pos2)) : stack.vectors.get(0f, 0f, 0f);

									vel.sub(vel1, vel2);

									rel_vel = cp.normalWorldOnB.dot(vel);

									solverConstraint.penetration = cp.getDistance();///btScalar(infoGlobal.m_numIterations);
									solverConstraint.friction = cp.combinedFriction;
									solverConstraint.restitution = restitutionCurve(rel_vel, cp.combinedRestitution);
									if (solverConstraint.restitution <= 0f) {
										solverConstraint.restitution = 0f;
									}

									float penVel = -solverConstraint.penetration / infoGlobal.timeStep;
									solverConstraint.penetration *= -(infoGlobal.erp / infoGlobal.timeStep);

									if (solverConstraint.restitution > penVel) {
										solverConstraint.penetration = 0f;
									}

									solverConstraint.appliedImpulse = 0f;
									solverConstraint.appliedVelocityImpulse = 0f;
								}

								{
									Vector3f frictionDir1 = stack.vectors.get();
									frictionDir1.scale(rel_vel, cp.normalWorldOnB);
									frictionDir1.sub(vel, frictionDir1);

									float lat_rel_vel = frictionDir1.lengthSquared();
									if (lat_rel_vel > BulletGlobals.FLT_EPSILON)//0.0f)
									{
										frictionDir1.scale(1f / (float) Math.sqrt(lat_rel_vel));
										addFrictionConstraint(frictionDir1, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
										Vector3f frictionDir2 = stack.vectors.get();
										frictionDir2.cross(frictionDir1, cp.normalWorldOnB);
										frictionDir2.normalize();//??
										addFrictionConstraint(frictionDir2, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
									}
									else {
										// re-calculate friction direction every frame, todo: check if this is really needed
										Vector3f /*frictionDir1 = stack.vectors.get(),*/ frictionDir2 = stack.vectors.get();
										TransformUtil.planeSpace1(cp.normalWorldOnB, frictionDir1, frictionDir2);
										addFrictionConstraint(frictionDir1, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
										addFrictionConstraint(frictionDir2, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
									}
								}
							}
						}
					}
				}
			}

			// TODO: btContactSolverInfo info = infoGlobal;

			{
				int j;
				for (j = 0; j < numConstraints; j++) {
					TypedConstraint constraint = constraints.get(constraints_offset+j);
					constraint.buildJacobian();
				}
			}



			int numConstraintPool = tmpSolverConstraintPool.size();
			int numFrictionPool = tmpSolverFrictionConstraintPool.size();

			// todo: use stack allocator for such temporarily memory, same for solver bodies/constraints
			MiscUtil.resize(orderTmpConstraintPool, numConstraintPool, 0);
			MiscUtil.resize(orderFrictionConstraintPool, numFrictionPool, 0);
			{
				int i;
				for (i = 0; i < numConstraintPool; i++) {
					orderTmpConstraintPool.set(i, i);
				}
				for (i = 0; i < numFrictionPool; i++) {
					orderFrictionConstraintPool.set(i, i);
				}
			}

			return 0f;
		}
		finally {
			stack.vectors.pop();
			BulletGlobals.popProfile();
		}
	}
	
	public float solveGroupCacheFriendlyIterations(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, List<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal, IDebugDraw debugDrawer/*,btStackAlloc* stackAlloc*/) {
		BulletGlobals.pushProfile("solveGroupCacheFriendlyIterations");
		try {
			int numConstraintPool = tmpSolverConstraintPool.size();
			int numFrictionPool = tmpSolverFrictionConstraintPool.size();

			// should traverse the contacts random order...
			int iteration;
			{
				for (iteration = 0; iteration < infoGlobal.numIterations; iteration++) {

					int j;
					if ((solverMode & SolverMode.SOLVER_RANDMIZE_ORDER) != 0) {
						if ((iteration & 7) == 0) {
							for (j = 0; j < numConstraintPool; ++j) {
								int tmp = orderTmpConstraintPool.get(j);
								int swapi = randInt2(j + 1);
								orderTmpConstraintPool.set(j, orderTmpConstraintPool.get(swapi));
								orderTmpConstraintPool.set(swapi, tmp);
							}

							for (j = 0; j < numFrictionPool; ++j) {
								int tmp = orderFrictionConstraintPool.get(j);
								int swapi = randInt2(j + 1);
								orderFrictionConstraintPool.set(j, orderFrictionConstraintPool.get(swapi));
								orderFrictionConstraintPool.set(swapi, tmp);
							}
						}
					}

					for (j = 0; j < numConstraints; j++) {
						BulletGlobals.pushProfile("solveConstraint");
						try {
							TypedConstraint constraint = constraints.get(constraints_offset+j);
							// todo: use solver bodies, so we don't need to copy from/to btRigidBody

							if ((constraint.getRigidBodyA().getIslandTag() >= 0) && (constraint.getRigidBodyA().getCompanionId() >= 0)) {
								tmpSolverBodyPool.get(constraint.getRigidBodyA().getCompanionId()).writebackVelocity();
							}
							if ((constraint.getRigidBodyB().getIslandTag() >= 0) && (constraint.getRigidBodyB().getCompanionId() >= 0)) {
								tmpSolverBodyPool.get(constraint.getRigidBodyB().getCompanionId()).writebackVelocity();
							}

							constraint.solveConstraint(infoGlobal.timeStep);

							if ((constraint.getRigidBodyA().getIslandTag() >= 0) && (constraint.getRigidBodyA().getCompanionId() >= 0)) {
								tmpSolverBodyPool.get(constraint.getRigidBodyA().getCompanionId()).readVelocity();
							}
							if ((constraint.getRigidBodyB().getIslandTag() >= 0) && (constraint.getRigidBodyB().getCompanionId() >= 0)) {
								tmpSolverBodyPool.get(constraint.getRigidBodyB().getCompanionId()).readVelocity();
							}
						}
						finally {
							BulletGlobals.popProfile();
						}
					}

					{
						BulletGlobals.pushProfile("resolveSingleCollisionCombinedCacheFriendly");
						try {
							int numPoolConstraints = tmpSolverConstraintPool.size();
							for (j = 0; j < numPoolConstraints; j++) {
								SolverConstraint solveManifold = tmpSolverConstraintPool.get(orderTmpConstraintPool.get(j));
								resolveSingleCollisionCombinedCacheFriendly(tmpSolverBodyPool.get(solveManifold.solverBodyIdA),
										tmpSolverBodyPool.get(solveManifold.solverBodyIdB), solveManifold, infoGlobal);
							}
						}
						finally {
							BulletGlobals.popProfile();
						}
					}

					{
						BulletGlobals.pushProfile("resolveSingleFrictionCacheFriendly");
						try {
							int numFrictionPoolConstraints = tmpSolverFrictionConstraintPool.size();

							for (j = 0; j < numFrictionPoolConstraints; j++) {
								SolverConstraint solveManifold = tmpSolverFrictionConstraintPool.get(orderFrictionConstraintPool.get(j));
								resolveSingleFrictionCacheFriendly(tmpSolverBodyPool.get(solveManifold.solverBodyIdA),
										tmpSolverBodyPool.get(solveManifold.solverBodyIdB), solveManifold, infoGlobal,
										tmpSolverConstraintPool.get(solveManifold.frictionIndex).appliedImpulse);
							}
						}
						finally {
							BulletGlobals.popProfile();
						}
					}
				}
			}

			return 0f;
		}
		finally {
			BulletGlobals.popProfile();		
		}
	}

	public float solveGroupCacheFriendly(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, List<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal, IDebugDraw debugDrawer/*,btStackAlloc* stackAlloc*/) {
		int i;

		solveGroupCacheFriendlySetup(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal, debugDrawer/*, stackAlloc*/);
		solveGroupCacheFriendlyIterations(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal, debugDrawer/*, stackAlloc*/);

		for (i = 0; i < tmpSolverBodyPool.size(); i++) {
			SolverBody body = tmpSolverBodyPool.get(i);
			body.writebackVelocity();
			bodiesPool.release(body);
		}

		//	printf("m_tmpSolverConstraintPool.size() = %i\n",m_tmpSolverConstraintPool.size());

		/*
		printf("m_tmpSolverBodyPool.size() = %i\n",m_tmpSolverBodyPool.size());
		printf("m_tmpSolverConstraintPool.size() = %i\n",m_tmpSolverConstraintPool.size());
		printf("m_tmpSolverFrictionConstraintPool.size() = %i\n",m_tmpSolverFrictionConstraintPool.size());
		printf("m_tmpSolverBodyPool.capacity() = %i\n",m_tmpSolverBodyPool.capacity());
		printf("m_tmpSolverConstraintPool.capacity() = %i\n",m_tmpSolverConstraintPool.capacity());
		printf("m_tmpSolverFrictionConstraintPool.capacity() = %i\n",m_tmpSolverFrictionConstraintPool.capacity());
		*/

		tmpSolverBodyPool.clear();
		
		for (i=0; i<tmpSolverConstraintPool.size(); i++) {
			constraintsPool.release(tmpSolverConstraintPool.get(i));
		}
		tmpSolverConstraintPool.clear();
		
		for (i=0; i<tmpSolverFrictionConstraintPool.size(); i++) {
			constraintsPool.release(tmpSolverFrictionConstraintPool.get(i));
		}
		tmpSolverFrictionConstraintPool.clear();

		return 0f;
	}
	
	/**
	 * Sequentially applies impulses.
	 */
	@Override
	public float solveGroup(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, List<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal, IDebugDraw debugDrawer, Dispatcher dispatcher) {
		BulletGlobals.pushProfile("solveGroup");
		try {
			// TODO: solver cache friendly
			if ((getSolverMode() & SolverMode.SOLVER_CACHE_FRIENDLY) != 0) {
				// you need to provide at least some bodies
				// SimpleDynamicsWorld needs to switch off SOLVER_CACHE_FRIENDLY
				assert (bodies != null);
				assert (numBodies != 0);
				float value = solveGroupCacheFriendly(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal, debugDrawer/*,stackAlloc*/);
				return value;
			}

			ContactSolverInfo info = new ContactSolverInfo(infoGlobal);

			int numiter = infoGlobal.numIterations;

			int totalPoints = 0;
			{
				short j;
				for (j = 0; j < numManifolds; j++) {
					PersistentManifold manifold = manifoldPtr.get(manifold_offset+j);
					prepareConstraints(manifold, info, debugDrawer);

					for (short p = 0; p < manifoldPtr.get(manifold_offset+j).getNumContacts(); p++) {
						gOrder[totalPoints].manifoldIndex = j;
						gOrder[totalPoints].pointIndex = p;
						totalPoints++;
					}
				}
			}

			{
				int j;
				for (j = 0; j < numConstraints; j++) {
					TypedConstraint constraint = constraints.get(constraints_offset+j);
					constraint.buildJacobian();
				}
			}

			// should traverse the contacts random order...
			int iteration;
			{
				for (iteration = 0; iteration < numiter; iteration++) {
					int j;
					if ((solverMode & SolverMode.SOLVER_RANDMIZE_ORDER) != 0) {
						if ((iteration & 7) == 0) {
							for (j = 0; j < totalPoints; ++j) {
								// JAVA NOTE: swaps references instead of copying values (but that's fine in this context)
								OrderIndex tmp = gOrder[j];
								int swapi = randInt2(j + 1);
								gOrder[j] = gOrder[swapi];
								gOrder[swapi] = tmp;
							}
						}
					}

					for (j = 0; j < numConstraints; j++) {
						TypedConstraint constraint = constraints.get(constraints_offset+j);
						constraint.solveConstraint(info.timeStep);
					}

					for (j = 0; j < totalPoints; j++) {
						PersistentManifold manifold = manifoldPtr.get(manifold_offset+gOrder[j].manifoldIndex);
						solve((RigidBody) manifold.getBody0(),
								(RigidBody) manifold.getBody1(), manifold.getContactPoint(gOrder[j].pointIndex), info, iteration, debugDrawer);
					}

					for (j = 0; j < totalPoints; j++) {
						PersistentManifold manifold = manifoldPtr.get(manifold_offset+gOrder[j].manifoldIndex);
						solveFriction((RigidBody) manifold.getBody0(),
								(RigidBody) manifold.getBody1(), manifold.getContactPoint(gOrder[j].pointIndex), info, iteration, debugDrawer);
					}

				}
			}

			return 0f;
		}
		finally {
			BulletGlobals.popProfile();
		}
	}
	
	protected void prepareConstraints(PersistentManifold manifoldPtr, ContactSolverInfo info, IDebugDraw debugDrawer) {
		stack.pushCommonMath();
		try {
			RigidBody body0 = (RigidBody) manifoldPtr.getBody0();
			RigidBody body1 = (RigidBody) manifoldPtr.getBody1();

			// only necessary to refresh the manifold once (first iteration). The integration is done outside the loop
			{
				//#ifdef FORCE_REFESH_CONTACT_MANIFOLDS
				//manifoldPtr->refreshContactPoints(body0->getCenterOfMassTransform(),body1->getCenterOfMassTransform());
				//#endif //FORCE_REFESH_CONTACT_MANIFOLDS		
				int numpoints = manifoldPtr.getNumContacts();

				BulletGlobals.gTotalContactPoints += numpoints;

				Vector3f color = stack.vectors.get(0f, 1f, 0f);
				for (int i = 0; i < numpoints; i++) {
					ManifoldPoint cp = manifoldPtr.getContactPoint(i);
					if (cp.getDistance() <= 0f) {
						Vector3f pos1 = cp.getPositionWorldOnA();
						Vector3f pos2 = cp.getPositionWorldOnB();

						Vector3f rel_pos1 = stack.vectors.get();
						rel_pos1.sub(pos1, body0.getCenterOfMassPosition());

						Vector3f rel_pos2 = stack.vectors.get();
						rel_pos2.sub(pos2, body1.getCenterOfMassPosition());

						// this jacobian entry is re-used for all iterations
						Matrix3f mat1 = stack.matrices.get(body0.getCenterOfMassTransform().basis);
						mat1.transpose();

						Matrix3f mat2 = stack.matrices.get(body1.getCenterOfMassTransform().basis);
						mat2.transpose();

						JacobianEntry jac = jacobiansPool.get();
						jac.init(mat1, mat2,
								rel_pos1, rel_pos2, cp.normalWorldOnB, body0.getInvInertiaDiagLocal(), body0.getInvMass(),
								body1.getInvInertiaDiagLocal(), body1.getInvMass());

						float jacDiagAB = jac.getDiagonal();
						jacobiansPool.release(jac);

						ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
						if (cpd != null) {
							// might be invalid
							cpd.persistentLifeTime++;
							if (cpd.persistentLifeTime != cp.getLifeTime()) {
								//printf("Invalid: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
								//new (cpd) btConstraintPersistentData;
								cpd.reset();
								cpd.persistentLifeTime = cp.getLifeTime();

							}
							else {
							//printf("Persistent: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
							}
						}
						else {
							// todo: should this be in a pool?
							//void* mem = btAlignedAlloc(sizeof(btConstraintPersistentData),16);
							//cpd = new (mem)btConstraintPersistentData;
							cpd = new ConstraintPersistentData();
							//assert(cpd != null);

							totalCpd++;
							//printf("totalCpd = %i Created Ptr %x\n",totalCpd,cpd);
							cp.userPersistentData = cpd;
							cpd.persistentLifeTime = cp.getLifeTime();
						//printf("CREATED: %x . cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd,cpd->m_persistentLifeTime,cp.getLifeTime());
						}
						assert (cpd != null);

						cpd.jacDiagABInv = 1f / jacDiagAB;

						// Dependent on Rigidbody A and B types, fetch the contact/friction response func
						// perhaps do a similar thing for friction/restutution combiner funcs...

						cpd.frictionSolverFunc = frictionDispatch[body0.frictionSolverType][body1.frictionSolverType];
						cpd.contactSolverFunc = contactDispatch[body0.contactSolverType][body1.contactSolverType];

						Vector3f vel1 = stack.vectors.get(body0.getVelocityInLocalPoint(rel_pos1));
						Vector3f vel2 = stack.vectors.get(body1.getVelocityInLocalPoint(rel_pos2));
						Vector3f vel = stack.vectors.get();
						vel.sub(vel1, vel2);

						float rel_vel;
						rel_vel = cp.normalWorldOnB.dot(vel);

						float combinedRestitution = cp.combinedRestitution;

						cpd.penetration = cp.getDistance(); ///btScalar(info.m_numIterations);
						cpd.friction = cp.combinedFriction;
						cpd.restitution = restitutionCurve(rel_vel, combinedRestitution);
						if (cpd.restitution <= 0f) {
							cpd.restitution = 0f;
						}

						// restitution and penetration work in same direction so
						// rel_vel 

						float penVel = -cpd.penetration / info.timeStep;

						if (cpd.restitution > penVel) {
							cpd.penetration = 0f;
						}

						float relaxation = info.damping;
						if ((solverMode & SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
							cpd.appliedImpulse *= relaxation;
						}
						else {
							cpd.appliedImpulse = 0f;
						}

						// for friction
						cpd.prevAppliedImpulse = cpd.appliedImpulse;

						// re-calculate friction direction every frame, todo: check if this is really needed
						TransformUtil.planeSpace1(cp.normalWorldOnB, cpd.frictionWorldTangential0, cpd.frictionWorldTangential1);

						//#define NO_FRICTION_WARMSTART 1
						//#ifdef NO_FRICTION_WARMSTART
						cpd.accumulatedTangentImpulse0 = 0f;
						cpd.accumulatedTangentImpulse1 = 0f;
						//#endif //NO_FRICTION_WARMSTART
						float denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential0);
						float denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential0);
						float denom = relaxation / (denom0 + denom1);
						cpd.jacDiagABInvTangent0 = denom;

						denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential1);
						denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential1);
						denom = relaxation / (denom0 + denom1);
						cpd.jacDiagABInvTangent1 = denom;

						Vector3f totalImpulse = stack.vectors.get();
						//btVector3 totalImpulse = 
						//	//#ifndef NO_FRICTION_WARMSTART
						//	//cpd->m_frictionWorldTangential0*cpd->m_accumulatedTangentImpulse0+
						//	//cpd->m_frictionWorldTangential1*cpd->m_accumulatedTangentImpulse1+
						//	//#endif //NO_FRICTION_WARMSTART
						//	cp.normalWorldOnB*cpd.appliedImpulse;
						totalImpulse.scale(cpd.appliedImpulse, cp.normalWorldOnB);

						///
						{
							Vector3f torqueAxis0 = stack.vectors.get();
							torqueAxis0.cross(rel_pos1, cp.normalWorldOnB);

							cpd.angularComponentA.set(torqueAxis0);
							body0.getInvInertiaTensorWorld().transform(cpd.angularComponentA);

							Vector3f torqueAxis1 = stack.vectors.get();
							torqueAxis1.cross(rel_pos2, cp.normalWorldOnB);

							cpd.angularComponentB.set(torqueAxis1);
							body1.getInvInertiaTensorWorld().transform(cpd.angularComponentB);
						}
						{
							Vector3f ftorqueAxis0 = stack.vectors.get();
							ftorqueAxis0.cross(rel_pos1, cpd.frictionWorldTangential0);

							cpd.frictionAngularComponent0A.set(ftorqueAxis0);
							body0.getInvInertiaTensorWorld().transform(cpd.frictionAngularComponent0A);
						}
						{
							Vector3f ftorqueAxis1 = stack.vectors.get();
							ftorqueAxis1.cross(rel_pos1, cpd.frictionWorldTangential1);

							cpd.frictionAngularComponent1A.set(ftorqueAxis1);
							body0.getInvInertiaTensorWorld().transform(cpd.frictionAngularComponent1A);
						}
						{
							Vector3f ftorqueAxis0 = stack.vectors.get();
							ftorqueAxis0.cross(rel_pos2, cpd.frictionWorldTangential0);

							cpd.frictionAngularComponent0B.set(ftorqueAxis0);
							body1.getInvInertiaTensorWorld().transform(cpd.frictionAngularComponent0B);
						}
						{
							Vector3f ftorqueAxis1 = stack.vectors.get();
							ftorqueAxis1.cross(rel_pos2, cpd.frictionWorldTangential1);

							cpd.frictionAngularComponent1B.set(ftorqueAxis1);
							body1.getInvInertiaTensorWorld().transform(cpd.frictionAngularComponent1B);
						}

						///

						// apply previous frames impulse on both bodies
						body0.applyImpulse(totalImpulse, rel_pos1);

						Vector3f tmp = stack.vectors.get(totalImpulse);
						tmp.negate();
						body1.applyImpulse(tmp, rel_pos2);
					}

				}
			}
		}
		finally {
			stack.popCommonMath();
		}
	}

	public float solveCombinedContactFriction(RigidBody body0, RigidBody body1, ManifoldPoint cp, ContactSolverInfo info, int iter, IDebugDraw debugDrawer) {
		stack.vectors.push();
		try {
			float maxImpulse = 0f;
			
			Vector3f color = stack.vectors.get(0f, 1f, 0f);
			{
				if (cp.getDistance() <= 0f) {

					if (iter == 0) {
						if (debugDrawer != null) {
							debugDrawer.drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.getDistance(), cp.getLifeTime(), color);
						}
					}

					{
						//btConstraintPersistentData* cpd = (btConstraintPersistentData*) cp.m_userPersistentData;
						float impulse = ContactConstraint.resolveSingleCollisionCombined(body0, body1, cp, info);

						if (maxImpulse < impulse) {
							maxImpulse = impulse;
						}
					}
				}
			}
			return maxImpulse;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	protected float solve(RigidBody body0, RigidBody body1, ManifoldPoint cp, ContactSolverInfo info, int iter, IDebugDraw debugDrawer) {
		stack.vectors.push();
		try {
			float maxImpulse = 0f;
			
			Vector3f color = stack.vectors.get(0f, 1f, 0f);
			{
				if (cp.getDistance() <= 0f) {

					if (iter == 0) {
						if (debugDrawer != null) {
							debugDrawer.drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.getDistance(), cp.getLifeTime(), color);
						}
					}

					{
						ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
						float impulse = cpd.contactSolverFunc.invoke(body0, body1, cp, info);

						if (maxImpulse < impulse) {
							maxImpulse = impulse;
						}
					}
				}
			}
			
			return maxImpulse;
		}
		finally {
			stack.vectors.pop();
		}
	}

	protected float solveFriction(RigidBody body0, RigidBody body1, ManifoldPoint cp, ContactSolverInfo info, int iter, IDebugDraw debugDrawer) {
		stack.vectors.push();
		try {
			Vector3f color = stack.vectors.get(0f, 1f, 0f);
			{
				if (cp.getDistance() <= 0f) {
					ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
					cpd.frictionSolverFunc.invoke(body0, body1, cp, info);
				}
			}
			return 0f;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	@Override
	public void reset() {
		btSeed2 = 0;
	}
	
	/**
	 * Advanced: Override the default contact solving function for contacts, for certain types of rigidbody<br>
	 * See RigidBody.contactSolverType and RigidBody.frictionSolverType
	 */
	public void setContactSolverFunc(ContactSolverFunc func, int type0, int type1) {
		contactDispatch[type0][type1] = func;
	}
	
	/**
	 * Advanced: Override the default friction solving function for contacts, for certain types of rigidbody<br>
	 * See RigidBody.contactSolverType and RigidBody.frictionSolverType
	 */
	public void setFrictionSolverFunc(ContactSolverFunc func, int type0, int type1) {
		frictionDispatch[type0][type1] = func;
	}

	public int getSolverMode() {
		return solverMode;
	}

	public void setSolverMode(int solverMode) {
		this.solverMode = solverMode;
	}

	public void setRandSeed(long seed) {
		btSeed2 = seed;
	}

	public long getRandSeed() {
		return btSeed2;
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private static class OrderIndex {
		public int manifoldIndex;
		public int pointIndex;
	}

}
