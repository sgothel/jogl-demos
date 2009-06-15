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

import javax.vecmath.Matrix3f;

/**
 * Stack-based object pool for {@link Matrix3f}.
 * 
 * @author jezek2
 */
public class MatrixStackList extends StackList<Matrix3f> {

	public Matrix3f get(Matrix3f mat) {
		Matrix3f obj = get();
		obj.set(mat);
		return obj;
	}
	
	@Override
	protected Matrix3f create() {
		return new Matrix3f();
	}

	@Override
	protected void copy(Matrix3f dest, Matrix3f src) {
		dest.set(src);
	}

}
