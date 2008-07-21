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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jezek2
 */
public class TriangleIndexVertexArray extends StridingMeshInterface {

	private List<IndexedMesh> indexedMeshes = new ArrayList<IndexedMesh>();

	public TriangleIndexVertexArray() {
	}

	/**
	 * Just to be backwards compatible.
	 */
	public TriangleIndexVertexArray(int numTriangles, ByteBuffer triangleIndexBase, int triangleIndexStride, int numVertices, ByteBuffer vertexBase, int vertexStride) {
		IndexedMesh mesh = new IndexedMesh();

		mesh.numTriangles = numTriangles;
		mesh.triangleIndexBase = triangleIndexBase;
		mesh.triangleIndexStride = triangleIndexStride;
		mesh.numVertices = numVertices;
		mesh.vertexBase = vertexBase;
		mesh.vertexStride = vertexStride;

		addIndexedMesh(mesh);
	}

	public void addIndexedMesh(IndexedMesh mesh) {
		addIndexedMesh(mesh, ScalarType.PHY_INTEGER);
	}

	public void addIndexedMesh(IndexedMesh mesh, ScalarType indexType) {
		indexedMeshes.add(mesh);
		indexedMeshes.get(indexedMeshes.size() - 1).indexType = indexType;
	}
	
	@Override
	public void getLockedVertexIndexBase(VertexData data, int subpart) {
		assert (subpart < getNumSubParts());

		IndexedMesh mesh = indexedMeshes.get(subpart);

		data.numverts = mesh.numVertices;
		data.vertexbase = mesh.vertexBase;
		//#ifdef BT_USE_DOUBLE_PRECISION
		//type = PHY_DOUBLE;
		//#else
		data.type = ScalarType.PHY_FLOAT;
		//#endif
		data.stride = mesh.vertexStride;

		data.numfaces = mesh.numTriangles;

		data.indexbase = mesh.triangleIndexBase;
		data.indexstride = mesh.triangleIndexStride;
		data.indicestype = mesh.indexType;
	}

	@Override
	public void getLockedReadOnlyVertexIndexBase(VertexData data, int subpart) {
		IndexedMesh mesh = indexedMeshes.get(subpart);

		data.numverts = mesh.numVertices;
		data.vertexbase = mesh.vertexBase;
		//#ifdef BT_USE_DOUBLE_PRECISION
		//type = PHY_DOUBLE;
		//#else
		data.type = ScalarType.PHY_FLOAT;
		//#endif
		data.stride = mesh.vertexStride;

		data.numfaces = mesh.numTriangles;
		data.indexbase = mesh.triangleIndexBase;
		data.indexstride = mesh.triangleIndexStride;
		data.indicestype = mesh.indexType;
	}

	/**
	 * unLockVertexBase finishes the access to a subpart of the triangle mesh.
	 * Make a call to unLockVertexBase when the read and write access (using getLockedVertexIndexBase) is finished.
	 */
	@Override
	public void unLockVertexBase(int subpart) {
	}

	@Override
	public void unLockReadOnlyVertexBase(int subpart) {
	}

	/**
	 * getNumSubParts returns the number of seperate subparts.
	 * Each subpart has a continuous array of vertices and indices.
	 */
	@Override
	public int getNumSubParts() {
		return indexedMeshes.size();
	}

	public List<IndexedMesh> getIndexedMeshArray() {
		return indexedMeshes;
	}
	
	@Override
	public void preallocateVertices(int numverts) {
	}

	@Override
	public void preallocateIndices(int numindices) {
	}

}
