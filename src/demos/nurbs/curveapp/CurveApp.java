package demos.nurbs.curveapp;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLCanvas;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.ToolTipManager;

import demos.nurbs.icons.*;
import demos.nurbs.knotslidercomponent.JKnotSlider;

/**
 * Main class of application demostrating capabilities of JOGL when working with NURBS curves
 * Hlavní třída aplikace demonstrující shopnosti knihovny JOGL při práci s NURBS křivkami
 * @author Tomáš Hráský
 *
 */
@SuppressWarnings("serial")
public class CurveApp extends JFrame implements ActionListener
{

  /**
   * Name of X-coord editing component of actually selected control point
   * Jméno komponenty pro editaci X-ové souřadnice aktuálního bodu
   */
  public static final String X_SPINNER_NAME = "xspinner";
  /**
   * Name of Y-coord editing component of actually selected control point
   * Jméno komponenty pro editaci Y-ové souřadnice aktuálního bodu
   */
  public static final String Y_SPINNER_NAME = "yspinner";
  /**
   * Name of weight editing component of actually selected control point
   * Jméno komponenty pro editaci váhy aktuálního bodu
   */
  public static final String W_SPINNER_NAME = "wspinner";

  /**
   * Name of ADD CONTROL POINT event
   * Jméno události přidání řídícího bodu
   */
  public static final String PRIDAT_AC = "PRIDAT";
	
  /**
   * Name of SET CURVE DEGREE event
   * Jméno události zadání stupně křivky
   */
  public static final String STUPEN_AC="STUPEN";
  /**
   * Name of DELETE CONTROL POINT event
   * Jméno události smazání řídícího bodu
   */
  public static final String SMAZAT_AC = "SMAZAT";
  /**
   * Name of MAKE CLOSED KNOTVECTOR event
   * Jméno události vytvoření uzavřeného uzlového vektoru
   */
  public static final String UZAVRENY_AC = "UZAVRENY";
  /**
   * Name of MAKE OPEN (UNIFORM) KNOTVECTOR event
   * Jméno události vytvoření otevřeného (uniformního) uzlového vektoru
   */
  public static final String OTEVRENY_AC = "OTEVRENY";
  /**
   * Name of SAVE CURVE event
   * Jméno události uložení křivky
   */
  public static final String ULOZIT_AC = "ULOZIT";
  /**
   * Name of LOAD CURVE event
   * Jméno události načetení uložené definice křivky
   */
  public static final String NACIST_AC = "NACIST";
  /**
   * Name of MOVE CONTROL POINT event
   * Jméno události pohybu řídícího bodu
   */
  private static final String MOVE_AC = "MOVE";

  /**
   * Name of CREATE NEW CURVE event
   * Jméno události vytvoření nové křivky
   */
  static final String NOVA_AC = "NEWCURVE";

  /**
   * Name of EXIT APP event
   * Jméno události ukončení aplikace
   */
  public static final String EXIT_AC = "EXIT";
  /**
   * Name of SHOW ABOUT event
   * Jméno události zobrazení okna o aplikaci
   */
  public static final String INFO_AC = "INFO";

  /**
   * OpenGL canvas
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
   * Mouse events listener
   * Listener událostí myši
   */
  private CurveMouseListener mouseListener;

  /**
   * Knot vector editing component
   * Komponenta pro editaci uzlového vektoru
   */
  private JKnotSlider knotSlider;

  /**
   * Start "move control point" mode
   * Tlačítko pro zapnutí módu pohybu řídících bodů
   */
  private JToggleButton moveTB;

