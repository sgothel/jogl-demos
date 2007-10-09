package demos.nurbs.surfaceapp;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLCanvas;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.ToolTipManager;

import demos.nurbs.icons.*;
import demos.nurbs.knotslidercomponent.JKnotSlider;

/**
 * Main class for application demostrating capabilitues of JOGL library extend by NURBS surface functionalities
 * Hlavní třída aplikace demonstrující shopnosti knihovny JOGL při práci s NURBS
 * plochami
 * 
 * @author Tomáš Hráský
 * 
 */
@SuppressWarnings("serial")
public class SurfaceApp extends JFrame implements ActionListener
{

    /**
     * X-coord editing component name
     * Jméno komponenty pro editaci X-ové souřadnice aktuálního bodu
     */
    public static final String X_SPINNER_NAME = "xspinner";

    /**
     * Y-coord editing component name
     * Jméno komponenty pro editaci Y-ové souřadnice aktuálního bodu
     */
    public static final String Y_SPINNER_NAME = "yspinner";

    /**
     * Z-coord editing component name
     * Jméno komponenty pro editaci Z-ové souřadnice aktuálního bodu
     */
  public static final String Z_SPINNER_NAME = "zspinner";

  /**
   * Weight editing component name
   * Jméno komponenty pro editaci váhy aktuálního bodu
   */
  public static final String W_SPINNER_NAME = "wspinner";

  /**
   * U direction knotvector editing component
   * Jméno komponenty pro editaci uzlového vektoru ve směru parametru U
   */
  private static final String U_KNOTSLIDER = "Uknotspinner";

  /**
   * V direction knotvector editing component
   * Jméno komponenty pro editaci uzlového vektoru ve směru parametru V
   */
  private static final String V_KNOTSLIDER = "Vknotspinner";

  /**
   * New control point action name
   * Jméno události přidání řídícího bodu
   */
  public static final String PRIDAT_AC = "PRIDAT";

  /**
   * Degree set event name
   * Jméno události zadání stupně křivky
   */
  public static final String STUPEN_AC = "STUPEN";

  /**
   * Delete control point event name
   * Jméno události smazání řídícího bodu
   */
  public static final String SMAZAT_AC = "SMAZAT";

  /**
   * New clamped knotvector event name
   * Jméno události vytvoření uzavřeného uzlového vektoru
   */
  public static final String UZAVRENY_AC = "UZAVRENY";

  /**
   * New uniform knotvector event name
   * Jméno události vytvoření otevřeného (uniformního) uzlového vektoru
   */
  public static final String OTEVRENY_AC = "OTEVRENY";

  /**
   * Save surface event name
   * Jméno události uložení plochy
   */
  public static final String ULOZIT_AC = "ULOZIT";

  /**
   * Load surface event name
   * Jméno události načetení uložené definice plochy
   */
  public static final String NACIST_AC = "NACIST";

  /**
   * Move control point event name
   * Jméno události pohybu řídícího bodu
   */
  private static final String MOVE_AC = "MOVE";

  /**
   * New surface event name
   * Jméno události vytvoření nové plochy
   */
  static final String NOVA_AC = "NEWSURFACE";

  /**
   * Exit app event name
   * Jméno události ukončení aplikace
   */
  public static final String EXIT_AC = "EXIT";

  /**
   * Show about event name
   * Jméno události zobrazení okna o aplikaci
   */
  public static final String INFO_AC = "INFO";

  /**
   * Add column of control points event name
   * Jméno události přidání sloupce řídících bodů
   */
  public static final String PRIDAT_AC_SLOUPEC = "ADDCOL";

  /**
   * Add row of control points event name
   * Jméno události přidání řádku řídících bodů
   */
  public static final String PRIDAT_AC_RADEK = "ADDROW";

  /**
   * Remove row of control points event name
   * Jméno události smazání řádku řídících bodů
   */
  public static final String SMAZAT_AC_RADEK = "DELROW";

  /**
   * Remove column of control points event name
   * Jméno události smazání sloupce řídících bodů
   */
  public static final String SMAZAT_AC_SLOUPEC = "DELCOL";

  /**
   * OpenGL drawing canvas
   * Plátno pro vykreslování pomocí OpenGL
   */
  private GLCanvas glCanvas;

