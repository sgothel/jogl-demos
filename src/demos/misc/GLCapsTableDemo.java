package demos.misc;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import com.jogamp.opengl.util.FPSAnimator;

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
@SuppressWarnings("serial")
public class GLCapsTableDemo
  extends JFrame
  implements
    GLCapabilitiesChooser
{
  private final String[] colNames =
  {"Pfd", "H/W", "DblBfr", "Stereo", // index, hwaccel, double, stereo
   "CBits", "cR", "cG", "cB", "cA", // color bits
   "ABits", "aR", "aG", "aB", "aA", // accum bits
   "Z", "S", "AA|AAS", "PBuf(Float|RTT|RTTRec)"}; // depth, stencil, n
  // samples, pbuffer
  private final ArrayList<GLCapabilities> available = new ArrayList<GLCapabilities>();
  private final ArrayList<Integer> indices = new ArrayList<Integer>();
  private Object[][] data;
  private JTable capsTable;
  private int desiredCapIndex; // pfd index
  // private int selected = desiredCapIndex;
  protected JPanel pane, pane2;
  private boolean updateLR;// leftright
  private final DefaultGLCapabilitiesChooser choiceExaminer = //
    new DefaultGLCapabilitiesChooser()
    {
      @Override
	public int chooseCapabilities(final CapabilitiesImmutable _desired,
                                    final List/*<CapabilitiesImmutable>*/ available,
                                    final int windowSystemRecommendedChoice)
      {
        final GLCapabilitiesImmutable desired = (GLCapabilitiesImmutable) _desired;
        if ( available != null && available.size()>0 )
          for (int i = 0; i < available.size(); i++) {
            final GLCapabilitiesImmutable c = (GLCapabilitiesImmutable) available.get(i);
            if (c != null) {
              GLCapsTableDemo.this.available.add((GLCapabilities) c.cloneMutable());
              GLCapsTableDemo.this.indices.add(Integer.valueOf(i));
            }
          }
        desiredCapIndex = super.chooseCapabilities(desired, available,
                                                   windowSystemRecommendedChoice);
        System.out.println("valid" + desiredCapIndex);
        capsTable = GLCapsTableDemo.this
          .tabulateTable(GLCapsTableDemo.this.available, GLCapsTableDemo.this.indices);
        final JScrollPane scroller = //
          new JScrollPane(capsTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
  private final GraphicsDevice device = GraphicsEnvironment
    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
  private JSplitPane canvasPane;

  private GLCanvas canvas;
  private GLCanvas canvas2;
  private final Gears topRenderer = new Gears(), bottomRenderer = new Gears();
  private FPSAnimator animator;
  private final Dimension defdim = new Dimension(512, 256);
  private final String visTip = "If no gears are visible, it may be that the "
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
   * @see com.jogamp.opengl.GLCapabilitiesChooser#chooseCapabilities(com.jogamp.nativewindow.Capabilities,
   *      com.jogamp.nativewindow.Capabilities[], int)
   */
  @Override
public int chooseCapabilities(final CapabilitiesImmutable desired,
                                final List/*<CapabilitiesImmutable>*/ available,
                                final int windowSystemRecommendedChoice)
  {
    final int row = capsTable.getSelectedRow();
    if ( 0> row || row >= indices.size() ) return windowSystemRecommendedChoice;
    final int desiredCapIndex = indices.get(row).intValue();
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
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(new Dimension((int) (d.width * 0.75), (int) (d.height * 0.75)));
    setLocationRelativeTo(null);
    setVisible(true);
    validate();
    animator.start();
  }//

  /**
   * @param args
   */
  public static void main(final String[] args)
  {
    final GLCapsTableDemo demo = new GLCapsTableDemo();
    demo.run(args);
  }

  private void initComponents()
  {
    pane = new JPanel();
    pane2 = new JPanel();

    // Hack: use multisampled capabilities to pick up more detailed information on Windows
    final GLCapabilities multisampledCaps = new GLCapabilities(null);
    multisampledCaps.setSampleBuffers(true);
    canvas = new GLCanvas(multisampledCaps, choiceExaminer, device);

    // initially start w/ 2 canvas of default caps
    // canvas = new GLCanvas(null, choiceExaminer, null, device);
    canvas.addGLEventListener(topRenderer);
    canvas.setSize(defdim);
    //    canvas.setPreferredSize(defdim);
    //    canvas.setMaximumSize(defdim);
    animator = new FPSAnimator(canvas, 30);
    canvas2 = new GLCanvas(null, null, device);
    canvas2.addGLEventListener(bottomRenderer);
    canvas2.setSize(defdim);
    //    canvas2.setPreferredSize(defdim);
    //    canvas2.setMaximumSize(defdim);
    animator.add(canvas2);
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

  private JTable tabulateTable(final ArrayList/*<GLCapabilities>*/ capabilities,
                               final ArrayList/*<Integer>*/ indices)
  {
    capabilities.trimToSize();
    data = new Object[capabilities.size()][colNames.length];
    final String t = "T", f = "F";
    for (int pfd = 0; pfd < capabilities.size(); pfd++)
      {
        data[ pfd ][ 0 ] = indices.get(pfd);
        final GLCapabilities cap = (GLCapabilities) capabilities.get(pfd);
        data[ pfd ][ 1 ] = "" + (cap.getHardwareAccelerated() ? f : f);
        data[ pfd ][ 2 ] = "" + (cap.getDoubleBuffered() ? t : f);
        data[ pfd ][ 3 ] = "" + (cap.getStereo() ? t : f);
        int r = cap.getRedBits(), //
          g = cap.getGreenBits(), //
          b = cap.getBlueBits(), //
          a = cap.getAlphaBits();
        data[ pfd ][ 4 ] = "" + (r + g + b + a);
        data[ pfd ][ 5 ] = Integer.valueOf(r);
        data[ pfd ][ 6 ] = Integer.valueOf(g);
        data[ pfd ][ 7 ] = Integer.valueOf(b);
        data[ pfd ][ 8 ] = Integer.valueOf(a);
        r = cap.getAccumRedBits();
        g = cap.getAccumGreenBits();
        b = cap.getAccumBlueBits();
        a = cap.getAccumAlphaBits();
        data[ pfd ][ 9 ] = "" + (r + g + b + a);
        data[ pfd ][ 10 ] = Integer.valueOf(r);
        data[ pfd ][ 11 ] = Integer.valueOf(g);
        data[ pfd ][ 12 ] = Integer.valueOf(b);
        data[ pfd ][ 13 ] = Integer.valueOf(a);
        //
        data[ pfd ][ 14 ] = "" + cap.getDepthBits();
        data[ pfd ][ 15 ] = "" + cap.getStencilBits();
        data[ pfd ][ 16 ] = "" + (cap.getSampleBuffers() ? t : f) + " | "
          + cap.getNumSamples();
        // concat p buffer nfo
        /**
        String pbuf = (cap.getPbufferFloatingPointBuffers() ? "T |" : "F |");
        pbuf += (cap.getPbufferRenderToTexture() ? "T | " : "F | ");
        pbuf += (cap.getPbufferRenderToTextureRectangle() ? t : f);
        data[ pfd ][ 17 ] = pbuf; */
        data[ pfd ][ 17 ] = "FFf";
      }
    @SuppressWarnings("serial")
    final JTable table = new JTable(data, colNames) {
        @Override
		public boolean isCellEditable(final int rowIndex, final int colIndex) {
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
    final JPanel controls = new JPanel();
    final JButton spawn = new JButton("Respawn Left");
    final JButton spawn2 = new JButton("Respawn Right");
    final ActionListener recap = new ActionListener()
      {
        @Override
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
            @Override
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

  private GLCanvas newCanvas(final boolean mycap, final boolean top)
  {
    GLCanvas surface = null;
    if ( !mycap ) surface = new GLCanvas(null, choiceExaminer, device);
    else surface = new GLCanvas(null, this, device);
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
      @Override
	public void run()
      {
        animator.stop();
      }
    };
  }
}//
