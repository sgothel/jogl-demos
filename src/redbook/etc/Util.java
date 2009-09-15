package etc;

import java.util.Scanner;
import java.util.TreeSet;

/**
 *
 * @author mbien
 */
public class Util {


    public static void main(String[] args) {
        Scanner scanner = new Scanner(Util.class.getResourceAsStream("redbook.txt"));

        TreeSet<String> set = new TreeSet<String>();
        while(scanner.hasNext())
            set.add(scanner.next());

//        for (String item : set) {
//            System.out.println("<applet width='${JNLP.APPLET.WIDTH}' height='${JNLP.APPLET.HEIGHT}'>");
//            System.out.println("    <param name='jnlp_href' value='${JNLP.FILE}'/>");
//            System.out.println("    <param name='demo' value='"+item+"'/>");
//            System.out.println("</applet>");
//        }
        for (String item : set) {
            System.out.println("<option value='"+item+"' selected>"+item);
        }


    }
}
