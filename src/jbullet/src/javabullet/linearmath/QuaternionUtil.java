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

import javabullet.BulletGlobals;
import javabullet.BulletStack;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 *
 * @author jezek2
 */
public class QuaternionUtil {

	public static float getAngle(Quat4f q) {
		float s = 2f * (float) Math.acos(q.w);
		return s;
	}
	
	public static void setRotation(Quat4f q, Vector3f axis, float angle) {
		float d = axis.length();
		assert (d != 0f);
		float s = (float)Math.sin(angle * 0.5f) / d;
		q.set(axis.x * s, axis.y * s, axis.z * s, (float) Math.cos(angle * 0.5f));
	}
	
	// Game Programming Gems 2.10. make sure v0,v1 are normalized
	public static Quat4f shortestArcQuat(Vector3f v0, Vector3f v1) {
		BulletStack stack = BulletStack.get();

		stack.vectors.push();
		stack.quats.push();
		try {
			Vector3f c = stack.vectors.get();
			c.cross(v0, v1);
			float d = v0.dot(v1);

			if (d < -1.0 + BulletGlobals.FLT_EPSILON) {
				// just pick any vector
				return stack.quats.returning(stack.quats.get(0.0f, 1.0f, 0.0f, 0.0f));
			}

			float s = (float) Math.sqrt((1.0f + d) * 2.0f);
			float rs = 1.0f / s;

			return stack.quats.returning(stack.quats.get(c.x * rs, c.y * rs, c.z * rs, s * 0.5f));
		}
		finally {
			stack.vectors.pop();
			stack.quats.pop();
		}
	}
	
	public static void mul(Quat4f q, Vector3f w) {
		float rx = q.w * w.x + q.y * w.z - q.z * w.y;
		float ry = q.w * w.y + q.z * w.x - q.x * w.z;
		float rz = q.w * w.z + q.x * w.y - q.y * w.x;
		float rw = -q.x * w.x - q.y * w.y - q.z * w.z;
		q.set(rx, ry, rz, rw);
	}
	
	public static Vector3f quatRotate(Quat4f rotation, Vector3f v) {
		BulletStack stack = BulletStack.get();

		stack.vectors.push();
		stack.quats.push();
		try {
			Quat4f q = stack.quats.get(rotation);
			QuaternionUtil.mul(q, v);

			Quat4f tmp = stack.quats.get();
			tmp.inverse(rotation);
			q.mul(tmp);
			return stack.vectors.returning(stack.vectors.get(q.x, q.y, q.z));
		}
		finally {
			stack.vectors.pop();
			stack.quats.pop();
		}
	}
	
}
