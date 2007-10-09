package demos.nurbs.curveapp;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Listener reacting to events on GUI components setting coords and weight of selected control point
 * Listener zpracovávající události na komponentách editujících souřadnice a váhu vybraného řídícícho bodu
 * @author Tomáš Hráský
 *
 */
public class SpinnerListener implements ChangeListener {

  /**
   * Application window 
   * Okno aplikace, k němuž listener patří
   */
  private CurveApp appWindow;

  /**
   * Creates new instance with link to parent window
   * Vytvoří instanci objektu s odkazem na rodičovské okno
   * @param app app window
   */
  public SpinnerListener(CurveApp app) {
    this.appWindow=app;
  }

  /* (non-Javadoc)
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {
    JSpinner src=(JSpinner) e.getSource();
    float val = 0;
    if(src.getValue() instanceof Double) val=((Double) src.getValue()).floatValue();
    if(src.getValue() instanceof Float) val=((Float) src.getValue()).floatValue();
		
    if(src.getName()==CurveApp.X_SPINNER_NAME)
      Curve.getInstance().setActiveX(val);
    if(src.getName()==CurveApp.Y_SPINNER_NAME)
      Curve.getInstance().setActiveY(val);
    if(src.getName()==CurveApp.W_SPINNER_NAME)
      Curve.getInstance().setActiveW(val);
    appWindow.updateGLCanvas();
  }
}
