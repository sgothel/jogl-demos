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

import java.util.List;
import javabullet.BulletStack;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 *
 * @author jezek2
 */
public class GeometryUtil {

	public static boolean isPointInsidePlanes(List<Vector4f> planeEquations, Vector3f point, float margin) {
		int numbrushes = planeEquations.size();
		for (int i = 0; i < numbrushes; i++) {
			Vector4f N1 = planeEquations.get(i);
			float dist = VectorUtil.dot3(N1, point) + N1.w - margin;
			if (dist > 0f) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean areVerticesBehindPlane(Vector4f planeNormal, List<Vector3f> vertices, float margin) {
		int numvertices = vertices.size();
		for (int i = 0; i < numvertices; i++) {
			Vector3f N1 = vertices.get(i);
			float dist = VectorUtil.dot3(planeNormal, N1) + planeNormal.w - margin;
			if (dist > 0f) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean notExist(Vector4f planeEquation, List<Vector4f> planeEquations) {
		int numbrushes = planeEquations.size();
		for (int i = 0; i < numbrushes; i++) {
			Vector4f N1 = planeEquations.get(i);
			if (VectorUtil.dot3(planeEquation, N1) > 0.999f) {
				return false;
			}
		}
		return true;
	}

	public static void getPlaneEquationsFromVertices(List<Vector3f> vertices, List<Vector4f> planeEquationsOut) {
		BulletStack stack = BulletStack.get();
		
		stack.vectors.push();
		stack.vectors4.push();
		try {
			Vector4f planeEquation = stack.vectors4.get();
			Vector3f edge0 = stack.vectors.get(), edge1 = stack.vectors.get();
			Vector3f tmp = stack.vectors.get();

			int numvertices = vertices.size();
			// brute force:
			for (int i = 0; i < numvertices; i++) {
				Vector3f N1 = vertices.get(i);

				for (int j = i + 1; j < numvertices; j++) {
					Vector3f N2 = vertices.get(j);

					for (int k = j + 1; k < numvertices; k++) {
						Vector3f N3 = vertices.get(k);

						edge0.sub(N2, N1);
						edge1.sub(N3, N1);
						float normalSign = 1f;
						for (int ww = 0; ww < 2; ww++) {
							tmp.cross(edge0, edge1);
							planeEquation.x = normalSign * tmp.x;
							planeEquation.y = normalSign * tmp.y;
							planeEquation.z = normalSign * tmp.z;

							if (VectorUtil.lengthSquared3(planeEquation) > 0.0001f) {
								VectorUtil.normalize3(planeEquation);
								if (notExist(planeEquation, planeEquationsOut)) {
									planeEquation.w = -VectorUtil.dot3(planeEquation, N1);

									// check if inside, and replace supportingVertexOut if needed
									if (areVerticesBehindPlane(planeEquation, vertices, 0.01f)) {
										planeEquationsOut.add(planeEquation);
									}
								}
							}
							normalSign = -1f;
						}
					}
				}
			}
		}
		finally {
			stack.vectors.pop();
			stack.vectors4.pop();
		}
	}
	
	public static void getVerticesFromPlaneEquations(List<Vector4f> planeEquations, List<Vector3f> verticesOut) {
		BulletStack stack = BulletStack.get();
		
		stack.vectors.push();
		try {
			Vector3f n2n3 = stack.vectors.get();
			Vector3f n3n1 = stack.vectors.get();
			Vector3f n1n2 = stack.vectors.get();
			Vector3f potentialVertex = stack.vectors.get();

			int numbrushes = planeEquations.size();
			// brute force:
			for (int i = 0; i < numbrushes; i++) {
				Vector4f N1 = planeEquations.get(i);

				for (int j = i + 1; j < numbrushes; j++) {
					Vector4f N2 = planeEquations.get(j);

					for (int k = j + 1; k < numbrushes; k++) {
						Vector4f N3 = planeEquations.get(k);

						VectorUtil.cross3(n2n3, N2, N3);
						VectorUtil.cross3(n3n1, N3, N1);
						VectorUtil.cross3(n1n2, N1, N2);

						if ((n2n3.lengthSquared() > 0.0001f) &&
								(n3n1.lengthSquared() > 0.0001f) &&
								(n1n2.lengthSquared() > 0.0001f)) {
							// point P out of 3 plane equations:

							// 	     d1 ( N2 * N3 ) + d2 ( N3 * N1 ) + d3 ( N1 * N2 )  
							// P =  -------------------------------------------------------------------------  
							//    N1 . ( N2 * N3 )  

							float quotient = VectorUtil.dot3(N1, n2n3);
							if (Math.abs(quotient) > 0.000001f) {
								quotient = -1f / quotient;
								n2n3.scale(N1.w);
								n3n1.scale(N2.w);
								n1n2.scale(N3.w);
								potentialVertex.set(n2n3);
								potentialVertex.add(n3n1);
								potentialVertex.add(n1n2);
								potentialVertex.scale(quotient);

								// check if inside, and replace supportingVertexOut if needed
								if (isPointInsidePlanes(planeEquations, potentialVertex, 0.01f)) {
									verticesOut.add(potentialVertex);
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
	
}
