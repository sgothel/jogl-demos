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

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.nativewindow.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.*;
import java.nio.*;

/**
 *
 * @author jezek2
 */

public class JOGL implements WindowListener, MouseListener {
	
    private GLWindow window;
    public boolean quit = false;

    public void windowResized(WindowEvent e) { }
    public void windowMoved(WindowEvent e) { }
    public void windowDestroyNotify(WindowEvent e) {
        quit = true;
    }

    public void mouseClicked(MouseEvent e) {
        //if(e.getClickCount()>1) {
            quit=true;
        //}
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

    private void run(String title, DemoApplication demoApp, int type, int width, int height) {
        System.err.print(title);
        System.err.println(" run()");
        GLProfile.setProfileGLAny();
        try {
            NWCapabilities caps = new NWCapabilities();
            // For emulation library, use 16 bpp
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setDepthBits(16);

            Window nWindow = null;
            if(0!=(type&USE_AWT)) {
                Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null); // local display
                Screen nScreen  = NewtFactory.createScreen(NativeWindowFactory.TYPE_AWT, nDisplay, 0); // screen 0
                nWindow = NewtFactory.createWindow(NativeWindowFactory.TYPE_AWT, nScreen, caps, false);
                window = GLWindow.create(nWindow);
            } else {
                window = GLWindow.create(caps);
            }

            window.addWindowListener(this);
            window.addMouseListener(this);
            window.addMouseListener(demoApp);
            window.addKeyListener(demoApp);
            window.addGLEventListener(demoApp);
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_CURRENT); // default
            // window.setEventHandlerMode(GLWindow.EVENT_HANDLER_GL_NONE); // no current ..

            window.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);
            // Size OpenGL to Video Surface
            window.setSize(width, height);
            window.setFullscreen(true);
            window.setVisible(true);
            width = window.getWidth();
            height = window.getHeight();

            while (!quit && window.getTotalFPSDuration() < 200000) {
                window.display();
            }

            // Shut things down cooperatively
            window.destroy();
            System.out.print(title);
            System.out.println(" shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static int USE_NEWT      = 0;
    public static int USE_AWT       = 1 << 0;

    public static void main(String title, DemoApplication demo, String[] args) {
        int width = 480;
        int height = 800;
        int type = USE_NEWT ;
        for(int i=args.length-1; i>=0; i--) {
            if(args[i].equals("-awt")) {
                type |= USE_AWT; 
            }
            if(args[i].equals("-width") && i+1<args.length) {
                try {
                    width = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException nfe) {}
            }
            if(args[i].equals("-height") && i+1<args.length) {
                try {
                    height = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException nfe) {}
            }
        }
        new JOGL().run(title, demo, type, width, height);
        System.exit(0);
    }

}
