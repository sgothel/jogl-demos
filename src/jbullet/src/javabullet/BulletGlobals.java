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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.vecmath.Vector3f;

/**
 * Bullet's global variables and constants.
 * 
 * @author jezek2
 */
public class BulletGlobals {
	
	public static final boolean DEBUG = true;
	public static final boolean ENABLE_PROFILE = false;
	
	public static final float CONVEX_DISTANCE_MARGIN = 0.04f;
	public static final float FLT_EPSILON = 1.19209290e-07f;
	public static final float SIMD_EPSILON = FLT_EPSILON;
	
	public static final float SIMD_2_PI = 6.283185307179586232f;
	public static final float SIMD_PI = SIMD_2_PI * 0.5f;
	public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;
	public static final float SIMD_RADS_PER_DEG = SIMD_2_PI / 360f;
	public static final float SIMD_DEGS_PER_RAD = 360f / SIMD_2_PI;
	public static final float SIMD_INFINITY = Float.MAX_VALUE;

	public static ContactDestroyedCallback gContactDestroyedCallback;
	public static ContactAddedCallback gContactAddedCallback;
	public static float gContactBreakingThreshold = 0.02f;

	// RigidBody
	public static float gDeactivationTime = 2f;
	public static boolean gDisableDeactivation = false;
	
	public static int gTotalContactPoints;
	
	// GjkPairDetector
	// temp globals, to improve GJK/EPA/penetration calculations
	public static int gNumDeepPenetrationChecks = 0;
	public static int gNumGjkChecks = 0;
	
	public static int gNumAlignedAllocs;
	public static int gNumAlignedFree;
	public static int gTotalBytesAlignedAllocs;	
	
	public static int gPickingConstraintId = 0;
	public static final Vector3f gOldPickingPos = new Vector3f();
	public static float gOldPickingDist = 0.f;
	
	public static int gOverlappingPairs = 0;
	public static int gRemovePairs = 0;
	public static int gAddedPairs = 0;
	public static int gFindPairs = 0;
	
	public static final Vector3f ZERO_VECTOR3 = new Vector3f(0f, 0f, 0f);
	
	private static final List<ProfileBlock> profileStack = new ArrayList<ProfileBlock>();
	private static final Map<String,Long> profiles = new HashMap<String,Long>();

	// JAVA NOTE: added for statistics in applet demo
	public static long stepSimulationTime;
	public static long updateTime;
	
	public static void pushProfile(String name) {
		if (!ENABLE_PROFILE) return;
		
		ProfileBlock block = new ProfileBlock();
		block.name = name;
		block.startTime = System.currentTimeMillis();
		profileStack.add(block);
	}
	
	public static void popProfile() {
		if (!ENABLE_PROFILE) return;
		
		ProfileBlock block = profileStack.remove(profileStack.size() - 1);
		long time = System.currentTimeMillis();
		
		Long totalTime = profiles.get(block.name);
		if (totalTime == null) totalTime = 0L;
		totalTime += (time - block.startTime);
		profiles.put(block.name, totalTime);
	}
	
	public static void printProfiles() {
		ArrayList<Entry<String,Long>> list = new ArrayList<Entry<String,Long>>(profiles.entrySet());
		Collections.sort(list, new Comparator<Entry<String,Long>>() {
			public int compare(Entry<String,Long> e1, Entry<String,Long> e2) {
				return e1.getValue().compareTo(e2.getValue());
			}
		});
		
		for (Entry<String,Long> e : /*profiles.entrySet()*/list) {
			System.out.println(e.getKey()+" = "+e.getValue()+" ms");
		}
	}
	
	static {
		if (ENABLE_PROFILE) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					printProfiles();
				}
			});
		}
	}
	
	private static class ProfileBlock {
		public String name;
		public long startTime;
	}
	
}
