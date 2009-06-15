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
import javabullet.BulletGlobals;
import javabullet.BulletPool;
import javabullet.ObjectPool;
import javabullet.collision.broadphase.BroadphaseNativeType;
import javabullet.collision.narrowphase.TriangleConvexcastCallback;
import javabullet.collision.narrowphase.TriangleRaycastCallback;
import javabullet.linearmath.VectorUtil;
import javax.vecmath.Vector3f;

/**
 * Bvh Concave triangle mesh is a static-triangle mesh shape with Bounding Volume Hierarchy optimization.
 * Uses an interface to access the triangles to allow for sharing graphics/physics triangles.
 * 
 * @author jezek2
 */
public class BvhTriangleMeshShape extends TriangleMeshShape {

	private OptimizedBvh bvh;
	private boolean useQuantizedAabbCompression;
	private boolean ownsBvh;
	
	private ObjectPool<MyNodeOverlapCallback> myNodeCallbacks = BulletPool.get(MyNodeOverlapCallback.class);
	
	public BvhTriangleMeshShape() {
		super(null);
		this.bvh = null;
		this.ownsBvh = false;
	}

	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression) {
		this(meshInterface, useQuantizedAabbCompression, true);
	}
	
	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, boolean buildBvh) {
		super(meshInterface);
		this.bvh = null;
		this.useQuantizedAabbCompression = useQuantizedAabbCompression;
		this.ownsBvh = false;

		// construct bvh from meshInterface
		//#ifndef DISABLE_BVH

		Vector3f bvhAabbMin = new Vector3f(), bvhAabbMax = new Vector3f();
		meshInterface.calculateAabbBruteForce(bvhAabbMin, bvhAabbMax);

		if (buildBvh) {
			bvh = new OptimizedBvh();
			bvh.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax);
			ownsBvh = true;
		}

		// JAVA NOTE: moved from TriangleMeshShape
		recalcLocalAabb();
		//#endif //DISABLE_BVH
	}

	/**
	 * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
	 */
	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, Vector3f bvhAabbMin, Vector3f bvhAabbMax) {
		this(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax, true);
	}
	
	/**
	 * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
	 */
	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, Vector3f bvhAabbMin, Vector3f bvhAabbMax, boolean buildBvh) {
		super(meshInterface);

		this.bvh = null;
		this.useQuantizedAabbCompression = useQuantizedAabbCompression;
		this.ownsBvh = false;

		// construct bvh from meshInterface
		//#ifndef DISABLE_BVH

		if (buildBvh) {
			bvh = new OptimizedBvh();

			bvh.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax);
			ownsBvh = true;
		}

		// JAVA NOTE: moved from TriangleMeshShape
		recalcLocalAabb();
		//#endif //DISABLE_BVH
	}
	
	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE;
	}

	public void performRaycast(TriangleRaycastCallback callback, Vector3f raySource, Vector3f rayTarget) {
		MyNodeOverlapCallback myNodeCallback = myNodeCallbacks.get();
		myNodeCallback.init(callback, meshInterface);

		bvh.reportRayOverlappingNodex(myNodeCallback, raySource, rayTarget);
		
		myNodeCallbacks.release(myNodeCallback);
	}
	
	public void performConvexcast(TriangleConvexcastCallback callback, Vector3f raySource, Vector3f rayTarget, Vector3f aabbMin, Vector3f aabbMax) {
		MyNodeOverlapCallback myNodeCallback = myNodeCallbacks.get();
		myNodeCallback.init(callback, meshInterface);

		bvh.reportBoxCastOverlappingNodex(myNodeCallback, raySource, rayTarget, aabbMin, aabbMax);

		myNodeCallbacks.release(myNodeCallback);
	}

	/**
	 * Perform bvh tree traversal and report overlapping triangles to 'callback'.
	 */
	@Override
	public void processAllTriangles(TriangleCallback callback, Vector3f aabbMin, Vector3f aabbMax) {
		//#ifdef DISABLE_BVH
		// // brute force traverse all triangles
		//btTriangleMeshShape::processAllTriangles(callback,aabbMin,aabbMax);
		//#else

		// first get all the nodes
		MyNodeOverlapCallback myNodeCallback = myNodeCallbacks.get();
		myNodeCallback.init(callback, meshInterface);

		bvh.reportAabbOverlappingNodex(myNodeCallback, aabbMin, aabbMax);

		myNodeCallbacks.release(myNodeCallback);
		//#endif//DISABLE_BVH
	}
	
	public void refitTree() {
		bvh.refit(meshInterface);

		recalcLocalAabb();
	}

	/**
	 * For a fast incremental refit of parts of the tree. Note: the entire AABB of the tree will become more conservative, it never shrinks.
	 */
	public void partialRefitTree(Vector3f aabbMin, Vector3f aabbMax) {
		bvh.refitPartial(meshInterface,aabbMin,aabbMax );

		VectorUtil.setMin(localAabbMin, aabbMin);
		VectorUtil.setMax(localAabbMax, aabbMax);
	}

	@Override
	public String getName() {
		return "BVHTRIANGLEMESH";
	}
	
	@Override
	public void setLocalScaling(Vector3f scaling) {
		stack.vectors.push();
		try {
			Vector3f tmp = stack.vectors.get();
			tmp.sub(getLocalScaling(), scaling);

			if (tmp.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
				super.setLocalScaling(scaling);
				/*
				if (ownsBvh)
				{
				m_bvh->~btOptimizedBvh();
				btAlignedFree(m_bvh);
				}
				*/
				///m_localAabbMin/m_localAabbMax is already re-calculated in btTriangleMeshShape. We could just scale aabb, but this needs some more work
				bvh = new OptimizedBvh();
				// rebuild the bvh...
				bvh.build(meshInterface, useQuantizedAabbCompression, localAabbMin, localAabbMax);

			}
		}
		finally {
			stack.vectors.pop();
		}
	}
	
	public OptimizedBvh getOptimizedBvh() {
		return bvh;
	}

	public void setOptimizedBvh(OptimizedBvh bvh) {
		assert (this.bvh == null);
		assert (!ownsBvh);

		this.bvh = bvh;
		ownsBvh = false;
	}

	public boolean usesQuantizedAabbCompression() {
		return useQuantizedAabbCompression;
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	protected static class MyNodeOverlapCallback implements NodeOverlapCallback {
		public StridingMeshInterface meshInterface;
		public TriangleCallback callback;

		private Vector3f[] triangle/*[3]*/ = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };
		private VertexData data = new VertexData();

		public MyNodeOverlapCallback() {
		}
		
		public void init(TriangleCallback callback, StridingMeshInterface meshInterface) {
			this.meshInterface = meshInterface;
			this.callback = callback;
		}

		public void processNode(int nodeSubPart, int nodeTriangleIndex) {
			meshInterface.getLockedReadOnlyVertexIndexBase(data, nodeSubPart);

			//int* gfxbase = (int*)(indexbase+nodeTriangleIndex*indexstride);
			ByteBuffer gfxbase_ptr = data.indexbase;
			int gfxbase_index = (nodeTriangleIndex * data.indexstride);
			assert (data.indicestype == ScalarType.PHY_INTEGER || data.indicestype == ScalarType.PHY_SHORT);

			Vector3f meshScaling = meshInterface.getScaling();
			for (int j = 2; j >= 0; j--) {
				int graphicsindex;
				if (data.indicestype == ScalarType.PHY_SHORT) {
					graphicsindex = gfxbase_ptr.getShort(gfxbase_index + j * 2) & 0xFFFF;
				}
				else {
					graphicsindex = gfxbase_ptr.getInt(gfxbase_index + j * 4);
				}

				//float* graphicsbase = (float*)(vertexbase+graphicsindex*stride);
				ByteBuffer graphicsbase_ptr = data.vertexbase;
				int graphicsbase_index = graphicsindex * data.stride;

				triangle[j].set(
						graphicsbase_ptr.getFloat(graphicsbase_index + 4 * 0) * meshScaling.x,
						graphicsbase_ptr.getFloat(graphicsbase_index + 4 * 1) * meshScaling.y,
						graphicsbase_ptr.getFloat(graphicsbase_index + 4 * 2) * meshScaling.z);
			}

			/* Perform ray vs. triangle collision here */
			callback.processTriangle(triangle, nodeSubPart, nodeTriangleIndex);
			meshInterface.unLockReadOnlyVertexBase(nodeSubPart);

			data.unref();
		}
	}
	
}
