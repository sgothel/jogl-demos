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
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.*;
import demos.common.*;
import demos.hdr.HDR;
import demos.hwShadowmapsSimple.HWShadowmapsSimple;
import demos.infiniteShadowVolumes.InfiniteShadowVolumes;
import demos.j2d.FlyingText;
import demos.jgears.JGears;
import demos.proceduralTexturePhysics.ProceduralTexturePhysics;
import demos.util.*;
import demos.vertexBufferObject.VertexBufferObject;
import demos.vertexProgRefract.VertexProgRefract;
import demos.vertexProgWarp.VertexProgWarp;

import demos.xtrans.*;

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

  private Animator animator;
  private JDesktopPane desktop;

  public static void main(String[] args) {
    new JRefract().run(args);
  }

  private static final int GEARS     = 1;
  private static final int HDR       = 2;
  private static final int HWSHADOWS = 3;
  private static final int INFINITE  = 4;
  private static final int REFRACT   = 5;
  private static final int TEXT      = 6;
  private static final int VBO       = 7;
  private static final int WARP      = 8;
  private static final int WATER     = 9;

  private JInternalFrame addWindow(int which) {
    // FIXME: workaround for problem in 1.6 where ALL Components,
    // including Swing components, are Finalizable, requiring two full
    // GC cycles (and running of finalizers) to reclaim
    System.gc();
    // Try to get finalizers run
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
    }
    System.gc();

    String str = "";
    switch (which) {
    case GEARS:     str = "Gears Demo"; break;
    case HDR:       str = "High Dynamic Range Rendering Demo"; break;
    case HWSHADOWS: str = "ARB_shadow Shadows"; break;
    case INFINITE:  str = "Infinite Shadow Volumes"; break;
    case REFRACT:   str = "Refraction Using Vertex Programs"; break;
    case TEXT:      str = "Flying Text"; break;
    case VBO:       str = "Very Simple vertex_buffer_object demo"; break;
    case WATER:     str = "Procedural Texture Waves"; break;
    }
    final JInternalFrame inner = new JInternalFrame(str);
    inner.setResizable(true);
    inner.setClosable(true);
    inner.setVisible(true);

    GLCapabilities caps = new GLCapabilities();
    if (which == INFINITE) {
      caps.setStencilBits(16);
    }
    final GLJPanel canvas =
      (which == GEARS) ?
      new JGears() :
      new GLJPanel(caps);
    final DemoListener demoListener = new DemoListener() {
        public void shutdownDemo() {
          removeJPanel(canvas);
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                inner.doDefaultCloseAction();
              }
            });
        }

        public void repaint() {
          canvas.repaint();
        }
      };

    Demo demo = null;
    switch (which) {
      case GEARS: {
        // GLEventListener already added
        break;
      }

      case HDR: {
        demo = new HDR();
        ((HDR) demo).setup(null);
        inner.setSize(((HDR) demo).getPreferredWidth(), ((HDR) demo).getPreferredHeight());
        break;
      }

      case HWSHADOWS: {
        demo = new HWShadowmapsSimple();
        break;
      }

      case INFINITE: {
        demo = new InfiniteShadowVolumes();
        break;
      }

      case REFRACT: {
        demo = new VertexProgRefract();
        break;
      }

      case TEXT: {
        demo = new FlyingText();
        break;
      }

      case VBO: {
        demo = new VertexBufferObject();
        break;
      }

      case WARP: {
        demo = new VertexProgWarp();
        ((VertexProgWarp) demo).setTitleSetter(new VertexProgWarp.TitleSetter() {
            public void setTitle(final String title) {
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    inner.setTitle(title);
                  }
                });
            }
          });
        break;
      }

      case WATER: {
        demo = new ProceduralTexturePhysics();
        break;
      }
    }
    if (which != GEARS) {
      demo.setDemoListener(demoListener);
      canvas.addGLEventListener(demo);
    }
    canvas.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          canvas.requestFocus();
        }
      });

    addJPanel(canvas);

    final Demo fDemo = demo;

    inner.addInternalFrameListener(new InternalFrameAdapter() {
        public void internalFrameClosed(InternalFrameEvent e) {
          if (fDemo != null) {
            fDemo.shutdownDemo();
          }
        }
      });

    inner.getContentPane().setLayout(new BorderLayout());
    /*    if (which == REFRACT) {
      // Testing scrolling
      canvas.setSize(512, 512);
      canvas.setPreferredSize(new Dimension(512, 512));
      JScrollPane scroller = new JScrollPane(canvas);
      inner.getContentPane().add(scroller);
      } else */ if (which == GEARS) {
      // Provide control over transparency of gears background
      canvas.setOpaque(false);
      JPanel gradientPanel = JGears.createGradientPanel();
      inner.getContentPane().add(gradientPanel, BorderLayout.CENTER);
      gradientPanel.add(canvas, BorderLayout.CENTER);

      final JCheckBox checkBox = new JCheckBox("Transparent", true);
      checkBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            canvas.setOpaque(!checkBox.isSelected());
          }
        });
      inner.getContentPane().add(checkBox, BorderLayout.SOUTH);
    } else if (which == TEXT) {
      FlyingText text = (FlyingText) demo;
      inner.getContentPane().add(text.buildGUI(), BorderLayout.NORTH);
      inner.getContentPane().add(canvas, BorderLayout.CENTER);
    } else {
      inner.getContentPane().add(canvas, BorderLayout.CENTER);
    }

    if (which != HDR) {
      inner.setSize(512, 512);
    }
    desktop.add(inner);

    return inner;
  }

  public void run(String[] args) {
    JFrame frame = new JFrame("JOGL and Swing Interoperability");
    if ((args.length > 0) && args[0].equals("-xt")) {
      desktop = new XTDesktopPane();
      // FIXME: this is a hack to get the repaint behavior to work correctly
      ((XTDesktopPane) desktop).setAlwaysRedraw(true);
    } else {
      desktop = new JDesktopPane();
    }

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
    JMenuItem item;

    item = new JMenuItem("Gears");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(GEARS);
        }
      });
    menu.add(item);

    item = new JMenuItem("High Dynamic Range");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(HDR);
        }
      });
    menu.add(item);

    item = new JMenuItem("Hardware Shadow Maps");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(HWSHADOWS);
        }
      });
    menu.add(item);

    item = new JMenuItem("Infinite Shadow Volumes");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(INFINITE);
        }
      });
    menu.add(item);

    item = new JMenuItem("Refraction");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(REFRACT);
        }
      });
    menu.add(item);

    item = new JMenuItem("Text");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(TEXT);
        }
      });
    menu.add(item);

    item = new JMenuItem("Vertex Buffer Object");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(VBO);
        }
      });
    menu.add(item);

    item = new JMenuItem("Warp");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(WARP);
        }
      });
    menu.add(item);

    item = new JMenuItem("Water");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(WATER);
        }
      });
    menu.add(item);

    item = new JMenuItem("Loop Gears Demo");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          startAutoMode();
        }
      });
    menu.add(item);

    item = new JMenuItem("Quit");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          runExit();
        }
      });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
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

    animator = new FPSAnimator(60);
    animator.start();
  }

  private void runExit() {
    // Note: calling System.exit() synchronously inside the draw,
    // reshape or init callbacks can lead to deadlocks on certain
    // platforms (in particular, X11) because the JAWT's locking
    // routines cause a global AWT lock to be grabbed. Instead run
    // the exit routine in another thread.
    new Thread(new Runnable() {
        public void run() {
          animator.stop();
          System.exit(0);
        }
      }).start();
  }

  private synchronized void addJPanel(GLJPanel panel) {
    animator.add(panel);
  }

  private synchronized void removeJPanel(GLJPanel panel) {
    animator.remove(panel);
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
