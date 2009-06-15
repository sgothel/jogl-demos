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

package javabullet;

import javax.vecmath.Vector4f;

/**
 * Stack-based object pool for {@link Vector4f}.
 * 
 * @author jezek2
 */
public class Vector4StackList extends StackList<Vector4f> {

	public Vector4f get(float x, float y, float z, float w) {
		Vector4f v = get();
		v.set(x, y, z, w);
		return v;
	}

	public Vector4f get(Vector4f vec) {
		Vector4f v = get();
		v.set(vec);
		return v;
	}

	@Override
	protected Vector4f create() {
		return new Vector4f();
	}

	@Override
	protected void copy(Vector4f dest, Vector4f src) {
		dest.set(src);
	}

}