  /**
   * X-coord editing component
   * Komponenta pro editaci X-ové souřadnice aktuálního bodu
   */
  private JSpinner xSpinner;

  /**
   * Y-coord editing component
   * Komponenta pro editaci Y-ové souřadnice aktuálního bodu
   */
  private JSpinner ySpinner;

  /**
   * Weight editing component
   * Komponenta pro editaci váhy aktuálního bodu
   */
  private JSpinner wSpinner;

  /**
   * Z-coord editing component
   * Komponenta pro editaci Z-ové souřadnice aktuálního bodu
   */
  private JSpinner zSpinner;

  /**
   * Mouse listener
   * Listener událostí myši
   */
  private SurfaceMouseListener mouseListener;

  /**
   * U direction knotvector editing component
   * Komponenta pro editaci uzlového vektoru ve směru parametru U
   */
  private JKnotSlider knotSlider;

  /**
   * V direction knotvector editing component
   * Komponenta pro editaci uzlového vektoru ve směru parametru V
   */
  private JKnotSlider knotSlider2;

  /**
   * Set move points mode
   * Tlačítko pro zapnutí módu pohybu řídících bodů
   */
  private JToggleButton moveTB;

  /**
   * X rotation component
   * Komponenta ovládající otočení definované plochy kolem osy X
   */
  private JSlider rotaceXSlider;

  /**
   * Y rotation component
   * Komponenta ovládající otočení definované plochy kolem osy Y
   */
  private JSlider rotaceYSlider;

  /**
   * Z rotation component
   * Komponenta ovládající otočení definované plochy kolem osy Z
   */
  private JSlider rotaceZSlider;

  /**
   * Label for X rotation editing component
   * Nadpis komponenty ovládající otočení definované plochy kolem osy X
   */
  private JLabel rotaceXLabel;
  /**
   * Label for Y rotation editing component
   * Nadpis komponenty ovládající otočení definované plochy kolem osy Y
   */
  private JLabel rotaceYLabel;
  /**
   * Label for Z rotation editing component
   * Nadpis komponenty ovládající otočení definované plochy kolem osy Z
   */
  private JLabel rotaceZLabel;

  /**
   * GL events listener
   * Objekt reagující na události OpenGL plátna
   */
  private GLListener glListener;

  /**
   * Use lighting checkbox
   * Checkbox použití nasvícení objektu 
   */
  private JCheckBox lightingChBox;

  /**
   * Constructor, creates GUI
   * Konstruktor, vytvoří grafické uživatelské rozhraní
   */
  public SurfaceApp() {
    //super( "Tomáš Hráský - ukázková aplikace funkcionality GLU NURBS funkcí - JOGL");
    super( "Tomáš Hráský - example application of GLU NURBS in JOGL");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    initGUI();
  }

