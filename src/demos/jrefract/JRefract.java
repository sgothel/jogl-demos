/*
 * Portions Copyright (C) 2005 Sun Microsystems, Inc.
 * All rights reserved.
 */

/*
 *
 * COPYRIGHT NVIDIA CORPORATION 2003. ALL RIGHTS RESERVED.
 * BY ACCESSING OR USING THIS SOFTWARE, YOU AGREE TO:
 *
 *  1) ACKNOWLEDGE NVIDIA'S EXCLUSIVE OWNERSHIP OF ALL RIGHTS
 *     IN AND TO THE SOFTWARE;
 *
 *  2) NOT MAKE OR DISTRIBUTE COPIES OF THE SOFTWARE WITHOUT
 *     INCLUDING THIS NOTICE AND AGREEMENT;
 *
 *  3) ACKNOWLEDGE THAT TO THE MAXIMUM EXTENT PERMITTED BY
 *     APPLICABLE LAW, THIS SOFTWARE IS PROVIDED *AS IS* AND
 *     THAT NVIDIA AND ITS SUPPLIERS DISCLAIM ALL WARRANTIES,
 *     EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED
 *     TO, IMPLIED WARRANTIES OF MERCHANTABILITY  AND FITNESS
 *     FOR A PARTICULAR PURPOSE.
 *
 * IN NO EVENT SHALL NVIDIA OR ITS SUPPLIERS BE LIABLE FOR ANY
 * SPECIAL, INCIDENTAL, INDIRECT, OR CONSEQUENTIAL DAMAGES
 * WHATSOEVER (INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS
 * OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS
 * INFORMATION, OR ANY OTHER PECUNIARY LOSS), INCLUDING ATTORNEYS'
 * FEES, RELATING TO THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package demos.jrefract;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.games.jogl.*;
import net.java.games.jogl.util.*;
import demos.gears.Gears;
import demos.proceduralTexturePhysics.ProceduralTexturePhysics;
import demos.util.*;
import demos.vertexProgRefract.VertexProgRefract;

/**
  Wavelength-dependent refraction demo<br>
  It's a chromatic aberration!<br>
  sgreen@nvidia.com 4/2001<br><p>

  Currently 3 passes - could do it in 1 with 4 texture units<p>

  Cubemap courtesy of Paul Debevec<p>

  Ported to Java, Swing and ARB_fragment_program by Kenneth Russell
*/

public class JRefract {
  private boolean useRegisterCombiners;
  private ArrayList canvases;

  private volatile boolean quit;
  private volatile boolean animatorStopped;
  private JDesktopPane desktop;

  public static void main(String[] args) {
    new JRefract().run(args);
  }

  private static final int GEARS = 1;
  private static final int BUNNY = 2;
  private static final int WATER = 3;

