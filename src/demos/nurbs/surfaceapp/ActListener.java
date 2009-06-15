package demos.nurbs.surfaceapp;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import demos.nurbs.icons.*;

/**
 * Class reacting to events occuring on toolbar and menu
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
  private SurfaceApp app;
  /**
   * FIle chooser object
   * Objekt pro výběr souboru
   */
  private JFileChooser fc;

  /**
   * Creates new instance with link to parent window
   * Vytvoří instanci objektu s odkazem na rodičovské okno
   * @param app parent window
   */
  public ActListener(SurfaceApp app) {
    this.app=app;
    fc=new JFileChooser("./");
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if(e.getActionCommand()==SurfaceApp.PRIDAT_AC_RADEK){
      if(Surface.getInstance().getPointsInU()>=2){
        Surface surface=Surface.getInstance();
        Vector<Float> lastRow=new Vector<Float>();
        int i;
        for(i=surface.getCtrlPoints().length-1;i>=surface.getCtrlPoints().length-surface.getPointsInV()*4;i--){
          lastRow.add(surface.getCtrlPoints()[i]);
        }
				
        Collections.reverse(lastRow);
				
        for(int j=0;j<lastRow.size();j+=4){
          lastRow.set(j,lastRow.get(j)/lastRow.get(j+3));
          lastRow.set(j+1,lastRow.get(j+1)/lastRow.get(j+3));
          lastRow.set(j+2,lastRow.get(j+2)/lastRow.get(j+3));
        }
				
				
        Vector<Float> prevLastRow=new Vector<Float>();
        for(;i>=surface.getCtrlPoints().length-2*surface.getPointsInV()*4;i--){
          prevLastRow.add(surface.getCtrlPoints()[i]);
        }
        Collections.reverse(prevLastRow);

        for(int j=0;j<prevLastRow.size();j+=4){
          prevLastRow.set(j,prevLastRow.get(j)/prevLastRow.get(j+3));
          prevLastRow.set(j+1,prevLastRow.get(j+1)/prevLastRow.get(j+3));
          prevLastRow.set(j+2,prevLastRow.get(j+2)/prevLastRow.get(j+3));
        }
				
				
        Vector<Float> diffs=new Vector<Float>();
        for(i=0;i<prevLastRow.size();i++){
          if((i+1)%4==0)
            diffs.add(0f);
          else
            diffs.add(lastRow.get(i)-prevLastRow.get(i));
        }
				
				
        //TODO ošetřit speciální případy (0 nebo 1 řada bodů)
        //TODO react to special cases - 0 or 1 row of control points
        Vector<Float> newCtrls=new Vector<Float>();
        i=0;
				
        for(float f:surface.getCtrlPoints()){
          newCtrls.add(f);
        }
        //				newCtrls.addAll(lastRow);
        for(i=0;i<lastRow.size();i++){
          newCtrls.add(lastRow.get(i)+diffs.get(i));
        }
				
				
        float[] newCtrlArr=new float[newCtrls.size()];
        i=0;
				
        for(float f:newCtrls)
          newCtrlArr[i++]=f;
				
        surface.setIsSurfaceFinished(false);
        surface.setPointsInU(surface.getPointsInU()+1);
        surface.setCtrlPoints(newCtrlArr);
      }else{
        //TODO informaci o tom že to lze jen při dvou řádcích, ano ne dialog, pokud ano, tak zavolá akci nové plochy
        //TODO inform that this can be done only with two row -> yes/no dialog -> if yes->new surface action
        //int retval=JOptionPane.showOptionDialog(null,"Malý počet bodů pro vytvoření nového řádku. Přejete si definovat novou plochu?","Definovat novou plochu?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,JOptionPane.YES_OPTION);
        int retval=JOptionPane.showOptionDialog(null,"Not enough points for newe row. Would you like to define new surface?","Define new surface?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,JOptionPane.YES_OPTION);
        if(retval==JOptionPane.YES_OPTION)
          actionPerformed(new ActionEvent(this,AWTEvent.RESERVED_ID_MAX+1,SurfaceApp.NOVA_AC));
      }
    }else if(e.getActionCommand()==SurfaceApp.PRIDAT_AC_SLOUPEC){
      if(Surface.getInstance().getPointsInV()>=2){
        Surface srf = Surface.getInstance();
        Vector<Float> leftCol=new Vector<Float>();
        for(int i=0;i<srf.getCtrlPoints().length;i+=srf.getPointsInV()*4){
          leftCol.add(srf.getCtrlPoints()[i]);
          leftCol.add(srf.getCtrlPoints()[i+1]);
          leftCol.add(srf.getCtrlPoints()[i+2]);
          leftCol.add(srf.getCtrlPoints()[i+3]);
        }
        Vector<Float> nextCol=new Vector<Float>();
        for(int i=4;i<srf.getCtrlPoints().length;i+=srf.getPointsInV()*4){
          nextCol.add(srf.getCtrlPoints()[i]);
          nextCol.add(srf.getCtrlPoints()[i+1]);
          nextCol.add(srf.getCtrlPoints()[i+2]);
          nextCol.add(srf.getCtrlPoints()[i+3]);
        }
        //			System.out.println(nextCol);
			
        for(int j=0;j<leftCol.size();j+=4){
          leftCol.set(j,leftCol.get(j)/leftCol.get(j+3));
          leftCol.set(j+1,leftCol.get(j+1)/leftCol.get(j+3));
          leftCol.set(j+2,leftCol.get(j+2)/leftCol.get(j+3));
        }
			
        for(int j=0;j<nextCol.size();j+=4){
          nextCol.set(j,nextCol.get(j)/nextCol.get(j+3));
          nextCol.set(j+1,nextCol.get(j+1)/nextCol.get(j+3));
          nextCol.set(j+2,nextCol.get(j+2)/nextCol.get(j+3));
        }
			
        Vector<Float> diffs=new Vector<Float>();
        for(int i=0;i<nextCol.size();i++){
          if((i+1)%4==0)
            diffs.add(0f);
          else
            diffs.add(leftCol.get(i)-nextCol.get(i));
					
        }
			
        Vector<Float> newCol=new Vector<Float>();
        for(int i=0;i<diffs.size();i++){
          newCol.add(leftCol.get(i)+diffs.get(i));
        }
			
        Vector<float[]> oldPoints=new Vector<float[]>();
        for(int i=0;i<srf.getCtrlPoints().length;i+=4){
          float[] pole={srf.getCtrlPoints()[i],srf.getCtrlPoints()[i+1],srf.getCtrlPoints()[i+2],srf.getCtrlPoints()[i+3]};
          oldPoints.add(pole);
        }
			
			
        int index=0;
			
        Vector<Integer> indexes=new Vector<Integer>();
			
        for(int i=index;i<srf.getCtrlPoints().length/4;i+=srf.getPointsInV()){
          indexes.add(i);
        }
			
        //			System.out.println(indexes);
        int korekce=0;
        for(int i=0;i<oldPoints.size();i++)
          if(indexes.contains(Integer.valueOf(i))){
            oldPoints.add(i+korekce,null);
            //					System.out.println(i+korekce);
            korekce++;
          }
        korekce=0;
        //			for(int i=indexes.size()-1,j=newCol.size()-4;i>=0&&j>=0;i--,j-=4){
        for(int i=0,j=0;i<indexes.size()&&j<newCol.size();i++,j+=4){
          float[] pole={newCol.get(j),newCol.get(j+1),newCol.get(j+2),newCol.get(j+3)};
          oldPoints.set(indexes.get(i)+korekce,pole);
          korekce++;
          //				System.out.println(indexes.get(i)+korekce);
        }
			
        float[] newPoints=new float[oldPoints.size()*4];
        int i=0;
        for(float[] f:oldPoints){
          newPoints[i++]=f[0];
          newPoints[i++]=f[1];
          newPoints[i++]=f[2];
          newPoints[i++]=f[3];
        }
        srf.setIsSurfaceFinished(false);
        srf.setPointsInV(srf.getPointsInV()+1);
        srf.setBodIndex(-1);
        srf.setCtrlPoints(newPoints);
      }else{
        //int retval=JOptionPane.showOptionDialog(null,"Malý počet bodů pro vytvoření nového sloupce. Přejete si definovat novou plochu?","Definovat novou plochu?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,JOptionPane.YES_OPTION);
        int retval=JOptionPane.showOptionDialog(null,"Not enough points for new column. Would you like to define new surface?","Define new surface?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,JOptionPane.YES_OPTION);
        if(retval==JOptionPane.YES_OPTION)
          actionPerformed(new ActionEvent(this,AWTEvent.RESERVED_ID_MAX+1,SurfaceApp.NOVA_AC));
      }
    }else if(e.getActionCommand()==SurfaceApp.SMAZAT_AC_RADEK){
			
    }else if(e.getActionCommand()==SurfaceApp.SMAZAT_AC_SLOUPEC){
			
    }else if(e.getActionCommand()==SurfaceApp.UZAVRENY_AC){
      app.uzavernyKV();
    }else if(e.getActionCommand()==SurfaceApp.OTEVRENY_AC){
      app.otevrenyKV();
    }else if(e.getActionCommand()==SurfaceApp.ULOZIT_AC){
      if(fc.showSaveDialog(app)==JFileChooser.APPROVE_OPTION){
        Surface.getInstance().persist(fc.getSelectedFile());
      }
    }else if(e.getActionCommand()==SurfaceApp.NACIST_AC){
      if(fc.showOpenDialog(app)==JFileChooser.APPROVE_OPTION){
        try{
          Surface.getInstance().unPersist(fc.getSelectedFile());
          app.updateGLCanvas();
          app.selectMoveButt();
          app.updateJKnotSlider();
        }catch(Exception e1){
          //JOptionPane.showMessageDialog(app,"Chyba při načítání ze souboru","Chyba",JOptionPane.ERROR_MESSAGE);
          JOptionPane.showMessageDialog(app,"Error reading file","Error",JOptionPane.ERROR_MESSAGE);
        }
      }
    }else if(e.getActionCommand()==SurfaceApp.NOVA_AC){
      Surface.getInstance().clear();
      app.getMouseListener().setBodIndex(-1);
      Surface.getInstance().setBodIndex(-1);
      app.updateGLCanvas();
      app.updateJKnotSlider();
			
      String retval2=null,retval=null;
      //retval=JOptionPane.showInputDialog(null,"Zadejte počet bodů ve směru paramteru U (řádků)",new Integer(4));
      retval=JOptionPane.showInputDialog(null,"Number of control points in U direction (rows)",new Integer(4));
      if(retval!=null)
        //retval2=JOptionPane.showInputDialog(null,"Zadejte počet bodů ve směru paramteru V (sloupců)",new Integer(4));
        retval2=JOptionPane.showInputDialog(null,"Number of control points in V direction (columns)",new Integer(4));
      if(retval!=null&&retval2!=null){
        try{
          int radku=(new Integer(retval)).intValue();
          int sloupcu=(new Integer(retval2)).intValue();
					 
          Surface.getInstance().setPointsInU(radku);
          Surface.getInstance().setPointsInV(sloupcu);
					 
          int krokX=600/sloupcu;
          int krokZ=-600/radku;
					 
          Vector<Float> souradnice=new Vector<Float>();
          float x = 0,z = 0;
          for(int i=0;i<radku;i++){
            z=i*krokZ;
            for(int j=0;j<sloupcu;j++){
              x=j*krokX;
              souradnice.add(x);
              souradnice.add(0f);//Y
              souradnice.add(z);
              souradnice.add(1f);//W
            }
          }
					 
          float[] ctrlpoints=new float[souradnice.size()];
          int i=0;
          for(Float d:souradnice)
            ctrlpoints[i++]=d.floatValue();
					 
          Surface.getInstance().setCtrlPoints(ctrlpoints);
					 
					 
        }catch(NumberFormatException ex){
          //JOptionPane.showMessageDialog(null,"Chybný formát přirozeného čísla","Chyba!",JOptionPane.ERROR_MESSAGE);
          JOptionPane.showMessageDialog(null,"Wrong natural number format","Error!",JOptionPane.ERROR_MESSAGE);
        }
      }
			
      app.updateGLCanvas();
			
    }else if(e.getActionCommand()==SurfaceApp.EXIT_AC){
      //TODO dotaz na uložení ??
      System.exit(0);
    }else if(e.getActionCommand()==SurfaceApp.STUPEN_AC){
      try{
        String retval2=null;
        //String retval=JOptionPane.showInputDialog(null,"Zadejte stupeň křivky ve směru parametru U",new Integer(Surface.getInstance().getOrderU()));
        String retval=JOptionPane.showInputDialog(null,"Degree in U direction",new Integer(Surface.getInstance().getOrderU()));
        if(retval!=null) 
          retval2=JOptionPane.showInputDialog(null,"Degree in V direction",new Integer(Surface.getInstance().getOrderV()));
        if(retval!=null&&retval2!=null){
          int stupen=(new Integer(retval)).intValue();
          int stupenV=(new Integer(retval2)).intValue();
          Surface.getInstance().setOrderU(stupen);
          Surface.getInstance().setOrderV(stupenV);
          Surface.getInstance().setIsSurfaceFinished(false);
        }
      }catch (NumberFormatException ex){
        JOptionPane.showMessageDialog(null,"Wrong nutural number format","Error!",JOptionPane.ERROR_MESSAGE);
      }
    }else if(e.getActionCommand()==SurfaceApp.INFO_AC){
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