  /**
   * GUI initialization
   * Inicializace grafického uživatelského rozhraní
   */
  private void initGUI() {
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

    this.glCanvas = new GLCanvas();
    glCanvas.setSize(new Dimension(750, 500));
    this.glListener=new GLListener(this);
    glCanvas.addGLEventListener(glListener);
    mouseListener = new SurfaceMouseListener(this);
    glCanvas.addMouseListener(mouseListener);
    glCanvas.addMouseMotionListener(mouseListener);
    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;

    c.gridy = 0;
    c.gridwidth = GridBagConstraints.REMAINDER;

    ActListener listener = new ActListener(this);

    JMenuBar menuBar = new JMenuBar();
    getContentPane().add(menuBar, c);

    JMenu aplikaceMenu = new JMenu("Aplication");
    menuBar.add(aplikaceMenu);

    JMenuItem aboutMI = new JMenuItem("About");
    aboutMI.setActionCommand(INFO_AC);
    aboutMI.addActionListener(listener);
    aplikaceMenu.add(aboutMI);

    aplikaceMenu.add(new JSeparator());

    JMenuItem konecMI = new JMenuItem("Exit");
    konecMI.addActionListener(listener);
    konecMI.setActionCommand(EXIT_AC);
    aplikaceMenu.add(konecMI);

    JMenu krivkaMenu = new JMenu("Surface");
    menuBar.add(krivkaMenu);

    JMenu pridatBodyM = new JMenu("Add points");
    krivkaMenu.add(pridatBodyM);
    //		pridatBodyM.addActionListener(listener);

    //		pridatBodyM.setActionCommand(PRIDAT_AC);
		
    JMenuItem pridatRadkyMI=new JMenuItem("Points row");
    pridatRadkyMI.setActionCommand(PRIDAT_AC_RADEK);
    pridatRadkyMI.addActionListener(listener);
		
    JMenuItem pridatSloupceMI=new JMenuItem("Points column");
    pridatSloupceMI.setActionCommand(PRIDAT_AC_SLOUPEC);
    pridatSloupceMI.addActionListener(listener);
    pridatBodyM.add(pridatRadkyMI);
    pridatBodyM.add(pridatSloupceMI);
		
		
    JMenu smazatBodyM = new JMenu("Delete points");
    krivkaMenu.add(smazatBodyM);
    //		smazatBodyM.addActionListener(listener);

    //		smazatBodyM.setActionCommand(SMAZAT_AC);

    JMenuItem smazatRadkyMI=new JMenuItem("Points row");
    smazatRadkyMI.setActionCommand(SMAZAT_AC_RADEK);
    smazatRadkyMI.addActionListener(listener);
    smazatBodyM.add(smazatRadkyMI);
    JMenuItem smazatSloupceMI=new JMenuItem("Points column");
    smazatSloupceMI.setActionCommand(SMAZAT_AC_SLOUPEC);
    smazatSloupceMI.addActionListener(listener);
    smazatBodyM.add(smazatSloupceMI);
		
		
    JMenuItem stupenMI = new JMenuItem("Set surface degree");
    krivkaMenu.add(stupenMI);
    stupenMI.addActionListener(listener);
    stupenMI.setActionCommand(STUPEN_AC);

    JMenu knotVecMenu = new JMenu("Create knotvectors");
    krivkaMenu.add(knotVecMenu);

    JMenuItem clampedKVMI = new JMenuItem("Clamped");
    knotVecMenu.add(clampedKVMI);
    clampedKVMI.setActionCommand(UZAVRENY_AC);
    clampedKVMI.addActionListener(listener);
    JMenuItem unclampedKVMI = new JMenuItem("Uniform");
    knotVecMenu.add(unclampedKVMI);
    unclampedKVMI.setActionCommand(OTEVRENY_AC);
    unclampedKVMI.addActionListener(listener);

    JMenuItem moveMI = new JMenuItem("Move points");
    krivkaMenu.add(moveMI);
    moveMI.setActionCommand(MOVE_AC);
    moveMI.addActionListener(listener);

    krivkaMenu.add(new JSeparator());

    krivkaMenu.add(new JSeparator());

    JMenuItem novaMI = new JMenuItem("New surface");
    krivkaMenu.add(novaMI);
    novaMI.setActionCommand(NOVA_AC);
    novaMI.addActionListener(listener);

    JMenuItem ulozitMI = new JMenuItem("Safe surface as...");
    krivkaMenu.add(ulozitMI);
    ulozitMI.setActionCommand(ULOZIT_AC);
    ulozitMI.addActionListener(listener);
    JMenuItem nacistMI = new JMenuItem("Load surface");
    krivkaMenu.add(nacistMI);
    nacistMI.setActionCommand(NACIST_AC);
    nacistMI.addActionListener(listener);

    c.gridy++;
    JToolBar toolBar = new JToolBar();
    getContentPane().add(toolBar, c);

    ButtonGroup bg = new ButtonGroup();

    JButton novaB = new JButton();
    // novaB.setText("Nová");
    novaB.setToolTipText("New surface");
    novaB.setIcon(IconFactory.getIcon("demos/nurbs/icons/folder_new.png"));
    novaB.setActionCommand(NOVA_AC);
    novaB.addActionListener(listener);
    toolBar.add(novaB);

    JButton ulozitB = new JButton();
    ulozitB.setIcon(IconFactory.getIcon("demos/nurbs/icons/adept_sourceseditor.png"));
    // ulozitB.setText("Uložit");
    ulozitB.setToolTipText("Save");
    ulozitB.setActionCommand(ULOZIT_AC);
    ulozitB.addActionListener(listener);
    toolBar.add(ulozitB);

    JButton nahratB = new JButton();
    // nahratB.setText("Nahrát");
    nahratB.setToolTipText("Load surface");
    nahratB.setIcon(IconFactory.getIcon("demos/nurbs/icons/fileimport.png"));
    nahratB.setActionCommand(NACIST_AC);
    nahratB.addActionListener(listener);
    toolBar.add(nahratB);

    toolBar.add(new JToolBar.Separator());

    JToggleButton pridatTB = new JToggleButton();
    // pridatTB.setText("Přidat body");
    pridatTB.setToolTipText("Add contol points");
    toolBar.add(pridatTB);
    pridatTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/add.png"));
    //		pridatTB.setActionCommand(PRIDAT_AC);
    //		pridatTB.addActionListener(listener);
		
    bg.add(pridatTB);
		
    final JPopupMenu popup2=new JPopupMenu();
    JMenuItem radkyPopupMI=new JMenuItem("Poits row");
    radkyPopupMI.setActionCommand(PRIDAT_AC_RADEK);
    JMenuItem sloupcePopupMI=new JMenuItem("Points column");
    sloupcePopupMI.setActionCommand(PRIDAT_AC_SLOUPEC);
    radkyPopupMI.addActionListener(listener);
    sloupcePopupMI.addActionListener(listener);
		
    popup2.add(radkyPopupMI);
    popup2.add(sloupcePopupMI);
		
    pridatTB.addMouseListener(new 
                              /**
                               * CLass to add context menu to toolbar button
                               * Třída pro připojení kontextového menu na
                               *         tlačítko na liště nástrojů
                               * @author Tomáš Hráský 
                               */
                              MouseAdapter() {

        @Override
          public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          e.isPopupTrigger();
          popup2.show(e.getComponent(), e.getX(), e.getY());
        }
			
			
      });
		
