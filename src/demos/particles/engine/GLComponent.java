/*
 * Copyright (c) 2006 Ben Chappell (bwchappell@gmail.com) All Rights Reserved.
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

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;

import java.io.*;

public class GLComponent extends GLCanvas implements GLEventListener {
    
    private GLU glu;
    private FPSAnimator animator;
    private RGBA background;
    private RGBA ambient;
    private Engine engine;
    
    public GLComponent(int fps, RGBA ambient, RGBA background, Engine engine) {
        super(getCapabilities());
        addGLEventListener(this);
        glu = new GLU();
        
        this.background=background;
        this.ambient=ambient;             
        this.engine=engine;
        
        animator = new FPSAnimator(this, fps);
    }
    
    private static GLCapabilities getCapabilities() {
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);
        return caps;
    }
    
    public void display(GLAutoDrawable drawable) {
        final GL gl = drawable.getGL(); 
        engine.draw(gl);
    }
    
    
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        
    }
    
    public void init(GLAutoDrawable drawable) {
        final GL gl = drawable.getGL();        

        gl.glShadeModel(GL.GL_SMOOTH);
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
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        // Nice point smoothing.
        gl.glHint(GL.GL_POINT_SMOOTH_HINT, GL.GL_NICEST);
        // Enable texture mapping.
        gl.glEnable(GL.GL_TEXTURE_2D);
        
        animator.start();
        
        engine.init();
        
    }    
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL gl = drawable.getGL();
        // the size of openGL
        gl.glViewport(0,0, width, height);
        
        // perspective view (smaller for further behind)
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        
        // perspective
        double ratio = (double)width/(double)height;
        // angle, ratio, nearest, farthest
        glu.gluPerspective(45.0, ratio, 0.0,  1.0);
        
        // draw into the model matrix now
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
    
    public void setFPS(int fps) {
        animator.stop();
        animator = new FPSAnimator(this, fps);
        animator.start();
    }
    
    public void kill() {
        animator.stop();
    }
}
