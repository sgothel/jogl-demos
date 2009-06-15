package demos.nurbs.surfaceapp;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

/**
 * Třída zpracovávající události myši (implementuje rozhraní zpracovávající stisk tlačítek i pohyb a tažení myší)
 * @author Tomáš Hráský
 *
 */
public class SurfaceMouseListener implements MouseListener, MouseMotionListener {
  /**
   * Index aktuálně vybraného řídícího bodu
   */
  private int bodIndex;

  /**
   * Okno k nemuž liustener patří
   */
  private SurfaceApp appWindow;

  /**
   * Typ prováděné činnosti
   */
  private String actionType;

  /**
   * Tolerance pro indikaci kliku na řídící bod
   */
  private static final int TOLERANCE=10;

  /**
   * Vytvoří listener s odkazem na zadané okno
   * @param app rodičovské okno
   */
  public SurfaceMouseListener(SurfaceApp app) {
    this.appWindow=app;
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  public void mouseClicked(MouseEvent e) {
    if(actionType==SurfaceApp.PRIDAT_AC){
      //			Surface.getInstance().setIsSurfaceFinished(false);
      //			float x=e.getX();
      //			float y=e.getY();
      //			float z=0;
      //			float w=1;
      //			int size;
      //			float[] newCtrls;
      //			try{
      //				size=Surface.getInstance().getCtrlPoints().length;
      //			}catch (Exception ex) {
      //				size=0;
      //			}
      //			newCtrls=new float[size+4];
      //			System.arraycopy(Surface.getInstance().getCtrlPoints(),0,newCtrls,0,size);
      //			
      //			newCtrls[size]=x;
      //			newCtrls[size+1]=y;
      //			newCtrls[size+2]=z;
      //			newCtrls[size+3]=w;
      //			Surface.getInstance().setCtrlPoints(newCtrls);
    }else if(actionType==SurfaceApp.SMAZAT_AC&&bodIndex>=0){
      //			Surface.getInstance().setIsSurfaceFinished(false);
      //			int size=Surface.getInstance().getCtrlPoints().length;
      //			float[] newCtrls=new float[size-4];
      //			
      //			int firstPartSize=(bodIndex)*4;
      //			int secondPartSize=newCtrls.length-firstPartSize;
      //			System.arraycopy(Surface.getInstance().getCtrlPoints(),0,newCtrls,0,firstPartSize);
      //			System.arraycopy(Surface.getInstance().getCtrlPoints(),firstPartSize+4,newCtrls,firstPartSize,secondPartSize);
      //			bodIndex=-1;
      //			Surface.getInstance().setBodIndex(bodIndex);
      //			Surface.getInstance().setCtrlPoints(newCtrls);
    }else if(actionType==SurfaceApp.SMAZAT_AC_RADEK&&bodIndex>=0){
			
      Vector<float[]> oldPoints=new Vector<float[]>();
      Surface srf=Surface.getInstance();
      for(int i=0;i<srf.getCtrlPoints().length;i+=4){
        float[] pole={srf.getCtrlPoints()[i],srf.getCtrlPoints()[i+1],srf.getCtrlPoints()[i+2],srf.getCtrlPoints()[i+3]};
        oldPoints.add(pole);
      }
			
      int index=bodIndex+1;
      while(!(index%srf.getPointsInV()==0))
        index--;
      //			index--;
      Vector<Integer> indexes=new Vector<Integer>();
      for(int i=index;i<index+srf.getPointsInV();i++)
        indexes.add(i);
      Vector<float[]> newOldPoints=new Vector<float[]>();
      for(int i=0;i<oldPoints.size();i++){
        if(!indexes.contains(Integer.valueOf(i)))
          newOldPoints.add(oldPoints.get(i));
      }
      //				oldPoints.remove(i);
      float[] newPoints=new float[newOldPoints.size()*4];
      int i=0;
      for(float[] f:newOldPoints){
        newPoints[i++]=f[0];
        newPoints[i++]=f[1];
        newPoints[i++]=f[2];
        newPoints[i++]=f[3];
      }
      srf.setIsSurfaceFinished(false);
      srf.setPointsInU(srf.getPointsInU()-1);
      bodIndex=-1;
      srf.setBodIndex(-1);
      srf.setCtrlPoints(newPoints);
			
    }else if(actionType==SurfaceApp.SMAZAT_AC_SLOUPEC&&bodIndex>=0){
      Vector<float[]> oldPoints=new Vector<float[]>();
      Surface srf=Surface.getInstance();
      for(int i=0;i<srf.getCtrlPoints().length;i+=4){
        float[] pole={srf.getCtrlPoints()[i],srf.getCtrlPoints()[i+1],srf.getCtrlPoints()[i+2],srf.getCtrlPoints()[i+3]};
        oldPoints.add(pole);
      }
			
      int index=bodIndex+1;
			
      Vector<Integer> indexes=new Vector<Integer>();
			
      for(int i=index;i>=0;i-=srf.getPointsInV()){
        indexes.add(i-1);
      }
      for(int i=index;i<srf.getCtrlPoints().length;i+=srf.getPointsInV()){
        indexes.add(i-1);
      }
			
      //			index--;
      //			for(int i=index;i<index+srf.getPointsInV();i++)
      //				
			
      Vector<float[]> newOldPoints=new Vector<float[]>();
      for(int i=0;i<oldPoints.size();i++){
        if(!indexes.contains(Integer.valueOf(i)))
          newOldPoints.add(oldPoints.get(i));
      }
      //				oldPoints.remove(i);
      float[] newPoints=new float[newOldPoints.size()*4];
      int i=0;
      for(float[] f:newOldPoints){
        newPoints[i++]=f[0];
        newPoints[i++]=f[1];
        newPoints[i++]=f[2];
        newPoints[i++]=f[3];
      }
      srf.setIsSurfaceFinished(false);
      srf.setPointsInV(srf.getPointsInV()-1);
      bodIndex=-1;
      srf.setBodIndex(-1);
      srf.setCtrlPoints(newPoints);

    }
		
    appWindow.updateGLCanvas();
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  public void mousePressed(MouseEvent e) {
    float[] ctrlpoints=Surface.getInstance().getCtrlPoints();
    int xE=e.getX();
    int yE=e.getY();
    //		System.out.println(xE+" "+yE);
    double x,y,z=0;
		
    this.bodIndex=-1;
		
    GL gl=appWindow.getGlCanvas().getGL();
    GLU glu=appWindow.getGlListener().getGlu();
		
    int[] viewport;
    double[] mvmatrix;
    double[] projmatrix;
    int realy;
    double[] wcoord=new double[4];

    viewport=appWindow.getGlListener().getViewport();
    mvmatrix=appWindow.getGlListener().getMvmatrix();
    projmatrix=appWindow.getGlListener().getProjmatrix();

		
    for(int i=0;i<ctrlpoints.length/4;i++){
      x = ctrlpoints[i*4]/ctrlpoints[i*4+3];
      y = ctrlpoints[i*4+1]/ctrlpoints[i*4+3];
      z=ctrlpoints[i*4+2]/ctrlpoints[i*4+3];			
      //projekce souřadnic do okna
      glu.gluProject(x,y,z,mvmatrix,0,projmatrix,0,viewport,0,wcoord,0);
			
      x=wcoord[0];
      y=(viewport[3]-wcoord[1]-1);
			
      if(xE>=x-TOLERANCE&&xE<=x+TOLERANCE&&yE>=y-TOLERANCE&&yE<=y+TOLERANCE){
        this.bodIndex=i;
      }                    
    }
		
    Surface.getInstance().setBodIndex(bodIndex);
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
    //		if(this.bodIndex>=0){
    //			int x=e.getX();
    //			int y=e.getY();
    //			
    //			Surface.getInstance().setActiveX(x);
    //			Surface.getInstance().setActiveY(y);
    //		}
    //		appWindow.updateGLCanvas();
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
   */
  public void mouseMoved(MouseEvent e) {
		
  }

  /**
   * Nastaví typ prováděné činnosti
   * @param action typ prováděné činnosti
   */
  public void setActionType(String action) {
    this.actionType=action;
  }

  /**
   * Vrací index aktuálně vybraného řídícího bodu
   * @return index aktuálně vybraného řídícího bodu
   */
  public int getBodIndex() {
    return bodIndex;
  }

  /**
   * Vrací index aktuálně vybraného řídícího bodu
   * @param bodIndex aktuálně vybraného řídícího bodu
   */
  public void setBodIndex(int bodIndex) {
    this.bodIndex = bodIndex;
  }
}
