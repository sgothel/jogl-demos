package demos.misc;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.swing.*;
import com.sun.opengl.util.FPSAnimator;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import demos.gears.Gears;

/*******************************************************************************
 * @file GLCapsTableDemo.java
 * @desc Demonstrate use of GLCapabilitiesChooser and DefaultGLCapabilities.
 *       Demo tabulates the available capabilities array and put the data into a
 *       table. Pressing respawn button displays a canvas created with the
 *       currently selected index corresponding to the available array. There
 *       are two canvas to respawn: left or right.<br>
 *       TODO: if the number of samples > 0, setSampleBuffer(true) and run an
 *       antialiased renderer?;<br>
 *       TODO: if pbuffer is available, enable Float, RTT, RTTRec and create a
 *       pbuffer for eacH?<br>
 *       TODO: spawn using a diff renderer option(such as ones from demo
 *       package) <br>
 * @version Jan 22, 2006 - GLCapsTableDemo.java created at 7:17:31 PM
 * @platform ATI X600SE/XP Tablet SP2/JDK5/Eclipse
 * @author Kiet Le
 * @legal (c) 2006 Kiet Le. Released under BSD licence.
 ******************************************************************************/
public class GLCapsTableDemo
  extends JFrame
  implements
    GLCapabilitiesChooser
{
  private String[] colNames =
  {"Pfd", "H/W", "DblBfr", "Stereo", // index, hwaccel, double, stereo
   "CBits", "cR", "cG", "cB", "cA", // color bits
   "ABits", "aR", "aG", "aB", "aA", // accum bits
   "Z", "S", "AA|AAS", "PBuf(Float|RTT|RTTRec)"}; // depth, stencil, n
  // samples, pbuffer
  private ArrayList/*<GLCapabilities>*/ available = new ArrayList/*<GLCapabilities>*/();
  private ArrayList/*<Integer>*/ indices = new ArrayList/*<Integer>*/();
  private Object[][] data;
  private JTable capsTable;
  private int desiredCapIndex; // pfd index
  private int selected = desiredCapIndex;
  protected JPanel pane, pane2;
  private boolean updateLR;// leftright
  private DefaultGLCapabilitiesChooser choiceExaminer = //
    new DefaultGLCapabilitiesChooser()
    {
      public int chooseCapabilities(GLCapabilities desired,
                                    GLCapabilities[] available,
                                    int windowSystemRecommendedChoice)
      {
        if ( available != null )
          for (int i = 0; i < available.length; i++) {
            GLCapabilities c = available[i];
            if (c != null) {
              GLCapsTableDemo.this.available.add((GLCapabilities) c.clone());
              GLCapsTableDemo.this.indices.add(new Integer(i));
            }
          }
        desiredCapIndex = super.chooseCapabilities(desired, available,
                                                   windowSystemRecommendedChoice);
        System.out.println("valid" + desiredCapIndex);
        capsTable = GLCapsTableDemo.this
          .tabulateTable(GLCapsTableDemo.this.available, GLCapsTableDemo.this.indices);
        JScrollPane scroller = //
          new JScrollPane(capsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        GLCapsTableDemo.this.getContentPane().add(scroller);
        pane.setBorder(BorderFactory
                       .createTitledBorder(null, "" + desiredCapIndex, TitledBorder.TRAILING,
                                           TitledBorder.DEFAULT_POSITION));
        pane2.setBorder(BorderFactory
                        .createTitledBorder(null, "" + desiredCapIndex, TitledBorder.LEADING,
                                            TitledBorder.DEFAULT_POSITION));
        GLCapsTableDemo.this.validate();// so table'll show up
        System.out.println("valid");
        return desiredCapIndex;
      }
    };
  private GraphicsDevice device = GraphicsEnvironment
    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
  private JSplitPane canvasPane;

  private GLCanvas canvas;
  private GLCanvas canvas2;
  private Gears topRenderer = new Gears(), bottomRenderer = new Gears();
  private FPSAnimator animator;
  private Dimension defdim = new Dimension(512, 256);
  private String visTip = "If no gears are visible, it may be that the "
    + "current desktop color resolution doesn't match "
    + "the GLCapabilities chosen. Check CBits column.";

  /**
	 
  */
  public GLCapsTableDemo()
  {
    super(GLCapsTableDemo.class.getName());
    initComponents();
  }

  /**
   * (non-Javadoc)
   * 
   * @see javax.media.opengl.GLCapabilitiesChooser#chooseCapabilities(javax.media.opengl.GLCapabilities,
   *      javax.media.opengl.GLCapabilities[], int)
   */
  public int chooseCapabilities(GLCapabilities desired,
                                GLCapabilities[] available,
                                int windowSystemRecommendedChoice)
  {
    int row = capsTable.getSelectedRow();
    int desiredCapIndex = ((Integer) indices.get(row)).intValue();
    if ( updateLR )
      {
        pane.setBorder(BorderFactory
                       .createTitledBorder(null, "" + desiredCapIndex,
                                           TitledBorder.TRAILING,
                                           TitledBorder.DEFAULT_POSITION));
      }
    else
      {
        pane2.setBorder(BorderFactory
                        .createTitledBorder(null, "" + desiredCapIndex, TitledBorder.LEADING,
                                            TitledBorder.DEFAULT_POSITION));
      }
    return desiredCapIndex;
  }

  public void run(final String[] args)
  {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(new Dimension((int) (d.width * 0.75), (int) (d.height * 0.75)));
    setLocationRelativeTo(null);
    setVisible(true);
    validate();
    animator.start();
  }//

  /**
   * @param args
   */
  public static void main(String[] args)
  {
    GLCapsTableDemo demo = new GLCapsTableDemo();
    demo.run(args);
  }

  private void initComponents()
  {
    // Hack: use multisampled capabilities to pick up more detailed information on Windows
    GLCapabilities multisampledCaps = new GLCapabilities();
    multisampledCaps.setSampleBuffers(true);
    canvas = new GLCanvas(multisampledCaps, choiceExaminer, null, device);

    // initially start w/ 2 canvas of default caps
    // canvas = new GLCanvas(null, choiceExaminer, null, device);
    canvas.addGLEventListener(topRenderer);
    canvas.setSize(defdim);
    //    canvas.setPreferredSize(defdim);
    //    canvas.setMaximumSize(defdim);
    animator = new FPSAnimator(canvas, 30);
    canvas2 = new GLCanvas(null, null, null, device);
    canvas2.addGLEventListener(bottomRenderer);
    canvas2.setSize(defdim);
    //    canvas2.setPreferredSize(defdim);
    //    canvas2.setMaximumSize(defdim);
    animator.add(canvas2);
    pane = new JPanel();
    pane2 = new JPanel();
    pane.add(canvas);
    pane2.add(canvas2);
    canvasPane = new JSplitPane();
    canvasPane.setResizeWeight(0.5);// 50-50 division
    canvasPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    canvasPane.setLeftComponent(pane);
    canvasPane.setRightComponent(pane2);
    getContentPane().add(canvasPane, BorderLayout.SOUTH);
    getContentPane().add(buildControls(), BorderLayout.NORTH);
  }

  private JTable tabulateTable(ArrayList/*<GLCapabilities>*/ capabilities,
                               ArrayList/*<Integer>*/ indices)
  {
    capabilities.trimToSize();
    data = new Object[capabilities.size()][colNames.length];
    String t = "T", f = "F";
    for (int pfd = 0; pfd < capabilities.size(); pfd++)
      {
        data[ pfd ][ 0 ] = indices.get(pfd);
        GLCapabilities cap = (GLCapabilities) capabilities.get(pfd);
        data[ pfd ][ 1 ] = "" + (cap.getHardwareAccelerated() ? f : f);
        data[ pfd ][ 2 ] = "" + (cap.getDoubleBuffered() ? t : f);
        data[ pfd ][ 3 ] = "" + (cap.getStereo() ? t : f);
        int r = cap.getRedBits(), // 
          g = cap.getGreenBits(), //
          b = cap.getBlueBits(), //
          a = cap.getAlphaBits();
        data[ pfd ][ 4 ] = "" + (r + g + b + a);
        data[ pfd ][ 5 ] = new Integer(r);
        data[ pfd ][ 6 ] = new Integer(g);
        data[ pfd ][ 7 ] = new Integer(b);
        data[ pfd ][ 8 ] = new Integer(a);
        r = cap.getAccumRedBits();
        g = cap.getAccumGreenBits();
        b = cap.getAccumBlueBits();
        a = cap.getAccumAlphaBits();
        data[ pfd ][ 9 ] = "" + (r + g + b + a);
        data[ pfd ][ 10 ] = new Integer(r);
        data[ pfd ][ 11 ] = new Integer(g);
        data[ pfd ][ 12 ] = new Integer(b);
        data[ pfd ][ 13 ] = new Integer(a);
        //
        data[ pfd ][ 14 ] = "" + cap.getDepthBits();
        data[ pfd ][ 15 ] = "" + cap.getStencilBits();
        data[ pfd ][ 16 ] = "" + (cap.getSampleBuffers() ? t : f) + " | "
          + cap.getNumSamples();
        // concat p buffer nfo
        String pbuf = (cap.getPbufferFloatingPointBuffers() ? "T |" : "F |");
        pbuf += (cap.getPbufferRenderToTexture() ? "T | " : "F | ");
        pbuf += (cap.getPbufferRenderToTextureRectangle() ? t : f);
        data[ pfd ][ 17 ] = pbuf;
      }
    JTable table = new JTable(data, colNames) {
        public boolean isCellEditable(int rowIndex, int colIndex) {
          return false;
        }
      };
    //    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    TableColumn column = null;
    for (int i = 0; i < colNames.length; i++)
      {
        column = table.getColumnModel().getColumn(i);
        if ( i == (colNames.length - 1) )
          {
            column.setPreferredWidth(100); // pbuffer column is bigger
          }
        else column.setPreferredWidth(7);
      }
    table.setDoubleBuffered(true);
    return table;
  }

  private JPanel buildControls()
  {
    JPanel controls = new JPanel();
    final JButton spawn = new JButton("Respawn Left");
    final JButton spawn2 = new JButton("Respawn Right");
    ActionListener recap = new ActionListener()
      {
        public void actionPerformed(final ActionEvent act)
        {
          animator.stop();
          if ( act.getSource() == spawn )
            {
              updateLR = true;// left
              animator.remove(canvas);
              pane.remove(canvas);
              canvas = newCanvas(true, true);// get new canvas w/ selected index
              pane.add(canvas);
              animator.add(canvas);
            }
          else
            {
              updateLR = false;
              animator.remove(canvas2);
              pane2.remove(canvas2);
              canvas2 = newCanvas(true, false);
              pane2.add(canvas2);
              animator.add(canvas2);
            }
          new Thread()
          {
            public void run()
            {
              animator.start();
            }
          }.start();
          GLCapsTableDemo.this.validate();
        }
      };
    spawn.setToolTipText(visTip);
    spawn.addActionListener(recap);
    spawn2.addActionListener(recap);
    //
    controls.add(spawn);
    controls.add(spawn2);
    return controls;
  }

  private GLCanvas newCanvas(boolean mycap, boolean top)
  {
    GLCanvas surface = null;
    if ( !mycap ) surface = new GLCanvas(null, choiceExaminer, null, device);
    else surface = new GLCanvas(null, this, null, device);
    if ( top ) surface.addGLEventListener(topRenderer);
    else surface.addGLEventListener(bottomRenderer);
    surface.setSize(defdim);// otherwise, no show; mixin' light-heavy containers
    //  surface.setMinimumSize(defdim);
    return surface;
  }

  private void exitRunner()
  {
    new Thread()
    {
      public void run()
      {
        animator.stop();
      }
    };
  }
}//
