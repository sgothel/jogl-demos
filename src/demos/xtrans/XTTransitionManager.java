package demos.xtrans;

import java.awt.*;
import java.awt.geom.*;

/** Specifies how the XTDesktopPane creates new transitions. */

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
