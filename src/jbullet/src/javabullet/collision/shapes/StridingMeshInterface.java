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

package javabullet.collision.shapes;

import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 * StridingMeshInterface is the interface class for high performance access to triangle meshes.
 * It allows for sharing graphics and collision meshes. Also it provides locking/unlocking of graphics meshes that are in gpu memory.
 * 
 * @author jezek2
 */
public abstract class StridingMeshInterface {

	protected final Vector3f scaling = new Vector3f(1f, 1f, 1f);
	
	private VertexData data = new VertexData();
	
	public void internalProcessAllTriangles(InternalTriangleIndexCallback callback, Vector3f aabbMin, Vector3f aabbMax) {
		int numtotalphysicsverts = 0;
		int part, graphicssubparts = getNumSubParts();
		int gfxindex;
		Vector3f[] triangle/*[3]*/ = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f()};
		int graphicsbase;

		Vector3f meshScaling = new Vector3f(getScaling());

		// if the number of parts is big, the performance might drop due to the innerloop switch on indextype
		for (part = 0; part < graphicssubparts; part++) {
			getLockedReadOnlyVertexIndexBase(data, part);
			numtotalphysicsverts += data.numfaces * 3; // upper bound

			switch (data.indicestype) {
				case PHY_INTEGER: {
					for (gfxindex = 0; gfxindex < data.numfaces; gfxindex++) {
						//int* tri_indices= (int*)(indexbase+gfxindex*indexstride);
						int tri_indices = gfxindex * data.indexstride;

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[0]*stride);
						graphicsbase = data.indexbase.getInt(tri_indices + 0) * data.stride;

						//triangle[0].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[0].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[1]*stride);
						graphicsbase = data.indexbase.getInt(tri_indices + 4) * data.stride;

						//triangle[1].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[1].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[2]*stride);
						graphicsbase = data.indexbase.getInt(tri_indices + 8) * data.stride;

						//triangle[2].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[2].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						callback.internalProcessTriangleIndex(triangle, part, gfxindex);
					}
					break;
				}
				case PHY_SHORT: {
					for (gfxindex = 0; gfxindex < data.numfaces; gfxindex++) {
						//short int* tri_indices= (short int*)(indexbase+gfxindex*indexstride);
						int tri_indices = gfxindex * data.indexstride;

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[0]*stride);
						graphicsbase = (data.indexbase.getShort(tri_indices + 0) & 0xFFFF) * data.stride;

						//triangle[0].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[0].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[1]*stride);
						graphicsbase = (data.indexbase.getShort(tri_indices + 2) & 0xFFFF) * data.stride;

						//triangle[1].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[1].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						//graphicsbase = (btScalar*)(vertexbase+tri_indices[2]*stride);
						graphicsbase = (data.indexbase.getShort(tri_indices + 4) & 0xFFFF) * data.stride;

						//triangle[1].setValue(graphicsbase[0]*meshScaling.getX(),graphicsbase[1]*meshScaling.getY(),graphicsbase[2]*meshScaling.getZ());
						triangle[2].set(
								data.vertexbase.getFloat(graphicsbase + 0) * meshScaling.x,
								data.vertexbase.getFloat(graphicsbase + 4) * meshScaling.y,
								data.vertexbase.getFloat(graphicsbase + 8) * meshScaling.z);

						callback.internalProcessTriangleIndex(triangle, part, gfxindex);
					}
					break;
				}
				default:
					assert ((data.indicestype == ScalarType.PHY_INTEGER) || (data.indicestype == ScalarType.PHY_SHORT));
			}

			unLockReadOnlyVertexBase(part);
		}
		
		data.unref();
	}

	private static class AabbCalculationCallback implements InternalTriangleIndexCallback {
		public final Vector3f aabbMin = new Vector3f(1e30f, 1e30f, 1e30f);
		public final Vector3f aabbMax = new Vector3f(-1e30f, -1e30f, -1e30f);

		public void internalProcessTriangleIndex(Vector3f[] triangle, int partId, int triangleIndex) {
			VectorUtil.setMin(aabbMin, triangle[0]);
			VectorUtil.setMax(aabbMax, triangle[0]);
			VectorUtil.setMin(aabbMin, triangle[1]);
			VectorUtil.setMax(aabbMax, triangle[1]);
			VectorUtil.setMin(aabbMin, triangle[2]);
			VectorUtil.setMax(aabbMax, triangle[2]);
		}
	}
	
	public void calculateAabbBruteForce(Vector3f aabbMin, Vector3f aabbMax) {
		// first calculate the total aabb for all triangles
		AabbCalculationCallback aabbCallback = new AabbCalculationCallback();
		aabbMin.set(-1e30f, -1e30f, -1e30f);
		aabbMax.set(1e30f, 1e30f, 1e30f);
		internalProcessAllTriangles(aabbCallback, aabbMin, aabbMax);

		aabbMin.set(aabbCallback.aabbMin);
		aabbMax.set(aabbCallback.aabbMax);
	}
	
	/**
	 * Get read and write access to a subpart of a triangle mesh.
	 * This subpart has a continuous array of vertices and indices.
	 * In this way the mesh can be handled as chunks of memory with striding
	 * very similar to OpenGL vertexarray support.
	 * Make a call to unLockVertexBase when the read and write access is finished.
	 */
	public abstract void getLockedVertexIndexBase(VertexData data, int subpart/*=0*/);

	public abstract void getLockedReadOnlyVertexIndexBase(VertexData data, int subpart/*=0*/);

	/**
	 * unLockVertexBase finishes the access to a subpart of the triangle mesh.
	 * Make a call to unLockVertexBase when the read and write access (using getLockedVertexIndexBase) is finished.
	 */
	public abstract void unLockVertexBase(int subpart);

	public abstract void unLockReadOnlyVertexBase(int subpart);

	/**
	 * getNumSubParts returns the number of seperate subparts.
	 * Each subpart has a continuous array of vertices and indices.
	 */
	public abstract int getNumSubParts();

	public abstract void preallocateVertices(int numverts);
	public abstract void preallocateIndices(int numindices);

	public Vector3f getScaling() {
		return scaling;
	}
	
	public void setScaling(Vector3f scaling)
	{
		this.scaling.set(scaling);
	}
	
}
