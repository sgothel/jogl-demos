package demos.nurbs.surfaceapp;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class reactiong to events occuring on JSliders controlling rotation
 * Třída pro zpracování událostí na JSliderech ovládajících rotaci
 * @author Tomáš Hráský 
 *
 */
public class SliderListener implements ChangeListener {

  /**
     Parent window
     *Rodičovské okno 
     */
  private SurfaceApp app;

  /**
   * Creates new instance with link to parent window
   * Vytvoří novou instanci s odkazem na rodičovské okno
   * @param app parent window
   */
  public SliderListener(SurfaceApp app) {
    this.app=app;
  }

  /* (non-Javadoc)
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {
    app.updateRotationLabels();
    app.updateGLCanvas();
  }
}
