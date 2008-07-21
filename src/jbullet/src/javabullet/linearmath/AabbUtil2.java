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

package javabullet.linearmath;

import javabullet.BulletStack;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class AabbUtil2 {

	public static int outcode(Vector3f p, Vector3f halfExtent) {
		return (p.x < -halfExtent.x ? 0x01 : 0x0) |
				(p.x > halfExtent.x ? 0x08 : 0x0) |
				(p.y < -halfExtent.y ? 0x02 : 0x0) |
				(p.y > halfExtent.y ? 0x10 : 0x0) |
				(p.z < -halfExtent.z ? 0x4 : 0x0) |
				(p.z > halfExtent.z ? 0x20 : 0x0);
	}
	
	public static boolean rayAabb(Vector3f rayFrom, Vector3f rayTo, Vector3f aabbMin, Vector3f aabbMax, float[] param, Vector3f normal) {
		BulletStack stack = BulletStack.get();
		
		stack.vectors.push();
		try {
			Vector3f aabbHalfExtent = stack.vectors.get();
			Vector3f aabbCenter = stack.vectors.get();
			Vector3f source = stack.vectors.get();
			Vector3f target = stack.vectors.get();
			Vector3f r = stack.vectors.get();
			Vector3f hitNormal = stack.vectors.get();

			aabbHalfExtent.sub(aabbMax, aabbMin);
			aabbHalfExtent.scale(0.5f);

			aabbCenter.add(aabbMax, aabbMin);
			aabbCenter.scale(0.5f);

			source.sub(rayFrom, aabbCenter);
			target.sub(rayTo, aabbCenter);

			int sourceOutcode = outcode(source, aabbHalfExtent);
			int targetOutcode = outcode(target, aabbHalfExtent);
			if ((sourceOutcode & targetOutcode) == 0x0) {
				float lambda_enter = 0f;
				float lambda_exit = param[0];
				r.sub(target, source);

				float normSign = 1f;
				hitNormal.set(0f, 0f, 0f);
				int bit = 1;

				for (int j = 0; j < 2; j++) {
					for (int i = 0; i != 3; ++i) {
						if ((sourceOutcode & bit) != 0) {
							float lambda = (-VectorUtil.getCoord(source, i) - VectorUtil.getCoord(aabbHalfExtent, i) * normSign) / VectorUtil.getCoord(r, i);
							if (lambda_enter <= lambda) {
								lambda_enter = lambda;
								hitNormal.set(0f, 0f, 0f);
								VectorUtil.setCoord(hitNormal, i, normSign);
							}
						}
						else if ((targetOutcode & bit) != 0) {
							float lambda = (-VectorUtil.getCoord(source, i) - VectorUtil.getCoord(aabbHalfExtent, i) * normSign) / VectorUtil.getCoord(r, i);
							//btSetMin(lambda_exit, lambda);
							lambda_exit = Math.min(lambda_exit, lambda);
						}
						bit <<= 1;
					}
					normSign = -1f;
				}
				if (lambda_enter <= lambda_exit) {
					param[0] = lambda_enter;
					normal.set(hitNormal);
					return true;
				}
			}
			return false;
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	/**
	 * Conservative test for overlap between two aabbs.
	 */
	public static boolean testAabbAgainstAabb2(Vector3f aabbMin1, Vector3f aabbMax1, Vector3f aabbMin2, Vector3f aabbMax2) {
		boolean overlap = true;
		overlap = (aabbMin1.x > aabbMax2.x || aabbMax1.x < aabbMin2.x) ? false : overlap;
		overlap = (aabbMin1.z > aabbMax2.z || aabbMax1.z < aabbMin2.z) ? false : overlap;
		overlap = (aabbMin1.y > aabbMax2.y || aabbMax1.y < aabbMin2.y) ? false : overlap;
		return overlap;
	}
	
	/**
	 * Conservative test for overlap between triangle and aabb.
	 */
	public static boolean testTriangleAgainstAabb2(Vector3f[] vertices, Vector3f aabbMin, Vector3f aabbMax) {
		Vector3f p1 = vertices[0];
		Vector3f p2 = vertices[1];
		Vector3f p3 = vertices[2];

		if (Math.min(Math.min(p1.x, p2.x), p3.x) > aabbMax.x) return false;
		if (Math.max(Math.max(p1.x, p2.x), p3.x) < aabbMin.x) return false;

		if (Math.min(Math.min(p1.z, p2.z), p3.z) > aabbMax.z) return false;
		if (Math.max(Math.max(p1.z, p2.z), p3.z) < aabbMin.z) return false;

		if (Math.min(Math.min(p1.y, p2.y), p3.y) > aabbMax.y) return false;
		if (Math.max(Math.max(p1.y, p2.y), p3.y) < aabbMin.y) return false;
		
		return true;
	}
	
}
