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

import javabullet.BulletStack;
import javabullet.collision.shapes.TriangleCallback;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public abstract class TriangleRaycastCallback implements TriangleCallback {
	
	protected final BulletStack stack = BulletStack.get();

	public final Vector3f from = new Vector3f();
	public final Vector3f to = new Vector3f();

	public float hitFraction;

	public TriangleRaycastCallback(Vector3f from, Vector3f to) {
		this.from.set(from);
		this.to.set(to);
		this.hitFraction = 1f;
	}
	
	public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
		stack.vectors.push();
		try {
			Vector3f vert0 = triangle[0];
			Vector3f vert1 = triangle[1];
			Vector3f vert2 = triangle[2];

			Vector3f v10 = stack.vectors.get();
			v10.sub(vert1, vert0);

			Vector3f v20 = stack.vectors.get();
			v20.sub(vert2, vert0);

			Vector3f triangleNormal = stack.vectors.get();
			triangleNormal.cross(v10, v20);

			float dist = vert0.dot(triangleNormal);
			float dist_a = triangleNormal.dot(from);
			dist_a -= dist;
			float dist_b = triangleNormal.dot(to);
			dist_b -= dist;

			if (dist_a * dist_b >= 0f) {
				return; // same sign
			}

			float proj_length = dist_a - dist_b;
			float distance = (dist_a) / (proj_length);
			// Now we have the intersection point on the plane, we'll see if it's inside the triangle
			// Add an epsilon as a tolerance for the raycast,
			// in case the ray hits exacly on the edge of the triangle.
			// It must be scaled for the triangle size.

			if (distance < hitFraction) {
				float edge_tolerance = triangleNormal.lengthSquared();
				edge_tolerance *= -0.0001f;
				Vector3f point = new Vector3f();
				VectorUtil.setInterpolate3(point, from, to, distance);
				{
					Vector3f v0p = stack.vectors.get();
					v0p.sub(vert0, point);
					Vector3f v1p = stack.vectors.get();
					v1p.sub(vert1, point);
					Vector3f cp0 = stack.vectors.get();
					cp0.cross(v0p, v1p);

					if (cp0.dot(triangleNormal) >= edge_tolerance) {
						Vector3f v2p = stack.vectors.get();
						v2p.sub(vert2, point);
						Vector3f cp1 = stack.vectors.get();
						cp1.cross(v1p, v2p);
						if (cp1.dot(triangleNormal) >= edge_tolerance) {
							Vector3f cp2 = stack.vectors.get();
							cp2.cross(v2p, v0p);

							if (cp2.dot(triangleNormal) >= edge_tolerance) {

								if (dist_a > 0f) {
									hitFraction = reportHit(triangleNormal, distance, partId, triangleIndex);
								}
								else {
									Vector3f tmp = stack.vectors.get();
									tmp.negate(triangleNormal);
									hitFraction = reportHit(tmp, distance, partId, triangleIndex);
								}
							}
						}
					}
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}

	public abstract float reportHit(Vector3f hitNormalLocal, float hitFraction, int partId, int triangleIndex );

}
