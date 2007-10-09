package demos.nurbs.icons;

import java.io.*;
import javax.swing.ImageIcon;
import com.sun.opengl.util.StreamUtil;

public class IconFactory {
  private IconFactory() {}

  public static ImageIcon getIcon(String resourceName) {
    try {
      InputStream input = IconFactory.class.getClassLoader().getResourceAsStream(resourceName);
      byte[] data = StreamUtil.readAll(input);
      return new ImageIcon(data, resourceName);
    } catch (IOException e) {
      return new ImageIcon();
    }
  }
}
