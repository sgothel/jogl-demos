package demos.jgears;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.games.jogl.*;
import demos.gears.Gears;

/**
 * JGears.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Goethel) <P>
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21
 */

public class JGears {
  public static void main(String[] args) {
    JFrame frame = new JFrame("Gear Demo");
    frame.getContentPane().setLayout(new BorderLayout());
    GLCapabilities caps = new GLCapabilities();
    caps.setAlphaBits(8);
    final GLJPanel drawable = GLDrawableFactory.getFactory().createGLJPanel(caps);
    drawable.setOpaque(false);
    drawable.addGLEventListener(new Gears());

    JPanel gradientPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          ((Graphics2D) g).setPaint(new GradientPaint(0, 0, Color.WHITE,
                                                      getWidth(), getHeight(), Color.DARK_GRAY));
          g.fillRect(0, 0, getWidth(), getHeight());
        }
      };
    gradientPanel.setLayout(new BorderLayout());
    frame.getContentPane().add(gradientPanel, BorderLayout.CENTER);
    gradientPanel.add(drawable, BorderLayout.CENTER);

    final JCheckBox checkBox = new JCheckBox("Transparent", true);
    checkBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawable.setOpaque(!checkBox.isSelected());
        }
      });
    frame.getContentPane().add(checkBox, BorderLayout.SOUTH);

    frame.setSize(300, 300);
    final Animator animator = new Animator(drawable);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          // Run this on another thread than the AWT event queue to
          // make sure the call to Animator.stop() completes before
          // exiting
          new Thread(new Runnable() {
              public void run() {
                animator.stop();
                System.exit(0);
              }
            }).start();
        }
      });
    frame.show();
    animator.start();
  }
}
