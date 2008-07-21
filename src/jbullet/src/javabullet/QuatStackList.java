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

import javax.vecmath.Quat4f;

/**
 * Stack-based object pool for {@link Quat4f}.
 * 
 * @author jezek2
 */
public class QuatStackList extends StackList<Quat4f> {

	public Quat4f get(float x, float y, float z, float w) {
		Quat4f v = get();
		v.set(x, y, z, w);
		return v;
	}

	public Quat4f get(Quat4f quat) {
		Quat4f obj = get();
		obj.set(quat);
		return obj;
	}

	@Override
	protected Quat4f create() {
		return new Quat4f();
	}

	@Override
	protected void copy(Quat4f dest, Quat4f src) {
		dest.set(src);
	}
	
}
