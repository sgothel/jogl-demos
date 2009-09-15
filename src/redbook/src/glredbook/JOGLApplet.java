package glredbook;

import glredbook10.GLSkeleton;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JApplet;

/**
 *
 * @author michael-bien.com
 */
public class JOGLApplet extends JApplet {

    private GLSkeleton<?> skeleton;

    @Override
    public void start() {
        String className = getParameter("demo");
        loadDemo(className);
    }

    @Override
    public void stop() {
        if (skeleton != null) {
            skeleton.runExit();
        }
    }

    private Logger log() {
        return Logger.getLogger(JOGLApplet.class.getName());
    }

    public void loadDemo(String className) {

        if (skeleton != null) {
            skeleton.runExit();
            remove((Component) skeleton.drawable);
        }

        log().info("i'll try to instantiate: " + className);

        try {

            final Class<?> clazz = Class.forName(className);

            try {
                skeleton = (GLSkeleton<?>) clazz.newInstance();
                System.out.println(skeleton);
                add((Component) skeleton.drawable);
                System.out.println("added");
                validate();
            } catch (InstantiationException ex) {
                log().log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                log().log(Level.SEVERE, null, ex);
            }


        } catch (ClassNotFoundException ex) {
            log().log(Level.SEVERE, "can't find main class", ex);
        }
    }
}
