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
import java.util.*;
import gleem.linalg.*;

/** A basic transition manager supporting animated scrolling, rotating
 * and fading of components.
 *
 * @author Kenneth Russell
 */

public class XTBasicTransitionManager implements XTTransitionManager {
  /** Indicates the style of the transition (either no motion,
      scrolling, or rotating). */
  public static class Style {
    private Style() {}
  }

  /** Indicates the component has no motion (scrolling or rotation) in
      its animation. */
  public static Style STYLE_NO_MOTION = new Style();

  /** Indicates the component is to be scrolled in to or out of
      place. */
  public static Style STYLE_SCROLL    = new Style();

  /** Indicates the component is to be rotated in to or out of
      place. */
  public static Style STYLE_ROTATE    = new Style();

  /** Indicates the direction of the transition if it contains any
      motion (either up, down, left, or right). */
  public static class Direction {
    private Direction() {}
  }

  /** Indicates the component's animation is from or toward the left,
      depending on whether the transition is an "in" or "out"
      transition. */
  public static Direction DIR_LEFT  = new Direction();

  /** Indicates the component's animation is from or toward the right,
      depending on whether the transition is an "in" or "out"
      transition. */
  public static Direction DIR_RIGHT = new Direction();

  /** Indicates the component's animation is in the upward
      direction. */
  public static Direction DIR_UP    = new Direction();

  /** Indicates the component's animation is in the downward
      direction. */
  public static Direction DIR_DOWN  = new Direction();

  private Style     nextTransitionStyle;
  private Direction nextTransitionDirection;
  private boolean   nextTransitionFade;

  private Random random;

  /** Sets the next transition to be used by this transition manager
      for either an "in" or an "out" transition. By default the
      transition manager selects random transitions from those
      available. */
  public void setNextTransition(Style style,
                                Direction direction,
                                boolean fade) {
    if (style == null) {
      throw new IllegalArgumentException("Must supply a style");
    }
    nextTransitionStyle     = style;
    nextTransitionDirection = direction;
    nextTransitionFade      = fade;
  }

