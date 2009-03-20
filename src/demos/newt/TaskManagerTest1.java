package demos.newt;

import javax.media.nativewindow.*;
import com.sun.javafx.newt.*;
import demos.util.TaskManager;

public class TaskManagerTest1  implements WindowListener, KeyListener
{
    final static TaskManager eventMgr;
    final static TaskManager renderMgr;

    static
    {
        System.setProperty("java.awt.headless", "true");

        eventMgr = new TaskManager("Event Manager");
        eventMgr.start();

        renderMgr = new TaskManager("Render Manager");
        renderMgr.start();
    }

    public static void main(String[] args)
    {
        new TaskManagerTest1().run();
    }

    Window window;

    public void windowResized(WindowEvent e) {}
    public void windowMoved(WindowEvent e) {}
    public void windowDestroyNotify(WindowEvent e) {
        System.err.println("Window Event Listener DestroyNotify send stop request - START");
        renderMgr.stop();
        eventMgr.stop();
        System.err.println("Window Event Listener DestroyNotify send stop request - DONE");
    }

    public void keyPressed(KeyEvent e)
    {
        System.err.println("keyPressed "+e);
    }
    public void keyReleased(KeyEvent e)
    {
        System.err.println("keyReleased "+e);
    }
    public void keyTyped(KeyEvent e)
    {
        System.err.println("keyTyped "+e);
    }

    void render(long context)
    {

    }

    private class EventThread implements Runnable {
        public void run() {
            try {
                // prolog - lock whatever you need

                // do it ..
                if(null!=window) {
                    window.pumpMessages();
                }
            } catch (Throwable t) {
                // handle errors ..
                t.printStackTrace();
            } finally {
                // epilog - unlock locked stuff
            }
        }
    }

    private class RenderThread implements Runnable {
        public void run() {
            if(null==window) {
                return;
            }
            try {
                // prolog - lock whatever you need
                window.lockSurface();

                // render(window.getSurfaceHandle());
                System.out.print(".");
                Thread.sleep(100);
            } catch (Throwable t) {
                // handle errors ..
                t.printStackTrace();
            } finally {
                // epilog - unlock locked stuff
                window.unlockSurface();
            }
        }
    }

    void run()
    {
        try
        {

            Capabilities caps = new Capabilities();
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            //caps.setBackgroundOpaque(true);

            Display display = NewtFactory.createDisplay(null);
            Screen screen = NewtFactory.createScreen(display, 0);
            window = NewtFactory.createWindow(screen, caps);
            window.setTitle("GlassPrism");
            window.setAutoDrawableClient(true);
            window.setUndecorated(false);
            window.setSize(256, 256);
            window.addKeyListener(this);

            // let's get notified if window is closed
            window.addWindowListener(this); 

            window.setVisible(true);

            eventMgr.addTask(new EventThread());
            renderMgr.addTask(new RenderThread());

            System.out.println("Main - wait until finished");
            renderMgr.waitUntilStopped();
            eventMgr.waitUntilStopped();
            System.out.println("Main - finished");

            window.destroy();
            System.out.println("Main - window destroyed");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}

