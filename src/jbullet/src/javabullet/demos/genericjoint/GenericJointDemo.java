/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Ragdoll Demo
 * Copyright (c) 2007 Starbreeze Studios
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
 * Originally Written by: Marten Svanfeldt
 * ReWritten by: Francisco Le�n
 */

package javabullet.demos.genericjoint;

import java.util.ArrayList;
import java.util.List;
import javabullet.collision.broadphase.BroadphaseInterface;
import javabullet.collision.broadphase.SimpleBroadphase;
import javabullet.collision.dispatch.CollisionDispatcher;
import javabullet.collision.dispatch.DefaultCollisionConfiguration;
import javabullet.collision.shapes.BoxShape;
import javabullet.collision.shapes.CollisionShape;
import javabullet.demos.opengl.DemoApplication;
import javabullet.demos.opengl.JOGL;
import javabullet.dynamics.DiscreteDynamicsWorld;
import javabullet.dynamics.constraintsolver.ConstraintSolver;
import javabullet.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import javabullet.linearmath.Transform;
import javax.vecmath.Vector3f;
import javax.media.opengl.*;

/**
 *
 * @author jezek2
 */
public class GenericJointDemo extends DemoApplication {

	private List<RagDoll> ragdolls = new ArrayList<RagDoll>();

	public GenericJointDemo() {
		super();
	}

	public void initPhysics() {
		// Setup the basic world
		DefaultCollisionConfiguration collision_config = new DefaultCollisionConfiguration();

		CollisionDispatcher dispatcher = new CollisionDispatcher(collision_config);

		//btPoint3 worldAabbMin(-10000,-10000,-10000);
		//btPoint3 worldAabbMax(10000,10000,10000);
		//btBroadphaseInterface* overlappingPairCache = new btAxisSweep3 (worldAabbMin, worldAabbMax);
		BroadphaseInterface overlappingPairCache = new SimpleBroadphase();

		//#ifdef USE_ODE_QUICKSTEP
		//btConstraintSolver* constraintSolver = new OdeConstraintSolver();
		//#else
		ConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();
		//#endif

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, constraintSolver, collision_config);

		dynamicsWorld.setGravity(new Vector3f(0f, -30f, 0f));

		// Setup a big ground box
		{
			CollisionShape groundShape = new BoxShape(new Vector3f(200f, 10f, 200f));
			Transform groundTransform = new Transform();
			groundTransform.setIdentity();
			groundTransform.origin.set(0f, -15f, 0f);
			localCreateRigidBody(0f, groundTransform, groundShape);
		}

		// Spawn one ragdoll
		spawnRagdoll();

		clientResetScene();
	}

	public void spawnRagdoll() {
		spawnRagdoll(false);
	}
	
	public void spawnRagdoll(boolean random) {
		RagDoll ragDoll = new RagDoll(dynamicsWorld, new Vector3f(0f, 0f, 10f), 5f);
		ragdolls.add(ragDoll);
	}
	
	@Override
	public void keyboardCallback(char key) {
		switch (key) {
			case 'e':
				spawnRagdoll(true);
				break;
			default:
				super.keyboardCallback(key);
		}
	}

	public static void main(String[] args) {
		GenericJointDemo demoApp = new GenericJointDemo();
		demoApp.initPhysics();
		demoApp.setCameraDistance(10f);

		JOGL.main("Joint 6DOF - Sequencial Impulse Solver", demoApp, args);
	}
	
}