    JToggleButton smazatTB = new JToggleButton();
    // smazatTB.setText("Smazat body");
    smazatTB.setToolTipText("Delete points");
    toolBar.add(smazatTB);
    smazatTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/fileclose.png"));
    //		smazatTB.setActionCommand(SMAZAT_AC);
    //		smazatTB.addActionListener(listener);
    bg.add(smazatTB);
		
    final JPopupMenu popup3=new JPopupMenu();
    JMenuItem radky2PopupMI=new JMenuItem("Points row");
    radky2PopupMI.setActionCommand(SMAZAT_AC_RADEK);
    JMenuItem sloupce2PopupMI=new JMenuItem("Points column");
    sloupce2PopupMI.setActionCommand(SMAZAT_AC_SLOUPEC);
    radky2PopupMI.addActionListener(listener);
    sloupce2PopupMI.addActionListener(listener);
		
    popup3.add(radky2PopupMI);
    popup3.add(sloupce2PopupMI);

		
    smazatTB.addMouseListener(new 
                              /**
                               * CLass to add context menu to toolbar button
                               * Třída pro připojení kontextového menu na
                               *         tlačítko na liště nástrojů
                               * @author Tomáš Hráský 
                               */
                              MouseAdapter() {

        @Override
          public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          e.isPopupTrigger();
          popup3.show(e.getComponent(), e.getX(), e.getY());
        }
			
			
      });
		
    JToggleButton stupenTB = new JToggleButton();
    // stupenTB.setText("Smazat body");
    stupenTB.setToolTipText("Set surface degree");
    toolBar.add(stupenTB);
    stupenTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/math_rsup.png"));
    stupenTB.setActionCommand(STUPEN_AC);
    stupenTB.addActionListener(listener);
    bg.add(stupenTB);

    final JPopupMenu popup = new JPopupMenu();

    JMenuItem uzavrenyPopupMI = new JMenuItem("Clamped");
    popup.add(uzavrenyPopupMI);
    uzavrenyPopupMI.setActionCommand(UZAVRENY_AC);
    uzavrenyPopupMI.addActionListener(listener);
    JMenuItem otevrenyPopupMI = new JMenuItem("Uniform");
    popup.add(otevrenyPopupMI);
    otevrenyPopupMI.setActionCommand(OTEVRENY_AC);
    otevrenyPopupMI.addActionListener(listener);

    JToggleButton vytvoritButton = new JToggleButton();
    // vytvoritButton.setText("Vytvořit uzlový vektor");
    vytvoritButton.setToolTipText("Create knotvectors");
    vytvoritButton.setIcon(IconFactory.getIcon("demos/nurbs/icons/newfunction.png"));
    bg.add(vytvoritButton);

    vytvoritButton.addMouseListener(new
                                    /**
                                     * CLass to add context menu to toolbar button
                                     * Třída pro připojení kontextového menu na
                                     *         tlačítko na liště nástrojů
                                     * @author Tomáš Hráský 
                                     */
                                    MouseAdapter() {

        @Override
          public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          e.isPopupTrigger();
          popup.show(e.getComponent(), e.getX(), e.getY());
        }

      });
    popup.setInvoker(vytvoritButton);
    toolBar.add(vytvoritButton);

    moveTB = new JToggleButton();
    // moveTB.setText("Hýbat body");
    moveTB.setToolTipText("Move points");
    moveTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/mouse.png"));
    toolBar.add(moveTB);
    moveTB.setActionCommand(MOVE_AC);
    moveTB.addActionListener(listener);
    bg.add(moveTB);
    toolBar.add(new JToolBar.Separator());
    JButton infoB = new JButton();
    // infoB.setText("Ukončit");
    infoB.setToolTipText("About");

    infoB.setIcon(IconFactory.getIcon("demos/nurbs/icons/info.png"));
    toolBar.add(infoB);
    infoB.setActionCommand(INFO_AC);
    infoB.addActionListener(listener);
    toolBar.add(new JToolBar.Separator());

    JButton exitB = new JButton();
    // exitB.setText("Ukončit");
    exitB.setToolTipText("Exit");

    exitB.setIcon(IconFactory.getIcon("demos/nurbs/icons/exit.png"));
    toolBar.add(exitB);
    exitB.setActionCommand(EXIT_AC);
    exitB.addActionListener(listener);

    c.gridwidth = 1;

    c.gridx = 0;
    c.gridy = 2;

    c.weightx = 1;
    c.weighty = 1;

    getContentPane().add(glCanvas, c);
    c.gridx = 1;
    JPanel rightPanel = new JPanel(new GridBagLayout());
    GridBagConstraints cc = new GridBagConstraints();
    cc.insets = new Insets(5, 5, 5, 5);
    xSpinner = new JSpinner(new SpinnerNumberModel(0, -10000, 10000.0, 1));
    ySpinner = new JSpinner(new SpinnerNumberModel(0, -10000.0, 10000.0, 1));
    zSpinner = new JSpinner(new SpinnerNumberModel(0, -10000.0, 10000.0, 1));
    wSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000.0, .05));

    SpinnerListener spinnerListener = new SpinnerListener(this);
    SliderListener sliderListener=new SliderListener(this);
		
    xSpinner.addChangeListener(spinnerListener);
    xSpinner.setName(X_SPINNER_NAME);
    ySpinner.addChangeListener(spinnerListener);
    ySpinner.setName(Y_SPINNER_NAME);
    wSpinner.addChangeListener(spinnerListener);
    wSpinner.setName(W_SPINNER_NAME);
		
    zSpinner.setName(Z_SPINNER_NAME);
    zSpinner.addChangeListener(spinnerListener);

    cc.gridx = 0;
    cc.gridy = 0;
    cc.gridwidth = 2;
    cc.weighty = 0;

    rotaceXLabel = new JLabel();
    rightPanel.add(rotaceXLabel, cc);
    cc.gridy++;

    rotaceXSlider = new JSlider(-180, 180, 0);
    rotaceXSlider.addChangeListener(sliderListener);
    rightPanel.add(rotaceXSlider, cc);
    cc.gridy++;

    rotaceYLabel = new JLabel();
    rightPanel.add(rotaceYLabel, cc);
    cc.gridy++;

    rotaceYSlider = new JSlider(-180, 180, 0);
    rotaceYSlider.addChangeListener(sliderListener);
    rightPanel.add(rotaceYSlider, cc);
    cc.gridy++;

    rotaceZLabel = new JLabel();
    rightPanel.add(rotaceZLabel, cc);
    cc.gridy++;

    rotaceZSlider = new JSlider(-180, 180, 0);
    rotaceZSlider.addChangeListener(sliderListener);
    rightPanel.add(rotaceZSlider, cc);
    cc.gridy++;
		
    lightingChBox=new JCheckBox(new 
                                /**
                                 * Class for easy reaction to checkbox event
                                 * Třída pro jendoduché zpracování akce na checkboxu 
                                 * @author Tomáš Hráský
                                 */	
                                AbstractAction("Show Bézier plates"){

        public void actionPerformed(ActionEvent e) {
          updateGLCanvas();				
        }
			
      });
    lightingChBox.setSelected(false);
		
    rightPanel.add(lightingChBox,cc);
		
    cc.gridy++;

    updateRotationLabels();
		
    cc.weighty = 1;
    rightPanel.add(new JPanel(), cc);
    cc.weighty = 0;
    cc.gridwidth = 1;

    cc.gridy++;
    rightPanel.add(new JLabel("X"), cc);
    cc.gridy++;
    rightPanel.add(new JLabel("Y"), cc);
    cc.gridy++;
    rightPanel.add(new JLabel("Z"), cc);
    cc.gridy++;
    rightPanel.add(new JLabel("W"), cc);

    cc.gridx = 1;
    cc.gridy -= 3;
    rightPanel.add(xSpinner, cc);
    cc.gridy++;
    rightPanel.add(ySpinner, cc);
    cc.gridy++;
    rightPanel.add(zSpinner, cc);
    cc.gridy++;
    rightPanel.add(wSpinner, cc);

    xSpinner.setEnabled(false);
    ySpinner.setEnabled(false);
    zSpinner.setEnabled(false);
    wSpinner.setEnabled(false);

    c.weightx = 0;
    c.weighty = 0;
    getContentPane().add(rightPanel, c);

    c.gridx = 0;
    c.gridy++;

    knotSlider = new JKnotSlider(Surface.getInstance().getKnotsU());
    knotSlider.addActionListener(this);
    knotSlider.setName(U_KNOTSLIDER);
    getContentPane().add(knotSlider, c);

    c.gridy++;
    knotSlider2 = new JKnotSlider(Surface.getInstance().getKnotsU());
    knotSlider2.addActionListener(this);
    knotSlider2.setName(V_KNOTSLIDER);
    getContentPane().add(knotSlider2, c);

    pack();
    invalidate();
    setVisible(true);
  }

  /**
   * Method for running application
   * Metoda pro spuštění aplikace
   * 
   * @param args
   * 	no arguments from command line
   * 
   */
  public static void main(String[] args) {
    new SurfaceApp();

  }

  /**
   * Reaction to reqest for canvas redraw - redraws canvas and sets coords of actually selected control point to editing components
   * Reakce na požadavek překreslení OpenGL plátna, překreslí plátno a nastaví
   * souřadnice aktuálního vybraného bodu do editačních komponent
   */
  public void updateGLCanvas() {
    glCanvas.repaint();
    if (Surface.getInstance().getBodIndex() >= 0) {
      xSpinner.setEnabled(true);
      ySpinner.setEnabled(true);
      zSpinner.setEnabled(true);
      wSpinner.setEnabled(true);

      xSpinner.setValue(Double.valueOf(Math.round(Surface.getInstance()
                                                  .getActiveX())));
      ySpinner.setValue(Double.valueOf(Math.round(Surface.getInstance()
                                                  .getActiveY())));
      zSpinner.setValue(Double.valueOf(Math.round(Surface.getInstance()
                                                  .getActiveZ())));
      wSpinner.setValue(Double
                        .valueOf(Surface.getInstance().getActiveW()));
    } else {
      xSpinner.setEnabled(false);
      ySpinner.setEnabled(false);
      zSpinner.setEnabled(false);
      wSpinner.setEnabled(false);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JKnotSlider) {
      JKnotSlider src = (JKnotSlider) e.getSource();
      if (src.getName().equals(U_KNOTSLIDER)) {
        if(src.checkKnotMulti(Surface.getInstance().getOrderU())){
          Surface.getInstance().setKnotsU(src.getKnotsFloat());
        }else{
          JOptionPane.showMessageDialog(this,"Maximum knot multiplicity exceeded","Error",JOptionPane.ERROR_MESSAGE);
          src.setKnots(Surface.getInstance().getKnotsU());
        }

      } else {
        if(src.checkKnotMulti(Surface.getInstance().getOrderV())){
          Surface.getInstance().setKnotsV(src.getKnotsFloat());
        }else{
          JOptionPane.showMessageDialog(this,"Maximum knot multiplicity exceeded","Error",JOptionPane.ERROR_MESSAGE);
          //JOptionPane.showMessageDialog(this,"Překročení maximální násobnosti uzlu","Chyba",JOptionPane.ERROR_MESSAGE);
          src.setKnots(Surface.getInstance().getKnotsV());
        }
      }
      updateGLCanvas();
    }
  }

  /**
   * Returns OpenGL canvas
   * Vrací OpenGL plátno
   * 
   * @return OpenGL canvas
   */
  public GLCanvas getGlCanvas() {
    return glCanvas;
  }

  /**
   * Returns mouse listener
   * Vrací listener událostí myši
   * 
   * @return mouse listener
   */
  public SurfaceMouseListener getMouseListener() {
    return mouseListener;
  }

  /**
   * Creates NURBS surface with clamped knotvectors
   * Vytvoří NURBS plochu s okrajovými uzlovými vektory
   */
  public void uzavernyKV() {
    int stupen;
    int pocetBodu;
    boolean isOK=true;
    float[] newKnots = null,newKnotsV = null;
		
    stupen= Surface.getInstance().getOrderU();
    pocetBodu= Surface.getInstance().getPointsInU();
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount - 2 * stupen;
      newKnots = new float[knotCount];
      int i;
      int j = 0;
      float middleStep = 1f / (middlePartSize + 2);
      float knot = middleStep;

      // knot=.5f;

      for (i = 0; i < stupen; i++)
        newKnots[j++] = 0;
      for (i = 0; i < middlePartSize; i++) {
        newKnots[j++] = knot;
        knot += middleStep;
      }
      for (i = 0; i < stupen; i++)
        newKnots[j++] = 1;

			

    } else{
      isOK=false;
      //errorMessage("Malý počet řídících bodů ve směru paramteru U vzhledem k zadanému stupni plochy");
      errorMessage("Too few control points as of U degree");
    }
		
    stupen= Surface.getInstance().getOrderV();
    pocetBodu= Surface.getInstance().getPointsInV();
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount - 2 * stupen;
      newKnotsV = new float[knotCount];
      int i;
      int j = 0;
      float middleStep = 1f / (middlePartSize + 2);
      float knot = middleStep;

      // knot=.5f;

      for (i = 0; i < stupen; i++)
        newKnotsV[j++] = 0;
      for (i = 0; i < middlePartSize; i++) {
        newKnotsV[j++] = knot;
        knot += middleStep;
      }
      for (i = 0; i < stupen; i++)
        newKnotsV[j++] = 1;

			

    } else{
      isOK=false;
      //errorMessage("Malý počet řídících bodů ve směru paramteru V vzhledem k zadanému stupni plochy");
      errorMessage("Too few control points as of V degree");
    }
		
    if(isOK)
      postNewKnot(newKnots,newKnotsV);
  }

  /**
   * Shows modal window with error report
   * Zobrazí modální okno s hlášením chyby
   * 
   * @param error
   * 	error report
   */

  public void errorMessage(String error) {
    JOptionPane.showMessageDialog(this, error, "Error!",
                                  JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Creates NURBS surface with uniform knotvectors
   * Vytvoří NURBS plochu s uniformními uzlovými vektory
   */
  public void otevrenyKV() {
    int stupen,pocetBodu;
		
    boolean isOK=true;
    float[] newKnots = null,newKnotsV = null;

		
    stupen = Surface.getInstance().getOrderU();
    pocetBodu = Surface.getInstance().getPointsInU();
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount;
      newKnots = new float[knotCount];
      int i;
      int j = 0;
      float middleStep = 1f / (middlePartSize - 1);
      float knot = 0;

      for (i = 0; i < middlePartSize; i++) {
        newKnots[j++] = knot;
        knot += middleStep;
      }

			
    } else{
      isOK=false;
      //errorMessage("Malý počet řídících bodů ve směru parametru U vzhledem k zadanému stupni plochy");
      errorMessage("Too few control points as of U degree");
    }
		
    stupen = Surface.getInstance().getOrderV();
    pocetBodu = Surface.getInstance().getPointsInV();
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount;
      newKnotsV = new float[knotCount];
      int i;
      int j = 0;
      float middleStep = 1f / (middlePartSize - 1);
      float knot = 0;

      for (i = 0; i < middlePartSize; i++) {
        newKnotsV[j++] = knot;
        knot += middleStep;
      }

					
    } else{
      isOK=false;
      //errorMessage("Malý počet řídících bodů ve směru parametru V vzhledem k zadanému stupni plochy");
      errorMessage("Too few control points as of V degree");
    }
		
    if(isOK)
      postNewKnot(newKnots,newKnotsV);	

  }
	

  /**
   * Method called after adding new knot
   * Metoda volaná po přidání nového uzlu
   * @param newKnots new U knotvector
   * @param newKnotsV new V knotvector
   */
  private void postNewKnot(float[] newKnots,float[] newKnotsV) {
    Surface.getInstance().setKnotsU(newKnots);
    Surface.getInstance().setKnotsV(newKnotsV);
    knotSlider.setKnots(newKnots);
    knotSlider2.setKnots(newKnotsV);
    Surface.getInstance().setIsSurfaceFinished(true);
    updateGLCanvas();
    moveTB.setSelected(true);
  }

  /**
   * Activates move mode button
   * Aktivuje tlačítko módu pohybu řícími body
   */
  public void selectMoveButt() {
    moveTB.setSelected(true);
  }

  /**
   * Sets datasource for editation of knotvectors from surface definition object
   * Nastaví zdroje dat komponent pro editaci uzlových vektorů podle uz.
   * vektorů v objektu plochy
   */
  public void updateJKnotSlider() {
    knotSlider.setKnots(Surface.getInstance().getKnotsU());
    knotSlider2.setKnots(Surface.getInstance().getKnotsV());
  }

  /**
   * Returns value of X axe rotation set in editing component
   * Vrací hodnotu rotace kolem osy X nastavenou v editační komponentě 
   * @return X rotation
   */
  public float getXrotation() {
    return rotaceXSlider.getValue();
  }
	
  /**
   * Returns value of Y axe rotation set in editing component
   * Vrací hodnotu rotace kolem osy Y nastavenou v editační komponentě 
   * @return Y rotation
   */
  public float getYrotation() {
    return rotaceYSlider.getValue();
  }
	
  /**
   * Returns value of Z axe rotation set in editing component
   * Vrací hodnotu rotace kolem osy Z nastavenou v editační komponentě 
   * @return Z rotation
   */
  public float getZrotation() {
    return rotaceZSlider.getValue();
  }
	
  /**
   * Updates labels's text according to their actual state
   * Upraví text popisků prvků pro ovládání rotace podle jejich aktuálního stavu
   */
  public void updateRotationLabels(){
    String zakladniText = "Rotation by axe ";
		
    PrintfFormat format=new PrintfFormat("%0.3d");
    String add;
    if(rotaceXSlider.getValue()<0)add="-";
    else add="+";
    rotaceXLabel.setText(zakladniText+"X "+add+format.sprintf(Math.abs(rotaceXSlider.getValue()))+"˚");
    if(rotaceYSlider.getValue()<0)add="-";
    else add="+";
    rotaceYLabel.setText(zakladniText+"Y "+add+format.sprintf(Math.abs(rotaceYSlider.getValue()))+"˚");
    if(rotaceZSlider.getValue()<0)add="-";
    else add="+";	
    rotaceZLabel.setText(zakladniText+"Z "+add+format.sprintf(Math.abs(rotaceZSlider.getValue()))+"˚");
  }

  /**
   * Return OpenGL canvas listener
   * Vrací listener OpenGL plátna
   * @return OpenGL canvas listener
   */
  public GLListener getGlListener() {
    return glListener;
  }

  /**
   * Notifies about reqest to light surface
   * Informuje o požadavku na nasvětlení tělesa
   * @return true if lighting is enabled
   */
  public boolean isLightingEnabled() {
    return !lightingChBox.isSelected();
  }
}
