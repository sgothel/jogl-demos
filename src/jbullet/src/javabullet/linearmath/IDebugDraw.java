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
public abstract class IDebugDraw {
	
	protected final BulletStack stack = BulletStack.get();

	public abstract void drawLine(Vector3f from, Vector3f to, Vector3f color);

	public abstract void drawContactPoint(Vector3f PointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color);

	public abstract void reportErrorWarning(String warningString);

	public abstract void draw3dText(Vector3f location, String textString);

	public abstract void setDebugMode(int debugMode);

	public abstract int getDebugMode();

	public void drawAabb(Vector3f from, Vector3f to, Vector3f color) {
		stack.vectors.push();
		try {
			Vector3f halfExtents = stack.vectors.get(to);
			halfExtents.sub(from);
			halfExtents.scale(0.5f);

			Vector3f center = stack.vectors.get(to);
			center.add(from);
			center.scale(0.5f);

			int i, j;

			Vector3f edgecoord = stack.vectors.get(1f, 1f, 1f), pa = stack.vectors.get(), pb = stack.vectors.get();
			for (i = 0; i < 4; i++) {
				for (j = 0; j < 3; j++) {
					pa.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z);
					pa.add(center);

					int othercoord = j % 3;

					VectorUtil.mulCoord(edgecoord, othercoord, -1f);
					pb.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z);
					pb.add(center);

					drawLine(pa, pb, color);
				}
				edgecoord.set(-1f, -1f, -1f);
				if (i < 3) {
					VectorUtil.mulCoord(edgecoord, i, -1f);
				}
			}
		}
		finally {
			stack.vectors.pop();
		}
	}
}
