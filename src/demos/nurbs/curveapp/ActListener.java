package demos.nurbs.curveapp;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import demos.nurbs.icons.*;

/**
 * Class reacting to events from toolbar and menu
 * Třída reagující na události z nástrojové lišty a menu
 * @author Tomáš Hráský
 *
 */
@SuppressWarnings("serial")
public class ActListener extends AbstractAction
{

  /**
   * Parent window
   * Odkaz na rodičovské okno
   */
  private CurveApp app;
  /**
   * File chooser object
   * Objekt pro výběr souboru
   */
  private JFileChooser fc;

  /**
   * Creates instance of object with pointer to parent window
   * Vytvoří instanci objektu s odkazem na rodičovské okno
   * @param app parent window
   */
  public ActListener(CurveApp app) {
    this.app=app;
    fc=new JFileChooser("./");
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
		
		
    if(e.getActionCommand()==CurveApp.PRIDAT_AC){
				
    }else if(e.getActionCommand()==CurveApp.SMAZAT_AC){
			
    }else if(e.getActionCommand()==CurveApp.UZAVRENY_AC){
      app.uzavernyKV();
    }else if(e.getActionCommand()==CurveApp.OTEVRENY_AC){
      app.otevrenyKV();
    }else if(e.getActionCommand()==CurveApp.ULOZIT_AC){
      if(fc.showSaveDialog(app)==JFileChooser.APPROVE_OPTION){
        Curve.getInstance().persist(fc.getSelectedFile());
      }
    }else if(e.getActionCommand()==CurveApp.NACIST_AC){
      if(fc.showOpenDialog(app)==JFileChooser.APPROVE_OPTION){
        try {
          Curve.getInstance().unPersist(fc.getSelectedFile());
          app.updateGLCanvas();
          app.selectMoveButt();
          app.updateJKnotSlider();
        } catch (Exception e1) {
          //JOptionPane.showMessageDialog(app,"Chyba při načítání ze souboru","Chyba",JOptionPane.ERROR_MESSAGE);
          JOptionPane.showMessageDialog(app,"Error loading file","Error",JOptionPane.ERROR_MESSAGE);
        }
      }
    }else if(e.getActionCommand()==CurveApp.NOVA_AC){
      Curve.getInstance().clear();
      app.getMouseListener().setBodIndex(-1);
      Curve.getInstance().setBodIndex(-1);
      app.updateGLCanvas();
      app.updateJKnotSlider();
    }else if(e.getActionCommand()==CurveApp.EXIT_AC){
      //TODO exit confirmation ?
      System.exit(0);
    }else if(e.getActionCommand()==CurveApp.STUPEN_AC){
      try{
        //String retval = JOptionPane.showInputDialog(null,"Zadejte stupeň křivky",new Integer(Curve.getInstance().getOrder()));
        String retval = JOptionPane.showInputDialog(null,"Curve degree",new Integer(Curve.getInstance().getOrder()));
        if(retval!=null){
          int stupen=(new Integer(retval)).intValue();
          Curve.getInstance().setOrder(stupen);
          Curve.getInstance().setIsCurveFinished(false);
        }
      }catch (NumberFormatException ex){
        //JOptionPane.showMessageDialog(null,"Chybný formát přirozeného čísla","Chyba!",JOptionPane.ERROR_MESSAGE);
        JOptionPane.showMessageDialog(null,"Wrong natural number format","Error!",JOptionPane.ERROR_MESSAGE);
      }
    }else if(e.getActionCommand()==CurveApp.INFO_AC){
      /*
        JOptionPane.showMessageDialog(null,"Ukázková aplikace rozšířené funkcionality knihovny JOGL\n" +
        "Autor: Tomáš Hráský\n" +
        "Součást bakalářské práce na téma Softwarová implementace NURBS křivek a ploch\n" +
        "2007 Fakulta Informatiky a Managementu UHK\n" +
        "Pro serializaci objektů využívá open source framework Simple XML - http://simple.sourceforge.net/","O aplikaci",JOptionPane.INFORMATION_MESSAGE,IconFactory.getIcon("demos/nurbs/icons/info.png"));
      */
      JOptionPane.showMessageDialog(null,"Example aplication of extended functionality JOGL library\n" +
                                    "Author: Tomáš Hráský\n" +
                                    "Part of bachelor's degree thesis Software implementation of NURBS curves and surfaces\n" +
                                    "2007 Faculty of Informatics and Management University of Hradec Králové\n" +
                                    "Simple XML framework is used for object serialization - http://simple.sourceforge.net/","About",JOptionPane.INFORMATION_MESSAGE,IconFactory.getIcon("demos/nurbs/icons/info.png"));
    }
		
    app.getMouseListener().setActionType(e.getActionCommand());
		
		
  }
}
