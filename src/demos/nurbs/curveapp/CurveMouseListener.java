package demos.nurbs.curveapp;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Class reacting to mouse events (implements interface for button press, drag and movement)
 * Třída zpracovávající události myši (implementuje rozhraní zpracovávající stisk tlačítek i pohyb a tažení myší)
 * @author Tomáš Hráský
 *
 */
public class CurveMouseListener implements MouseListener, MouseMotionListener {
  /**
   * Actually selected control point index
   * Index aktuálně vybraného řídícího bodu
   */
  private int bodIndex;

  /**
   * Window listener is connected to
   * Okno k nemuž listener patří
   */
  private CurveApp appWindow;

  /**
   * Action type
   * Typ prováděné činnosti
   */
  private String actionType;

  /**
   * Pixel tolerance when selecting control point by clicking
   * Tolerance pro indikaci kliku na řídící bod
   */
  private static final int TOLERANCE=10;

  /**
   * Creates new listener with connection to given app window
   * Vytvoří listener s odkazem na zadané okno
   * @param app rodičovské okno
   */
  public CurveMouseListener(CurveApp app) {
    this.appWindow=app;
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  public void mouseClicked(MouseEvent e) {
    if(actionType==CurveApp.PRIDAT_AC){
      Curve.getInstance().setIsCurveFinished(false);
      float x=e.getX();
      float y=e.getY();
      float z=0;
      float w=1;
      int size;
      float[] newCtrls;
      try{
        size=Curve.getInstance().getCtrlPoints().length;
      }catch (Exception ex) {
        size=0;
      }
      newCtrls=new float[size+4];
      System.arraycopy(Curve.getInstance().getCtrlPoints(),0,newCtrls,0,size);
			
      newCtrls[size]=x;
      newCtrls[size+1]=y;
      newCtrls[size+2]=z;
      newCtrls[size+3]=w;
      Curve.getInstance().setCtrlPoints(newCtrls);
    }else if(actionType==CurveApp.SMAZAT_AC&&bodIndex>=0){
      Curve.getInstance().setIsCurveFinished(false);
      int size=Curve.getInstance().getCtrlPoints().length;
      float[] newCtrls=new float[size-4];
			
      int firstPartSize=(bodIndex)*4;
      int secondPartSize=newCtrls.length-firstPartSize;
      System.arraycopy(Curve.getInstance().getCtrlPoints(),0,newCtrls,0,firstPartSize);
      System.arraycopy(Curve.getInstance().getCtrlPoints(),firstPartSize+4,newCtrls,firstPartSize,secondPartSize);
      bodIndex=-1;
      Curve.getInstance().setBodIndex(bodIndex);
      Curve.getInstance().setCtrlPoints(newCtrls);
    }
    appWindow.updateGLCanvas();
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  public void mousePressed(MouseEvent e) {
    //		if(actionType==MOVE_AC){
    float[] ctrlpoints=Curve.getInstance().getCtrlPoints();
    int x=e.getX();
    int y=e.getY();
    this.bodIndex=-1;
    //			System.out.println(ctrlpoints.length);
    for(int i=0;i<ctrlpoints.length/4;i++){
      float xS = ctrlpoints[i*4]/ctrlpoints[i*4+3];
      float yS = ctrlpoints[i*4+1]/ctrlpoints[i*4+3];
      if(x>=xS-TOLERANCE&&x<=xS+TOLERANCE&&y>=yS-TOLERANCE&&y<=yS+TOLERANCE){
        this.bodIndex=i;
      }                    
    }
			
    Curve.getInstance().setBodIndex(bodIndex);
    //		}
    appWindow.updateGLCanvas();
		
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
   */
  public void mouseReleased(MouseEvent e) {
    //		this.bodIndex=-1;
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
   */
  public void mouseEntered(MouseEvent e) {

  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
   */
  public void mouseExited(MouseEvent e) {

  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
   */
  public void mouseDragged(MouseEvent e) {
    if(this.bodIndex>=0){
      int x=e.getX();
      int y=e.getY();
			
      Curve.getInstance().setActiveX(x);
      Curve.getInstance().setActiveY(y);
    }
    appWindow.updateGLCanvas();
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
   */
  public void mouseMoved(MouseEvent e) {
		
  }

  /**
   * Set action type
   * Nastaví typ prováděné činnosti
   * @param action Action type
   */
  public void setActionType(String action) {
    this.actionType=action;
  }

  /**
   * Returns actually selected control point index
   * Vrací index aktuálně vybraného řídícího bodu
   * @return actually selected control point index
   */
  public int getBodIndex() {
    return bodIndex;
  }

  /**
   * Sets actually selected control point index
   * Nastavuje index aktuálně vybraného řídícího bodu
   * @param bodIndex actually selected control point index
   */
  public void setBodIndex(int bodIndex) {
    this.bodIndex = bodIndex;
  }
}
