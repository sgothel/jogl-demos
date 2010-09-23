package demos.newt;

import javax.media.nativewindow.*;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;

public class NEWTTest1 implements WindowListener, KeyListener, MouseListener
{
    static
    {
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String[] args)
    {
        new NEWTTest1().run();
    }

    public void windowRepaint(WindowUpdateEvent e) { 
        System.err.println("windowRepaint "+e);
    }

    public void windowResized(WindowEvent e) {
        System.err.println("windowResized "+e);
    }
    public void windowMoved(WindowEvent e) {
        System.err.println("windowMoved "+e);
    }
    public void windowGainedFocus(WindowEvent e) {
        System.err.println("windowGainedFocus "+e);
    }
    public void windowLostFocus(WindowEvent e) {
        System.err.println("windowLostFocus "+e);
    }
    public void windowDestroyNotify(WindowEvent e) {
        System.err.println("windowDestroyNotify "+e);
        // stop running ..
        running = false;
    }
    boolean running = true;

    public void keyPressed(KeyEvent e) {
        System.err.println("keyPressed "+e);
    }
    public void keyReleased(KeyEvent e) {
        System.err.println("keyReleased "+e);
    }
    public void keyTyped(KeyEvent e) {
        System.err.println("keyTyped "+e);
    }
    public void mouseClicked(MouseEvent e) {
        System.err.println("mouseClicked "+e);
    }
    public void mouseEntered(MouseEvent e) {
        System.err.println("mouseEntered "+e);
    }
    public void mouseExited(MouseEvent e) {
        System.err.println("mouseExited "+e);
    }
    public void mousePressed(MouseEvent e) {
        System.err.println("mousePressed "+e);
    }
    public void mouseReleased(MouseEvent e) {
        System.err.println("mouseReleased "+e);
    }
    public void mouseMoved(MouseEvent e) {
        System.err.println("mouseMoved "+e);
    }
    public void mouseDragged(MouseEvent e) {
        System.err.println("mouseDragged "+e);
    }
    public void mouseWheelMoved(MouseEvent e) {
        System.err.println("mouseWheelMoved "+e);
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
            // window.setHandleDestroyNotify(false);
            window.setUndecorated(false);
            window.setSize(256, 256);
            window.addKeyListener(this);
            window.addMouseListener(this);

            // let's get notified if window is closed
            window.addWindowListener(this); 

            window.setVisible(true);

            while (running)
            {
                display.dispatchMessages();

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

