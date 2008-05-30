/* San Angeles Observation OpenGL ES version example
 * Copyright 2004-2005 Jetro Lauha
 * All rights reserved.
 * Web: http://iki.fi/jetro/
 *
 * This source is free software; you can redistribute it and/or
 * modify it under the terms of EITHER:
 *   (1) The GNU Lesser General Public License as published by the Free
 *       Software Foundation; either version 2.1 of the License, or (at
 *       your option) any later version. The text of the GNU Lesser
 *       General Public License is included with this source in the
 *       file LICENSE-LGPL.txt.
 *   (2) The BSD-style license that is included with this source in
 *       the file LICENSE-BSD.txt.
 *
 * This source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files
 * LICENSE-LGPL.txt and LICENSE-BSD.txt for more details.
 *
 * $Id$
 * $Revision$
 */

package demos.es1.angeles;

// Camera track definition for one camera trucking shot.
public class CamTrack
{
    /* Length in milliseconds of one camera track base unit.
     * The value originates from the music synchronization.
     */
    static final int  CAMTRACK_LEN = 5442;

    /* Five parameters of src[5] and dest[5]:
     * eyeX, eyeY, eyeZ, viewAngle, viewHeightOffs
     */
    short src[], dest[];
    int dist;     // if >0, cam rotates around eye xy on dist * 0.1
    int len;      // length multiplier

    public CamTrack() {
        src  = new short[5];
        dest = new short[5];
    }
    public CamTrack(short s[], short d[], int dx, int l) {
        src=s;
        dest=d;
        dist=dx;
        len=l;
    }

static CamTrack sCamTracks[] =
    { new CamTrack( new short[]{ 4500, 2700, 100, 70, -30 }, new short[]{ 50, 50, -90, -100, 0 }, 20, 1 ),
      new CamTrack( new short[]{ -1448, 4294, 25, 363, 0 }, new short[]{ -136, 202, 125, -98, 100 }, 0, 1 ),
      new CamTrack( new short[]{ 1437, 4930, 200, -275, -20 }, new short[]{ 1684, 0, 0, 9, 0 }, 0, 1 ),
      new CamTrack( new short[]{ 1800, 3609, 200, 0, 675 }, new short[]{ 0, 0, 0, 300, 0 }, 0, 1 ),
      new CamTrack( new short[]{ 923, 996, 50, 2336, -80 }, new short[]{ 0, -20, -50, 0, 170 }, 0, 1 ),
      new CamTrack( new short[]{ -1663, -43, 600, 2170, 0 }, new short[]{ 20, 0, -600, 0, 100 }, 0, 1 ),
      new CamTrack( new short[]{ 1049, -1420, 175, 2111, -17 }, new short[]{ 0, 0, 0, -334, 0 }, 0, 2 ),
      new CamTrack( new short[]{ 0, 0, 50, 300, 25 }, new short[]{ 0, 0, 0, 300, 0 }, 70, 2 ),
      new CamTrack( new short[]{ -473, -953, 3500, -353, -350 }, new short[]{ 0, 0, -2800, 0, 0 }, 0, 2 ),
      new CamTrack( new short[]{ 191, 1938, 35, 1139, -17 }, new short[]{ 1205, -2909, 0, 0, 0 }, 0, 2 ),
      new CamTrack( new short[]{ -1449, -2700, 150, 0, 0 }, new short[]{ 0, 2000, 0, 0, 0 }, 0, 2 ),
      new CamTrack( new short[]{ 5273, 4992, 650, 373, -50 }, new short[]{ -4598, -3072, 0, 0, 0 }, 0, 2 ),
      new CamTrack( new short[]{ 3223, -3282, 1075, -393, -25 }, new short[]{ 1649, -1649, 0, 0, 0 }, 0, 2 ) };

static final int CAMTRACK_COUNT = 13;

}