  /** Creates an XTBasicTransition for the given component. By default
      this transition manager chooses a random transition from those
      available if one is not specified via {@link #setNextTransition
      setNextTransition}. */
  public XTTransition createTransitionForComponent(Component c,
                                                   boolean isAddition,
                                                   Rectangle   oglViewportOfDesktop,
                                                   Point       viewportOffsetFromOrigin,
                                                   Rectangle2D oglTexCoordsOnBackBuffer) {
    if (nextTransitionStyle == null) {
      chooseRandomTransition();
    }

    // Figure out the final positions of everything
    // Keep in mind that the Java2D origin is at the upper left and
    // the OpenGL origin is at the lower left
    Rectangle bounds = c.getBounds();
    int x = bounds.x;
    int y = bounds.y;
    int w = bounds.width;
    int h = bounds.height;
    float tx = (float) oglTexCoordsOnBackBuffer.getX();
    float ty = (float) oglTexCoordsOnBackBuffer.getY();
    float tw = (float) oglTexCoordsOnBackBuffer.getWidth();
    float th = (float) oglTexCoordsOnBackBuffer.getHeight();
    float vx = oglViewportOfDesktop.x;
    float vy = oglViewportOfDesktop.y;
    float vw = oglViewportOfDesktop.width;
    float vh = oglViewportOfDesktop.height;
    Quad3f verts = new Quad3f(new Vec3f(0,  0, 0),
                              new Vec3f(0, -h, 0),
                              new Vec3f(w, -h, 0),
                              new Vec3f(w,  0, 0));
    Quad2f texcoords = new Quad2f(new Vec2f(tx,      ty + th),
                                  new Vec2f(tx,      ty),
                                  new Vec2f(tx + tw, ty),
                                  new Vec2f(tx + tw, ty + th));

    XTBasicTransition trans = new XTBasicTransition();

    Vec3f translation = new Vec3f(x - viewportOffsetFromOrigin.x,
                                  vh - y - viewportOffsetFromOrigin.y,
                                  0);
    InterpolatedVec3f transInterp = new InterpolatedVec3f();
    transInterp.setStart(translation);
    transInterp.setEnd(translation);

    InterpolatedQuad3f quadInterp = new InterpolatedQuad3f();
    quadInterp.setStart(verts);
    quadInterp.setEnd(verts);

    InterpolatedQuad2f texInterp = new InterpolatedQuad2f();
    texInterp.setStart(texcoords);
    texInterp.setEnd(texcoords);

    trans.setTranslation(transInterp);
    trans.setVertices(quadInterp);
    trans.setTexCoords(texInterp);

    // Now decide how we are going to handle this transition
    Style     transitionStyle     = nextTransitionStyle;
    Direction transitionDirection = nextTransitionDirection;
    boolean   fade                = nextTransitionFade;
    nextTransitionStyle = null;
    nextTransitionDirection = null;
    nextTransitionFade = false;

    int[] vtIdxs = null;
    int[] ttIdxs = null;
    Vec3f rotAxis = null;
    Vec3f pivot = null;
    float startAngle = 0;
    float endAngle = 0;

    if (fade) {
      InterpolatedFloat alpha = new InterpolatedFloat();
      float start = (isAddition ? 0.0f : 1.0f);
      float end   = (isAddition ? 1.0f : 0.0f);
      alpha.setStart(start);
      alpha.setEnd(end);
      trans.setAlpha(alpha);
    }

    if (transitionDirection != null) {
      if (transitionStyle == STYLE_SCROLL) {
        if (transitionDirection == DIR_LEFT) {
          vtIdxs = new int[] { 3, 2, 2, 3 };
          ttIdxs = new int[] { 0, 1, 1, 0 };
        } else if (transitionDirection == DIR_RIGHT) {
          vtIdxs = new int[] { 0, 1, 1, 0 };
          ttIdxs = new int[] { 3, 2, 2, 3 };
        } else if (transitionDirection == DIR_UP) {
          vtIdxs = new int[] { 1, 1, 2, 2 };
          ttIdxs = new int[] { 0, 0, 3, 3 };
        } else {
          // DIR_DOWN
          vtIdxs = new int[] { 0, 0, 3, 3 };
          ttIdxs = new int[] { 1, 1, 2, 2 };
        }
      } else if (transitionStyle == STYLE_ROTATE) {
        if (transitionDirection == DIR_LEFT) {
          rotAxis = new Vec3f(0, 1, 0);
          pivot = new Vec3f();
          startAngle = -90;
          endAngle = 0;
        } else if (transitionDirection == DIR_RIGHT) {
          rotAxis = new Vec3f(0, 1, 0);
          pivot = new Vec3f(w, 0, 0);
          startAngle = 90;
          endAngle = 0;
        } else if (transitionDirection == DIR_UP) {
          rotAxis = new Vec3f(1, 0, 0);
          pivot = new Vec3f(0, -h, 0);
          startAngle = 90;
          endAngle = 0;
        } else {
          // DIR_DOWN
          rotAxis = new Vec3f(1, 0, 0);
          pivot = new Vec3f();
          startAngle = -90;
          endAngle = 0;
        }
      }
    }


    /*
    switch (transitionType) {
      case FADE:
      {
        InterpolatedFloat alpha = new InterpolatedFloat();
        float start = (isAddition ? 0.0f : 1.0f);
        float end   = (isAddition ? 1.0f : 0.0f);
        alpha.setStart(start);
        alpha.setEnd(end);
        trans.setAlpha(alpha);
        break;
      }
      case SCROLL_LEFT:
      {
        vtIdxs = new int[] { 3, 2, 2, 3 };
        ttIdxs = new int[] { 0, 1, 1, 0 };
        break;
      }
      case SCROLL_RIGHT:
      {
        vtIdxs = new int[] { 0, 1, 1, 0 };
        ttIdxs = new int[] { 3, 2, 2, 3 };
        break;
      }
      case SCROLL_UP:
      {
        vtIdxs = new int[] { 1, 1, 2, 2 };
        ttIdxs = new int[] { 0, 0, 3, 3 };
        break;
      }
      case SCROLL_DOWN:
      {
        vtIdxs = new int[] { 0, 0, 3, 3 };
        ttIdxs = new int[] { 1, 1, 2, 2 };
        break;
      }
      case ROTATE_LEFT:
      {
        rotAxis = new Vec3f(0, 1, 0);
        pivot = new Vec3f();
        startAngle = -90;
        endAngle = 0;
        break;
      }
      case ROTATE_RIGHT:
      {
        rotAxis = new Vec3f(0, 1, 0);
        //        pivot = translation.plus(new Vec3f(w, 0, 0));
        pivot = new Vec3f(w, 0, 0);
        startAngle = 90;
        endAngle = 0;
        break;
      }
      case ROTATE_UP:
      {
        rotAxis = new Vec3f(1, 0, 0);
        //        pivot = translation.plus(new Vec3f(0, -h, 0));
        pivot = new Vec3f(0, -h, 0);
        startAngle = 90;
        endAngle = 0;
        break;
      }
      case ROTATE_DOWN:
      {
        rotAxis = new Vec3f(1, 0, 0);
        pivot = new Vec3f();
        startAngle = -90;
        endAngle = 0;
        break;
      }
    }

    */

    if (vtIdxs != null) {
      if (isAddition) {
        quadInterp.setStart(new Quad3f(verts.getVec(vtIdxs[0]),
                                       verts.getVec(vtIdxs[1]),
                                       verts.getVec(vtIdxs[2]),
                                       verts.getVec(vtIdxs[3])));
        texInterp.setStart(new Quad2f(texcoords.getVec(ttIdxs[0]),
                                      texcoords.getVec(ttIdxs[1]),
                                      texcoords.getVec(ttIdxs[2]),
                                      texcoords.getVec(ttIdxs[3])));
      } else {
        // Note: swapping the vertex and texture indices happens to
        // have the correct effect
        int[] tmp = vtIdxs;
        vtIdxs = ttIdxs;
        ttIdxs = tmp;

        quadInterp.setEnd(new Quad3f(verts.getVec(vtIdxs[0]),
                                     verts.getVec(vtIdxs[1]),
                                     verts.getVec(vtIdxs[2]),
                                     verts.getVec(vtIdxs[3])));
        texInterp.setEnd(new Quad2f(texcoords.getVec(ttIdxs[0]),
                                    texcoords.getVec(ttIdxs[1]),
                                    texcoords.getVec(ttIdxs[2]),
                                    texcoords.getVec(ttIdxs[3])));
      }
    } else if (rotAxis != null) {
      if (!isAddition) {
        float tmp = endAngle;
        endAngle = -startAngle;
        startAngle = tmp;
      }

      trans.setPivotPoint(pivot);
      trans.setRotationAxis(rotAxis);
      InterpolatedFloat rotInterp = new InterpolatedFloat();
      rotInterp.setStart(startAngle);
      rotInterp.setEnd(endAngle);
      trans.setRotationAngle(rotInterp);
    }

    return trans;
  }

  /** Chooses a random transition from those available. */
  protected void chooseRandomTransition() {
    if (random == null) {
      random = new Random();
    }
    nextTransitionFade = random.nextBoolean();
    nextTransitionStyle = null;
    do {
      int style = random.nextInt(3);
      switch (style) {
        // Make no-motion transitions always use fades for effect
        // without biasing transitions toward no-motion transitions
        case 0:   if (nextTransitionFade) nextTransitionStyle = STYLE_NO_MOTION; break;
        case 1:   nextTransitionStyle = STYLE_SCROLL;                            break;
        default:  nextTransitionStyle = STYLE_ROTATE;                            break;
      }
    } while (nextTransitionStyle == null);
    int dir = random.nextInt(4);
    switch (dir) {
      case 0:   nextTransitionDirection = DIR_LEFT;  break;
      case 1:   nextTransitionDirection = DIR_RIGHT; break;
      case 2:   nextTransitionDirection = DIR_UP;    break;
      default:  nextTransitionDirection = DIR_DOWN;  break;
    }
  }
}
