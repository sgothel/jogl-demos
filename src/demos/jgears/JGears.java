package demos.jgears;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import javax.imageio.*;
import javax.swing.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import demos.gears.Gears;

/**
 * JGears.java <BR>
 * author: Brian Paul (converted to Java by Ron Cemer and Sven Goethel) <P>
 *
 * This version is equal to Brian Paul's version 1.2 1999/10/21
 */

public class JGears extends GLJPanel {
  private static GLCapabilities caps;
  private long startTime;
  private int frameCount;
  private float fps;
  private static Font fpsFont = new Font("SansSerif", Font.BOLD, 24);
  private DecimalFormat format = new DecimalFormat("####.00");
  private BufferedImage javaImage;
  private BufferedImage openglImage;

  static {
    caps = new GLCapabilities();
    caps.setAlphaBits(8);
  }
  
  public JGears() {
    super(caps, null, null);
    addGLEventListener(new Gears());
    try {
      InputStream in = JGears.class.getClassLoader().getResourceAsStream("demos/data/images/java_logo.png");
      BufferedImage image = ImageIO.read(in);
      javaImage = scaleImage(image, 0.25f, 0.25f);

      in = JGears.class.getClassLoader().getResourceAsStream("demos/data/images/opengl_logo.png");
      image = ImageIO.read(in);
      openglImage = scaleImage(image, 0.45f, 0.45f);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (startTime == 0) {
      startTime = System.currentTimeMillis();
    }

    if (++frameCount == 30) {
      long endTime = System.currentTimeMillis();
      fps = 30.0f / (float) (endTime - startTime) * 1000;
      frameCount = 0;
      startTime = System.currentTimeMillis();
    }

    if (fps > 0) {
      g.setColor(Color.WHITE);
      g.setFont(fpsFont);
      g.drawString("FPS: " + format.format(fps), getWidth() - 140, getHeight() - 30);
    }

    int sp = 10;
    if (javaImage != null) {
      g.drawImage(javaImage, sp, getHeight() - javaImage.getHeight() - sp, null);
      if (openglImage != null) {
        g.drawImage(openglImage, sp + javaImage.getWidth() + sp, getHeight() - openglImage.getHeight() - sp, null);
      }
    }
  }

  // Helper routine for various demos
  public static JPanel createGradientPanel() {
    JPanel gradientPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          ((Graphics2D) g).setPaint(new GradientPaint(0, 0, Color.WHITE,
                                                      getWidth(), getHeight(), Color.DARK_GRAY));
          g.fillRect(0, 0, getWidth(), getHeight());
        }
      };
    gradientPanel.setLayout(new BorderLayout());
    return gradientPanel;
  }

  private BufferedImage scaleImage(BufferedImage img, float xScale, float yScale) {
    BufferedImage scaled = new BufferedImage((int) (img.getWidth() * xScale),
                                             (int) (img.getHeight() * yScale),
                                             BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = scaled.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawRenderedImage(img, AffineTransform.getScaleInstance(xScale, yScale));
    return scaled;
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("Gear Demo");
    frame.getContentPane().setLayout(new BorderLayout());
    final GLJPanel drawable = new JGears();
    drawable.setOpaque(false);

    JPanel gradientPanel = createGradientPanel();
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
