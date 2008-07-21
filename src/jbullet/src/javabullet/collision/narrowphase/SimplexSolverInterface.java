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

import javax.vecmath.Vector3f;

/**
 * SimplexSolverInterface can incrementally calculate distance between origin and up to 4 vertices
 * Used by GJK or Linear Casting. Can be implemented by the Johnson-algorithm or alternative approaches based on
 * voronoi regions or barycentric coordinates.
 * 
 * @author jezek2
 */
public interface SimplexSolverInterface {

	public void reset();

	public void addVertex(Vector3f w, Vector3f p, Vector3f q);
	
	public boolean closest(Vector3f v);

	public float maxVertex();

	public boolean fullSimplex();

	public int getSimplex(Vector3f[] pBuf, Vector3f[] qBuf, Vector3f[] yBuf);

	public boolean inSimplex(Vector3f w);
	
	public void backup_closest(Vector3f v);

	public boolean emptySimplex();

	public void compute_points(Vector3f p1, Vector3f p2);

	public int numVertices();
	
}
