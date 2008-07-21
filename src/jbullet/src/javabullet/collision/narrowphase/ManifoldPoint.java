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

package javabullet.collision.narrowphase;

import javax.vecmath.Vector3f;

/**
 * ManifoldContactPoint collects and maintains persistent contactpoints.
 * Used to improve stability and performance of rigidbody dynamics response.
 * 
 * @author jezek2
 */
public class ManifoldPoint {

	public final Vector3f localPointA = new Vector3f();
	public final Vector3f localPointB = new Vector3f();
	public final Vector3f positionWorldOnB = new Vector3f();
	///m_positionWorldOnA is redundant information, see getPositionWorldOnA(), but for clarity
	public final Vector3f positionWorldOnA = new Vector3f();
	public final Vector3f normalWorldOnB = new Vector3f();
	
	public float distance1;
	public float combinedFriction;
	public float combinedRestitution;
	
	public Object userPersistentData;
	public int lifeTime; //lifetime of the contactpoint in frames

	public ManifoldPoint() {
	}
	
	public ManifoldPoint(Vector3f pointA, Vector3f pointB, Vector3f normal, float distance) {
		init(pointA, pointB, normal, distance);
	}

	public void init(Vector3f pointA, Vector3f pointB, Vector3f normal, float distance) {
		this.localPointA.set(pointA);
		this.localPointB.set(pointB);
		this.normalWorldOnB.set(normal);
		this.distance1 = distance;
	}

	public float getDistance() {
		return distance1;
	}

	public int getLifeTime() {
		return lifeTime;
	}
	
	public void set(ManifoldPoint p) {
		localPointA.set(p.localPointA);
		localPointB.set(p.localPointB);
		positionWorldOnA.set(p.positionWorldOnA);
		positionWorldOnB.set(p.positionWorldOnB);
		normalWorldOnB.set(p.normalWorldOnB);
		distance1 = p.distance1;
		combinedFriction = p.combinedFriction;
		combinedRestitution = p.combinedRestitution;
		userPersistentData = p.userPersistentData;
		lifeTime = p.lifeTime;
	}
	
	public Vector3f getPositionWorldOnA() {
		return positionWorldOnA;
		//return m_positionWorldOnB + m_normalWorldOnB * m_distance1;
	}

	public Vector3f getPositionWorldOnB() {
		return positionWorldOnB;
	}

	public void setDistance(float dist) {
		distance1 = dist;
	}
	
}
