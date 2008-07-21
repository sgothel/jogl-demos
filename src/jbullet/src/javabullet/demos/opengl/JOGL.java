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

package javabullet.demos.opengl;

import com.sun.javafx.newt.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.opengl.util.*;
import java.nio.*;

/**
 *
 * @author jezek2
 */

public class JOGL implements MouseListener {
	
    private GLWindow window;
    public boolean quit = false;

    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount()>1) {
            quit=true;
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

	private static boolean redisplay = false;
	
	public static void postRedisplay() {
		redisplay = true;
	}

    private void run(String title, DemoApplication demoApp, int type) {
        int width = 480;
        int height = 800;
        System.err.println(title+"run()");
        GLProfile.setProfileGL2ES1();
        try {
            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NewtFactory.AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NewtFactory.AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NewtFactory.AWT, nScreen, 0); // dummy VisualID
            }

            GLCapabilities caps = new GLCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);
            window = GLWindow.create(nWindow, caps);

            window.addMouseListener(this);
            window.addMouseListener(demoApp);
            window.addKeyListener(demoApp);
            window.addGLEventListener(demoApp);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);
            width = window.getWidth();
            height = window.getHeight();

            long startTime = System.currentTimeMillis();
            long lastTime = startTime, curTime = 0, dt0, dt1;
            int totalFrames = 0, lastFrames = 0;

            while (!quit) {
                window.display();

                totalFrames++; lastFrames++;
                curTime = System.currentTimeMillis();
                dt0 = curTime-lastTime;
                if ( (curTime-lastTime) > 5000 ) {
                    dt1 = curTime-startTime;
                    StringBuffer sb = new StringBuffer();
                    sb.append(dt1/1000);
                    sb.append("s, 5s: ");
                    sb.append((lastFrames*1000)/dt0);
                    sb.append(" fps, total: ");
                    sb.append((totalFrames*1000)/dt1);
                    sb.append(" fps");
                    System.out.println(sb);
                    lastTime=curTime;
                    lastFrames=0;
                }
            }

            // Shut things down cooperatively
            window.close();
            window.getFactory().shutdown();
            System.out.println(title+" shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String title, DemoApplication demo, String[] args) {
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
        }
        new JOGL().run(title, demo, type);
        System.exit(0);
    }

}
