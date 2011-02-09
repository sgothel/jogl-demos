package demos.nurbs.icons;

import jogamp.opengl.io.StreamUtil;
import java.io.*;
import javax.swing.ImageIcon;

public class IconFactory {
  private IconFactory() {}

  public static ImageIcon getIcon(String resourceName) {
    try {
      InputStream input = IconFactory.class.getClassLoader().getResourceAsStream(resourceName);
      byte[] data = StreamUtil.readAll2Array(input);
      return new ImageIcon(data, resourceName);
    } catch (IOException e) {
      return new ImageIcon();
    }
  }
}