  private JInternalFrame addWindow(int which) {
    String str = null;
    switch (which) {
    case GEARS: str = "Gears Demo"; break;
    case BUNNY: str = "Refraction Using Vertex Programs"; break;
    case WATER: str = "Procedural Texture Waves"; break;
    default: throw new IllegalArgumentException("Invalid demo " + which);
    }
    final JInternalFrame inner = new JInternalFrame(str);
    inner.setResizable(true);
    inner.setClosable(true);
    inner.setVisible(true);

    GLCapabilities caps = new GLCapabilities();
    if (which == GEARS) {
      caps.setAlphaBits(8);
    }
    final GLJPanel canvas = GLDrawableFactory.getFactory().createGLJPanel(caps);
    final DemoListener demoListener = new DemoListener() {
        public void shutdownDemo() {
          removeJPanel(canvas);
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                inner.doDefaultCloseAction();
              }
            });
        }
      };

    switch (which) {
      case GEARS: {
        canvas.addGLEventListener(new Gears());
        break;
      }
      case BUNNY: {
        VertexProgRefract demo = new VertexProgRefract();
        demo.setDemoListener(demoListener);
        canvas.addGLEventListener(demo);
        break;
      }
      case WATER: {
        ProceduralTexturePhysics demo = new ProceduralTexturePhysics();
        demo.setDemoListener(demoListener);
        canvas.addGLEventListener(demo);
        break;
      }
    }
    canvas.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          canvas.requestFocus();
        }
      });

    addJPanel(canvas);

    inner.addInternalFrameListener(new InternalFrameAdapter() {
        public void internalFrameClosed(InternalFrameEvent e) {
          removeJPanel(canvas);
          System.gc();
        }
      });

    inner.getContentPane().setLayout(new BorderLayout());
    if (which == BUNNY) {
      inner.getContentPane().add(canvas, BorderLayout.CENTER);
      inner.getContentPane().add(new JButton("West"), BorderLayout.WEST);
      inner.getContentPane().add(new JButton("East"), BorderLayout.EAST);
      inner.getContentPane().add(new JButton("North"), BorderLayout.NORTH);
      inner.getContentPane().add(new JButton("South"), BorderLayout.SOUTH);
    } else if (which == GEARS) {
      // Provide control over transparency of gears background
      canvas.setOpaque(false);
      JPanel gradientPanel = new JPanel() {
          public void paintComponent(Graphics g) {
            ((Graphics2D) g).setPaint(new GradientPaint(0, 0, Color.WHITE,
                                                        getWidth(), getHeight(), Color.DARK_GRAY));
            g.fillRect(0, 0, getWidth(), getHeight());
          }
        };
      gradientPanel.setLayout(new BorderLayout());
      inner.getContentPane().add(gradientPanel, BorderLayout.CENTER);
      gradientPanel.add(canvas, BorderLayout.CENTER);

      final JCheckBox checkBox = new JCheckBox("Transparent", true);
      checkBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            canvas.setOpaque(!checkBox.isSelected());
          }
        });
      inner.getContentPane().add(checkBox, BorderLayout.SOUTH);
    } else {
      inner.getContentPane().add(canvas, BorderLayout.CENTER);
    }

    inner.setSize(512, 512);
    desktop.add(inner);

    return inner;
  }

  public void run(String[] args) {

    canvases = new ArrayList();

    JFrame frame = new JFrame("JOGL and Swing Interoperability");
    desktop = new JDesktopPane();
    desktop.setSize(1024, 768);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(desktop, BorderLayout.CENTER);

    JInternalFrame inner2 = new JInternalFrame("Hello, World");
    JLabel label = new JLabel("Hello, World!");
    label.setFont(new Font("SansSerif", Font.PLAIN, 128));
    inner2.getContentPane().add(label);
    inner2.pack();
    inner2.setResizable(true);
    desktop.add(inner2);
    inner2.setVisible(true);

    JMenuBar menuBar = new JMenuBar();

    JMenu menu = new JMenu("Actions");
    JMenuItem item = new JMenuItem("New bunny");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(BUNNY);
        }
      });
    menu.add(item);

    item = new JMenuItem("New gears");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(GEARS);
        }
      });
    menu.add(item);

    item = new JMenuItem("New water");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(WATER);
        }
      });
    menu.add(item);

    item = new JMenuItem("Auto mode");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          startAutoMode();
        }
      });
    menu.add(item);

    item = new JMenuItem("Exit");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          runExit();
        }
      });
    menu.add(item);

    menuBar.add(menu);
    frame.setJMenuBar(menuBar);

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          runExit();
        }
      });
    frame.setSize(desktop.getSize());
    frame.setVisible(true);

    new Thread(new ListAnimator()).start();
  }

  private void runExit() {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Instead run
    // the exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          quit = true;
          while (!animatorStopped) {
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
            }
          }
          System.exit(0);
        }
      }).start();
  }

  private synchronized void addJPanel(GLJPanel panel) {
    ArrayList newCanvases = (ArrayList) canvases.clone();
    newCanvases.add(panel);
    canvases = newCanvases;
  }

  private synchronized void removeJPanel(GLJPanel panel) {
    ArrayList newCanvases = (ArrayList) canvases.clone();
    newCanvases.remove(panel);
    canvases = newCanvases;
  }

  class ListAnimator implements Runnable {
    public void run() {
      while (!quit) {
        if (canvases.isEmpty()) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
          }
        } else {
          for (Iterator iter = canvases.iterator(); iter.hasNext(); ) {
            GLJPanel panel = (GLJPanel) iter.next();
            panel.display();
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
          }
        }
      }
      animatorStopped = true;
    }
  }

  private JInternalFrame curFrame;
  private void startAutoMode() {
    new Thread(new Runnable() {
        public void run() {
          while (true) {
            try {
              SwingUtilities.invokeAndWait(new Runnable() {
                  public void run() {
                    curFrame = addWindow(GEARS);
                  }
                });
            } catch (Exception e) {
              e.printStackTrace();
            }

            try {
              Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            try {
              SwingUtilities.invokeAndWait(new Runnable() {
                  public void run() {
                    curFrame.doDefaultCloseAction();
                  }
                });
            } catch (Exception e) {
              e.printStackTrace();
            }

            try {
              Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
          }
        }
      }).start();
  }
}
