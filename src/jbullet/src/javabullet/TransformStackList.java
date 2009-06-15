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

import javabullet.linearmath.Transform;
import javax.vecmath.Matrix3f;

/**
 * Stack-based object pool for {@link Transform}.
 * 
 * @author jezek2
 */
public class TransformStackList extends StackList<Transform> {
	
	public Transform get(Transform tr) {
		Transform obj = get();
		obj.set(tr);
		return obj;
	}

	public Transform get(Matrix3f mat) {
		Transform obj = get();
		obj.basis.set(mat);
		obj.origin.set(0f, 0f, 0f);
		return obj;
	}

	@Override
	protected Transform create() {
		return new Transform();
	}
	
	@Override
	protected void copy(Transform dest, Transform src) {
		dest.set(src);
	}
	
}
