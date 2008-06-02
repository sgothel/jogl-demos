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

import java.nio.*;
import javax.media.opengl.*;
import com.sun.javafx.newt.*;

public class Main implements MouseListener {

    private boolean quit = false;
    private boolean toggleFS = false;
    private Window window;

    private boolean dragging;
    private int lastDragX;
    private int lastDragY;
    private float motionIncr;
    private float xRot, yRot;

    public void mouseClicked(MouseEvent e) {
        switch(e.getClickCount()) {
            case 1:
                toggleFS=true;
                break;
            default: 
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
        dragging = false;
    }
    public void mouseMoved(MouseEvent e) {
    }
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            dragging = true;
            lastDragX = e.getX();
            lastDragY = e.getY();
        } else {
            yRot += (e.getX() - lastDragX) * motionIncr;
            xRot += (e.getY() - lastDragY) * motionIncr;
            lastDragX = e.getX();
            lastDragY = e.getY();
        }
    }

    public void run() {
        System.out.println("CubeFBO Main");
        try {
            Display display = NewtFactory.createDisplay(null); // local display
            Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
            window = NewtFactory.createWindow(screen, 0); // dummy VisualID

            window.addMouseListener(this);

            // Size OpenGL to Video Surface
            int width = 800;
            int height = 480;
            window.setSize(width, height);
            window.setFullscreen(true);

            // Hook this into EGL
            GLDrawableFactory factory = GLDrawableFactory.getFactory(GLDrawableFactory.PROFILE_GLES1, window);
            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            GLDrawable drawable = factory.createGLDrawable(window, caps, null);
            window.setVisible(true);
            drawable.setRealized(true);
            GLContext context = drawable.createContext(null);
            context.makeCurrent();

            GL gl = context.getGL();

            motionIncr = 180.f / Math.max(window.getWidth(), window.getHeight());
            FBCubes cubes = new FBCubes();
            cubes.init(gl);

            long startTime = System.currentTimeMillis();
            long lastTime = startTime, curTime = 0, dt0, dt1;
            int totalFrames = 0, lastFrames = 0;

            do {
                cubes.reshape(gl, 0, 0, window.getWidth(), window.getHeight());
                cubes.display(gl, xRot, yRot);
                drawable.swapBuffers();
                totalFrames++; lastFrames++;
                curTime = System.currentTimeMillis();
                dt0 = curTime-lastTime;
                if ( (curTime-lastTime) > 5000 ) {
                    dt1 = curTime-startTime;
                    System.out.println(dt1/1000+"s, 5s: "+ (lastFrames*1000)/dt0 + " fps, "+
                                                 "total: "+ (totalFrames*1000)/dt1 + " fps");
                    lastTime=curTime;
                    lastFrames=0;
                }
                if(toggleFS) {
                    window.setFullscreen(!window.isFullscreen());
                    toggleFS=false;
                }

                window.pumpMessages();

                //                Thread.yield();

                //                try{
                //                    Thread.sleep(10);
                //                } catch(InterruptedException ie) {}
            } while (!quit && (curTime - startTime) < 215000);

            // Shut things down cooperatively
            context.release();
            context.destroy();
            drawable.destroy();
            factory.shutdown();
            System.out.println("CubeFBO shut down cleanly.");
        } catch (GLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new Main().run();
        System.exit(0);
    }
}
