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

import java.util.Comparator;
import java.util.List;
import javabullet.util.FloatArrayList;
import javabullet.util.IntArrayList;

/**
 *
 * @author jezek2
 */
public class MiscUtil {

	public static int getListCapacityForHash(List<?> list) {
		return getListCapacityForHash(list.size());
	}
	
	public static int getListCapacityForHash(int size) {
		int n = 2;
		while (n < size) {
			n <<= 1;
		}
		return n;
	}

	public static <T> void ensureIndex(List<T> list, int index, T value) {
		while (list.size() <= index) {
			list.add(value);
		}
	}
	
	public static void resize(IntArrayList list, int size, int value) {
		while (list.size() < size) {
			list.add(value);
		}
		
		while (list.size() > size) {
			list.remove(list.size() - 1);
		}
	}
	
	public static void resize(FloatArrayList list, int size, float value) {
		while (list.size() < size) {
			list.add(value);
		}
		
		while (list.size() > size) {
			list.remove(list.size() - 1);
		}
	}

	public static <T> void resize(List<T> list, int size, Class<T> valueCls) {
		try {
			while (list.size() < size) {
				list.add(valueCls != null? valueCls.newInstance() : null);
			}

			while (list.size() > size) {
				list.remove(list.size() - 1);
			}
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		catch (InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static <T> int indexOf(T[] array, T obj) {
		for (int i=0; i<array.length; i++) {
			if (array[i] == obj) return i;
		}
		return -1;
	}
	
	public static float GEN_clamped(float a, float lb, float ub) {
		return a < lb ? lb : (ub < a ? ub : a);
	}

	/**
	 * Heap sort from http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Sort/Heap/
	 */
	private static <T> void downHeap(List<T> pArr, int k, int n, Comparator<T> comparator) {
		/*  PRE: a[k+1..N] is a heap */
		/* POST:  a[k..N]  is a heap */

		T temp = pArr.get(k - 1);
		/* k has child(s) */
		while (k <= n / 2) {
			int child = 2 * k;

			if ((child < n) && comparator.compare(pArr.get(child - 1), pArr.get(child)) < 0) {
				child++;
			}
			/* pick larger child */
			if (comparator.compare(temp, pArr.get(child - 1)) < 0) {
				/* move child up */
				pArr.set(k - 1, pArr.get(child - 1));
				k = child;
			}
			else {
				break;
			}
		}
		pArr.set(k - 1, temp);
	}

	public static <T> void heapSort(List<T> list, Comparator<T> comparator) {
		/* sort a[0..N-1],  N.B. 0 to N-1 */
		int k;
		int n = list.size();
		for (k = n / 2; k > 0; k--) {
			downHeap(list, k, n, comparator);
		}

		/* a[1..N] is now a heap */
		while (n >= 1) {
			swap(list, 0, n - 1); /* largest of a[0..n-1] */

			n = n - 1;
			/* restore a[1..i-1] heap */
			downHeap(list, 1, n, comparator);
		}
	}

	private static <T> void swap(List<T> list, int index0, int index1) {
		T temp = list.get(index0);
		list.set(index0, list.get(index1));
		list.set(index1, temp);
	}
	
}
