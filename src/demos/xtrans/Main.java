/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package demos.xtrans;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

/** Demonstration showing off XTDesktopPane.
 *
 * @author Kenneth Russell
 */

public class Main {
  private XTDesktopPane desktop;
  private XTBasicTransitionManager transManager;

  private static final int TABLE = 1;
  private static final int TREE  = 2;

  private Point loc = new Point();

  private boolean scrollingEnabled = true;
  private boolean rotationEnabled  = true;
  private boolean fadesEnabled     = true;
  private Random random;

  private void chooseNextTransition() {
    // Only choose one if the user's constraints force us to
    if (scrollingEnabled && rotationEnabled && fadesEnabled) {
      return;
    }
    if (random == null) {
      random = new Random();
    }
    boolean fade = random.nextBoolean();
    if (!fadesEnabled) {
      fade = false;
    }

    XTBasicTransitionManager.Style style = XTBasicTransitionManager.STYLE_NO_MOTION;
    if (scrollingEnabled) {
      style = XTBasicTransitionManager.STYLE_SCROLL;
    } else if (rotationEnabled) {
      style = XTBasicTransitionManager.STYLE_ROTATE;
    }
    XTBasicTransitionManager.Direction direction = null;    
    switch (random.nextInt(4)) {
      case 0:  direction = XTBasicTransitionManager.DIR_LEFT;  break;
      case 1:  direction = XTBasicTransitionManager.DIR_RIGHT; break;
      case 2:  direction = XTBasicTransitionManager.DIR_UP;    break;
      default: direction = XTBasicTransitionManager.DIR_DOWN;  break;
    }
    transManager.setNextTransition(style, direction, fade);
  }

  private void addWindow(int which) {
    JInternalFrame frame = new JInternalFrame();
    frame.setResizable(true);
    frame.setClosable(true);
    frame.setVisible(true);
    
    switch (which) {
      case TABLE:
      {
        frame.setTitle("Table Example");
        Object[][] data = produceTableData(3, 20);
        DefaultTableModel model = new DefaultTableModel(data, new String[] { "A", "B", "C" });
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.getContentPane().add(scrollPane);
        break;
      }

      case TREE:
      {
        frame.setTitle("Tree Example");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        populateTree(root, 2);
        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        frame.getContentPane().add(tree);
        break;
      }

      default:
        throw new IllegalArgumentException();
    }

    frame.setLocation(loc);
    loc = new Point((loc.x + 20) % desktop.getWidth(), (loc.y + 20) % desktop.getHeight());
    frame.addInternalFrameListener(new InternalFrameAdapter() {
        public void internalFrameClosing(InternalFrameEvent e) {
          chooseNextTransition();
        }
      });
    frame.pack();
    int sz = Math.min(desktop.getWidth() / 3, desktop.getHeight());
    frame.setSize(sz, sz);
    chooseNextTransition();
    desktop.add(frame);
    desktop.moveToFront(frame);
  }

  private Object[][] produceTableData(int cols, int rows) {
    Object[][] res = new Object[rows][];

    Random r = new Random();

    for (int i = 0; i < rows; i++) {
      Object[] row = new Object[cols];
      for (int j = 0; j < cols; j++) {
        row[j] = new Integer(r.nextInt(1000));
      }
      res[i] = row;
    }

    return res;
  }

  private void populateTree(DefaultMutableTreeNode node, int depth) {
    node.add(new DefaultMutableTreeNode("A"));
    node.add(new DefaultMutableTreeNode("B"));
    node.add(new DefaultMutableTreeNode("C"));

    if (depth > 0) {
      for (Enumeration e = node.children(); e.hasMoreElements(); ) {
        populateTree((DefaultMutableTreeNode) e.nextElement(), depth - 1);
      }
    }
  }

  private void run(String[] args) {
    JFrame frame = new JFrame("Desktop Demo");

    JMenu menu = new JMenu("Actions");
    JMenuBar menuBar = new JMenuBar();
    JMenuItem item;

    item = new JMenuItem("Add Table");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(TABLE);
        }
      });
    menu.add(item);

    item = new JMenuItem("Add Tree");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addWindow(TREE);
        }
      });
    menu.add(item);

    item = new JMenuItem("Close all");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Component[] cs = desktop.getComponents();
          for (int i = 0; i < cs.length; i++) {
            chooseNextTransition();
            desktop.remove(cs[i]);
          }
        }
      });
    menu.add(item);

    item = new JMenuItem("Quit");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.exit(0);
        }
      });
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
    menu.add(item);
    
    menuBar.add(menu);

    menu = new JMenu("Options");
    JCheckBoxMenuItem ckitem = new JCheckBoxMenuItem("Enable Scrolling");
    ckitem.setState(true);
    ckitem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          scrollingEnabled = ((JCheckBoxMenuItem) e.getSource()).getState();
        }
      });
    menu.add(ckitem);

    ckitem = new JCheckBoxMenuItem("Enable Rotation");
    ckitem.setState(true);
    ckitem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          rotationEnabled = ((JCheckBoxMenuItem) e.getSource()).getState();
        }
      });
    menu.add(ckitem);

    ckitem = new JCheckBoxMenuItem("Enable Fades");
    ckitem.setState(true);
    ckitem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          fadesEnabled = ((JCheckBoxMenuItem) e.getSource()).getState();
        }
      });
    menu.add(ckitem);

    menuBar.add(menu);

    frame.setJMenuBar(menuBar);

    desktop = new XTDesktopPane();
    transManager = (XTBasicTransitionManager) desktop.getTransitionManager();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(desktop);

    DisplayMode cur = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
    int width = (int) (cur.getWidth() * 0.75f);
    int height = (int) (width * 3.0f / 4.0f);
    if (height >= 95.0f * cur.getHeight()) {
      height = (int) (cur.getHeight() * 0.75f);
      width = (int) (height * 4.0f / 3.0f);
    }
    frame.setSize(width, height);
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    new Main().run(args);
  }
}
