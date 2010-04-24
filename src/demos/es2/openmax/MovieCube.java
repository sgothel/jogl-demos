/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package demos.es2.openmax;

// import demos.es1.cube.Cube;

import javax.media.opengl.*;
import com.jogamp.opengl.util.glsl.fixedfunc.*;

import com.jogamp.openmax.*;

import java.net.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

public class MovieCube implements MouseListener, GLEventListener, OMXEventListener {
    GLWindow window;
    boolean quit = false;
    Cube cube=null;
    String stream;
    OMXInstance movie=null;

    public void changedAttributes(OMXInstance omx, int event_mask) {
        System.out.println("changed stream attr ("+event_mask+"): "+omx);
    }

    public void mouseClicked(MouseEvent e) {
        switch(e.getClickCount()) {
            case 2:
                quit=true;
                break;
        }
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseMoved(MouseEvent e) {
    }
    public void mouseDragged(MouseEvent e) {
    }
    public void mouseWheelMoved(MouseEvent e) {
    }

    public MovieCube (String stream) {
        cube = new Cube(true, false);
        this.stream = stream;
    }

    private void run() {
        System.err.println("MovieCube.run()");
        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            window = GLWindow.create(caps);

            window.addMouseListener(this);
            window.addGLEventListener(this);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            // Size OpenGL to Video Surface
            window.setFullscreen(true);
            window.setVisible(true);

            while (!quit) {
                window.display();
            }

            // Shut things down cooperatively
            if(null!=movie) {
                movie.destroy(window.getGL());
                movie=null;
            }
            window.destroy();
            System.out.println("MovieCube shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES1 gl = FixedFuncUtil.getFixedFuncImpl(drawable.getGL());
        System.out.println(gl);

        gl.glGetError(); // flush error ..

        gl.glActiveTexture(GL.GL_TEXTURE0);

        try {
            movie = new OMXInstance();
            movie.addEventListener(this);
            movie.setStream(4, new URL(stream));
            System.out.println("p0 "+movie);
        } catch (MalformedURLException mue) { mue.printStackTrace(); }
        if(null!=movie) {
            movie.setStreamAllEGLImageTexture2D(gl);
            movie.activateStream();
            System.out.println("p1 "+movie);
            movie.play();
        }

        cube.init(drawable);

        /*
        if(gl.isGLES2()) {
            GLES2 gles2 = gl.getGLES2();

            // Debug ..
            //DebugGLES2 gldbg = new DebugGLES2(gles2);
            //gles2.getContext().setGL(gldbg);
            //gles2 = gldbg;

            // Trace ..
            //TraceGLES2 gltrace = new TraceGLES2(gles2, System.err);
            gles2.getContext().setGL(gltrace);
            gl = gltrace;
        }*/

    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        cube.reshape(drawable, x, y, width, height);

        System.out.println("reshape "+width+"x"+height);
    }

    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        movie.destroy(gl);
        movie=null;
        cube.dispose(drawable);
        cube=null;
        quit=true;
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES1 gl = drawable.getGL().getGL2ES1();

        com.jogamp.opengl.util.texture.Texture tex = null;
        if(null!=movie) {
            tex=movie.getNextTextureID();
            if(null!=tex) {
                System.out.println("Use: "+tex);
                tex.enable();
                tex.bind();
            }
        }
        cube.display(drawable);
        if(null!=tex) {
            tex.disable();
        }
    }

    public void displayChanged(javax.media.opengl.GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    public static void main(String[] args) {
        String fname="file:///Storage Card/resources/a.mp4";
        if(args.length>0) fname=args[0];
        new MovieCube(fname).run();
        System.exit(0);
    }
}