  /**
   * Constructor, initializes GUI
   * Konstruktor, vytvoří grafické uživatelské rozhraní
   */
  public CurveApp() {
    super("Tomáš Hráský - example application demonstrating GLU NURBS capabilites - JOGL");
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
    glCanvas.addGLEventListener(new GLListener());
    mouseListener = new CurveMouseListener(this);
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

    //JMenu aplikaceMenu = new JMenu("Aplikace");
    JMenu aplikaceMenu = new JMenu("Application");
    menuBar.add(aplikaceMenu);
		
    //JMenuItem aboutMI=new JMenuItem("O aplikaci");
    JMenuItem aboutMI=new JMenuItem("About");
    aboutMI.setActionCommand(INFO_AC);
    aboutMI.addActionListener(listener);
    aplikaceMenu.add(aboutMI);
		
    aplikaceMenu.add(new JSeparator());
		
    //JMenuItem konecMI=new JMenuItem("Ukončit");
    JMenuItem konecMI=new JMenuItem("Exit");
    konecMI.addActionListener(listener);
    konecMI.setActionCommand(EXIT_AC);
    aplikaceMenu.add(konecMI);
		
    //JMenu krivkaMenu = new JMenu("Křivka");
    JMenu krivkaMenu = new JMenu("Curve");
    menuBar.add(krivkaMenu);

    //JMenuItem pridatBodyMI = new JMenuItem("Přidat body");
    JMenuItem pridatBodyMI = new JMenuItem("Add control points");
    krivkaMenu.add(pridatBodyMI);
    pridatBodyMI.addActionListener(listener);
		
		
    pridatBodyMI.setActionCommand(PRIDAT_AC);
    JMenuItem smazatBodyMI = new JMenuItem(
                                           //"Smazat body");
                                           "Delete points");
    krivkaMenu.add(smazatBodyMI);
    smazatBodyMI.addActionListener(listener);
		
    smazatBodyMI.setActionCommand(SMAZAT_AC);
		
    //JMenuItem stupenMI=new JMenuItem("Zadat stupeň křivky");
    JMenuItem stupenMI=new JMenuItem("Set curve degree");
    krivkaMenu.add(stupenMI);
    stupenMI.addActionListener(listener);
    stupenMI.setActionCommand(STUPEN_AC);

    //JMenu knotVecMenu = new JMenu("Vytvořit uzlový vektor");
    JMenu knotVecMenu = new JMenu("Create knot vector");
    krivkaMenu.add(knotVecMenu);

    //JMenuItem clampedKVMI = new JMenuItem("Okrajový");
    JMenuItem clampedKVMI = new JMenuItem("Clamped");
    knotVecMenu.add(clampedKVMI);
    clampedKVMI.setActionCommand(UZAVRENY_AC);
    clampedKVMI.addActionListener(listener);
    //JMenuItem unclampedKVMI = new JMenuItem("Uniformní");
    JMenuItem unclampedKVMI = new JMenuItem("Uniform");
    knotVecMenu.add(unclampedKVMI);
    unclampedKVMI.setActionCommand(OTEVRENY_AC);
    unclampedKVMI.addActionListener(listener);

    //JMenuItem moveMI=new JMenuItem("Hýbat body");
    JMenuItem moveMI=new JMenuItem("Move points");
    krivkaMenu.add(moveMI);
    moveMI.setActionCommand(MOVE_AC);
    moveMI.addActionListener(listener);
		
    krivkaMenu.add(new JSeparator());

    krivkaMenu.add(new JSeparator());

    //JMenuItem novaMI=new JMenuItem("Nová křivka");
    JMenuItem novaMI=new JMenuItem("New curve");
    krivkaMenu.add(novaMI);
    novaMI.setActionCommand(NOVA_AC);
    novaMI.addActionListener(listener);
		
    //JMenuItem ulozitMI = new JMenuItem("Uložit křivku jako...");
    JMenuItem ulozitMI = new JMenuItem("Save curve as...");
    krivkaMenu.add(ulozitMI);
    ulozitMI.setActionCommand(ULOZIT_AC);
    ulozitMI.addActionListener(listener);
    //JMenuItem nacistMI = new JMenuItem("Načíst křivku");
    JMenuItem nacistMI = new JMenuItem("Load curve");
    krivkaMenu.add(nacistMI);
    nacistMI.setActionCommand(NACIST_AC);
    nacistMI.addActionListener(listener);

    c.gridy++;
    JToolBar toolBar = new JToolBar();
    getContentPane().add(toolBar, c);

    ButtonGroup bg = new ButtonGroup();

		
    JButton novaB=new JButton();
    //		novaB.setText("Nová");
    //novaB.setToolTipText("Nová křivka");
    novaB.setToolTipText("New curve");
    novaB.setIcon(IconFactory.getIcon("demos/nurbs/icons/folder_new.png"));
    novaB.setActionCommand(NOVA_AC);
    novaB.addActionListener(listener);
    toolBar.add(novaB);
		
    JButton ulozitB=new JButton();
    ulozitB.setIcon(IconFactory.getIcon("demos/nurbs/icons/adept_sourceseditor.png"));
    //		ulozitB.setText("Uložit");
    //ulozitB.setToolTipText("Uložit");
    ulozitB.setToolTipText("Save");
    ulozitB.setActionCommand(ULOZIT_AC);
    ulozitB.addActionListener(listener);
    toolBar.add(ulozitB);
		
    JButton nahratB=new JButton();
    //		nahratB.setText("Nahrát");
    //nahratB.setToolTipText("Nahrát uloženou křivku");
    nahratB.setToolTipText("Load curve");
    nahratB.setIcon(IconFactory.getIcon("demos/nurbs/icons/fileimport.png"));
    nahratB.setActionCommand(NACIST_AC);
    nahratB.addActionListener(listener);
    toolBar.add(nahratB);
		
    toolBar.add(new JToolBar.Separator());
		
    JToggleButton pridatTB = new JToggleButton();
    //		pridatTB.setText("Přidat body");
    //pridatTB.setToolTipText("Přidat body");
    pridatTB.setToolTipText("Add points");
    toolBar.add(pridatTB);
    pridatTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/add.png"));
    pridatTB.setActionCommand(PRIDAT_AC);
    pridatTB.addActionListener(listener);
    bg.add(pridatTB);
    JToggleButton smazatTB = new JToggleButton();
    //		smazatTB.setText("Smazat body");
    //smazatTB.setToolTipText("Smazat body");
    smazatTB.setToolTipText("Delete points");
    toolBar.add(smazatTB);
    smazatTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/fileclose.png"));
    smazatTB.setActionCommand(SMAZAT_AC);
    smazatTB.addActionListener(listener);
    bg.add(smazatTB);
		
    JToggleButton stupenTB = new JToggleButton();
    //		stupenTB.setText("Smazat body");
    //stupenTB.setToolTipText("Zadat stupeň křivky");
    stupenTB.setToolTipText("Set curve degree");
    toolBar.add(stupenTB);
    stupenTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/math_rsup.png"));
    stupenTB.setActionCommand(STUPEN_AC);
    stupenTB.addActionListener(listener);
    bg.add(stupenTB);
		

    final JPopupMenu popup = new JPopupMenu();

    //JMenuItem uzavrenyPopupMI = new JMenuItem("Okrajový");
    JMenuItem uzavrenyPopupMI = new JMenuItem("Clamped");
    popup.add(uzavrenyPopupMI);
    uzavrenyPopupMI.setActionCommand(UZAVRENY_AC);
    uzavrenyPopupMI.addActionListener(listener);
    //JMenuItem otevrenyPopupMI = new JMenuItem("Uniformní");
    JMenuItem otevrenyPopupMI = new JMenuItem("Uniform");
    popup.add(otevrenyPopupMI);
    otevrenyPopupMI.setActionCommand(OTEVRENY_AC);
    otevrenyPopupMI.addActionListener(listener);

    JToggleButton vytvoritButton = new JToggleButton();
    //		vytvoritButton.setText("Vytvořit uzlový vektor");
    //vytvoritButton.setToolTipText("Vytvořit uzlový vektor");
    vytvoritButton.setToolTipText("Create knot vector");
    vytvoritButton.setIcon(IconFactory.getIcon("demos/nurbs/icons/newfunction.png"));
    bg.add(vytvoritButton);
		
    vytvoritButton.addMouseListener(new 
                                    /**
                                     * @author Tomáš Hráský
                                     * Class connecting context menu to button on toolbar
                                     * Třída pro připojení kontextového menu na tlačítko na liště nástrojů
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
		
    moveTB=new JToggleButton();
    //		moveTB.setText("Hýbat body");
    //moveTB.setToolTipText("Hýbat body");
    moveTB.setToolTipText("Move points");
    moveTB.setIcon(IconFactory.getIcon("demos/nurbs/icons/mouse.png"));
    toolBar.add(moveTB);
    moveTB.setActionCommand(MOVE_AC);
    moveTB.addActionListener(listener);
    bg.add(moveTB);
    toolBar.add(new JToolBar.Separator());
    JButton infoB=new JButton();
    //		infoB.setText("Ukončit");
    //infoB.setToolTipText("O aplikaci");
    infoB.setToolTipText("About");
		
    infoB.setIcon(IconFactory.getIcon("demos/nurbs/icons/info.png"));
    toolBar.add(infoB);
    infoB.setActionCommand(INFO_AC);
    infoB.addActionListener(listener);
    toolBar.add(new JToolBar.Separator());
		
    JButton exitB=new JButton();
    //		exitB.setText("Ukončit");
    //exitB.setToolTipText("Ukončit");
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
    xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000.0, 1));
    ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000.0, 1));
    wSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000.0, .05));

    SpinnerListener spinnerListener = new SpinnerListener(this);
    xSpinner.addChangeListener(spinnerListener);
    xSpinner.setName(X_SPINNER_NAME);
    ySpinner.addChangeListener(spinnerListener);
    ySpinner.setName(Y_SPINNER_NAME);
    wSpinner.addChangeListener(spinnerListener);
    wSpinner.setName(W_SPINNER_NAME);

    cc.gridx = 0;
    cc.gridy = 0;

    cc.gridwidth = 2;
    cc.weighty = 1;
    rightPanel.add(new JPanel(), cc);
    cc.weighty = 0;
    cc.gridwidth = 1;

    cc.gridy++;
    rightPanel.add(new JLabel("X"), cc);
    cc.gridy++;
    rightPanel.add(new JLabel("Y"), cc);
    cc.gridy++;
    rightPanel.add(new JLabel("W"), cc);

    cc.gridx = 1;
    cc.gridy = 1;
    rightPanel.add(xSpinner, cc);
    cc.gridy++;
    rightPanel.add(ySpinner, cc);
    cc.gridy++;
    rightPanel.add(wSpinner, cc);

    xSpinner.setEnabled(false);
    ySpinner.setEnabled(false);
    wSpinner.setEnabled(false);

    c.weightx = 0;
    c.weighty = 0;
    getContentPane().add(rightPanel, c);

    c.gridx = 0;
    c.gridy++;

    knotSlider = new JKnotSlider(Curve.getInstance().getKnots());
    knotSlider.addActionListener(this);
    getContentPane().add(knotSlider, c);

    pack();
    invalidate();
    setVisible(true);
  }

  /**
   * Main method starting application
   * Metoda pro spuštění aplikace
   * @param args no arguments accepted 
   * 
   */
  public static void main(String[] args) {
    new CurveApp();

  }

  /**
   * Reaction to request for redrive OpenGL canvas - repaints canvas, sets actually selected control points coords to editing components
   * Reakce na požadavek překreslení OpenGL plátna, překreslí plátno a nastaví souřadnice aktuálního vybraného bodu do editačních komponent
   */
  public void updateGLCanvas() {
    glCanvas.repaint();
    if (Curve.getInstance().getBodIndex() >= 0) {
      xSpinner.setEnabled(true);
      ySpinner.setEnabled(true);
      wSpinner.setEnabled(true);

      xSpinner.setValue(Double.valueOf(Math.round(Curve.getInstance()
                                                  .getActiveX())));
      ySpinner.setValue(Double.valueOf(Math.round(Curve.getInstance()
                                                  .getActiveY())));
      wSpinner.setValue(Double.valueOf(Curve.getInstance().getActiveW()));
    } else {
      xSpinner.setEnabled(false);
      ySpinner.setEnabled(false);
      wSpinner.setEnabled(false);
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    JKnotSlider src = (JKnotSlider) e.getSource();
    if(src.checkKnotMulti(Curve.getInstance().getOrder())){
      Curve.getInstance().setKnots(src.getKnotsFloat());
    }else{
      //JOptionPane.showMessageDialog(this,"Překročení maximální násobnosti uzlu","Chyba",JOptionPane.ERROR_MESSAGE);
      JOptionPane.showMessageDialog(this,"Maximum knot multiplicity exceeded","Error",JOptionPane.ERROR_MESSAGE);
      src.setKnots(Curve.getInstance().getKnots());
    }
    updateGLCanvas();
  }

  /**
   * Returns OpenGL canvas
   * Vrací OpenGL plátno
   * @return OpenGL canvas
   */
  public GLCanvas getGlCanvas() {
    return glCanvas;
  }

  /**
   * Returns mouse events listener
   * Vrací listener událostí myši
   * @return mouse listener
   */
  public CurveMouseListener getMouseListener() {
    return mouseListener;
  }

  /**
   * Creates NURBS curve with clamped knot vector
   * Vytvoří NURBS křivku s okrajovým uzlovým vektorem
   */
  public void uzavernyKV() {
    int stupen = Curve.getInstance().getOrder();
    int pocetBodu = Curve.getInstance().getCtrlPoints().length / 4;
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount - 2 * stupen;
      float[] newKnots = new float[knotCount];
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

      postNewKnot(newKnots);

    } else
      //errorMessage("Malý počet řídících bodů vzhledem k zadanému stupni křivky");
      errorMessage("Too few control points regarding set curve degree");
  }

  /**
   * Displays modal window with error report
   * Zobrazí modální okno s hlášením chyby
   * @param error error message
   */
  public void errorMessage(String error){
    //JOptionPane.showMessageDialog(this,error,"Chyba!",JOptionPane.ERROR_MESSAGE);
    JOptionPane.showMessageDialog(this,error,"Error!",JOptionPane.ERROR_MESSAGE);
  }
	
  /**
   * Creates NURBS curves with uniform knot vector
   * Vytvoří NURBS křivku s uniformním uzlovým vektorem
   */
  public void otevrenyKV() {
    int stupen = Curve.getInstance().getOrder();
    int pocetBodu = Curve.getInstance().getCtrlPoints().length / 4;
    if (stupen <= pocetBodu) {
      int knotCount = stupen + pocetBodu;
      int middlePartSize = knotCount;
      float[] newKnots = new float[knotCount];
      int i;
      int j = 0;
      float middleStep = 1f / (middlePartSize - 1);
      float knot = 0;

      // knot=.5f;

      // for(i=0;i<stupen;i++)
      // newKnots[j++]=0;
      for (i = 0; i < middlePartSize; i++) {
        newKnots[j++] = knot;
        knot += middleStep;
      }
      // for(i=0;i<stupen;i++)
      // newKnots[j++]=1;

      postNewKnot(newKnots);
    }
    else
      //errorMessage("Malý počet řídících bodů vzhledem k zadanému stupni křivky");
      errorMessage("Too few control points regarding set curve degree");
  }

  /**
   * Method called after adding new knot
   * Metoda volaná po přidání nového uzlu
   * @param newKnots new knot vector
   */
  private void postNewKnot(float[] newKnots) {
    Curve.getInstance().setKnots(newKnots);
    knotSlider.setKnots(newKnots);
    Curve.getInstance().setIsCurveFinished(true);
    updateGLCanvas();
    moveTB.setSelected(true);
  }

  /**
   * Activates MOVE MODE button
   * Aktivuje tlačítko módu pohybu řícími body
   */
  public void selectMoveButt() {
    moveTB.setSelected(true);
  }

  /**
   * Sets data source for knot editing component from knot vector of curve object
   * Nastaví zdroj dat komponenty pro editaci uzlového vektoru podle uz. vektoru v objektu křivky
   */
  public void updateJKnotSlider() {
    knotSlider.setKnots(Curve.getInstance().getKnots());
  }
}
