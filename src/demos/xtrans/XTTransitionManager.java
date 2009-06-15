/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package demos.xtrans;

import java.awt.*;
import java.awt.geom.*;

/** Specifies how the XTDesktopPane creates new transitions.
 *
 * @author Kenneth Russell
 */

public interface XTTransitionManager {
  /** Create a new transition for the given component.
      <ul>

      <li> The passed component's bounds indicate the location of the
      component on the desktop. The (x,y) of the bounds correspond to
      the upper left of the component; note that this differs from the
      OpenGL coordinate system.

      <li> The <code>isAddition</code> parameter indicates whether
      the component is being added to or removed from the desktop.

      <li> The <code>oglViewportOfDesktop</code> specifies the
      rectangle corresponding to the entire desktop pane in the
      default OpenGL coordinate system with the (x, y) origin of the
      rectangle at the lower left. This rectangle should be used in
      conjunction with the component's bounds and the
      <code>viewportOffsetFromOrigin</code> to determine where to draw
      the vertices for the component.

      <li> The <code>viewportOffsetFromOrigin</code> specifies the
      current OpenGL viewport's offset from the origin of the OpenGL
      coordinate system. The XTDesktopPane re-centers the OpenGL
      viewport around each component so any perspective effects appear
      symmetric. This offset should be subtracted from the translation
      of the component or its vertex locations.

      <li> The <code>oglTexCoordsOnBackBuffer</code> specifies the
      texture coordinates of the passed component on the back
      buffer. The (x,y) of this rectangle specifies the lower-left
      corner of the image corresponding to the component. </ul>
  */
  public XTTransition createTransitionForComponent(Component c,
                                                   boolean isAddition,
                                                   Rectangle   oglViewportOfDesktop,
                                                   Point       viewportOffsetFromOrigin,
                                                   Rectangle2D oglTexCoordsOnBackBuffer);
}
