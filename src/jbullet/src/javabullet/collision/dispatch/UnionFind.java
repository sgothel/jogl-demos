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

package javabullet.collision.dispatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javabullet.linearmath.MiscUtil;

/**
 * UnionFind calculates connected subsets.
 * Implements weighted Quick Union with path compression.
 * Optimization: could use short ints instead of ints (halving memory, would limit the number of rigid bodies to 64k, sounds reasonable).
 * 
 * @author jezek2
 */
public class UnionFind {

	private final List<Element> elements = new ArrayList<Element>();
	
	/**
	 * This is a special operation, destroying the content of UnionFind.
	 * It sorts the elements, based on island id, in order to make it easy to iterate over islands.
	 */
	public void sortIslands() {
		// first store the original body index, and islandId
		int numElements = elements.size();

		for (int i = 0; i < numElements; i++) {
			elements.get(i).id = find(i);
			elements.get(i).sz = i;
		}

		// Sort the vector using predicate and std::sort
		//std::sort(m_elements.begin(), m_elements.end(), btUnionFindElementSortPredicate);
		//perhaps use radix sort?
		//elements.heapSort(btUnionFindElementSortPredicate());
		
		//Collections.sort(elements);
		MiscUtil.heapSort(elements, elementComparator);
	}

	public void reset(int N) {
		allocate(N);

		for (int i = 0; i < N; i++) {
			elements.get(i).id = i;
			elements.get(i).sz = 1;
		}
	}

	public int getNumElements() {
		return elements.size();
	}

	public boolean isRoot(int x) {
		return (x == elements.get(x).id);
	}

	public Element getElement(int index) {
		return elements.get(index);
	}

	public void allocate(int N) {
		MiscUtil.resize(elements, N, Element.class);
	}

	public void free() {
		elements.clear();
	}

	public int find(int p, int q) {
		return (find(p) == find(q))? 1 : 0;
	}

	public void unite(int p, int q) {
		int i = find(p), j = find(q);
		if (i == j) {
			return;
		}

		//#ifndef USE_PATH_COMPRESSION
		////weighted quick union, this keeps the 'trees' balanced, and keeps performance of unite O( log(n) )
		//if (m_elements[i].m_sz < m_elements[j].m_sz)
		//{ 
		//	m_elements[i].m_id = j; m_elements[j].m_sz += m_elements[i].m_sz; 
		//}
		//else 
		//{ 
		//	m_elements[j].m_id = i; m_elements[i].m_sz += m_elements[j].m_sz; 
		//}
		//#else
		elements.get(i).id = j;
		elements.get(j).sz += elements.get(i).sz;
		//#endif //USE_PATH_COMPRESSION
	}

	public int find(int x) {
		//assert(x < m_N);
		//assert(x >= 0);

		while (x != elements.get(x).id) {
			// not really a reason not to use path compression, and it flattens the trees/improves find performance dramatically

			//#ifdef USE_PATH_COMPRESSION
			elements.get(x).id = elements.get(elements.get(x).id).id;
			//#endif //
			x = elements.get(x).id;
			//assert(x < m_N);
			//assert(x >= 0);
		}
		return x;
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	public static class Element {
		public int id;
		public int sz;
	}
	
	private static final Comparator<Element> elementComparator = new Comparator<Element>() {
		public int compare(Element o1, Element o2) {
			return o1.id < o2.id? -1 : +1;
		}
	};
	
}
