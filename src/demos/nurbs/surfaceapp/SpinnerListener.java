package demos.nurbs.surfaceapp;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Listener reacting to event occuring on components editing coords end weight of selected control point
 * Listener zpracovávající události na komponentách editujících souřadnice a váhu vybraného řídícícho bodu
 * @author Tomáš Hráský
 *
 */
public class SpinnerListener implements ChangeListener {

  /**
   * Parent window
   * Okno aplikace, k němuž listener patří
   */
  private SurfaceApp appWindow;

  /**
   * Creates new instance with link to parent window
   * Vytvoří instanci objektu s odkazem na rodičovské okno
   * @param app parent app window
   */
  public SpinnerListener(SurfaceApp app) {
    this.appWindow=app;
  }

  /* (non-Javadoc)
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {
    JSpinner src=(JSpinner) e.getSource();
    //		System.out.println(src.getValue().getClass().toString());
    float val = 0;
    if(src.getValue() instanceof Double) val=((Double) src.getValue()).floatValue();
    if(src.getValue() instanceof Float) val=((Float) src.getValue()).floatValue();
		
    if(src.getName()==SurfaceApp.X_SPINNER_NAME)
      Surface.getInstance().setActiveX(val);
    if(src.getName()==SurfaceApp.Y_SPINNER_NAME)
      Surface.getInstance().setActiveY(val);
    if(src.getName()==SurfaceApp.Z_SPINNER_NAME)
      Surface.getInstance().setActiveZ(val);
    if(src.getName()==SurfaceApp.W_SPINNER_NAME)
      Surface.getInstance().setActiveW(val);
		
		
    appWindow.updateGLCanvas();
  }
}
