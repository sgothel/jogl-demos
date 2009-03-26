package demos.newt;

import javax.media.nativewindow.*;
import com.sun.javafx.newt.*;
import demos.newt.util.TaskToolWM;

public class TaskManagerTest2  implements WindowListener, KeyListener
{
    public static void main(String[] args)
    {
        new TaskManagerTest2().run();
    }

    public void windowResized(WindowEvent e) {}
    public void windowMoved(WindowEvent e) {}
    public void windowDestroyNotify(WindowEvent e) {
        System.err.println("Window Event Listener DestroyNotify send stop request - START");
        TaskToolWM.unregisterWindowEvent(e.getSource());
        System.err.println("Window Event Listener DestroyNotify send stop request - DONE");
    }

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyChar()=='q') {
            System.err.println("Key Event Listener 'q' - ..");
            TaskToolWM.unregisterWindowEvent(e.getSource());
        } else {
            System.err.println("keyPressed "+e);
        }
    }
    public void keyReleased(KeyEvent e)
    {
        System.err.println("keyReleased "+e);
    }
    public void keyTyped(KeyEvent e)
    {
        System.err.println("keyTyped "+e);
    }

    private class RenderThread implements Runnable {
        Window window;

        public RenderThread(Window w) {
            window = w;
        }
        public void run() {
            if(null==window) {
                return;
            }
            try {
                // prolog - lock whatever you need
                window.lockSurface();

                // render(window.getSurfaceHandle());
                System.out.println("Render: "+window);
                Thread.sleep(200);
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
            Window window = NewtFactory.createWindow(screen, caps);
            window.setTitle("GlassPrism");
            window.setAutoDrawableClient(true);
            window.setUndecorated(false);
            window.setSize(256, 256);
            window.addKeyListener(this);

            // let's get notified if window is closed
            window.addWindowListener(this); 

            window.setVisible(true);

            TaskToolWM.registerWindowEvent(window);
            TaskToolWM.addRenderTask(window, new RenderThread(window));

            System.out.println("Main - wait until finished");
            TaskToolWM.waitUntilWindowUnregistered(window);
            System.out.println("Main - finished");

            window.destroy();
            System.out.println("Main - window destroyed");
            TaskToolWM.exit(true);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}

