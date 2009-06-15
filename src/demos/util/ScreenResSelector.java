/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package demos.util;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class ScreenResSelector {
  static interface DisplayModeFilter {
    public boolean filter(DisplayMode mode);
  }

  public static java.util.List getAvailableDisplayModes() {
    java.util.List modes = getDisplayModes();
    final DisplayMode curMode = getCurrentDisplayMode();
    // Filter everything which is higher frequency than the current
    // display mode
    modes = filterDisplayModes(modes, new DisplayModeFilter() {
        public boolean filter(DisplayMode mode) {
          return (mode.getRefreshRate() <= curMode.getRefreshRate());
        }
      });
    // Filter everything that is not at least 24-bit
    modes = filterDisplayModes(modes, new DisplayModeFilter() {
        public boolean filter(DisplayMode mode) {
          // Bit depth < 0 means "multi-depth" -- can't reason about it
          return (mode.getBitDepth() < 0 || mode.getBitDepth() >= 24);
        }
      });
    // Filter everything less than 640x480
    modes = filterDisplayModes(modes, new DisplayModeFilter() {
        public boolean filter(DisplayMode mode) {
          return (mode.getWidth() >= 640 && mode.getHeight() >= 480);
        }
      });
    if (modes.size() == 0) {
      throw new RuntimeException("Couldn't find any valid display modes");
    }
    return modes;
  }

  /** Shows a modal dialog containing the available screen resolutions
      and requests selection of one of them. Returns the selected one. */
  public static DisplayMode showSelectionDialog() {
    SelectionDialog dialog = new SelectionDialog();
    dialog.setVisible(true);
    dialog.waitFor();
    return dialog.selected();
  }

  public static void main(String[] args) {
    DisplayMode mode = showSelectionDialog();
    if (mode != null) {
      System.err.println("Selected display mode:");
      System.err.println(modeToString(mode));
    } else {
      System.err.println("No display mode selected.");
    }
    System.exit(0);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static DisplayMode getCurrentDisplayMode() {
    GraphicsDevice dev = getDefaultScreenDevice();
    return dev.getDisplayMode();
  }

  private static java.util.List/*<DisplayMode>*/ getDisplayModes() {
    GraphicsDevice dev = getDefaultScreenDevice();
    DisplayMode[] modes = dev.getDisplayModes();
    return toList(modes);
  }

  private static GraphicsDevice getDefaultScreenDevice() {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
  }

  private static java.util.List/*<DisplayMode>*/ toList(DisplayMode[] modes) {
    java.util.List res = new ArrayList();
    for (int i = 0; i < modes.length; i++) {
      res.add(modes[i]);
    }
    return res;
  }

  private static String modeToString(DisplayMode mode) {
    return (((mode.getBitDepth() > 0) ? (mode.getBitDepth() + " bits, ") : "") +
            mode.getWidth() + "x" + mode.getHeight() + ", " +
            mode.getRefreshRate() + " Hz");
  }

  private static String[] modesToString(java.util.List/*<DisplayMode>*/ modes) {
    String[] res = new String[modes.size()];
    int i = 0;
    for (Iterator iter = modes.iterator(); iter.hasNext(); ) {
      res[i++] = modeToString((DisplayMode) iter.next());
    }
    return res;
  }

  private static java.util.List/*<DisplayMode>*/ filterDisplayModes(java.util.List/*<DisplayMode>*/ modes,
                                                                    DisplayModeFilter filter) {
    java.util.List res = new ArrayList();
    for (Iterator iter = modes.iterator(); iter.hasNext(); ) {
      DisplayMode mode = (DisplayMode) iter.next();
      if (filter.filter(mode)) {
        res.add(mode);
      }
    }
    return res;
  }

  static class SelectionDialog extends JFrame {
    private Object monitor = new Object();
    private java.util.List modes;
    private volatile boolean done = false;
    private volatile int selectedIndex;

    public SelectionDialog() {
      super();

      setTitle("Display Modes");
      modes = getAvailableDisplayModes();
      String[] strings = modesToString(modes);
      final JList modeList = new JList(strings);
      modeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      modeList.setSelectedIndex(0);
      JScrollPane scroller = new JScrollPane(modeList);
      getContentPane().setLayout(new BorderLayout());
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(new JLabel("Select full-screen display mode,"), BorderLayout.NORTH);
      panel.add(new JLabel("or Cancel for windowed mode:"),     BorderLayout.CENTER);
      getContentPane().add(BorderLayout.NORTH, panel);
      getContentPane().add(BorderLayout.CENTER, scroller);
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      buttonPanel.add(Box.createGlue());
      JButton button = new JButton("OK");
      button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            selectedIndex = modeList.getSelectedIndex();
            done = true;
            synchronized(monitor) {
              monitor.notify();
            }
            setVisible(false);
            dispose();
          }
        });
      buttonPanel.add(button);
      buttonPanel.add(Box.createHorizontalStrut(10));
      button = new JButton("Cancel");
      button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            selectedIndex = -1;
            done = true;
            synchronized(monitor) {
              monitor.notify();
            }
            setVisible(false);
            dispose();
          }
        });
      buttonPanel.add(button);
      buttonPanel.add(Box.createGlue());
      getContentPane().add(BorderLayout.SOUTH, buttonPanel);
      setSize(300, 200);
      center(this, Toolkit.getDefaultToolkit().getScreenSize());
    }

    public void waitFor() {
      synchronized(monitor) {
        while (!done) {
          try {
            monitor.wait();
          } catch (InterruptedException e) {
          }
        }
      }
    }

    public DisplayMode selected() {
      if (selectedIndex < 0) {
        return null;
      } else {
        return (DisplayMode) modes.get(selectedIndex);
      }
    }
  }

  private static void center(Component component,
                             Dimension containerDimension) {
    Dimension sz = component.getSize();
    int x = ((containerDimension.width - sz.width) / 2);
    int y = ((containerDimension.height - sz.height) / 2);
    component.setLocation(x, y);
  }
}
