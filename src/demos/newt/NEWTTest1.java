package demos.newt;

import javax.media.nativewindow.*;
import com.sun.javafx.newt.*;

public class NEWTTest1 implements WindowListener, KeyListener
{
    static
    {
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String[] args)
    {
        new NEWTTest1().run();
    }

    public void windowResized(WindowEvent e) {}
    public void windowMoved(WindowEvent e) {}
    public void windowDestroyNotify(WindowEvent e) {
        // stop running ..
        running = false;
    }
    boolean running = true;

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

            while (running)
            {
                window.pumpMessages();

                window.lockSurface();
                try
                {
                    render(window.getSurfaceHandle());
                }
                finally
                {
                    window.unlockSurface();
                }

                Thread.yield();
                // not necessary Thread.sleep(40);
            }

            window.destroy();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}

