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

package demos.es1.cubefbo;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import java.nio.*;

class FBObject {
    private int fb, fbo_tex, depth_rb, stencil_rb, width, height;

    public FBObject(int width, int height) {
        this.width = width;
        this.height = height;
    }        

    public void init(GL gl) {
        // generate fbo ..
        int name[] = new int[1];

        gl.glGenTextures(1, name, 0);
        fbo_tex = name[0];
        System.out.println("fbo_tex: "+fbo_tex);

        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8_OES, width, height, 0,
                        GL.GL_RGB, GL.GL_UNSIGNED_BYTE, null);

        gl.glGenRenderbuffersOES(1, name, 0);
        depth_rb = name[0];
        System.out.println("depth_rb: "+depth_rb);

        // Initialize the depth buffer:
        gl.glBindRenderbufferOES(GL.GL_RENDERBUFFER_OES, depth_rb);
        gl.glRenderbufferStorageOES(GL.GL_RENDERBUFFER_OES,
                                    GL.GL_DEPTH_COMPONENT16_OES, width, height);

        // gl.glGenRenderbuffersOES(1, name, 0);
        // stencil_rb = name[0];
        stencil_rb = 0;
        System.out.println("stencil_rb: "+stencil_rb);

        gl.glGenFramebuffersOES(1, name, 0);
        fb = name[0];
        System.out.println("fb: "+fb);

        // bind fbo ..
        gl.glBindFramebufferOES(GL.GL_FRAMEBUFFER_OES, fb);

        // Set up the color buffer for use as a renderable texture:
        gl.glFramebufferTexture2DOES(GL.GL_FRAMEBUFFER_OES,
                                  GL.GL_COLOR_ATTACHMENT0_OES,
                                  GL.GL_TEXTURE_2D, fbo_tex, 0); 

        // Set up the depth buffer attachment:
        gl.glFramebufferRenderbufferOES(GL.GL_FRAMEBUFFER_OES,
                                        GL.GL_DEPTH_ATTACHMENT_OES,
                                        GL.GL_RENDERBUFFER_OES, depth_rb);

        // bind fbo ..
        gl.glBindFramebufferOES(GL.GL_FRAMEBUFFER_OES, fb);

        // Setup the color buffer for use as a renderable texture:
        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);

        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8_OES, width, height, 0,
                     GL.GL_RGB, GL.GL_UNSIGNED_BYTE, null);

        gl.glFramebufferTexture2DOES(GL.GL_FRAMEBUFFER_OES,
                                  GL.GL_COLOR_ATTACHMENT0_OES,
                                  GL.GL_TEXTURE_2D, fbo_tex, 0); 

        // Initialize the depth buffer:
        gl.glBindRenderbufferOES(GL.GL_RENDERBUFFER_OES, depth_rb);

        gl.glRenderbufferStorageOES(GL.GL_RENDERBUFFER_OES,
                                    GL.GL_DEPTH_COMPONENT16_OES, width, height);

        gl.glFramebufferRenderbufferOES(GL.GL_FRAMEBUFFER_OES,
                                        GL.GL_DEPTH_ATTACHMENT_OES,
                                        GL.GL_RENDERBUFFER_OES, depth_rb);

        if(stencil_rb!=0) {
            // Initialize the stencil buffer:
            gl.glBindRenderbufferOES(GL.GL_RENDERBUFFER_OES, stencil_rb);

            gl.glRenderbufferStorageOES(GL.GL_RENDERBUFFER_OES,
                                        GL.GL_STENCIL_INDEX8_OES, width, height);

            gl.glFramebufferRenderbufferOES(GL.GL_FRAMEBUFFER_OES,
                                            GL.GL_STENCIL_ATTACHMENT_OES,
                                            GL.GL_RENDERBUFFER_OES, stencil_rb);
        }

        // Check the FBO for completeness
        int res = gl.glCheckFramebufferStatusOES(fb);
        if (res == GL.GL_FRAMEBUFFER_COMPLETE_OES) {
            System.out.println("Framebuffer " + fb + " is complete");
        } else {
            System.out.println("Framebuffer " + fb + " is incomplete: status = 0x" + Integer.toHexString(res));
        }
    }

    public void bind(GL gl) {
        gl.glBindFramebufferOES(GL.GL_FRAMEBUFFER_OES, fb); 
    }

    public void unbind(GL gl) {
        gl.glBindFramebufferOES(GL.GL_FRAMEBUFFER_OES, 0);
    }

    public int getFBName() {
        return fb;
    }
    public int getTextureName() {
        return fbo_tex;
    }
}
