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

package javabullet.util;

import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Wrapper for THashMap (from GNU Trove), with fallback to less effective original HashMap.
 * 
 * @author jezek2
 */
public class HashUtil {
	
	private HashUtil() {}
	
	public interface IMap<K,V> {
		public V get(K key);
		public V put(K key, V value);
		public V remove(K key);
		public int size();
		public boolean forEachValue(IObjectProcedure<V> proc);
		public boolean retainEntries(IObjectProcedure<V> proc);
	}
	
	public interface IObjectProcedure<T> {
		public boolean execute(T value);
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private static Class mapCls;
	
	static {
		try {
			mapCls = TroveHashMapImpl.class;
		}
		catch (Throwable t) {
			mapCls = JavaHashMapImpl.class;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <K,V> IMap<K,V> createMap() {
		try {
			return (IMap<K,V>)mapCls.newInstance();
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		catch (InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	protected static class JavaHashMapImpl<K,V> extends HashMap<K,V> implements IMap<K,V> {
		public boolean forEachValue(IObjectProcedure<V> proc) {
			for (V value : values()) {
				if (!proc.execute(value)) return false;
			}
			return true;
		}

		public boolean retainEntries(IObjectProcedure<V> proc) {
			boolean mod = false;
			for (Iterator<V> it = values().iterator(); it.hasNext(); ) {
				V value = it.next();
				if (!proc.execute(value)) {
					it.remove();
					mod = true;
				}
			}
			return mod;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	protected static class TroveHashMapImpl<K,V> extends THashMap<K,V> implements IMap<K,V> {
		private TroveObjectObjectProcedureWrapper<K,V> valueWrapper = new TroveObjectObjectProcedureWrapper<K,V>();
		
		public boolean forEachValue(IObjectProcedure<V> proc) {
			valueWrapper.proc = proc;
			return forEachEntry(valueWrapper);
		}

		public boolean retainEntries(IObjectProcedure<V> proc) {
			valueWrapper.proc = proc;
			return retainEntries(valueWrapper);
		}
	}
	
	protected static class TroveObjectObjectProcedureWrapper<K,V> implements TObjectObjectProcedure<K,V> {
		public IObjectProcedure<V> proc;
		
		public boolean execute(K key, V value) {
			return proc.execute(value);
		}
	}
	
}
