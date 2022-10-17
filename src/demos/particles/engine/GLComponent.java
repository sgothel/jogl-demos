/*
 * Copyright (c) 2006 Ben Chappell. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * The names of Ben Chappell, Sun Microsystems, Inc. or the names of
 * contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. BEN CHAPPELL,
 * SUN MICROSYSTEMS, INC. ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL BEN
 * CHAPPELL, SUN, OR SUN'S LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF BEN
 * CHAPPELL OR SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package demos.particles.engine;

import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.awt.*;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.FPSAnimator;

public class GLComponent extends GLCanvas implements GLEventListener {

    private final GLU glu;
    private FPSAnimator animator;
    private final RGBA background;
    private final RGBA ambient;
    private Engine engine;

    public GLComponent(final int fps, final RGBA ambient, final RGBA background, final Engine engine) {
        super(getCapabilities());
        addGLEventListener(this);
        glu = new GLU();

        this.background=background;
        this.ambient=ambient;
        this.engine=engine;

        animator = new FPSAnimator(this, fps);
    }

    private static GLCapabilities getCapabilities() {
        final GLCapabilities caps = new GLCapabilities(null);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);
        return caps;
    }

    public void dispose(final GLAutoDrawable drawable) {
        this.engine=null;
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        engine.draw(gl);
    }



    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {

    }

    public void init(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        // Set the background / clear color.
        gl.glClearColor(background.r, background.g, background.b, background.a);
        // Clear the depth
        gl.glClearDepth(1.0);
        // Disable depth testing.
        gl.glDisable(GL.GL_DEPTH_TEST);
        // Enable blending and specify blening function.
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        // Get nice perspective calculations.
        gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        // Nice point smoothing.
        gl.glHint(GL2ES1.GL_POINT_SMOOTH_HINT, GL.GL_NICEST);
        // Enable texture mapping.
        gl.glEnable(GL.GL_TEXTURE_2D);

        animator.start();

        engine.init(gl);

    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2 gl = drawable.getGL().getGL2();
        // the size of openGL
        gl.glViewport(0,0, width, height);

        // perspective view (smaller for further behind)
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();

        // perspective
        final double ratio = (double)width/(double)height;
        // angle, ratio, nearest, farthest
        glu.gluPerspective(45.0, ratio, FloatUtil.EPSILON,  1.0);

        // draw into the model matrix now
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void setFPS(final int fps) {
        animator.stop();
        animator = new FPSAnimator(this, fps);
        animator.start();
    }

    public void kill() {
        animator.stop();
    }
}
