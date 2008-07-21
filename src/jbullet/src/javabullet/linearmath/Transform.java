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
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Transform supports rigid transforms (only translation and rotation, no scaling/shear).
 * 
 * @author jezek2
 */
public class Transform {
	
	protected BulletStack stack;

	public final Matrix3f basis = new Matrix3f();
	public final Vector3f origin = new Vector3f();

	public Transform() {
	}

	public Transform(Matrix3f mat) {
		basis.set(mat);
	}

	public Transform(Transform tr) {
		set(tr);
	}
	
	public void set(Transform tr) {
		basis.set(tr.basis);
		origin.set(tr.origin);
	}
	
	public void transform(Vector3f v) {
		basis.transform(v);
		v.add(origin);
	}

	public void setIdentity() {
		basis.setIdentity();
		origin.set(0f, 0f, 0f);
	}
	
	public void inverse() {
		basis.transpose();
		origin.scale(-1f);
		basis.transform(origin);
	}

	public void inverse(Transform tr) {
		set(tr);
		inverse();
	}
	
	public void mul(Transform tr) {
		if (stack == null) stack = BulletStack.get();
		
		stack.vectors.push();
		try {
			Vector3f vec = stack.vectors.get(tr.origin);
			transform(vec);

			basis.mul(tr.basis);
			origin.set(vec);
		}
		finally {
			stack.vectors.pop();
		}
	}

	public void mul(Transform tr1, Transform tr2) {
		set(tr1);
		mul(tr2);
	}
	
	public void invXform(Vector3f inVec, Vector3f out) {
		if (stack == null) stack = BulletStack.get();

		stack.matrices.push();
		try {
			out.sub(inVec, origin);

			Matrix3f mat = stack.matrices.get(basis);
			mat.transpose();
			mat.transform(out);
		}
		finally {
			stack.matrices.pop();
		}
	}
	
	public Quat4f getRotation() {
		if (stack == null) stack = BulletStack.get();

		stack.quats.push();
		try {
			Quat4f q = stack.quats.get();
			MatrixUtil.getRotation(basis, q);
			return stack.quats.returning(q);
		}
		finally {
			stack.quats.pop();
		}
	}
	
	public void setRotation(Quat4f q) {
		MatrixUtil.setRotation(basis, q);
	}
	
	public void setFromOpenGLMatrix(float[] m) {
		MatrixUtil.setFromOpenGLSubMatrix(basis, m);
		origin.set(m[12], m[13], m[14]);
	}

	public void getOpenGLMatrix(float[] m) {
		MatrixUtil.getOpenGLSubMatrix(basis, m);
		m[12] = origin.x;
		m[13] = origin.y;
		m[14] = origin.z;
		m[15] = 1f;
	}
	
}
